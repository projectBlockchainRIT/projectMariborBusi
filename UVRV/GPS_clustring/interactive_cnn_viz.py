#!/usr/bin/env python3
"""
Interaktivna CNN vizualizacija - 2 panela.
Slider za premikanje skozi čas.

Uporaba: python3 interactive_cnn_viz.py
"""

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.widgets import Slider
import torch
import torch.nn as nn
from scipy import ndimage

GRID_SIZE = 128  # Večja resolucija za boljšo natančnost

# ============================================================
# IZBOLJŠAN MODEL
# ============================================================

class ImprovedDetector(nn.Module):
    """Globlja CNN z residual povezavami za boljšo natančnost."""
    def __init__(self):
        super().__init__()

        # Encoder
        self.enc1 = nn.Sequential(
            nn.Conv2d(1, 64, 3, padding=1), nn.BatchNorm2d(64), nn.ReLU(),
            nn.Conv2d(64, 64, 3, padding=1), nn.BatchNorm2d(64), nn.ReLU(),
        )
        self.pool1 = nn.MaxPool2d(2)

        self.enc2 = nn.Sequential(
            nn.Conv2d(64, 128, 3, padding=1), nn.BatchNorm2d(128), nn.ReLU(),
            nn.Conv2d(128, 128, 3, padding=1), nn.BatchNorm2d(128), nn.ReLU(),
        )
        self.pool2 = nn.MaxPool2d(2)

        self.enc3 = nn.Sequential(
            nn.Conv2d(128, 256, 3, padding=1), nn.BatchNorm2d(256), nn.ReLU(),
            nn.Conv2d(256, 256, 3, padding=1), nn.BatchNorm2d(256), nn.ReLU(),
        )
        self.pool3 = nn.MaxPool2d(2)

        # Bottleneck
        self.bottleneck = nn.Sequential(
            nn.Conv2d(256, 512, 3, padding=1), nn.BatchNorm2d(512), nn.ReLU(),
            nn.Conv2d(512, 512, 3, padding=1), nn.BatchNorm2d(512), nn.ReLU(),
        )

        # Decoder with skip connections
        self.up3 = nn.ConvTranspose2d(512, 256, 2, stride=2)
        self.dec3 = nn.Sequential(
            nn.Conv2d(512, 256, 3, padding=1), nn.BatchNorm2d(256), nn.ReLU(),
            nn.Conv2d(256, 256, 3, padding=1), nn.BatchNorm2d(256), nn.ReLU(),
        )

        self.up2 = nn.ConvTranspose2d(256, 128, 2, stride=2)
        self.dec2 = nn.Sequential(
            nn.Conv2d(256, 128, 3, padding=1), nn.BatchNorm2d(128), nn.ReLU(),
            nn.Conv2d(128, 128, 3, padding=1), nn.BatchNorm2d(128), nn.ReLU(),
        )

        self.up1 = nn.ConvTranspose2d(128, 64, 2, stride=2)
        self.dec1 = nn.Sequential(
            nn.Conv2d(128, 64, 3, padding=1), nn.BatchNorm2d(64), nn.ReLU(),
            nn.Conv2d(64, 64, 3, padding=1), nn.BatchNorm2d(64), nn.ReLU(),
        )

        self.final = nn.Sequential(
            nn.Conv2d(64, 32, 3, padding=1), nn.ReLU(),
            nn.Conv2d(32, 1, 1),
            nn.Sigmoid()
        )

    def forward(self, x):
        # Encoder
        e1 = self.enc1(x)
        e2 = self.enc2(self.pool1(e1))
        e3 = self.enc3(self.pool2(e2))

        # Bottleneck
        b = self.bottleneck(self.pool3(e3))

        # Decoder with skip connections
        d3 = self.dec3(torch.cat([self.up3(b), e3], dim=1))
        d2 = self.dec2(torch.cat([self.up2(d3), e2], dim=1))
        d1 = self.dec1(torch.cat([self.up1(d2), e1], dim=1))

        return self.final(d1)


# ============================================================
# POMOŽNE FUNKCIJE
# ============================================================

