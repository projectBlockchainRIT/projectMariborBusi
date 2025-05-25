package main

import (
	"backend/cmd/utils"
	"net/http"
)

func (app *app) healthCheckHandler(w http.ResponseWriter, r *http.Request) {
	data := map[string]string{
		"status":  "ok",
		"env":     app.serverConfig.env,
		"version": version,
	}

	if err := utils.WriteJSONResponse(w, http.StatusOK, data); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
	}
}
