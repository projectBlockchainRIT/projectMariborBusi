-- ** Database generated with pgModeler (PostgreSQL Database Modeler).
-- ** pgModeler version: 1.2.0
-- ** PostgreSQL version: 17.0
-- ** Project Site: pgmodeler.io
-- ** Model Author: ---

-- ** Database creation must be performed outside a multi lined SQL file. 
-- ** These commands were put in this file only as a convenience.

-- object: "m-busi" | type: DATABASE --
-- DROP DATABASE IF EXISTS "m-busi";

-- object: public.users_id_seq | type: SEQUENCE --

-- ddl-end --

-- object: public.users | type: TABLE --
-- DROP TABLE IF EXISTS public.users CASCADE;
CREATE TABLE public.users (
	id serial NOT NULL,
	username character varying(50) NOT NULL,
	email character varying(100) NOT NULL,
	password character varying(100) NOT NULL,
	created_at timestamp(0) with time zone DEFAULT CURRENT_TIMESTAMP,
	last_login timestamp with time zone,
	CONSTRAINT users_pk PRIMARY KEY (id),
	CONSTRAINT username_uq UNIQUE (username),
	CONSTRAINT email_uq UNIQUE (email)
);

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


-- object: public.lines | type: TABLE --
-- DROP TABLE IF EXISTS public.lines CASCADE;
CREATE TABLE public.lines (
	id integer NOT NULL,
	line_code character varying(10) NOT NULL,
	CONSTRAINT lines_pk PRIMARY KEY (id),
	CONSTRAINT code_uq UNIQUE (line_code)
);




-- object: public.directions | type: TABLE --
-- DROP TABLE IF EXISTS public.directions CASCADE;
CREATE TABLE public.directions (
	id integer NOT NULL,
	line_id integer,
	name text NOT NULL,
	CONSTRAINT directions_pk PRIMARY KEY (id),
	CONSTRAINT unique_pair UNIQUE (line_id,name)
);



-- object: public.departures | type: TABLE --
-- DROP TABLE IF EXISTS public.departures CASCADE;
CREATE TABLE public.departures (
	id integer NOT NULL,
	stop_id integer NOT NULL,
	direction_id integer NOT NULL,
	date date NOT NULL,
	CONSTRAINT departures_pk PRIMARY KEY (id)
);

ALTER TABLE departures ADD CONSTRAINT departures_date_unique UNIQUE (date);



-- object: public.routes | type: TABLE --
-- DROP TABLE IF EXISTS public.routes CASCADE;
CREATE TABLE public.routes (
	id integer NOT NULL,
	name character varying(10) NOT NULL,
	path jsonb NOT NULL,
	line_id integer,
	CONSTRAINT routes_pk PRIMARY KEY (id),
	CONSTRAINT uq_routes_name_line UNIQUE (name,line_id)
);


-- object: public.arrivals | type: TABLE --
-- DROP TABLE IF EXISTS public.arrivals CASCADE;
CREATE TABLE public.arrivals (
	date date NOT NULL,
	departure_time time[],
	CONSTRAINT arrivals_pk PRIMARY KEY (date)
);


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


