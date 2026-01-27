# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

M-Busi is a public transit tracking and analysis system for Maribor bus services. The system consists of:

- **Backend**: Go-based REST API with WebSocket support for real-time bus location simulation
- **Dashboard**: React + TypeScript frontend with Vite, using MapBox/Leaflet for interactive maps
- **Database**: PostgreSQL with geospatial support for bus routes, stops, schedules, and user-submitted data

## Architecture

### Backend Structure (Go)

The backend follows a layered architecture pattern:

```
backend/
├── cmd/api/           # HTTP handlers and API routes
│   ├── main.go        # Entry point with Swagger docs setup
│   ├── api.go         # Router configuration and middleware
│   ├── auth.go        # JWT authentication handlers
│   ├── stations.go    # Bus stop handlers
│   ├── routes.go      # Bus line/route handlers
│   ├── delays.go      # Delay reporting handlers
│   ├── occupancy.go   # Passenger occupancy handlers
│   └── path.go        # Optimal path finding handlers
├── internal/
│   ├── data/          # Data layer - database models and queries
│   │   └── storage.go # Storage interface defining all data operations
│   ├── db/            # Database connection management
│   └── env/           # Environment configuration
└── docs/              # Auto-generated Swagger documentation
```

**Key Architecture Patterns:**

- **Storage Interface Pattern**: `internal/data/storage.go` defines interfaces for all data operations (User, Stations, Routes, Delays, Occupancy). Each domain has its own implementation struct (e.g., `StopStorage`, `RoutesStorage`).
- **Handler Layer**: HTTP handlers in `cmd/api/` are methods on the `app` struct, which holds the storage interface and logger. Handlers parse requests, call storage methods, and return JSON responses.
- **JWT Authentication**: `WithJWTAuth` middleware wraps protected routes. User context is passed through `context.Context`.
- **Versioned API**: All routes are under `/v1` prefix for future compatibility.

### Frontend Structure (React)

```
dashboard/src/
├── App.tsx            # Route configuration with React Router
├── components/        # Reusable UI components
│   ├── layout/        # DashboardLayout, navigation
│   ├── InteractiveMap.tsx        # Main map with bus routes/stops
│   ├── DelaysController.tsx      # Delay reporting interface
│   ├── OccupancyController.tsx   # Occupancy tracking interface
│   └── ProtectedRoute.tsx        # Auth guard component
├── pages/             # Top-level page components
│   ├── LandingPage.tsx
│   ├── Login.tsx / Register.tsx
│   ├── DelaysPage.tsx
│   ├── OccupancyPage.tsx
│   ├── Graphs.tsx
│   └── SettingsPage.tsx
├── context/           # React Context providers
│   ├── UserContext.tsx           # User authentication state
│   └── ThemeContext.tsx          # Theme switching
├── types/             # TypeScript type definitions
└── utils/             # Helper functions
```

**Key Frontend Patterns:**

- **Protected Routes**: Routes under `/dashboard/*` use `<ProtectedRoute>` component to enforce authentication.
- **Context for State**: `UserContext` manages authentication state globally. `ThemeContext` handles dark/light mode.
- **Map Components**: The app uses both MapBox GL JS and Leaflet for different map views. Bus routes and stops are rendered as GeoJSON layers.

### Database Schema

Key tables (see `databaseSchema3.sql`):

- **users**: User accounts with JWT authentication
- **bus_stops**: Geolocation data for all stops (lat/long with PostGIS support)
- **bus_lines**: Bus line metadata (line number, direction)
- **routes**: Polyline geometry for each bus route (stored as PostGIS geometry)
- **schedules**: Departure times for each line at each stop
- **delays**: User-reported delays at specific stops/times
- **occupancy**: Passenger density data by line/time/date

**Geospatial Queries**: The database uses PostGIS for proximity searches (e.g., finding nearby stops) and route visualization.

## Development Commands

### Backend (Go)

