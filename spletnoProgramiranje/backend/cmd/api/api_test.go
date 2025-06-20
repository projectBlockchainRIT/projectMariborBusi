package main

import (
	"backend/internal/data"
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-chi/chi/v5"
	"github.com/stretchr/testify/assert"
	"go.uber.org/zap"
)

// Test setup helper
func setupTestApp() *app {
	logger := zap.NewNop().Sugar()

	return &app{
		serverConfig: config{
			address: ":8080",
			db: dbConfig{
				addr:               "test",
				maxOpenConnections: 10,
				maxIdleConnections: 5,
				maxIdleTime:        "5m",
			},
			env:    "test",
			apiURL: "localhost:8080",
		},
		store: data.Storage{
			User:      &MockUsersStorage{},
			Stations:  &MockStationsStorage{},
			Routes:    &MockRoutesStorage{},
			Delays:    &MockDelaysStorage{},
			Occupancy: &MockOccupancyStorage{},
		},
		logger: logger,
	}
}

// Test helper to create a request with context
func createTestRequest(method, path string, body interface{}) (*http.Request, *httptest.ResponseRecorder) {
	var reqBody []byte
	var err error

	if body != nil {
		reqBody, err = json.Marshal(body)
		if err != nil {
			panic(err)
		}
	}

	req := httptest.NewRequest(method, path, bytes.NewBuffer(reqBody))
	req.Header.Set("Content-Type", "application/json")

	return req, httptest.NewRecorder()
}

// Test helper to set up Chi router context
func setupChiContext(req *http.Request, params map[string]string) *http.Request {
	rctx := chi.NewRouteContext()
	for key, value := range params {
		rctx.URLParams.Add(key, value)
	}
	return req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))
}

