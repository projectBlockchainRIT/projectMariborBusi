-- ** Database generated with pgModeler (PostgreSQL Database Modeler).
-- ** pgModeler version: 1.2.0
-- ** PostgreSQL version: 17.0
-- ** Project Site: pgmodeler.io
-- ** Model Author: ---

-- ** Database creation must be performed outside a multi lined SQL file. 
-- ** These commands were put in this file only as a convenience.

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
	id serial NOT NULL,
	line_code character varying(10) NOT NULL,
	CONSTRAINT lines_pk PRIMARY KEY (id),
	CONSTRAINT code_uq UNIQUE (line_code)
);



-- object: public.directions | type: TABLE --
-- DROP TABLE IF EXISTS public.directions CASCADE;
CREATE TABLE public.directions (
	id serial NOT NULL,
	line_id integer,
	name text NOT NULL,
	CONSTRAINT directions_pk PRIMARY KEY (id),
	CONSTRAINT unique_pair UNIQUE (line_id,name)
);


-- object: public.departures | type: TABLE --
-- DROP TABLE IF EXISTS public.departures CASCADE;
CREATE TABLE public.departures (
    id serial NOT NULL, -- PostgreSQL will automatically generate unique IDs
    stop_id INTEGER NOT NULL,
    direction_id INTEGER NOT NULL,
    date DATE NOT NULL,
    CONSTRAINT departures_pk PRIMARY KEY (id),
    CONSTRAINT departures_unique_run UNIQUE (stop_id, direction_id, date)
);


-- object: public.routes | type: TABLE --
-- DROP TABLE IF EXISTS public.routes CASCADE;
CREATE TABLE public.routes (
	id serial NOT NULL,
	name character varying(10) NOT NULL,
	path jsonb NOT NULL,
	line_id integer,
	CONSTRAINT routes_pk PRIMARY KEY (id),
	CONSTRAINT uq_routes_name_line UNIQUE (name,line_id)
);


-- object: public.arrivals | type: TABLE --
-- DROP TABLE IF EXISTS public.arrivals CASCADE;
CREATE TABLE public.arrivals (
    id serial NOT NULL,
    departure_time TIME[] NOT NULL,
    departures_id INTEGER NOT NULL,
    CONSTRAINT arrivals_pk PRIMARY KEY (id),
    CONSTRAINT arrivals_departures_id_unique UNIQUE (departures_id), -- Ensures only one time array per departure run
    CONSTRAINT fk_arrivals_departures FOREIGN KEY (departures_id)
        REFERENCES public.departures (id) MATCH SIMPLE
        ON DELETE CASCADE ON UPDATE NO ACTION
);

-- object: public.delays | type: TABLE --
-- DROP TABLE IF EXISTS public.delays CASCADE;
CREATE TABLE public.delays (
	id serial NOT NULL,
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
	id serial NOT NULL,
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

-- object: departure_id | type: CONSTRAINT --
-- ALTER TABLE public.arrivals DROP CONSTRAINT IF EXISTS departure_id CASCADE;
ALTER TABLE public.arrivals ADD CONSTRAINT departure_id FOREIGN KEY (departures_id)
REFERENCES public.departures (id) MATCH SIMPLE
ON DELETE CASCADE ON UPDATE NO ACTION;
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




