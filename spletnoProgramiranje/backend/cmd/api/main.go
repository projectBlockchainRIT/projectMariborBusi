package main

import (
	"backend/internal/data"
	"backend/internal/db"
	"backend/internal/env"
	"log"
)

const version = "0.0.1"

func main() {

	cfg := dbConfig{
		addr:               env.GetString("DB_ADDR", "postgres://user:password@localhost:5432/mydatabase?sslmode=disable"),
		maxOpenConnections: env.GetInt("DB_MAX_OPEN_CONNS", 30),
		maxIdleConnections: env.GetInt("DB_MAX_IDLE_CONNS", 30),
		maxIdleTime:        env.GetString("DB_MAX_IDLE_TIME", "15m"),
	}

	db, err := db.New(cfg.addr, cfg.maxOpenConnections, cfg.maxIdleConnections, cfg.maxIdleTime)

	if err != nil {
		log.Panic(err)
	}

	defer db.Close()
	log.Printf("established database connection")

	store := data.NewStorage(db)

	app := &app{
		serverConfig: config{
			address: env.GetString("ADDR", ":8080"),
			db:      cfg,
			env:     env.GetString("ENV", "development"),
		},
		store: store,
	}

	mux := app.mount()

	log.Fatal(app.run(mux))

}
