package data

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
)

type Route struct {
	ID   int         `json:"id"`
	Name string      `json:"name"`
	Path [][]float64 `json:"path"`
}

type RoutesStorage struct {
	db *sql.DB
}

func (s *RoutesStorage) ReadRoute(ctx context.Context, id int64) (*Route, error) {
	query := `
        SELECT id, name, path
        FROM routes
        WHERE line_id = $1
        LIMIT 1
    `

	var route Route
	var pathData []byte

	row := s.db.QueryRowContext(ctx, query, id)
	err := row.Scan(&route.ID, &route.Name, &pathData)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, fmt.Errorf("no route found for line_id %d", id)
		}
		return nil, fmt.Errorf("failed to scan route: %w", err)
	}

	if err := json.Unmarshal(pathData, &route.Path); err != nil {
		return nil, fmt.Errorf("failed to unmarshal path data: %w", err)
	}

	return &route, nil
}

func (s *RoutesStorage) ReadRouteStations(ctx context.Context, id int64) ([]Stop, error) {
	query := `
        SELECT DISTINCT s.id, s.number, s.name, s.latitude, s.longitude
        FROM stops s
        JOIN departures d ON s.id = d.stop_id
        JOIN directions dir ON d.direction_id = dir.id
        JOIN lines l ON dir.line_id = l.id
        WHERE l.id = $1
        ORDER BY s.number
    `

	var stops []Stop

	rows, err := s.db.QueryContext(ctx, query, id)
	if err != nil {
		return nil, fmt.Errorf("failed to query stops: %w", err)
	}

	defer rows.Close()

	for rows.Next() {
		var stop Stop
		err := rows.Scan(&stop.ID, &stop.Number, &stop.Name, &stop.Latitude, &stop.Longitude)
		if err != nil {
			return nil, fmt.Errorf("failed to scan stop: %w", err)
		}
		stops = append(stops, stop)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error during rows iteration: %w", err)
	}

	return stops, nil
}

func (s *RoutesStorage) ReadRoutesList(ctx context.Context) ([]Route, error) {
	query := `
        SELECT id, name, path
        FROM routes
    `

	var routes []Route

	rows, err := s.db.QueryContext(ctx, query)
	if err != nil {
		return nil, fmt.Errorf("failed to query stops: %w", err)
	}

	defer rows.Close()

	for rows.Next() {
		var route Route
		var pathData []byte
		err := rows.Scan(&route.ID, &route.Name, &pathData)
		if err != nil {
			return nil, fmt.Errorf("failed to scan stop: %w", err)
		}

		if err := json.Unmarshal(pathData, &route.Path); err != nil {
			return nil, fmt.Errorf("failed to unmarshal path data: %w", err)
		}

		routes = append(routes, route)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error during rows iteration: %w", err)
	}

	return routes, nil

}
