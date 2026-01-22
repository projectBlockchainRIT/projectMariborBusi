#!/usr/bin/env python3
"""
Vizualizacija Napovedovanj Nevronske Mreže
Koristi SAMO trenirani model za detekcijo avtobusnih lokacij
"""

import numpy as np
import pandas as pd
import glob
import pickle
import json
from pathlib import Path
import matplotlib.pyplot as plt
import seaborn as sns
import torch
import torch.nn as nn

DATA_FOLDER = "data"
TRAINING_FOLDER = "training_data"
RESULTS_FOLDER = "results"

COLORS = {
    'actual': '#DC2626',
    'predicted': '#10B981',
    'error': '#F59E0B',
    'users': '#2563EB',
}


class BusLocationModel(nn.Module):
    """Ista arhitektura kot pri treniranju (PyTorch)."""

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

            nn.Linear(16, 2),  # izhod: lat, lon
        )

    def forward(self, x):
        return self.network(x)

def load_model_and_scalers(route):
    """Naloži PyTorch model (.pt) in pripadajoče skalere."""

    print(f"\nNalagam model za linijo {route}...")

    checkpoint_path = f"{RESULTS_FOLDER}/{route}_model.pt"
    if not Path(checkpoint_path).exists():
        print(f"Model ne obstaja: {checkpoint_path}")
        print("   Najprej zaženi: python3 train_model.py")
        return None, None, None, None

    try:
        checkpoint = torch.load(checkpoint_path, map_location='cpu')
        input_size = checkpoint.get('input_size')
        if input_size is None:
            print("V checkpointu manjka input_size")
            return None, None, None, None

        model = BusLocationModel(input_size)
        model.load_state_dict(checkpoint['model_state_dict'])
        model.eval()
        print(f"Model nalozen: {checkpoint_path} (input_size={input_size})")

        # Najdi najnovejši paket scalerjev
        training_files = glob.glob(f"{TRAINING_FOLDER}/{route}_training_*_metadata.json")
        if not training_files:
            print(f"Ni treninskih podatkov za {route}")
            return None, None, None, None

        latest_training = sorted(training_files)[-1]
        base_name = latest_training.replace("_metadata.json", "")

        with open(f"{base_name}_scaler_X.pkl", 'rb') as f:
            scaler_X = pickle.load(f)
        with open(f"{base_name}_scaler_y.pkl", 'rb') as f:
            scaler_y = pickle.load(f)

        print("Skaleri nalozeni")
        return model, scaler_X, scaler_y, input_size

    except Exception as e:
        print(f"Napaka pri nalaganju: {e}")
        return None, None, None, None

def load_simulation_data(route):
    """Učitaj simulacijske podatke"""
    csv_files = glob.glob(f"{DATA_FOLDER}/{route}_multibus_*.csv")
    if not csv_files:
        csv_files = glob.glob(f"{DATA_FOLDER}/{route}_all_*.csv")
    if not csv_files:
        print(f"Ni podatkov za linijo {route}")
        return None
    
    csv_file = sorted(csv_files)[-1]
    print(f"Ucitavam podatke: {csv_file}")
    return pd.read_csv(csv_file)

def predict_bus_locations(df, model, scaler_X, scaler_y, input_size):
    """Napove lokacije avtobusov za vsak timestamp (PyTorch model)."""

    print("\nUporabljam model za napovedovanje...")

    timestamps = sorted(df['timestamp'].unique())
    predictions_data = {
        'timestamp': [],
        'predicted_lat': [],
        'predicted_lon': [],
        'actual_lat': [],
        'actual_lon': [],
        'num_users': [],
        'error_meters': []
    }

    target_length = input_size  # mora se ujemati s treniranjem (npr. 150 = 50 uporabnikov * 3 featureji)

    for i, ts in enumerate(timestamps):
        if (i + 1) % 50 == 0 or i == 0:
            print(f"  Obdelava: {i+1}/{len(timestamps)}", end='\r')

        group = df[df['timestamp'] == ts]

        on_bus = group[group['user_type'] == 'on_bus']
        if len(on_bus) == 0:
            continue

        features = on_bus[['lat', 'lon', 'signal_strength']].values.flatten()

        features_fixed = np.zeros(target_length, dtype=np.float32)
        if len(features) >= target_length:
            features_fixed[:] = features[:target_length]
        else:
            features_fixed[:len(features)] = features

        features_norm = scaler_X.transform([features_fixed])[0].astype(np.float32)

        with torch.no_grad():
            x = torch.from_numpy(features_norm).unsqueeze(0)  # shape (1, input_size)
            pred_norm = model(x).squeeze(0).cpu().numpy()

        pred = scaler_y.inverse_transform([pred_norm])[0]

        actual_lat = on_bus.iloc[0]['bus_lat']
        actual_lon = on_bus.iloc[0]['bus_lon']

        error_deg = np.sqrt((pred[0] - actual_lat) ** 2 + (pred[1] - actual_lon) ** 2)
        error_m = error_deg * 111000

        predictions_data['timestamp'].append(ts)
        predictions_data['predicted_lat'].append(pred[0])
        predictions_data['predicted_lon'].append(pred[1])
        predictions_data['actual_lat'].append(actual_lat)
        predictions_data['actual_lon'].append(actual_lon)
        predictions_data['num_users'].append(len(on_bus))
        predictions_data['error_meters'].append(error_m)

    print(f"\nGotovo! Obdelava {len(predictions_data['timestamp'])} okvirjev")
    return pd.DataFrame(predictions_data)

