package main

import (
	"backend/cmd/utils"
	"net/http"
	"strconv"
	"time"

	"github.com/go-chi/chi/v5"
)

const dateLayout = "2006-01-02"

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
