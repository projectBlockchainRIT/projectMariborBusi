# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Bus location detection system using GPS data from mobile users. The system:
1. Collects GPS locations from all users (bus passengers, pedestrians, people at stations, nearby users)
2. Discretizes GPS coordinates into a 2D heatmap grid
3. Uses CNN (Convolutional Neural Network) to detect bus locations from the user distribution

**Key Concept**: The neural network learns to identify WHERE people are on buses by analyzing patterns in the discretized GPS heatmap of ALL users.

**Tech Stack**: Python (simulation, CNN training, visualization) with PyTorch

## Quick Start

```bash
# 1. Generate multi-bus simulation data
python3 generate_multibus.py

# 2. Train CNN and visualize results
python3 heatmap_cnn.py
# Choose: 1 (Single Bus), 2 (Multi-Bus), or 3 (Segmentation)

# 3. Alternative: Simple visualization with DBSCAN clustering
python3 simple_viz.py
```

## Architecture

### Data Flow
```
GPS Simulation → All User Locations → Discretized Heatmap (64x64) → CNN → Bus Location(s)
```

### CNN Approach (Main)

**Input**: 64x64 heatmap of ALL user GPS locations (weighted by signal strength)
**Output**: Bus location(s) - depends on model type:
- Single Bus: `(lat, lon)` normalized coordinates
- Multi-Bus: `(max_buses, 3)` = `[lat, lon, confidence]` per bus
- Segmentation: `64x64` probability mask of bus locations

### Key Files

| File | Purpose |
|------|---------|
| `heatmap_cnn.py` | **Main CNN system** - discretization, training, visualization |
| `generate_multibus.py` | Data generator for multi-bus simulation (4 buses, realistic spread) |
| `simple_viz.py` | Alternative visualization with DBSCAN clustering (no NN) |
| `train_model.py` | Legacy MLP training (flat features, not heatmap) |
| `prepare_training_data.py` | Legacy data preparation for MLP |
| `routes.json` | Bus route definitions (coordinate arrays) |

### CNN Models in `heatmap_cnn.py`

| Model | Output | Use Case |
|-------|--------|----------|
| `HeatmapCNN` | Single (lat, lon) | One bus detection |
| `MultiBusRegressor` | N x (lat, lon, conf) | Multiple buses with confidence |
| `MultiBusDetector` | 64x64 probability mask | Segmentation approach |

### User Types in Simulation

| Type | Description | Spread (meters) |
|------|-------------|-----------------|
| `on_bus` | Users riding the bus | 30-80m |
| `waiting_at_station` | Users at bus stops | 20-50m |
| `pedestrian` | People walking nearby | 100-300m |
| `nearby` | Users in buildings/cars | 150-400m |

## Commands

### Generate Data
```bash
# Multi-bus data (4 buses, 500 timestamps)
python3 generate_multibus.py
```

### Train CNN
```bash
python3 heatmap_cnn.py
# Interactive menu:
#   1 - Single Bus (regresija)
#   2 - Multi-Bus Regressor
#   3 - Segmentation
#   4 - Load existing model for visualization
```

### Visualization Only
```bash
python3 simple_viz.py  # DBSCAN-based (no CNN)
```

### Dependencies
```bash
pip install numpy pandas matplotlib scikit-learn scipy torch
```

## Algorithm Details

### Heatmap Creation
1. Define geographic bounds with margin
2. Create 64x64 grid over the area
3. For each user GPS point:
   - Map to grid cell
   - Add weight based on signal_strength
4. Normalize heatmap to [0, 1]

### CNN Training
1. Input: Normalized heatmap of ALL users
2. Ground truth: Actual bus location(s) from simulation
3. Loss: MSE for regression, BCE for segmentation
4. Output: Predicted bus location(s)

### Multi-Bus Detection
- Each bus outputs `[lat_norm, lon_norm, confidence]`
- Confidence indicates if bus exists at that slot
- Training uses weighted loss (position matters only when bus exists)

## Data Format

**CSV columns:**
- `lat, lon` - User GPS coordinates
- `timestamp` - Unix timestamp
- `signal_strength` - GPS quality (20-100)
- `user_type` - on_bus, waiting_at_station, pedestrian, nearby
- `bus_lat, bus_lon` - Ground truth bus location
- `bus_id` - Bus identifier (0-3 for multi-bus)

## Results

Models are saved to `results/` folder:
- `{route}_heatmap_cnn.pt` - Single bus model
- `{route}_heatmap_cnn_multibus.pt` - Multi-bus regressor
- `{route}_heatmap_cnn_segmentation.pt` - Segmentation model
- `{route}_heatmap_metadata.json` - Geographic bounds and config
