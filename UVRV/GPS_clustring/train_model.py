#!/usr/bin/env python3
"""
Treniranje nevronske mreže za napoved avtobusne lokacije.
"""

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import pickle
import json
import glob
from pathlib import Path

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers, models
from sklearn.metrics import mean_absolute_error, mean_squared_error

TRAINING_FOLDER = "training_data"
RESULTS_FOLDER = "results"

def load_training_data(route):
    """Naloži treningske podatke za izbrano linijo."""
    print(f"📂 Nalagam podatke za {route}...")

    files = glob.glob(f"{TRAINING_FOLDER}/{route}_training_*_X_train.npy")
    if not files:
        print(f"❌ Ni podatkov za {route}")
        return None

    latest = sorted(files)[-1]
    base_name = latest.replace("_X_train.npy", "")

    X_train = np.load(f"{base_name}_X_train.npy")
    y_train = np.load(f"{base_name}_y_train.npy")
    X_val = np.load(f"{base_name}_X_val.npy")
    y_val = np.load(f"{base_name}_y_val.npy")
    X_test = np.load(f"{base_name}_X_test.npy")
    y_test = np.load(f"{base_name}_y_test.npy")

    with open(f"{base_name}_scaler_X.pkl", 'rb') as f:
        scaler_X = pickle.load(f)
    with open(f"{base_name}_scaler_y.pkl", 'rb') as f:
        scaler_y = pickle.load(f)

    with open(f"{base_name}_metadata.json", 'r') as f:
        metadata = json.load(f)

    print(f"✅ Podatki naloženi:")
    print(f"   • Train: {X_train.shape}")
    print(f"   • Val:   {X_val.shape}")
    print(f"   • Test:  {X_test.shape}")

    return {
        'X_train': X_train, 'y_train': y_train,
        'X_val': X_val, 'y_val': y_val,
        'X_test': X_test, 'y_test': y_test,
        'scaler_X': scaler_X, 'scaler_y': scaler_y,
        'metadata': metadata,
        'base_name': base_name
    }

def build_model(input_shape):
    """Zgradi nevronsko mrežo za regresijo (lat, lon)."""
    print(f"🔨 Gradim model (input: {input_shape})...")

    model = models.Sequential([
        layers.Dense(128, activation='relu', input_shape=(input_shape,)),
        layers.Dropout(0.2),

        layers.Dense(64, activation='relu'),
        layers.Dropout(0.2),

        layers.Dense(32, activation='relu'),
        layers.Dropout(0.1),

        layers.Dense(16, activation='relu'),

        layers.Dense(2, activation='linear')
    ])

    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=0.001),
        loss='mse',
        metrics=['mae']
    )

    print(model.summary())
    return model

def train_model(model, data, epochs=50, batch_size=16):
    """Trenira model."""
    print(f"\n🚀 Treniranje ({epochs} epochs)...")

    history = model.fit(
        data['X_train'], data['y_train'],
        validation_data=(data['X_val'], data['y_val']),
        epochs=epochs,
        batch_size=batch_size,
        verbose=1
    )

    return history

