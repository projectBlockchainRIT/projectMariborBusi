#!/usr/bin/env python3
"""
Priprava treningskih podatkov iz simulatorja avtobusnih lokacij.
Ustvari X (GPS lokacije uporabnikov + signal strength) in y (prava lokacija avtobusa).
"""

import pandas as pd
import numpy as np
import glob
import json
from pathlib import Path
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
import pickle

DATA_FOLDER = "data"
TRAINING_FOLDER = "training_data"

def load_simulation_data(route):
    """Naloži CSV podatke za izbrano linijo."""
    csv_files = glob.glob(f"{DATA_FOLDER}/{route}_all_*.csv")
    if not csv_files:
        print(f"❌ Ni podatkov za linijo {route}")
        return None

    csv_file = sorted(csv_files)[-1]
    print(f"📂 Nalagam podatke iz: {csv_file}")
    return pd.read_csv(csv_file)

def create_training_pairs(df):
    """
    Ustvari treningske pare (X, y):
    X = [lat, lon, signal_strength] za vse uporabnike NA AVTOBUSU
    y = [bus_lat, bus_lon] prava lokacija iz simulatorja

    Uporabniki na postajah so ignorirani, ker so predaleč od avtobusa.
    """
    print("🔄 Ustvarjam treningske pare...")

    X_raw = []
    y = []

    for timestamp, group in df.groupby('timestamp'):
        bus_lat = group['bus_lat'].iloc[0]
        bus_lon = group['bus_lon'].iloc[0]

        on_bus_users = group[group['user_type'] == 'on_bus']

        if len(on_bus_users) < 2:
            continue

        group_sorted = on_bus_users.sort_values('user_id')

        features = []
        for _, user in group_sorted.iterrows():
            features.extend([
                user['lat'],
                user['lon'],
                user['signal_strength'] / 100.0
            ])

        X_raw.append(features)
        y.append([bus_lat, bus_lon])

    if len(X_raw) > 0:
        max_length = max(len(row) for row in X_raw)
        X = np.zeros((len(X_raw), max_length))
        for i, row in enumerate(X_raw):
            X[i, :len(row)] = row
    else:
        X = np.array(X_raw)

    y = np.array(y)

    print(f"✅ Ustvarjenih parov: {len(X)}")
    print(f"   • Dimenzija X: {X.shape}")
    print(f"   • Dimenzija y: {y.shape}")

    return X, y

