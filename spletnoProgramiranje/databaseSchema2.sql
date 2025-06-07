-- ** Database generated with pgModeler (PostgreSQL Database Modeler).
-- ** pgModeler version: 1.2.0
-- ** PostgreSQL version: 17.0
-- ** Project Site: pgmodeler.io
-- ** Model Author: ---

-- ** Database creation must be performed outside a multi lined SQL file. 
-- ** These commands were put in this file only as a convenience.

-- object: "m-busi" | type: DATABASE --
-- DROP DATABASE IF EXISTS "m-busi";
CREATE DATABASE "m-busi"
	ENCODING = 'UTF8'
	LC_COLLATE = 'en_US.utf8'
	LC_CTYPE = 'en_US.utf8'
	TABLESPACE = pg_default
	OWNER = "user";
-- ddl-end --


-- object: topology | type: SCHEMA --
-- DROP SCHEMA IF EXISTS topology CASCADE;
CREATE SCHEMA topology;
-- ddl-end --
ALTER SCHEMA topology OWNER TO "user";
-- ddl-end --
COMMENT ON SCHEMA topology IS E'PostGIS Topology schema';
-- ddl-end --

-- object: tiger | type: SCHEMA --
-- DROP SCHEMA IF EXISTS tiger CASCADE;
CREATE SCHEMA tiger;
-- ddl-end --
ALTER SCHEMA tiger OWNER TO "user";
-- ddl-end --

-- object: tiger_data | type: SCHEMA --
-- DROP SCHEMA IF EXISTS tiger_data CASCADE;
CREATE SCHEMA tiger_data;
-- ddl-end --
ALTER SCHEMA tiger_data OWNER TO "user";
-- ddl-end --

SET search_path TO pg_catalog,public,topology,tiger,tiger_data;
-- ddl-end --

-- object: postgis | type: EXTENSION --
-- DROP EXTENSION IF EXISTS postgis CASCADE;
CREATE EXTENSION postgis
WITH SCHEMA public
VERSION '3.5.2';
-- ddl-end --
COMMENT ON EXTENSION postgis IS E'PostGIS geometry and geography spatial types and functions';
-- ddl-end --

-- object: postgis_topology | type: EXTENSION --
-- DROP EXTENSION IF EXISTS postgis_topology CASCADE;
CREATE EXTENSION postgis_topology
WITH SCHEMA topology
VERSION '3.5.2';
-- ddl-end --
COMMENT ON EXTENSION postgis_topology IS E'PostGIS topology spatial types and functions';
-- ddl-end --

-- object: fuzzystrmatch | type: EXTENSION --
-- DROP EXTENSION IF EXISTS fuzzystrmatch CASCADE;
CREATE EXTENSION fuzzystrmatch
WITH SCHEMA public
VERSION '1.2';
-- ddl-end --
COMMENT ON EXTENSION fuzzystrmatch IS E'determine similarities and distance between strings';
-- ddl-end --

-- object: postgis_tiger_geocoder | type: EXTENSION --
-- DROP EXTENSION IF EXISTS postgis_tiger_geocoder CASCADE;
CREATE EXTENSION postgis_tiger_geocoder
WITH SCHEMA tiger
VERSION '3.5.2';
-- ddl-end --
COMMENT ON EXTENSION postgis_tiger_geocoder IS E'PostGIS tiger geocoder and reverse geocoder';
-- ddl-end --

-- object: public.users_id_seq | type: SEQUENCE --
-- DROP SEQUENCE IF EXISTS public.users_id_seq CASCADE;
CREATE SEQUENCE public.users_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START WITH 1
	CACHE 1
	NO CYCLE
	OWNED BY NONE;

-- ddl-end --
ALTER SEQUENCE public.users_id_seq OWNER TO "user";
-- ddl-end --

-- object: public.users | type: TABLE --
-- DROP TABLE IF EXISTS public.users CASCADE;
CREATE TABLE public.users (
	id integer NOT NULL DEFAULT nextval('public.users_id_seq'::regclass),
	username character varying(50) NOT NULL,
	email character varying(100) NOT NULL,
	password character varying(100) NOT NULL,
	created_at timestamp(0) with time zone DEFAULT CURRENT_TIMESTAMP,
	last_login timestamp with time zone,
	CONSTRAINT users_pk PRIMARY KEY (id),
	CONSTRAINT username_uq UNIQUE (username),
	CONSTRAINT email_uq UNIQUE (email)
);
-- ddl-end --
ALTER TABLE public.users OWNER TO "user";
-- ddl-end --

