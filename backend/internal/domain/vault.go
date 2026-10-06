package domain

import (
	"context"
	"errors"
	"time"
)

var (
	ErrVaultItemNotFound = errors.New("vault item not found")
	ErrInvalidNonce      = errors.New("invalid nonce format (must be 16-character base64)")
	ErrInvalidPayload    = errors.New("invalid encrypted payload")
)

type VaultItem struct {
	ID            string     `json:"id"`
	UserID        string     `json:"user_id,omitempty"`
	EncryptedData string     `json:"encrypted_data"`
	Nonce         string     `json:"nonce"`
	Version       int        `json:"version"`
	CreatedAt     time.Time  `json:"created_at"`
	UpdatedAt     time.Time  `json:"updated_at"`
	DeletedAt     *time.Time `json:"deleted_at,omitempty"`
}

type VaultRepository interface {
	Create(ctx context.Context, item *VaultItem) error
	GetByID(ctx context.Context, id, userID string) (*VaultItem, error)
	ListByUserID(ctx context.Context, userID string, since *time.Time) ([]VaultItem, error)
	Update(ctx context.Context, item *VaultItem) error
	Delete(ctx context.Context, id, userID string) error
}
