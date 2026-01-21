#!/usr/bin/env python3
"""
Heatmap-based CNN pristop za detekcijo lokacije avtobusov.
- Diskretizira GPS koordinate VSEH uporabnikov v 2D grid (heatmap)
- CNN napove lokacije avtobusov iz skupin uporabnikov
- Podpora za vec avtobusov hkrati

Vhod: Heatmap vseh GPS lokacij (on_bus, pedestrians, nearby, station)
Izhod: Lokacije avtobusov (lat, lon) ali segmentacijska maska
"""

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.widgets import Slider
import glob
import pickle
import json
from pathlib import Path
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.cluster import DBSCAN
from scipy import ndimage

import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset

DATA_FOLDER = "data"
TRAINING_FOLDER = "training_data"
RESULTS_FOLDER = "results"

# Grid parametri
GRID_SIZE = 64  # 64x64 grid za boljso locljivost
MAX_BUSES = 4  # Maksimalno stevilo avtobusov za detekcijo
MIN_USERS_THRESHOLD = 5  # Minimalno stevilo uporabnikov za veljaven vzorec


class HeatmapCNN(nn.Module):
    """CNN za napoved lokacije avtobusa iz heatmap vhoda (single bus)."""
    def __init__(self, grid_size=64):
        super().__init__()
        self.grid_size = grid_size

        # Izracunaj velikost po conv layerjih
        final_size = grid_size // 8  # 3x MaxPool2d(2)

        # Convolutional layers
        self.conv_layers = nn.Sequential(
            # Input: 1 x grid_size x grid_size
            nn.Conv2d(1, 32, kernel_size=3, padding=1),
            nn.BatchNorm2d(32),
            nn.ReLU(),
            nn.MaxPool2d(2),

            nn.Conv2d(32, 64, kernel_size=3, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU(),
            nn.MaxPool2d(2),

            nn.Conv2d(64, 128, kernel_size=3, padding=1),
            nn.BatchNorm2d(128),
            nn.ReLU(),
            nn.MaxPool2d(2),
        )

        # Fully connected layers
        self.fc_layers = nn.Sequential(
            nn.Flatten(),
            nn.Linear(128 * final_size * final_size, 256),
            nn.ReLU(),
            nn.Dropout(0.3),
            nn.Linear(256, 64),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(64, 2)  # Output: normalized lat, lon
        )

    def forward(self, x):
        x = self.conv_layers(x)
        x = self.fc_layers(x)
        return x


class MultiBusDetector(nn.Module):
    """
    CNN za detekcijo VEC avtobusov hkrati.
    Izhod: heatmap verjetnosti lokacij avtobusov (segmentacija).
    """
    def __init__(self, grid_size=64):
        super().__init__()
        self.grid_size = grid_size

        # Encoder
        self.encoder = nn.Sequential(
            nn.Conv2d(1, 32, kernel_size=3, padding=1),
            nn.BatchNorm2d(32),
            nn.ReLU(),
            nn.Conv2d(32, 32, kernel_size=3, padding=1),
            nn.BatchNorm2d(32),
            nn.ReLU(),
            nn.MaxPool2d(2),  # /2

            nn.Conv2d(32, 64, kernel_size=3, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU(),
            nn.Conv2d(64, 64, kernel_size=3, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU(),
            nn.MaxPool2d(2),  # /4

            nn.Conv2d(64, 128, kernel_size=3, padding=1),
            nn.BatchNorm2d(128),
            nn.ReLU(),
            nn.Conv2d(128, 128, kernel_size=3, padding=1),
            nn.BatchNorm2d(128),
            nn.ReLU(),
        )

        # Decoder (upsample nazaj na originalno velikost)
        self.decoder = nn.Sequential(
            nn.ConvTranspose2d(128, 64, kernel_size=2, stride=2),  # *2
            nn.BatchNorm2d(64),
            nn.ReLU(),
            nn.Conv2d(64, 64, kernel_size=3, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU(),

            nn.ConvTranspose2d(64, 32, kernel_size=2, stride=2),  # *4
            nn.BatchNorm2d(32),
            nn.ReLU(),
            nn.Conv2d(32, 32, kernel_size=3, padding=1),
            nn.BatchNorm2d(32),
            nn.ReLU(),

            nn.Conv2d(32, 1, kernel_size=1),  # Output: 1 kanal (verjetnost)
            nn.Sigmoid()  # Verjetnost [0, 1]
        )

    def forward(self, x):
        x = self.encoder(x)
        x = self.decoder(x)
        return x


class MultiBusRegressor(nn.Module):
    """
    CNN za regresijo lokacij vec avtobusov.
    Izhod: (MAX_BUSES * 3) vrednosti = [lat, lon, confidence] za vsak avtobus.
    """
    def __init__(self, grid_size=64, max_buses=4):
        super().__init__()
        self.grid_size = grid_size
        self.max_buses = max_buses

        final_size = grid_size // 8

        self.conv_layers = nn.Sequential(
            nn.Conv2d(1, 32, kernel_size=3, padding=1),
            nn.BatchNorm2d(32),
            nn.ReLU(),
            nn.MaxPool2d(2),

            nn.Conv2d(32, 64, kernel_size=3, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU(),
            nn.MaxPool2d(2),

            nn.Conv2d(64, 128, kernel_size=3, padding=1),
            nn.BatchNorm2d(128),
            nn.ReLU(),
            nn.MaxPool2d(2),

            nn.Conv2d(128, 256, kernel_size=3, padding=1),
            nn.BatchNorm2d(256),
            nn.ReLU(),
        )

        self.fc_layers = nn.Sequential(
            nn.Flatten(),
            nn.Linear(256 * final_size * final_size, 512),
            nn.ReLU(),
            nn.Dropout(0.3),
            nn.Linear(512, 256),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(256, max_buses * 3)  # [lat, lon, confidence] * max_buses
        )

    def forward(self, x):
        x = self.conv_layers(x)
        x = self.fc_layers(x)
        # Reshape to (batch, max_buses, 3)
        batch_size = x.shape[0]
        x = x.view(batch_size, self.max_buses, 3)
        # Sigmoid na confidence
        x[:, :, 2] = torch.sigmoid(x[:, :, 2])
        return x


def load_simulation_data(route):
    """Nalozi podatke simulacije - preferira multibus podatke."""
    # Najprej poskusi multibus podatke
    csv_files = glob.glob(f"{DATA_FOLDER}/{route}_multibus_*.csv")
    if not csv_files:
        # Fallback na obicajne podatke
        csv_files = glob.glob(f"{DATA_FOLDER}/{route}_all_*.csv")
    if not csv_files:
        print(f"Ni podatkov za linijo {route}")
        return None

    csv_file = sorted(csv_files)[-1]
    print(f"Nalagam podatke: {csv_file}")
    return pd.read_csv(csv_file)


def create_heatmap(users_df, lat_min, lat_max, lon_min, lon_max, grid_size=32):
    """
    Ustvari 2D heatmap iz GPS lokacij uporabnikov.

    Args:
        users_df: DataFrame z lat, lon, signal_strength stolpci
        lat_min, lat_max, lon_min, lon_max: Geografske meje
        grid_size: Velikost grida

    Returns:
        2D numpy array (grid_size x grid_size) z stevilom uporabnikov v vsaki celici
    """
    heatmap = np.zeros((grid_size, grid_size), dtype=np.float32)

    lat_step = (lat_max - lat_min) / grid_size
    lon_step = (lon_max - lon_min) / grid_size

    for _, user in users_df.iterrows():
        lat_idx = int((user['lat'] - lat_min) / lat_step)
        lon_idx = int((user['lon'] - lon_min) / lon_step)

        # Clamp indekse
        lat_idx = max(0, min(grid_size - 1, lat_idx))
        lon_idx = max(0, min(grid_size - 1, lon_idx))

        # Utezimo po signal_strength
        weight = user['signal_strength'] / 100.0 if 'signal_strength' in user else 1.0
        heatmap[lat_idx, lon_idx] += weight

    return heatmap


def coords_to_grid_indices(lat, lon, lat_min, lat_max, lon_min, lon_max, grid_size=32):
    """Pretvori koordinate v grid indekse."""
    lat_step = (lat_max - lat_min) / grid_size
    lon_step = (lon_max - lon_min) / grid_size

    lat_idx = (lat - lat_min) / lat_step
    lon_idx = (lon - lon_min) / lon_step

    return lat_idx, lon_idx


def grid_indices_to_coords(lat_idx, lon_idx, lat_min, lat_max, lon_min, lon_max, grid_size=32):
    """Pretvori grid indekse nazaj v koordinate."""
    lat_step = (lat_max - lat_min) / grid_size
    lon_step = (lon_max - lon_min) / grid_size

    lat = lat_min + (lat_idx + 0.5) * lat_step  # +0.5 za center celice
    lon = lon_min + (lon_idx + 0.5) * lon_step

    return lat, lon


def prepare_heatmap_training_data(route, grid_size=64):
    """
    Pripravi treningske podatke z heatmap reprezentacijo.
    POMEMBNO: Uporablja VSE uporabnike za vhod (ne samo on_bus)!

    Returns:
        X: heatmaps (N, 1, grid_size, grid_size) - VSI uporabniki
        y: normalized bus positions (N, 2) v grid koordinatah
        metadata: slovar z informacijami
    """
    print(f"\nPriprava heatmap treningskih podatkov za {route}...")
    print("OPOMBA: Heatmap vsebuje VSE uporabnike (on_bus, pedestrian, nearby, station)")

    df = load_simulation_data(route)
    if df is None:
        return None, None, None

    # Doloci geografske meje
    margin = 0.002
    lat_min = df['lat'].min() - margin
    lat_max = df['lat'].max() + margin
    lon_min = df['lon'].min() - margin
    lon_max = df['lon'].max() + margin

    print(f"Geografske meje:")
    print(f"  Lat: {lat_min:.6f} - {lat_max:.6f}")
    print(f"  Lon: {lon_min:.6f} - {lon_max:.6f}")

    # Grupiraj po timestamp
    timestamps = sorted(df['timestamp'].unique())
    print(f"Unikatnih timestampov: {len(timestamps)}")

    X_list = []
    y_list = []

    for ts in timestamps:
        group = df[df['timestamp'] == ts]

        # Uporabi VSE uporabnike za heatmap (to je bistvo - detekcija iz vseh)
        if len(group) < MIN_USERS_THRESHOLD:
            continue

        # Ustvari heatmap iz VSEH uporabnikov
        heatmap = create_heatmap(
            group, lat_min, lat_max, lon_min, lon_max, grid_size
        )

        # Normaliziraj heatmap
        if heatmap.max() > 0:
            heatmap = heatmap / heatmap.max()

        # Pridobi ground truth bus lokacijo (povprecje vseh avtobusov)
        on_bus = group[group['user_type'] == 'on_bus']
        if len(on_bus) == 0 or 'bus_lat' not in group.columns:
            continue

        # Za single bus: vzemi povprecje lokacij (ce je vec avtobusov)
        bus_lat = on_bus['bus_lat'].mean()
        bus_lon = on_bus['bus_lon'].mean()

        # Pretvori bus lokacijo v normalizirane grid koordinate [0, 1]
        bus_lat_norm = (bus_lat - lat_min) / (lat_max - lat_min)
        bus_lon_norm = (bus_lon - lon_min) / (lon_max - lon_min)

        X_list.append(heatmap)
        y_list.append([bus_lat_norm, bus_lon_norm])

    if len(X_list) == 0:
        print("Ni veljavnih vzorcev!")
        return None, None, None

    X = np.array(X_list).reshape(-1, 1, grid_size, grid_size)
    y = np.array(y_list)

    print(f"\nPripravljeno vzorcev: {len(X)}")
    print(f"  X shape: {X.shape}")
    print(f"  y shape: {y.shape}")

    metadata = {
        'lat_min': lat_min,
        'lat_max': lat_max,
        'lon_min': lon_min,
        'lon_max': lon_max,
        'grid_size': grid_size,
        'num_samples': len(X),
        'input_type': 'all_users'
    }

    return X, y, metadata


def prepare_multibus_training_data(route, grid_size=64, max_buses=4):
    """
    Pripravi treningske podatke za detekcijo VEC avtobusov.

    Returns:
        X: heatmaps (N, 1, grid_size, grid_size)
        y: bus positions (N, max_buses, 3) = [lat_norm, lon_norm, exists]
        metadata: slovar z informacijami
    """
    print(f"\nPriprava MULTI-BUS treningskih podatkov za {route}...")

    df = load_simulation_data(route)
    if df is None:
        return None, None, None

    # Doloci geografske meje
    margin = 0.002
    lat_min = df['lat'].min() - margin
    lat_max = df['lat'].max() + margin
    lon_min = df['lon'].min() - margin
    lon_max = df['lon'].max() + margin

    timestamps = sorted(df['timestamp'].unique())
    print(f"Timestampov: {len(timestamps)}")

    X_list = []
    y_list = []

    for ts in timestamps:
        group = df[df['timestamp'] == ts]

        if len(group) < MIN_USERS_THRESHOLD:
            continue

        # Heatmap iz VSEH uporabnikov
        heatmap = create_heatmap(group, lat_min, lat_max, lon_min, lon_max, grid_size)
        if heatmap.max() > 0:
            heatmap = heatmap / heatmap.max()

        # Ground truth: lokacije vseh avtobusov
        on_bus = group[group['user_type'] == 'on_bus']
        if len(on_bus) == 0:
            continue

        # Pridobi unikatne avtobuse
        if 'bus_id' in on_bus.columns:
            bus_ids = on_bus['bus_id'].unique()
        else:
            bus_ids = [0]  # Single bus

        # Inicializiraj output array
        y_sample = np.zeros((max_buses, 3))  # [lat, lon, exists]

        for i, bus_id in enumerate(bus_ids[:max_buses]):
            bus_data = on_bus[on_bus['bus_id'] == bus_id] if 'bus_id' in on_bus.columns else on_bus
            if len(bus_data) > 0:
                bus_lat = bus_data['bus_lat'].iloc[0]
                bus_lon = bus_data['bus_lon'].iloc[0]

                y_sample[i, 0] = (bus_lat - lat_min) / (lat_max - lat_min)
                y_sample[i, 1] = (bus_lon - lon_min) / (lon_max - lon_min)
                y_sample[i, 2] = 1.0  # Avtobus obstaja

        X_list.append(heatmap)
        y_list.append(y_sample)

    X = np.array(X_list).reshape(-1, 1, grid_size, grid_size)
    y = np.array(y_list)

    print(f"\nPripravljeno vzorcev: {len(X)}")
    print(f"  X shape: {X.shape}")
    print(f"  y shape: {y.shape}")

    metadata = {
        'lat_min': lat_min,
        'lat_max': lat_max,
        'lon_min': lon_min,
        'lon_max': lon_max,
        'grid_size': grid_size,
        'max_buses': max_buses,
        'num_samples': len(X),
        'input_type': 'all_users',
        'output_type': 'multi_bus'
    }

    return X, y, metadata


def prepare_segmentation_training_data(route, grid_size=64):
    """
    Pripravi treningske podatke za segmentacijski pristop.
    Target je heatmap z 1 na lokacijah avtobusov.

    Returns:
        X: input heatmaps (N, 1, grid_size, grid_size)
        y: target heatmaps (N, 1, grid_size, grid_size) - 1 kjer so avtobusi
        metadata: slovar z informacijami
    """
    print(f"\nPriprava SEGMENTACIJSKIH treningskih podatkov za {route}...")

    df = load_simulation_data(route)
    if df is None:
        return None, None, None

    margin = 0.002
    lat_min = df['lat'].min() - margin
    lat_max = df['lat'].max() + margin
    lon_min = df['lon'].min() - margin
    lon_max = df['lon'].max() + margin

    lat_step = (lat_max - lat_min) / grid_size
    lon_step = (lon_max - lon_min) / grid_size

    timestamps = sorted(df['timestamp'].unique())

    X_list = []
    y_list = []

    for ts in timestamps:
        group = df[df['timestamp'] == ts]

        if len(group) < MIN_USERS_THRESHOLD:
            continue

        # Input: heatmap VSEH uporabnikov
        heatmap = create_heatmap(group, lat_min, lat_max, lon_min, lon_max, grid_size)
        if heatmap.max() > 0:
            heatmap = heatmap / heatmap.max()

        # Target: heatmap samo na lokacijah avtobusov
        target = np.zeros((grid_size, grid_size), dtype=np.float32)

        on_bus = group[group['user_type'] == 'on_bus']
        if len(on_bus) == 0:
            continue

        # Oznaci lokacije avtobusov v target heatmapu
        if 'bus_id' in on_bus.columns:
            bus_ids = on_bus['bus_id'].unique()
        else:
            bus_ids = [0]

        for bus_id in bus_ids:
            bus_data = on_bus[on_bus['bus_id'] == bus_id] if 'bus_id' in on_bus.columns else on_bus
            if len(bus_data) > 0:
                bus_lat = bus_data['bus_lat'].iloc[0]
                bus_lon = bus_data['bus_lon'].iloc[0]

                lat_idx = int((bus_lat - lat_min) / lat_step)
                lon_idx = int((bus_lon - lon_min) / lon_step)
                lat_idx = max(0, min(grid_size - 1, lat_idx))
                lon_idx = max(0, min(grid_size - 1, lon_idx))

                # Gaussian blob okoli lokacije avtobusa
                for di in range(-2, 3):
                    for dj in range(-2, 3):
                        ni, nj = lat_idx + di, lon_idx + dj
                        if 0 <= ni < grid_size and 0 <= nj < grid_size:
                            dist = np.sqrt(di**2 + dj**2)
                            target[ni, nj] = max(target[ni, nj], np.exp(-dist / 1.5))

        X_list.append(heatmap)
        y_list.append(target)

    X = np.array(X_list).reshape(-1, 1, grid_size, grid_size)
    y = np.array(y_list).reshape(-1, 1, grid_size, grid_size)

    print(f"\nPripravljeno vzorcev: {len(X)}")
    print(f"  X shape: {X.shape}")
    print(f"  y shape: {y.shape}")

    metadata = {
        'lat_min': lat_min,
        'lat_max': lat_max,
        'lon_min': lon_min,
        'lon_max': lon_max,
        'grid_size': grid_size,
        'num_samples': len(X),
        'input_type': 'all_users',
        'output_type': 'segmentation'
    }

    return X, y, metadata


def train_heatmap_cnn(X, y, metadata, epochs=100, batch_size=32, lr=0.001):
    """Trenira CNN model na heatmap podatkih (single bus)."""
    print(f"\nTreniranje Heatmap CNN ({epochs} epochs)...")

    # Split
    X_train, X_temp, y_train, y_temp = train_test_split(X, y, test_size=0.3, random_state=42)
    X_val, X_test, y_val, y_test = train_test_split(X_temp, y_temp, test_size=0.5, random_state=42)

    print(f"Train: {len(X_train)}, Val: {len(X_val)}, Test: {len(X_test)}")

    # Tensors
    X_train_t = torch.FloatTensor(X_train)
    y_train_t = torch.FloatTensor(y_train)
    X_val_t = torch.FloatTensor(X_val)
    y_val_t = torch.FloatTensor(y_val)
    X_test_t = torch.FloatTensor(X_test)
    y_test_t = torch.FloatTensor(y_test)

    train_dataset = TensorDataset(X_train_t, y_train_t)
    val_dataset = TensorDataset(X_val_t, y_val_t)

    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=batch_size)

    # Model
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Device: {device}")

    model = HeatmapCNN(metadata['grid_size']).to(device)
    print(model)

    criterion = nn.MSELoss()
    optimizer = optim.Adam(model.parameters(), lr=lr)
    scheduler = optim.lr_scheduler.ReduceLROnPlateau(optimizer, 'min', patience=10, factor=0.5)

    history = {'loss': [], 'val_loss': [], 'mae': [], 'val_mae': []}
    best_val_loss = float('inf')

    for epoch in range(epochs):
        # Training
        model.train()
        train_loss = 0.0
        train_mae = 0.0

        for X_batch, y_batch in train_loader:
            X_batch, y_batch = X_batch.to(device), y_batch.to(device)

            optimizer.zero_grad()
            outputs = model(X_batch)
            loss = criterion(outputs, y_batch)
            loss.backward()
            optimizer.step()

            train_loss += loss.item() * X_batch.size(0)
            train_mae += torch.mean(torch.abs(outputs - y_batch)).item() * X_batch.size(0)

        train_loss /= len(train_loader.dataset)
        train_mae /= len(train_loader.dataset)

        # Validation
        model.eval()
        val_loss = 0.0
        val_mae = 0.0

        with torch.no_grad():
            for X_batch, y_batch in val_loader:
                X_batch, y_batch = X_batch.to(device), y_batch.to(device)
                outputs = model(X_batch)
                val_loss += criterion(outputs, y_batch).item() * X_batch.size(0)
                val_mae += torch.mean(torch.abs(outputs - y_batch)).item() * X_batch.size(0)

        val_loss /= len(val_loader.dataset)
        val_mae /= len(val_loader.dataset)

        scheduler.step(val_loss)

        history['loss'].append(train_loss)
        history['val_loss'].append(val_loss)
        history['mae'].append(train_mae)
        history['val_mae'].append(val_mae)

        if val_loss < best_val_loss:
            best_val_loss = val_loss
            best_model_state = model.state_dict().copy()

        if (epoch + 1) % 20 == 0 or epoch == 0:
            print(f"Epoch {epoch+1:3d}/{epochs} - loss: {train_loss:.6f} - mae: {train_mae:.4f} - val_loss: {val_loss:.6f} - val_mae: {val_mae:.4f}")

    # Restore best model
    model.load_state_dict(best_model_state)

    # Test evaluation
    model.eval()
    with torch.no_grad():
        X_test_d = X_test_t.to(device)
        y_test_pred = model(X_test_d).cpu().numpy()

    # Izracunaj napako v metrih
    lat_range = metadata['lat_max'] - metadata['lat_min']
    lon_range = metadata['lon_max'] - metadata['lon_min']

    # Pretvori normalizirane koordinate nazaj
    y_test_real = y_test.copy()
    y_test_real[:, 0] = y_test[:, 0] * lat_range + metadata['lat_min']
    y_test_real[:, 1] = y_test[:, 1] * lon_range + metadata['lon_min']

    y_pred_real = y_test_pred.copy()
    y_pred_real[:, 0] = y_test_pred[:, 0] * lat_range + metadata['lat_min']
    y_pred_real[:, 1] = y_test_pred[:, 1] * lon_range + metadata['lon_min']

    errors_m = np.sqrt((y_test_real[:, 0] - y_pred_real[:, 0])**2 +
                       (y_test_real[:, 1] - y_pred_real[:, 1])**2) * 111000

    print(f"\nTest MAE: {errors_m.mean():.1f} m")
    print(f"Test RMSE: {np.sqrt((errors_m**2).mean()):.1f} m")
    print(f"Test Median: {np.median(errors_m):.1f} m")

    return model, history, {
        'X_test': X_test,
        'y_test': y_test,
        'y_test_pred': y_test_pred,
        'y_test_real': y_test_real,
        'y_pred_real': y_pred_real,
        'errors_m': errors_m
    }


def train_segmentation_model(X, y, metadata, epochs=100, batch_size=16, lr=0.001):
    """Trenira segmentacijski model za multi-bus detekcijo."""
    print(f"\nTreniranje Segmentation Model ({epochs} epochs)...")

    X_train, X_temp, y_train, y_temp = train_test_split(X, y, test_size=0.3, random_state=42)
    X_val, X_test, y_val, y_test = train_test_split(X_temp, y_temp, test_size=0.5, random_state=42)

    print(f"Train: {len(X_train)}, Val: {len(X_val)}, Test: {len(X_test)}")

    train_dataset = TensorDataset(torch.FloatTensor(X_train), torch.FloatTensor(y_train))
    val_dataset = TensorDataset(torch.FloatTensor(X_val), torch.FloatTensor(y_val))

    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=batch_size)

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Device: {device}")

    model = MultiBusDetector(metadata['grid_size']).to(device)
    print(f"Model parametrov: {sum(p.numel() for p in model.parameters()):,}")

    criterion = nn.BCELoss()  # Binary cross entropy za segmentacijo
    optimizer = optim.Adam(model.parameters(), lr=lr)
    scheduler = optim.lr_scheduler.ReduceLROnPlateau(optimizer, 'min', patience=10, factor=0.5)

    history = {'loss': [], 'val_loss': [], 'iou': [], 'val_iou': []}
    best_val_loss = float('inf')
    best_model_state = None

    for epoch in range(epochs):
        model.train()
        train_loss = 0.0
        train_iou = 0.0

        for X_batch, y_batch in train_loader:
            X_batch, y_batch = X_batch.to(device), y_batch.to(device)

            optimizer.zero_grad()
            outputs = model(X_batch)
            loss = criterion(outputs, y_batch)
            loss.backward()
            optimizer.step()

            train_loss += loss.item() * X_batch.size(0)
            # IoU metrika
            pred_bin = (outputs > 0.5).float()
            intersection = (pred_bin * y_batch).sum()
            union = ((pred_bin + y_batch) > 0).float().sum()
            train_iou += (intersection / (union + 1e-6)).item() * X_batch.size(0)

        train_loss /= len(train_loader.dataset)
        train_iou /= len(train_loader.dataset)

        model.eval()
        val_loss = 0.0
        val_iou = 0.0

        with torch.no_grad():
            for X_batch, y_batch in val_loader:
                X_batch, y_batch = X_batch.to(device), y_batch.to(device)
                outputs = model(X_batch)
                val_loss += criterion(outputs, y_batch).item() * X_batch.size(0)
                pred_bin = (outputs > 0.5).float()
                intersection = (pred_bin * y_batch).sum()
                union = ((pred_bin + y_batch) > 0).float().sum()
                val_iou += (intersection / (union + 1e-6)).item() * X_batch.size(0)

        val_loss /= len(val_loader.dataset)
        val_iou /= len(val_loader.dataset)

        scheduler.step(val_loss)

        history['loss'].append(train_loss)
        history['val_loss'].append(val_loss)
        history['iou'].append(train_iou)
        history['val_iou'].append(val_iou)

        if val_loss < best_val_loss:
            best_val_loss = val_loss
            best_model_state = model.state_dict().copy()

        if (epoch + 1) % 20 == 0 or epoch == 0:
            print(f"Epoch {epoch+1:3d}/{epochs} - loss: {train_loss:.4f} - IoU: {train_iou:.4f} - val_loss: {val_loss:.4f} - val_IoU: {val_iou:.4f}")

    model.load_state_dict(best_model_state)

    # Test evaluation
    model.eval()
    with torch.no_grad():
        X_test_t = torch.FloatTensor(X_test).to(device)
        y_test_pred = model(X_test_t).cpu().numpy()

    print(f"\nTest rezultati:")
    pred_bin = (y_test_pred > 0.5).astype(float)
    intersection = (pred_bin * y_test).sum()
    union = ((pred_bin + y_test) > 0).astype(float).sum()
    test_iou = intersection / (union + 1e-6)
    print(f"  Test IoU: {test_iou:.4f}")

    return model, history, {
        'X_test': X_test,
        'y_test': y_test,
        'y_test_pred': y_test_pred,
        'test_iou': test_iou
    }


def train_multibus_regressor(X, y, metadata, epochs=100, batch_size=16, lr=0.001):
    """Trenira multi-bus regressor model."""
    print(f"\nTreniranje MultiBus Regressor ({epochs} epochs)...")

    X_train, X_temp, y_train, y_temp = train_test_split(X, y, test_size=0.3, random_state=42)
    X_val, X_test, y_val, y_test = train_test_split(X_temp, y_temp, test_size=0.5, random_state=42)

    print(f"Train: {len(X_train)}, Val: {len(X_val)}, Test: {len(X_test)}")

    train_dataset = TensorDataset(torch.FloatTensor(X_train), torch.FloatTensor(y_train))
    val_dataset = TensorDataset(torch.FloatTensor(X_val), torch.FloatTensor(y_val))

    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=batch_size)

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Device: {device}")

    max_buses = metadata.get('max_buses', MAX_BUSES)
    model = MultiBusRegressor(metadata['grid_size'], max_buses).to(device)
    print(f"Model parametrov: {sum(p.numel() for p in model.parameters()):,}")

    def custom_loss(pred, target):
        # pred/target: (batch, max_buses, 3) = [lat, lon, exists]
        pos_loss = nn.MSELoss()(pred[:, :, :2], target[:, :, :2])
        exists_loss = nn.BCELoss()(pred[:, :, 2], target[:, :, 2])
        # Utezi position loss glede na exists
        weighted_pos_loss = (pos_loss * target[:, :, 2].unsqueeze(-1)).mean()
        return weighted_pos_loss + exists_loss

    optimizer = optim.Adam(model.parameters(), lr=lr)
    scheduler = optim.lr_scheduler.ReduceLROnPlateau(optimizer, 'min', patience=10, factor=0.5)

    history = {'loss': [], 'val_loss': []}
    best_val_loss = float('inf')
    best_model_state = None

    for epoch in range(epochs):
        model.train()
        train_loss = 0.0

        for X_batch, y_batch in train_loader:
            X_batch, y_batch = X_batch.to(device), y_batch.to(device)

            optimizer.zero_grad()
            outputs = model(X_batch)
            loss = custom_loss(outputs, y_batch)
            loss.backward()
            optimizer.step()

            train_loss += loss.item() * X_batch.size(0)

        train_loss /= len(train_loader.dataset)

        model.eval()
        val_loss = 0.0

        with torch.no_grad():
            for X_batch, y_batch in val_loader:
                X_batch, y_batch = X_batch.to(device), y_batch.to(device)
                outputs = model(X_batch)
                val_loss += custom_loss(outputs, y_batch).item() * X_batch.size(0)

        val_loss /= len(val_loader.dataset)
        scheduler.step(val_loss)

        history['loss'].append(train_loss)
        history['val_loss'].append(val_loss)

        if val_loss < best_val_loss:
            best_val_loss = val_loss
            best_model_state = model.state_dict().copy()

        if (epoch + 1) % 20 == 0 or epoch == 0:
            print(f"Epoch {epoch+1:3d}/{epochs} - loss: {train_loss:.6f} - val_loss: {val_loss:.6f}")

    model.load_state_dict(best_model_state)

    # Test evaluation
    model.eval()
    with torch.no_grad():
        X_test_t = torch.FloatTensor(X_test).to(device)
        y_test_pred = model(X_test_t).cpu().numpy()

    # Izracunaj napake za detektirane avtobuse
    lat_range = metadata['lat_max'] - metadata['lat_min']
    lon_range = metadata['lon_max'] - metadata['lon_min']

    errors = []
    for i in range(len(y_test)):
        for j in range(max_buses):
            if y_test[i, j, 2] > 0.5:  # Avtobus obstaja
                true_lat = y_test[i, j, 0] * lat_range + metadata['lat_min']
                true_lon = y_test[i, j, 1] * lon_range + metadata['lon_min']
                pred_lat = y_test_pred[i, j, 0] * lat_range + metadata['lat_min']
                pred_lon = y_test_pred[i, j, 1] * lon_range + metadata['lon_min']
                error_m = np.sqrt((true_lat - pred_lat)**2 + (true_lon - pred_lon)**2) * 111000
                errors.append(error_m)

    errors = np.array(errors)
    print(f"\nTest rezultati za detektirane avtobuse:")
    print(f"  MAE: {errors.mean():.1f} m")
    print(f"  RMSE: {np.sqrt((errors**2).mean()):.1f} m")
    print(f"  Median: {np.median(errors):.1f} m")

    return model, history, {
        'X_test': X_test,
        'y_test': y_test,
        'y_test_pred': y_test_pred,
        'errors_m': errors
    }


def apply_clustering(users_df, eps=0.0003, min_samples=2):
    """
    Uporabi DBSCAN clustering za detekcijo gruc uporabnikov.

    Args:
        users_df: DataFrame z lat, lon stolpci
        eps: DBSCAN epsilon (v stopinjah, ~30m)
        min_samples: Minimalno stevilo tock v gruci

    Returns:
        Cluster labels za vsako tocko (-1 = sum)
    """
    coords = users_df[['lat', 'lon']].values
    clustering = DBSCAN(eps=eps, min_samples=min_samples).fit(coords)
    return clustering.labels_


def find_bus_cluster(users_df, cluster_labels):
    """
    Najdi gruco, ki je najverjetneje avtobus (najvecja gruca ali gruca z najboljsim signalom).

    Returns:
        Indeks gruče ali -1 če ni gruče
    """
    unique_labels = set(cluster_labels)
    unique_labels.discard(-1)  # Odstrani šum

    if not unique_labels:
        return -1

    # Najdi gruco z največjim številom točk
    best_cluster = -1
    best_size = 0

    for label in unique_labels:
        cluster_mask = cluster_labels == label
        cluster_size = cluster_mask.sum()

        if cluster_size > best_size:
            best_size = cluster_size
            best_cluster = label

    return best_cluster


def visualize_heatmap_predictions(df, model, metadata, route, model_type='single'):
    """
    Interaktivna vizualizacija heatmap napovedi.
    model_type: 'single', 'segmentation', 'multibus'
    """
    print("\nPripravlam interaktivno heatmap vizualizacijo...")
    print(f"Model tip: {model_type}")
    print("OPOMBA: Vhod je heatmap VSEH uporabnikov, model detektira avtobuse")

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    model = model.to(device)
    model.eval()

    timestamps = sorted(df['timestamp'].unique())

    # Precompute predictions
    predictions = {}
    for ts in timestamps:
        group = df[df['timestamp'] == ts]

        if len(group) < MIN_USERS_THRESHOLD:
            continue

        # Heatmap iz VSEH uporabnikov
        heatmap = create_heatmap(
            group,  # Vsi uporabniki, ne samo on_bus!
            metadata['lat_min'], metadata['lat_max'],
            metadata['lon_min'], metadata['lon_max'],
            metadata['grid_size']
        )
        if heatmap.max() > 0:
            heatmap = heatmap / heatmap.max()

        # Prediction
        with torch.no_grad():
            heatmap_t = torch.FloatTensor(heatmap).unsqueeze(0).unsqueeze(0).to(device)

            if model_type == 'segmentation':
                pred_mask = model(heatmap_t).cpu().numpy()[0, 0]
                # Najdi lokacije iz maske
                pred_locations = extract_bus_locations_from_mask(
                    pred_mask, metadata, threshold=0.3
                )
            elif model_type == 'multibus':
                pred_raw = model(heatmap_t).cpu().numpy()[0]
                pred_locations = []
                for j in range(pred_raw.shape[0]):
                    if pred_raw[j, 2] > 0.5:  # confidence threshold
                        lat = pred_raw[j, 0] * (metadata['lat_max'] - metadata['lat_min']) + metadata['lat_min']
                        lon = pred_raw[j, 1] * (metadata['lon_max'] - metadata['lon_min']) + metadata['lon_min']
                        pred_locations.append((lat, lon, pred_raw[j, 2]))
                pred_mask = None
            else:  # single
                pred = model(heatmap_t).cpu().numpy()[0]
                pred_lat = pred[0] * (metadata['lat_max'] - metadata['lat_min']) + metadata['lat_min']
                pred_lon = pred[1] * (metadata['lon_max'] - metadata['lon_min']) + metadata['lon_min']
                pred_locations = [(pred_lat, pred_lon, 1.0)]
                pred_mask = None

        # Ground truth lokacije avtobusov
        on_bus = group[group['user_type'] == 'on_bus']
        actual_buses = []
        if len(on_bus) > 0 and 'bus_lat' in on_bus.columns:
            if 'bus_id' in on_bus.columns:
                for bus_id in on_bus['bus_id'].unique():
                    bus_data = on_bus[on_bus['bus_id'] == bus_id]
                    actual_buses.append((bus_data['bus_lat'].iloc[0], bus_data['bus_lon'].iloc[0]))
            else:
                actual_buses.append((on_bus['bus_lat'].iloc[0], on_bus['bus_lon'].iloc[0]))

        predictions[ts] = {
            'heatmap': heatmap,
            'pred_locations': pred_locations,
            'pred_mask': pred_mask,
            'actual_buses': actual_buses,
            'all_users': group,
            'on_bus_users': on_bus
        }

    valid_timestamps = list(predictions.keys())
    print(f"Pripravljeno {len(valid_timestamps)} napovedi")

    # Visualization
    fig, axes = plt.subplots(1, 3 if model_type == 'segmentation' else 2, figsize=(18 if model_type == 'segmentation' else 14, 7))
    plt.subplots_adjust(bottom=0.2)

    ax_slider = plt.axes([0.2, 0.08, 0.6, 0.03])
    slider = Slider(ax_slider, 'Cas', 0, len(valid_timestamps) - 1, valinit=0, valstep=1)

    colors = {
        'on_bus': '#2196F3',
        'waiting_at_station': '#FF9800',
        'pedestrian': '#9C27B0',
        'nearby': '#607D8B'
    }

    def update(val):
        idx = int(slider.val)
        ts = valid_timestamps[idx]
        data = predictions[ts]

        # Panel 1: Vhodni heatmap
        axes[0].clear()
        axes[0].imshow(data['heatmap'], cmap='YlOrRd', origin='lower',
                       extent=[metadata['lon_min'], metadata['lon_max'],
                              metadata['lat_min'], metadata['lat_max']])
        axes[0].set_xlabel('Longitude')
        axes[0].set_ylabel('Latitude')
        axes[0].set_title(f'Vhod: Heatmap VSEH uporabnikov\n({len(data["all_users"])} uporabnikov)')

        # Oznaci prave lokacije na heatmapu
        for i, (blat, blon) in enumerate(data['actual_buses']):
            axes[0].scatter([blon], [blat], c='cyan', s=150, marker='o',
                           edgecolors='white', linewidth=2, zorder=10)

        # Panel 2 (ali 3 ce segmentacija): Geografski prikaz z napovedmi
        ax_geo = axes[2] if model_type == 'segmentation' else axes[1]
        ax_geo.clear()

        # Plot vseh uporabnikov po tipu
        for ut, color in colors.items():
            subset = data['all_users'][data['all_users']['user_type'] == ut]
            if len(subset) > 0:
                ax_geo.scatter(subset['lon'], subset['lat'], c=color, s=20, alpha=0.5,
                             label=f'{ut} ({len(subset)})')

        # CNN napovedi
        for i, pred in enumerate(data['pred_locations']):
            lat, lon = pred[0], pred[1]
            conf = pred[2] if len(pred) > 2 else 1.0
            ax_geo.scatter([lon], [lat], c='red', s=300, marker='*',
                          edgecolors='darkred', linewidth=2, zorder=10,
                          label=f'CNN Bus {i+1} ({conf:.0%})' if i < 4 else None)
            circle = plt.Circle((lon, lat), 0.0008, fill=False,
                                color='red', linewidth=2, linestyle='--', alpha=0.7)
            ax_geo.add_patch(circle)

        # Prave lokacije
        for i, (blat, blon) in enumerate(data['actual_buses']):
            ax_geo.scatter([blon], [blat], c='lime', s=200, marker='o',
                          edgecolors='darkgreen', linewidth=2, alpha=0.8, zorder=9,
                          label=f'Pravi Bus {i+1}' if i < 4 else None)

        # Izracunaj napako
        if data['actual_buses'] and data['pred_locations']:
            total_error = 0
            for pred in data['pred_locations']:
                min_error = float('inf')
                for actual in data['actual_buses']:
                    err = np.sqrt((pred[0] - actual[0])**2 + (pred[1] - actual[1])**2) * 111000
                    min_error = min(min_error, err)
                total_error += min_error
            avg_error = total_error / len(data['pred_locations'])
            ax_geo.set_title(f'CNN Napovedi vs Prave lokacije\nPovp. napaka: {avg_error:.1f} m')
        else:
            ax_geo.set_title('CNN Napovedi vs Prave lokacije')

        ax_geo.set_xlabel('Longitude')
        ax_geo.set_ylabel('Latitude')
        ax_geo.legend(loc='upper right', fontsize=7)
        ax_geo.grid(True, alpha=0.3)
        ax_geo.set_xlim(metadata['lon_min'], metadata['lon_max'])
        ax_geo.set_ylim(metadata['lat_min'], metadata['lat_max'])

        # Panel 2: Segmentacijska maska (ce je)
        if model_type == 'segmentation' and data['pred_mask'] is not None:
            axes[1].clear()
            axes[1].imshow(data['pred_mask'], cmap='hot', origin='lower',
                          extent=[metadata['lon_min'], metadata['lon_max'],
                                 metadata['lat_min'], metadata['lat_max']],
                          vmin=0, vmax=1)
            axes[1].set_xlabel('Longitude')
            axes[1].set_ylabel('Latitude')
            axes[1].set_title('Izhod: Verjetnostna maska avtobusov')

            # Oznaci detektirane lokacije
            for i, pred in enumerate(data['pred_locations']):
                axes[1].scatter([pred[1]], [pred[0]], c='cyan', s=200, marker='X',
                              edgecolors='white', linewidth=2, zorder=10)

        fig.canvas.draw_idle()

    slider.on_changed(update)
    update(0)

    fig.suptitle(f'CNN Detekcija Avtobusov - Linija {route} ({model_type})', fontsize=14, fontweight='bold')
    plt.show()


def extract_bus_locations_from_mask(mask, metadata, threshold=0.3):
    """Ekstrahiraj lokacije avtobusov iz segmentacijske maske."""
    # Threshold
    binary = (mask > threshold).astype(int)

    # Find connected components
    labeled, num_features = ndimage.label(binary)

    locations = []
    for i in range(1, num_features + 1):
        component = (labeled == i)
        if component.sum() < 2:  # Premajhna komponenta
            continue

        # Center of mass
        lat_idx, lon_idx = ndimage.center_of_mass(mask * component)

        # Convert to coordinates
        lat = metadata['lat_min'] + (lat_idx + 0.5) * (metadata['lat_max'] - metadata['lat_min']) / mask.shape[0]
        lon = metadata['lon_min'] + (lon_idx + 0.5) * (metadata['lon_max'] - metadata['lon_min']) / mask.shape[1]

        # Confidence = max value in component
        conf = (mask * component).max()

        locations.append((lat, lon, conf))

    # Sort by confidence
    locations.sort(key=lambda x: -x[2])

    return locations[:MAX_BUSES]


def save_heatmap_model(model, history, test_results, metadata, route):
    """Shrani heatmap CNN model in rezultate."""
    Path(RESULTS_FOLDER).mkdir(exist_ok=True)

    # Model
    model_path = f"{RESULTS_FOLDER}/{route}_heatmap_cnn.pt"
    torch.save({
        'model_state_dict': model.state_dict(),
        'metadata': metadata
    }, model_path)
    print(f"Model shranjen: {model_path}")

    # Metadata
    with open(f"{RESULTS_FOLDER}/{route}_heatmap_metadata.json", 'w') as f:
        json.dump(metadata, f, indent=2)

    # Evaluation
    eval_summary = {
        'test_mae_meters': float(test_results['errors_m'].mean()),
        'test_rmse_meters': float(np.sqrt((test_results['errors_m']**2).mean())),
        'test_median_meters': float(np.median(test_results['errors_m'])),
        'num_test_samples': len(test_results['errors_m'])
    }
    with open(f"{RESULTS_FOLDER}/{route}_heatmap_evaluation.json", 'w') as f:
        json.dump(eval_summary, f, indent=2)


def plot_heatmap_results(history, test_results, route):
    """Narisi rezultate heatmap CNN modela."""
    fig, axes = plt.subplots(2, 2, figsize=(14, 10))
    fig.suptitle(f'Heatmap CNN Rezultati - {route}', fontsize=14, fontweight='bold')

    # Loss
    axes[0, 0].plot(history['loss'], label='Train')
    axes[0, 0].plot(history['val_loss'], label='Val')
    axes[0, 0].set_xlabel('Epoch')
    axes[0, 0].set_ylabel('Loss')
    axes[0, 0].set_title('Loss')
    axes[0, 0].legend()
    axes[0, 0].grid(True, alpha=0.3)

    # MAE
    axes[0, 1].plot(history['mae'], label='Train')
    axes[0, 1].plot(history['val_mae'], label='Val')
    axes[0, 1].set_xlabel('Epoch')
    axes[0, 1].set_ylabel('MAE (normalized)')
    axes[0, 1].set_title('MAE')
    axes[0, 1].legend()
    axes[0, 1].grid(True, alpha=0.3)

    # Geographic comparison
    y_test_real = test_results['y_test_real']
    y_pred_real = test_results['y_pred_real']

    axes[1, 0].scatter(y_test_real[:, 1], y_test_real[:, 0],
                      c='green', alpha=0.6, s=50, label='Prava', edgecolors='black')
    axes[1, 0].scatter(y_pred_real[:, 1], y_pred_real[:, 0],
                      c='red', alpha=0.6, s=50, label='Napoved', marker='x')
    axes[1, 0].set_xlabel('Longitude')
    axes[1, 0].set_ylabel('Latitude')
    axes[1, 0].set_title('Geografski prikaz')
    axes[1, 0].legend()
    axes[1, 0].grid(True, alpha=0.3)

    # Error histogram
    errors = test_results['errors_m']
    axes[1, 1].hist(errors, bins=30, color='orange', edgecolor='black')
    axes[1, 1].axvline(errors.mean(), color='red', linestyle='--',
                      label=f'Povprecje: {errors.mean():.1f}m')
    axes[1, 1].axvline(np.median(errors), color='blue', linestyle='--',
                      label=f'Median: {np.median(errors):.1f}m')
    axes[1, 1].set_xlabel('Napaka (m)')
    axes[1, 1].set_ylabel('Frekvenca')
    axes[1, 1].set_title('Razporeditev napak')
    axes[1, 1].legend()
    axes[1, 1].grid(True, alpha=0.3)

    plt.tight_layout()
    plt.savefig(f"{RESULTS_FOLDER}/{route}_heatmap_results.png", dpi=150, bbox_inches='tight')
    print(f"Graf shranjen: {RESULTS_FOLDER}/{route}_heatmap_results.png")
    plt.show()


def save_model(model, history, test_results, metadata, route, model_type='single'):
    """Shrani model in rezultate."""
    Path(RESULTS_FOLDER).mkdir(exist_ok=True)

    suffix = f"_{model_type}" if model_type != 'single' else ""

    # Model
    model_path = f"{RESULTS_FOLDER}/{route}_heatmap_cnn{suffix}.pt"
    torch.save({
        'model_state_dict': model.state_dict(),
        'metadata': metadata,
        'model_type': model_type
    }, model_path)
    print(f"Model shranjen: {model_path}")

    # Metadata
    with open(f"{RESULTS_FOLDER}/{route}_heatmap_metadata{suffix}.json", 'w') as f:
        json.dump(metadata, f, indent=2)

    # History
    with open(f"{RESULTS_FOLDER}/{route}_history{suffix}.pkl", 'wb') as f:
        pickle.dump(history, f)


def load_trained_model(route, model_type='single'):
    """Nalozi shranjen model."""
    suffix = f"_{model_type}" if model_type != 'single' else ""
    model_path = f"{RESULTS_FOLDER}/{route}_heatmap_cnn{suffix}.pt"

    if not Path(model_path).exists():
        print(f"Model ne obstaja: {model_path}")
        return None, None

    checkpoint = torch.load(model_path, map_location='cpu')
    metadata = checkpoint['metadata']

    if model_type == 'segmentation':
        model = MultiBusDetector(metadata['grid_size'])
    elif model_type == 'multibus':
        model = MultiBusRegressor(metadata['grid_size'], metadata.get('max_buses', MAX_BUSES))
    else:
        model = HeatmapCNN(metadata['grid_size'])

    model.load_state_dict(checkpoint['model_state_dict'])
    print(f"Model nalozen: {model_path}")

    return model, metadata


def main():
    print("=" * 70)
    print("CNN DETEKCIJA AVTOBUSOV IZ DISKRETIZIRANEGA HEATMAPA")
    print("=" * 70)
    print()
    print("Vhod:  Heatmap VSEH GPS lokacij (on_bus, pedestrian, nearby, station)")
    print("Izhod: Lokacije avtobusov (kje so osebe na avtobusu)")
    print()
    print("Nacini delovanja:")
    print("  1. Single Bus   - Napoved ene lokacije (regresija lat/lon)")
    print("  2. Multi-Bus    - Napoved vec lokacij (regresija N * [lat, lon, conf])")
    print("  3. Segmentation - Verjetnostna maska lokacij avtobusov")
    print("=" * 70)

    # Izbira linije
    csv_files = glob.glob(f"{DATA_FOLDER}/*_multibus_*.csv") + glob.glob(f"{DATA_FOLDER}/*_all_*.csv")
    if not csv_files:
        print("\nNi simulacijskih podatkov!")
        print("Zazeni: python3 generate_multibus.py")
        return

    routes = set()
    for f in sorted(csv_files):
        route = f.split('/')[-1].split('_')[0]
        routes.add(route)

    print(f"\nDostopne linije: {', '.join(sorted(routes))}")
    route = input("Vnesi linijo (npr. G1): ").strip().upper()

    if route not in routes:
        print(f"Linija {route} ni dostopna")
        return

    # Izbira nacina
    print("\nIzberi nacin:")
    print("  1 - Single Bus (privzeto)")
    print("  2 - Multi-Bus Regressor")
    print("  3 - Segmentation")
    print("  4 - Samo vizualizacija (nalozi obstojecih model)")

    mode = input("Izbira [1]: ").strip() or "1"

    if mode == "4":
        # Samo vizualizacija
        print("\nKateri model naloziti?")
        print("  1 - Single Bus")
        print("  2 - Multi-Bus Regressor")
        print("  3 - Segmentation")
        model_choice = input("Izbira [1]: ").strip() or "1"
        model_type = {'1': 'single', '2': 'multibus', '3': 'segmentation'}.get(model_choice, 'single')

        model, metadata = load_trained_model(route, model_type)
        if model is None:
            print("Model ne obstaja. Najprej treniraj model.")
            return

        df = load_simulation_data(route)
        visualize_heatmap_predictions(df, model, metadata, route, model_type)
        return

    # Treniranje
    epochs = int(input("Stevilo epoch [100]: ").strip() or "100")

    if mode == "1":
        # Single bus
        X, y, metadata = prepare_heatmap_training_data(route, grid_size=GRID_SIZE)
        if X is None:
            return
        model, history, test_results = train_heatmap_cnn(X, y, metadata, epochs=epochs)
        save_model(model, history, test_results, metadata, route, 'single')
        plot_heatmap_results(history, test_results, route)
        model_type = 'single'

    elif mode == "2":
        # Multi-bus regressor
        X, y, metadata = prepare_multibus_training_data(route, grid_size=GRID_SIZE, max_buses=MAX_BUSES)
        if X is None:
            return
        model, history, test_results = train_multibus_regressor(X, y, metadata, epochs=epochs)
        save_model(model, history, test_results, metadata, route, 'multibus')
        model_type = 'multibus'

    elif mode == "3":
        # Segmentation
        X, y, metadata = prepare_segmentation_training_data(route, grid_size=GRID_SIZE)
        if X is None:
            return
        model, history, test_results = train_segmentation_model(X, y, metadata, epochs=epochs)
        save_model(model, history, test_results, metadata, route, 'segmentation')
        model_type = 'segmentation'

    else:
        print("Neveljavna izbira")
        return

    # Interaktivna vizualizacija
    print("\nZaganjam interaktivno vizualizacijo...")
    df = load_simulation_data(route)
    visualize_heatmap_predictions(df, model, metadata, route, model_type)

    print("\n" + "=" * 70)
    print("ZAKLJUCENO")
    print("=" * 70)


if __name__ == "__main__":
    main()
