package main

import (
	"backend/cmd/utils"
	"backend/internal/data"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
)

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
