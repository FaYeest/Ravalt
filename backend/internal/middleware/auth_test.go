package middleware_test

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/FaYeest/ravalt/backend/internal/middleware"
	"github.com/FaYeest/ravalt/backend/internal/service"
)

func TestRequireAuthMiddleware(t *testing.T) {
	jwtService := service.NewJWTService("super-secret-jwt-key-32b-length!", 1)
	authMiddleware := middleware.RequireAuth(jwtService)

	testHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		userID, ok := middleware.GetUserIDFromContext(r.Context())
		if !ok || userID == "" {
			t.Fatal("expected userID in context")
		}
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("ok"))
	})

	handlerToTest := authMiddleware(testHandler)

	// 1. Missing Authorization header -> 401
	req := httptest.NewRequest("GET", "/protected", nil)
	rr := httptest.NewRecorder()
	handlerToTest.ServeHTTP(rr, req)
	if rr.Code != http.StatusUnauthorized {
		t.Fatalf("expected 401 for missing header, got %d", rr.Code)
	}

	// 2. Invalid Token -> 401
	req = httptest.NewRequest("GET", "/protected", nil)
	req.Header.Set("Authorization", "Bearer invalid.token.here")
	rr = httptest.NewRecorder()
	handlerToTest.ServeHTTP(rr, req)
	if rr.Code != http.StatusUnauthorized {
		t.Fatalf("expected 401 for invalid token, got %d", rr.Code)
	}

	// 3. Valid Token -> 200 OK
	validToken, err := jwtService.GenerateToken("user-123", "user@example.com")
	if err != nil {
		t.Fatalf("failed to generate token: %v", err)
	}

	req = httptest.NewRequest("GET", "/protected", nil)
	req.Header.Set("Authorization", "Bearer "+validToken)
	rr = httptest.NewRecorder()
	handlerToTest.ServeHTTP(rr, req)
	if rr.Code != http.StatusOK {
		t.Fatalf("expected 200 OK for valid token, got %d", rr.Code)
	}
}
