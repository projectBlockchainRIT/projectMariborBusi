import json
import psycopg2
import re
from datetime import date

DB_PARAMS = {
    'host': 'localhost', 
    'port': 5432,         
    'database': 'm-busi',
    'user': 'user',
    'password': 'password'
}
def connectToDb():
    try:
        conn = psycopg2.connect(**DB_PARAMS)
        return conn
    except psycopg2.Error as e:
        print(f"Database connection error: {e}")
        exit(1)

def fixJsonFormat(jsonStr):
    if not jsonStr.strip().startswith('['):
        jsonStr = '[' + jsonStr
    
    if not jsonStr.strip().endswith(']'):
        jsonStr = jsonStr + ']'
    
    jsonStr = re.sub(r'}\s*{', '},{', jsonStr)
    
    return jsonStr

def loadJsonData(filePath):
    try:
        with open(filePath, 'r', encoding='utf-8') as file:
            content = file.read()
            content = fixJsonFormat(content)
            return json.loads(content)
    except (json.JSONDecodeError, FileNotFoundError) as e:
        print(f"Error loading JSON from {filePath}: {e}")
        return []

def importStops(conn, stopData):
    cursor = conn.cursor()
    success_count = 0
    
    for stop in stopData:
        try:
            cursor.execute(
                """
                INSERT INTO stops (id, number, name, latitude, longitude)
                VALUES (%s, %s, %s, %s, %s)
                ON CONFLICT (id) DO UPDATE 
                SET number = EXCLUDED.number,
                    name = EXCLUDED.name,
                    latitude = EXCLUDED.latitude,
                    longitude = EXCLUDED.longitude
                """,
                (int(stop['id']), stop['number'], stop['name'], stop['latitude'], stop['longitude'])
            )
            conn.commit()
            success_count += 1

        except KeyError as e:
            conn.rollback()
            print(f"Missing key in stop data: {e}, stop: {stop}")
        except Exception as e:
            conn.rollback()
            print(f"Error importing stop {stop.get('id', 'unknown')}: {e}")
    
    print(f"Successfully imported {success_count} out of {len(stopData)} stops")
    # print(f"Imported {len(stops_data)} stops")

def importDepartures(conn, arrivalData):
    cursor = conn.cursor()

    # Keep track of imported data counts
    lines_count = 0
    directions_count = 0
    departures_runs_count = 0  
    arrivals_count = 0  

    for date_str, stops_on_date in arrivalData[0].items():
        current_date = date.fromisoformat(date_str)

        for stopInfo in stops_on_date:
            stopId = stopInfo.get('id')
            departures = stopInfo.get('departures', [])

            if not stopId or not departures:
                continue

            for departureInfo in departures:
                lineCode = departureInfo.get('line')
                directionName = departureInfo.get('direction')
                times = departureInfo.get('times', [])

                if not lineCode or not directionName:
                    continue

                try:

                    cursor.execute(
                        """
                        INSERT INTO lines (line_code)
                        VALUES (%s)
                        ON CONFLICT (line_code) DO NOTHING
                        RETURNING id
                        """,
                        (lineCode,)
                    )
                    result = cursor.fetchone()
                    if result:
                        lineId = result[0]
                        lines_count += 1
                    else:
                        cursor.execute("SELECT id FROM lines WHERE line_code = %s", (lineCode,))
                        lineId = cursor.fetchone()[0]
                    conn.commit()

                    cursor.execute(
                        """
                        INSERT INTO directions (line_id, name)
                        VALUES (%s, %s)
                        ON CONFLICT (line_id, name) DO NOTHING
                        RETURNING id
                        """,
                        (lineId, directionName)
                    )
                    result = cursor.fetchone()
                    if result:
                        directionId = result[0]
                        directions_count += 1
                    else:
                        cursor.execute(
                            "SELECT id FROM directions WHERE line_id = %s AND name = %s",
                            (lineId, directionName)
                        )
                        directionId = cursor.fetchone()[0]
                    conn.commit()

                   
                    cursor.execute(
                        """
                        INSERT INTO departures (stop_id, direction_id, date)
                        VALUES (%s, %s, %s)
                        ON CONFLICT (stop_id, direction_id, date) DO NOTHING
                        RETURNING id
                        """,
                        (stopId, directionId, current_date)
                    )
                    result = cursor.fetchone()
                    if result:
                        departuresId = result[0]
                        departures_runs_count += 1
                    else:
                        cursor.execute(
                            "SELECT id FROM departures WHERE stop_id = %s AND direction_id = %s AND date = %s",
                            (stopId, directionId, current_date)
                        )
                        departuresId = cursor.fetchone()[0]
                    conn.commit()


                    timeArray = "{" + ",".join([f"'{t}'" for t in times]) + "}"

                    cursor.execute(
                        """
                        INSERT INTO arrivals (departure_time, departures_id)
                        VALUES (%s, %s)
                        ON CONFLICT (departures_id) DO NOTHING
                        """,
                        (timeArray, departuresId)
                    )
                    conn.commit()
                    arrivals_count += 1

                except Exception as e:
                    print(f"Error importing departure for stop {stopId}, line {lineCode} on {current_date}: {e}")
                    conn.rollback() 

    print(f"Imported {lines_count} new lines, {directions_count} new directions, {departures_runs_count} new departure runs, and {arrivals_count} new arrival time sets.")

