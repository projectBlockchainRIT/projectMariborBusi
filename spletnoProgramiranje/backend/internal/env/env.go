package env

import (
	"os"
	"strconv"

	"github.com/joho/godotenv"
)

func GetString(key string, fallback string) string {
	err := godotenv.Load()

	if err != nil {
		// log.Fatalf(err.Error())
		return fallback
	}

	return os.Getenv(key)
}

func GetInt(key string, fallback int) int {
	val, ok := os.LookupEnv(key)
	if !ok {
		return fallback
	}

	valAsInt, err := strconv.Atoi(val)
	if err != nil {
		return fallback
	}

	return valAsInt
}
