"""
Treniranje nevronske mreze za napoved avtobusne lokacije.
PyTorch verzija.
"""

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import pickle
import json
import glob
from pathlib import Path

import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset
from sklearn.metrics import mean_absolute_error, mean_squared_error

TRAINING_FOLDER = "training_data"
RESULTS_FOLDER = "results"

class BusLocationModel(nn.Module):
    def __init__(self, input_size):
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
            nn.Linear(16, 2)
        )

    def forward(self, x):
        return self.network(x)

def load_training_data(route):
    """Nalozi treningske podatke za izbrano linijo."""
    print(f"Nalagam podatke za {route}...")

    files = glob.glob(f"{TRAINING_FOLDER}/{route}_training_*_X_train.npy")
    if not files:
        print(f"Ni podatkov za {route}")
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

    print(f"Podatki nalozeni:")
    print(f"   Train: {X_train.shape}")
    print(f"   Val:   {X_val.shape}")
    print(f"   Test:  {X_test.shape}")

    return {
        'X_train': X_train, 'y_train': y_train,
        'X_val': X_val, 'y_val': y_val,
        'X_test': X_test, 'y_test': y_test,
        'scaler_X': scaler_X, 'scaler_y': scaler_y,
        'metadata': metadata,
        'base_name': base_name
    }

def train_model(model, data, epochs=50, batch_size=16, lr=0.001):
    """Trenira model."""
    print(f"\nTreniranje ({epochs} epochs)...")

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Device: {device}")
    model = model.to(device)
    X_train_t = torch.FloatTensor(data['X_train'])
    y_train_t = torch.FloatTensor(data['y_train'])
    X_val_t = torch.FloatTensor(data['X_val'])
    y_val_t = torch.FloatTensor(data['y_val'])

    train_dataset = TensorDataset(X_train_t, y_train_t)
    val_dataset = TensorDataset(X_val_t, y_val_t)

    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=batch_size)

    criterion = nn.MSELoss()
    optimizer = optim.Adam(model.parameters(), lr=lr)

    history = {'loss': [], 'val_loss': [], 'mae': [], 'val_mae': []}

    for epoch in range(epochs):
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

        history['loss'].append(train_loss)
        history['val_loss'].append(val_loss)
        history['mae'].append(train_mae)
        history['val_mae'].append(val_mae)

        if (epoch + 1) % 10 == 0 or epoch == 0:
            print(f"Epoch {epoch+1:3d}/{epochs} - loss: {train_loss:.6f} - mae: {train_mae:.6f} - val_loss: {val_loss:.6f} - val_mae: {val_mae:.6f}")

    return model, history

def evaluate_model(model, data):
    """Evalvira model in izracuna MAE, RMSE."""
    print("\nEvaluacija...")

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    model = model.to(device)
    model.eval()

    with torch.no_grad():
        X_train_t = torch.FloatTensor(data['X_train']).to(device)
        X_val_t = torch.FloatTensor(data['X_val']).to(device)
        X_test_t = torch.FloatTensor(data['X_test']).to(device)

        y_train_pred = model(X_train_t).cpu().numpy()
        y_val_pred = model(X_val_t).cpu().numpy()
        y_test_pred = model(X_test_t).cpu().numpy()

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

    print(f"\n   Train - MAE: {train_mae:.6f} ({train_mae*111000:.1f}m), RMSE: {train_rmse:.6f}")
    print(f"   Val   - MAE: {val_mae:.6f} ({val_mae*111000:.1f}m), RMSE: {val_rmse:.6f}")
    print(f"   Test  - MAE: {test_mae:.6f} ({test_mae*111000:.1f}m), RMSE: {test_rmse:.6f}")

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
    """Narisi loss in MAE krivulje."""
    print("\nNarisovanje krivulj...")

    fig, axes = plt.subplots(1, 2, figsize=(14, 5))
    fig.suptitle(f'Treniranje - {route}', fontsize=14, fontweight='bold')

    axes[0].plot(history['loss'], label='Train', linewidth=2)
    axes[0].plot(history['val_loss'], label='Val', linewidth=2)
    axes[0].set_xlabel('Epoch')
    axes[0].set_ylabel('Loss (MSE)')
    axes[0].set_title('Loss')
    axes[0].legend()
    axes[0].grid(True, alpha=0.3)

    axes[1].plot(history['mae'], label='Train', linewidth=2)
    axes[1].plot(history['val_mae'], label='Val', linewidth=2)
    axes[1].set_xlabel('Epoch')
    axes[1].set_ylabel('MAE')
    axes[1].set_title('MAE')
    axes[1].legend()
    axes[1].grid(True, alpha=0.3)

    plt.tight_layout()
    return fig

def plot_predictions(eval_results, route):
    """Narisi napovedi vs prave vrednosti."""
    print("\nNarisovanje napovedi...")

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
                      label=f'Povprecje: {errors.mean():.1f}m')
    axes[1, 1].set_xlabel('Napaka (m)')
    axes[1, 1].set_ylabel('Frekvenca')
    axes[1, 1].set_title(f'Razporeditev Napak (MAE={eval_results["test_mae"]*111000:.1f}m)')
    axes[1, 1].legend()
    axes[1, 1].grid(True, alpha=0.3)

    plt.tight_layout()
    return fig

def save_results(model, history, eval_results, route, input_size):
    """Shrani model, historijo in evalvacijo."""
    print(f"\nShranjujem rezultate...")

    Path(RESULTS_FOLDER).mkdir(exist_ok=True)

    # Shrani PyTorch model
    model_path = f"{RESULTS_FOLDER}/{route}_model.pt"
    torch.save({
        'model_state_dict': model.state_dict(),
        'input_size': input_size
    }, model_path)
    print(f"Model: {model_path}")

    with open(f"{RESULTS_FOLDER}/{route}_history.pkl", 'wb') as f:
        pickle.dump(history, f)

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
    print("TRENIRANJE NEVRONSKE MREZE (PyTorch)")
    print("="*70)

    routes = set()
    for f in glob.glob(f"{TRAINING_FOLDER}/*_training_*_metadata.json"):
        route = f.split('/')[-1].split('_')[0]
        routes.add(route)

    if not routes:
        print("Ni podatkov! Zazeni prepare_training_data.py")
        return

    print("\nDostopne linije:")
    for r in sorted(routes):
        print(f"   {r}")

    route = input("\nVnesi linijo: ").strip().upper()

    if route not in routes:
        print(f"Linija {route} ni dostopna")
        return

    data = load_training_data(route)
    if data is None:
        return

    input_size = data['X_train'].shape[1]
    print(f"\nGradim model (input: {input_size})...")
    model = BusLocationModel(input_size)
    print(model)

    model, history = train_model(model, data, epochs=50, batch_size=16)
    eval_results = evaluate_model(model, data)

    Path(RESULTS_FOLDER).mkdir(exist_ok=True)

    save_results(model, history, eval_results, route, input_size)

    print("\nNarisovanje rezultatov...")

    fig1 = plot_training_history(history, route)
    fig1.savefig(f"{RESULTS_FOLDER}/{route}_training_history.png", dpi=150, bbox_inches='tight')
    print(f"{RESULTS_FOLDER}/{route}_training_history.png")

    fig2 = plot_predictions(eval_results, route)
    fig2.savefig(f"{RESULTS_FOLDER}/{route}_predictions.png", dpi=150, bbox_inches='tight')
    print(f"{RESULTS_FOLDER}/{route}_predictions.png")

    plt.show()

    print("\n" + "="*70)
    print("TRENIRANJE ZAKLJUCENO")
    print("="*70)

if __name__ == "__main__":
    main()
