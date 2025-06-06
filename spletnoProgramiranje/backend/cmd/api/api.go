package main

import (
	"backend/internal/data"
	"fmt"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"
	"github.com/gorilla/websocket"
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

var wsUpgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

func (app *app) mount() http.Handler {
	r := chi.NewRouter()

	r.Use(cors.Handler(cors.Options{
		AllowedOrigins: []string{"http://localhost:5173"},
		AllowedMethods: []string{"GET", "POST", "OPTIONS"},
		AllowedHeaders: []string{"Accept", "Content-Type"},
	}))

	r.Use(middleware.Logger)
	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(middleware.Recoverer)
	r.Use(middleware.Timeout(60 * time.Second))

	r.Group(func(ws chi.Router) {
		ws.Get("/v1/estimate/simulate/{lineId}", app.serveRealtimeLine) // simulates an estimate of current bus locations through the city
	})

	// version 1.0 group of the api routes
	// easy addition of new handlers and routes in the future without breaking the current funcionality
	r.Route("/v1", func(r chi.Router) {
		r.Get("/health", app.WithJWTAuth(app.healthCheckHandler))

		r.Route("/stations", func(r chi.Router) {
			r.Get("/list", app.stationsListHandler)               // fetch a list of basic station data for displaying a list
			r.Get("/location/{stationId}", app.getStationHandler) // fetch geolocation data of a station
			r.Get("/{stationId}", app.getStationMetadataHandler)  // fetch detailed station data, like the geolocation, depatrute times and associated bus lines
			r.Post("/closeBy", app.getStationsCloseBy)            // fetch all of the stations in a specified radius from the given location
		})

		r.Route("/routes", func(r chi.Router) {
			r.Get("/{lineId}", app.getRouteOfLineHandler)              // fetch the route of a specifc line based on the id
			r.Get("/stations/{lineId}", app.getStationsOnRouteHandler) // fetch all stops that appear on this route
			r.Get("/list", app.routesListHandler)                      // fetch all routes to display entire bus coverage on the map
			r.Get("/active", app.getActiveRoutes)                      // fetch all of the currently active routes
		})

		r.Route("/authentication", func(r chi.Router) {
			r.Post("/register", app.usersResgisterUser) // creating a new user
			r.Post("/login", app.usersLoginUser)        // logging in an existing user
		})

		r.Route("/show", func(r chi.Router) {
			r.Post("/shortest", app.getShortestPath) // finds the most optimal path to he desired location
		})
	})

	return r
}

func (app *app) run(mux http.Handler) error {

	server := &http.Server{
		Addr:         app.serverConfig.address,
		Handler:      mux,
		WriteTimeout: time.Second * 300,
		ReadTimeout:  time.Second * 300,
		IdleTimeout:  time.Minute,
	}

	fmt.Printf("Listening on port: %s\n", app.serverConfig.address)

	return server.ListenAndServe()
}