def pad_features(X):
    """Normalizira dolžine vektorjev z zero-padding."""
    print("📏 Normalizujem dolžine vektorjev...")

    max_length = max(len(row) for row in X)
    max_users = (max_length // 3) + 1
    target_length = max_users * 3

    print(f"   • Maksimalno uporabnikov: {max_users}")
    print(f"   • Target dolžina: {target_length}")

    X_padded = np.zeros((len(X), target_length))

    for i, row in enumerate(X):
        X_padded[i, :len(row)] = row

    return X_padded, target_length, max_users

def normalize_data(X, y):
    """StandardScaler normalizacija X in y."""
    print("📊 Normaliziram podatke...")

    scaler_X = StandardScaler()
    X_normalized = scaler_X.fit_transform(X)

    scaler_y = StandardScaler()
    y_normalized = scaler_y.fit_transform(y)

    return X_normalized, y_normalized, scaler_X, scaler_y

def split_data(X, y, test_size=0.2, val_size=0.1):
    """Razdeli podatke na train/val/test (70%/10%/20%)."""
    print(f"📋 Razdeljujem podatke...")

    X_train, X_temp, y_train, y_temp = train_test_split(
        X, y, test_size=(test_size + val_size), random_state=42
    )

    val_ratio = val_size / (test_size + val_size)
    X_val, X_test, y_val, y_test = train_test_split(
        X_temp, y_temp, test_size=1-val_ratio, random_state=42
    )

    print(f"   • Train: {len(X_train)} ({len(X_train)/len(X)*100:.1f}%)")
    print(f"   • Val:   {len(X_val)} ({len(X_val)/len(X)*100:.1f}%)")
    print(f"   • Test:  {len(X_test)} ({len(X_test)/len(X)*100:.1f}%)")

    return (X_train, y_train), (X_val, y_val), (X_test, y_test)

def save_training_data(route, train_data, val_data, test_data, scaler_X, scaler_y, metadata):
    """Shrani vse treningske podatke v training_data/."""
    print(f"💾 Shranjujem treningske podatke...")

    Path(TRAINING_FOLDER).mkdir(exist_ok=True)

    timestamp = int(__import__('time').time())
    base_name = f"{TRAINING_FOLDER}/{route}_training_{timestamp}"

    np.save(f"{base_name}_X_train.npy", train_data[0])
    np.save(f"{base_name}_y_train.npy", train_data[1])
    np.save(f"{base_name}_X_val.npy", val_data[0])
    np.save(f"{base_name}_y_val.npy", val_data[1])
    np.save(f"{base_name}_X_test.npy", test_data[0])
    np.save(f"{base_name}_y_test.npy", test_data[1])

    with open(f"{base_name}_scaler_X.pkl", 'wb') as f:
        pickle.dump(scaler_X, f)
    with open(f"{base_name}_scaler_y.pkl", 'wb') as f:
        pickle.dump(scaler_y, f)

    with open(f"{base_name}_metadata.json", 'w') as f:
        json.dump(metadata, f, indent=2)

    print(f"✅ Shranjeno: {base_name}*")

    return base_name

def print_summary(X, y, train_data, val_data, test_data):
    """Izpiši povzetek pripravljenih podatkov."""
    print("\n" + "="*70)
    print("📊 POVZETEK".center(70))
    print("="*70)

    print(f"\n🔢 Podatki:")
    print(f"   • Parov: {len(X):,}")
    print(f"   • Feature dimenzija: {X.shape[1]}")
    print(f"   • Output dimenzija: {y.shape[1]}")

    print(f"\n📈 Razdelitev:")
    print(f"   • Train: {len(train_data[0]):,}")
    print(f"   • Val:   {len(val_data[0]):,}")
    print(f"   • Test:  {len(test_data[0]):,}")

    print(f"\n🌍 Geografski obseg:")
    print(f"   • Lat:  {y[:, 0].min():.6f} - {y[:, 0].max():.6f}")
    print(f"   • Lon:  {y[:, 1].min():.6f} - {y[:, 1].max():.6f}")

    print("\n" + "="*70 + "\n")

def main():
    print("🚌 PRIPRAVA TRENINGSKIH PODATKOV")
    print("="*70)

    csv_files = glob.glob(f"{DATA_FOLDER}/*_all_*.csv")
    if not csv_files:
        print("❌ Ni simulacijskih podatkov!")
        return

    print("\nDostopne linije:")
    routes = {}
    for f in sorted(csv_files)[-5:]:
        route = f.split('/')[-1].split('_')[0]
        if route not in routes:
            routes[route] = f
            print(f"   • {route}")

    route = input("\nVnesi linijo (npr. G1): ").strip().upper()

    df = load_simulation_data(route)
    if df is None:
        return

    X, y = create_training_pairs(df)

    if len(X) == 0:
        print("❌ Ni zadosti podatkov!")
        return

    X_padded, target_length, max_users = pad_features(X)
    X_norm, y_norm, scaler_X, scaler_y = normalize_data(X_padded, y)
    train_data, val_data, test_data = split_data(X_norm, y_norm)

    metadata = {
        "route": route,
        "total_samples": len(X),
        "train_samples": len(train_data[0]),
        "val_samples": len(val_data[0]),
        "test_samples": len(test_data[0]),
        "feature_length": target_length,
        "max_users": max_users,
        "output_dim": 2,
        "normalization": "StandardScaler",
        "timestamp": int(__import__('time').time())
    }

    save_training_data(route, train_data, val_data, test_data, scaler_X, scaler_y, metadata)
    print_summary(X_padded, y, train_data, val_data, test_data)

    print("✅ Treningski podatki pripravljeni!")
    print(f"\n💡 Naslednji korak: python3 train_model.py")

if __name__ == "__main__":
    main()
