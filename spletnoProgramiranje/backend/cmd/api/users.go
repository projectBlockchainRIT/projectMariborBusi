package main

import (
	"backend/cmd/utils"
	"backend/internal/data"
	"backend/internal/env"
	"encoding/json"
	"net/http"
)

//	@Summary		Register a new user
//	@Description	Register a new user with username, email and password
//	@Tags			authentication
//	@Accept			json
//	@Produce		json
//	@Param			user	body		data.RegisterUserPayload	true	"User registration data"
//	@Success		201		{object}	nil
//	@Failure		500		{object}	utils.ErrorResponse
//	@Router			/authentication/register [post]
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
