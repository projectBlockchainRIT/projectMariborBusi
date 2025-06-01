package main

import (
	"backend/cmd/utils"
	"backend/internal/data"
	"encoding/json"
	"net/http"
)

func (app *app) getShortestPath(w http.ResponseWriter, r *http.Request) {
	if r.Body == nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, "No user data in request body")
		return
	}

	var payload data.PathLocation
	err := json.NewDecoder(r.Body).Decode(&payload)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	ctx := r.Context()

	stopsAtDestination, err := app.store.Stations.ReadThreeStationsAtDestination(ctx, &payload)

	//utils.WriteJSONResponse(w, http.StatusOK, stopsAtDestination)

	linesAtDestination, err := app.store.Stations.ReadStationLines(ctx, stopsAtDestination)

	stopsAtLocation, err := app.store.Stations.ReadThreeStationsAtLocation(ctx, &payload, linesAtDestination)

	utils.WriteJSONResponse(w, http.StatusOK, stopsAtLocation)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, linesAtDestination); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
}
