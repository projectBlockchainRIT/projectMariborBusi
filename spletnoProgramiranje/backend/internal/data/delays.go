package data

import (
	"context"
	"database/sql"
	"fmt"
	"time"
)

type Delay struct {
	ID       int
	Date     time.Time
	DelayMin int
	LineID   int
	LineCode string
	UserID   sql.NullInt64
	Username sql.NullString
}

type DelayEntry struct {
	ID       int
	Date     time.Time
	DelayMin int
	StopID   int
	StopName string
	UserID   sql.NullInt64
	Username sql.NullString
}

type UserDelay struct {
	ID       int
	Date     time.Time
	DelayMin int
	StopID   int
	StopName string
	LineID   int
	LineCode string
}

type MostRecentDelay struct {
	ID       int
	Date     time.Time
	DelayMin int
	StopID   int
	StopName string
	LineID   int
	LineCode string
	UserID   sql.NullInt64
	Username sql.NullString
}

type LineDelayCount struct {
	LineID     int
	LineCode   string
	DelayCount int
}

type LineAverageDelay struct {
	LineID       int
	LineCode     string
	AvgDelayMins float64
}

type DelaysStorage struct {
	db *sql.DB
}

func (s *DelaysStorage) GetDelaysByStop(ctx context.Context, stopID int64) ([]Delay, error) {
	query := `
	SELECT
	  d.id,
	  d.date,
	  d.delay_min,
	  d.line_id,
	  l.line_code,
	  d.user_id,
	  u.username
	FROM delays AS d
	JOIN stops   AS s ON d.stop_id = s.id
	JOIN lines   AS l ON d.line_id = l.id
	LEFT JOIN users AS u ON d.user_id = u.id
	WHERE s.id = $1
	ORDER BY d.date DESC, d.id DESC;
	`

	rows, err := s.db.QueryContext(ctx, query, stopID)
	if err != nil {
		return nil, fmt.Errorf("query failed: %w", err)
	}
	defer rows.Close()

	var delays []Delay
	for rows.Next() {
		var d Delay
		err := rows.Scan(
			&d.ID,
			&d.Date,
			&d.DelayMin,
			&d.LineID,
			&d.LineCode,
			&d.UserID,
			&d.Username,
		)
		if err != nil {
			return nil, fmt.Errorf("row scan failed: %w", err)
		}
		delays = append(delays, d)
	}

	if err = rows.Err(); err != nil {
		return nil, fmt.Errorf("row iteration failed: %w", err)
	}

	return delays, nil
}

func (s *DelaysStorage) GetRecentDelaysByLine(ctx context.Context, lineID int64) ([]DelayEntry, error) {
	query := `
	SELECT
	  d.id,
	  d.date,
	  d.delay_min,
	  d.stop_id,
	  s.name   AS stop_name,
	  d.user_id,
	  u.username
	FROM delays AS d
	JOIN stops   AS s ON d.stop_id = s.id
	LEFT JOIN users AS u ON d.user_id = u.id
	WHERE d.line_id = $1
	ORDER BY d.date DESC, d.id DESC
	LIMIT 8;
	`

	rows, err := s.db.QueryContext(ctx, query, lineID)
	if err != nil {
		return nil, fmt.Errorf("query failed: %w", err)
	}
	defer rows.Close()

	var delays []DelayEntry
	for rows.Next() {
		var d DelayEntry
		err := rows.Scan(
			&d.ID,
			&d.Date,
			&d.DelayMin,
			&d.StopID,
			&d.StopName,
			&d.UserID,
			&d.Username,
		)
		if err != nil {
			return nil, fmt.Errorf("row scan failed: %w", err)
		}
		delays = append(delays, d)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("row iteration failed: %w", err)
	}

	return delays, nil
}

