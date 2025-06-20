package data

import (
	"context"
	"database/sql"
)

type Storage struct {
	User interface {
		Create(context.Context, *User) error
		GetByEmail(context.Context, string) (*User, error)
		GetById(context.Context, int) (*User, error)
		UpdateById(context.Context, int, *UpdateUserPayload) error
		GetByIDForClient(context.Context, int) (*UserForClient, error)
	}
	Stations interface {
		ReadStation(context.Context, int64) (*Stop, error)
		ReadList(context.Context) ([]Stop, error)
		ReadStationMetadata(context.Context, int64) (*StopMetadata, error)
		ReadStationsCloseBy(context.Context, *Location) ([]Stop, error)
		ReadThreeStationsAtDestination(context.Context, *PathLocation) ([]Stop, error)
		ReadStationLines(context.Context, []Stop) ([]Line, error)
		ReadThreeStationsAtLocation(context.Context, *PathLocation, []Line) ([]Stop, error)
	}
	Routes interface {
		ReadRoute(context.Context, int64) (*Route, error)
		ReadRouteStations(context.Context, int64) ([]Stop, error)
		ReadRoutesList(context.Context) ([]Route, error)
		ReadActiveLines(context.Context) (int, error)
		FetchActiveRuns(context.Context, int) ([]ActiveRun, error)
	}

	Delays interface {
		GetDelaysByStop(context.Context, int64) ([]Delay, error)
		GetRecentDelaysByLine(context.Context, int64) ([]DelayEntry, error)
		GetDelaysByUser(context.Context, int64) ([]UserDelay, error)
		GetMostRecentDelays(context.Context) ([]MostRecentDelay, error)
		GetDelayCountsByLine(context.Context) ([]LineDelayCount, error)
		GetAverageDelayForLine(context.Context, int64) (*LineAverageDelay, error)
		GetOverallAverageDelay(context.Context) (float64, error)
		InsertDelay(context.Context, DelayReportInputUnMarshaled) error
	}

	Occupancy interface {
		GetOccupancyForLineByDate(context.Context, int, string) ([]OccupancyRecord, error)
		GetOccupancyForLineByDateAndHour(context.Context, int, string, int) ([]OccupancyRecord, error)
		GetAvgOccupancyAllLinesByHour(context.Context, int) (*AvgOccupancyByHour, error)
		GetAvgDailyOccupancyAllLines(context.Context, string) (*AvgDailyOccupancy, error)
	}
}

func NewStorage(db *sql.DB) Storage {
	return Storage{
		Stations:  &StopStorage{db},
		Routes:    &RoutesStorage{db},
		User:      &UsersStorage{db},
		Delays:    &DelaysStorage{db},
		Occupancy: &OccupancyStorage{db},
	}
}
