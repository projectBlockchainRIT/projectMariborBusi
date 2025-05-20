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
		ReadList(context.Context) ([]Stop, error)
		ReadStationMetadata(context.Context, int64) (*StopMetadata, error)
	}
	Routes interface {
		ReadRoute(context.Context, int64) (*Route, error)
		ReadRouteStations(context.Context, int64) ([]Stop, error)
		ReadRoutesList(ctx context.Context) ([]Route, error)
	}
}

func NewStorage(db *sql.DB) Storage {
	return Storage{
		Stations: &StopStorage{db},
		Routes:   &RoutesStorage{db},
		User:     &UsersStorage{db},
	}
}