def create_heatmap(df, lat_min, lat_max, lon_min, lon_max):
    """Ustvari heatmap iz GPS lokacij."""
    heatmap = np.zeros((GRID_SIZE, GRID_SIZE), dtype=np.float32)

    lat_scale = GRID_SIZE / (lat_max - lat_min)
    lon_scale = GRID_SIZE / (lon_max - lon_min)

    lats = np.clip(((df['lat'].values - lat_min) * lat_scale).astype(int), 0, GRID_SIZE-1)
    lons = np.clip(((df['lon'].values - lon_min) * lon_scale).astype(int), 0, GRID_SIZE-1)
    weights = df['signal_strength'].values / 100.0

    for i in range(len(lats)):
        heatmap[lats[i], lons[i]] += weights[i]

    return heatmap


def extract_locations(mask, lat_min, lat_max, lon_min, lon_max, threshold=0.4):
    """Ekstrahiraj lokacije iz maske."""
    binary = (mask > threshold).astype(int)
    labeled, num = ndimage.label(binary)

    locations = []
    for i in range(1, num + 1):
        comp = (labeled == i)
        if comp.sum() < 3:
            continue
        li, lo = ndimage.center_of_mass(mask * comp)
        lat = lat_min + (li + 0.5) * (lat_max - lat_min) / GRID_SIZE
        lon = lon_min + (lo + 0.5) * (lon_max - lon_min) / GRID_SIZE
        conf = (mask * comp).max()
        locations.append((lat, lon, conf))

    locations.sort(key=lambda x: -x[2])
    return locations[:6]


def prepare_data(df, lat_min, lat_max, lon_min, lon_max):
    """Pripravi treningske podatke."""
    timestamps = sorted(df['timestamp'].unique())
    X_list, y_list = [], []

    lat_step = (lat_max - lat_min) / GRID_SIZE
    lon_step = (lon_max - lon_min) / GRID_SIZE

    for ts in timestamps:
        group = df[df['timestamp'] == ts]

        heatmap = create_heatmap(group, lat_min, lat_max, lon_min, lon_max)
        if heatmap.max() > 0:
            heatmap = heatmap / heatmap.max()

        # Target maska
        target = np.zeros((GRID_SIZE, GRID_SIZE), dtype=np.float32)
        on_bus = group[group['user_type'] == 'on_bus']

        for bus_id in on_bus['bus_id'].unique():
            if bus_id >= 0:
                bd = on_bus[on_bus['bus_id'] == bus_id]
                if len(bd) > 0:
                    bus_lat, bus_lon = bd['bus_lat'].iloc[0], bd['bus_lon'].iloc[0]
                    lat_idx = int(np.clip((bus_lat - lat_min) / lat_step, 0, GRID_SIZE-1))
                    lon_idx = int(np.clip((bus_lon - lon_min) / lon_step, 0, GRID_SIZE-1))

                    # Večji Gaussian za 128x128
                    for di in range(-5, 6):
                        for dj in range(-5, 6):
                            ni, nj = lat_idx + di, lon_idx + dj
                            if 0 <= ni < GRID_SIZE and 0 <= nj < GRID_SIZE:
                                dist = np.sqrt(di**2 + dj**2)
                                target[ni, nj] = max(target[ni, nj], np.exp(-dist/3))

        X_list.append(heatmap)
        y_list.append(target)

    return np.array(X_list).reshape(-1, 1, GRID_SIZE, GRID_SIZE), \
           np.array(y_list).reshape(-1, 1, GRID_SIZE, GRID_SIZE)


