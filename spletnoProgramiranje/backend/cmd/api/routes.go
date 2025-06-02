package main

import (
	"backend/cmd/utils"
	"backend/internal/data"
	"fmt"
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

//	@Summary		Get route of line
//	@Description	Get the route path for a specific bus line
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Param			lineId	path		int	true	"Line ID"
//	@Success		200		{object}	data.Route
//	@Failure		500		{object}	utils.ErrorResponse
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

//	@Summary		Get stations on route
//	@Description	Get all stops that appear on a specific route
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Param			lineId	path		int	true	"Line ID"
//	@Success		200		{array}		data.Stop
//	@Failure		500		{object}	utils.ErrorResponse
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

//	@Summary		Get routes list
//	@Description	Get all bus routes to display coverage on map
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Success		200	{array}		data.Route
//	@Failure		500	{object}	utils.ErrorResponse
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

//	@Summary		Get realtime line location
//	@Description	Get realtime bus locations through websocket connection
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Param			lineId	path		int		true	"Line ID"
//	@Success		101		{string}	string	"Switching to WebSocket protocol"
//	@Failure		400		{object}	utils.ErrorResponse
//	@Router			/routes/simulate/{lineId} [get]
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

//	@Summary		Get active routes
//	@Description	Get all currently active bus routes
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Success		200	{integer}	int
//	@Failure		500	{object}	utils.ErrorResponse
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
