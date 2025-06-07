package main

import (
	"backend/cmd/utils"
	"net/http"
	"strconv"
	"time"

	"github.com/go-chi/chi/v5"
)

const dateLayout = "2006-01-02"

// @Summary		Get detailed bus line occupancy throughout a specific day
// @Description	Retrieves comprehensive occupancy data for a specific bus line throughout an entire day.
// @Description	The data includes hourly breakdowns of passenger counts, occupancy percentages,
// @Description	peak and off-peak patterns, and historical comparisons where available.
// @Description	This endpoint is particularly useful for analyzing daily ridership patterns,
// @Description	planning capacity adjustments, and helping users choose less crowded travel times.
// @Description	The response includes timestamps, occupancy levels, and trend indicators.
// @Tags			occupancy
// @Accept			json
// @Produce		json
// @Param			lineId	path	int						true	"Unique identifier of the bus line"
// @Param			date	path	string					true	"Target date in YYYY-MM-DD format"
// @Success		200		{array}	data.OccupancyRecord	"Detailed hourly occupancy data for the specified day"
// @Router			/occupancy/line/{lineId}/date/{date} [get]
func (app *app) getLineOccupancyThroughDay(w http.ResponseWriter, r *http.Request) {
	idParam := chi.URLParam(r, "lineId")
	dateParam := chi.URLParam(r, "date") // format: YYYY-MM-DD

	lineID, err := strconv.Atoi(idParam)
	if err != nil {
		utils.WriteJSONError(w, http.StatusBadRequest, "invalid line ID")
		return
	}

	if _, err := time.Parse(dateLayout, dateParam); err != nil {
		utils.WriteJSONError(w, http.StatusBadRequest, "invalid date format (expected YYYY-MM-DD)")
		return
	}

	ctx := r.Context()
	records, err := app.store.Occupancy.GetOccupancyForLineByDate(ctx, lineID, dateParam)
	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, records); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
}

// @Summary		Get detailed bus line occupancy for a specific hour
// @Description	Provides granular occupancy data for a specific bus line during a particular hour of a day.
// @Description	The response includes detailed metrics such as current passenger count, capacity percentage,
// @Description	historical comparison for the same hour, typical occupancy patterns, and real-time updates if available.
// @Description	This endpoint is essential for real-time crowd management and helping users plan their immediate travel.
// @Description	The data can be used to make informed decisions about immediate travel plans and avoid overcrowded buses.
// @Tags			occupancy
// @Accept			json
// @Produce		json
// @Param			lineId	path	int						true	"Unique identifier of the bus line"
// @Param			date	path	string					true	"Target date in YYYY-MM-DD format"
// @Param			hour	path	int						true	"Hour of the day (0-23)"
// @Success		200		{array}	data.OccupancyRecord	"Detailed occupancy data for the specified hour"
// @Router			/occupancy/line/{lineId}/date/{date}/hour/{hour} [get]
func (app *app) getLineOccupancyForHour(w http.ResponseWriter, r *http.Request) {
	idParam := chi.URLParam(r, "lineId")
	dateParam := chi.URLParam(r, "date")
	hourParam := chi.URLParam(r, "hour")

	lineID, err := strconv.Atoi(idParam)
	if err != nil {
		utils.WriteJSONError(w, http.StatusBadRequest, "invalid line ID")
		return
	}

	if _, err := time.Parse(dateLayout, dateParam); err != nil {
		utils.WriteJSONError(w, http.StatusBadRequest, "invalid date format (expected YYYY-MM-DD)")
		return
	}

	hour, err := strconv.Atoi(hourParam)
	if err != nil || hour < 0 || hour > 23 {
		utils.WriteJSONError(w, http.StatusBadRequest, "invalid hour (expected 0–23)")
		return
	}

	ctx := r.Context()
	records, err := app.store.Occupancy.GetOccupancyForLineByDateAndHour(ctx, lineID, dateParam, hour)
	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, records); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
}

// @Summary		Get average occupancy across all lines for a specific hour
// @Description	Calculates and returns comprehensive average occupancy statistics across all bus lines for a specific hour.
// @Description	This aggregated data includes system-wide occupancy patterns, comparative analysis between different lines,
// @Description	identification of busiest routes, and historical trends for the specified hour.
// @Description	The endpoint is valuable for system-wide capacity planning, identifying peak travel patterns,
// @Description	and helping users understand general system busyness during specific hours.
// @Description	Results can be used for optimizing service frequency and capacity allocation.
// @Tags			occupancy
// @Accept			json
// @Produce		json
// @Param			hour	path		int						true	"Hour of the day (0-23)"
// @Success		200		{object}	data.AvgOccupancyByHour	"System-wide average occupancy statistics with detailed breakdowns"
// @Router			/occupancy/average/{hour} [get]
func (app *app) getAvgLineOccupancyForHour(w http.ResponseWriter, r *http.Request) {
	hourParam := chi.URLParam(r, "hour")

	hour, err := strconv.Atoi(hourParam)
	if err != nil || hour < 0 || hour > 23 {
		utils.WriteJSONError(w, http.StatusBadRequest, "invalid hour")
		return
	}

	ctx := r.Context()
	avg, err := app.store.Occupancy.GetAvgOccupancyAllLinesByHour(ctx, hour)
	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, avg); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
}

// @Summary		Get average daily occupancy across all lines for a specific date
// @Description	Provides detailed daily occupancy analytics across the entire bus network for a specific date.
// @Description	The response includes comprehensive metrics such as daily passenger totals, peak hours identification,
// @Description	line-by-line comparisons, unusual patterns detection, and historical trend analysis.
// @Description	This data is crucial for daily operations management, service optimization,
// @Description	and understanding system-wide usage patterns on specific dates (e.g., events, holidays).
// @Description	The information helps in both operational planning and user travel planning.
// @Tags			occupancy
// @Accept			json
// @Produce		json
// @Param			date	path		string					true	"Target date in YYYY-MM-DD format"
// @Success		200		{object}	data.AvgDailyOccupancy	"Comprehensive daily occupancy statistics with detailed analysis"
// @Router			/occupancy/average/{date} [get]
func (app *app) getAvgLineOccupancyForDate(w http.ResponseWriter, r *http.Request) {
	dateParam := chi.URLParam(r, "date")

	if _, err := time.Parse(dateLayout, dateParam); err != nil {
		utils.WriteJSONError(w, http.StatusBadRequest, "invalid date format (expected YYYY-MM-DD)")
		return
	}

	ctx := r.Context()
	avg, err := app.store.Occupancy.GetAvgDailyOccupancyAllLines(ctx, dateParam)
	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, avg); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
}
