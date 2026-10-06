package handler

import (
	"encoding/json"
	"errors"
	"net/http"

	"github.com/FaYeest/ravalt/backend/internal/domain"
	"github.com/FaYeest/ravalt/backend/internal/middleware"
	"github.com/FaYeest/ravalt/backend/internal/service"
)

type AuthHandler struct {
	authService *service.AuthService
}

func NewAuthHandler(authService *service.AuthService) *AuthHandler {
	return &AuthHandler{
		authService: authService,
	}
}

type PreloginResponse struct {
	Salt string `json:"salt"`
}

func (h *AuthHandler) Prelogin(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Query().Get("email")
	if email == "" {
		respondError(w, http.StatusBadRequest, "email query parameter is required")
		return
	}

	salt, err := h.authService.GetPreloginSalt(r.Context(), email)
	if err != nil {
		if errors.Is(err, domain.ErrInvalidEmail) {
			respondError(w, http.StatusBadRequest, "invalid email address")
			return
		}
		respondError(w, http.StatusInternalServerError, "failed to process prelogin request")
		return
	}

	respondJSON(w, http.StatusOK, PreloginResponse{
		Salt: salt,
	})
}

type RegisterRequest struct {
	Email    string `json:"email"`
	Salt     string `json:"salt"`
	AuthHash string `json:"auth_hash"`
}

type RegisterResponse struct {
	ID        string    `json:"id"`
	Email     string    `json:"email"`
	CreatedAt string    `json:"created_at"`
}

func (h *AuthHandler) Register(w http.ResponseWriter, r *http.Request) {
	var req RegisterRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	user, err := h.authService.Register(r.Context(), service.RegisterInput{
		Email:    req.Email,
		UserSalt: req.Salt,
		AuthHash: req.AuthHash,
	})
	if err != nil {
		switch {
		case errors.Is(err, domain.ErrInvalidEmail):
			respondError(w, http.StatusBadRequest, "invalid email address")
		case errors.Is(err, domain.ErrInvalidSalt):
			respondError(w, http.StatusBadRequest, "invalid salt format (must be 32-byte base64)")
		case errors.Is(err, domain.ErrInvalidAuthHash):
			respondError(w, http.StatusBadRequest, "invalid auth hash")
		case errors.Is(err, domain.ErrUserAlreadyExists):
			respondError(w, http.StatusConflict, "user with this email already exists")
		default:
			respondError(w, http.StatusInternalServerError, "failed to register user")
		}
		return
	}

	respondJSON(w, http.StatusCreated, RegisterResponse{
		ID:        user.ID,
		Email:     user.Email,
		CreatedAt: user.CreatedAt.Format("2006-01-02T15:04:05Z07:00"),
	})
}

type LoginRequest struct {
	Email    string `json:"email"`
	AuthHash string `json:"auth_hash"`
}

type UserSummary struct {
	ID    string `json:"id"`
	Email string `json:"email"`
}

type LoginResponse struct {
	Token     string      `json:"token"`
	TokenType string      `json:"token_type"`
	ExpiresIn int64       `json:"expires_in"`
	User      UserSummary `json:"user"`
}

func (h *AuthHandler) Login(w http.ResponseWriter, r *http.Request) {
	var req LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	result, err := h.authService.Login(r.Context(), service.LoginInput{
		Email:    req.Email,
		AuthHash: req.AuthHash,
	})
	if err != nil {
		if errors.Is(err, domain.ErrInvalidCredentials) {
			respondError(w, http.StatusUnauthorized, "invalid email or master credentials")
			return
		}
		respondError(w, http.StatusInternalServerError, "failed to authenticate")
		return
	}

	respondJSON(w, http.StatusOK, LoginResponse{
		Token:     result.Token,
		TokenType: result.TokenType,
		ExpiresIn: result.ExpiresIn,
		User: UserSummary{
			ID:    result.User.ID,
			Email: result.User.Email,
		},
	})
}

func (h *AuthHandler) Me(w http.ResponseWriter, r *http.Request) {
	userID, ok := middleware.GetUserIDFromContext(r.Context())
	if !ok {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	email, _ := middleware.GetEmailFromContext(r.Context())

	respondJSON(w, http.StatusOK, map[string]string{
		"id":    userID,
		"email": email,
	})
}

func (h *AuthHandler) DeleteMe(w http.ResponseWriter, r *http.Request) {
	userID, ok := middleware.GetUserIDFromContext(r.Context())
	if !ok {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	if err := h.authService.DeleteAccount(r.Context(), userID); err != nil {
		if errors.Is(err, domain.ErrUserNotFound) {
			respondError(w, http.StatusNotFound, "user not found")
			return
		}
		respondError(w, http.StatusInternalServerError, "failed to delete account")
		return
	}

	respondJSON(w, http.StatusOK, map[string]string{
		"message": "account and all vault data successfully deleted",
	})
}

type UpdatePasswordRequest struct {
	NewSalt     string `json:"new_salt"`
	NewAuthHash string `json:"new_auth_hash"`
}

func (h *AuthHandler) UpdatePassword(w http.ResponseWriter, r *http.Request) {
	userID, ok := middleware.GetUserIDFromContext(r.Context())
	if !ok {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var req UpdatePasswordRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	err := h.authService.UpdateMasterPassword(r.Context(), service.UpdatePasswordInput{
		UserID:      userID,
		NewUserSalt: req.NewSalt,
		NewAuthHash: req.NewAuthHash,
	})
	if err != nil {
		switch {
		case errors.Is(err, domain.ErrInvalidSalt):
			respondError(w, http.StatusBadRequest, "invalid salt format (must be 32-byte base64)")
		case errors.Is(err, domain.ErrInvalidAuthHash):
			respondError(w, http.StatusBadRequest, "invalid auth hash")
		case errors.Is(err, domain.ErrUserNotFound):
			respondError(w, http.StatusNotFound, "user not found")
		default:
			respondError(w, http.StatusInternalServerError, "failed to update master credentials")
		}
		return
	}

	respondJSON(w, http.StatusOK, map[string]string{
		"message": "master credentials successfully updated",
	})
}
