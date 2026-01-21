#!/usr/bin/env python3
"""
Modern & Clean Interactive Bus Location Visualization
- Real-time GPS tracking with DBSCAN clustering
- Multi-bus detection and display
- Modern UI with dark mode support
"""

import numpy as np
import pandas as pd
import glob
import pickle
import json
from pathlib import Path
import matplotlib.pyplot as plt
from matplotlib.widgets import Slider
from sklearn.cluster import DBSCAN
import torch
import torch.nn as nn

# Set modern style
plt.style.use('seaborn-v0_8-darkgrid')
# Brez emoji: uporabi le DejaVu/Arial
plt.rcParams["font.family"] = ["DejaVu Sans", "Arial"]
plt.rcParams["font.sans-serif"] = ["DejaVu Sans", "Arial"]
plt.rcParams["font.monospace"] = ["Menlo", "DejaVu Sans Mono"]

DATA_FOLDER = "data"
TRAINING_FOLDER = "training_data"
RESULTS_FOLDER = "results"

# Modern color palette
COLORS = {
    'on_bus': '#2563EB',           # Bright Blue
    'waiting_at_station': '#F59E0B', # Amber
    'pedestrian': '#8B5CF6',       # Purple
    'nearby': '#6B7280',           # Gray
    'bus_actual': '#DC2626',       # Red
    'bus_detected': '#10B981',     # Green
    'bg': '#F8FAFC',               # Light background
    'text': '#1E293B',             # Dark text
    'border': '#CBD5E1'            # Border
}


class BusLocationModel(nn.Module):
    """Enaka arhitektura kot v train_model.py (PyTorch)."""

    def __init__(self, input_size: int):
        super().__init__()
        self.network = nn.Sequential(
            nn.Linear(input_size, 128),
            nn.ReLU(),
            nn.Dropout(0.2),

            nn.Linear(128, 64),
            nn.ReLU(),
            nn.Dropout(0.2),

            nn.Linear(64, 32),
            nn.ReLU(),
            nn.Dropout(0.1),

            nn.Linear(32, 16),
            nn.ReLU(),

            nn.Linear(16, 2),
        )

    def forward(self, x):
        return self.network(x)

def load_simulation_data(route):
    """Load simulation data - try multibus first"""
    csv_files = glob.glob(f"{DATA_FOLDER}/{route}_multibus_*.csv")
    if not csv_files:
        csv_files = glob.glob(f"{DATA_FOLDER}/{route}_all_*.csv")
    if not csv_files:
        print(f"❌ No data found for route {route}")
        return None
    
    csv_file = sorted(csv_files)[-1]
    print(f"📂 Loading data: {csv_file}")
    return pd.read_csv(csv_file)

def estimate_buses_with_dbscan(users_df, eps=0.0015, min_samples=5):
    """Estimate bus locations using DBSCAN clustering"""
    if len(users_df) < min_samples:
        return [(users_df['lat'].mean(), users_df['lon'].mean(), len(users_df), 0)]

    coords = users_df[['lat', 'lon']].values

    # Weight by signal strength
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

    unique_labels = set(labels)
    unique_labels.discard(-1)

    if not unique_labels:
        return [(users_df['lat'].mean(), users_df['lon'].mean(), len(users_df), 0)]

    bus_locations = []
    for label in unique_labels:
        mask = labels == label
        cluster_users = users_df.iloc[np.where(mask)[0]]

        on_bus_count = 0
        if 'user_type' in cluster_users.columns:
            on_bus_count = len(cluster_users[cluster_users['user_type'] == 'on_bus'])

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

        bus_locations.append((center_lat, center_lon, len(cluster_users), on_bus_count))

    bus_locations.sort(key=lambda x: (-x[3], -x[2]))
    return bus_locations

