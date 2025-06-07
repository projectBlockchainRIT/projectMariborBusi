package data

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"time"

	"github.com/lib/pq"
)

type Route struct {
	ID     int         `json:"id"`
	Name   string      `json:"name"`
	Path   [][]float64 `json:"path"`
	LineID int         `json:"line_id"`
}

type RoutesStorage struct {
	db *sql.DB
}

type Coord struct {
	Lat float64 `json:"lat"`
	Lon float64 `json:"lon"`
}

type ActiveRun struct {
	DepartureID int
	DirectionID int
	ArrTimes    []time.Time
	Path        [][]float64
	StartSec    int
	EndSec      int
}

type BusPosition struct {
	DepartureID int     `json:"departure_id"`
	DirectionID int     `json:"direction_id"`
	Lat         float64 `json:"lat"`
	Lon         float64 `json:"lon"`
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
        SELECT id, name, path, line_id
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
		err := rows.Scan(&route.ID, &route.Name, &pathData, &route.LineID)
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

// a trip is a sequence of departures for a specific line, direction and route
func (s *RoutesStorage) ReadActiveLines(ctx context.Context) (int, error) {
	query := `WITH ScheduledTrips AS (
				SELECT
					d.id AS departure_id,
					d.stop_id,
					s.name AS stop_name,
					d.direction_id,
					dir.name AS direction_name,
					l.line_code,
					d.departure AS segment_start_time,
					LEAD(d.departure) OVER (PARTITION BY d.direction_id, d.stop_id ORDER BY d.departure) AS segment_end_time,
					l.id as line_table_id,
					dir.id as direction_table_id
				FROM
					public.departures d
				JOIN
					public.stops s ON d.stop_id = s.id
				JOIN
					public.directions dir ON d.direction_id = dir.id
				JOIN
					public.lines l ON dir.line_id = l.id
			),
			TripSegments AS (
				SELECT
					st.line_table_id,
					st.direction_table_id,
					st.stop_id AS current_stop_id,
					st.segment_start_time,
					LEAD(st.segment_start_time) OVER (PARTITION BY st.line_table_id, st.direction_table_id ORDER BY st.segment_start_time) AS segment_end_time
				FROM
					ScheduledTrips st
			)
			SELECT
				COUNT(DISTINCT ts.line_table_id || '-' || ts.direction_table_id || '-' || ts.segment_start_time) AS active_scheduled_trips
			FROM
				TripSegments ts
			WHERE
				(NOW()::time + INTERVAL '1 hour') >= ts.segment_start_time
				AND (NOW()::time + INTERVAL '1 hour') < ts.segment_end_time
				AND ts.segment_end_time IS NOT NULL;`

	var activeTrips int

	err := s.db.QueryRow(query).Scan(&activeTrips)

	if err != nil {
		if err == sql.ErrNoRows {
			return 0, nil
		}
		return 0, fmt.Errorf("Error querying active lines: %w", err)
	}

	return activeTrips, nil

	/*
			WITH ActiveLines AS (
		    SELECT DISTINCT
		        d.direction_id,
		        dir.line_id,
		        d.departure
		    FROM
		        public.departures d
		    JOIN
		        public.directions dir ON d.direction_id = dir.id
		    WHERE
		        (NOW()::time + INTERVAL '1 hour') >= d.departure - INTERVAL '3 minutes'
		        AND (NOW()::time + INTERVAL '1 hour') <= d.departure + INTERVAL '3 minutes'
		)
		SELECT
		    COUNT(DISTINCT line_id) AS active_lines
		FROM
		    ActiveLines;


	*/

}

func (s *RoutesStorage) FetchActiveRuns(ctx context.Context, lineID int) ([]ActiveRun, error) {
	now := time.Now()
	today := now.Format("2006-01-02")
	currentTime := now.Format("15:04:05")

	const sqlQuery = `
						SELECT
						d.id AS departure_id,
						d.direction_id AS direction_id,
						a.departure_time AS arr_times,
						r.path AS route_path,
						a.departure_time[1]::text AS start_time_str,
						a.departure_time[array_length(a.departure_time,1)]::text AS end_time_str
						FROM public.departures d
						JOIN public.arrivals a
						ON a.departures_id = d.id
						JOIN public.directions dir
						ON dir.id = d.direction_id
						JOIN public.lines l
						ON l.id = dir.line_id
						JOIN public.routes r
						ON r.line_id = l.id
						WHERE
						l.id = $1
						AND d.date = $2
						AND a.departure_time[1] <= $3::time
						AND a.departure_time[array_length(a.departure_time,1)] >= $3::time;
						`

	rows, err := s.db.Query(sqlQuery, lineID, today, currentTime)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var runs []ActiveRun
	for rows.Next() {
		var (
			runID       int
			dirID       int
			arrTimesRaw []string
			pathRaw     []byte
			startStr    string
			endStr      string
		)

		if err := rows.Scan(&runID, &dirID, pq.Array(&arrTimesRaw), &pathRaw, &startStr, &endStr); err != nil {
			return nil, err
		}

		var arrTimes []time.Time
		for _, ts := range arrTimesRaw {
			t, err := time.ParseInLocation("15:04:05", ts, time.Local)
			if err != nil {
				return nil, fmt.Errorf("cannot parse arrival time %q: %w", ts, err)
			}
			full := time.Date(now.Year(), now.Month(), now.Day(),
				t.Hour(), t.Minute(), t.Second(), 0, time.Local)
			arrTimes = append(arrTimes, full)
		}

		var path [][]float64
		if err := json.Unmarshal(pathRaw, &path); err != nil {
			fmt.Print(pathRaw)
			return nil, fmt.Errorf("cannot unmarshal path for run %d: %w", runID, err)
		}

		startT, _ := time.ParseInLocation("15:04:05", startStr, time.Local)
		endT, _ := time.ParseInLocation("15:04:05", endStr, time.Local)
		sSec := startT.Hour()*3600 + startT.Minute()*60 + startT.Second()
		eSec := endT.Hour()*3600 + endT.Minute()*60 + endT.Second()

		runs = append(runs, ActiveRun{
			DepartureID: runID,
			DirectionID: dirID,
			ArrTimes:    arrTimes,
			Path:        path,
			StartSec:    sSec,
			EndSec:      eSec,
		})
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return runs, nil
}
