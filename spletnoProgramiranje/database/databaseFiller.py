import json
import psycopg2
import re
from datetime import datetime

# Database connection parameters
DB_PARAMS = {
    'host': 'localhost',
    'database': 'm-busi',
    'user': 'postgres',
    'password': ''
}

def connect_to_db():
    """Establish and return a database connection"""
    try:
        conn = psycopg2.connect(**DB_PARAMS)
        return conn
    except psycopg2.Error as e:
        print(f"Database connection error: {e}")
        exit(1)

def fix_json_format(json_str):
    """Fix the malformed JSON by adding the missing brackets and commas"""
    # Add opening bracket if missing
    if not json_str.strip().startswith('['):
        json_str = '[' + json_str
    
    # Add closing bracket if missing
    if not json_str.strip().endswith(']'):
        json_str = json_str + ']'
    
    # Fix missing commas between objects
    json_str = re.sub(r'}\s*{', '},{', json_str)
    
    return json_str

def load_json_data(file_path):
    """Load and parse JSON data from file"""
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            content = file.read()
            # Fix potentially malformed JSON
            content = fix_json_format(content)
            return json.loads(content)
    except (json.JSONDecodeError, FileNotFoundError) as e:
        print(f"Error loading JSON from {file_path}: {e}")
        return []

def import_stops(conn, stops_data):
    """Import stops data into the database"""
    cursor = conn.cursor()
    success_count = 0
    
    for stop in stops_data:
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
    
    print(f"Successfully imported {success_count} out of {len(stops_data)} stops")
    # print(f"Imported {len(stops_data)} stops")

def import_departures(conn, arrivals_data):
    """Import arrivals/departures data into the database"""
    cursor = conn.cursor()
    
    # Keep track of imported data counts
    lines_count = 0
    directions_count = 0
    departures_count = 0
    
    for stop_info in arrivals_data:
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

def import_route_data(conn, routes_data):
    """
    Import route data into the `routes` table:
      - name      VARCHAR(10)  ← item['route']
      - path      JSONB        ← item['path']
      - line_id   INTEGER      ← lines.id matching route name
    """
    cursor = conn.cursor()
    imported = 0

    for item in routes_data:
        try:
            route_name = item['route']       # e.g. "G1"
            path_list  = item.get('path', [])  # list of [lat, lng] pairs
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


def main():
    # File paths - adjust these to your actual file locations
    stops_file = 'bus_stops_maribor2.json'
    arrivals_file = 'bus_arrival_times_stops_maribor.json'
    routes_file = 'routes_maribor_2025-05-19.json'
    
    print(f"Starting import at {datetime.now()}")
    
    # Load data from JSON files
    print("Loading data from JSON files...")
    stops_data = load_json_data(stops_file)
    arrivals_data = load_json_data(arrivals_file)
    routes_data = load_json_data(routes_file)
    
    # Connect to database
    print("Connecting to database...")
    conn = connect_to_db()

    if conn:
        print("connected")
    else:
        print("lol ne")
    
    '''
    counter = 1
    for stop in stops_data:
        print(f"{counter}: {stop['id']}, {stop['number']}, {stop['name']}, {stop['latitude']}, {stop['longitude']}\n")
        counter+=1
    '''
    

    
    try:
        import_stops(conn, stops_data)
        import_departures(conn, arrivals_data)
        import_route_data(conn, routes_data)
        # counter = 1
        # for stop in routes_data:
        #     print(f"{counter}: {stop}\n\n")
        #     counter+=1
    except Exception as e:
        print(f"Error during import: {e}")
    finally:
        conn.close()
        print("Database connection closed")

    
    '''

    
    try:
        # Import all data sets
        print("\nImporting stops data...")
        import_stops(conn, stops_data)
        
        print("\nImporting departures data...")
        import_departures(conn, arrivals_data)
        
        print("\nImporting route data...")
        import_route_data(conn, routes_data)
        
        print(f"\nData import completed successfully at {datetime.now()}")
        
    except Exception as e:
        print(f"Error during import: {e}")
    finally:
        conn.close()
        print("Database connection closed")
    '''

if __name__ == "__main__":
    main()