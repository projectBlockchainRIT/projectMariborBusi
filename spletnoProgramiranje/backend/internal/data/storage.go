package data

import (
	"context"
	"database/sql"
)

type Storage struct {
	User interface {
		Create(context.Context, *User) error
	}
	Stations interface {
		ReadStation(context.Context, int64) (*Stop, error)
		ReadList(ctx context.Context) ([]Stop, error)
		ReadStationMetadata(context.Context, int64) (*StopMetadata, error)
	}
}

func NewStorage(db *sql.DB) Storage {
	return Storage{
		Stations: &StopStorage{db},
		User:     &UsersStorage{db},
	}
}
