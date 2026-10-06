package handler_test

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/FaYeest/ravalt/backend/internal/domain"
	"github.com/FaYeest/ravalt/backend/internal/handler"
	"github.com/FaYeest/ravalt/backend/internal/middleware"
	"github.com/FaYeest/ravalt/backend/internal/service"
	"github.com/go-chi/chi/v5"
)

type mockVaultRepo struct {
	items map[string]*domain.VaultItem
}

func (m *mockVaultRepo) Create(ctx context.Context, item *domain.VaultItem) error {
	item.ID = "vault-uuid-001"
	item.CreatedAt = time.Now().UTC()
	item.UpdatedAt = time.Now().UTC()
	m.items[item.ID] = item
	return nil
}

func (m *mockVaultRepo) GetByID(ctx context.Context, id, userID string) (*domain.VaultItem, error) {
	item, ok := m.items[id]
	if !ok || item.UserID != userID || item.DeletedAt != nil {
		return nil, domain.ErrVaultItemNotFound
	}
	return item, nil
}

func (m *mockVaultRepo) ListByUserID(ctx context.Context, userID string, since *time.Time) ([]domain.VaultItem, error) {
	result := make([]domain.VaultItem, 0)
	for _, item := range m.items {
		if item.UserID == userID {
			if since == nil && item.DeletedAt == nil {
				result = append(result, *item)
			} else if since != nil && item.UpdatedAt.After(*since) {
				result = append(result, *item)
			}
		}
	}
	return result, nil
}

func (m *mockVaultRepo) Update(ctx context.Context, item *domain.VaultItem) error {
	existing, ok := m.items[item.ID]
	if !ok || existing.UserID != item.UserID || existing.DeletedAt != nil {
		return domain.ErrVaultItemNotFound
	}
	existing.EncryptedData = item.EncryptedData
	existing.Nonce = item.Nonce
	existing.Version++
	existing.UpdatedAt = time.Now().UTC()
	item.Version = existing.Version
	item.UpdatedAt = existing.UpdatedAt
	return nil
}

func (m *mockVaultRepo) Delete(ctx context.Context, id, userID string) error {
	existing, ok := m.items[id]
	if !ok || existing.UserID != userID || existing.DeletedAt != nil {
		return domain.ErrVaultItemNotFound
	}
	now := time.Now().UTC()
	existing.DeletedAt = &now
	existing.UpdatedAt = now
	return nil
}

func setupVaultHandler() (*handler.VaultHandler, *mockVaultRepo) {
	repo := &mockVaultRepo{
		items: make(map[string]*domain.VaultItem),
	}
	vaultService := service.NewVaultService(repo)
	vaultHandler := handler.NewVaultHandler(vaultService)
	return vaultHandler, repo
}

