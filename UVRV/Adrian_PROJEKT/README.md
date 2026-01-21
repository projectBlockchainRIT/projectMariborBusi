# Bus Location Prediction using Neural Networks

Projekt za predmet "Uvod v računalniški vid in razpoznavanje vzorcev" - Napoved lokacije avtobusa na podlagi GPS podatkov uporabnikov.

## Opis projekta

Sistem simulira realistične GPS podatke mobilnih uporabnikov z variabilno močjo signala in nato trenira nevronsko mrežo za napoved prave lokacije avtobusa na podlagi zašumljenih GPS meritev uporabnikov.

### Ključne ugotovitve

- ✅ Uspešna implementacija celotnega pipeline-a od simulacije do napovedovanja
- ✅ Model dosega **187.8m MAE** na testnih podatkih (3x izboljšava po popravku)
- ✅ Kritična napaka popravljena: Ground truth je zdaj pravilno izračunan
- ✅ Uporaba samo uporabnikov NA AVTOBUSU za učenje (ignoriramo uporabnike na postajah)

## Arhitektura projekta

```
├── main.cpp / main_v2.cpp     # C++ simulator GPS podatkov
├── prepare_training_data.py   # Priprava podatkov za učenje
├── train_model.py             # Treniranje nevronske mreže
├── interactive_viz.py         # Interaktivna vizualizacija napovedi
├── visualize.py               # Statične vizualizacije
├── run_all.py                 # Master script (zažene celoten pipeline)
├── routes.json                # Definicije avtobusnih prog
├── CLAUDE.md                  # Podrobna dokumentacija
└── README.md                  # Ta datoteka
```

## Hitra uporaba

### Vse naenkrat (priporočeno)
```bash
./run_all.py
```

Izbere linijo in vas vodi skozi vse korake.

### Korak po korak

1. **Simulacija GPS podatkov**
```bash
clang++ -std=c++17 -o main_v2 main_v2.cpp
./main_v2
# Vnesi: G1
```

2. **Priprava treningskih podatkov**
```bash
python3 prepare_training_data.py
# Vnesi: G1
```

3. **Treniranje modela**
```bash
python3 train_model.py
# Vnesi: G1
```

4. **Interaktivna vizualizacija**
```bash
python3 interactive_viz.py
# Vnesi: G1
```

## Struktura podatkov

### Simulirani podatki (`data/`)
CSV format z:
- `lat`, `lon` - GPS koordinate uporabnika
- `signal_strength` - Moč GPS signala (20-100)
- `accuracy_meters` - Natančnost GPS (5-50m)
- `user_type` - `on_bus` ali `waiting_at_station`
- `bus_lat`, `bus_lon` - **Prava lokacija avtobusa** (ground truth)

### Treningski podatki (`training_data/`)
- `X_train.npy`, `X_val.npy`, `X_test.npy` - Feature vektorji (lat, lon, signal_strength padded)
- `y_train.npy`, `y_val.npy`, `y_test.npy` - Ground truth (bus_lat, bus_lon)
- `scaler_X.pkl`, `scaler_y.pkl` - StandardScaler objekti
- `metadata.json` - Metapodatki (število vzorcev, feature dimenzija, itd.)

### Rezultati (`results/`)
- `G1_model.h5` - Trenirani TensorFlow model
- `G1_evaluation.json` - MAE, RMSE metrike
- `G1_training_history.png` - Loss/MAE krivulje
- `G1_predictions.png` - Scatter ploti napovedi

## Model arhitektura

Sequential model:
```
Input (123 features) → Dense(128) → Dropout(0.2) →
Dense(64) → Dropout(0.2) →
Dense(32) → Dropout(0.1) →
Dense(16) →
Output(2: lat, lon)
```

- **Optimizer**: Adam (lr=0.001)
- **Loss**: MSE
- **Metric**: MAE
- **Epochs**: 50
- **Batch size**: 16

## Rezultati

**Test set performance (G1):**
- MAE: 187.8 m
- RMSE: 286.2 m

**Train/Val:**
- Train MAE: 155.8 m
- Val MAE: 209.4 m

## Pomembni popravki

### Kritična napaka (POPRAVLJENA)
Originalna koda je izračunala ground truth kot:
```python
bus_lat = group['lat'].mean()  # ❌ NAPAČNO
bus_lon = group['lon'].mean()
```

To je vzelo povprečje VSEH uporabnikov, vključno s tistimi na postajah (3-4 km stran)!

**Popravek:**
```python
bus_lat = group['bus_lat'].iloc[0]  # ✅ PRAVILNO
bus_lon = group['bus_lon'].iloc[0]

on_bus_users = group[group['user_type'] == 'on_bus']  # Samo uporabniki NA avtobusu
```

**Rezultat:** 3x izboljšava (545m → 188m MAE)

## Odvisnosti

```bash
# Python paketi
pip install numpy pandas matplotlib seaborn scikit-learn tensorflow

# C++ compiler
clang++ (ali g++)
```

## Možne izboljšave

1. **Attention mehanizem** - Učenje utežitve uporabnikov po signal_strength
2. **Večja mreža** - LSTM/GRU za časovno sekvenco
3. **Več podatkov** - Simulacija večih linij in več scenarijev
4. **Weighted average** - Utežitev uporabnikov po moči signala
5. **Data augmentation** - Več šuma, različni vremenski pogoji

## Avtorji

Projekt za predmet "Uvod v računalniški vid in razpoznavanje vzorcev", 2025.

## Licenca

Izobraževalni projekt - uporaba dovoljena.