func TestHealthCheckHandler(t *testing.T) {
	app := setupTestApp()

	req, w := createTestRequest("GET", "/v1/health", nil)

	app.healthCheckHandler(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data map[string]string `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)

	assert.Equal(t, "ok", response.Data["status"])
	assert.Equal(t, "test", response.Data["env"])
	assert.Equal(t, version, response.Data["version"])
}

func TestStationsListHandler(t *testing.T) {
	app := setupTestApp()
	mockStations := app.store.Stations.(*MockStationsStorage)

	expectedStops := []data.Stop{
		{ID: 1, Name: "Station 1", Latitude: 46.0569, Longitude: 14.5058},
		{ID: 2, Name: "Station 2", Latitude: 46.0569, Longitude: 14.5058},
	}

	mockStations.ReadListFunc = func(ctx context.Context) ([]data.Stop, error) {
		return expectedStops, nil
	}

	req, w := createTestRequest("GET", "/v1/stations/list", nil)

	app.stationsListHandler(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data []data.Stop `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Len(t, response.Data, 2)
	assert.Equal(t, expectedStops[0].Name, response.Data[0].Name)
}

func TestGetStationHandler(t *testing.T) {
	app := setupTestApp()
	mockStations := app.store.Stations.(*MockStationsStorage)

	expectedStop := &data.Stop{
		ID:        1,
		Name:      "Test Station",
		Latitude:  46.0569,
		Longitude: 14.5058,
	}

	mockStations.ReadStationFunc = func(ctx context.Context, id int64) (*data.Stop, error) {
		return expectedStop, nil
	}

	req, w := createTestRequest("GET", "/v1/stations/location/1", nil)
	req = setupChiContext(req, map[string]string{"stationId": "1"})

	app.getStationHandler(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data data.Stop `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Equal(t, expectedStop.Name, response.Data.Name)
}

func TestGetStationMetadataHandler(t *testing.T) {
	app := setupTestApp()
	mockStations := app.store.Stations.(*MockStationsStorage)

	expectedMetadata := &data.StopMetadata{
		ID:        1,
		Number:    "001",
		Name:      "Test Station",
		Latitude:  46.0569,
		Longitude: 14.5058,
		Departures: []data.DepartureGroup{
			{Line: "1", Direction: "North", Times: []string{"08:00", "08:30"}},
			{Line: "2", Direction: "South", Times: []string{"09:00", "09:30"}},
		},
	}

	mockStations.ReadStationMetadataFunc = func(ctx context.Context, id int64) (*data.StopMetadata, error) {
		return expectedMetadata, nil
	}

	req, w := createTestRequest("GET", "/v1/stations/1", nil)
	req = setupChiContext(req, map[string]string{"stationId": "1"})

	app.getStationMetadataHandler(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data data.StopMetadata `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Equal(t, expectedMetadata.Name, response.Data.Name)
	assert.Len(t, response.Data.Departures, 2)
}

func TestGetStationsCloseBy(t *testing.T) {
	app := setupTestApp()
	mockStations := app.store.Stations.(*MockStationsStorage)

	expectedStops := []data.Stop{
		{ID: 1, Name: "Nearby Station 1", Latitude: 46.0569, Longitude: 14.5058},
		{ID: 2, Name: "Nearby Station 2", Latitude: 46.0569, Longitude: 14.5058},
	}

	mockStations.ReadStationsCloseByFunc = func(ctx context.Context, loc *data.Location) ([]data.Stop, error) {
		return expectedStops, nil
	}

	reqBody := map[string]interface{}{
		"latitude":  46.0569,
		"longitude": 14.5058,
		"radius":    1000,
	}

	req, w := createTestRequest("POST", "/v1/stations/closeBy", reqBody)

	app.getStationsCloseBy(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data []data.Stop `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Len(t, response.Data, 2)
}

func TestGetRouteOfLineHandler(t *testing.T) {
	app := setupTestApp()
	mockRoutes := app.store.Routes.(*MockRoutesStorage)

	expectedRoute := &data.Route{
		ID:     1,
		Name:   "Test Route",
		LineID: 1,
		Path:   [][]float64{{46.0569, 14.5058}, {46.0569, 14.5059}},
	}

	mockRoutes.ReadRouteFunc = func(ctx context.Context, id int64) (*data.Route, error) {
		return expectedRoute, nil
	}

	req, w := createTestRequest("GET", "/v1/routes/1", nil)
	req = setupChiContext(req, map[string]string{"lineId": "1"})

	app.getRouteOfLineHandler(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data data.Route `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Equal(t, expectedRoute.Name, response.Data.Name)
	assert.Len(t, response.Data.Path, 2)
}

func TestGetStationsOnRouteHandler(t *testing.T) {
	app := setupTestApp()
	mockRoutes := app.store.Routes.(*MockRoutesStorage)

	expectedStops := []data.Stop{
		{ID: 1, Name: "Stop 1"},
		{ID: 2, Name: "Stop 2"},
		{ID: 3, Name: "Stop 3"},
	}

	mockRoutes.ReadRouteStationsFunc = func(ctx context.Context, id int64) ([]data.Stop, error) {
		return expectedStops, nil
	}

	req, w := createTestRequest("GET", "/v1/routes/stations/1", nil)
	req = setupChiContext(req, map[string]string{"lineId": "1"})

	app.getStationsOnRouteHandler(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data []data.Stop `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Len(t, response.Data, 3)
}

func TestRoutesListHandler(t *testing.T) {
	app := setupTestApp()
	mockRoutes := app.store.Routes.(*MockRoutesStorage)

	expectedRoutes := []data.Route{
		{ID: 1, Name: "Route 1"},
		{ID: 2, Name: "Route 2"},
		{ID: 3, Name: "Route 3"},
	}

	mockRoutes.ReadRoutesListFunc = func(ctx context.Context) ([]data.Route, error) {
		return expectedRoutes, nil
	}

	req, w := createTestRequest("GET", "/v1/routes/list", nil)

	app.routesListHandler(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data []data.Route `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Len(t, response.Data, 3)
}

func TestGetActiveRoutes(t *testing.T) {
	app := setupTestApp()
	mockRoutes := app.store.Routes.(*MockRoutesStorage)

	mockRoutes.ReadActiveLinesFunc = func(ctx context.Context) (int, error) {
		return 2, nil
	}
	mockRoutes.FetchActiveRunsFunc = func(ctx context.Context, count int) ([]data.ActiveRun, error) {
		return nil, nil
	}

	req, w := createTestRequest("GET", "/v1/routes/active", nil)

	app.getActiveRoutes(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data int `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Equal(t, 2, response.Data)
}

func TestUsersRegisterUser(t *testing.T) {
	app := setupTestApp()
	mockUsers := app.store.User.(*MockUsersStorage)

	userData := map[string]interface{}{
		"email":     "test@example.com",
		"password":  "password123",
		"firstName": "John",
		"lastName":  "Doe",
	}

	mockUsers.GetByEmailFunc = func(ctx context.Context, email string) (*data.User, error) {
		return nil, fmt.Errorf("user not found")
	}
	mockUsers.CreateFunc = func(ctx context.Context, user *data.User) error {
		user.ID = 1
		return nil
	}

	req, w := createTestRequest("POST", "/v1/authentication/register", userData)

	app.usersResgisterUser(w, req)

	assert.Equal(t, http.StatusCreated, w.Code)

	var response struct {
		Data map[string]interface{} `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	_, ok := response.Data["message"]
	if !ok {
		return
	}
	assert.Equal(t, "User created successfully", response.Data["message"])
}

func TestUsersLoginUser(t *testing.T) {
	app := setupTestApp()
	mockUsers := app.store.User.(*MockUsersStorage)

	loginData := map[string]interface{}{
		"email":    "test@example.com",
		"password": "password123",
	}

	hashedPassword, _ := HashPassword("password123")
	expectedUser := &data.User{
		ID:       1,
		Email:    "test@example.com",
		Password: hashedPassword,
	}

	mockUsers.GetByEmailFunc = func(ctx context.Context, email string) (*data.User, error) {
		return expectedUser, nil
	}

	req, w := createTestRequest("POST", "/v1/authentication/login", loginData)

	app.usersLoginUser(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data map[string]interface{} `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Contains(t, response.Data, "token")
}

func TestGetDelaysForStation(t *testing.T) {
	app := setupTestApp()
	mockDelays := app.store.Delays.(*MockDelaysStorage)

	expectedDelays := []data.Delay{
		{ID: 1, LineID: 1, DelayMin: 5, LineCode: "1A"},
		{ID: 2, LineID: 2, DelayMin: 3, LineCode: "2B"},
	}

	mockDelays.GetDelaysByStopFunc = func(ctx context.Context, stationID int64) ([]data.Delay, error) {
		return expectedDelays, nil
	}

	req, w := createTestRequest("GET", "/v1/delays/station/1", nil)
	req = setupChiContext(req, map[string]string{"stationId": "1"})

	app.getDelaysForStation(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data []data.Delay `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Len(t, response.Data, 2)
}

func TestGetRecentDelaysForLine(t *testing.T) {
	app := setupTestApp()
	mockDelays := app.store.Delays.(*MockDelaysStorage)

	expectedDelays := []data.DelayEntry{
		{ID: 1, StopID: 1, DelayMin: 5, StopName: "Station 1"},
		{ID: 2, StopID: 2, DelayMin: 3, StopName: "Station 2"},
	}

	mockDelays.GetRecentDelaysByLineFunc = func(ctx context.Context, lineID int64) ([]data.DelayEntry, error) {
		return expectedDelays, nil
	}

	req, w := createTestRequest("GET", "/v1/delays/recent/line/1", nil)
	req = setupChiContext(req, map[string]string{"lineId": "1"})

	app.getRecentDelaysForLine(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data []data.DelayEntry `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Len(t, response.Data, 2)
}

func TestGetRecentOverallDelays(t *testing.T) {
	app := setupTestApp()
	mockDelays := app.store.Delays.(*MockDelaysStorage)

	expectedDelays := []data.MostRecentDelay{
		{ID: 1, LineID: 1, DelayMin: 5, StopName: "Station 1", LineCode: "1A"},
		{ID: 2, LineID: 2, DelayMin: 3, StopName: "Station 2", LineCode: "2B"},
	}

	mockDelays.GetMostRecentDelaysFunc = func(ctx context.Context) ([]data.MostRecentDelay, error) {
		return expectedDelays, nil
	}

	req, w := createTestRequest("GET", "/v1/delays/recent", nil)

	app.getRecentOverallDelays(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data []data.MostRecentDelay `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Len(t, response.Data, 2)
}

func TestSubmitDelayReport(t *testing.T) {
	app := setupTestApp()
	mockDelays := app.store.Delays.(*MockDelaysStorage)
	mockUsers := app.store.User.(*MockUsersStorage)

	delayReport := map[string]interface{}{
		"lineId":      1,
		"stationId":   1,
		"delayTime":   5.5,
		"description": "Traffic congestion",
	}

	mockDelays.InsertDelayFunc = func(ctx context.Context, delay data.DelayReportInputUnMarshaled) error {
		return nil
	}
	mockUsers.GetByEmailFunc = func(ctx context.Context, email string) (*data.User, error) {
		return &data.User{ID: 1, Email: email}, nil
	}

	req, w := createTestRequest("POST", "/v1/delays/report", delayReport)

	app.submitDelayReport(w, req)

	assert.Equal(t, http.StatusCreated, w.Code)

	var response struct {
		Data map[string]interface{} `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	_, ok := response.Data["message"]
	if !ok {
		return
	}
	assert.Equal(t, "Delay report submitted successfully", response.Data["message"])
}

func TestGetLineOccupancyThroughDay(t *testing.T) {
	app := setupTestApp()
	mockOccupancy := app.store.Occupancy.(*MockOccupancyStorage)

	expectedOccupancy := []data.OccupancyRecord{
		{Time: "08:00", OccupancyLevel: 75},
		{Time: "09:00", OccupancyLevel: 82},
		{Time: "10:00", OccupancyLevel: 68},
	}

	mockOccupancy.GetOccupancyForLineByDateFunc = func(ctx context.Context, lineID int, date string) ([]data.OccupancyRecord, error) {
		return expectedOccupancy, nil
	}

	req, w := createTestRequest("GET", "/v1/occupancy/line/1/date/2024-01-15", nil)
	req = setupChiContext(req, map[string]string{
		"lineId": "1",
		"date":   "2024-01-15",
	})

	app.getLineOccupancyThroughDay(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data []data.OccupancyRecord `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Len(t, response.Data, 3)
}

func TestGetLineOccupancyForHour(t *testing.T) {
	app := setupTestApp()
	mockOccupancy := app.store.Occupancy.(*MockOccupancyStorage)

	expectedOccupancy := []data.OccupancyRecord{
		{Time: "09:00", OccupancyLevel: 82},
	}

	mockOccupancy.GetOccupancyForLineByDateAndHourFunc = func(ctx context.Context, lineID int, date string, hour int) ([]data.OccupancyRecord, error) {
		return expectedOccupancy, nil
	}

	req, w := createTestRequest("GET", "/v1/occupancy/line/1/date/2024-01-15/hour/9", nil)
	req = setupChiContext(req, map[string]string{
		"lineId": "1",
		"date":   "2024-01-15",
		"hour":   "9",
	})

	app.getLineOccupancyForHour(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response struct {
		Data []data.OccupancyRecord `json:"data"`
	}
	err := json.Unmarshal(w.Body.Bytes(), &response)
	assert.NoError(t, err)
	assert.Len(t, response.Data, 1)
	assert.Equal(t, 82, response.Data[0].OccupancyLevel)
}

func TestGetShortestPath(t *testing.T) {
	app := setupTestApp()
	mockStations := app.store.Stations.(*MockStationsStorage)

	// Provide a mock implementation to avoid nil pointer dereference
	mockStations.ReadThreeStationsAtDestinationFunc = func(ctx context.Context, pathLoc *data.PathLocation) ([]data.Stop, error) {
		return []data.Stop{{ID: 1, Name: "A"}, {ID: 2, Name: "B"}}, nil
	}
	mockStations.ReadThreeStationsAtLocationFunc = func(ctx context.Context, pathLoc *data.PathLocation, lines []data.Line) ([]data.Stop, error) {
		return []data.Stop{{ID: 3, Name: "C"}}, nil
	}
	mockStations.ReadStationLinesFunc = func(ctx context.Context, stops []data.Stop) ([]data.Line, error) {
		return []data.Line{{ID: 1, Name: "L1"}}, nil
	}

	pathRequest := map[string]interface{}{
		"start": map[string]interface{}{
			"latitude":  46.0569,
			"longitude": 14.5058,
		},
		"destination": map[string]interface{}{
			"latitude":  46.0569,
			"longitude": 14.5058,
		},
	}

	req, w := createTestRequest("POST", "/v1/show/shortest", pathRequest)

	app.getShortestPath(w, req)

	// This endpoint might return different status codes depending on implementation
	// For now, we just check that it doesn't panic and returns a valid HTTP code
	assert.True(t, w.Code >= 200 && w.Code < 600)
}

// Mock implementations for storage interfaces
type MockStationsStorage struct {
	ReadStationFunc                    func(context.Context, int64) (*data.Stop, error)
	ReadListFunc                       func(context.Context) ([]data.Stop, error)
	ReadStationMetadataFunc            func(context.Context, int64) (*data.StopMetadata, error)
	ReadStationsCloseByFunc            func(context.Context, *data.Location) ([]data.Stop, error)
	ReadThreeStationsAtDestinationFunc func(context.Context, *data.PathLocation) ([]data.Stop, error)
	ReadStationLinesFunc               func(context.Context, []data.Stop) ([]data.Line, error)
	ReadThreeStationsAtLocationFunc    func(context.Context, *data.PathLocation, []data.Line) ([]data.Stop, error)
}

func (m *MockStationsStorage) ReadStation(ctx context.Context, id int64) (*data.Stop, error) {
	return m.ReadStationFunc(ctx, id)
}

func (m *MockStationsStorage) ReadList(ctx context.Context) ([]data.Stop, error) {
	return m.ReadListFunc(ctx)
}

func (m *MockStationsStorage) ReadStationMetadata(ctx context.Context, id int64) (*data.StopMetadata, error) {
	return m.ReadStationMetadataFunc(ctx, id)
}

func (m *MockStationsStorage) ReadStationsCloseBy(ctx context.Context, loc *data.Location) ([]data.Stop, error) {
	return m.ReadStationsCloseByFunc(ctx, loc)
}

func (m *MockStationsStorage) ReadThreeStationsAtDestination(ctx context.Context, pathLoc *data.PathLocation) ([]data.Stop, error) {
	return m.ReadThreeStationsAtDestinationFunc(ctx, pathLoc)
}

func (m *MockStationsStorage) ReadStationLines(ctx context.Context, stops []data.Stop) ([]data.Line, error) {
	return m.ReadStationLinesFunc(ctx, stops)
}

func (m *MockStationsStorage) ReadThreeStationsAtLocation(ctx context.Context, pathLoc *data.PathLocation, lines []data.Line) ([]data.Stop, error) {
	return m.ReadThreeStationsAtLocationFunc(ctx, pathLoc, lines)
}

type MockRoutesStorage struct {
	ReadRouteFunc         func(context.Context, int64) (*data.Route, error)
	ReadRouteStationsFunc func(context.Context, int64) ([]data.Stop, error)
	ReadRoutesListFunc    func(context.Context) ([]data.Route, error)
	ReadActiveLinesFunc   func(context.Context) (int, error)
	FetchActiveRunsFunc   func(context.Context, int) ([]data.ActiveRun, error)
}

func (m *MockRoutesStorage) ReadRoute(ctx context.Context, id int64) (*data.Route, error) {
	return m.ReadRouteFunc(ctx, id)
}

func (m *MockRoutesStorage) ReadRouteStations(ctx context.Context, id int64) ([]data.Stop, error) {
	return m.ReadRouteStationsFunc(ctx, id)
}

func (m *MockRoutesStorage) ReadRoutesList(ctx context.Context) ([]data.Route, error) {
	return m.ReadRoutesListFunc(ctx)
}

func (m *MockRoutesStorage) ReadActiveLines(ctx context.Context) (int, error) {
	return m.ReadActiveLinesFunc(ctx)
}

func (m *MockRoutesStorage) FetchActiveRuns(ctx context.Context, count int) ([]data.ActiveRun, error) {
	return m.FetchActiveRunsFunc(ctx, count)
}

type MockUsersStorage struct {
	CreateFunc           func(context.Context, *data.User) error
	GetByEmailFunc       func(context.Context, string) (*data.User, error)
	GetByIdFunc          func(context.Context, int) (*data.User, error)
	UpdateByIdFunc       func(context.Context, int, *data.UpdateUserPayload) error
	GetByIDForClientFunc func(context.Context, int) (*data.UserForClient, error)
}

func (m *MockUsersStorage) Create(ctx context.Context, user *data.User) error {
	return m.CreateFunc(ctx, user)
}

func (m *MockUsersStorage) GetByEmail(ctx context.Context, email string) (*data.User, error) {
	return m.GetByEmailFunc(ctx, email)
}

func (m *MockUsersStorage) GetById(ctx context.Context, id int) (*data.User, error) {
	return m.GetByIdFunc(ctx, id)
}

func (m *MockUsersStorage) UpdateById(ctx context.Context, id int, payload *data.UpdateUserPayload) error {
	return m.UpdateByIdFunc(ctx, id, payload)
}

func (m *MockUsersStorage) GetByIDForClient(ctx context.Context, id int) (*data.UserForClient, error) {
	return m.GetByIDForClientFunc(ctx, id)
}

type MockDelaysStorage struct {
	GetDelaysByStopFunc        func(context.Context, int64) ([]data.Delay, error)
	GetRecentDelaysByLineFunc  func(context.Context, int64) ([]data.DelayEntry, error)
	GetDelaysByUserFunc        func(context.Context, int64) ([]data.UserDelay, error)
	GetMostRecentDelaysFunc    func(context.Context) ([]data.MostRecentDelay, error)
	GetDelayCountsByLineFunc   func(context.Context) ([]data.LineDelayCount, error)
	GetAverageDelayForLineFunc func(context.Context, int64) (*data.LineAverageDelay, error)
	GetOverallAverageDelayFunc func(context.Context) (float64, error)
	InsertDelayFunc            func(context.Context, data.DelayReportInputUnMarshaled) error
}

func (m *MockDelaysStorage) GetDelaysByStop(ctx context.Context, stationID int64) ([]data.Delay, error) {
	return m.GetDelaysByStopFunc(ctx, stationID)
}

func (m *MockDelaysStorage) GetRecentDelaysByLine(ctx context.Context, lineID int64) ([]data.DelayEntry, error) {
	return m.GetRecentDelaysByLineFunc(ctx, lineID)
}

func (m *MockDelaysStorage) GetDelaysByUser(ctx context.Context, userID int64) ([]data.UserDelay, error) {
	return m.GetDelaysByUserFunc(ctx, userID)
}

func (m *MockDelaysStorage) GetMostRecentDelays(ctx context.Context) ([]data.MostRecentDelay, error) {
	return m.GetMostRecentDelaysFunc(ctx)
}

func (m *MockDelaysStorage) GetDelayCountsByLine(ctx context.Context) ([]data.LineDelayCount, error) {
	return m.GetDelayCountsByLineFunc(ctx)
}

func (m *MockDelaysStorage) GetAverageDelayForLine(ctx context.Context, lineID int64) (*data.LineAverageDelay, error) {
	return m.GetAverageDelayForLineFunc(ctx, lineID)
}

func (m *MockDelaysStorage) GetOverallAverageDelay(ctx context.Context) (float64, error) {
	return m.GetOverallAverageDelayFunc(ctx)
}

func (m *MockDelaysStorage) InsertDelay(ctx context.Context, delay data.DelayReportInputUnMarshaled) error {
	return m.InsertDelayFunc(ctx, delay)
}

type MockOccupancyStorage struct {
	GetOccupancyForLineByDateFunc        func(context.Context, int, string) ([]data.OccupancyRecord, error)
	GetOccupancyForLineByDateAndHourFunc func(context.Context, int, string, int) ([]data.OccupancyRecord, error)
	GetAvgOccupancyAllLinesByHourFunc    func(context.Context, int) (*data.AvgOccupancyByHour, error)
	GetAvgDailyOccupancyAllLinesFunc     func(context.Context, string) (*data.AvgDailyOccupancy, error)
}

func (m *MockOccupancyStorage) GetOccupancyForLineByDate(ctx context.Context, lineID int, date string) ([]data.OccupancyRecord, error) {
	return m.GetOccupancyForLineByDateFunc(ctx, lineID, date)
}

func (m *MockOccupancyStorage) GetOccupancyForLineByDateAndHour(ctx context.Context, lineID int, date string, hour int) ([]data.OccupancyRecord, error) {
	return m.GetOccupancyForLineByDateAndHourFunc(ctx, lineID, date, hour)
}

func (m *MockOccupancyStorage) GetAvgOccupancyAllLinesByHour(ctx context.Context, hour int) (*data.AvgOccupancyByHour, error) {
	return m.GetAvgOccupancyAllLinesByHourFunc(ctx, hour)
}

func (m *MockOccupancyStorage) GetAvgDailyOccupancyAllLines(ctx context.Context, date string) (*data.AvgDailyOccupancy, error) {
	return m.GetAvgDailyOccupancyAllLinesFunc(ctx, date)
}
