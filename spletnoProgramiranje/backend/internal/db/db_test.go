package db

import (
	"context"
	"database/sql"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestNew(t *testing.T) {
	// Test with valid connection string
	connStr := "postgresql://user:password@localhost:5432/m-busi?sslmode=disable"

	db, err := New(connStr, 10, 5, "5m")

	// This test will fail if there's no actual database connection
	// In a real CI/CD environment, you'd use a test database or mock
	if err != nil {
		t.Skip("Database connection failed, skipping test")
	}

	assert.NotNil(t, db)
	assert.NoError(t, db.Ping())

	// Test connection limits
	stats := db.Stats()
	assert.Equal(t, 10, stats.MaxOpenConnections)

	db.Close()
}

func TestDatabasePing(t *testing.T) {
	connStr := "postgresql://user:password@localhost:5432/testdb?sslmode=disable"

	db, err := New(connStr, 10, 5, "5m")
	if err != nil {
		t.Skip("Database connection failed, skipping test")
	}
	defer db.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	err = db.PingContext(ctx)
	assert.NoError(t, err)
}

func TestDatabaseQuery(t *testing.T) {
	connStr := "postgresql://user:password@localhost:5432/testdb?sslmode=disable"

	db, err := New(connStr, 10, 5, "5m")
	if err != nil {
		t.Skip("Database connection failed, skipping test")
	}
	defer db.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	// Test a simple query
	rows, err := db.QueryContext(ctx, "SELECT 1")
	if err != nil {
		t.Skip("Database query failed, skipping test")
	}
	defer rows.Close()

	assert.NoError(t, err)
	assert.True(t, rows.Next())

	var result int
	err = rows.Scan(&result)
	assert.NoError(t, err)
	assert.Equal(t, 1, result)
}

func TestDatabaseTransaction(t *testing.T) {
	connStr := "postgresql://user:password@localhost:5432/testdb?sslmode=disable"

	db, err := New(connStr, 10, 5, "5m")
	if err != nil {
		t.Skip("Database connection failed, skipping test")
	}
	defer db.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	tx, err := db.BeginTx(ctx, &sql.TxOptions{ReadOnly: true})
	if err != nil {
		t.Skip("Database transaction failed, skipping test")
	}
	defer tx.Rollback()

	assert.NotNil(t, tx)
	assert.NoError(t, tx.Commit())
}

func TestDatabaseConnectionPool(t *testing.T) {
	connStr := "postgresql://user:password@localhost:5432/testdb?sslmode=disable"

	db, err := New(connStr, 5, 2, "1m")
	if err != nil {
		t.Skip("Database connection failed, skipping test")
	}
	defer db.Close()

	stats := db.Stats()
	assert.Equal(t, 5, stats.MaxOpenConnections)

	// Test that we can use multiple connections
	for i := 0; i < 3; i++ {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		err := db.PingContext(ctx)
		cancel()
		assert.NoError(t, err)
	}
}

func TestDatabaseClose(t *testing.T) {
	connStr := "postgresql://user:password@localhost:5432/testdb?sslmode=disable"

	db, err := New(connStr, 10, 5, "5m")
	if err != nil {
		t.Skip("Database connection failed, skipping test")
	}

	// Verify connection is working
	err = db.Ping()
	require.NoError(t, err)

	// Close the database
	err = db.Close()
	assert.NoError(t, err)

	// Verify connection is closed
	err = db.Ping()
	assert.Error(t, err)
}

func TestDatabaseStats(t *testing.T) {
	connStr := "postgresql://user:password@localhost:5432/testdb?sslmode=disable"

	db, err := New(connStr, 10, 5, "5m")
	if err != nil {
		t.Skip("Database connection failed, skipping test")
	}
	defer db.Close()

	stats := db.Stats()

	// Verify stats are available
	assert.GreaterOrEqual(t, stats.OpenConnections, 0)
	assert.GreaterOrEqual(t, stats.InUse, 0)
	assert.GreaterOrEqual(t, stats.Idle, 0)
	assert.GreaterOrEqual(t, stats.WaitCount, int64(0))
	assert.GreaterOrEqual(t, stats.WaitDuration, time.Duration(0))
	assert.GreaterOrEqual(t, stats.MaxIdleClosed, int64(0))
	assert.GreaterOrEqual(t, stats.MaxLifetimeClosed, int64(0))
}
