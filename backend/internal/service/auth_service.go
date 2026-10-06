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
}

func NewAuthService(userRepo domain.UserRepository, saltSecret string) *AuthService {
	return &AuthService{
		userRepo:   userRepo,
		saltSecret: saltSecret,
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
