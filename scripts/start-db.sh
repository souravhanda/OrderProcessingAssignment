#!/bin/sh
set -eu

if ! docker info >/dev/null 2>&1; then
  echo "Docker is not running. Start Docker Desktop or Colima, then retry." >&2
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  docker compose up -d --wait postgres
  exit 0
fi

container_name=order-processing-db
if docker container inspect "$container_name" >/dev/null 2>&1; then
  if [ "$(docker inspect --format '{{.State.Running}}' "$container_name")" = "true" ]; then
    echo "$container_name is already running"
  else
    docker start "$container_name"
  fi
else
  docker run -d --name "$container_name" \
    -e POSTGRES_DB=orders \
    -e POSTGRES_USER=orders \
    -e POSTGRES_PASSWORD=orders \
    -p 15432:5432 \
    -v order_processing_data:/var/lib/postgresql/data \
    postgres:17-alpine
fi

attempt=0
until docker exec "$container_name" pg_isready -U orders -d orders >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 30 ]; then
    echo "PostgreSQL did not become ready within 30 seconds." >&2
    exit 1
  fi
  sleep 1
done
echo "PostgreSQL is ready on localhost:15432"
