#!/usr/bin/env python3
"""
Generator podatkov z vec avtobusi na isti liniji.
Ustvari bolj realisticne podatke z vec grucami.
"""

import numpy as np
import pandas as pd
import json
from pathlib import Path

DATA_FOLDER = "data"
NUM_BUSES = 3
NUM_TIMESTAMPS = 500

ON_BUS_SPREAD = (40, 100)
PEDESTRIAN_SPREAD = (120, 350)
NEARBY_SPREAD = (180, 450)
STATION_SPREAD = (25, 60)

ON_BUS_USERS = (6, 15)
PEDESTRIANS = (1, 10)
NEARBY_USERS = (1, 7)
STATION_USERS = (1, 8)  


def load_route(route_name):
    """Nalozi pot iz routes.json"""
    with open("routes.json", "r") as f:
        routes = json.load(f)
    for r in routes:
        if r["route"] == route_name:
            return np.array(r["path"])
    return None


def meters_to_degrees(meters, lat):
    """Pretvori metre v stopinje"""
    lat_deg = meters / 111000
    lon_deg = meters / (111000 * np.cos(np.radians(lat)))
    return lat_deg, lon_deg


def add_noise(lat, lon, spread_m):
    """Dodaj Gaussov sum v metrih"""
    lat_noise, lon_noise = meters_to_degrees(spread_m, lat)
    angle = np.random.uniform(0, 2 * np.pi)
    r = np.sqrt(np.random.gamma(2.0, 0.25))
    r = np.clip(r, 0, 2.5)
    return lat + r * lat_noise * np.cos(angle), lon + r * lon_noise * np.sin(angle)


