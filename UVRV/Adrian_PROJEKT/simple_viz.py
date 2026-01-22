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


def estimate_bus_locations(users_df, eps=0.0015, min_samples=5):
    """
    Estimiraj lokacije avtobusov z DBSCAN clustering.
    Vrne seznam (lat, lon, num_users, on_bus_count) za vsako detektirano gruco.

    eps=0.0015 corresponds to ~167m radius, suitable for 40-100m bus spread with noise
    min_samples=5 requires at least 5 users for a cluster (good for 6-15 users per bus)
    """
    if len(users_df) < min_samples:
        return []

    coords = users_df[['lat', 'lon']].values

    if 'signal_strength' in users_df.columns:
        weights = users_df['signal_strength'].values / 100.0
        if 'user_type' in users_df.columns:
            type_weights = users_df['user_type'].apply(
                lambda x: 1.5 if x == 'on_bus' else 1.0
            ).values
            weights = weights * type_weights
    else:
        weights = None

    clustering = DBSCAN(eps=eps, min_samples=min_samples).fit(coords, sample_weight=weights)
    labels = clustering.labels_

    bus_locations = []
    unique_labels = set(labels)
    unique_labels.discard(-1)

    for label in unique_labels:
        mask = labels == label
        cluster_users = users_df.iloc[np.where(mask)[0]]

        if 'signal_strength' in cluster_users.columns:
            weights_cluster = cluster_users['signal_strength'].values / 100.0
            if 'user_type' in cluster_users.columns:
                type_w = cluster_users['user_type'].apply(
                    lambda x: 2.0 if x == 'on_bus' else 1.0
                ).values
                weights_cluster = weights_cluster * type_w

            center_lat = np.average(cluster_users['lat'], weights=weights_cluster)
            center_lon = np.average(cluster_users['lon'], weights=weights_cluster)
        else:
            center_lat = cluster_users['lat'].mean()
            center_lon = cluster_users['lon'].mean()

        num_users = len(cluster_users)

        on_bus_count = len(cluster_users[cluster_users['user_type'] == 'on_bus']) if 'user_type' in cluster_users.columns else 0

        bus_locations.append((center_lat, center_lon, num_users, on_bus_count))

    bus_locations.sort(key=lambda x: (-x[3], -x[2]))

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

    margin = 0.003
    lat_min, lat_max = df['lat'].min() - margin, df['lat'].max() + margin
    lon_min, lon_max = df['lon'].min() - margin, df['lon'].max() + margin

    timestamps = sorted(df['timestamp'].unique())

    print("\nPriprava podatkov...")
    cache = {}
    for ts in timestamps:
        group = df[df['timestamp'] == ts]
        heatmap = create_heatmap(group, lat_min, lat_max, lon_min, lon_max, GRID_SIZE)
        bus_estimates = estimate_bus_locations(group, eps=0.0015, min_samples=5)
        cache[ts] = {'group': group, 'heatmap': heatmap, 'buses': bus_estimates}

    print(f"Pripravljeno {len(cache)} tock")

    colors = {
        'on_bus': '#2196F3',
        'waiting_at_station': '#FF9800',
        'pedestrian': '#9C27B0',
        'nearby': '#607D8B'
    }

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 6))
    plt.subplots_adjust(bottom=0.18)

    ax_slider = plt.axes([0.15, 0.06, 0.7, 0.03])
    slider = Slider(ax_slider, 'Cas', 0, len(timestamps) - 1, valinit=0, valstep=1)

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

        ax1.clear()
        for ut, color in colors.items():
            subset = group[group['user_type'] == ut]
            if len(subset) > 0:
                ax1.scatter(subset['lon'], subset['lat'], c=color, s=25, alpha=0.6,
                           label=f'{ut} ({len(subset)})', edgecolors='none')

        # Estimirane lokacije avtobusov - razlicne barve za lazjo identifikacijo
        bus_colors = ['#FF0000', '#00FF00', '#0000FF', '#FF00FF', '#FFFF00']  # rdeča, zelena, modra, magenta, rumena
        for i, (blat, blon, nusers, on_bus_count) in enumerate(buses):
            color = bus_colors[i % len(bus_colors)]
            ax1.scatter([blon], [blat], c=color, s=350, marker='X',
                       edgecolors='black', linewidth=2.5, zorder=10,
                       label=f'Bus {i+1} ({on_bus_count}/{nusers} on bus)' if i < 5 else None)
            # Vecji krog okoli estimacije za boljso vidnost
            circle = plt.Circle((blon, blat), 0.0012, fill=False,
                                color=color, linewidth=2.5, linestyle='--', alpha=0.8)
            ax1.add_patch(circle)
            # Dodaj stevilko avtobusa
            ax1.text(blon, blat, str(i+1), color='white', fontsize=12,
                    fontweight='bold', ha='center', va='center', zorder=11)

        ax1.set_xlim(lon_min, lon_max)
        ax1.set_ylim(lat_min, lat_max)
        ax1.set_xlabel('Longitude', fontsize=10)
        ax1.set_ylabel('Latitude', fontsize=10)
        ax1.set_title(f'GPS lokacije + {len(buses)} detektiranih avtobusov | {len(group)} uporabnikov | Cas: {idx+1}/{len(timestamps)}',
                     fontsize=11, fontweight='bold')
        ax1.legend(loc='upper right', fontsize=8, markerscale=0.8, framealpha=0.9)
        ax1.grid(True, alpha=0.3, linestyle=':')

        # Heatmap
        ax2.clear()
        hm = data['heatmap']
        if hm.max() > 0:
            hm = hm / hm.max()
        ax2.imshow(hm, cmap='hot', origin='lower',
                   extent=[lon_min, lon_max, lat_min, lat_max], aspect='auto')

        bus_colors_heatmap = ['cyan', 'lime', 'yellow', 'magenta', 'orange']
        for i, (blat, blon, nusers, on_bus_count) in enumerate(buses):
            color = bus_colors_heatmap[i % len(bus_colors_heatmap)]
            ax2.scatter([blon], [blat], c=color, s=250, marker='X',
                       edgecolors='white', linewidth=2.5, zorder=10)
            ax2.annotate(f'{i+1}', (blon, blat), color='white', fontsize=11,
                        fontweight='bold', ha='center', va='bottom',
                        xytext=(0, 10), textcoords='offset points',
                        bbox=dict(boxstyle='circle,pad=0.3', facecolor='black', alpha=0.7))

        ax2.set_xlabel('Longitude', fontsize=10)
        ax2.set_ylabel('Latitude', fontsize=10)
        ax2.set_title(f'Heatmap + {len(buses)} avtobusov', fontsize=11, fontweight='bold')

        fig.canvas.draw_idle()

    slider.on_changed(update)
    init()
    update(0)

    fig.suptitle(f'Linija {route}', fontsize=14, fontweight='bold')
    plt.show()


if __name__ == "__main__":
    main()
