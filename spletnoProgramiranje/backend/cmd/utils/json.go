package utils

import (
	"encoding/json"
	"log"
	"net/http"
	"os"
	"sync"
)

var (
	errorLog *log.Logger
	once     sync.Once
)

func initLogger() {
	once.Do(func() {
		file, err := os.OpenFile("error.log", os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0666)
		if err != nil {
			log.Fatalf("Failed to open error log file: %v", err)
		}

		errorLog = log.New(file, "ERROR: ", log.Ldate|log.Ltime|log.Lshortfile)
	})
}

func WriteJSON(w http.ResponseWriter, status int, data any) error {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	return json.NewEncoder(w).Encode(data)
}

func ReadJSON(w http.ResponseWriter, r *http.Request, data any) error {

	maxBytes := 1_048_578
	r.Body = http.MaxBytesReader(w, r.Body, int64(maxBytes))

	decoder := json.NewDecoder(r.Body)
	decoder.DisallowUnknownFields()

	return decoder.Decode(data)
}

func WriteJSONError(w http.ResponseWriter, status int, message string) error {
	initLogger()

	errorLog.Printf("Status: %d, Message: %s", status, message)

	type envelope struct {
		Error string `json:"error"`
	}

	return WriteJSON(w, status, &envelope{Error: message})
}

func WriteJSONResponse(w http.ResponseWriter, status int, data any) error {
	type envelope struct {
		Data any `json:"data"`
	}

	return WriteJSON(w, status, &envelope{data})
}
