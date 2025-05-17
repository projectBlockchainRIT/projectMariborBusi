package main

import "net/http"

func (app *app) healthCheckHandler(w http.ResponseWriter, r *http.Request) {
	w.Write([]byte("Just chillin rn :)\n"))
}
