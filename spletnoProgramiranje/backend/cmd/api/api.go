package main

import (
	"backend/internal/data"
	"fmt"
	"net/http"
	"time"

	"backend/docs"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"
	"github.com/gorilla/websocket"
	httpSwagger "github.com/swaggo/http-swagger"
)

type app struct {
	serverConfig config
	store        data.Storage
}

type config struct {
	address string
	db      dbConfig
	env     string
	apiURL  string
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
		AllowedOrigins:   []string{"http://localhost:5173"},
		AllowedHeaders:   []string{"Accept", "Content-Type"},
		AllowedMethods:   []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		ExposedHeaders:   []string{"Link"},
		AllowCredentials: true,
		MaxAge:           300,
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

		docsURL := fmt.Sprintf("%s/swagger/doc.json", app.serverConfig.address)
		r.Get("/swagger/*", httpSwagger.Handler(
			httpSwagger.URL(docsURL),
		))

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
			r.Post("/register", app.usersResgisterUser)               // creating a new user
			r.Post("/login", app.usersLoginUser)                      // logging in an existing user
			r.Put("/update", app.WithJWTAuth(app.usersUpdateProfile)) // update the user profile
			r.Get("/users/{id}", app.getUserByID)
		})

		r.Route("/delays", func(r chi.Router) {
			r.Get("/station/{stationId}", app.getDelaysForStation)     // fetch all delays for specific station
			r.Get("/recent/line/{lineId}", app.getRecentDelaysForLine) // fetch recent delays for specific line
			r.Get("/user/{userId}", app.getDelaysFromUser)             // fetch delays submitted by a user
			r.Get("/recent", app.getRecentOverallDelays)               // fetch the overall most recent delays
			r.Get("/lines/number", app.getNumDelaysForLine)            // fetch the number of delays for each line
			r.Get("/average/{lineId}", app.getAvgDelayForLine)         // fetch the average delay time for a specific line
			r.Get("/average", app.getAvgDelay)                         // fetch the average delay time overall
			r.Post("/report", app.submitDelayReport)
		})

		r.Route("/occupancy", func(r chi.Router) {
			r.Get("/line/{lineId}/date/{date}", app.getLineOccupancyThroughDay)          // fetch the occupancy of a line throughout a day
			r.Get("/line/{lineId}/date/{date}/hour/{hour}", app.getLineOccupancyForHour) // fetch the occupancy of a line on a specific date for a specific hour
			r.Get("/average/{hour}", app.getAvgLineOccupancyForHour)                     // fetch the occupancy of a line on a specific date for a specific hour
			r.Get("/average/{date}", app.getAvgLineOccupancyForDate)                     // fetch the occupancy of a line on a specific date for a specific hour
		})

		r.Route("/show", func(r chi.Router) {
			r.Post("/shortest", app.getShortestPath) // finds the most optimal path to he desired location
		})
	})

	return r
}

func (app *app) run(mux http.Handler) error {

	docs.SwaggerInfo.Host = app.serverConfig.apiURL

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