def importRouteData(conn, routeData):

    cursor = conn.cursor()
    imported = 0

    for item in routeData:
        try:
            route_name = item['route']      
            path_list  = item.get('path', [])  
            if not path_list:
                print(f"Skipping empty path for route {route_name}")
                continue


            cursor.execute(
                "SELECT id FROM lines WHERE line_code = %s",
                (route_name,)
            )
            row = cursor.fetchone()
            if row:
                line_id = row[0]
            else:
                cursor.execute(
                    "INSERT INTO lines (line_code) VALUES (%s) RETURNING id",
                    (route_name,)
                )
                line_id = cursor.fetchone()[0]
            conn.commit() 


            cursor.execute(
                """
                INSERT INTO public.routes (name, path, line_id)
                VALUES (%s, %s::jsonb, %s)
                ON CONFLICT ON CONSTRAINT uq_routes_name_line
                  DO UPDATE SET path = EXCLUDED.path
                """,
                (route_name, json.dumps(path_list), line_id)
            )
        except KeyError as e:
            conn.rollback()
            print(f"Missing key {e} in route item: {item}")
        except Exception as e:
            conn.rollback()
            print(f"Error importing route {item.get('route', '<unknown>')}: {e}")
        else:
            conn.commit()
            imported += 1

    print(f"Imported or updated {imported} routes")

def lastAlterScript (conn):
    try:
        cursor = conn.cursor()

        cursor.execute(
            "ALTER TABLE public.stops ADD COLUMN IF NOT EXISTS geom geography(Point, 4326);"
        )

        conn.commit()

        cursor.execute(
            """
            UPDATE public.stops
            SET geom = ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4326)::geography
            WHERE geom IS NULL;
            """
        )

        conn.commit()

        cursor.execute(
            """
            CREATE INDEX IF NOT EXISTS stops_geom_idx ON public.stops USING GIST (geom);
            """
        )

        conn.commit()
    except KeyError as e:
            conn.rollback()
            print(f"Error: {e}")

