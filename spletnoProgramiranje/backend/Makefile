.PHONY: test test-race test-coverage test-integration build clean lint vet help

help:
	@echo "Available commands:"
	@echo "  test           - Run unit tests"
	@echo "  test-race      - Run tests with race detection"
	@echo "  test-coverage  - Run tests with coverage report"
	@echo "  test-integration - Run integration tests"
	@echo "  build          - Build the application"
	@echo "  clean          - Clean build artifacts"
	@echo "  lint           - Run linter"
	@echo "  vet            - Run go vet"
	@echo "  all            - Run all checks (lint, vet, test, build)"

test:
	go test -v -count=1 ./cmd/api
	@echo "✅ All tests passed successfully!"

test-race:
	go test -v -race -count=1 ./cmd/api
	@echo "✅ Race detection tests passed successfully!"

test-coverage:
	go test -v -coverprofile=coverage.out -count=1 ./cmd/api
	go tool cover -html=coverage.out -o coverage.html
	@echo "✅ Coverage report generated: coverage.html"

test-integration:
	go test -v -tags=integration ./...

build:
	go build -o bin/api ./cmd/api

clean:
	rm -rf bin/
	rm -f coverage.out coverage.html

lint:
	golint -set_exit_status ./...

vet:
	go vet ./...

test-all: test test-race test-coverage
	@echo "🎉 All test suites completed successfully!"

all: lint vet test-race build
	@echo "🎉 All checks completed successfully!"

deps:
	go mod download
	go mod tidy

dev:
	air

docker-build:
	docker build -t m-busi-backend .

docker-run:
	docker run -p 8080:8080 m-busi-backend

db-setup:
	@echo "Setting up test database..."
	@echo "Make sure PostgreSQL is running and update connection string in .env"

mocks:
	mockgen -source=internal/data/storage.go -destination=internal/data/mocks.go -package=data

bench:
	go test -bench=. -benchmem ./...

security:
	gosec ./...

fmt:
	go fmt ./...

update-deps:
	go get -u ./...
	go mod tidy 