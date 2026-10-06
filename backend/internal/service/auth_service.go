package service

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"strings"

	"github.com/FaYeest/ravalt/backend/internal/domain"
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