```bash
cd backend

# Development
make dev                 # Start with hot reload (uses Air)
make build              # Build binary to bin/api
make run                # Run the built binary

# Testing
make test               # Run unit tests
make test-race          # Run tests with race detection
make test-coverage      # Generate HTML coverage report
make test-integration   # Run integration tests

# Code Quality
make lint               # Run golint
make vet                # Run go vet
make fmt                # Format code
make all                # Run lint, vet, test-race, and build

# Database
make db-setup           # Initialize test database

# Swagger Documentation
# After modifying API handlers with Swagger comments:
swag init -g ./main.go -d cmd/api,internal/data && swag fmt
```

**Running Backend Locally**:
The backend expects a PostgreSQL database connection string via the `DB_ADDR` environment variable. See `.envrc` for port configuration (default `:3000` for dev, `:8080` for production).

### Frontend (Dashboard)

```bash
cd dashboard

# Development
npm run dev             # Start dev server (http://localhost:5173)
npm run build           # Build for production
npm run preview         # Preview production build

# Code Quality
npm run lint            # Run ESLint
```

**Environment Variables**:
The frontend expects `VITE_BACKEND_URL` to point to the backend API (e.g., `http://localhost:8080` or `http://backend:8080` in Docker).

### Full Stack (Docker)

```bash
# From project root
docker compose up -d --build     # Start all services (database, backend, frontend)
docker compose down              # Stop all services

# Data loading (after containers are up)
./dataLoader.sh                  # Initialize database with bus data
```

**Services**:
- **database**: PostgreSQL on port 5432
- **backend**: Go API on port 8080
- **frontend**: Vite dev server on port 5173

The `dataLoader.sh` script waits for PostgreSQL to be ready, then runs `database/databaseFiller.py` to populate the database from JSON files in `sharedLibraries/`.

## Important Implementation Details

### Real-Time Bus Location Simulation

The backend provides WebSocket endpoint `/v1/estimate/simulate/{lineId}` that simulates real-time bus locations based on schedule data and current time. Implementation approach:

1. Fetch all active runs for the line at current time
2. Find nearest stop based on departure time
3. Interpolate position between stops using route geometry
4. Broadcast updates via WebSocket

### Optimal Path Finding

The `/v1/show/shortest` endpoint calculates optimal public transit routes:

1. Find 3 nearest stops to destination
2. Identify lines/directions serving those stops
3. Find 3 nearest stops to origin with matching lines
4. Return departure times and transfer options

### Authentication Flow

- **Registration**: `/v1/authentication/register` creates user with bcrypt password hash
- **Login**: `/v1/authentication/login` validates credentials and returns JWT token
- **Protected Routes**: Frontend stores JWT in context, sends with requests. Backend validates with `WithJWTAuth` middleware.

### Data Sources

- **sharedLibraries/**: Static JSON files with bus stops, routes, and schedules scraped from official sources
- **database/databaseFiller.py**: Python script that parses JSON files and populates PostgreSQL
- **JAR files** in `database/`: Java scrapers for fetching updated data from Maribor transit system

## Testing Notes

- **Backend Tests**: Located in `cmd/api/*_test.go`. Use `testify` for assertions. See `backend/TESTING.md` for details.
- **Test Database**: Integration tests should use a separate test database to avoid polluting development data.
- **CI/CD**: GitHub Actions workflow at `.github/workflows/test.yml` runs tests on push.

## Project-Specific Conventions

- **API Versioning**: All endpoints prefixed with `/v1` to allow future breaking changes without disrupting clients.
- **Error Handling**: Handlers should return structured JSON errors with appropriate HTTP status codes.
- **CORS**: Backend allows `http://localhost:5173` for local development. Update for production deployment.
- **Logging**: Backend uses `zap.SugaredLogger` for structured logging. Always include relevant context (line ID, station ID, etc.).

## Known Limitations & TODOs

- WebSocket simulation needs async update loop with mutex-protected state
- Frontend needs search functionality for Maribor locations
- Settings page is not fully implemented
- Need to add proper environment variable management for deployment
- Consider Redis caching for frequently accessed routes/schedules
