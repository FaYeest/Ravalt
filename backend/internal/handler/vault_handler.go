package handler

import (
	"encoding/json"
	"errors"
	"net/http"
	"time"

	"github.com/FaYeest/ravalt/backend/internal/domain"
	"github.com/FaYeest/ravalt/backend/internal/middleware"
	"github.com/FaYeest/ravalt/backend/internal/service"
	"github.com/go-chi/chi/v5"
)

type VaultHandler struct {
	vaultService *service.VaultService
}

func NewVaultHandler(vaultService *service.VaultService) *VaultHandler {
	return &VaultHandler{vaultService: vaultService}
}

type VaultItemPayload struct {
	EncryptedData string `json:"encrypted_data"`
	Nonce         string `json:"nonce"`
}

type ListVaultItemsResponse struct {
	Items []domain.VaultItem `json:"items"`
}

func (h *VaultHandler) CreateItem(w http.ResponseWriter, r *http.Request) {
	userID, ok := middleware.GetUserIDFromContext(r.Context())
	if !ok {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var req VaultItemPayload
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	item, err := h.vaultService.CreateItem(r.Context(), service.CreateVaultItemInput{
		UserID:        userID,
		EncryptedData: req.EncryptedData,
		Nonce:         req.Nonce,
	})
	if err != nil {
		switch {
		case errors.Is(err, domain.ErrInvalidNonce):
			respondError(w, http.StatusBadRequest, "invalid nonce format (must be 16-character base64)")
		case errors.Is(err, domain.ErrInvalidPayload):
			respondError(w, http.StatusBadRequest, "invalid encrypted payload (must be non-empty base64)")
		default:
			respondError(w, http.StatusInternalServerError, "failed to create vault item")
		}
		return
	}

	respondJSON(w, http.StatusCreated, item)
}

func (h *VaultHandler) ListItems(w http.ResponseWriter, r *http.Request) {
	userID, ok := middleware.GetUserIDFromContext(r.Context())
	if !ok {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var sincePtr *time.Time
	sinceQuery := r.URL.Query().Get("since")
	if sinceQuery != "" {
		parsedTime, err := time.Parse(time.RFC3339, sinceQuery)
		if err != nil {
			respondError(w, http.StatusBadRequest, "invalid since parameter format (expected RFC3339)")
			return
		}
		sincePtr = &parsedTime
	}

	items, err := h.vaultService.ListItems(r.Context(), userID, sincePtr)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to list vault items")
		return
	}

	respondJSON(w, http.StatusOK, ListVaultItemsResponse{
		Items: items,
	})
}

func (h *VaultHandler) GetItem(w http.ResponseWriter, r *http.Request) {
	userID, ok := middleware.GetUserIDFromContext(r.Context())
	if !ok {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	itemID := chi.URLParam(r, "id")
	if itemID == "" {
		respondError(w, http.StatusBadRequest, "item id is required")
		return
	}

	item, err := h.vaultService.GetItem(r.Context(), itemID, userID)
	if err != nil {
		if errors.Is(err, domain.ErrVaultItemNotFound) {
			respondError(w, http.StatusNotFound, "vault item not found")
			return
		}
		respondError(w, http.StatusInternalServerError, "failed to get vault item")
		return
	}

	respondJSON(w, http.StatusOK, item)
}

func (h *VaultHandler) UpdateItem(w http.ResponseWriter, r *http.Request) {
	userID, ok := middleware.GetUserIDFromContext(r.Context())
	if !ok {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	itemID := chi.URLParam(r, "id")
	if itemID == "" {
		respondError(w, http.StatusBadRequest, "item id is required")
		return
	}

	var req VaultItemPayload
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	item, err := h.vaultService.UpdateItem(r.Context(), service.UpdateVaultItemInput{
		ID:            itemID,
		UserID:        userID,
		EncryptedData: req.EncryptedData,
		Nonce:         req.Nonce,
	})
	if err != nil {
		switch {
		case errors.Is(err, domain.ErrVaultItemNotFound):
			respondError(w, http.StatusNotFound, "vault item not found")
		case errors.Is(err, domain.ErrInvalidNonce):
			respondError(w, http.StatusBadRequest, "invalid nonce format (must be 16-character base64)")
		case errors.Is(err, domain.ErrInvalidPayload):
			respondError(w, http.StatusBadRequest, "invalid encrypted payload (must be non-empty base64)")
		default:
			respondError(w, http.StatusInternalServerError, "failed to update vault item")
		}
		return
	}

	respondJSON(w, http.StatusOK, item)
}

func (h *VaultHandler) DeleteItem(w http.ResponseWriter, r *http.Request) {
	userID, ok := middleware.GetUserIDFromContext(r.Context())
	if !ok {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	itemID := chi.URLParam(r, "id")
	if itemID == "" {
		respondError(w, http.StatusBadRequest, "item id is required")
		return
	}

	err := h.vaultService.DeleteItem(r.Context(), itemID, userID)
	if err != nil {
		if errors.Is(err, domain.ErrVaultItemNotFound) {
			respondError(w, http.StatusNotFound, "vault item not found")
			return
		}
		respondError(w, http.StatusInternalServerError, "failed to delete vault item")
		return
	}

	respondJSON(w, http.StatusOK, map[string]string{
		"message": "vault item deleted successfully",
	})
}

type SyncRequest struct {
	Since      *string                 `json:"since"`
	Items      []service.SyncItemInput `json:"items"`
	DeletedIDs []string                `json:"deleted_ids"`
}

func (h *VaultHandler) Sync(w http.ResponseWriter, r *http.Request) {
	userID, ok := middleware.GetUserIDFromContext(r.Context())
	if !ok {
		respondError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var req SyncRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	var sincePtr *time.Time
	if req.Since != nil && *req.Since != "" {
		parsedTime, err := time.Parse(time.RFC3339, *req.Since)
		if err != nil {
			respondError(w, http.StatusBadRequest, "invalid since parameter format (expected RFC3339)")
			return
		}
		sincePtr = &parsedTime
	}

	result, err := h.vaultService.Sync(r.Context(), service.SyncInput{
		UserID:     userID,
		Since:      sincePtr,
		Items:      req.Items,
		DeletedIDs: req.DeletedIDs,
	})
	if err != nil {
		respondError(w, http.StatusInternalServerError, "failed to sync vault items")
		return
	}

	respondJSON(w, http.StatusOK, result)
}
