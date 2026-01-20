#!/usr/bin/env python3
"""
Interaktivna vizualizacija avtobusne lokacije s časovnim drsnikom
- Prikaže uporabnikove lokacije
- Prikaže napovedano avtobusno lokacijo
- Časovni slider za premikanje naprej/nazaj
"""

import numpy as np
import pandas as pd
import glob
import pickle
import json
from pathlib import Path
import matplotlib.pyplot as plt
from matplotlib.widgets import Slider
import tensorflow as tf

DATA_FOLDER = "data"
TRAINING_FOLDER = "training_data"
RESULTS_FOLDER = "results"

def load_simulation_data(route):
    """Naloži podatke simulacije"""
    csv_files = glob.glob(f"{DATA_FOLDER}/{route}_all_*.csv")
    if not csv_files:
        print(f"❌ Ni podatkov za linijo {route}")
        return None
    
    csv_file = sorted(csv_files)[-1]
    print(f"📂 Nalagam podatke: {csv_file}")
    return pd.read_csv(csv_file)

def load_model_and_scalers(route):
    """Naloži naučeni model in scalerje"""
    model_path = f"{RESULTS_FOLDER}/{route}_model.h5"
    if not Path(model_path).exists():
        print(f"❌ Model za linijo {route} ne obstaja")
        return None, None, None
    
    print(f"🧠 Nalagam model: {model_path}")
    model = tf.keras.models.load_model(model_path, compile=False)
    # Ponovno kompaјliraj model
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
        loss='mse',
        metrics=['mae']
    )
    
    # Naloži scalerje
    files = glob.glob(f"{TRAINING_FOLDER}/{route}_training_*_scaler_X.pkl")
    if not files:
        print("❌ Scalerji ne obstajajo")
        return None, None, None
    
    latest = sorted(files)[-1]
    base_name = latest.replace("_scaler_X.pkl", "")
    
    with open(f"{base_name}_scaler_X.pkl", 'rb') as f:
        scaler_X = pickle.load(f)
    with open(f"{base_name}_scaler_y.pkl", 'rb') as f:
        scaler_y = pickle.load(f)
    with open(f"{base_name}_metadata.json", 'r') as f:
        metadata = json.load(f)
    
    return model, scaler_X, scaler_y, metadata

def prepare_predictions(df, model, scaler_X, scaler_y, metadata):
    """Pripravi napovedi za vse časovne točke"""
    print("🔮 Pripravlam napovedi...")

    timestamps = sorted(df['timestamp'].unique())
    predictions = {}

    for timestamp in timestamps:
        group = df[df['timestamp'] == timestamp]

        # Pripravi feature vektorje
        group_sorted = group.sort_values('user_id')
        features = []
        for _, user in group_sorted.iterrows():
            features.extend([
                user['lat'],
                user['lon'],
                user['signal_strength'] / 100.0
            ])

        # Padaj na fiksno dolžino
        target_length = metadata['feature_length']
        if len(features) < target_length:
            features.extend([0] * (target_length - len(features)))
        features = np.array(features[:target_length]).reshape(1, -1)

        # Normalizacija
        features_norm = scaler_X.transform(features)

        # Napoved
        pred_norm = model.predict(features_norm, verbose=0)[0]
        pred = scaler_y.inverse_transform(pred_norm.reshape(1, -1))[0]

        # Pridobi pravo avtobusno lokacijo iz CSV (če obstaja)
        actual_bus_lat = None
        actual_bus_lon = None
        if 'bus_lat' in group.columns and 'bus_lon' in group.columns:
            # Vsi uporabniki v istem timestamp imajo enako bus lokacijo
            actual_bus_lat = group.iloc[0]['bus_lat']
            actual_bus_lon = group.iloc[0]['bus_lon']

        predictions[timestamp] = {
            'bus_lat': pred[0],
            'bus_lon': pred[1],
            'actual_bus_lat': actual_bus_lat,
            'actual_bus_lon': actual_bus_lon,
            'users': group
        }

    print(f"✅ Pripravljeno {len(predictions)} napovedi")
    return timestamps, predictions

