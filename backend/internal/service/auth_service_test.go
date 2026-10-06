package service_test

import (
	"context"
	"testing"

	"github.com/FaYeest/ravalt/backend/internal/domain"
	"github.com/FaYeest/ravalt/backend/internal/service"
)

type mockUserRepo struct {
	users map[string]*domain.User
}

func (m *mockUserRepo) GetByEmail(ctx context.Context, email string) (*domain.User, error) {
	if u, exists := m.users[email]; exists {
		return u, nil
	}
	return nil, domain.ErrUserNotFound
}

func (m *mockUserRepo) Create(ctx context.Context, user *domain.User) error {
	m.users[user.Email] = user
	return nil
}

func TestGetPreloginSalt_AntiEnumeration(t *testing.T) {
	repo := &mockUserRepo{
		users: map[string]*domain.User{
			"existing@example.com": {
				Email:    "existing@example.com",
				UserSalt: "existing-user-salt-exact-44-chars-base64==",
			},
		},
	}

	authService := service.NewAuthService(repo, "test-server-secret-key-32b-length")

	ctx := context.Background()

	// 1. Existing user should return their actual salt
	salt1, err := authService.GetPreloginSalt(ctx, "existing@example.com")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if salt1 != "existing-user-salt-exact-44-chars-base64==" {
		t.Fatalf("expected registered salt, got %s", salt1)
	}

	// 2. Non-existing user should return deterministic fake salt (length 44 Base64)
	saltNonExist1, err := authService.GetPreloginSalt(ctx, "unknown@example.com")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(saltNonExist1) != 44 {
		t.Fatalf("expected 44-char base64 salt, got %d chars: %s", len(saltNonExist1), saltNonExist1)
	}

	// 3. Repeated call with same unknown email should return IDENTICAL salt
	saltNonExist2, err := authService.GetPreloginSalt(ctx, "unknown@example.com")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if saltNonExist1 != saltNonExist2 {
		t.Fatalf("expected deterministic salt, got different values: %s vs %s", saltNonExist1, saltNonExist2)
	}

	// 4. Different unknown email should produce DIFFERENT salt
	saltDifferent, err := authService.GetPreloginSalt(ctx, "another@example.com")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if saltNonExist1 == saltDifferent {
		t.Fatalf("expected different salt for different emails, got identical: %s", saltDifferent)
	}

	// 5. Invalid email should return ErrInvalidEmail
	_, err = authService.GetPreloginSalt(ctx, "not-an-email")
	if err != domain.ErrInvalidEmail {
		t.Fatalf("expected ErrInvalidEmail, got %v", err)
	}
}

func TestRegister(t *testing.T) {
	repo := &mockUserRepo{
		users: make(map[string]*domain.User),
	}
	authService := service.NewAuthService(repo, "test-server-secret-key-32b-length")
	ctx := context.Background()

	validSalt := "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" // 32 bytes base64 (44 chars)
	validAuthHash := "client-derived-auth-hash-minimum-length-ok"

	// 1. Success registration
	user, err := authService.Register(ctx, service.RegisterInput{
		Email:    "newuser@example.com",
		UserSalt: validSalt,
		AuthHash: validAuthHash,
	})
	if err != nil {
		t.Fatalf("unexpected register error: %v", err)
	}
	if user.Email != "newuser@example.com" {
		t.Fatalf("expected email newuser@example.com, got %s", user.Email)
	}
	if len(user.AuthHash) != 60 {
		t.Fatalf("expected 60-char bcrypt hash, got %d chars: %s", len(user.AuthHash), user.AuthHash)
	}

	// 2. Duplicate registration fails
	_, err = authService.Register(ctx, service.RegisterInput{
		Email:    "newuser@example.com",
		UserSalt: validSalt,
		AuthHash: validAuthHash,
	})
	if err != domain.ErrUserAlreadyExists {
		t.Fatalf("expected ErrUserAlreadyExists, got %v", err)
	}

	// 3. Invalid salt length fails
	_, err = authService.Register(ctx, service.RegisterInput{
		Email:    "user2@example.com",
		UserSalt: "short-invalid-salt",
		AuthHash: validAuthHash,
	})
	if err != domain.ErrInvalidSalt {
		t.Fatalf("expected ErrInvalidSalt, got %v", err)
	}

	// 4. Invalid email fails
	_, err = authService.Register(ctx, service.RegisterInput{
		Email:    "notanemail",
		UserSalt: validSalt,
		AuthHash: validAuthHash,
	})
	if err != domain.ErrInvalidEmail {
		t.Fatalf("expected ErrInvalidEmail, got %v", err)
	}
}
