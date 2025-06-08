package main

import (
	"backend/internal/data"
	"backend/internal/db"
	"backend/internal/env"
	"log"
)

const version = "0.0.1"

//	@title			M-Busi
//	@version		1.0
//	@description	Bus simulation app
//	@termsOfService	http://swagger.io/terms/

//	@contact.name	API Support
//	@contact.url	http://www.swagger.io/support
//	@contact.email	support@swagger.io

//	@license.name	Apache 2.0
//	@license.url	http://www.apache.org/licenses/LICENSE-2.0.html

//	@BasePath	/v1

func main() {

	cfg := dbConfig{
		addr:               env.GetString("DB_ADDR", "postgresql://user:password@localhost:5432/m-busi?sslmode=disable"),
		maxOpenConnections: env.GetInt("DB_MAX_OPEN_CONNS", 30),
		maxIdleConnections: env.GetInt("DB_MAX_IDLE_CONNS", 30),
		maxIdleTime:        env.GetString("DB_MAX_IDLE_TIME", "15m"),
	}

	// fmt.Printf(cfg.addr)

	db, err := db.New(cfg.addr, cfg.maxOpenConnections, cfg.maxIdleConnections, cfg.maxIdleTime)

	if err != nil {
		log.Panic(err)
	}

	defer db.Close()
	log.Printf("established database connection")
	log.Print("Testiram za webhook 2")

	store := data.NewStorage(db)

	app := &app{
		serverConfig: config{
			address: env.GetString("ADDR", ":3000"),
			db:      cfg,
			env:     env.GetString("ENV", "development"),
			apiURL:  env.GetString("EXTERNAL_URL", "localhost:3000"),
		},
		store: store,
	}

	mux := app.mount()

	log.Fatal(app.run(mux))

}