def load_model_and_scalers(route):
    """Naloži PyTorch checkpoint (.pt) in skalere iz training_data."""

    checkpoint_path = f"{RESULTS_FOLDER}/{route}_model.pt"
    if not Path(checkpoint_path).exists():
        return None, None, None, None

    try:
        checkpoint = torch.load(checkpoint_path, map_location='cpu')
        input_size = checkpoint.get('input_size')
        if input_size is None:
            print("❌ input_size manjka v checkpointu")
            return None, None, None, None

        model = BusLocationModel(input_size)
        model.load_state_dict(checkpoint['model_state_dict'])
        model.eval()

        # Najdi najnovejši set scalerjev
        training_files = glob.glob(f"{TRAINING_FOLDER}/{route}_training_*_metadata.json")
        if not training_files:
            print(f"❌ Ni scalerjev za {route} (manjka metadata)")
            return None, None, None, None

        latest_training = sorted(training_files)[-1]
        base_name = latest_training.replace("_metadata.json", "")

        with open(f"{base_name}_scaler_X.pkl", 'rb') as f:
            scaler_X = pickle.load(f)
        with open(f"{base_name}_scaler_y.pkl", 'rb') as f:
            scaler_y = pickle.load(f)

        return model, scaler_X, scaler_y, input_size

    except Exception as e:
        print(f"❌ Napaka pri nalaganju modela: {e}")
        return None, None, None, None

def prepare_predictions(df, model, scaler_X, scaler_y, input_size):
    """Pripravi napovedi za vse časovne korake (SAMO model)."""

    timestamps = sorted(df['timestamp'].unique())
    predictions = {}

    for timestamp in timestamps:
        group = df[df['timestamp'] == timestamp]

        on_bus = group[group['user_type'] == 'on_bus']
        if len(on_bus) > 0:
            features = on_bus[['lat', 'lon', 'signal_strength']].values.flatten()
            features_fixed = np.zeros(input_size, dtype=np.float32)
            if len(features) >= input_size:
                features_fixed[:] = features[:input_size]
            else:
                features_fixed[:len(features)] = features

            features_norm = scaler_X.transform([features_fixed])[0].astype(np.float32)

            with torch.no_grad():
                x = torch.from_numpy(features_norm).unsqueeze(0)
                pred_norm = model(x).squeeze(0).cpu().numpy()

            pred = scaler_y.inverse_transform([pred_norm])[0]
            detected_buses = [(pred[0], pred[1], len(on_bus), len(on_bus))]
        else:
            detected_buses = []

        predictions[timestamp] = {
            'detected_buses': detected_buses,
            'users': group
        }

    return timestamps, predictions