def evaluate_model(model, data):
    """Evalvira model in izračuna MAE, RMSE."""
    print("\n📊 Evaluacija...")

    y_train_pred = model.predict(data['X_train'], verbose=0)
    y_val_pred = model.predict(data['X_val'], verbose=0)
    y_test_pred = model.predict(data['X_test'], verbose=0)

    y_train_real = data['scaler_y'].inverse_transform(data['y_train'])
    y_train_pred_real = data['scaler_y'].inverse_transform(y_train_pred)

    y_val_real = data['scaler_y'].inverse_transform(data['y_val'])
    y_val_pred_real = data['scaler_y'].inverse_transform(y_val_pred)

    y_test_real = data['scaler_y'].inverse_transform(data['y_test'])
    y_test_pred_real = data['scaler_y'].inverse_transform(y_test_pred)

    train_mae = mean_absolute_error(y_train_real, y_train_pred_real)
    train_rmse = np.sqrt(mean_squared_error(y_train_real, y_train_pred_real))

    val_mae = mean_absolute_error(y_val_real, y_val_pred_real)
    val_rmse = np.sqrt(mean_squared_error(y_val_real, y_val_pred_real))

    test_mae = mean_absolute_error(y_test_real, y_test_pred_real)
    test_rmse = np.sqrt(mean_squared_error(y_test_real, y_test_pred_real))

    print(f"\n   Train - MAE: {train_mae:.6f}° ({train_mae*111000:.1f}m), RMSE: {train_rmse:.6f}°")
    print(f"   Val   - MAE: {val_mae:.6f}° ({val_mae*111000:.1f}m), RMSE: {val_rmse:.6f}°")
    print(f"   Test  - MAE: {test_mae:.6f}° ({test_mae*111000:.1f}m), RMSE: {test_rmse:.6f}°")

    return {
        'y_train_pred_real': y_train_pred_real,
        'y_train_real': y_train_real,
        'y_val_pred_real': y_val_pred_real,
        'y_val_real': y_val_real,
        'y_test_pred_real': y_test_pred_real,
        'y_test_real': y_test_real,
        'train_mae': train_mae,
        'val_mae': val_mae,
        'test_mae': test_mae,
        'train_rmse': train_rmse,
        'val_rmse': val_rmse,
        'test_rmse': test_rmse,
    }

def plot_training_history(history, route):
    """Nariši loss in MAE krivulje."""
    print("\n📈 Narisovanje krivulj...")

    fig, axes = plt.subplots(1, 2, figsize=(14, 5))
    fig.suptitle(f'Treniranje - {route}', fontsize=14, fontweight='bold')

    axes[0].plot(history.history['loss'], label='Train', linewidth=2)
    axes[0].plot(history.history['val_loss'], label='Val', linewidth=2)
    axes[0].set_xlabel('Epoch')
    axes[0].set_ylabel('Loss (MSE)')
    axes[0].set_title('Loss')
    axes[0].legend()
    axes[0].grid(True, alpha=0.3)

    axes[1].plot(history.history['mae'], label='Train', linewidth=2)
    axes[1].plot(history.history['val_mae'], label='Val', linewidth=2)
    axes[1].set_xlabel('Epoch')
    axes[1].set_ylabel('MAE')
    axes[1].set_title('MAE')
    axes[1].legend()
    axes[1].grid(True, alpha=0.3)

    plt.tight_layout()
    return fig

def plot_predictions(eval_results, route):
    """Nariši napovedi vs prave vrednosti."""
    print("\n📍 Narisovanje napovedi...")

    fig, axes = plt.subplots(2, 2, figsize=(14, 10))
    fig.suptitle(f'Napovedi - {route}', fontsize=14, fontweight='bold')

    y_test_real = eval_results['y_test_real']
    y_test_pred = eval_results['y_test_pred_real']

    axes[0, 0].scatter(y_test_real[:, 0], y_test_pred[:, 0], alpha=0.5, s=30)
    min_lat = min(y_test_real[:, 0].min(), y_test_pred[:, 0].min())
    max_lat = max(y_test_real[:, 0].max(), y_test_pred[:, 0].max())
    axes[0, 0].plot([min_lat, max_lat], [min_lat, max_lat], 'r--', linewidth=2)
    axes[0, 0].set_xlabel('Prava Latitude')
    axes[0, 0].set_ylabel('Napovedana Latitude')
    axes[0, 0].set_title('Test - Latitude')
    axes[0, 0].grid(True, alpha=0.3)

    axes[0, 1].scatter(y_test_real[:, 1], y_test_pred[:, 1], alpha=0.5, s=30, color='green')
    min_lon = min(y_test_real[:, 1].min(), y_test_pred[:, 1].min())
    max_lon = max(y_test_real[:, 1].max(), y_test_pred[:, 1].max())
    axes[0, 1].plot([min_lon, max_lon], [min_lon, max_lon], 'r--', linewidth=2)
    axes[0, 1].set_xlabel('Prava Longitude')
    axes[0, 1].set_ylabel('Napovedana Longitude')
    axes[0, 1].set_title('Test - Longitude')
    axes[0, 1].grid(True, alpha=0.3)

    axes[1, 0].scatter(y_test_real[:, 1], y_test_real[:, 0],
                      c='green', alpha=0.6, s=50, label='Prava', edgecolors='black', linewidth=0.5)
    axes[1, 0].scatter(y_test_pred[:, 1], y_test_pred[:, 0],
                      c='red', alpha=0.6, s=50, label='Napoved', marker='x', linewidth=2)
    axes[1, 0].set_xlabel('Longitude')
    axes[1, 0].set_ylabel('Latitude')
    axes[1, 0].set_title('Geografski Prikaz')
    axes[1, 0].legend()
    axes[1, 0].grid(True, alpha=0.3)

    errors = np.linalg.norm(y_test_real - y_test_pred, axis=1) * 111000
    axes[1, 1].hist(errors, bins=30, color='orange', edgecolor='black')
    axes[1, 1].axvline(errors.mean(), color='red', linestyle='--', linewidth=2,
                      label=f'Povprečje: {errors.mean():.1f}m')
    axes[1, 1].set_xlabel('Napaka (m)')
    axes[1, 1].set_ylabel('Frekvenca')
    axes[1, 1].set_title(f'Razporeditev Napak (MAE={eval_results["test_mae"]*111000:.1f}m)')
    axes[1, 1].legend()
    axes[1, 1].grid(True, alpha=0.3)

    plt.tight_layout()
    return fig

