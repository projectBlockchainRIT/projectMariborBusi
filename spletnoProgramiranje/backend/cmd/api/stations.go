package main

import (
	"backend/cmd/utils"
	"backend/internal/data"
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
)

func (app *app) stationsListHandler(w http.ResponseWriter, r *http.Request) {
	var stops []data.Stop
	ctx := r.Context()

	//log.Printf("sem tu notri")
	stops, err := app.store.Stations.ReadList(ctx)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, stops); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

}

func (app *app) getStationHandler(w http.ResponseWriter, r *http.Request) {
	idParam := chi.URLParam(r, "stationId")
	stationId, err := strconv.ParseInt(idParam, 10, 64)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	ctx := r.Context()

	//log.Printf("sem tu notri")
	stop, err := app.store.Stations.ReadStation(ctx, stationId)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, stop); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

}

func (app *app) getStationMetadataHandler(w http.ResponseWriter, r *http.Request) {
	idParam := chi.URLParam(r, "stationId")
	stationId, err := strconv.ParseInt(idParam, 10, 64)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	ctx := r.Context()

	stopMetadata, err := app.store.Stations.ReadStationMetadata(ctx, stationId)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, stopMetadata); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

}

func (app *app) getStationsCloseBy(w http.ResponseWriter, r *http.Request) {
	if r.Body == nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, "No user data in request body")
		return
	}

	var payload data.Location
	err := json.NewDecoder(r.Body).Decode(&payload)

	//fmt.Printf("Latitude: %f\nLongitude: %f\nRadius: %d\n", payload.Latitude, payload.Longitude, payload.Radius)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	ctx := r.Context()

	stops, err := app.store.Stations.ReadStationsCloseBy(ctx, &payload)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, stops); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

}
