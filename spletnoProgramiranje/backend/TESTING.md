# Testing Guide

This document provides information about the testing setup for the M-Busi backend application.

## Overview

The application uses a comprehensive testing strategy that includes:

- **Unit Tests**: Test individual functions and methods in isolation
- **Integration Tests**: Test the interaction between components
- **API Tests**: Test HTTP endpoints with mocked dependencies
- **Database Tests**: Test database operations (with test database)

## Test Structure

```
├── cmd/api/
│   ├── api_test.go      # API endpoint tests
│   └── auth_test.go     # Authentication tests
├── internal/
│   ├── db/
│   │   └── db_test.go   # Database connection tests
│   └── data/            # Data layer tests (if needed)
├── .github/workflows/
│   └── test.yml         # CI/CD pipeline
└── Makefile             # Test automation
```

## Running Tests

### Prerequisites

1. **Go 1.24.3 or later**
2. **PostgreSQL** (for database tests)
3. **Testify** (already included in go.mod)

### Basic Commands

```bash
# Run all tests
make test

# Run tests with race detection
make test-race

# Run tests with coverage report
make test-coverage

# Run integration tests
make test-integration

# Run all checks (lint, vet, test, build)
make all
```

### Individual Commands

```bash
# Run specific test file
go test -v ./cmd/api/api_test.go

# Run tests in a specific package
go test -v ./cmd/api

# Run tests with verbose output
go test -v ./...

# Run tests with coverage
go test -cover ./...

# Run tests with race detection
go test -race ./...
```
