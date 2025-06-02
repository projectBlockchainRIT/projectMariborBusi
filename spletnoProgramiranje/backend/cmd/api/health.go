package main

import (
	"backend/cmd/utils"
	"net/http"
)

//	@Summary		Health check endpoint
//	@Description	Get the health status of the API
//	@Tags			health
//	@Accept			json
//	@Produce		json
//	@Success		200	{object}	map[string]string
//	@Router			/health [get]
//	@Security		Bearer
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
