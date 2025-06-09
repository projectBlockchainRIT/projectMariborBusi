package main

import (
	"backend/cmd/utils"
	"backend/internal/data"
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
)

// @Summary		Get all delays for a specific station
// @Description	Retrieves a comprehensive list of all recorded delays at a particular bus station.
// @Description	The response includes detailed information about each delay incident, including
// @Description	timestamp, duration, cause (if available), affected bus lines, and impact level.
// @Description	This data helps analyze station-specific performance and identify problematic locations.
// @Tags			delays
// @Accept			json
// @Produce		json
// @Param			stationId	path	int				true	"Unique identifier of the bus station"
// @Success		200			{array}	data.APIDelay	"List of delays with detailed information"
// @Router			/delays/station/{stationId} [get]
func (app *app) getDelaysForStation(w http.ResponseWriter, r *http.Request) {
	idParam := chi.URLParam(r, "stationId")

	stationId, err := strconv.ParseInt(idParam, 10, 64)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	ctx := r.Context()

	stop, err := app.store.Delays.GetDelaysByStop(ctx, stationId)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, stop); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
}

// @Summary		Get recent delays for a specific bus line
// @Description	Returns the most recent delay incidents for a particular bus line.
// @Description	The data includes detailed timing information, delay durations, locations,
// @Description	passenger impact, and any available resolution information.
// @Description	This endpoint is useful for monitoring current service status and recent performance.
// @Tags			delays
// @Accept			json
// @Produce		json
// @Param			lineId	path	int					true	"Unique identifier of the bus line"
// @Success		200		{array}	data.APIDelayEntry	"Recent delays with comprehensive details"
// @Router			/delays/recent/line/{lineId} [get]
func (app *app) getRecentDelaysForLine(w http.ResponseWriter, r *http.Request) {
	idParam := chi.URLParam(r, "lineId")

	lineId, err := strconv.ParseInt(idParam, 10, 64)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	ctx := r.Context()

	stop, err := app.store.Delays.GetRecentDelaysByLine(ctx, lineId)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, stop); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
}

// @Summary		Get all delays reported by a specific user
// @Description	Retrieves all delay reports submitted by a particular user.
// @Description	The response includes full details of each reported delay, including
// @Description	timestamp, location, affected services, and any additional notes provided.
// @Description	This endpoint helps track user contributions and verify reporting patterns.
// @Tags			delays
// @Accept			json
// @Produce		json
// @Param			userId	path	int					true	"Unique identifier of the user"
// @Success		200		{array}	data.APIUserDelay	"List of user-reported delays with details"
// @Router			/delays/user/{userId} [get]
func (app *app) getDelaysFromUser(w http.ResponseWriter, r *http.Request) {
	idParam := chi.URLParam(r, "userId")

	userId, err := strconv.ParseInt(idParam, 10, 64)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	ctx := r.Context()

	stop, err := app.store.Delays.GetDelaysByUser(ctx, userId)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, stop); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
}

// @Summary		Get most recent delays across the entire system
// @Description	Provides a list of the most recent delay incidents across all bus lines and stations.
// @Description	The response includes comprehensive details about each delay, including location,
// @Description	duration, affected services, passenger impact, and current status.
// @Description	This endpoint is crucial for real-time system monitoring and service updates.
// @Tags			delays
// @Accept			json
// @Produce		json
// @Success		200	{array}	data.APIMostRecentDelay	"List of recent system-wide delays with full details"
// @Router			/delays/recent [get]
func (app *app) getRecentOverallDelays(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	stop, err := app.store.Delays.GetMostRecentDelays(ctx)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, stop); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
}

// @Summary		Get delay frequency statistics by bus line
// @Description	Returns statistical data about delay frequencies for each bus line.
// @Description	The response includes the total number of delays per line, frequency patterns,
// @Description	common delay causes, and trend analysis where available.
// @Description	This data is valuable for identifying problematic routes and planning improvements.
// @Tags			delays
// @Accept			json
// @Produce		json
// @Success		200	{array}	data.APILineDelayCount	"Delay frequency statistics by line"
// @Router			/delays/lines/number [get]
func (app *app) getNumDelaysForLine(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	stop, err := app.store.Delays.GetDelayCountsByLine(ctx)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, stop); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
}

// @Summary		Get average delay duration for a specific line
// @Description	Calculates and returns the average delay duration for a particular bus line.
// @Description	The response includes mean delay time, standard deviation, peak delay periods,
// @Description	and historical trends. This information helps understand service reliability
// @Description	and identify patterns in service disruptions for specific routes.
// @Tags			delays
// @Accept			json
// @Produce		json
// @Param			lineId	path		int							true	"Unique identifier of the bus line"
// @Success		200		{object}	data.APILineAverageDelay	"Detailed delay statistics for the specified line"
// @Router			/delays/average/{lineId} [get]
func (app *app) getAvgDelayForLine(w http.ResponseWriter, r *http.Request) {
	idParam := chi.URLParam(r, "lineId")

	lineId, err := strconv.ParseInt(idParam, 10, 64)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
	ctx := r.Context()

	stop, err := app.store.Delays.GetAverageDelayForLine(ctx, lineId)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, stop); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
}

// @Summary		Get system-wide average delay statistics
// @Description	Provides comprehensive statistics about average delays across the entire bus network.
// @Description	The response includes system-wide mean delay time, variation by time of day,
// @Description	seasonal patterns, and comparative analysis across different service areas.
// @Description	This data is essential for overall system performance assessment and planning.
// @Tags			delays
// @Accept			json
// @Produce		json
// @Success		200	{object}	float64	"System-wide average delay in minutes"
// @Router			/delays/average [get]
func (app *app) getAvgDelay(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	stop, err := app.store.Delays.GetOverallAverageDelay(ctx)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, stop); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
}

// @Summary Submit a new delay report
// @Description Users can submit a new delay incident report for a specific bus stop and line.
// @Tags delays
// @Accept json
// @Produce json
// @Param delay body DelayReportInput true "Delay report payload"
// @Success 201 {string} string "Delay report submitted successfully"
// @Failure 400 {string} string "Invalid input"
// @Failure 500 {string} string "Internal server error"
// @Router /delays [post]
func (app *app) submitDelayReport(w http.ResponseWriter, r *http.Request) {
	var input data.DelayReportInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		utils.WriteJSONError(w, http.StatusBadRequest, "Invalid JSON body: "+err.Error())
		return
	}

	ctx := r.Context()
	user, err := app.store.User.GetByEmail(ctx, input.UserEmail)
	if err != nil {
		utils.WriteJSONError(w, http.StatusBadRequest, "User lookup failed: "+err.Error())
		return
	}

	dbInput := data.DelayReportInputUnMarshaled{
		Date:     input.Date,
		DelayMin: input.DelayMin,
		StopID:   input.StopID,
		LineID:   input.LineID,
		UserId:   user.ID,
	}

	// Step 4: Insert into DB
	if err := app.store.Delays.InsertDelay(ctx, dbInput); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, "Failed to save delay: "+err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusCreated, nil); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
}
