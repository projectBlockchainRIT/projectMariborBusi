#!/usr/bin/env python3
"""
Enostavna in hitra vizualizacija z 2 paneloma:
- Scatter plot po tipu uporabnika + estimacija lokacij avtobusov
- Diskretiziran heatmap
"""

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.widgets import Slider
from sklearn.cluster import DBSCAN
import glob

DATA_FOLDER = "data"
GRID_SIZE = 64


def load_latest_multibus(route):
    """Nalozi najnovejse multibus podatke"""
    files = glob.glob(f"{DATA_FOLDER}/{route}_multibus_*.csv")
    if not files:
        # Fallback na obicajne podatke
        files = glob.glob(f"{DATA_FOLDER}/{route}_all_*.csv")
    if not files:
        return None
    return pd.read_csv(sorted(files)[-1])


def create_heatmap(users_df, lat_min, lat_max, lon_min, lon_max, grid_size):
    """Hitra heatmap kreacija"""
    heatmap = np.zeros((grid_size, grid_size), dtype=np.float32)

    lat_scale = grid_size / (lat_max - lat_min)
    lon_scale = grid_size / (lon_max - lon_min)

    lats = ((users_df['lat'].values - lat_min) * lat_scale).astype(int)
    lons = ((users_df['lon'].values - lon_min) * lon_scale).astype(int)

    lats = np.clip(lats, 0, grid_size - 1)
    lons = np.clip(lons, 0, grid_size - 1)

    weights = users_df['signal_strength'].values / 100.0 if 'signal_strength' in users_df.columns else np.ones(len(users_df))

    for i in range(len(lats)):
        heatmap[lats[i], lons[i]] += weights[i]

    return heatmap


def estimate_bus_locations(users_df, eps=0.0008, min_samples=4):
    """
    Estimiraj lokacije avtobusov z DBSCAN clustering.
    Vrne seznam (lat, lon, num_users) za vsako detektirano gruco.
    """
    if len(users_df) < min_samples:
        return []

    coords = users_df[['lat', 'lon']].values
    clustering = DBSCAN(eps=eps, min_samples=min_samples).fit(coords)
    labels = clustering.labels_

    bus_locations = []
    unique_labels = set(labels)
    unique_labels.discard(-1)  # Odstrani sum

    for label in unique_labels:
        mask = labels == label
        cluster_users = users_df.iloc[np.where(mask)[0]]

        # Centroid gruce
        center_lat = cluster_users['lat'].mean()
        center_lon = cluster_users['lon'].mean()
        num_users = len(cluster_users)

        bus_locations.append((center_lat, center_lon, num_users))

    # Sortiraj po velikosti (najvecje gruce najprej)
    bus_locations.sort(key=lambda x: -x[2])

    return bus_locations


