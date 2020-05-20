#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL

CREATE TABLE measurements (
  timestamp TIMESTAMPTZ NOT NULL,
  name TEXT NOT NULL,
  value DOUBLE PRECISION NOT NULL
);

SELECT create_hypertable('measurements', 'timestamp', migrate_data => true);

CREATE TABLE scraper_data (
  timestamp TIMESTAMPTZ NOT NULL,
  source TEXT NOT NULL,
  data JSONB NOT NULL
);

SELECT create_hypertable('scraper_data', 'timestamp');

EOSQL