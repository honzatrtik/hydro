#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL

CREATE TABLE measurements (
  timestamp TIMESTAMPTZ NOT NULL,
  name TEXT NOT NULL,
  value DOUBLE PRECISION NOT NULL
);

EOSQL