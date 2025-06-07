package main

import (
	"backend/cmd/utils"
	"net/http"
)

// @Summary		Check API health status
// @Description	Returns the current health status of the API service, including environment information
// @Description	and version details. This endpoint is useful for monitoring service availability,
// @Description	performing health checks, and verifying deployment configurations.
// @Description	The response includes the service status, environment name, and version number.
// @Tags			system
// @Accept			json
// @Produce		json
// @Success		200	{object}	map[string]string	"Health status information including environment and version"
// @Router			/health [get]
// @Security		ApiKeyAuth
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