def train_model(X, y, epochs=150):
    """Treniraj z boljšimi nastavitvami."""
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    model = ImprovedDetector().to(device)

    n = len(X)
    idx = np.random.permutation(n)
    split = int(0.85 * n)

    X_train = torch.FloatTensor(X[idx[:split]]).to(device)
    y_train = torch.FloatTensor(y[idx[:split]]).to(device)
    X_val = torch.FloatTensor(X[idx[split:]]).to(device)
    y_val = torch.FloatTensor(y[idx[split:]]).to(device)

    # Focal loss za boljše učenje
    def focal_loss(pred, target, gamma=2):
        bce = nn.functional.binary_cross_entropy(pred, target, reduction='none')
        pt = torch.exp(-bce)
        return ((1 - pt) ** gamma * bce).mean()

    optimizer = torch.optim.AdamW(model.parameters(), lr=0.001, weight_decay=0.01)
    scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, epochs)

    print(f"Treniranje na {device}...")
    print(f"Train: {len(X_train)}, Val: {len(X_val)}")
    print(f"Model parametrov: {sum(p.numel() for p in model.parameters()):,}")

    best_loss = float('inf')
    best_state = None

    for epoch in range(epochs):
        model.train()

        # Mini-batch training
        batch_size = 16
        perm = torch.randperm(len(X_train))
        total_loss = 0

        for i in range(0, len(X_train), batch_size):
            batch_idx = perm[i:i+batch_size]

            optimizer.zero_grad()
            pred = model(X_train[batch_idx])
            loss = focal_loss(pred, y_train[batch_idx])
            loss.backward()
            optimizer.step()
            total_loss += loss.item()

        scheduler.step()

        # Validation
        if (epoch + 1) % 10 == 0:
            model.eval()
            with torch.no_grad():
                val_pred = model(X_val)
                val_loss = focal_loss(val_pred, y_val).item()

            if val_loss < best_loss:
                best_loss = val_loss
                best_state = {k: v.cpu().clone() for k, v in model.state_dict().items()}

            print(f"  Epoch {epoch+1:3d}/{epochs} - loss: {total_loss/(len(X_train)//batch_size):.4f} - val_loss: {val_loss:.4f}")

    if best_state:
        model.load_state_dict(best_state)

    return model.cpu()


# ============================================================
# GLAVNA FUNKCIJA
# ============================================================