def visualize_predictions(pred_df, route):
    """Vizualizacija napovedovanja modela"""
    print(f"\nKreiram vizualizacijo...")
    
    fig = plt.figure(figsize=(18, 12))
    fig.patch.set_facecolor('#F8FAFC')
    
    # Naslov
    fig.suptitle(f'Napovedi Nevronske Mreze - Linija {route}', 
                fontsize=20, fontweight='bold', color='#1E293B', y=0.98)
    
    # 1. Mapa - Prava vs Napovedana
    ax1 = plt.subplot(2, 3, 1)
    ax1.scatter(pred_df['actual_lon'], pred_df['actual_lat'], 
               c=COLORS['actual'], s=100, alpha=0.7, label='Prava', edgecolors='white', linewidth=1.5)
    ax1.scatter(pred_df['predicted_lon'], pred_df['predicted_lat'], 
               c=COLORS['predicted'], s=100, alpha=0.7, label='Napovedana', edgecolors='white', linewidth=1.5, marker='^')
    
    # Linije med točkami
    for i in range(len(pred_df)):
        ax1.plot([pred_df.iloc[i]['actual_lon'], pred_df.iloc[i]['predicted_lon']],
                [pred_df.iloc[i]['actual_lat'], pred_df.iloc[i]['predicted_lat']],
                'gray', alpha=0.2, linewidth=0.5)
    
    ax1.set_xlabel('Dolžina', fontweight='bold')
    ax1.set_ylabel('Širina', fontweight='bold')
    ax1.set_title('Prava vs Napovedana Lokacija', fontweight='bold', fontsize=12)
    ax1.legend(fontsize=10, framealpha=0.9)
    ax1.grid(True, alpha=0.2)
    ax1.set_facecolor('#F1F5F9')
    
    # 2. Histogram napak
    ax2 = plt.subplot(2, 3, 2)
    ax2.hist(pred_df['error_meters'], bins=30, color=COLORS['error'], alpha=0.7, edgecolor='black')
    ax2.axvline(pred_df['error_meters'].mean(), color='red', linestyle='--', linewidth=2, label=f'Povprečje: {pred_df["error_meters"].mean():.1f}m')
    ax2.axvline(pred_df['error_meters'].median(), color='blue', linestyle='--', linewidth=2, label=f'Mediana: {pred_df["error_meters"].median():.1f}m')
    ax2.set_xlabel('Napaka (metri)', fontweight='bold')
    ax2.set_ylabel('Pogostost', fontweight='bold')
    ax2.set_title('Porazdelitev Napak', fontweight='bold', fontsize=12)
    ax2.legend(fontsize=9)
    ax2.grid(True, alpha=0.2, axis='y')
    ax2.set_facecolor('#F1F5F9')
    
    # 3. Napaka skozi čas
    ax3 = plt.subplot(2, 3, 3)
    ax3.plot(range(len(pred_df)), pred_df['error_meters'].values, 
            color=COLORS['error'], linewidth=2, alpha=0.7)
    ax3.fill_between(range(len(pred_df)), pred_df['error_meters'].values, alpha=0.3, color=COLORS['error'])
    ax3.set_xlabel('Okvir', fontweight='bold')
    ax3.set_ylabel('Napaka (metri)', fontweight='bold')
    ax3.set_title('Napaka Skozi Čas', fontweight='bold', fontsize=12)
    ax3.grid(True, alpha=0.2)
    ax3.set_facecolor('#F1F5F9')
    
    # 4. Natančnost Širine
    ax4 = plt.subplot(2, 3, 4)
    ax4.scatter(pred_df['actual_lat'], pred_df['predicted_lat'], 
               c=COLORS['predicted'], alpha=0.7, edgecolors='white', linewidth=1)
    min_lat = min(pred_df['actual_lat'].min(), pred_df['predicted_lat'].min())
    max_lat = max(pred_df['actual_lat'].max(), pred_df['predicted_lat'].max())
    ax4.plot([min_lat, max_lat], [min_lat, max_lat], 'r--', linewidth=2, label='Popolna Napoved')
    ax4.set_xlabel('Prava Širina', fontweight='bold')
    ax4.set_ylabel('Napovedana Širina', fontweight='bold')
    ax4.set_title('Natančnost Širine', fontweight='bold', fontsize=12)
    ax4.legend(fontsize=9)
    ax4.grid(True, alpha=0.2)
    ax4.set_facecolor('#F1F5F9')
    
    # 5. Natančnost Dolžine
    ax5 = plt.subplot(2, 3, 5)
    ax5.scatter(pred_df['actual_lon'], pred_df['predicted_lon'], 
               c=COLORS['actual'], alpha=0.7, edgecolors='white', linewidth=1)
    min_lon = min(pred_df['actual_lon'].min(), pred_df['predicted_lon'].min())
    max_lon = max(pred_df['actual_lon'].max(), pred_df['predicted_lon'].max())
    ax5.plot([min_lon, max_lon], [min_lon, max_lon], 'r--', linewidth=2, label='Popolna Napoved')
    ax5.set_xlabel('Prava Dolžina', fontweight='bold')
    ax5.set_ylabel('Napovedana Dolžina', fontweight='bold')
    ax5.set_title('Natančnost Dolžine', fontweight='bold', fontsize=12)
    ax5.legend(fontsize=9)
    ax5.grid(True, alpha=0.2)
    ax5.set_facecolor('#F1F5F9')
    
    # 6. Statistika
    ax6 = plt.subplot(2, 3, 6)
    ax6.axis('off')
    
    stats_text = (
        f"STATISTIKA MODELA\n"
        f"{'─' * 35}\n"
        f"Skupaj Napovedi: {len(pred_df)}\n"
        f"\n📏 METRIKE NAPAKE\n"
        f"Povprečna Napaka: {pred_df['error_meters'].mean():.2f} m\n"
        f"Mediana Napake: {pred_df['error_meters'].median():.2f} m\n"
        f"Std Odklona: {pred_df['error_meters'].std():.2f} m\n"
        f"Najmanja Napaka: {pred_df['error_meters'].min():.2f} m\n"
        f"Največja Napaka: {pred_df['error_meters'].max():.2f} m\n"
        f"\nNATANCNOST\n"
        f"< 100m: {len(pred_df[pred_df['error_meters'] < 100]) / len(pred_df) * 100:.1f}%\n"
        f"< 200m: {len(pred_df[pred_df['error_meters'] < 200]) / len(pred_df) * 100:.1f}%\n"
        f"< 300m: {len(pred_df[pred_df['error_meters'] < 300]) / len(pred_df) * 100:.1f}%\n"
    )
    
    ax6.text(0.1, 0.95, stats_text, transform=ax6.transAxes,
            fontsize=11, verticalalignment='top', family='monospace',
            fontweight='bold',
            bbox=dict(boxstyle='round,pad=1', facecolor='#E0F2FE', 
                     edgecolor='#0284C7', linewidth=2.5, alpha=0.9))
    
    plt.tight_layout()
    plt.show()