-- object: public.stops | type: TABLE --
-- DROP TABLE IF EXISTS public.stops CASCADE;
CREATE TABLE public.stops (
	id integer NOT NULL,
	number character varying(10) NOT NULL,
	name character varying(100) NOT NULL,
	latitude numeric(9,6) NOT NULL,
	longitude numeric(9,6) NOT NULL,
	CONSTRAINT stops_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.stops OWNER TO "user";
-- ddl-end --

-- object: public.lines_id_seq | type: SEQUENCE --
-- DROP SEQUENCE IF EXISTS public.lines_id_seq CASCADE;
CREATE SEQUENCE public.lines_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START WITH 1
	CACHE 1
	NO CYCLE
	OWNED BY NONE;

-- ddl-end --
ALTER SEQUENCE public.lines_id_seq OWNER TO "user";
-- ddl-end --

-- object: public.lines | type: TABLE --
-- DROP TABLE IF EXISTS public.lines CASCADE;
CREATE TABLE public.lines (
	id integer NOT NULL DEFAULT nextval('public.lines_id_seq'::regclass),
	line_code character varying(10) NOT NULL,
	CONSTRAINT lines_pk PRIMARY KEY (id),
	CONSTRAINT code_uq UNIQUE (line_code)
);
-- ddl-end --
ALTER TABLE public.lines OWNER TO "user";
-- ddl-end --

-- object: public.directions_id_seq | type: SEQUENCE --
-- DROP SEQUENCE IF EXISTS public.directions_id_seq CASCADE;
CREATE SEQUENCE public.directions_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START WITH 1
	CACHE 1
	NO CYCLE
	OWNED BY NONE;

-- ddl-end --
ALTER SEQUENCE public.directions_id_seq OWNER TO "user";
-- ddl-end --

-- object: public.directions | type: TABLE --
-- DROP TABLE IF EXISTS public.directions CASCADE;
CREATE TABLE public.directions (
	id integer NOT NULL DEFAULT nextval('public.directions_id_seq'::regclass),
	line_id integer,
	name text NOT NULL,
	CONSTRAINT directions_pk PRIMARY KEY (id),
	CONSTRAINT unique_pair UNIQUE (line_id,name)
);
-- ddl-end --
ALTER TABLE public.directions OWNER TO "user";
-- ddl-end --

-- object: public.departures_id_seq | type: SEQUENCE --
-- DROP SEQUENCE IF EXISTS public.departures_id_seq CASCADE;
CREATE SEQUENCE public.departures_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START WITH 1
	CACHE 1
	NO CYCLE
	OWNED BY NONE;

-- ddl-end --
ALTER SEQUENCE public.departures_id_seq OWNER TO "user";
-- ddl-end --

-- object: public.departures | type: TABLE --
-- DROP TABLE IF EXISTS public.departures CASCADE;
CREATE TABLE public.departures (
	id integer NOT NULL DEFAULT nextval('public.departures_id_seq'::regclass),
	stop_id integer NOT NULL,
	direction_id integer NOT NULL,
	date date NOT NULL,
	CONSTRAINT departures_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.departures OWNER TO "user";
-- ddl-end --

-- object: public.routes_id_seq | type: SEQUENCE --
-- DROP SEQUENCE IF EXISTS public.routes_id_seq CASCADE;
CREATE SEQUENCE public.routes_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START WITH 1
	CACHE 1
	NO CYCLE
	OWNED BY NONE;

-- ddl-end --
ALTER SEQUENCE public.routes_id_seq OWNER TO "user";
-- ddl-end --

-- object: public.routes | type: TABLE --
-- DROP TABLE IF EXISTS public.routes CASCADE;
CREATE TABLE public.routes (
	id integer NOT NULL DEFAULT nextval('public.routes_id_seq'::regclass),
	name character varying(10) NOT NULL,
	path jsonb NOT NULL,
	line_id integer,
	CONSTRAINT routes_pk PRIMARY KEY (id),
	CONSTRAINT uq_routes_name_line UNIQUE (name,line_id)
);
-- ddl-end --
ALTER TABLE public.routes OWNER TO "user";
-- ddl-end --

-- object: public.arrivals | type: TABLE --
-- DROP TABLE IF EXISTS public.arrivals CASCADE;
CREATE TABLE public.arrivals (
	date date NOT NULL,
	departure_time time[],
	CONSTRAINT arrivals_pk PRIMARY KEY (date)
);
-- ddl-end --
ALTER TABLE public.arrivals OWNER TO postgres;
-- ddl-end --

-- object: public.delays | type: TABLE --
-- DROP TABLE IF EXISTS public.delays CASCADE;
CREATE TABLE public.delays (
	id integer NOT NULL,
	date date NOT NULL,
	delay_min integer NOT NULL,
	stop_id integer NOT NULL,
	line_id integer NOT NULL,
	user_id integer,
	CONSTRAINT delays_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.delays OWNER TO postgres;
-- ddl-end --

-- object: public.occupancy | type: TABLE --
-- DROP TABLE IF EXISTS public.occupancy CASCADE;
CREATE TABLE public.occupancy (
	id integer NOT NULL,
	line_id integer NOT NULL,
	date date NOT NULL,
	"time" time NOT NULL,
	occupancy_level integer NOT NULL,
	CONSTRAINT occupancy_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.occupancy OWNER TO postgres;
-- ddl-end --

-- object: line_id | type: CONSTRAINT --
-- ALTER TABLE public.directions DROP CONSTRAINT IF EXISTS line_id CASCADE;
ALTER TABLE public.directions ADD CONSTRAINT line_id FOREIGN KEY (line_id)
REFERENCES public.lines (id) MATCH SIMPLE
ON DELETE SET NULL ON UPDATE NO ACTION;
-- ddl-end --

-- object: stop_id | type: CONSTRAINT --
-- ALTER TABLE public.departures DROP CONSTRAINT IF EXISTS stop_id CASCADE;
ALTER TABLE public.departures ADD CONSTRAINT stop_id FOREIGN KEY (stop_id)
REFERENCES public.stops (id) MATCH SIMPLE
ON DELETE SET NULL ON UPDATE NO ACTION;
-- ddl-end --

-- object: direction_id | type: CONSTRAINT --
-- ALTER TABLE public.departures DROP CONSTRAINT IF EXISTS direction_id CASCADE;
ALTER TABLE public.departures ADD CONSTRAINT direction_id FOREIGN KEY (direction_id)
REFERENCES public.directions (id) MATCH SIMPLE
ON DELETE SET NULL ON UPDATE NO ACTION;
-- ddl-end --

-- object: line_id | type: CONSTRAINT --
-- ALTER TABLE public.routes DROP CONSTRAINT IF EXISTS line_id CASCADE;
ALTER TABLE public.routes ADD CONSTRAINT line_id FOREIGN KEY (line_id)
REFERENCES public.lines (id) MATCH SIMPLE
ON DELETE SET NULL ON UPDATE NO ACTION;
-- ddl-end --

-- object: date_departure | type: CONSTRAINT --
-- ALTER TABLE public.arrivals DROP CONSTRAINT IF EXISTS date_departure CASCADE;
ALTER TABLE public.arrivals ADD CONSTRAINT date_departure FOREIGN KEY (date)
REFERENCES public.departures (date) MATCH SIMPLE
ON DELETE NO ACTION ON UPDATE NO ACTION;
-- ddl-end --

-- object: user_id | type: CONSTRAINT --
-- ALTER TABLE public.delays DROP CONSTRAINT IF EXISTS user_id CASCADE;
ALTER TABLE public.delays ADD CONSTRAINT user_id FOREIGN KEY (user_id)
REFERENCES public.users (id) MATCH SIMPLE
ON DELETE CASCADE ON UPDATE NO ACTION;
-- ddl-end --

-- object: stop_id | type: CONSTRAINT --
-- ALTER TABLE public.delays DROP CONSTRAINT IF EXISTS stop_id CASCADE;
ALTER TABLE public.delays ADD CONSTRAINT stop_id FOREIGN KEY (stop_id)
REFERENCES public.stops (id) MATCH SIMPLE
ON DELETE CASCADE ON UPDATE NO ACTION;
-- ddl-end --

-- object: line_id | type: CONSTRAINT --
-- ALTER TABLE public.delays DROP CONSTRAINT IF EXISTS line_id CASCADE;
ALTER TABLE public.delays ADD CONSTRAINT line_id FOREIGN KEY (line_id)
REFERENCES public.lines (id) MATCH SIMPLE
ON DELETE CASCADE ON UPDATE NO ACTION;
-- ddl-end --

-- object: line_id | type: CONSTRAINT --
-- ALTER TABLE public.occupancy DROP CONSTRAINT IF EXISTS line_id CASCADE;
ALTER TABLE public.occupancy ADD CONSTRAINT line_id FOREIGN KEY (line_id)
REFERENCES public.lines (id) MATCH SIMPLE
ON DELETE RESTRICT ON UPDATE NO ACTION;
-- ddl-end --

-- object: "grant_CU_88c8db3131" | type: PERMISSION --
GRANT CREATE,USAGE
   ON SCHEMA topology
   TO "user";

-- ddl-end --


-- object: "grant_U_29b4d6553c" | type: PERMISSION --
GRANT USAGE
   ON SCHEMA topology
   TO PUBLIC;

-- ddl-end --