def main():
    print("="*70)
    print("CNN DETEKCIJA AVTOBUSOV - IZBOLJŠAN MODEL")
    print("="*70)

    # Naloži podatke
    print("\nNalagam podatke...")
    try:
        df = pd.read_csv('data/G1_dense.csv')
    except:
        df = pd.read_csv('data/G1_multibus_1768978813.csv')

    print(f"GPS točk: {len(df):,}")
    print(f"Timestampov: {df['timestamp'].nunique()}")
    print(f"Povprečno uporabnikov: {len(df) // df['timestamp'].nunique()}")

    margin = 0.003
    lat_min, lat_max = df['lat'].min() - margin, df['lat'].max() + margin
    lon_min, lon_max = df['lon'].min() - margin, df['lon'].max() + margin

    # Pripravi podatke
    print(f"\nPriprava podatkov (grid: {GRID_SIZE}x{GRID_SIZE})...")
    X, y = prepare_data(df, lat_min, lat_max, lon_min, lon_max)
    print(f"X: {X.shape}, y: {y.shape}")

    # Treniraj
    print("\n" + "="*70)
    print("TRENIRANJE")
    print("="*70)
    model = train_model(X, y, epochs=150)
    model.eval()

    # Evalvacija
    print("\n" + "="*70)
    print("EVALVACIJA")
    print("="*70)

    timestamps = sorted(df['timestamp'].unique())
    errors = []

    for ts in timestamps:
        group = df[df['timestamp'] == ts]
        heatmap = create_heatmap(group, lat_min, lat_max, lon_min, lon_max)
        if heatmap.max() > 0:
            heatmap = heatmap / heatmap.max()

        with torch.no_grad():
            pred_mask = model(torch.FloatTensor(heatmap).unsqueeze(0).unsqueeze(0)).numpy()[0, 0]

        pred_locs = extract_locations(pred_mask, lat_min, lat_max, lon_min, lon_max)

        on_bus = group[group['user_type'] == 'on_bus']
        actual = [(on_bus[on_bus['bus_id']==bid]['bus_lat'].iloc[0],
                   on_bus[on_bus['bus_id']==bid]['bus_lon'].iloc[0])
                  for bid in on_bus['bus_id'].unique() if bid >= 0 and len(on_bus[on_bus['bus_id']==bid]) > 0]

        for pred in pred_locs:
            if actual:
                err = min(np.sqrt((pred[0]-a[0])**2 + (pred[1]-a[1])**2) * 111000 for a in actual)
                errors.append(err)

    errors = np.array(errors)
    print(f"""
ACCURACY:
  Povprečna napaka:  {errors.mean():.1f} m
  Mediana napake:    {np.median(errors):.1f} m
  Napaka < 50m:      {(errors<50).mean()*100:.1f}%
  Napaka < 100m:     {(errors<100).mean()*100:.1f}%
  Napaka < 150m:     {(errors<150).mean()*100:.1f}%
""")

    # Vizualizacija
    print("="*70)
    print("INTERAKTIVNA VIZUALIZACIJA")
    print("="*70)

    fig, axes = plt.subplots(1, 2, figsize=(14, 6))
    plt.subplots_adjust(bottom=0.18)

    ax_slider = plt.axes([0.15, 0.06, 0.7, 0.04])
    slider = Slider(ax_slider, 'Čas', 0, len(timestamps)-1, valinit=0, valstep=1)

    def update(val):
        idx = int(slider.val)
        ts = timestamps[idx]
        group = df[df['timestamp'] == ts]

        heatmap = create_heatmap(group, lat_min, lat_max, lon_min, lon_max)
        if heatmap.max() > 0:
            heatmap = heatmap / heatmap.max()

        with torch.no_grad():
            pred_mask = model(torch.FloatTensor(heatmap).unsqueeze(0).unsqueeze(0)).numpy()[0, 0]

        pred_locs = extract_locations(pred_mask, lat_min, lat_max, lon_min, lon_max)

        on_bus = group[group['user_type'] == 'on_bus']
        actual = [(on_bus[on_bus['bus_id']==bid]['bus_lat'].iloc[0],
                   on_bus[on_bus['bus_id']==bid]['bus_lon'].iloc[0])
                  for bid in sorted(on_bus['bus_id'].unique()) if bid >= 0 and len(on_bus[on_bus['bus_id']==bid]) > 0]

        # Panel 1: Heatmap
        axes[0].clear()
        axes[0].imshow(heatmap, cmap='YlOrRd', origin='lower',
                      extent=[lon_min, lon_max, lat_min, lat_max], aspect='auto')
        for i, (lat, lon) in enumerate(actual):
            axes[0].scatter([lon], [lat], c='cyan', s=200, marker='o',
                          edgecolors='white', linewidth=2, zorder=10,
                          label='Pravi avtobus' if i == 0 else None)
        axes[0].set_title(f'DISKRETIZIRAN ZEMLJEVID\n{len(group)} GPS lokacij', fontsize=13, fontweight='bold')
        axes[0].set_xlabel('Longitude')
        axes[0].set_ylabel('Latitude')
        axes[0].legend(loc='upper right')

        # Panel 2: CNN napoved
        axes[1].clear()
        axes[1].imshow(pred_mask, cmap='hot', origin='lower', vmin=0, vmax=1,
                      extent=[lon_min, lon_max, lat_min, lat_max], aspect='auto')

        for i, (lat, lon, conf) in enumerate(pred_locs):
            axes[1].scatter([lon], [lat], c='lime', s=250, marker='X',
                          edgecolors='black', linewidth=2.5, zorder=10,
                          label=f'CNN napoved' if i == 0 else None)

        for i, (lat, lon) in enumerate(actual):
            axes[1].scatter([lon], [lat], c='cyan', s=150, marker='o',
                          edgecolors='white', linewidth=2, zorder=9,
                          label='Pravi avtobus' if i == 0 else None)

        # Napaka
        if pred_locs and actual:
            errs = [min(np.sqrt((p[0]-a[0])**2+(p[1]-a[1])**2)*111000 for a in actual) for p in pred_locs]
            title = f'CNN DETEKCIJA\nNapaka: {np.mean(errs):.0f} m'
        else:
            title = 'CNN DETEKCIJA'

        axes[1].set_title(title, fontsize=13, fontweight='bold')
        axes[1].set_xlabel('Longitude')
        axes[1].set_ylabel('Latitude')
        axes[1].legend(loc='upper right')

        fig.suptitle(f'Čas: {idx}/{len(timestamps)-1}', fontsize=14)
        fig.canvas.draw_idle()

    slider.on_changed(update)
    update(0)

    plt.show()


if __name__ == "__main__":
    main()