def dummyDataGenerator (conn):
    try:
        cursor = conn.cursor()

        cursor.execute(
            """
            INSERT INTO public.users (username, email, password, created_at, last_login)
            SELECT
                'user_' || gs::text                                   AS username,
                'user_' || gs::text || '@example.com'                 AS email,
                'pbkdf2_sha256$260000$' || md5(random()::text) || '$' || md5(random()::text)  AS password,
                (CURRENT_TIMESTAMP - ((random() * 180)::int || ' days')::interval)   AS created_at,
                (CURRENT_TIMESTAMP - ((random() * 30)::int || ' days')::interval)    AS last_login
            FROM generate_series(1, 200) AS gs;
            """
        )

        conn.commit()

        print("Added 200 random user data")

        cursor.execute(
            """
            INSERT INTO public.occupancy (line_id, date, "time", occupancy_level)
            WITH
                all_lines AS (
                    SELECT unnest(ARRAY[
                        1, 2, 3, 5, 6, 9, 10, 12, 14, 15, 17, 19, 20, 21, 23, 29, 52, 112, 273
                    ]) AS line_id
                ),
                popular_lines AS (
                    SELECT unnest(ARRAY[6, 19, 20, 21, 23, 52, 112]) AS line_id
                ),
                observation_dates AS (
                    SELECT generate_series('2025-05-01'::date, '2025-06-30'::date, '1 day'::interval) AS obs_date
                ),
                observation_times AS (
                    SELECT unnest(ARRAY[
                        '06:00:00'::time,
                        '08:00:00'::time,
                        '10:00:00'::time,
                        '12:00:00'::time,
                        '16:00:00'::time,
                        '18:00:00'::time
                    ]) AS obs_time
                )
            SELECT
                l.line_id,
                d.obs_date                              AS date,
                t.obs_time                              AS "time",
                CASE
                    WHEN EXTRACT(ISODOW FROM d.obs_date) BETWEEN 1 AND 5
                        AND t.obs_time BETWEEN '07:00:00'::time AND '09:00:00'::time
                        AND l.line_id IN (SELECT line_id FROM popular_lines)
                        THEN (4 + floor(random() * 2))::int
                    WHEN EXTRACT(ISODOW FROM d.obs_date) BETWEEN 1 AND 5
                        AND t.obs_time BETWEEN '07:00:00'::time AND '09:00:00'::time
                        AND l.line_id NOT IN (SELECT line_id FROM popular_lines)
                        THEN (2 + floor(random() * 3))::int
                    WHEN EXTRACT(ISODOW FROM d.obs_date) BETWEEN 1 AND 5
                        AND t.obs_time BETWEEN '16:00:00'::time AND '18:00:00'::time
                        AND l.line_id IN (SELECT line_id FROM popular_lines)
                        THEN (4 + floor(random() * 2))::int
                    WHEN EXTRACT(ISODOW FROM d.obs_date) BETWEEN 1 AND 5
                        AND t.obs_time BETWEEN '16:00:00'::time AND '18:00:00'::time
                        AND l.line_id NOT IN (SELECT line_id FROM popular_lines)
                        THEN (2 + floor(random() * 3))::int
                    WHEN EXTRACT(ISODOW FROM d.obs_date) BETWEEN 1 AND 5
                        AND t.obs_time IN ('10:00:00'::time, '12:00:00'::time)
                        AND l.line_id IN (SELECT line_id FROM popular_lines)
                        THEN (3 + floor(random() * 2))::int
                    WHEN EXTRACT(ISODOW FROM d.obs_date) BETWEEN 1 AND 5
                        AND t.obs_time IN ('10:00:00'::time, '12:00:00'::time)
                        AND l.line_id NOT IN (SELECT line_id FROM popular_lines)
                        THEN (2 + floor(random() * 3))::int
                    WHEN t.obs_time = '06:00:00'::time
                        THEN (1 + floor(random() * 3))::int
                    WHEN EXTRACT(ISODOW FROM d.obs_date) IN (6, 7)
                        THEN (1 + floor(random() * 2))::int
                    ELSE (1 + floor(random() * 5))::int
                END AS occupancy_level
            FROM
                all_lines l
                CROSS JOIN observation_dates d
                CROSS JOIN observation_times t
            ORDER BY
                d.obs_date,
                l.line_id,
                t.obs_time
            ;
            """
        )

        conn.commit()
        

        print("Added random occupancy data")

        cursor.execute(
            """
            DROP TABLE IF EXISTS temp_all_stops;
            """
        )

        conn.commit()

        print("Dropped table if it existed")

        cursor.execute(
            """
            CREATE TEMP TABLE temp_all_stops (stop_id integer);

            INSERT INTO temp_all_stops (stop_id)
            VALUES 
                (192),(210),(424),(423),(199),(200),(358),(359),(84),(85),(130),(131),(133),(134),(88),(89),(92),(93),(94),(95),(90),(91),(393),(394),(237),(357),(144),(303),(143),(395),(396),(105),(106),(111),(112),(113),(114),(31),(32),(29),(30),(316),(315),(313),(312),(110),(107),(108),(194),(195),(35),(36),(21),(22),(23),(24),(19),(20),(18),(79),(349),(350),(1),(122),(123),(309),(310),(311),(337),(338),(346),(215),(135),(138),(137),(136),(214),(341),(339),(340),(201),(202),(230),(198),(56),(52),(50),(397),(54),(280),(51),(53),(55),(163),(164),(166),(167),(331),(330),(161),(162),(154),(196),(165),(157),(159),(160),(248),(249),(74),(75),(77),(335),(336),(334),(126),(153),(152),(253),(254),(255),(256),(398),(150),(151),(250),(17),(155),(156),(422),(229),(326),(327),(242),(243),(240),(241),(223),(224),(282),(283),(297),(298),(295),(296),(299),(300),(116),(115),(246),(247),(119),(275),(274),(276),(277),(7),(8),(9),(10),(463),(117),(118),(177),(172),(173),(174),(175),(278),(279),(212),(213),(70),(71),(72),(73),(301),(302),(67),(68),(69),(86),(87),(11),(12),(5),(6),(13),(14),(244),(245),(425),(426),(391),(392),(389),(390),(347),(180),(181),(191),(182),(170),(171),(168),(169),(273),(355),(356),(367),(382),(383),(281),(15),(16),(307),(306),(305),(304),(348),(372),(365),(366),(370),(371),(251),(252),(203),(204),(139),(140),(178),(179),(186),(187),(27),(28),(219),(220),(221),(222),(188),(317),(318),(145),(124),(208),(404),(101),(102),(100),(189),(362),(363),(360),(225),(206),(205),(96),(97),(218),(217),(3),(227),(228),(388),(216),(37),(184),(2),(427),(428),(401),(402),(399),(400),(120),(121),(406),(264),(294),(286),(288),(284),(290),(292),(257),(293),(291),(285),(289),(287),(353),(354),(260),(259),(261),(258),(59),(60),(61),(44),(45),(64),(65),(66),(46),(47),(43),(403),(332),(333),(57),(58),(25),(26),(62),(63),(33),(34),(238),(239),(262),(263),(235),(236),(41),(42),(39),(40),(384),(434),(385),(386),(387),(409),(420),(416),(417),(408),(412),(415),(49),(48),(147),(148),(149),(352),(265),(269),(270),(266),(322),(127),(132),(376),(378),(380),(374),(373),(368),(364),(369),(375),(381),(379),(377),(231),(38),(185),(4),(329),(407),(344),(342),(319),(323),(158),(80),(82),(83),(81),(324),(320),(343),(345),(183),(308),(314),(460),(351),(461),(421),(411),(418),(419),(146),(125),(413),(414),(209),(405),(190),(361),(226),(207),(141),(142),(211),(429),(435),(432),(433),(441),(442),(444),(445),(447),(448),(449),(443),(450),(451),(452),(453),(527),(528),(533),(534),(536),(537),(538),(540),(539),(541),(542),(543),(544),(546),(547),(564),(462),(464),(579),(580),(581),(582),(583),(584),(585),(586),(587),(588),(589),(590),(591),(593),(594),(595),(596),(597),(598),(599),(600),(601),(602),(603),(604),(605),(606),(607),(614),(615),(616),(617),(618),(619),(620),(621),(622),(623),(624),(625),(626),(627),(633),(634),(635),(636),(637),(643),(650),(646),(647),(644),(648),(651),(655),(645),(642),(657),(658),(656),(659),(660),(654),(649),(666);

            INSERT INTO public.delays (date, delay_min, stop_id, line_id, user_id)
            SELECT
                (date_trunc('month', CURRENT_DATE)::date + (floor(random() * 
                    (EXTRACT(DAY FROM date_trunc('month', CURRENT_DATE) + INTERVAL '1 month' - INTERVAL '1 day') - 1))::integer)) AS date,
                CASE
                    WHEN random() < 0.6 THEN (1 + floor(random() * 10))::integer
                    WHEN random() < 0.9 THEN (5 + floor(random() * 10))::integer
                    ELSE (10 + floor(random() * 16))::integer
                END AS delay_min,
                ts.stop_id,
                (ARRAY[1,2,3,5,6,9,10,12,14,15,17,19,20,21,23,29,52,112,273])[1 + floor(random() * 19)]::integer AS line_id,
                (1 + floor(random() * 200))::integer AS user_id
            FROM 
                (SELECT stop_id FROM temp_all_stops ORDER BY random() LIMIT 1000) AS ts;

            DROP TABLE IF EXISTS temp_all_stops;
            """
        )

        conn.commit()

        print("Added random delay data")
    except KeyError as e:
            conn.rollback()
            print(f"Error: {e}")

def main():
    stopsFile = 'sharedLibraries/bus_stops_maribor2.json'
    arrivalsFile = 'sharedLibraries/bus_schedules_daily_snapshot.json'
    routesFile = 'sharedLibraries/routes_maribor.json'
    
    # print(f"Starting import at {datetime.now()}")
    
    print("Loading data from JSON files...")
    stopsData = loadJsonData(stopsFile)
    arrivalsData = loadJsonData(arrivalsFile)
    routesData = loadJsonData(routesFile)
    
    print("Connecting to database...")
    conn = connectToDb()

    if conn:
        print("Connected to database")
    else:
        print("lol ne")
    
    
    try:
        importStops(conn, stopsData)
        # importDepartures(conn, arrivalsData)
        # importRouteData(conn, routesData)
        # lastAlterScript(conn)
        # dummyDataGenerator(conn)
        
    except Exception as e:
        print(f"Error during import: {e}")
    finally:
        conn.close()
        print("Database connection closed")


if __name__ == "__main__":
    main()