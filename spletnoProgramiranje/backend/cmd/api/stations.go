package main

import (
	"backend/cmd/utils"
	"backend/internal/data"
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
)

//	@Summary		Retrieve a comprehensive list of all bus stations
//	@Description	Returns a detailed list of all available bus stations in the system. Each station entry includes
//	@Description	its unique identifier, geographical coordinates (latitude and longitude), full name, description,
//	@Description	current status, and any associated metadata such as nearby landmarks or accessibility features.
//	@Description	This endpoint is useful for applications needing to display all available stations or create a station map.
//	@Tags			stations
//	@Accept			json
//	@Produce		json
//	@Success		200	{array}	data.Stop	"List of stations with their complete details"
//	@Router			/stations [get]
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

//	@Summary		Retrieve detailed information for a specific bus station
//	@Description	Fetches comprehensive information about a single bus station identified by its unique ID.
//	@Description	The response includes detailed station attributes such as exact location coordinates,
//	@Description	full station name, operational status, platform information, accessibility features,
//	@Description	available facilities, and real-time status updates if available.
//	@Description	This endpoint is essential for displaying detailed station information to users.
//	@Tags			stations
//	@Accept			json
//	@Produce		json
//	@Param			stationId	path		int			true	"Unique identifier of the bus station"
//	@Success		200			{object}	data.Stop	"Complete station details including location and status"
//	@Router			/stations/{stationId} [get]
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

//	@Summary		Fetch extended metadata for a specific station
//	@Description	Retrieves advanced metadata and supplementary information for a specific station.
//	@Description	This includes detailed information such as historical occupancy patterns,
//	@Description	peak hours, typical waiting times, available amenities (shelters, benches, lighting),
//	@Description	accessibility features (wheelchair access, tactile paving), nearby points of interest,
//	@Description	and any special notes about the station's operation or temporary changes.
//	@Description	This data is particularly useful for journey planning and accessibility requirements.
//	@Tags			stations
//	@Accept			json
//	@Produce		json
//	@Param			stationId	path		int					true	"Unique identifier of the station to fetch metadata for"
//	@Success		200			{object}	data.StopMetadata	"Comprehensive metadata including historical data and features"
//	@Router			/stations/{stationId}/metadata [get]
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

//	@Summary		Locate nearby bus stations based on geographical coordinates
//	@Description	Searches for and returns a list of bus stations within a specified radius of given coordinates.
//	@Description	The search uses precise geolocation calculations to find stations, considering the actual
//	@Description	walking distance where possible. Results are sorted by proximity to the provided location.
//	@Description	Each station in the response includes distance from the search point, walking time estimates,
//	@Description	and complete station details including real-time availability and accessibility information.
//	@Description	This endpoint is crucial for mobile apps and location-based services.
//	@Tags			stations
//	@Accept			json
//	@Produce		json
//	@Param			location	body	data.Location	true	"JSON object containing latitude, longitude, and search radius in meters"
//	@Success		200			{array}	data.Stop		"Array of nearby stations sorted by distance, with complete details"
//	@Router			/stations/nearby [post]
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
