import json
import psycopg2
import re
from datetime import datetime

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
    departures_count = 0
    
    for stop_info in arrivalData:
        stop_id = stop_info.get('id')
        departures = stop_info.get('departures', [])
        
        if not stop_id or not departures:
            continue
        
        for departure_info in departures:
            line_code = departure_info.get('line')
            direction_name = departure_info.get('direction')
            times = departure_info.get('times', [])
            
            if not line_code or not direction_name:
                continue
            
            # Insert or get line_id
            try:
                cursor.execute(
                    """
                    INSERT INTO lines (line_code)
                    VALUES (%s)
                    ON CONFLICT (line_code) DO NOTHING
                    RETURNING id
                    """,
                    (line_code,)
                )
                
                result = cursor.fetchone()
                
                
                if result:
                    line_id = result[0]
                    lines_count += 1
                else:
                    cursor.execute("SELECT id FROM lines WHERE line_code = %s", (line_code,))
                    line_id = cursor.fetchone()[0]

                conn.commit()
                
                # Insert or get direction_id
                cursor.execute(
                    """
                    INSERT INTO directions (line_id, name)
                    VALUES (%s, %s)
                    ON CONFLICT (line_id, name) DO NOTHING
                    RETURNING id
                    """,
                    (line_id, direction_name)
                )
                
                result = cursor.fetchone()
                
                
                if result:
                    direction_id = result[0]
                    directions_count += 1
                else:
                    cursor.execute(
                        "SELECT id FROM directions WHERE line_id = %s AND name = %s",
                        (line_id, direction_name)
                    )
                    
                    direction_id = cursor.fetchone()[0]
                    
                conn.commit()
                # Insert all departure times
                for time_str in times:
                    cursor.execute(
                        """
                        INSERT INTO departures (stop_id, direction_id, departure)
                        VALUES (%s, %s, %s)
                        ON CONFLICT DO NOTHING
                        """,
                        (stop_id, direction_id, time_str)
                    )
                    conn.commit()
                    departures_count += 1
                
            except Exception as e:
                print(f"Error importing departure for stop {stop_id}, line {line_code}: {e}")
    
    
    print(f"Imported {lines_count} lines, {directions_count} directions, {departures_count} departures")

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
    arrivalsFile = 'sharedLibraries/bus_arrival_times_stops_maribor.json'
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
        print("Error connecting to database")
        exit(1)
    
    
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