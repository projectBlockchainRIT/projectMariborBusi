package db

import (
	"database/sql"
	"fmt"
	"log"
	"time"

	_ "github.com/lib/pq"
)

func New(addr string, maxOpenConnections int, maxIdleConnections int, maxIdleTime string) (*sql.DB, error) {
	db, err := sql.Open("postgres", addr)
	if err != nil {
		return nil, fmt.Errorf("error opening database connection: %w", err)
	}

	fmt.Print("connecting:")

	// Set connection pool parameters.
	db.SetMaxOpenConns(maxOpenConnections)
	db.SetMaxIdleConns(maxIdleConnections)

	duration, err := time.ParseDuration(maxIdleTime)
	if err != nil {
		db.Close()
		return nil, fmt.Errorf("error parsing max idle time duration: %w", err)
	}
	db.SetConnMaxIdleTime(time.Duration(duration))

	log.Println("Attempting to connect to the database...")
	const maxRetries = 20
	const retryDelay = 5 * time.Second

	for i := 0; i < maxRetries; i++ {
		err = db.Ping()
		if err == nil {
			log.Println("Successfully connected to the database!")
			return db, nil
		}

		log.Printf("Failed to ping database (attempt %d/%d): %v. Retrying in %s...", i+1, maxRetries, err, retryDelay)
		time.Sleep(retryDelay)
	}

	fmt.Printf("%v\n", db.Ping())

	return db, nil
}
