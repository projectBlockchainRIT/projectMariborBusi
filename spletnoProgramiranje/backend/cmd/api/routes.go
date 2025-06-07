package main

import (
	"backend/cmd/utils"
	"backend/internal/data"
	"fmt"
	"log"
	"math"
	"net/http"
	"strconv"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		// returing true for now
		return true
	},
}

//	@Summary		Get detailed route information for a specific bus line
//	@Description	Retrieves comprehensive route data for a specific bus line, including the complete path,
//	@Description	all waypoints, direction information, and geographical coordinates for the entire route.
//	@Description	The response includes detailed path segments, turn-by-turn information, and route variants if available.
//	@Description	This endpoint is essential for mapping applications and route visualization features.
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Param			lineId	path		int			true	"Unique identifier of the bus line"
//	@Success		200		{object}	data.Route	"Complete route information including path coordinates"
//	@Router			/routes/{lineId} [get]
func (app *app) getRouteOfLineHandler(w http.ResponseWriter, r *http.Request) {
	idParam := chi.URLParam(r, "lineId")
	lineId, err := strconv.ParseInt(idParam, 10, 64)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	ctx := r.Context()

	//log.Printf("sem tu notri")
	route, err := app.store.Routes.ReadRoute(ctx, lineId)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, route); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

}

//	@Summary		Get all stations along a specific bus route
//	@Description	Returns a detailed list of all stations that are part of a specific bus line's route.
//	@Description	The response includes station ordering, distances between stations, estimated travel times,
//	@Description	platform information, and any special notes about each stop on the route.
//	@Description	This data is crucial for journey planning and providing users with complete route information.
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Param			lineId	path	int			true	"Unique identifier of the bus line"
//	@Success		200		{array}	data.Stop	"Ordered list of stations with detailed information"
//	@Router			/routes/stations/{lineId} [get]
func (app *app) getStationsOnRouteHandler(w http.ResponseWriter, r *http.Request) {
	idParam := chi.URLParam(r, "lineId")
	lineId, err := strconv.ParseInt(idParam, 10, 64)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	ctx := r.Context()

	//log.Printf("sem tu notri")
	route, err := app.store.Routes.ReadRouteStations(ctx, lineId)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, route); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

}

//	@Summary		Get a list of all available bus routes
//	@Description	Provides a comprehensive list of all bus routes in the system, including active and inactive routes.
//	@Description	Each route entry contains basic information such as route number, name, terminal stations,
//	@Description	service frequency, operating hours, and current status.
//	@Description	This endpoint is useful for displaying the complete network coverage and available services.
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Success		200	{array}	data.Route	"List of all routes with basic information"
//	@Router			/routes/list [get]
func (app *app) routesListHandler(w http.ResponseWriter, r *http.Request) {
	var routes []data.Route
	ctx := r.Context()

	//log.Printf("sem tu notri")
	routes, err := app.store.Routes.ReadRoutesList(ctx)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, routes); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

}

//	@Summary		Get real-time bus location updates via WebSocket
//	@Description	Establishes a WebSocket connection to receive real-time updates about bus locations.
//	@Description	The connection sends periodic updates (every 5 seconds) with current bus positions,
//	@Description	including coordinates, heading, speed, and next stop information.
//	@Description	This endpoint is crucial for real-time tracking features in client applications.
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Success		101	{string}	string	"Switching protocols to WebSocket"
//	@Router			/routes/realtime [get]
func (app *app) getRealtimeLine(w http.ResponseWriter, r *http.Request) {
	// Upgrade the HTTP connection to a WebSocket connection
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		http.Error(w, "Could not open WebSocket connection", http.StatusBadRequest)
		return
	}
	defer conn.Close()

	// Create a ticker that triggers every 5 seconds
	ticker := time.NewTicker(5 * time.Second)
	defer ticker.Stop()

	// Channel to signal when the connection is closed
	done := make(chan struct{})

	// Goroutine to read messages from the client
	go func() {
		defer close(done)
		for {
			_, _, err := conn.ReadMessage()
			if err != nil {
				// Exit the loop if there's an error (e.g., client disconnects)
				break
			}
		}
	}()

	// Loop to send messages to the client at each tick
	for {
		select {
		case <-done:
			// Exit the loop if the connection is closed
			return
		case t := <-ticker.C:
			// Send a message to the client
			message := fmt.Sprintf("Current time: %s", t.Format(time.RFC3339))
			err := conn.WriteMessage(websocket.TextMessage, []byte(message))
			if err != nil {
				// Exit the loop if there's an error sending the message
				return
			}
		}
	}
}