def main():
    print("ENOSTAVNA VIZUALIZACIJA (2 panela)")
    print("=" * 50)

    # Najdi podatke
    files = glob.glob(f"{DATA_FOLDER}/*_multibus_*.csv") + glob.glob(f"{DATA_FOLDER}/*_all_*.csv")
    if not files:
        print("Ni podatkov!")
        return

    routes = set()
    for f in files:
        route = f.split('/')[-1].split('_')[0]
        routes.add(route)

    print(f"\nLinije: {', '.join(sorted(routes))}")
    route = input("Vnesi linijo: ").strip().upper()

    df = load_latest_multibus(route)
    if df is None:
        print("Ni podatkov!")
        return

    print(f"\nNalozeno {len(df)} vrstic")
    print(f"Timestampov: {df['timestamp'].nunique()}")
    print(f"Tipi: {df['user_type'].value_counts().to_dict()}")

    # Meje
    margin = 0.003
    lat_min, lat_max = df['lat'].min() - margin, df['lat'].max() + margin
    lon_min, lon_max = df['lon'].min() - margin, df['lon'].max() + margin

    timestamps = sorted(df['timestamp'].unique())

    # Precompute - hitrejse
    print("\nPriprava podatkov...")
    cache = {}
    for ts in timestamps:
        group = df[df['timestamp'] == ts]
        heatmap = create_heatmap(group, lat_min, lat_max, lon_min, lon_max, GRID_SIZE)
        # Estimacija lokacij avtobusov
        bus_estimates = estimate_bus_locations(group, eps=0.001, min_samples=5)
        cache[ts] = {'group': group, 'heatmap': heatmap, 'buses': bus_estimates}

    print(f"Pripravljeno {len(cache)} tock")

    # Barve
    colors = {
        'on_bus': '#2196F3',
        'waiting_at_station': '#FF9800',
        'pedestrian': '#9C27B0',
        'nearby': '#607D8B'
    }

    # Vizualizacija - 2 panela
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 6))
    plt.subplots_adjust(bottom=0.18)

    ax_slider = plt.axes([0.15, 0.06, 0.7, 0.03])
    slider = Slider(ax_slider, 'Cas', 0, len(timestamps) - 1, valinit=0, valstep=1)

    # Elementi za hitro posodabljanje
    scatter_artists = {}
    heatmap_img = [None]

    def init():
        ax1.set_xlim(lon_min, lon_max)
        ax1.set_ylim(lat_min, lat_max)
        ax1.set_xlabel('Longitude')
        ax1.set_ylabel('Latitude')
        ax1.set_title('GPS lokacije po tipu')
        ax1.grid(True, alpha=0.3)

        ax2.set_xlabel('Longitude')
        ax2.set_ylabel('Latitude')
        ax2.set_title(f'Heatmap ({GRID_SIZE}x{GRID_SIZE})')

    def update(val):
        idx = int(slider.val)
        ts = timestamps[idx]
        data = cache[ts]
        group = data['group']
        buses = data['buses']

        # Scatter
        ax1.clear()
        for ut, color in colors.items():
            subset = group[group['user_type'] == ut]
            if len(subset) > 0:
                ax1.scatter(subset['lon'], subset['lat'], c=color, s=25, alpha=0.6,
                           label=f'{ut} ({len(subset)})', edgecolors='none')

        # Estimirane lokacije avtobusov
        for i, (blat, blon, nusers) in enumerate(buses):
            ax1.scatter([blon], [blat], c='red', s=300, marker='X',
                       edgecolors='darkred', linewidth=2, zorder=10,
                       label=f'Bus {i+1} ({nusers} users)' if i < 4 else None)
            # Krog okoli estimacije
            circle = plt.Circle((blon, blat), 0.001, fill=False,
                                color='red', linewidth=2, linestyle='--', alpha=0.7)
            ax1.add_patch(circle)

        ax1.set_xlim(lon_min, lon_max)
        ax1.set_ylim(lat_min, lat_max)
        ax1.set_xlabel('Longitude')
        ax1.set_ylabel('Latitude')
        ax1.set_title(f'GPS lokacije + {len(buses)} detektiranih avtobusov | {len(group)} uporabnikov')
        ax1.legend(loc='upper right', fontsize=7, markerscale=1.2)
        ax1.grid(True, alpha=0.3)

        # Heatmap
        ax2.clear()
        hm = data['heatmap']
        if hm.max() > 0:
            hm = hm / hm.max()
        ax2.imshow(hm, cmap='hot', origin='lower',
                   extent=[lon_min, lon_max, lat_min, lat_max], aspect='auto')

        # Estimirane lokacije na heatmapu
        for i, (blat, blon, nusers) in enumerate(buses):
            ax2.scatter([blon], [blat], c='cyan', s=200, marker='X',
                       edgecolors='white', linewidth=2, zorder=10)
            ax2.annotate(f'{i+1}', (blon, blat), color='white', fontsize=10,
                        fontweight='bold', ha='center', va='bottom',
                        xytext=(0, 8), textcoords='offset points')

        ax2.set_xlabel('Longitude')
        ax2.set_ylabel('Latitude')
        ax2.set_title(f'Heatmap + estimacije | Cas: {idx+1}/{len(timestamps)}')

        fig.canvas.draw_idle()

    slider.on_changed(update)
    init()
    update(0)

    fig.suptitle(f'Linija {route}', fontsize=14, fontweight='bold')
    plt.show()


if __name__ == "__main__":
    main()
