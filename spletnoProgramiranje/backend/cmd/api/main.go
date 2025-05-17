package main

import (
	"backend/internal/env"
	"log"
)

func main() {

	app := &app{
		serverConfig: config{
			address: env.GetString("ADDR", ":8080"),
		},
	}

	mux := app.mount()

	log.Fatal(app.run(mux))

}
