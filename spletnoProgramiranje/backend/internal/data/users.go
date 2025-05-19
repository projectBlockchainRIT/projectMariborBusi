package data

import (
	"context"
	"database/sql"
	"time"
)

type User struct {
	ID        int       `pg:"id,pk" json:"id"`
	Username  string    `pg:"username,unique" json:"username"`
	Email     string    `pg:"email,unique" json:"email"`
	Password  string    `pg:"password" json:"-"`
	CreatedAt time.Time `pg:"created_at,default:now()" json:"created_at"`
	LastLogin time.Time `pg:"last_login" json:"last_login"`
}

type UsersStorage struct {
	db *sql.DB
}

func (s *UsersStorage) Create(ctx context.Context, user *User) error {
	query := "INSERT INTO users (username, email, password) VALUES ($1, $2, $3), RETURNING id, created_at"

	row := s.db.QueryRowContext(ctx, query, user.Username, user.Email, user.Password)

	err := row.Scan(&user.ID, &user.Username, &user.Email, &user.CreatedAt)
	if err != nil {
		return err
	}

	return nil
}
