package service

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"fmt"
	"strings"

	"github.com/FaYeest/ravalt/backend/internal/domain"
	"golang.org/x/crypto/bcrypt"
)

type AuthService struct {
	userRepo   domain.UserRepository
	saltSecret string
	jwtService *JWTService
}

func NewAuthService(userRepo domain.UserRepository, saltSecret string, jwtService *JWTService) *AuthService {
	return &AuthService{
		userRepo:   userRepo,
		saltSecret: saltSecret,
		jwtService: jwtService,
	}
}

// GetPreloginSalt returns the user's salt if registered, or a deterministic fake salt
// to prevent user enumeration attacks.
func (s *AuthService) GetPreloginSalt(ctx context.Context, email string) (string, error) {
	cleanEmail := strings.ToLower(strings.TrimSpace(email))
	if cleanEmail == "" || !strings.Contains(cleanEmail, "@") || len(cleanEmail) > 254 {
		return "", domain.ErrInvalidEmail
	}

	user, err := s.userRepo.GetByEmail(ctx, cleanEmail)
	if err != nil {
		if errors.Is(err, domain.ErrUserNotFound) {
			// Generate deterministic 32-byte Base64 salt (44 chars) to prevent enumeration
			h := hmac.New(sha256.New, []byte(s.saltSecret))
			h.Write([]byte("ravalt-prelogin-salt:" + cleanEmail))
			fakeSalt := base64.StdEncoding.EncodeToString(h.Sum(nil))
			return fakeSalt, nil
		}
		return "", err
	}

	return user.UserSalt, nil
}

type RegisterInput struct {
	Email    string
	UserSalt string
	AuthHash string
}

func (s *AuthService) Register(ctx context.Context, input RegisterInput) (*domain.User, error) {
	cleanEmail := strings.ToLower(strings.TrimSpace(input.Email))
	if cleanEmail == "" || !strings.Contains(cleanEmail, "@") || len(cleanEmail) > 254 {
		return nil, domain.ErrInvalidEmail
	}

	cleanSalt := strings.TrimSpace(input.UserSalt)
	saltBytes, err := base64.StdEncoding.DecodeString(cleanSalt)
	if err != nil || len(saltBytes) != 32 {
		return nil, domain.ErrInvalidSalt
	}

	cleanAuthHash := strings.TrimSpace(input.AuthHash)
	if cleanAuthHash == "" || len(cleanAuthHash) < 16 {
		return nil, domain.ErrInvalidAuthHash
	}

	// Check if already registered
	existing, err := s.userRepo.GetByEmail(ctx, cleanEmail)
	if err == nil && existing != nil {
		return nil, domain.ErrUserAlreadyExists
	} else if err != nil && !errors.Is(err, domain.ErrUserNotFound) {
		return nil, err
	}

	// Hash client's auth_hash with Bcrypt before storing in database
	serverHashedAuth, err := bcrypt.GenerateFromPassword([]byte(cleanAuthHash), bcrypt.DefaultCost)
	if err != nil {
		return nil, fmt.Errorf("failed to hash auth credentials: %w", err)
	}

	user := &domain.User{
		Email:    cleanEmail,
		UserSalt: cleanSalt,
		AuthHash: string(serverHashedAuth),
	}

	if err := s.userRepo.Create(ctx, user); err != nil {
		return nil, err
	}

	return user, nil
}

type LoginInput struct {
	Email    string
	AuthHash string
}

type LoginResult struct {
	Token     string       `json:"token"`
	TokenType string       `json:"token_type"`
	ExpiresIn int64        `json:"expires_in"`
	User      *domain.User `json:"user"`
}

func (s *AuthService) Login(ctx context.Context, input LoginInput) (*LoginResult, error) {
	cleanEmail := strings.ToLower(strings.TrimSpace(input.Email))
	cleanAuthHash := strings.TrimSpace(input.AuthHash)

	if cleanEmail == "" || cleanAuthHash == "" {
		return nil, domain.ErrInvalidCredentials
	}

	user, err := s.userRepo.GetByEmail(ctx, cleanEmail)
	if err != nil {
		if errors.Is(err, domain.ErrUserNotFound) {
			return nil, domain.ErrInvalidCredentials
		}
		return nil, err
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.AuthHash), []byte(cleanAuthHash)); err != nil {
		return nil, domain.ErrInvalidCredentials
	}

	token, err := s.jwtService.GenerateToken(user.ID, user.Email)
	if err != nil {
		return nil, err
	}

	return &LoginResult{
		Token:     token,
		TokenType: "Bearer",
		ExpiresIn: int64(s.jwtService.Duration().Seconds()),
		User:      user,
	}, nil
}

func (s *AuthService) DeleteAccount(ctx context.Context, userID string) error {
	cleanID := strings.TrimSpace(userID)
	if cleanID == "" {
		return errors.New("user id is required")
	}
	return s.userRepo.Delete(ctx, cleanID)
}

type UpdatePasswordInput struct {
	UserID      string
	NewUserSalt string
	NewAuthHash string
}

func (s *AuthService) UpdateMasterPassword(ctx context.Context, input UpdatePasswordInput) error {
	cleanSalt := strings.TrimSpace(input.NewUserSalt)
	saltBytes, err := base64.StdEncoding.DecodeString(cleanSalt)
	if err != nil || len(saltBytes) != 32 {
		return domain.ErrInvalidSalt
	}

	cleanAuthHash := strings.TrimSpace(input.NewAuthHash)
	if cleanAuthHash == "" || len(cleanAuthHash) < 16 {
		return domain.ErrInvalidAuthHash
	}

	serverHashedAuth, err := bcrypt.GenerateFromPassword([]byte(cleanAuthHash), bcrypt.DefaultCost)
	if err != nil {
		return fmt.Errorf("failed to hash new auth credentials: %w", err)
	}

	return s.userRepo.UpdateCredentials(ctx, input.UserID, cleanSalt, string(serverHashedAuth))
}
