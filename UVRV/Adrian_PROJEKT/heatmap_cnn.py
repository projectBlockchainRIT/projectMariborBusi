#!/usr/bin/env python3
"""
Heatmap-based CNN pristop za napoved lokacije avtobusa.
- Diskretizira GPS koordinate v 2D grid (heatmap)
- CNN napove verjetnost avtobusa v vsaki celici
- Opcijsko clustering za detekcijo premikajocih se skupin

Originalna zasnova projekta.
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

import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset

DATA_FOLDER = "data"
TRAINING_FOLDER = "training_data"
RESULTS_FOLDER = "results"

# Grid parametri
GRID_SIZE = 32  # 32x32 grid
MIN_USERS_THRESHOLD = 2  # Minimalno stevilo uporabnikov za veljaven vzorec


class HeatmapCNN(nn.Module):
    """CNN za napoved lokacije avtobusa iz heatmap vhoda."""
    def __init__(self, grid_size=32):
        super().__init__()
        self.grid_size = grid_size

        # Convolutional layers
        self.conv_layers = nn.Sequential(
            # Input: 1 x 32 x 32
            nn.Conv2d(1, 32, kernel_size=3, padding=1),
            nn.BatchNorm2d(32),
            nn.ReLU(),
            nn.MaxPool2d(2),  # -> 32 x 16 x 16

            nn.Conv2d(32, 64, kernel_size=3, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU(),
            nn.MaxPool2d(2),  # -> 64 x 8 x 8

            nn.Conv2d(64, 128, kernel_size=3, padding=1),
            nn.BatchNorm2d(128),
            nn.ReLU(),
            nn.MaxPool2d(2),  # -> 128 x 4 x 4
        )

        # Fully connected layers
        self.fc_layers = nn.Sequential(
            nn.Flatten(),
            nn.Linear(128 * 4 * 4, 256),
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


def load_simulation_data(route):
    """Nalozi podatke simulacije."""
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


def prepare_heatmap_training_data(route, grid_size=32):
    """
    Pripravi treningske podatke z heatmap reprezentacijo.

    Returns:
        X: heatmaps (N, 1, grid_size, grid_size)
        y: normalized bus positions (N, 2) v grid koordinatah
        metadata: slovar z informacijami
    """
    print(f"\nPriprava heatmap treningskih podatkov za {route}...")

    df = load_simulation_data(route)
    if df is None:
        return None, None, None

    # Doloci geografske meje
    lat_min = df['lat'].min() - 0.001
    lat_max = df['lat'].max() + 0.001
    lon_min = df['lon'].min() - 0.001
    lon_max = df['lon'].max() + 0.001

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

        # Filtriraj samo on_bus uporabnike
        on_bus_users = group[group['user_type'] == 'on_bus']

        if len(on_bus_users) < MIN_USERS_THRESHOLD:
            continue

        # Ustvari heatmap
        heatmap = create_heatmap(
            on_bus_users, lat_min, lat_max, lon_min, lon_max, grid_size
        )

        # Normaliziraj heatmap
        if heatmap.max() > 0:
            heatmap = heatmap / heatmap.max()

        # Pridobi ground truth bus lokacijo
        if 'bus_lat' not in group.columns:
            continue

        bus_lat = group.iloc[0]['bus_lat']
        bus_lon = group.iloc[0]['bus_lon']

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
        'num_samples': len(X)
    }

    return X, y, metadata


def train_heatmap_cnn(X, y, metadata, epochs=100, batch_size=32, lr=0.001):
    """Trenira CNN model na heatmap podatkih."""
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


def visualize_heatmap_predictions(df, model, metadata, route):
    """Interaktivna vizualizacija heatmap napovedi s clusteringom."""
    print("\nPripravlam interaktivno heatmap vizualizacijo...")

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    model = model.to(device)
    model.eval()

    timestamps = sorted(df['timestamp'].unique())

    # Precompute predictions
    predictions = {}
    for ts in timestamps:
        group = df[df['timestamp'] == ts]
        on_bus_users = group[group['user_type'] == 'on_bus']

        if len(on_bus_users) < MIN_USERS_THRESHOLD:
            continue

        # Heatmap
        heatmap = create_heatmap(
            on_bus_users,
            metadata['lat_min'], metadata['lat_max'],
            metadata['lon_min'], metadata['lon_max'],
            metadata['grid_size']
        )
        if heatmap.max() > 0:
            heatmap = heatmap / heatmap.max()

        # Prediction
        with torch.no_grad():
            heatmap_t = torch.FloatTensor(heatmap).unsqueeze(0).unsqueeze(0).to(device)
            pred = model(heatmap_t).cpu().numpy()[0]

        # Denormalize
        pred_lat = pred[0] * (metadata['lat_max'] - metadata['lat_min']) + metadata['lat_min']
        pred_lon = pred[1] * (metadata['lon_max'] - metadata['lon_min']) + metadata['lon_min']

        # Clustering
        cluster_labels = apply_clustering(on_bus_users)
        bus_cluster = find_bus_cluster(on_bus_users, cluster_labels)

        # Ground truth
        actual_bus_lat = group.iloc[0]['bus_lat'] if 'bus_lat' in group.columns else None
        actual_bus_lon = group.iloc[0]['bus_lon'] if 'bus_lon' in group.columns else None

        predictions[ts] = {
            'heatmap': heatmap,
            'pred_lat': pred_lat,
            'pred_lon': pred_lon,
            'actual_bus_lat': actual_bus_lat,
            'actual_bus_lon': actual_bus_lon,
            'users': on_bus_users,
            'cluster_labels': cluster_labels,
            'bus_cluster': bus_cluster
        }

    valid_timestamps = list(predictions.keys())
    print(f"Pripravljeno {len(valid_timestamps)} napovedi")

    # Visualization
    fig, axes = plt.subplots(1, 2, figsize=(16, 7))
    plt.subplots_adjust(bottom=0.2)

    # Slider
    ax_slider = plt.axes([0.2, 0.08, 0.6, 0.03])
    slider = Slider(ax_slider, 'Cas', 0, len(valid_timestamps) - 1, valinit=0, valstep=1)

    def update(val):
        idx = int(slider.val)
        ts = valid_timestamps[idx]
        data = predictions[ts]

        # Left plot: Heatmap
        axes[0].clear()
        im = axes[0].imshow(data['heatmap'], cmap='YlOrRd', origin='lower',
                           extent=[metadata['lon_min'], metadata['lon_max'],
                                  metadata['lat_min'], metadata['lat_max']])
        axes[0].set_xlabel('Longitude')
        axes[0].set_ylabel('Latitude')
        axes[0].set_title(f'Heatmap gostote uporabnikov\nCas: {ts}')

        # Right plot: Geographic view with clusters
        axes[1].clear()

        users = data['users']
        labels = data['cluster_labels']

        # Plot clusters
        unique_labels = set(labels)
        colors = plt.cm.tab10(np.linspace(0, 1, len(unique_labels)))

        for label, color in zip(unique_labels, colors):
            mask = labels == label
            if label == -1:
                axes[1].scatter(users.loc[mask, 'lon'], users.loc[mask, 'lat'],
                              c='gray', s=30, alpha=0.3, label='Sum')
            elif label == data['bus_cluster']:
                axes[1].scatter(users.loc[mask, 'lon'], users.loc[mask, 'lat'],
                              c='blue', s=100, alpha=0.7, edgecolors='black',
                              label=f'Gruca {label} (avtobus)')
            else:
                axes[1].scatter(users.loc[mask, 'lon'], users.loc[mask, 'lat'],
                              c=[color], s=50, alpha=0.5, label=f'Gruca {label}')

        # CNN prediction
        axes[1].scatter([data['pred_lon']], [data['pred_lat']],
                       c='red', s=300, marker='*', edgecolors='darkred',
                       linewidth=2, label='CNN napoved', zorder=10)

        # Ground truth
        if data['actual_bus_lat'] is not None:
            axes[1].scatter([data['actual_bus_lon']], [data['actual_bus_lat']],
                           c='green', s=200, marker='o', edgecolors='darkgreen',
                           linewidth=2, alpha=0.7, label='Prava lokacija', zorder=9)

            error = np.sqrt((data['pred_lat'] - data['actual_bus_lat'])**2 +
                           (data['pred_lon'] - data['actual_bus_lon'])**2) * 111000
            axes[1].set_title(f'Geografski prikaz s clustering\nNapaka: {error:.1f} m')
        else:
            axes[1].set_title('Geografski prikaz s clustering')

        axes[1].set_xlabel('Longitude')
        axes[1].set_ylabel('Latitude')
        axes[1].legend(loc='upper right', fontsize=8)
        axes[1].grid(True, alpha=0.3)

        # Set same limits
        axes[1].set_xlim(metadata['lon_min'], metadata['lon_max'])
        axes[1].set_ylim(metadata['lat_min'], metadata['lat_max'])

        fig.canvas.draw_idle()

    slider.on_changed(update)
    update(0)

    fig.suptitle(f'Heatmap CNN + Clustering - Linija {route}', fontsize=14, fontweight='bold')
    plt.show()


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


def main():
    print("HEATMAP CNN + CLUSTERING")
    print("=" * 70)
    print("Originalna zasnova projekta:")
    print("  1. Diskretizacija GPS -> 2D grid (heatmap)")
    print("  2. CNN napove lokacijo avtobusa")
    print("  3. DBSCAN clustering za detekcijo gruc")
    print("=" * 70)

    # Izbira linije
    csv_files = glob.glob(f"{DATA_FOLDER}/*_all_*.csv")
    if not csv_files:
        print("Ni simulacijskih podatkov!")
        return

    print("\nDostopne linije:")
    routes = set()
    for f in sorted(csv_files):
        route = f.split('/')[-1].split('_')[0]
        routes.add(route)
        print(f"   {route}")

    route = input("\nVnesi linijo (npr. G1): ").strip().upper()

    if route not in routes:
        print(f"Linija {route} ni dostopna")
        return

    # Priprava podatkov
    X, y, metadata = prepare_heatmap_training_data(route, grid_size=GRID_SIZE)
    if X is None:
        return

    # Treniranje
    model, history, test_results = train_heatmap_cnn(X, y, metadata, epochs=100, batch_size=32)

    # Shrani
    save_heatmap_model(model, history, test_results, metadata, route)

    # Grafi
    plot_heatmap_results(history, test_results, route)

    # Interaktivna vizualizacija
    print("\nZaganjam interaktivno vizualizacijo...")
    df = load_simulation_data(route)
    visualize_heatmap_predictions(df, model, metadata, route)

    print("\n" + "=" * 70)
    print("ZAKLJUCENO")
    print("=" * 70)


if __name__ == "__main__":
    main()
