-- ** Database generated with pgModeler (PostgreSQL Database Modeler).
-- ** pgModeler version: 1.2.0
-- ** PostgreSQL version: 17.0
-- ** Project Site: pgmodeler.io
-- ** Model Author: ---

-- ** Database creation must be performed outside a multi lined SQL file. 
-- ** These commands were put in this file only as a convenience.

-- object: new_database | type: DATABASE --
-- DROP DATABASE IF EXISTS new_database;
CREATE DATABASE "m-busi";
-- ddl-end --


SET search_path TO pg_catalog,public;
-- ddl-end --

-- object: public.users | type: TABLE --
-- DROP TABLE IF EXISTS public.users CASCADE;
CREATE TABLE public.users (
	id serial NOT NULL,
	username varchar(50) NOT NULL,
	email varchar(100) NOT NULL,
	password varchar(100) NOT NULL,
	created_at timestamp(0) with time zone DEFAULT CURRENT_TIMESTAMP,
	last_login timestamp with time zone,
	CONSTRAINT users_pk PRIMARY KEY (id),
	CONSTRAINT username_uq UNIQUE (username),
	CONSTRAINT email_uq UNIQUE (email)
);
-- ddl-end --
ALTER TABLE public.users OWNER TO postgres;
-- ddl-end --

-- object: public.stops | type: TABLE --
-- DROP TABLE IF EXISTS public.stops CASCADE;
CREATE TABLE public.stops (
	id integer NOT NULL,
	number varchar(10) NOT NULL,
	name varchar(100) NOT NULL,
	latitude decimal(9,6) NOT NULL,
	longitude decimal(9,6) NOT NULL,
	CONSTRAINT stops_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.stops OWNER TO postgres;
-- ddl-end --

-- object: public.lines | type: TABLE --
-- DROP TABLE IF EXISTS public.lines CASCADE;
CREATE TABLE public.lines (
	id serial NOT NULL,
	line_code varchar(10) NOT NULL,
	CONSTRAINT lines_pk PRIMARY KEY (id),
	CONSTRAINT code_uq UNIQUE (line_code)
);
-- ddl-end --
ALTER TABLE public.lines OWNER TO postgres;
-- ddl-end --

-- object: public.directions | type: TABLE --
-- DROP TABLE IF EXISTS public.directions CASCADE;
CREATE TABLE public.directions (
	id serial NOT NULL,
	line_id integer,
	name text NOT NULL,
	CONSTRAINT directions_pk PRIMARY KEY (id),
	CONSTRAINT unique_pair UNIQUE (line_id,name)
);
-- ddl-end --
ALTER TABLE public.directions OWNER TO postgres;
-- ddl-end --

-- object: public.departures | type: TABLE --
-- DROP TABLE IF EXISTS public.departures CASCADE;
CREATE TABLE public.departures (
	id serial NOT NULL,
	stop_id integer NOT NULL,
	direction_id integer NOT NULL,
	departure time NOT NULL,
	CONSTRAINT departures_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.departures OWNER TO postgres;
-- ddl-end --

-- object: public.routes | type: TABLE --
-- DROP TABLE IF EXISTS public.routes CASCADE;
CREATE TABLE public.routes (
	id serial NOT NULL,
	name varchar(10) NOT NULL,
	path jsonb NOT NULL,
	line_id integer,
	CONSTRAINT routes_pk PRIMARY KEY (id),
	CONSTRAINT uq_routes_name_line UNIQUE (name,line_id)
);
-- ddl-end --
ALTER TABLE public.routes OWNER TO postgres;
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


