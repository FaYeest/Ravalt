package middleware

import (
	"context"
	"encoding/json"
	"net/http"
	"strings"

	"github.com/FaYeest/ravalt/backend/internal/service"
)

type contextKey string

const (
	UserIDContextKey contextKey = "user_id"
	EmailContextKey  contextKey = "email"
)

func RequireAuth(jwtService *service.JWTService) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			authHeader := r.Header.Get("Authorization")
			if authHeader == "" {
				respondUnauthorized(w, "authorization header is required")
				return
			}

			parts := strings.SplitN(authHeader, " ", 2)
			if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
				respondUnauthorized(w, "invalid authorization header format, expected 'Bearer <token>'")
				return
			}

			tokenStr := strings.TrimSpace(parts[1])
			claims, err := jwtService.ValidateToken(tokenStr)
			if err != nil {
				respondUnauthorized(w, "invalid or expired token")
				return
			}

			ctx := context.WithValue(r.Context(), UserIDContextKey, claims.UserID)
			ctx = context.WithValue(ctx, EmailContextKey, claims.Email)

			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

func GetUserIDFromContext(ctx context.Context) (string, bool) {
	val, ok := ctx.Value(UserIDContextKey).(string)
	return val, ok
}

func GetEmailFromContext(ctx context.Context) (string, bool) {
	val, ok := ctx.Value(EmailContextKey).(string)
	return val, ok
}

func respondUnauthorized(w http.ResponseWriter, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusUnauthorized)
	_ = json.NewEncoder(w).Encode(map[string]string{
		"error": message,
	})
}
