package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/FaYeest/ravalt/backend/internal/domain"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

type UserRepository struct {
	pool *pgxpool.Pool
}

func NewUserRepository(pool *pgxpool.Pool) *UserRepository {
	return &UserRepository{pool: pool}
}

func (r *UserRepository) GetByEmail(ctx context.Context, email string) (*domain.User, error) {
	query := `
		SELECT id, email, user_salt, auth_hash, created_at, updated_at
		FROM users
		WHERE email = $1
	`

	var user domain.User
	err := r.pool.QueryRow(ctx, query, email).Scan(
		&user.ID,
		&user.Email,
		&user.UserSalt,
		&user.AuthHash,
		&user.CreatedAt,
		&user.UpdatedAt,
	)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, domain.ErrUserNotFound
		}
		return nil, fmt.Errorf("failed to get user by email: %w", err)
	}

	return &user, nil
}

func (r *UserRepository) Create(ctx context.Context, user *domain.User) error {
	query := `
		INSERT INTO users (email, user_salt, auth_hash)
		VALUES ($1, $2, $3)
		RETURNING id, created_at, updated_at
	`

	err := r.pool.QueryRow(ctx, query, user.Email, user.UserSalt, user.AuthHash).Scan(
		&user.ID,
		&user.CreatedAt,
		&user.UpdatedAt,
	)

	if err != nil {
		var pgErr *pgconn.PgError
		if errors.As(err, &pgErr) && pgErr.Code == "23505" {
			return domain.ErrUserAlreadyExists
		}
		return fmt.Errorf("failed to create user: %w", err)
	}

	return nil
}

func (r *UserRepository) GetByID(ctx context.Context, id string) (*domain.User, error) {
	query := `
		SELECT id, email, user_salt, auth_hash, created_at, updated_at
		FROM users
		WHERE id = $1
	`

	var user domain.User
	err := r.pool.QueryRow(ctx, query, id).Scan(
		&user.ID,
		&user.Email,
		&user.UserSalt,
		&user.AuthHash,
		&user.CreatedAt,
		&user.UpdatedAt,
	)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, domain.ErrUserNotFound
		}
		return nil, fmt.Errorf("failed to get user by id: %w", err)
	}

	return &user, nil
}

func (r *UserRepository) UpdateCredentials(ctx context.Context, id, newUserSalt, newAuthHash string) error {
	query := `
		UPDATE users
		SET user_salt = $1, auth_hash = $2, updated_at = CURRENT_TIMESTAMP
		WHERE id = $3
	`

	tag, err := r.pool.Exec(ctx, query, newUserSalt, newAuthHash, id)
	if err != nil {
		return fmt.Errorf("failed to update user credentials: %w", err)
	}

	if tag.RowsAffected() == 0 {
		return domain.ErrUserNotFound
	}

	return nil
}

func (r *UserRepository) Delete(ctx context.Context, id string) error {
	query := `
		DELETE FROM users
		WHERE id = $1
	`

	tag, err := r.pool.Exec(ctx, query, id)
	if err != nil {
		return fmt.Errorf("failed to delete user: %w", err)
	}

	if tag.RowsAffected() == 0 {
		return domain.ErrUserNotFound
	}

	return nil
}