def generate_multibus_data(route_name):
    """Generiraj podatke z vec avtobusi"""
    path = load_route(route_name)
    if path is None:
        print(f"Linija {route_name} ne obstaja!")
        return None

    print(f"Generiranje podatkov za {route_name}...")
    print(f"  Avtobusov: {NUM_BUSES}")
    print(f"  Casovnih tock: {NUM_TIMESTAMPS}")

    num_stations = 8
    station_indices = np.linspace(0, len(path) - 1, num_stations, dtype=int)
    stations = path[station_indices]

    records = []

    bus_positions = np.linspace(0, len(path) * 0.8, NUM_BUSES)
    bus_directions = []

    for i in range(NUM_BUSES):
        if i % 2 == 0:
            bus_directions.append(1)
        else:
            bus_directions.append(-1)
            bus_positions[i] = len(path) * 0.8 - i * (len(path) * 0.2)

    for ts in range(NUM_TIMESTAMPS):
        timestamp = 1700000000 + ts * 10

        for bus_id in range(NUM_BUSES):
            speed = np.random.uniform(0.8, 2.5)
            bus_positions[bus_id] += speed * bus_directions[bus_id]

            if bus_positions[bus_id] >= len(path) - 1:
                bus_positions[bus_id] = len(path) - 1
                bus_directions[bus_id] = -1
            elif bus_positions[bus_id] <= 0:
                bus_positions[bus_id] = 0
                bus_directions[bus_id] = 1

        for bus_id in range(NUM_BUSES):
            pos_idx = int(bus_positions[bus_id]) % len(path)
            bus_lat, bus_lon = path[pos_idx]

            num_on_bus = np.random.randint(*ON_BUS_USERS)
            spread = np.random.uniform(*ON_BUS_SPREAD)

            for u in range(num_on_bus):
                user_spread = spread * np.random.uniform(0.6, 1.3)
                lat, lon = add_noise(bus_lat, bus_lon, user_spread)
                signal = np.clip(np.random.normal(70, 20), 20, 100)

                records.append({
                    'user_id': 1000 + bus_id * 100 + u,
                    'route': route_name,
                    'lat': lat,
                    'lon': lon,
                    'timestamp': timestamp,
                    'signal_strength': signal,
                    'accuracy_meters': 50 - signal * 0.4,
                    'user_type': 'on_bus',
                    'bus_lat': bus_lat,
                    'bus_lon': bus_lon,
                    'bus_id': bus_id
                })

        num_peds = np.random.randint(*PEDESTRIANS)
        for p in range(num_peds):
            ped_pos = np.random.randint(0, len(path))
            ped_lat, ped_lon = path[ped_pos]
            spread = np.random.uniform(*PEDESTRIAN_SPREAD)
            lat, lon = add_noise(ped_lat, ped_lon, spread)
            signal = np.clip(np.random.normal(60, 20), 20, 100)

            records.append({
                'user_id': 3000 + p,
                'route': route_name,
                'lat': lat,
                'lon': lon,
                'timestamp': timestamp,
                'signal_strength': signal,
                'accuracy_meters': 50 - signal * 0.4,
                'user_type': 'pedestrian',
                'bus_lat': 0,
                'bus_lon': 0,
                'bus_id': -1
            })

        num_nearby = np.random.randint(*NEARBY_USERS)
        for n in range(num_nearby):
            nearby_pos = np.random.randint(0, len(path))
            nearby_lat, nearby_lon = path[nearby_pos]
            spread = np.random.uniform(*NEARBY_SPREAD)
            lat, lon = add_noise(nearby_lat, nearby_lon, spread)
            signal = np.clip(np.random.normal(50, 20), 20, 100)

            records.append({
                'user_id': 4000 + n,
                'route': route_name,
                'lat': lat,
                'lon': lon,
                'timestamp': timestamp,
                'signal_strength': signal,
                'accuracy_meters': 50 - signal * 0.4,
                'user_type': 'nearby',
                'bus_lat': 0,
                'bus_lon': 0,
                'bus_id': -1
            })

        for s_idx, (s_lat, s_lon) in enumerate(stations):
            if np.random.random() < 0.4:
                num_waiting = np.random.randint(*STATION_USERS)
                for w in range(num_waiting):
                    spread = np.random.uniform(*STATION_SPREAD)
                    lat, lon = add_noise(s_lat, s_lon, spread)
                    signal = np.clip(np.random.normal(70, 15), 20, 100)

                    records.append({
                        'user_id': 2000 + s_idx * 100 + w,
                        'route': route_name,
                        'lat': lat,
                        'lon': lon,
                        'timestamp': timestamp,
                        'signal_strength': signal,
                        'accuracy_meters': 50 - signal * 0.4,
                        'user_type': 'waiting_at_station',
                        'bus_lat': 0,
                        'bus_lon': 0,
                        'bus_id': -1
                    })

    df = pd.DataFrame(records)

    Path(DATA_FOLDER).mkdir(exist_ok=True)
    filename = f"{DATA_FOLDER}/{route_name}_multibus_{int(pd.Timestamp.now().timestamp())}.csv"
    df.to_csv(filename, index=False)

    print(f"\nShranjeno: {filename}")
    print(f"Vrstic: {len(df)}")
    print(f"Tipi uporabnikov:")
    print(df['user_type'].value_counts())

    return filename


def generate_all_routes():
    """Generiraj podatke za vse linije"""
    import json
    with open("routes.json", "r") as f:
        routes = json.load(f)

    all_routes = [r["route"] for r in routes]
    print(f"\n{'='*70}")
    print(f"GENERIRANJE PODATKOV ZA {len(all_routes)} LINIJ")
    print(f"{'='*70}\n")

    successful = []
    failed = []

    for i, route in enumerate(all_routes, 1):
        print(f"\n[{i}/{len(all_routes)}] Procesiranje {route}...")
        print("-" * 50)
        try:
            filename = generate_multibus_data(route)
            if filename:
                successful.append(route)
        except Exception as e:
            print(f"Napaka pri {route}: {e}")
            failed.append(route)

    print(f"\n{'='*70}")
    print(f"KONČANO")
    print(f"{'='*70}")
    print(f"Uspesno: {len(successful)} linij")
    if successful:
        print(f"   {', '.join(successful)}")
    if failed:
        print(f"Neuspesno: {len(failed)} linij")
        print(f"   {', '.join(failed)}")


if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1:
        # Generiraj za določeno linijo
        route = sys.argv[1].upper()
        generate_multibus_data(route)
    else:
        # Generiraj za vse linije
        generate_all_routes()