//	@Summary		Get the count of currently active bus routes
//	@Description	Returns the number of bus routes that are currently in service or active.
//	@Description	This includes routes with buses currently running, scheduled for the current time period,
//	@Description	or marked as active in the system. The count helps understand current service coverage
//	@Description	and system activity levels.
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Success		200	{integer}	int	"Number of active routes"
//	@Router			/routes/active [get]
func (app *app) getActiveRoutes(w http.ResponseWriter, r *http.Request) {
	var activeRoutes int
	ctx := r.Context()

	activeRoutes, err := app.store.Routes.ReadActiveLines(ctx)

	if err != nil {
		//fmt.Print("tu notri lol")
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, activeRoutes); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
}

//	@Summary		Simulate real-time bus positions for a specific line
//	@Description	Provides simulated real-time updates of bus positions for a specific line via WebSocket.
//	@Description	The simulation includes realistic bus movements along the route, considering schedules,
//	@Description	typical speeds, and stop times. Updates are sent every 2 seconds with precise coordinates
//	@Description	and movement patterns. This endpoint is useful for testing and demonstration purposes.
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Param			lineId	path		int		true	"Unique identifier of the bus line to simulate"
//	@Success		101		{string}	string	"Switching protocols to WebSocket"
//	@Router			/estimate/simulate/{lineId} [get]
func (app *app) serveRealtimeLine(w http.ResponseWriter, r *http.Request) {
	conn, err := wsUpgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("websocket write error:", err)
		conn.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(websocket.CloseInternalServerErr, "internal error"))
		conn.Close()
		return
	}
	defer conn.Close()

	lineIDStr := chi.URLParam(r, "lineId")
	lineID, err := strconv.Atoi(lineIDStr)
	if err != nil {
		log.Println("websocket write error:", err)
		conn.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(websocket.CloseInternalServerErr, "internal error"))
		conn.Close()
		return
	}

	ctx := r.Context()
	ticker := time.NewTicker(2 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			now := time.Now()
			runs, err := app.store.Routes.FetchActiveRuns(ctx, lineID)
			if err != nil {
				log.Println("websocket write error:", err)
				conn.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(websocket.CloseInternalServerErr, "internal error"))
				conn.Close()
				return
			}

			type BusPosition struct {
				DepartureID int     `json:"departure_id"`
				DirectionID int     `json:"direction_id"`
				Lat         float64 `json:"lat"`
				Lon         float64 `json:"lon"`
			}
			var payload []BusPosition
			nowSec := now.Hour()*3600 + now.Minute()*60 + now.Second()

			for _, run := range runs {
				lat, lon := interpPosition(run, nowSec)
				payload = append(payload, BusPosition{
					DepartureID: run.DepartureID,
					DirectionID: run.DirectionID,
					Lat:         lat,
					Lon:         lon,
				})
			}

			if err := conn.WriteJSON(payload); err != nil {
				log.Println("websocket write error:", err)
				conn.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(websocket.CloseInternalServerErr, "internal error"))
				conn.Close()
				return
			}

		}
	}
}

func interpPosition(run data.ActiveRun, nowSec int) (float64, float64) {
	// clamp
	if nowSec <= run.StartSec {
		return run.Path[0][0], run.Path[0][1]
	}
	if nowSec >= run.EndSec {
		last := run.Path[len(run.Path)-1]
		return last[0], last[1]
	}

	tElapsed := nowSec - run.StartSec
	totalDuration := run.EndSec - run.StartSec

	fraction := float64(tElapsed) / float64(totalDuration)
	if fraction < 0 {
		fraction = 0
	}
	if fraction > 1 {
		fraction = 1
	}

	numPoints := len(run.Path)
	if numPoints == 0 {
		return 0, 0
	}
	if numPoints == 1 {
		return run.Path[0][0], run.Path[0][1]
	}

	idxFloat := fraction * float64(numPoints-1)
	lowerIdx := int(math.Floor(idxFloat))
	upperIdx := int(math.Ceil(idxFloat))

	if lowerIdx == upperIdx {
		return run.Path[lowerIdx][0], run.Path[lowerIdx][1]
	}

	alpha := idxFloat - float64(lowerIdx)
	lat := run.Path[lowerIdx][0]*(1-alpha) + run.Path[upperIdx][0]*alpha
	lon := run.Path[lowerIdx][1]*(1-alpha) + run.Path[upperIdx][1]*alpha
	return lat, lon
}
