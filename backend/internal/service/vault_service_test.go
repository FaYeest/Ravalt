package service_test

import (
	"context"
	"testing"
	"time"

	"github.com/FaYeest/ravalt/backend/internal/domain"
	"github.com/FaYeest/ravalt/backend/internal/service"
)

type mockVaultRepo struct {
	items map[string]*domain.VaultItem
}

func (m *mockVaultRepo) Create(ctx context.Context, item *domain.VaultItem) error {
	item.ID = "test-item-uuid-1234"
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

func TestVaultService_CRUD(t *testing.T) {
	repo := &mockVaultRepo{
		items: make(map[string]*domain.VaultItem),
	}
	vaultService := service.NewVaultService(repo)
	ctx := context.Background()

	validNonce := "MDEyMzQ1Njc4OWFi" // Base64 of 12 bytes "0123456789ab" (exact 16 chars)
	validCiphertext := "SGVsbG8sIFplcm8tS25vd2xlZGdlIQ==" // Base64 ciphertext

	// 1. Create item
	item, err := vaultService.CreateItem(ctx, service.CreateVaultItemInput{
		UserID:        "user-1",
		EncryptedData: validCiphertext,
		Nonce:         validNonce,
	})
	if err != nil {
		t.Fatalf("failed to create vault item: %v", err)
	}
	if item.ID != "test-item-uuid-1234" {
		t.Fatalf("expected ID test-item-uuid-1234, got %s", item.ID)
	}

	// 2. Invalid nonce rejection
	_, err = vaultService.CreateItem(ctx, service.CreateVaultItemInput{
		UserID:        "user-1",
		EncryptedData: validCiphertext,
		Nonce:         "invalid-nonce-too-long",
	})
	if err != domain.ErrInvalidNonce {
		t.Fatalf("expected ErrInvalidNonce, got %v", err)
	}

	// 3. Invalid payload rejection
	_, err = vaultService.CreateItem(ctx, service.CreateVaultItemInput{
		UserID:        "user-1",
		EncryptedData: "not-valid-base64@@@!!!",
		Nonce:         validNonce,
	})
	if err != domain.ErrInvalidPayload {
		t.Fatalf("expected ErrInvalidPayload, got %v", err)
	}

	// 4. Get item
	fetched, err := vaultService.GetItem(ctx, item.ID, "user-1")
	if err != nil {
		t.Fatalf("failed to get item: %v", err)
	}
	if fetched.EncryptedData != validCiphertext {
		t.Fatalf("data mismatch: %s", fetched.EncryptedData)
	}

	// 5. Update item
	updatedCiphertext := "VXBkYXRlZCBjaXBoZXJ0ZXh0IQ=="
	updatedItem, err := vaultService.UpdateItem(ctx, service.UpdateVaultItemInput{
		ID:            item.ID,
		UserID:        "user-1",
		EncryptedData: updatedCiphertext,
		Nonce:         validNonce,
	})
	if err != nil {
		t.Fatalf("failed to update item: %v", err)
	}
	if updatedItem.Version != 2 {
		t.Fatalf("expected version 2, got %d", updatedItem.Version)
	}

	// 6. List items
	list, err := vaultService.ListItems(ctx, "user-1", nil)
	if err != nil {
		t.Fatalf("failed to list items: %v", err)
	}
	if len(list) != 1 {
		t.Fatalf("expected 1 item in list, got %d", len(list))
	}

	// 7. Delete item
	if err := vaultService.DeleteItem(ctx, item.ID, "user-1"); err != nil {
		t.Fatalf("failed to delete item: %v", err)
	}

	// 8. Confirm deleted item is no longer found
	_, err = vaultService.GetItem(ctx, item.ID, "user-1")
	if err != domain.ErrVaultItemNotFound {
		t.Fatalf("expected ErrVaultItemNotFound, got %v", err)
	}
}

func TestVaultService_Sync(t *testing.T) {
	repo := &mockVaultRepo{
		items: make(map[string]*domain.VaultItem),
	}
	vaultService := service.NewVaultService(repo)
	ctx := context.Background()

	validNonce := "MDEyMzQ1Njc4OWFi"
	validCiphertext := "SGVsbG8sIFplcm8tS25vd2xlZGdlIQ=="

	// 1. Initial Sync with new items from client
	syncRes, err := vaultService.Sync(ctx, service.SyncInput{
		UserID: "user-sync-1",
		Items: []service.SyncItemInput{
			{
				EncryptedData: validCiphertext,
				Nonce:         validNonce,
			},
		},
		DeletedIDs: nil,
	})
	if err != nil {
		t.Fatalf("unexpected sync error: %v", err)
	}
	if len(syncRes.ServerItems) != 1 {
		t.Fatalf("expected 1 item synced, got %d", len(syncRes.ServerItems))
	}

	itemID := syncRes.ServerItems[0].ID

	// 2. Sync with client deletion
	syncRes2, err := vaultService.Sync(ctx, service.SyncInput{
		UserID:     "user-sync-1",
		Items:      nil,
		DeletedIDs: []string{itemID},
	})
	if err != nil {
		t.Fatalf("unexpected sync error on delete: %v", err)
	}
	if len(syncRes2.ServerItems) != 0 {
		t.Fatalf("expected 0 items after deletion, got %d", len(syncRes2.ServerItems))
	}
}
