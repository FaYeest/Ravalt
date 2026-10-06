package service

import (
	"context"
	"encoding/base64"
	"strings"
	"time"

	"github.com/FaYeest/ravalt/backend/internal/domain"
)

type VaultService struct {
	repo domain.VaultRepository
}

func NewVaultService(repo domain.VaultRepository) *VaultService {
	return &VaultService{repo: repo}
}

type CreateVaultItemInput struct {
	UserID        string
	EncryptedData string
	Nonce         string
}

func (s *VaultService) validateCryptoFields(encryptedData, nonce string) error {
	cleanNonce := strings.TrimSpace(nonce)
	nonceBytes, err := base64.StdEncoding.DecodeString(cleanNonce)
	if err != nil || len(nonceBytes) != 12 || len(cleanNonce) != 16 {
		return domain.ErrInvalidNonce
	}

	cleanData := strings.TrimSpace(encryptedData)
	if cleanData == "" {
		return domain.ErrInvalidPayload
	}
	if _, err := base64.StdEncoding.DecodeString(cleanData); err != nil {
		return domain.ErrInvalidPayload
	}

	return nil
}

func (s *VaultService) CreateItem(ctx context.Context, input CreateVaultItemInput) (*domain.VaultItem, error) {
	if err := s.validateCryptoFields(input.EncryptedData, input.Nonce); err != nil {
		return nil, err
	}

	item := &domain.VaultItem{
		UserID:        input.UserID,
		EncryptedData: strings.TrimSpace(input.EncryptedData),
		Nonce:         strings.TrimSpace(input.Nonce),
		Version:       1,
	}

	if err := s.repo.Create(ctx, item); err != nil {
		return nil, err
	}

	return item, nil
}

func (s *VaultService) GetItem(ctx context.Context, id, userID string) (*domain.VaultItem, error) {
	return s.repo.GetByID(ctx, id, userID)
}

func (s *VaultService) ListItems(ctx context.Context, userID string, since *time.Time) ([]domain.VaultItem, error) {
	return s.repo.ListByUserID(ctx, userID, since)
}

type UpdateVaultItemInput struct {
	ID            string
	UserID        string
	EncryptedData string
	Nonce         string
}

func (s *VaultService) UpdateItem(ctx context.Context, input UpdateVaultItemInput) (*domain.VaultItem, error) {
	if err := s.validateCryptoFields(input.EncryptedData, input.Nonce); err != nil {
		return nil, err
	}

	item := &domain.VaultItem{
		ID:            input.ID,
		UserID:        input.UserID,
		EncryptedData: strings.TrimSpace(input.EncryptedData),
		Nonce:         strings.TrimSpace(input.Nonce),
	}

	if err := s.repo.Update(ctx, item); err != nil {
		return nil, err
	}

	return item, nil
}

func (s *VaultService) DeleteItem(ctx context.Context, id, userID string) error {
	return s.repo.Delete(ctx, id, userID)
}

type SyncItemInput struct {
	ID            string `json:"id"`
	EncryptedData string `json:"encrypted_data"`
	Nonce         string `json:"nonce"`
	Version       int    `json:"version"`
}

type SyncInput struct {
	UserID     string
	Since      *time.Time
	Items      []SyncItemInput
	DeletedIDs []string
}

type SyncResult struct {
	ServerItems []domain.VaultItem `json:"server_items"`
	ServerTime  time.Time          `json:"server_time"`
}

func (s *VaultService) Sync(ctx context.Context, input SyncInput) (*SyncResult, error) {
	// 1. Process client deletions
	for _, delID := range input.DeletedIDs {
		_ = s.repo.Delete(ctx, delID, input.UserID)
	}

	// 2. Process client upserts
	for _, item := range input.Items {
		if err := s.validateCryptoFields(item.EncryptedData, item.Nonce); err != nil {
			continue
		}

		if item.ID != "" {
			existing, err := s.repo.GetByID(ctx, item.ID, input.UserID)
			if err == nil && existing != nil {
				_ = s.repo.Update(ctx, &domain.VaultItem{
					ID:            item.ID,
					UserID:        input.UserID,
					EncryptedData: strings.TrimSpace(item.EncryptedData),
					Nonce:         strings.TrimSpace(item.Nonce),
				})
				continue
			}
		}

		_ = s.repo.Create(ctx, &domain.VaultItem{
			UserID:        input.UserID,
			EncryptedData: strings.TrimSpace(item.EncryptedData),
			Nonce:         strings.TrimSpace(item.Nonce),
			Version:       1,
		})
	}

	// 3. Fetch server items updated since
	serverItems, err := s.repo.ListByUserID(ctx, input.UserID, input.Since)
	if err != nil {
		return nil, err
	}

	return &SyncResult{
		ServerItems: serverItems,
		ServerTime:  time.Now().UTC(),
	}, nil
}