def create_interactive_visualization(df, timestamps, predictions, route, detection_method: str):
    """Create modern interactive visualization."""
    print("\n📊 Creating interactive visualization...")

    fig = plt.figure(figsize=(18, 11))
    fig.patch.set_facecolor(COLORS['bg'])
    
    gs = fig.add_gridspec(3, 2, height_ratios=[0.4, 3, 0.6], hspace=0.35, wspace=0.3,
                          left=0.08, right=0.95, top=0.95, bottom=0.08)
    
    # === TITLE SECTION ===
    ax_title = fig.add_subplot(gs[0, :])
    ax_title.axis('off')
    
    ax_title.text(0.5, 0.75, f'Bus Location Tracker', 
                 transform=ax_title.transAxes, fontsize=24, fontweight='bold',
                 ha='center', color=COLORS['text'])
    ax_title.text(0.5, 0.35, f'Line {route} • Real-time GPS + Model/DBSCAN',
                 transform=ax_title.transAxes, fontsize=13, 
                 ha='center', color='#64748B', style='italic')
    
    # === MAIN MAP ===
    ax_map = fig.add_subplot(gs[1, :])
    
    all_lats = df['lat'].values
    all_lons = df['lon'].values
    lat_margin = (all_lats.max() - all_lats.min()) * 0.1
    lon_margin = (all_lons.max() - all_lons.min()) * 0.1
    
    ax_map.set_xlim(all_lons.min() - lon_margin, all_lons.max() + lon_margin)
    ax_map.set_ylim(all_lats.min() - lat_margin, all_lats.max() + lat_margin)
    ax_map.set_xlabel('Longitude', fontsize=11, color=COLORS['text'], fontweight='bold')
    ax_map.set_ylabel('Latitude', fontsize=11, color=COLORS['text'], fontweight='bold')
    ax_map.grid(True, alpha=0.15, linestyle='--', linewidth=0.7)
    ax_map.set_facecolor('#F1F5F9')
    
    for spine in ax_map.spines.values():
        spine.set_edgecolor(COLORS['border'])
        spine.set_linewidth(1.5)
    
    # Create scatter plots for each user type
    scatter_dict = {}
    for user_type, color in COLORS.items():
        if user_type not in COLORS or user_type.startswith('bus_') or user_type in ['bg', 'text', 'border']:
            continue
        label_text = user_type.replace('_', ' ').title()
        scatter_dict[user_type] = ax_map.scatter([], [], c=color, s=90, alpha=0.75, 
                                                label=label_text, edgecolors='white', 
                                                linewidth=1.5, zorder=5)

    scatter_buses_actual = []
    scatter_buses_detected = []
    text_labels = []
    
    # === INFO PANEL (Bottom Left) ===
    ax_info = fig.add_subplot(gs[2, 0])
    ax_info.axis('off')
    
    info_text = ax_info.text(0.05, 0.95, '', transform=ax_info.transAxes, 
                            fontsize=10, verticalalignment='top',
                            fontweight='bold',
                            bbox=dict(boxstyle='round,pad=1', facecolor='#E0F2FE', 
                                     edgecolor='#0284C7', linewidth=2.5, alpha=0.95))
    
    # === STATS PANEL (Bottom Right) ===
    ax_stats = fig.add_subplot(gs[2, 1])
    ax_stats.axis('off')
    
    stats_text = ax_stats.text(0.95, 0.95, '', transform=ax_stats.transAxes,
                              fontsize=10, verticalalignment='top', ha='right',
                              fontweight='bold',
                              bbox=dict(boxstyle='round,pad=1', facecolor='#F0FDF4',
                                       edgecolor='#22C55E', linewidth=2.5, alpha=0.95))
    
    # === LEGEND ===
    legend = ax_map.legend(loc='upper left', fontsize=11, framealpha=0.98,
                          edgecolor=COLORS['border'], fancybox=True, shadow=True)
    legend.get_frame().set_facecolor('#FFFFFF')
    for text in legend.get_texts():
        text.set_color(COLORS['text'])
    
    # === SLIDER ===
    ax_slider = plt.axes([0.15, 0.035, 0.7, 0.025])
    ax_slider.set_facecolor('#E2E8F0')
    slider = Slider(ax_slider, 'Timeline', 0, len(timestamps) - 1, 
                   valinit=0, valstep=1, color='#3B82F6', track_color='#CBD5E1')
    
    # === TIME DISPLAY ===
    time_display = fig.text(0.5, 0.005, '', fontsize=12, ha='center', 
                           fontweight='bold', color='#FFFFFF',
                           bbox=dict(boxstyle='round,pad=0.6', facecolor='#3B82F6',
                                    edgecolor='#1E40AF', linewidth=2, alpha=0.95))
    
    def update(val):
        idx = int(slider.val)
        ts = timestamps[idx]
        data = predictions[ts]
        users = data['users']

        # Clear old markers
        for scatter in scatter_buses_actual + scatter_buses_detected:
            scatter.remove()
        for text in text_labels:
            text.remove()
        scatter_buses_actual.clear()
        scatter_buses_detected.clear()
        text_labels.clear()

        # Update user scatter plots
        for user_type in scatter_dict:
            type_data = users[users['user_type'] == user_type]
            lons = type_data['lon'].values
            lats = type_data['lat'].values
            if len(lons) > 0:
                scatter_dict[user_type].set_offsets(np.column_stack((lons, lats)))
            else:
                scatter_dict[user_type].set_offsets(np.empty((0, 2)))

        # Show actual bus locations
        if 'bus_id' in users.columns and 'bus_lat' in users.columns:
            bus_colors = ['#EF4444', '#22C55E', '#3B82F6', '#F59E0B', '#EC4899', '#14B8A6']
            for bus_id in sorted(users['bus_id'].unique()):
                if bus_id >= 0:
                    bus_users = users[users['bus_id'] == bus_id]
                    if len(bus_users) > 0:
                        lat, lon = bus_users.iloc[0]['bus_lat'], bus_users.iloc[0]['bus_lon']
                        color = bus_colors[int(bus_id) % len(bus_colors)]
                        
                        s = ax_map.scatter([lon], [lat], c=color, s=600, marker='o',
                                         alpha=0.9, edgecolors='white', linewidth=3.5, zorder=15)
                        scatter_buses_actual.append(s)
                        
                        t = ax_map.text(lon, lat, str(int(bus_id) + 1), color='white',
                                      fontsize=12, fontweight='bold', ha='center', 
                                      va='center', zorder=16, family='sans-serif')
                        text_labels.append(t)

        # Show detected buses (DBSCAN)
        detected_buses = data['detected_buses']
        pred_markers = ['*', 'P', 'X', 'D']
        for i, (bus_lat, bus_lon, size, on_bus_cnt) in enumerate(detected_buses):
            marker = pred_markers[i % len(pred_markers)]
            s = ax_map.scatter([bus_lon], [bus_lat], c=COLORS['bus_detected'], s=700, 
                             marker=marker, alpha=0.85, edgecolors='white', 
                             linewidth=2.5, zorder=10)
            scatter_buses_detected.append(s)

        # Update info panel
        num_users = len(users)
        num_on_bus = len(users[users['user_type'] == 'on_bus'])
        num_stations = len(users[users['user_type'] == 'waiting_at_station'])
        
        info_str = (f"CURRENT STATUS\n"
               f"{'─' * 30}\n"
               f"Timestamp: {ts}\n"
               f"Total Users: {num_users}\n"
               f"On Bus: {num_on_bus}\n"
               f"At Station: {num_stations}\n"
               f"Detected: {len(detected_buses)} bus(es)")
        
        info_text.set_text(info_str)

        # Update stats panel
        if len(detected_buses) > 0:
            avg_lat = np.mean([b[0] for b in detected_buses])
            avg_lon = np.mean([b[1] for b in detected_buses])
            stats_str = (f"DETECTION STATS\n"
                        f"{'─' * 30}\n"
                        f"Method: {detection_method}\n"
                        f"Avg Lat: {avg_lat:.6f}\n"
                        f"Avg Lon: {avg_lon:.6f}\n"
                        f"Confidence: High\n"
                        f"Status: Active")
        else:
            stats_str = (f"DETECTION STATS\n"
                        f"{'─' * 30}\n"
                        f"Method: {detection_method}\n"
                        f"Buses Found: 0\n"
                        f"Confidence: Low\n"
                        f"Status: Searching...")
        
        stats_text.set_text(stats_str)

        # Update time display
        time_str = f"Frame {idx + 1} / {len(timestamps)}  •  Timestamp: {ts}"
        time_display.set_text(time_str)

    slider.on_changed(update)
    update(0)

    print("✅ Visualization ready! Use the slider to navigate through time.")
    plt.show()

