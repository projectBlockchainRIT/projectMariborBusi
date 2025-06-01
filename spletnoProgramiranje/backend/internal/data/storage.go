package data

import (
	"context"
	"database/sql"
)

type Storage struct {
	User interface {
		Create(context.Context, *User) error
		GetByEmail(context.Context, string) (*User, error)
		GetById(context.Context, int) (*User, error)
	}
	Stations interface {
		ReadStation(context.Context, int64) (*Stop, error)
		ReadList(context.Context) ([]Stop, error)
		ReadStationMetadata(context.Context, int64) (*StopMetadata, error)
		ReadStationsCloseBy(context.Context, *Location) ([]Stop, error)
		ReadThreeStationsAtDestination(context.Context, *PathLocation) ([]Stop, error)
		ReadStationLines(ctx context.Context, stops []Stop) ([]Line, error)
		ReadThreeStationsAtLocation(ctx context.Context, payload *PathLocation, lines []Line) ([]Stop, error)
	}
	Routes interface {
		ReadRoute(context.Context, int64) (*Route, error)
		ReadRouteStations(context.Context, int64) ([]Stop, error)
		ReadRoutesList(ctx context.Context) ([]Route, error)
		ReadActiveLines(ctx context.Context) (int, error)
	}
}

func NewStorage(db *sql.DB) Storage {
	return Storage{
		Stations: &StopStorage{db},
		Routes:   &RoutesStorage{db},
		User:     &UsersStorage{db},
	}
}