func (s *DelaysStorage) GetDelaysByUser(ctx context.Context, userID int64) ([]UserDelay, error) {
	query := `
	SELECT
	  d.id,
	  d.date,
	  d.delay_min,
	  d.stop_id,
	  s.name   AS stop_name,
	  d.line_id,
	  l.line_code
	FROM delays AS d
	JOIN stops AS s ON d.stop_id = s.id
	JOIN lines AS l ON d.line_id = l.id
	WHERE d.user_id = $1
	ORDER BY d.date DESC, d.id DESC;
	`

	rows, err := s.db.QueryContext(ctx, query, userID)
	if err != nil {
		return nil, fmt.Errorf("query failed: %w", err)
	}
	defer rows.Close()

	var delays []UserDelay
	for rows.Next() {
		var d UserDelay
		err := rows.Scan(
			&d.ID,
			&d.Date,
			&d.DelayMin,
			&d.StopID,
			&d.StopName,
			&d.LineID,
			&d.LineCode,
		)
		if err != nil {
			return nil, fmt.Errorf("row scan failed: %w", err)
		}
		delays = append(delays, d)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("row iteration failed: %w", err)
	}

	return delays, nil
}

func (s *DelaysStorage) GetMostRecentDelays(ctx context.Context) ([]MostRecentDelay, error) {
	query := `
	SELECT
	  d.id,
	  d.date,
	  d.delay_min,
	  d.stop_id,
	  s.name   AS stop_name,
	  d.line_id,
	  l.line_code,
	  d.user_id,
	  u.username
	FROM delays AS d
	JOIN stops   AS s ON d.stop_id = s.id
	JOIN lines   AS l ON d.line_id = l.id
	LEFT JOIN users AS u ON d.user_id = u.id
	ORDER BY d.date DESC, d.id DESC
	LIMIT 15;
	`

	rows, err := s.db.QueryContext(ctx, query)
	if err != nil {
		return nil, fmt.Errorf("query failed: %w", err)
	}
	defer rows.Close()

	var delays []MostRecentDelay
	for rows.Next() {
		var d MostRecentDelay
		err := rows.Scan(
			&d.ID,
			&d.Date,
			&d.DelayMin,
			&d.StopID,
			&d.StopName,
			&d.LineID,
			&d.LineCode,
			&d.UserID,
			&d.Username,
		)
		if err != nil {
			return nil, fmt.Errorf("row scan failed: %w", err)
		}
		delays = append(delays, d)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("row iteration failed: %w", err)
	}

	return delays, nil
}

func (s *DelaysStorage) GetDelayCountsByLine(ctx context.Context) ([]LineDelayCount, error) {
	query := `
	SELECT
	  l.id AS line_id,
	  l.line_code,
	  COUNT(d.id) AS delay_count
	FROM lines AS l
	LEFT JOIN delays AS d ON l.id = d.line_id
	GROUP BY l.id, l.line_code
	ORDER BY delay_count DESC;
	`

	rows, err := s.db.QueryContext(ctx, query)
	if err != nil {
		return nil, fmt.Errorf("query failed: %w", err)
	}
	defer rows.Close()

	var results []LineDelayCount
	for rows.Next() {
		var r LineDelayCount
		err := rows.Scan(&r.LineID, &r.LineCode, &r.DelayCount)
		if err != nil {
			return nil, fmt.Errorf("row scan failed: %w", err)
		}
		results = append(results, r)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("row iteration failed: %w", err)
	}

	return results, nil
}

func (s *DelaysStorage) GetAverageDelayForLine(ctx context.Context, lineID int64) (*LineAverageDelay, error) {
	query := `
	SELECT
	  d.line_id,
	  l.line_code,
	  AVG(d.delay_min)::NUMERIC(10,2) AS avg_delay_minutes
	FROM delays AS d
	JOIN lines AS l ON d.line_id = l.id
	WHERE d.line_id = $1
	GROUP BY d.line_id, l.line_code;
	`

	row := s.db.QueryRowContext(ctx, query, lineID)

	var result LineAverageDelay
	err := row.Scan(&result.LineID, &result.LineCode, &result.AvgDelayMins)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, fmt.Errorf("row scan failed: %w", err)
	}

	return &result, nil
}

func (s *DelaysStorage) GetOverallAverageDelay(ctx context.Context) (float64, error) {
	query := `
	SELECT
	  AVG(delay_min)::NUMERIC(10,2) AS overall_avg_delay_minutes
	FROM delays;
	`

	var avgDelay sql.NullFloat64
	err := s.db.QueryRowContext(ctx, query).Scan(&avgDelay)
	if err != nil {
		return 0, fmt.Errorf("query failed: %w", err)
	}

	if !avgDelay.Valid {
		return 0, nil
	}

	return avgDelay.Float64, nil
}
