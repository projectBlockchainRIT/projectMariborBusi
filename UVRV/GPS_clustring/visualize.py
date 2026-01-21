#!/usr/bin/env python3
"""
Vizualizacija podatkov iz simulatorja avtobusnih lokacij
"""

import subprocess
import os
import glob
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path
import json

# Nastavitve
DATA_FOLDER = "data"
SIMULATOR_EXECUTABLE = "./main"  # Kompajlirani C++ program

def compile_simulator():
    """Kompajliraj C++ simulator"""
    print("🔨 Kompaјliranje simulatorja...")
    try:
        result = subprocess.run(
            ["clang++", "-std=c++17", "-o", SIMULATOR_EXECUTABLE, "main.cpp", "-lcurl"],
            capture_output=True,
            text=True
        )
        if result.returncode == 0:
            print("✅ Kompajliranje uspešno!")
            return True
        else:
            print(f"❌ Napaka pri kompajliranju:\n{result.stderr}")
            return False
    except FileNotFoundError:
        print("❌ clang++ ni najden. Poskusi s: brew install llvm")
        return False

def run_simulator(route):
    """Zaženi simulator za izbrano linijo"""
    print(f"\n🚌 Zaganjam simulator za linijo: {route}")
    try:
        process = subprocess.Popen(
            [SIMULATOR_EXECUTABLE],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        stdout, stderr = process.communicate(input=route + "\n")
        print(stdout)
        if stderr:
            print(f"⚠️  {stderr}")
        return True
    except FileNotFoundError:
        print(f"❌ Simulator {SIMULATOR_EXECUTABLE} ne obstaja. Najprej kompaјliraj!")
        return False

def load_data(route):
    """Naloži vse podatke za izbrano linijo"""
    if not os.path.exists(DATA_FOLDER):
        print(f"❌ Folder {DATA_FOLDER} ne obstaja!")
        return None
    
    # Poišči najnovejšo CSV datoteko za to linijo
    csv_files = glob.glob(f"{DATA_FOLDER}/{route}_all_*.csv")
    if not csv_files:
        print(f"❌ Ni podatkov za linijo {route}")
        return None
    
    # Sortiraj po datumu - vzemi najnovejšo
    csv_file = sorted(csv_files)[-1]
    print(f"📂 Nalagam podatke iz: {csv_file}")
    
    return pd.read_csv(csv_file)

def plot_statistics(df, route):
    """Nariši osnovne statistike"""
    print("📊 Narišem statistike...")
    
    fig, axes = plt.subplots(2, 2, figsize=(14, 10))
    fig.suptitle(f'Statistika simulacije - Linija {route}', fontsize=16, fontweight='bold')
    
    # 1. Signal strength distribucija
    axes[0, 0].hist(df['signal_strength'], bins=30, color='skyblue', edgecolor='black')
    axes[0, 0].set_xlabel('Signal Strength')
    axes[0, 0].set_ylabel('Frekvenca')
    axes[0, 0].set_title('Distribucija moči signala')
    axes[0, 0].axvline(df['signal_strength'].mean(), color='red', linestyle='--', label=f'Povprečje: {df["signal_strength"].mean():.1f}')
    axes[0, 0].legend()
    
    # 2. Accuracy distribucija
    axes[0, 1].hist(df['accuracy_meters'], bins=30, color='lightcoral', edgecolor='black')
    axes[0, 1].set_xlabel('Natančnost (metri)')
    axes[0, 1].set_ylabel('Frekvenca')
    axes[0, 1].set_title('Distribucija natančnosti GPS')
    axes[0, 1].axvline(df['accuracy_meters'].mean(), color='red', linestyle='--', label=f'Povprečje: {df["accuracy_meters"].mean():.1f}m')
    axes[0, 1].legend()
    
    # 3. Število uporabnikov po segmentu
    segment_counts = df.groupby(pd.cut(df.index, bins=10)).size()
    axes[1, 0].plot(segment_counts.values, marker='o', linestyle='-', color='green', linewidth=2)
    axes[1, 0].set_xlabel('Segment')
    axes[1, 0].set_ylabel('Število uporabnikov')
    axes[1, 0].set_title('Uporabniki po segmentih')
    axes[1, 0].grid(True, alpha=0.3)
    
    # 4. Signal strength vs Accuracy
    axes[1, 1].scatter(df['signal_strength'], df['accuracy_meters'], alpha=0.5, s=30)
    axes[1, 1].set_xlabel('Signal Strength')
    axes[1, 1].set_ylabel('Natančnost (metri)')
    axes[1, 1].set_title('Korelacija: Signal → Natančnost')
    axes[1, 1].grid(True, alpha=0.3)
    
    # Dodaj trendno črto - z robustno metodo
    try:
        # Uporabi LOWESS za robustno trendecko črto
        from scipy.signal import savgol_filter
        sorted_idx = np.argsort(df['signal_strength'].values)
        x_sorted = df['signal_strength'].values[sorted_idx]
        y_sorted = df['accuracy_meters'].values[sorted_idx]
        
        # Poskusi s polynomom, če ne uspe, preskoči
        try:
            z = np.polyfit(x_sorted, y_sorted, 2)
            p = np.poly1d(z)
            x_trend = np.linspace(df['signal_strength'].min(), df['signal_strength'].max(), 100)
            axes[1, 1].plot(x_trend, p(x_trend), "r--", alpha=0.8, linewidth=2)
        except:
            # Če polyfit ne uspe, naredi samo preprosto linijo
            z = np.polyfit(x_sorted, y_sorted, 1)
            p = np.poly1d(z)
            x_trend = np.linspace(df['signal_strength'].min(), df['signal_strength'].max(), 100)
            axes[1, 1].plot(x_trend, p(x_trend), "r--", alpha=0.8, linewidth=2, label='Trend')
    except Exception as e:
        # Če kaj slučajno ne uspe, samo preskoči trendno črto
        pass
    
    plt.tight_layout()
    return fig

def plot_locations(df, route):
    """Nariši lokacije uporabnikov in avtobusa"""
    print("📍 Narišem lokacije...")
    
    fig, ax = plt.subplots(figsize=(14, 10))
    
    # Uporabnikove lokacije
    scatter = ax.scatter(df['lon'], df['lat'], c=df['signal_strength'], 
                        cmap='RdYlGn', s=50, alpha=0.6, edgecolors='black', linewidth=0.5)
    
    # Povprečna lokacija avtobusa po časovnih intervalih
    df_sorted = df.sort_values('timestamp')
    bus_positions = []
    timestamps = []
    
    # Grupiraj po 50 vrsticah za bolj jasen prikaz
    for i in range(0, len(df_sorted), max(1, len(df_sorted) // 20)):
        chunk = df_sorted.iloc[i:i+50]
        if len(chunk) > 0:
            bus_lat = chunk['lat'].mean()
            bus_lon = chunk['lon'].mean()
            bus_positions.append((bus_lon, bus_lat))
            timestamps.append(chunk['timestamp'].iloc[0])
    
    if bus_positions:
        bus_lons, bus_lats = zip(*bus_positions)
        ax.plot(bus_lons, bus_lats, 'r-', linewidth=3, label='Pot avtobusa (povprečje)', zorder=5)
        ax.scatter(bus_lons, bus_lats, color='red', s=100, marker='*', zorder=6, edgecolors='darkred', linewidth=1)
    
    ax.set_xlabel('Longitude', fontsize=12)
    ax.set_ylabel('Latitude', fontsize=12)
    ax.set_title(f'Lokacije uporabnikov - Linija {route}', fontsize=14, fontweight='bold')
    ax.grid(True, alpha=0.3)
    ax.legend(fontsize=10)
    
    cbar = plt.colorbar(scatter, ax=ax)
    cbar.set_label('Signal Strength', fontsize=11)
    
    return fig

def plot_heatmap(df, route):
    """Nariši heatmap lokacij"""
    print("🔥 Narišem heatmap...")
    
    fig, ax = plt.subplots(figsize=(14, 10))
    
    # 2D histogram za heatmap
    heatmap, xedges, yedges = np.histogram2d(df['lon'], df['lat'], bins=30)
    extent = [xedges[0], xedges[-1], yedges[0], yedges[-1]]
    
    im = ax.imshow(heatmap.T, extent=extent, origin='lower', cmap='YlOrRd', aspect='auto', interpolation='bilinear')
    
    ax.set_xlabel('Longitude', fontsize=12)
    ax.set_ylabel('Latitude', fontsize=12)
    ax.set_title(f'Heatmap gostote uporabnikov - Linija {route}', fontsize=14, fontweight='bold')
    
    cbar = plt.colorbar(im, ax=ax)
    cbar.set_label('Gostota', fontsize=11)
    
    return fig

def print_summary(df, route):
    """Izpiši povzetek podatkov"""
    print("\n" + "="*60)
    print(f"📈 POVZETEK PODATKOV - LINIJA {route}".center(60))
    print("="*60)
    
    print(f"\n📊 Skupna statistika:")
    print(f"  • Skupaj meritev: {len(df):,}")
    print(f"  • Unikatnih uporabnikov: {df['user_id'].nunique()}")
    print(f"  • Časovni razpon: {df['timestamp'].max() - df['timestamp'].min()} sekund")
    
    print(f"\n📡 Signal Strength:")
    print(f"  • Povprečje: {df['signal_strength'].mean():.2f}")
    print(f"  • Min: {df['signal_strength'].min():.2f}")
    print(f"  • Max: {df['signal_strength'].max():.2f}")
    print(f"  • Std Dev: {df['signal_strength'].std():.2f}")
    
    print(f"\n📍 Natančnost (metri):")
    print(f"  • Povprečje: {df['accuracy_meters'].mean():.2f}m")
    print(f"  • Min: {df['accuracy_meters'].min():.2f}m")
    print(f"  • Max: {df['accuracy_meters'].max():.2f}m")
    print(f"  • Std Dev: {df['accuracy_meters'].std():.2f}m")
    
    print(f"\n🗺️  Geografski podatki:")
    print(f"  • Latitude razpon: {df['lat'].min():.6f} do {df['lat'].max():.6f}")
    print(f"  • Longitude razpon: {df['lon'].min():.6f} do {df['lon'].max():.6f}")
    
    # Razdalja v metrih
    lat_range = (df['lat'].max() - df['lat'].min()) * 111000
    lon_range = (df['lon'].max() - df['lon'].min()) * 111000
    print(f"  • Pokrivana razdalja: ~{np.sqrt(lat_range**2 + lon_range**2)/1000:.2f} km")
    
    print("\n" + "="*60 + "\n")

def main():
    print("🚌 VIZUALIZACIJA AVTOBUSNIH LOKACIJ")
    print("=" * 50)
    
    # Preveri ali obstaja data folder
    if not os.path.exists(DATA_FOLDER):
        os.makedirs(DATA_FOLDER)
        print(f"✅ Ustvarjen folder: {DATA_FOLDER}\n")
    
    # Preveri ali je simulator kompajliran
    if not os.path.exists(SIMULATOR_EXECUTABLE):
        print(f"❌ Simulator ni kompajliran!\n")
        if input("Ali želiš da ga kompaјliram? (da/ne): ").lower() == "da":
            if not compile_simulator():
                print("Prekinjam...")
                return
        else:
            print("Prekinjam...")
            return
    
    print("\n" + "=" * 50)
    print("Izbira akcije:")
    print("1. Zaženi novo simulacijo")
    print("2. Prikaži podatke iz obstoječe simulacije")
    choice = input("\nVnesi izbiro (1/2): ").strip()
    
    route = None
    
    if choice == "1":
        # Zaženi novo simulacijo
        print("\nRazpoložljive linije:")
        route = input("Vnesi ime linije (npr. G1): ").strip().upper()
        if not run_simulator(route):
            print("Prekinjam...")
            return
    elif choice == "2":
        # Prikaži obstoječe podatke
        csv_files = glob.glob(f"{DATA_FOLDER}/*_all_*.csv")
        if not csv_files:
            print("❌ Ni podatkov v folderju!")
            return
        
        print("\nDostopni podatki:")
        for i, f in enumerate(sorted(csv_files)[-5:], 1):
            print(f"{i}. {os.path.basename(f)}")
        
        choice = input("\nVnesi številko datoteke: ").strip()
        try:
            selected_file = sorted(csv_files)[-int(choice)]
            route = selected_file.split('/')[-1].split('_')[0]
        except:
            print("❌ Neveljavna izbira!")
            return
    else:
        print("❌ Neveljavna izbira!")
        return
    
    if not route:
        print("❌ Napaka pri pridobivanju linije!")
        return
    
    # Naloži podatke
    df = load_data(route)
    if df is None or len(df) == 0:
        print("❌ Ni podatkov za prikaz!")
        return
    
    # Izpiši povzetek
    print_summary(df, route)
    
    # Nariši grafe
    print("📈 Narisovanje grafov...")
    
    fig1 = plot_statistics(df, route)
    fig1.savefig(f"{DATA_FOLDER}/{route}_statistics.png", dpi=150, bbox_inches='tight')
    print(f"✅ Shranjen: {DATA_FOLDER}/{route}_statistics.png")
    
    fig2 = plot_locations(df, route)
    fig2.savefig(f"{DATA_FOLDER}/{route}_locations.png", dpi=150, bbox_inches='tight')
    print(f"✅ Shranjen: {DATA_FOLDER}/{route}_locations.png")
    
    fig3 = plot_heatmap(df, route)
    fig3.savefig(f"{DATA_FOLDER}/{route}_heatmap.png", dpi=150, bbox_inches='tight')
    print(f"✅ Shranjen: {DATA_FOLDER}/{route}_heatmap.png")
    
    # Prikaži grafe
    print("\n📺 Prikazujem grafe...")
    plt.show()
    
    print("\n✅ Vizualizacija zaključena!")

if __name__ == "__main__":
    main()