def create_interactive_visualization(df, timestamps, predictions, route):
    """Naredi interaktivno vizualizacijo s časovnim drsnikom"""
    print("\n📊 Napravljam interaktivno vizualizacijo...")
    
    fig, ax = plt.subplots(figsize=(14, 10))
    plt.subplots_adjust(bottom=0.25)
    
    # Počakaj za inicializacijo
    initial_idx = 0
    initial_ts = timestamps[initial_idx]
    initial_data = predictions[initial_ts]
    
    # Izriši začetno stanje
    scatter_users = ax.scatter([], [], c='blue', s=100, alpha=0.6, label='Uporabniki', edgecolors='black', linewidth=1)
    scatter_bus = ax.scatter([], [], c='red', s=300, marker='*', label='Avtobusna napoved (NN)', edgecolors='darkred', linewidth=2)
    scatter_bus_actual = ax.scatter([], [], c='green', s=200, marker='o', label='Prava avtobusna lokacija', alpha=0.7, edgecolors='darkgreen', linewidth=2)
    
    # Nastavitve za osi
    all_lats = df['lat'].values
    all_lons = df['lon'].values
    lat_margin = (all_lats.max() - all_lats.min()) * 0.1
    lon_margin = (all_lons.max() - all_lons.min()) * 0.1
    
    ax.set_xlim(all_lons.min() - lon_margin, all_lons.max() + lon_margin)
    ax.set_ylim(all_lats.min() - lat_margin, all_lats.max() + lat_margin)
    ax.set_xlabel('Longitude', fontsize=12)
    ax.set_ylabel('Latitude', fontsize=12)
    ax.grid(True, alpha=0.3)
    ax.legend(fontsize=11, loc='upper right')
    
    # Besedilo za informacije
    info_text = ax.text(0.02, 0.98, '', transform=ax.transAxes, fontsize=11,
                        verticalalignment='top', bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))
    
    # Slider
    ax_slider = plt.axes([0.2, 0.1, 0.6, 0.03])
    slider = Slider(ax_slider, 'Čas', 0, len(timestamps) - 1, valinit=0, valstep=1)
    
    def update(val):
        idx = int(slider.val)
        ts = timestamps[idx]
        data = predictions[ts]
        
        users = data['users']
        user_lons = users['lon'].values
        user_lats = users['lat'].values
        
        # Posodobi uporabnikove lokacije
        scatter_users.set_offsets(np.column_stack((user_lons, user_lats)))
        
        # Posodobi avtobusno napoved
        scatter_bus.set_offsets([[data['bus_lon'], data['bus_lat']]])

        # Posodobi ground truth (prava avtobusna lokacija iz simulatorja)
        if data['actual_bus_lat'] is not None and data['actual_bus_lon'] is not None:
            gt_lat = data['actual_bus_lat']
            gt_lon = data['actual_bus_lon']
            scatter_bus_actual.set_offsets([[gt_lon, gt_lat]])
            has_actual = True
        else:
            # Fallback na povprečje uporabnikov, če ni podatka o pravi lokaciji
            gt_lat = users['lat'].mean()
            gt_lon = users['lon'].mean()
            scatter_bus_actual.set_offsets([[gt_lon, gt_lat]])
            has_actual = False

        # Izračunaj napako napovedi
        error = np.sqrt((data['bus_lat'] - gt_lat)**2 + (data['bus_lon'] - gt_lon)**2) * 111000

        # Posodobi informacijo
        location_type = "Prava lokacija" if has_actual else "Povprečje uporabnikov"
        info_text.set_text(
            f'⏱️  Čas: {ts}\n'
            f'👥 Uporabniki: {len(users)}\n'
            f'🔴 Napoved: ({data["bus_lat"]:.6f}, {data["bus_lon"]:.6f})\n'
            f'🟢 {location_type}: ({gt_lat:.6f}, {gt_lon:.6f})\n'
            f'📏 Napaka: {error:.1f} m'
        )
        
        fig.canvas.draw_idle()
    
    slider.on_changed(update)
    
    # Sprožи inizialni prikaz
    update(0)
    
    title = f'Interaktivna vizualizacija - Linija {route}\n'
    title += 'Premikaj drsnik za prehod skozi čas'
    ax.set_title(title, fontsize=14, fontweight='bold', pad=20)
    
    plt.show()

def main():
    print("🚌 INTERAKTIVNA VIZUALIZACIJA AVTOBUSNE LOKACIJE")
    print("=" * 70)
    
    # Izbira linije
    csv_files = glob.glob(f"{DATA_FOLDER}/*_all_*.csv")
    if not csv_files:
        print("❌ Ni simulacijskih podatkov!")
        return
    
    print("\nDostopne linije:")
    routes = set()
    for f in sorted(csv_files):
        route = f.split('/')[-1].split('_')[0]
        routes.add(route)
        print(f"   • {route}")
    
    route = input("\nVnesi linijo (npr. G1): ").strip().upper()
    
    if route not in routes:
        print(f"❌ Linija {route} ni dostopna")
        return
    
    # Naloži podatke
    df = load_simulation_data(route)
    if df is None or len(df) == 0:
        return
    
    # Naloži model in scalerje
    result = load_model_and_scalers(route)
    if result[0] is None:
        print("❌ Napaka pri nalaganju modela")
        return
    model, scaler_X, scaler_y, metadata = result
    
    # Pripravi napovedi
    timestamps, predictions = prepare_predictions(df, model, scaler_X, scaler_y, metadata)
    
    # Načini interaktivno vizualizacijo
    create_interactive_visualization(df, timestamps, predictions, route)
    
    print("\n✅ Vizualizacija zaključena!")

if __name__ == "__main__":
    main()
