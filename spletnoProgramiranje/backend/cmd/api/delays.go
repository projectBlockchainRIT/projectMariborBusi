package main

import (
	"backend/cmd/utils"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
)

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
