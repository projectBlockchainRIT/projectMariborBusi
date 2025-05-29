package data

import (
	"context"
	"database/sql"
	"fmt"
	"time"
)

type RegisterUserPayload struct {
	Username string `json:"username"`
	Email    string `json:"email"`
	Password string `json:"password"`
}

type User struct {
	ID        int        `json:"id"`
	Username  string     `json:"username"`
	Email     string     `json:"email"`
	Password  string     `json:"password"`
	CreatedAt time.Time  `json:"created_at"`
	LastLogin *time.Time `json:"last_login"`
}

type LoginUserPayload struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

type UsersStorage struct {
	db *sql.DB
}

func (s *UsersStorage) Create(ctx context.Context, user *User) error {
	//fmt.Printf("Username: %s\nEmail: %s\nPassword: %s\n", user.Username, user.Email, user.Password)
	_, err := s.db.Exec("INSERT INTO users (username, email, password) VALUES ($1, $2, $3)", user.Username, user.Email, user.Password)

	if err != nil {
		return err
	}

	return nil
}

func (s *UsersStorage) GetByEmail(ctx context.Context, email string) (*User, error) {
	rows, err := s.db.Query("SELECT * FROM users WHERE email = $1", email)

	if err != nil {
		fmt.Print(err.Error())
		return nil, err
	}

	var user User
	//lastLogin sql.NullTime

	for rows.Next() {
		err := rows.Scan(&user.ID, &user.Username, &user.Email, &user.Password, &user.CreatedAt, &user.LastLogin)
		if err != nil {
			fmt.Print(err.Error())
			return nil, err
		}
	}

	if user.ID == 0 {
		return nil, fmt.Errorf("User not found")
	}

	return &user, nil
}

func (s *UsersStorage) GetById(ctx context.Context, id int) (*User, error) {
	rows, err := s.db.Query("SELECT * FROM users WHERE id = $1", id)

	if err != nil {
		return nil, err
	}

	var user User

	for rows.Next() {
		err := rows.Scan(&user.ID, &user.Username, &user.Email, &user.Password, &user.CreatedAt, &user.LastLogin)
		if err != nil {
			return nil, err
		}
	}

	if user.ID == 0 {
		return nil, fmt.Errorf("User not found")
	}

	return &user, nil
}
