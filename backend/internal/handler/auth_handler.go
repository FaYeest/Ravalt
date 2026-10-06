package handler

import (
	"errors"
	"net/http"

	"github.com/FaYeest/ravalt/backend/internal/domain"
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
