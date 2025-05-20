package main

import (
	"backend/internal/data"
	"fmt"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
)

type app struct {
	serverConfig config
	store        data.Storage
}

type config struct {
	address string
	db      dbConfig
	env     string
}

type dbConfig struct {
	addr               string
	maxOpenConnections int
	maxIdleConnections int
	maxIdleTime        string
}

func (app *app) mount() http.Handler {
	r := chi.NewRouter()

	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)

	r.Use(middleware.Timeout(60 * time.Second))

	// version 1.0 group of the api routes
	// easy addition of new handlers and routes in the future without breaking the current funcionality
	r.Route("/v1", func(r chi.Router) {
		r.Get("/health", app.healthCheckHandler)

		r.Route("/stations", func(r chi.Router) {
			r.Get("/list", app.stationsListHandler)               // fetch a list of basic station data for displaying a list
			r.Get("/location/{stationId}", app.getStationHandler) // fetch geolocation data of a station
			r.Get("/{stationId}", app.getStationMetadataHandler)  // fetch detailed station data, like the geolocation, depatrute times and associated bus lines
		})

		r.Route("/routes", func(r chi.Router) {
			r.Get("/{lineId}", app.getRouteOfLineHandler)              // fetch the route of a specifc line based on the id
			r.Get("/stations/{lineId}", app.getStationsOnRouteHandler) // fetch all stops that appear on this route
			r.Get("/list", app.routesListHandler)                      // fetch all routes to display entire bus coverage on the map
		})

		r.Route("/user", func(r chi.Router) {
			r.Post("/create", app.getRouteOfLineHandler)               // fetch the route of a specifc line based on the id
			r.Get("/stations/{lineId}", app.getStationsOnRouteHandler) // fetch all stops that appear on this route
			r.Get("/list", app.routesListHandler)                      // fetch all routes to display entire bus coverage on the map
		})
	})

	return r
}

func (app *app) run(mux http.Handler) error {

	server := &http.Server{
		Addr:         app.serverConfig.address,
		Handler:      mux,
		WriteTimeout: time.Second * 30,
		ReadTimeout:  time.Second * 10,
		IdleTimeout:  time.Minute,
	}

	fmt.Printf("Listening on port: %s", app.serverConfig.address)

	return server.ListenAndServe()
}
