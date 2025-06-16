package data

// APIDelay represents a delay record with API-friendly types
type APIDelay struct {
	ID          int64   `json:"id"`
	StationID   int64   `json:"station_id"`
	LineID      int64   `json:"line_id"`
	DelayTime   float64 `json:"delay_time"`
	ReportTime  string  `json:"report_time"`
	Description string  `json:"description,omitempty"`
}

// APIDelayEntry represents a delay entry with API-friendly types
type APIDelayEntry struct {
	ID          int64   `json:"id"`
	LineID      int64   `json:"line_id"`
	DelayTime   float64 `json:"delay_time"`
	ReportTime  string  `json:"report_time"`
	Description string  `json:"description,omitempty"`
}

// APIUserDelay represents a user-reported delay with API-friendly types
type APIUserDelay struct {
	ID          int64   `json:"id"`
	UserID      int64   `json:"user_id"`
	LineID      int64   `json:"line_id"`
	DelayTime   float64 `json:"delay_time"`
	ReportTime  string  `json:"report_time"`
	Description string  `json:"description,omitempty"`
}

// APIMostRecentDelay represents the most recent delay with API-friendly types
type APIMostRecentDelay struct {
	ID          int64   `json:"id"`
	LineID      int64   `json:"line_id"`
	DelayTime   float64 `json:"delay_time"`
	ReportTime  string  `json:"report_time"`
	Description string  `json:"description,omitempty"`
}

// APILineDelayCount represents delay frequency statistics by line
type APILineDelayCount struct {
	LineID    int64   `json:"line_id"`
	DelayTime float64 `json:"delay_time"`
	Count     int64   `json:"count"`
}

// APILineAverageDelay represents detailed delay statistics for a line
type APILineAverageDelay struct {
	LineID         int64   `json:"line_id"`
	AverageDelay   float64 `json:"average_delay"`
	MaxDelay       float64 `json:"max_delay"`
	MinDelay       float64 `json:"min_delay"`
	TotalIncidents int64   `json:"total_incidents"`
}
