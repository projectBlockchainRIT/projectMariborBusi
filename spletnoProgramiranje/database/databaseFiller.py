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
    """Import arrivals/departures data into the database"""
    cursor = conn.cursor()

    # Keep track of imported data counts
    lines_count = 0
    directions_count = 0
    departures_runs_count = 0  # To count inserts into the 'departures' table
    arrivals_count = 0  # To count inserts into the 'arrivals' table

    for date_str, stops_on_date in arrivalData[0].items(): # Assuming the outer structure is a dictionary with dates as keys
        current_date = date.fromisoformat(date_str) # Convert date string to a date object

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
                    # Insert or get line_id
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

                    # Insert or get direction_id
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

                    # Insert into 'departures' table (representing a departure run for a specific stop, direction, and date)
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

                    # Insert into 'arrivals' table (storing the array of times for the departures run)
                    # Convert list of time strings to a PostgreSQL TIME[] array format
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
                    conn.rollback() # Rollback in case of an error

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

            # 1) Lookup or insert into `lines` to get line_id
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
            conn.commit()  # commit the lines insert

            # 2) Upsert into `routes`, using the new unique constraint
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
        importDepartures(conn, arrivalsData)
        importRouteData(conn, routesData)
        lastAlterScript(conn)
        
    except Exception as e:
        print(f"Error during import: {e}")
    finally:
        conn.close()
        print("Database connection closed")


if __name__ == "__main__":
    main()