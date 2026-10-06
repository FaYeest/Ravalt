package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/FaYeest/ravalt/backend/internal/domain"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type VaultRepository struct {
	pool *pgxpool.Pool
}

func NewVaultRepository(pool *pgxpool.Pool) *VaultRepository {
	return &VaultRepository{pool: pool}
}

func (r *VaultRepository) Create(ctx context.Context, item *domain.VaultItem) error {
	query := `
		INSERT INTO vault_items (user_id, encrypted_data, nonce, version)
		VALUES ($1, $2, $3, $4)
		RETURNING id, created_at, updated_at
	`

	if item.Version <= 0 {
		item.Version = 1
	}

	err := r.pool.QueryRow(ctx, query, item.UserID, item.EncryptedData, item.Nonce, item.Version).Scan(
		&item.ID,
		&item.CreatedAt,
		&item.UpdatedAt,
	)
	if err != nil {
		return fmt.Errorf("failed to create vault item: %w", err)
	}

	return nil
}

func (r *VaultRepository) GetByID(ctx context.Context, id, userID string) (*domain.VaultItem, error) {
	query := `
		SELECT id, user_id, encrypted_data, nonce, version, created_at, updated_at, deleted_at
		FROM vault_items
		WHERE id = $1 AND user_id = $2 AND deleted_at IS NULL
	`

	var item domain.VaultItem
	err := r.pool.QueryRow(ctx, query, id, userID).Scan(
		&item.ID,
		&item.UserID,
		&item.EncryptedData,
		&item.Nonce,
		&item.Version,
		&item.CreatedAt,
		&item.UpdatedAt,
		&item.DeletedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, domain.ErrVaultItemNotFound
		}
		return nil, fmt.Errorf("failed to get vault item: %w", err)
	}

	return &item, nil
}

func (r *VaultRepository) ListByUserID(ctx context.Context, userID string, since *time.Time) ([]domain.VaultItem, error) {
	var query string
	var args []any

	if since == nil {
		query = `
			SELECT id, user_id, encrypted_data, nonce, version, created_at, updated_at, deleted_at
			FROM vault_items
			WHERE user_id = $1 AND deleted_at IS NULL
			ORDER BY updated_at DESC
		`
		args = []any{userID}
	} else {
		// During sync, return items updated after 'since' (including soft-deleted ones so client can remove them)
		query = `
			SELECT id, user_id, encrypted_data, nonce, version, created_at, updated_at, deleted_at
			FROM vault_items
			WHERE user_id = $1 AND updated_at > $2
			ORDER BY updated_at ASC
		`
		args = []any{userID, *since}
	}

	rows, err := r.pool.Query(ctx, query, args...)
	if err != nil {
		return nil, fmt.Errorf("failed to list vault items: %w", err)
	}
	defer rows.Close()

	items := make([]domain.VaultItem, 0)
	for rows.Next() {
		var item domain.VaultItem
		if err := rows.Scan(
			&item.ID,
			&item.UserID,
			&item.EncryptedData,
			&item.Nonce,
			&item.Version,
			&item.CreatedAt,
			&item.UpdatedAt,
			&item.DeletedAt,
		); err != nil {
			return nil, fmt.Errorf("failed to scan vault item: %w", err)
		}
		items = append(items, item)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("row error listing vault items: %w", err)
	}

	return items, nil
}

func (r *VaultRepository) Update(ctx context.Context, item *domain.VaultItem) error {
	query := `
		UPDATE vault_items
		SET encrypted_data = $1, nonce = $2, version = version + 1, updated_at = CURRENT_TIMESTAMP
		WHERE id = $3 AND user_id = $4 AND deleted_at IS NULL
		RETURNING version, updated_at
	`

	err := r.pool.QueryRow(ctx, query, item.EncryptedData, item.Nonce, item.ID, item.UserID).Scan(
		&item.Version,
		&item.UpdatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.ErrVaultItemNotFound
		}
		return fmt.Errorf("failed to update vault item: %w", err)
	}

	return nil
}

func (r *VaultRepository) Delete(ctx context.Context, id, userID string) error {
	query := `
		UPDATE vault_items
		SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
		WHERE id = $1 AND user_id = $2 AND deleted_at IS NULL
	`

	cmdTag, err := r.pool.Exec(ctx, query, id, userID)
	if err != nil {
		return fmt.Errorf("failed to delete vault item: %w", err)
	}

	if cmdTag.RowsAffected() == 0 {
		return domain.ErrVaultItemNotFound
	}

	return nil
}
