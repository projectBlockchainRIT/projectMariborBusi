package data

import (
	"context"
	"database/sql"
	"fmt"
)

type OccupancyStorage struct {
	db *sql.DB
}

type OccupancyRecord struct {
	Time           string
	Date           string
	OccupancyLevel int
}

type AvgOccupancyByHour struct {
	HourOfDay    int
	AvgOccupancy float64
}

type AvgDailyOccupancy struct {
	Date              string
	AvgDailyOccupancy float64
}

func (s *OccupancyStorage) GetOccupancyForLineByDate(ctx context.Context, lineID int, targetDate string) ([]OccupancyRecord, error) {
	query := `
	SELECT
	  o.time,
	  o.occupancy_level
	FROM occupancy AS o
	WHERE o.line_id = $1
	  AND o.date = $2
	ORDER BY o.time;
	`

	rows, err := s.db.QueryContext(ctx, query, lineID, targetDate)
	if err != nil {
		return nil, fmt.Errorf("query failed: %w", err)
	}
	defer rows.Close()

	var results []OccupancyRecord
	for rows.Next() {
		var r OccupancyRecord
		err := rows.Scan(&r.Time, &r.OccupancyLevel)
		if err != nil {
			return nil, fmt.Errorf("scan failed: %w", err)
		}
		results = append(results, r)
	}

	return results, rows.Err()
}

func (s *OccupancyStorage) GetOccupancyForLineByDateAndHour(ctx context.Context, lineID int, targetDate string, targetHour int) ([]OccupancyRecord, error) {
	query := `
	SELECT
	  o.time,
	  o.date,
	  o.occupancy_level
	FROM occupancy AS o
	WHERE o.line_id = $1
	  AND o.date = $2
	  AND EXTRACT(HOUR FROM o.time) = $3
	ORDER BY o.time;
	`

	rows, err := s.db.QueryContext(ctx, query, lineID, targetDate, targetHour)
	if err != nil {
		return nil, fmt.Errorf("query failed: %w", err)
	}
	defer rows.Close()

	var results []OccupancyRecord
	for rows.Next() {
		var r OccupancyRecord
		err := rows.Scan(&r.Time, &r.Date, &r.OccupancyLevel)
		if err != nil {
			return nil, fmt.Errorf("scan failed: %w", err)
		}
		results = append(results, r)
	}

	return results, rows.Err()
}

func (s *OccupancyStorage) GetAvgOccupancyAllLinesByHour(ctx context.Context, targetHour int) (*AvgOccupancyByHour, error) {
	query := `
	SELECT
	  EXTRACT(HOUR FROM o.time) AS hour_of_day,
	  AVG(o.occupancy_level)::NUMERIC(10,2) AS avg_occupancy_all_lines
	FROM occupancy AS o
	WHERE EXTRACT(HOUR FROM o.time) = $1
	GROUP BY hour_of_day
	ORDER BY hour_of_day;
	`

	row := s.db.QueryRowContext(ctx, query, targetHour)

	var result AvgOccupancyByHour
	err := row.Scan(&result.HourOfDay, &result.AvgOccupancy)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, fmt.Errorf("scan failed: %w", err)
	}

	return &result, nil
}

func (s *OccupancyStorage) GetAvgDailyOccupancyAllLines(ctx context.Context, targetDate string) (*AvgDailyOccupancy, error) {
	query := `
	SELECT
	  o.date,
	  AVG(o.occupancy_level)::NUMERIC(10,2) AS avg_daily_occupancy_all_lines
	FROM occupancy AS o
	WHERE o.date = $1
	GROUP BY o.date;
	`

	row := s.db.QueryRowContext(ctx, query, targetDate)

	var result AvgDailyOccupancy
	err := row.Scan(&result.Date, &result.AvgDailyOccupancy)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, fmt.Errorf("scan failed: %w", err)
	}

	return &result, nil
}
