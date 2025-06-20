package main

import (
	"backend/internal/data"
	"context"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/golang-jwt/jwt/v5"
	"github.com/stretchr/testify/assert"
)

func TestHashPassword(t *testing.T) {
	password := "testPassword123"

	hashedPassword, err := HashPassword(password)

	assert.NoError(t, err)
	assert.NotEqual(t, password, hashedPassword)
	assert.Len(t, hashedPassword, 60) // bcrypt hash length
}

func TestComparePasswords(t *testing.T) {
	password := "testPassword123"
	hashedPassword, _ := HashPassword(password)

	// Test correct password
	result := ComparePasswords(hashedPassword, []byte(password))
	assert.True(t, result)

	// Test incorrect password
	result = ComparePasswords(hashedPassword, []byte("wrongPassword"))
	assert.False(t, result)
}

func TestCreateJWT(t *testing.T) {
	userID := 123
	secret := []byte("test-secret")

	token, err := CreateJWT(secret, userID)

	assert.NoError(t, err)
	assert.NotEmpty(t, token)

	// Verify the token
	parsedToken, err := jwt.Parse(token, func(token *jwt.Token) (interface{}, error) {
		return secret, nil
	})

	assert.NoError(t, err)
	assert.True(t, parsedToken.Valid)

	claims := parsedToken.Claims.(jwt.MapClaims)
	assert.Equal(t, "123", claims["userID"])
}

func TestGetTokenFromRequest(t *testing.T) {
	// Test with valid Authorization header
	req := httptest.NewRequest("GET", "/test", nil)
	req.Header.Set("Authorization", "Bearer test-token")

	token := getTokenFromRequest(req)
	assert.Equal(t, "test-token", token)

	// Test with invalid Authorization header
	req = httptest.NewRequest("GET", "/test", nil)
	req.Header.Set("Authorization", "Invalid test-token")

	token = getTokenFromRequest(req)
	assert.Empty(t, token)

	// Test with no Authorization header
	req = httptest.NewRequest("GET", "/test", nil)

	token = getTokenFromRequest(req)
	assert.Empty(t, token)
}

func TestValidateToken(t *testing.T) {
	// Use the same secret that validateToken uses
	secret := []byte("notSoSecret-anymore") // This matches the default in validateToken
	userID := 123

	// Create a valid token
	token, _ := CreateJWT(secret, userID)

	// Test valid token
	validatedToken, err := validateToken(token)
	assert.NoError(t, err)
	assert.True(t, validatedToken.Valid)

	// Test invalid token
	validatedToken, err = validateToken("invalid-token")
	assert.Error(t, err)
	// When there's an error, validatedToken is nil, so we can't check .Valid
	assert.Nil(t, validatedToken)
}

func TestGetUserIDFromContext(t *testing.T) {
	// Test with valid user ID in context
	ctx := context.WithValue(context.Background(), UserKey, 123)

	userID := GetUserIDFromContext(ctx)
	assert.Equal(t, 123, userID)

	// Test with no user ID in context
	ctx = context.Background()

	userID = GetUserIDFromContext(ctx)
	assert.Equal(t, -1, userID)

	// Test with wrong type in context
	ctx = context.WithValue(context.Background(), UserKey, "not-an-int")

	userID = GetUserIDFromContext(ctx)
	assert.Equal(t, -1, userID)
}

func TestWithJWTAuth(t *testing.T) {
	app := setupTestApp()

	// Test with valid token
	secret := []byte("notSoSecret-anymore") // Use the same secret as validateToken
	userID := 123

	token, _ := CreateJWT(secret, userID)

	req := httptest.NewRequest("GET", "/test", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	w := httptest.NewRecorder()

	// Mock the user storage to return a valid user
	mockUsers := app.store.User.(*MockUsersStorage)
	mockUsers.GetByIdFunc = func(ctx context.Context, id int) (*data.User, error) {
		return &data.User{ID: id, Email: "test@example.com"}, nil
	}

	handlerCalled := false
	testHandler := func(w http.ResponseWriter, r *http.Request) {
		handlerCalled = true
		ctxUserID := GetUserIDFromContext(r.Context())
		assert.Equal(t, userID, ctxUserID)
		w.WriteHeader(http.StatusOK)
	}

	app.WithJWTAuth(testHandler)(w, req)

	assert.True(t, handlerCalled)
	assert.Equal(t, http.StatusOK, w.Code)
}

func TestWithJWTAuthInvalidToken(t *testing.T) {
	app := setupTestApp()

	req := httptest.NewRequest("GET", "/test", nil)
	req.Header.Set("Authorization", "Bearer invalid-token")
	w := httptest.NewRecorder()

	handlerCalled := false
	testHandler := func(w http.ResponseWriter, r *http.Request) {
		handlerCalled = true
	}

	app.WithJWTAuth(testHandler)(w, req)

	assert.False(t, handlerCalled)
	assert.Equal(t, http.StatusForbidden, w.Code)
}

func TestWithJWTAuthNoToken(t *testing.T) {
	app := setupTestApp()

	req := httptest.NewRequest("GET", "/test", nil)
	w := httptest.NewRecorder()

	handlerCalled := false
	testHandler := func(w http.ResponseWriter, r *http.Request) {
		handlerCalled = true
	}

	app.WithJWTAuth(testHandler)(w, req)

	assert.False(t, handlerCalled)
	assert.Equal(t, http.StatusForbidden, w.Code)
}

func TestWithJWTAuthUserNotFound(t *testing.T) {
	app := setupTestApp()

	secret := []byte("notSoSecret-anymore") // Use the same secret as validateToken
	userID := 123

	token, _ := CreateJWT(secret, userID)

	req := httptest.NewRequest("GET", "/test", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	w := httptest.NewRecorder()

	// Mock the user storage to return an error
	mockUsers := app.store.User.(*MockUsersStorage)
	mockUsers.GetByIdFunc = func(ctx context.Context, id int) (*data.User, error) {
		return nil, assert.AnError
	}

	handlerCalled := false
	testHandler := func(w http.ResponseWriter, r *http.Request) {
		handlerCalled = true
	}

	app.WithJWTAuth(testHandler)(w, req)

	assert.False(t, handlerCalled)
	assert.Equal(t, http.StatusForbidden, w.Code)
}