def save_results(model, history, eval_results, route):
    """Shrani model, historijo in evalvacijo."""
    print(f"\n💾 Shranjujem rezultate...")

    Path(RESULTS_FOLDER).mkdir(exist_ok=True)

    model_path = f"{RESULTS_FOLDER}/{route}_model.h5"
    model.save(model_path)
    print(f"✅ Model: {model_path}")

    with open(f"{RESULTS_FOLDER}/{route}_history.pkl", 'wb') as f:
        pickle.dump(history.history, f)

    eval_summary = {
        'train_mae': float(eval_results['train_mae']),
        'train_mae_meters': float(eval_results['train_mae'] * 111000),
        'train_rmse': float(eval_results['train_rmse']),
        'val_mae': float(eval_results['val_mae']),
        'val_mae_meters': float(eval_results['val_mae'] * 111000),
        'val_rmse': float(eval_results['val_rmse']),
        'test_mae': float(eval_results['test_mae']),
        'test_mae_meters': float(eval_results['test_mae'] * 111000),
        'test_rmse': float(eval_results['test_rmse']),
        'test_rmse_meters': float(eval_results['test_rmse'] * 111000),
    }
    with open(f"{RESULTS_FOLDER}/{route}_evaluation.json", 'w') as f:
        json.dump(eval_summary, f, indent=2)

def main():
    print("🧠 TRENIRANJE NEVRONSKE MREŽE")
    print("="*70)

    routes = set()
    for f in glob.glob(f"{TRAINING_FOLDER}/*_training_*_metadata.json"):
        route = f.split('/')[-1].split('_')[0]
        routes.add(route)

    if not routes:
        print("❌ Ni podatkov! Zaženi prepare_training_data.py")
        return

    print("\nDostopne linije:")
    for r in sorted(routes):
        print(f"   • {r}")

    route = input("\nVnesi linijo: ").strip().upper()

    if route not in routes:
        print(f"❌ Linija {route} ni dostopna")
        return

    data = load_training_data(route)
    if data is None:
        return

    model = build_model(data['X_train'].shape[1])
    history = train_model(model, data, epochs=50, batch_size=16)
    eval_results = evaluate_model(model, data)

    Path(RESULTS_FOLDER).mkdir(exist_ok=True)

    save_results(model, history, eval_results, route)

    print("\n📊 Narisovanje rezultatov...")

    fig1 = plot_training_history(history, route)
    fig1.savefig(f"{RESULTS_FOLDER}/{route}_training_history.png", dpi=150, bbox_inches='tight')
    print(f"✅ {RESULTS_FOLDER}/{route}_training_history.png")

    fig2 = plot_predictions(eval_results, route)
    fig2.savefig(f"{RESULTS_FOLDER}/{route}_predictions.png", dpi=150, bbox_inches='tight')
    print(f"✅ {RESULTS_FOLDER}/{route}_predictions.png")

    plt.show()

    print("\n" + "="*70)
    print("✅ TRENIRANJE ZAKLJUČENO")
    print("="*70)

if __name__ == "__main__":
    main()