def main():
    print("\n" + "="*70)
    print("🚌 INTERACTIVE BUS LOCATION TRACKER".center(70))
    print("="*70)
    
    csv_files = glob.glob(f"{DATA_FOLDER}/*_multibus_*.csv") + glob.glob(f"{DATA_FOLDER}/*_all_*.csv")
    if not csv_files:
        print("❌ No simulation data found!")
        return
    
    print("\n📍 Available routes:")
    routes = set()
    for f in sorted(csv_files):
        route = f.split('/')[-1].split('_')[0]
        routes.add(route)
    
    for route in sorted(routes):
        print(f"   • {route}", end="  ")
    print("\n")
    
    route = input("Select route (e.g., G1): ").strip().upper() or "G1"
    
    if route not in routes:
        print(f"❌ Route {route} not available")
        return
    
    df = load_simulation_data(route)
    if df is None or len(df) == 0:
        return

    model, scaler_X, scaler_y, input_size = load_model_and_scalers(route)
    if model is None or scaler_X is None or scaler_y is None or input_size is None:
        print("❌ Model ni na voljo. Zaženi: python3 train_model.py")
        return

    print("✅ Using trained PyTorch model for bus localization\n")
    detection_method = "PyTorch model"

    timestamps, predictions = prepare_predictions(df, model, scaler_X, scaler_y, input_size)
    create_interactive_visualization(df, timestamps, predictions, route, detection_method)
    
    print("\n✅ Visualization closed!")

if __name__ == "__main__":
    main()
