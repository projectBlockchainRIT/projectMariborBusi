#!/bin/bash
set -e

sudo docker compose down --rmi all -v --remove-orphans
sudo docker compose up -d --build

until pg_isready -h localhost -p 5432 -U "user" -d "password"; do
  echo "PostgreSQL is unavailable - sleeping"
  sleep 1
done
echo "PostgreSQL is up and running!"

echo "Running Python database filler script..."
python3 database/databaseFiller.py

echo "Database initialization complete."