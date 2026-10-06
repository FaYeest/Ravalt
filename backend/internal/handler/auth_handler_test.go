package handler_test

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/FaYeest/ravalt/backend/internal/domain"
	"github.com/FaYeest/ravalt/backend/internal/handler"
	"github.com/FaYeest/ravalt/backend/internal/middleware"
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

func (m *mockUserRepo) GetByID(ctx context.Context, id string) (*domain.User, error) {
	for _, u := range m.users {
		if u.ID == id {
			return u, nil
		}
	}
	return nil, domain.ErrUserNotFound
}

func (m *mockUserRepo) Create(ctx context.Context, user *domain.User) error {
	if _, exists := m.users[user.Email]; exists {
		return domain.ErrUserAlreadyExists
	}
	user.ID = "generated-user-uuid"
	m.users[user.Email] = user
	return nil
}

func (m *mockUserRepo) UpdateCredentials(ctx context.Context, id, newUserSalt, newAuthHash string) error {
	for _, u := range m.users {
		if u.ID == id {
			u.UserSalt = newUserSalt
			u.AuthHash = newAuthHash
			return nil
		}
	}
	return domain.ErrUserNotFound
}

func (m *mockUserRepo) Delete(ctx context.Context, id string) error {
	for email, u := range m.users {
		if u.ID == id {
			delete(m.users, email)
			return nil
		}
	}
	return domain.ErrUserNotFound
}

func setupAuthHandler() (*handler.AuthHandler, *service.JWTService, *mockUserRepo) {
	repo := &mockUserRepo{
		users: make(map[string]*domain.User),
	}
	jwtService := service.NewJWTService("super-secure-jwt-secret-at-least-32b-long", 24)
	authService := service.NewAuthService(repo, "server-salt-secret-32b-length!!", jwtService)
	authHandler := handler.NewAuthHandler(authService)
	return authHandler, jwtService, repo
}

func TestAuthHandler_Prelogin(t *testing.T) {
	h, _, _ := setupAuthHandler()

	// 1. Success with valid email
	req := httptest.NewRequest(http.MethodGet, "/api/v1/auth/prelogin?email=test@example.com", nil)
	rr := httptest.NewRecorder()
	h.Prelogin(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", rr.Code)
	}

	var res handler.PreloginResponse
	if err := json.NewDecoder(rr.Body).Decode(&res); err != nil {
		t.Fatalf("failed to decode response: %v", err)
	}
	if len(res.Salt) != 44 {
		t.Fatalf("expected 44-char base64 salt, got %d (%s)", len(res.Salt), res.Salt)
	}

	// 2. Missing email query param
	req = httptest.NewRequest(http.MethodGet, "/api/v1/auth/prelogin", nil)
	rr = httptest.NewRecorder()
	h.Prelogin(rr, req)

	if rr.Code != http.StatusBadRequest {
		t.Fatalf("expected status 400 for missing email, got %d", rr.Code)
	}
}

func TestAuthHandler_RegisterAndLogin(t *testing.T) {
	h, _, _ := setupAuthHandler()

	validSalt := "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
	validHash := "client-derived-auth-hash-minimum-16"

	// 1. Register
	regBody, _ := json.Marshal(handler.RegisterRequest{
		Email:    "newuser@ravalt.io",
		Salt:     validSalt,
		AuthHash: validHash,
	})
	req := httptest.NewRequest(http.MethodPost, "/api/v1/auth/register", bytes.NewReader(regBody))
	rr := httptest.NewRecorder()
	h.Register(rr, req)

	if rr.Code != http.StatusCreated {
		t.Fatalf("expected status 201 for register, got %d: %s", rr.Code, rr.Body.String())
	}

	// 2. Duplicate registration conflict
	req = httptest.NewRequest(http.MethodPost, "/api/v1/auth/register", bytes.NewReader(regBody))
	rr = httptest.NewRecorder()
	h.Register(rr, req)

	if rr.Code != http.StatusConflict {
		t.Fatalf("expected status 409 for duplicate register, got %d", rr.Code)
	}

	// 3. Login success
	loginBody, _ := json.Marshal(handler.LoginRequest{
		Email:    "newuser@ravalt.io",
		AuthHash: validHash,
	})
	req = httptest.NewRequest(http.MethodPost, "/api/v1/auth/login", bytes.NewReader(loginBody))
	rr = httptest.NewRecorder()
	h.Login(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("expected status 200 for login, got %d", rr.Code)
	}

	var loginRes handler.LoginResponse
	if err := json.NewDecoder(rr.Body).Decode(&loginRes); err != nil {
		t.Fatalf("failed to decode login response: %v", err)
	}
	if loginRes.Token == "" {
		t.Fatal("expected JWT token in login response")
	}

	// 4. Login with wrong password
	badLoginBody, _ := json.Marshal(handler.LoginRequest{
		Email:    "newuser@ravalt.io",
		AuthHash: "wrong-password-credentials",
	})
	req = httptest.NewRequest(http.MethodPost, "/api/v1/auth/login", bytes.NewReader(badLoginBody))
	rr = httptest.NewRecorder()
	h.Login(rr, req)

	if rr.Code != http.StatusUnauthorized {
		t.Fatalf("expected status 401 for wrong credentials, got %d", rr.Code)
	}
}

func TestAuthHandler_MeAndDelete(t *testing.T) {
	h, _, repo := setupAuthHandler()

	repo.users["me@ravalt.io"] = &domain.User{
		ID:    "user-me-123",
		Email: "me@ravalt.io",
	}

	// 1. Authorized Me request
	req := httptest.NewRequest(http.MethodGet, "/api/v1/auth/me", nil)
	ctx := context.WithValue(req.Context(), middleware.UserIDContextKey, "user-me-123")
	ctx = context.WithValue(ctx, middleware.EmailContextKey, "me@ravalt.io")
	req = req.WithContext(ctx)

	rr := httptest.NewRecorder()
	h.Me(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("expected status 200 for Me, got %d", rr.Code)
	}

	// 2. Delete account
	delReq := httptest.NewRequest(http.MethodDelete, "/api/v1/auth/me", nil)
	delReq = delReq.WithContext(ctx)
	delRr := httptest.NewRecorder()
	h.DeleteMe(delRr, delReq)

	if delRr.Code != http.StatusOK {
		t.Fatalf("expected status 200 for DeleteMe, got %d", delRr.Code)
	}

	if _, exists := repo.users["me@ravalt.io"]; exists {
		t.Fatal("expected user to be removed from repo")
	}
}
