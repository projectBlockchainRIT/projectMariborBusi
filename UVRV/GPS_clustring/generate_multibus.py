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
NUM_BUSES = 4  # Stevilo avtobusov na liniji
NUM_TIMESTAMPS = 500  # Stevilo casovnih tock

# Parametri razprsenosti (v metrih)
ON_BUS_SPREAD = (30, 80)  # min, max
PEDESTRIAN_SPREAD = (100, 300)
NEARBY_SPREAD = (150, 400)
STATION_SPREAD = (20, 50)

# Stevilo uporabnikov
ON_BUS_USERS = (4, 12)  # na avtobus
PEDESTRIANS = (0, 8)  # skupaj
NEARBY_USERS = (0, 5)  # skupaj
STATION_USERS = (0, 6)  # na postajo


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
    r = np.abs(np.random.normal(0, 1))
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

    # Postaje - vsakih ~12% poti
    num_stations = 8
    station_indices = np.linspace(0, len(path) - 1, num_stations, dtype=int)
    stations = path[station_indices]

    records = []

    # Zacetne pozicije avtobusov - razporejeni po liniji
    bus_positions = np.linspace(0, len(path) * 0.7, NUM_BUSES)

    for ts in range(NUM_TIMESTAMPS):
        timestamp = 1700000000 + ts * 10  # 10 sekund med meritvami

        # Premakni avtobuse
        for bus_id in range(NUM_BUSES):
            bus_positions[bus_id] += np.random.uniform(0.5, 2.0)
            if bus_positions[bus_id] >= len(path):
                bus_positions[bus_id] = 0  # Zacni znova

        # Za vsak avtobus generiraj uporabnike
        for bus_id in range(NUM_BUSES):
            pos_idx = int(bus_positions[bus_id]) % len(path)
            bus_lat, bus_lon = path[pos_idx]

            # Uporabniki na avtobusu
            num_on_bus = np.random.randint(*ON_BUS_USERS)
            spread = np.random.uniform(*ON_BUS_SPREAD)

            for u in range(num_on_bus):
                user_spread = spread * np.random.uniform(0.5, 1.5)
                lat, lon = add_noise(bus_lat, bus_lon, user_spread)
                signal = np.clip(np.random.normal(75, 15), 20, 100)

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

        # Pesci - nakljucno vzdolz linije
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

        # Bliznji uporabniki
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

        # Uporabniki na postajah
        for s_idx, (s_lat, s_lon) in enumerate(stations):
            if np.random.random() < 0.4:  # 40% verjetnost
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

    # Shrani
    Path(DATA_FOLDER).mkdir(exist_ok=True)
    filename = f"{DATA_FOLDER}/{route_name}_multibus_{int(pd.Timestamp.now().timestamp())}.csv"
    df.to_csv(filename, index=False)

    print(f"\nShranjeno: {filename}")
    print(f"Vrstic: {len(df)}")
    print(f"Tipi uporabnikov:")
    print(df['user_type'].value_counts())

    return filename


if __name__ == "__main__":
    generate_multibus_data("G1")