func TestVaultHandler_CRUD(t *testing.T) {
	h, repo := setupVaultHandler()
	userID := "user-tester-456"

	validNonce := "MDEyMzQ1Njc4OWFi"
	validData := "SGVsbG8sIFplcm8tS25vd2xlZGdlIQ=="

	// 1. Create Vault Item
	createPayload, _ := json.Marshal(handler.VaultItemPayload{
		EncryptedData: validData,
		Nonce:         validNonce,
	})
	req := httptest.NewRequest(http.MethodPost, "/api/v1/vault/items", bytes.NewReader(createPayload))
	req = req.WithContext(context.WithValue(req.Context(), middleware.UserIDContextKey, userID))
	rr := httptest.NewRecorder()
	h.CreateItem(rr, req)

	if rr.Code != http.StatusCreated {
		t.Fatalf("expected status 201 for create, got %d: %s", rr.Code, rr.Body.String())
	}

	var created domain.VaultItem
	if err := json.NewDecoder(rr.Body).Decode(&created); err != nil {
		t.Fatalf("failed to decode created item: %v", err)
	}
	if created.ID != "vault-uuid-001" {
		t.Fatalf("expected item id vault-uuid-001, got %s", created.ID)
	}

	// 2. List Items
	listReq := httptest.NewRequest(http.MethodGet, "/api/v1/vault/items", nil)
	listReq = listReq.WithContext(context.WithValue(listReq.Context(), middleware.UserIDContextKey, userID))
	listRr := httptest.NewRecorder()
	h.ListItems(listRr, listReq)

	if listRr.Code != http.StatusOK {
		t.Fatalf("expected status 200 for list, got %d", listRr.Code)
	}

	var listRes handler.ListVaultItemsResponse
	if err := json.NewDecoder(listRr.Body).Decode(&listRes); err != nil {
		t.Fatalf("failed to decode list response: %v", err)
	}
	if len(listRes.Items) != 1 {
		t.Fatalf("expected 1 item in list, got %d", len(listRes.Items))
	}

	// 3. Get Single Item
	rctx := chi.NewRouteContext()
	rctx.URLParams.Add("id", "vault-uuid-001")
	getReq := httptest.NewRequest(http.MethodGet, "/api/v1/vault/items/vault-uuid-001", nil)
	getReq = getReq.WithContext(context.WithValue(getReq.Context(), chi.RouteCtxKey, rctx))
	getReq = getReq.WithContext(context.WithValue(getReq.Context(), middleware.UserIDContextKey, userID))
	getRr := httptest.NewRecorder()
	h.GetItem(getRr, getReq)

	if getRr.Code != http.StatusOK {
		t.Fatalf("expected status 200 for get, got %d", getRr.Code)
	}

	// 4. Update Item
	updatedData := "VXBkYXRlZCBjaXBoZXJ0ZXh0IQ=="
	updatePayload, _ := json.Marshal(handler.VaultItemPayload{
		EncryptedData: updatedData,
		Nonce:         validNonce,
	})
	putReq := httptest.NewRequest(http.MethodPut, "/api/v1/vault/items/vault-uuid-001", bytes.NewReader(updatePayload))
	putReq = putReq.WithContext(context.WithValue(putReq.Context(), chi.RouteCtxKey, rctx))
	putReq = putReq.WithContext(context.WithValue(putReq.Context(), middleware.UserIDContextKey, userID))
	putRr := httptest.NewRecorder()
	h.UpdateItem(putRr, putReq)

	if putRr.Code != http.StatusOK {
		t.Fatalf("expected status 200 for update, got %d", putRr.Code)
	}

	// 5. Delete Item
	delReq := httptest.NewRequest(http.MethodDelete, "/api/v1/vault/items/vault-uuid-001", nil)
	delReq = delReq.WithContext(context.WithValue(delReq.Context(), chi.RouteCtxKey, rctx))
	delReq = delReq.WithContext(context.WithValue(delReq.Context(), middleware.UserIDContextKey, userID))
	delRr := httptest.NewRecorder()
	h.DeleteItem(delRr, delReq)

	if delRr.Code != http.StatusOK {
		t.Fatalf("expected status 200 for delete, got %d", delRr.Code)
	}

	if repo.items["vault-uuid-001"].DeletedAt == nil {
		t.Fatal("expected item to be soft-deleted in repo")
	}
}

func TestVaultHandler_Sync(t *testing.T) {
	h, _ := setupVaultHandler()
	userID := "user-sync-789"

	validNonce := "MDEyMzQ1Njc4OWFi"
	validData := "SGVsbG8sIFplcm8tS25vd2xlZGdlIQ=="

	syncPayload, _ := json.Marshal(handler.SyncRequest{
		Items: []service.SyncItemInput{
			{
				EncryptedData: validData,
				Nonce:         validNonce,
			},
		},
		DeletedIDs: nil,
	})

	req := httptest.NewRequest(http.MethodPost, "/api/v1/vault/sync", bytes.NewReader(syncPayload))
	req = req.WithContext(context.WithValue(req.Context(), middleware.UserIDContextKey, userID))
	rr := httptest.NewRecorder()
	h.Sync(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("expected status 200 for sync, got %d: %s", rr.Code, rr.Body.String())
	}

	var syncRes service.SyncResult
	if err := json.NewDecoder(rr.Body).Decode(&syncRes); err != nil {
		t.Fatalf("failed to decode sync response: %v", err)
	}
	if len(syncRes.ServerItems) != 1 {
		t.Fatalf("expected 1 item returned from sync, got %d", len(syncRes.ServerItems))
	}
}
