package main

import (
	"backend/cmd/utils"
	"backend/internal/data"
	"backend/internal/env"
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
)

// @Summary		Register a new user account
// @Description	Creates a new user account in the system with the provided credentials.
// @Description	The endpoint expects a JSON payload containing username, email, and password.
// @Description	The password is securely hashed before storage, and the email must be unique
// @Description	in the system. Upon successful registration, the user can proceed to login.
// @Description	This endpoint performs validation of input data and checks for existing emails.
// @Tags			authentication
// @Accept			json
// @Produce		json
// @Param			user	body		data.RegisterUserPayload	true	"User registration details including username, email, and password"
// @Success		201		{object}	nil							"User successfully registered"
// @Router			/authentication/register [post]
func (app *app) usersResgisterUser(w http.ResponseWriter, r *http.Request) {
	/*
		{
		"Username": "testUser"
		"Email": "testEmail@email.com",
		"Password": "yoyo"
		}
	*/

	if r.Body == nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, "No user data in request body")
		return
	}

	var payload data.RegisterUserPayload
	err := json.NewDecoder(r.Body).Decode(&payload)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	ctx := r.Context()

	_, err3 := app.store.User.GetByEmail(ctx, payload.Email)

	if err3 == nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, "User with this email already exists")
		return
	}

	//log.Printf("sem tu notri")
	hashedPassword, err := HashPassword(payload.Password)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	temp := data.User{
		Username: payload.Username,
		Email:    payload.Email,
		Password: hashedPassword,
	}

	//fmt.Printf("Username: %s\nEmail: %s\nPassword: %s\n", temp.Username, temp.Email, temp.Password)
	err2 := app.store.User.Create(ctx, &temp)

	if err2 != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err2.Error())
		return
	}

	if err := utils.WriteJSONResponse(w, http.StatusCreated, nil); err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

}

// @Summary		Authenticate user and get access token
// @Description	Authenticates a user with their email and password, returning a JWT token for access.
// @Description	The endpoint validates the provided credentials against stored user data,
// @Description	ensuring the password matches the hashed version in the database.
// @Description	Upon successful authentication, returns a JWT token that should be included
// @Description	in subsequent API requests in the Authorization header.
// @Description	The token includes user identification and expiration information.
// @Tags			authentication
// @Accept			json
// @Produce		json
// @Param			credentials	body		data.LoginUserPayload	true	"User login credentials (email and password)"
// @Success		200			{object}	map[string]string		"JWT token for authenticated access"
// @Router			/authentication/login [post]
func (app *app) usersLoginUser(w http.ResponseWriter, r *http.Request) {
	/*
		{
		"Email": "testEmail@email.com",
		"Password": "yoyo"
		}
	*/

	if r.Body == nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, "No user data in request body")
		return
	}

	var payload data.LoginUserPayload
	err := json.NewDecoder(r.Body).Decode(&payload)

	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	ctx := r.Context()

	temp, err3 := app.store.User.GetByEmail(ctx, payload.Email)

	if err3 != nil {
		utils.WriteJSONError(w, http.StatusBadRequest, "User does not exist")
		return
	}

	if !ComparePasswords(temp.Password, []byte(payload.Password)) {
		utils.WriteJSONError(w, http.StatusBadRequest, "Invalid password and email combination")
		return
	}

	secret := []byte(env.GetString("JWT_SECRET", "notSoSecret-anymore"))
	token, err := CreateJWT(secret, temp.ID)
	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	utils.WriteJSONResponse(w, http.StatusOK, map[string]string{"token": token})

}

// @Summary		Update user profile
// @Description	Update user's profile information such as username and email.
// @Tags			users
// @Accept			json
// @Produce		json
// @Param			update	body		data.UpdateUserPayload	true	"User profile update payload"
// @Success		200		{object}	nil							"User successfully updated"
// @Failure		400		{object}	map[string]string			"Invalid input"
// @Router			/users/profile [put]
func (app *app) usersUpdateProfile(w http.ResponseWriter, r *http.Request) {
	if r.Body == nil {
		utils.WriteJSONError(w, http.StatusBadRequest, "No user data in request body")
		return
	}

	var payload data.UpdateUserPayload
	if err := json.NewDecoder(r.Body).Decode(&payload); err != nil {
		utils.WriteJSONError(w, http.StatusBadRequest, "Invalid input")
		return
	}

	userIDRaw := r.Context().Value("userID")
	if userIDRaw == nil {
		utils.WriteJSONError(w, http.StatusUnauthorized, "Unauthorized")
		return
	}

	userID, ok := userIDRaw.(int)
	if !ok {
		utils.WriteJSONError(w, http.StatusInternalServerError, "Invalid user ID in context")
		return
	}

	ctx := r.Context()

	existingUser, err := app.store.User.GetByEmail(ctx, payload.Email)
	if err == nil && existingUser.ID != userID {
		utils.WriteJSONError(w, http.StatusBadRequest, "Email already in use by another account")
		return
	}

	err = app.store.User.UpdateById(ctx, userID, &payload)
	if err != nil {
		utils.WriteJSONError(w, http.StatusInternalServerError, "Failed to update user")
		return
	}

	utils.WriteJSONResponse(w, http.StatusOK, nil)
}

func (app *app) getUserByID(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		utils.WriteJSONError(w, http.StatusBadRequest, "Invalid user ID")
		return
	}

	ctx := r.Context()
	user, err := app.store.User.GetByIDForClient(ctx, id)
	if err != nil {
		utils.WriteJSONError(w, http.StatusNotFound, "User not found: "+err.Error())
		return
	}

	utils.WriteJSONResponse(w, http.StatusOK, user)
}
