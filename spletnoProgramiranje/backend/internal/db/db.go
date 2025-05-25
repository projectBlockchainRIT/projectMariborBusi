package db

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	_ "github.com/lib/pq"
)

// New establishes a new database connection with the given parameters.
func New(addr string, maxOpenConnections int, maxIdleConnections int, maxIdleTime string) (*sql.DB, error) {
	// Open a database connection. This doesn't actually connect to the database yet,
	// it just validates the connection string and driver.
	db, err := sql.Open("postgres", addr)
	if err != nil {
		return nil, fmt.Errorf("error opening database connection: %w", err)
	}

	// Set connection pool parameters.
	db.SetMaxOpenConns(maxOpenConnections)
	db.SetMaxIdleConns(maxIdleConnections)

	// Parse the maxIdleTime string into a time.Duration.
	duration, err := time.ParseDuration(maxIdleTime)
	if err != nil {
		db.Close() // Close the connection if duration parsing fails.
		return nil, fmt.Errorf("error parsing max idle time duration: %w", err)
	}
	db.SetConnMaxIdleTime(time.Duration(duration))

	// Ping the database to verify the connection is active.
	// Use a context with a timeout to prevent indefinite blocking.
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel() // Ensure the context is cancelled when the function exits.

	if err = db.PingContext(ctx); err != nil {
		db.Close() // Close the connection if ping fails.
		return nil, fmt.Errorf("error pinging database: %w", err)
	}

	return db, nil
}
