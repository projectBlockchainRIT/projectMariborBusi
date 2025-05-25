package db

import (
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

	fmt.Print("connecting:")

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

	return db, nil
}