def main():
    print("\n" + "="*70)
    print("NAPOVEDI NEVRONSKE MREZE ZA AVTOBUSNE LOKACIJE".center(70))
    print("="*70)
    
    # Pronađi dostopne linije
    csv_files = glob.glob(f"{DATA_FOLDER}/*_multibus_*.csv") + glob.glob(f"{DATA_FOLDER}/*_all_*.csv")
    if not csv_files:
        print("Ni simulacijskih podatkov!")
        return
    
    routes = set()
    for f in csv_files:
        route = f.split('/')[-1].split('_')[0]
        routes.add(route)
    
    print("\nDostopne linije:")
    for route in sorted(routes):
        print(f"   • {route}", end="  ")
    print("\n")
    
    route = input("Izberi linijo (npr. G1): ").strip().upper() or "G1"
    
    if route not in routes:
        print(f"Linija {route} ni dostopna")
        return
    
    # Učitaj model
    model, scaler_X, scaler_y, input_size = load_model_and_scalers(route)
    if model is None:
        print("Ni mogoce ucitati model. Prvo je treba trenirati!")
        print("   Zaženi: python3 train_model.py")
        return
    
    # Učitaj podatke
    df = load_simulation_data(route)
    if df is None:
        return
    
    # Napovedi
    pred_df = predict_bus_locations(df, model, scaler_X, scaler_y, input_size)
    
    # Vizualiziraj
    visualize_predictions(pred_df, route)
    
    print("\nVizualizacija koncana!")

if __name__ == "__main__":
    main()
