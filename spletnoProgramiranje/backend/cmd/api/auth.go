package main

import (
	"backend/cmd/utils"
	"backend/internal/env"
	"context"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"golang.org/x/crypto/bcrypt"
)

type contextKey string

const UserKey contextKey = "userID"

func HashPassword(password string) (string, error) {
	hash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)

	if err != nil {
		return "", err
	}

	return string(hash), nil
}

func ComparePasswords(hashedPassword string, password []byte) bool {
	err := bcrypt.CompareHashAndPassword([]byte(hashedPassword), password)

	return err == nil
}

func CreateJWT(secret []byte, userID int) (string, error) {
	expiration := time.Second * time.Duration(env.GetInt("JWT_EXP", 3600*24*7))

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"userID":    strconv.Itoa(userID),
		"expiredAt": time.Now().Add(expiration).Unix(),
	})

	tokenString, err := token.SignedString(secret)

	if err != nil {
		return "", err
	}

	return tokenString, nil

}

func (app *app) WithJWTAuth(handlerFuncion http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		// get the token from the user request
		tokenString := getTokenFromRequest(r)

		// validate the jwt
		token, err := validateToken(tokenString)
		if err != nil {
			log.Printf("failed to validate token: %v", err)
			utils.WriteJSONError(w, http.StatusForbidden, "permision denied")
		}
		// if yes, fetch the user id from the db

		if !token.Valid {
			log.Println("invalid token")
			utils.WriteJSONError(w, http.StatusForbidden, "permision denied")
			return
		}

		claims := token.Claims.(jwt.MapClaims)
		str := claims["userID"].(string)

		userID, _ := strconv.Atoi(str)

		ctx := r.Context()

		temp, err := app.store.User.GetById(ctx, userID)
		if err != nil {
			log.Printf("failed to get user by id: %v", err)
			utils.WriteJSONError(w, http.StatusForbidden, "permision denied")
			return
		}

		ctx = context.WithValue(ctx, UserKey, temp.ID)
		r = r.WithContext(ctx)

		handlerFuncion(w, r)
		// set context "userID" to userID
	}
}

func getTokenFromRequest(r *http.Request) string {
	authHeader := r.Header.Get("Authorization")
	if authHeader == "" {
		return ""
	}

	parts := strings.SplitN(authHeader, " ", 2)
	if len(parts) != 2 || strings.ToLower(parts[0]) != "bearer" {
		return ""
	}

	return parts[1]
}

func validateToken(tokenString string) (*jwt.Token, error) {
	return jwt.Parse(tokenString, func(t *jwt.Token) (interface{}, error) {
		if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexptected signing method: %v", t.Header["alg"])
		}

		return []byte(env.GetString("JWT_SECRET", "notSoSecret-anymore")), nil
	})
}

func GetUserIDFromContext(ctx context.Context) int {
	userID, ok := ctx.Value(UserKey).(int)
	if !ok {
		return -1
	}

	return userID
}
