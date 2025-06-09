#!/bin/bash
set -e

DOCKER_USERNAME="a30drian"

sudo docker compose down --rmi all -v --remove-orphans

echo "Pulling latest backend image..."
# docker pull ${DOCKER_USERNAME}/backend:latest

sudo docker compose up -d --build

until sudo docker compose exec -T database pg_isready -h localhost -p 5432 -U "user" -d "m-busi"; do
  echo "PostgreSQL is unavailable - sleeping"
  sleep 1
done
echo "PostgreSQL is up and running!"

# pip install psycopg2-binary
echo "Running Python database filler script..."
python3 database/databaseFiller.py

echo "Database initialization complete."



:'
#!/bin/bash
set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

DOCKER_USERNAME="a30drian"

sudo docker compose down --rmi all -v --remove-orphans

echo "Pulling latest backend image..."
docker pull ${DOCKER_USERNAME}/backend:latest

sudo docker compose up -d --build

until sudo docker compose exec -T database pg_isready -h localhost -p 5432 -U "user" -d "m-busi"; do
  echo "PostgreSQL is unavailable - sleeping"
  sleep 1
done
echo "PostgreSQL is up and running!"

echo "Running Python database filler script..."
python3 "$SCRIPT_DIR/database/databaseFiller.py"

echo "Database initialization complete."

'

