package main

// Health Check endpoint
//	@Summary		Health check endpoint
//	@Description	Get the health status of the API
//	@Tags			health
//	@Accept			json
//	@Produce		json
//	@Success		200	{object}	map[string]string
//	@Router			/health [get]
//	@Security		Bearer

// Authentication endpoints
//	@Summary		Register a new user
//	@Description	Register a new user with username, email and password
//	@Tags			authentication
//	@Accept			json
//	@Produce		json
//	@Param			user	body		data.RegisterUserPayload	true	"User registration data"
//	@Success		201		{object}	nil
//	@Failure		500		{object}	utils.ErrorResponse
//	@Router			/authentication/register [post]

//	@Summary		Login user
//	@Description	Login with email and password to get JWT token
//	@Tags			authentication
//	@Accept			json
//	@Produce		json
//	@Param			credentials	body		data.LoginUserPayload	true	"Login credentials"
//	@Success		200			{object}	map[string]string
//	@Failure		400			{object}	utils.ErrorResponse
//	@Failure		500			{object}	utils.ErrorResponse
//	@Router			/authentication/login [post]

// Stations endpoints
//	@Summary		Get list of stations
//	@Description	Get a list of all bus stations
//	@Tags			stations
//	@Accept			json
//	@Produce		json
//	@Success		200	{array}		data.Stop
//	@Failure		500	{object}	utils.ErrorResponse
//	@Router			/stations/list [get]

//	@Summary		Get station location
//	@Description	Get geolocation data of a specific station
//	@Tags			stations
//	@Accept			json
//	@Produce		json
//	@Param			stationId	path		int	true	"Station ID"
//	@Success		200			{object}	data.Stop
//	@Failure		500			{object}	utils.ErrorResponse
//	@Router			/stations/location/{stationId} [get]

//	@Summary		Get station metadata
//	@Description	Get detailed station data including departure times and bus lines
//	@Tags			stations
//	@Accept			json
//	@Produce		json
//	@Param			stationId	path		int	true	"Station ID"
//	@Success		200			{object}	data.StopMetadata
//	@Failure		500			{object}	utils.ErrorResponse
//	@Router			/stations/{stationId} [get]

//	@Summary		Get stations nearby
//	@Description	Get all stations within a specified radius from given location
//	@Tags			stations
//	@Accept			json
//	@Produce		json
//	@Param			location	body		data.Location	true	"Location data"
//	@Success		200			{array}		data.Stop
//	@Failure		500			{object}	utils.ErrorResponse
//	@Router			/stations/closeBy [post]

// Routes endpoints
//	@Summary		Get route of line
//	@Description	Get the route path for a specific bus line
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Param			lineId	path		int	true	"Line ID"
//	@Success		200		{object}	data.Route
//	@Failure		500		{object}	utils.ErrorResponse
//	@Router			/routes/{lineId} [get]

//	@Summary		Get stations on route
//	@Description	Get all stops that appear on a specific route
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Param			lineId	path		int	true	"Line ID"
//	@Success		200		{array}		data.Stop
//	@Failure		500		{object}	utils.ErrorResponse
//	@Router			/routes/stations/{lineId} [get]

//	@Summary		Get routes list
//	@Description	Get all bus routes to display coverage on map
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Success		200	{array}		data.Route
//	@Failure		500	{object}	utils.ErrorResponse
//	@Router			/routes/list [get]

//	@Summary		Get realtime line location
//	@Description	Get realtime bus locations through websocket connection
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Param			lineId	path		int		true	"Line ID"
//	@Success		101		{string}	string	"Switching to WebSocket protocol"
//	@Failure		400		{object}	utils.ErrorResponse
//	@Router			/routes/simulate/{lineId} [get]

//	@Summary		Get active routes
//	@Description	Get all currently active bus routes
//	@Tags			routes
//	@Accept			json
//	@Produce		json
//	@Success		200	{integer}	int
//	@Failure		500	{object}	utils.ErrorResponse
//	@Router			/routes/active [get]

// Path Finding endpoints
//	@Summary		Get shortest path
//	@Description	Find the most optimal path to desired location
//	@Tags			path
//	@Accept			json
//	@Produce		json
//	@Param			location	body		data.PathLocation	true	"Source and destination locations"
//	@Success		200			{array}		data.Line
//	@Failure		500			{object}	utils.ErrorResponse
//	@Router			/show/shortest [post]
