package domain

import (
	"context"
	"errors"
	"time"
)

var (
	ErrUserNotFound         = errors.New("user not found")
	ErrUserAlreadyExists    = errors.New("user already exists")
	ErrInvalidEmail         = errors.New("invalid email address")
	ErrInvalidSalt          = errors.New("invalid user salt")
	ErrInvalidAuthHash      = errors.New("invalid auth hash")
	ErrInvalidCredentials   = errors.New("invalid email or master credentials")
)

type User struct {
	ID        string    `json:"id"`
	Email     string    `json:"email"`
	UserSalt  string    `json:"user_salt"`
	AuthHash  string    `json:"-"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

type UserRepository interface {
	GetByEmail(ctx context.Context, email string) (*User, error)
	GetByID(ctx context.Context, id string) (*User, error)
	Create(ctx context.Context, user *User) error
	UpdateCredentials(ctx context.Context, id, newUserSalt, newAuthHash string) error
	Delete(ctx context.Context, id string) error
}
