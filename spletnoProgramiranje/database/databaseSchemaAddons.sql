-- psql -U postgres -d m-busi -f databaseSchemaAddons.sql

-- Enable PostGIS and related extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;
CREATE EXTENSION IF NOT EXISTS postgis_raster;
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;
CREATE EXTENSION IF NOT EXISTS address_standardizer;
CREATE EXTENSION IF NOT EXISTS postgis_tiger_geocoder;

-- Add a geography column to the 'stops' table
ALTER TABLE public.stops ADD COLUMN IF NOT EXISTS geom geography(Point, 4326);

-- Populate the 'geom' column with point geometries
UPDATE public.stops
SET geom = ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4326)::geography
WHERE geom IS NULL;

-- Create a spatial index on the 'geom' column
CREATE INDEX IF NOT EXISTS stops_geom_idx ON public.stops USING GIST (geom);