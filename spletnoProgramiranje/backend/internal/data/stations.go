package data

import (
	"context"
	"database/sql"
	"fmt"
)

type Stop struct {
	ID        int     `json:"id"`
	Number    string  `json:"number"`
	Name      string  `json:"name"`
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
}

type StopMetadata struct {
	ID         int64            `json:"id"`
	Number     string           `json:"number"`
	Name       string           `json:"name"`
	Latitude   float64          `json:"latitude,omitempty"`
	Longitude  float64          `json:"longitude,omitempty"`
	Departures []DepartureGroup `json:"departures"`
}

type DepartureGroup struct {
	Line      string   `json:"line"`
	Direction string   `json:"direction"`
	Times     []string `json:"times"`
}

type StopStorage struct {
	db *sql.DB
}

func (s *StopStorage) ReadStation(ctx context.Context, id int64) (*Stop, error) {
	query := `
        SELECT id, number, name, latitude, longitude
        FROM stops
        WHERE id = $1
    `

	row := s.db.QueryRowContext(ctx, query, id)

	var stop Stop
	err := row.Scan(&stop.ID, &stop.Number, &stop.Name, &stop.Latitude, &stop.Longitude)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, fmt.Errorf("No stop with id %d found", id)
		}
		return nil, err
	}

	return &stop, nil
}

// fetches a list of all the stations in the database (used for a station list, to display all stations on the map and so on)
func (s *StopStorage) ReadList(ctx context.Context) ([]Stop, error) {
	query := `
        SELECT id, number, name, latitude, longitude
        FROM stops
    `

	rows, err := s.db.QueryContext(ctx, query)
	if err != nil {
		return nil, err
	}

	defer rows.Close()

	var stops []Stop
	for rows.Next() {
		var stop Stop
		err := rows.Scan(&stop.ID, &stop.Number, &stop.Name, &stop.Latitude, &stop.Longitude)
		if err != nil {
			return nil, err
		}
		stops = append(stops, stop)
	}

	if err = rows.Err(); err != nil {
		return nil, err
	}

	if len(stops) == 0 {
		return []Stop{}, nil
	}

	return stops, nil
}

func (s *StopStorage) ReadStationMetadata(ctx context.Context, id int64) (*StopMetadata, error) {
	query := `
        SELECT 
            s.id, s.number, s.name, s.latitude, s.longitude,
            d.departure,
            dir.name AS direction_name,
            l.line_code AS line_code
        FROM stops s
        LEFT JOIN departures d ON s.id = d.stop_id
        LEFT JOIN directions dir ON d.direction_id = dir.id
        LEFT JOIN lines l ON dir.line_id = l.id
        WHERE s.id = $1
        ORDER BY l.line_code, dir.name, d.departure
    `

	rows, err := s.db.QueryContext(ctx, query, id)
	if err != nil {
		return nil, fmt.Errorf("failed to query stop metadata: %w", err)
	}
	defer rows.Close()

	var stopMetadata StopMetadata
	var hasData bool

	type departureKey struct {
		line      string
		direction string
	}
	departuresMap := make(map[departureKey][]string)

	for rows.Next() {
		hasData = true

		var departure sql.NullTime
		var directionName sql.NullString
		var lineCode sql.NullString

		err := rows.Scan(
			&stopMetadata.ID,
			&stopMetadata.Number,
			&stopMetadata.Name,
			&stopMetadata.Latitude,
			&stopMetadata.Longitude,
			&departure,
			&directionName,
			&lineCode,
		)
		if err != nil {
			return nil, fmt.Errorf("failed to scan row: %w", err)
		}

		if departure.Valid && lineCode.Valid && directionName.Valid {
			key := departureKey{
				line:      lineCode.String,
				direction: directionName.String,
			}
			formattedTime := departure.Time.Format("15:04")
			departuresMap[key] = append(departuresMap[key], formattedTime)
		}
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error after row iteration: %w", err)
	}

	if !hasData {
		return nil, fmt.Errorf("no stop with id %d found", id)
	}

	for key, times := range departuresMap {
		stopMetadata.Departures = append(stopMetadata.Departures, DepartureGroup{
			Line:      key.line,
			Direction: key.direction,
			Times:     times,
		})
	}

	return &stopMetadata, nil
}
