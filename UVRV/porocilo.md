# Poročilo: Datotečni sistem projekta UVRV

## Pregled projekta

Projekt **UVRV** (Uvod v računalniški vid in razpoznavanje vzorcev) je kompleksni sistem za analizo in napovedovanje v javnem prevozu. Projekt vsebuje tri glavne komponente, ki skupaj omogočajo napoved lokacije avtobusa in zaznavanje zasedenosti.

**Datum generiranja poročila**: 2026-01-21

---

## Struktura direktorijev

```
UVRV/
├── GPS_clustring/          # Glavni projekt: Napoved lokacije avtobusa
├── PeopleNN/               # Projekt: Zaznavanje ljudi v avtobusu
├── Adrian_PROJEKT/         # Različica/kopija GPS_clustring projekta
├── venv/                   # Python virtualno okolje
└── porocilo.md            # To poročilo
```

---

## 1. GPS_clustring - Napoved lokacije avtobusa

### 1.1 Opis
Glavni projekt za napoved lokacije avtobusa na podlagi GPS podatkov uporabnikov, ki se nahajajo v avtobusu. Sistem uporablja simulirane podatke in nevronsko mrežo za učenje ter napovedovanje.

### 1.2 Struktura direktorija

```
GPS_clustring/
├── __pycache__/                    # Python cache datoteke
├── data/                           # Simulirani GPS podatki
│   ├── G1_all_*.csv               # CSV datoteke z GPS merjenji
│   ├── G1_dense.csv               # Gostejši podatki
│   ├── G1_multibus_*.csv          # Multi-bus simulacije
│   └── G2_all_*.csv               # Podatki za linijo G2
├── training_data/                   # Pripravljeni podatki za učenje
│   ├── G1_training_*_X_train.npy  # Vhodni podatki (train)
│   ├── G1_training_*_X_val.npy    # Vhodni podatki (validation)
│   ├── G1_training_*_X_test.npy   # Vhodni podatki (test)
│   ├── G1_training_*_y_train.npy  # Izhodni podatki (train)
│   ├── G1_training_*_y_val.npy    # Izhodni podatki (validation)
│   ├── G1_training_*_y_test.npy   # Izhodni podatki (test)
│   ├── G1_training_*_scaler_X.pkl # Scaler za normalizacijo X
│   ├── G1_training_*_scaler_y.pkl # Scaler za normalizacijo y
│   └── G1_training_*_metadata.json # Metapodatki
├── results/                         # Rezultati treniranja in evaluacije
│   ├── G1_model.h5                 # Trenirani TensorFlow model
│   ├── G1_evaluation.json          # Metrike (MAE, RMSE)
│   ├── G1_training_history.png     # Krivulje učenja
│   ├── G1_predictions.png          # Vizualizacije napovedi
│   ├── G1_heatmap_*.json           # Metapodatki za heatmap modele
│   └── *.pt                        # PyTorch modeli (za CNN pristop)
├── main_v2.cpp                      # C++ simulator GPS podatkov
├── main_v2.exe                     # Kompajlirani simulator (Windows)
├── json.hpp                         # JSON knjižnica za C++
├── routes.json                      # Definicije avtobusnih prog
├── run_all.py                       # Master skripta (celoten pipeline)
├── prepare_training_data.py        # Priprava podatkov za učenje
├── train_model.py                  # Treniranje nevronske mreže
├── interactive_viz.py              # Interaktivna vizualizacija (ne obstaja?)
├── interactive_cnn_viz.py         # Interaktivna CNN vizualizacija
├── simple_viz.py                   # Enostavna vizualizacija
├── generate_multibus.py            # Generator multi-bus podatkov
├── heatmap_cnn.py                  # CNN pristop z heatmap
├── requirements.txt                 # Python odvisnosti
├── README.md                        # Dokumentacija projekta
└── izvlecek.md                      # Izvleček o delovanju sistema
```

### 1.3 Ključne datoteke

#### **C++ Simulator** (`main_v2.cpp`)
- **Namen**: Generiranje realističnih GPS podatkov uporabnikov
- **Funkcionalnost**:
  - Simulira avtobus, ki se premika po definirani poti
  - Generira GPS podatke za uporabnike v avtobusu in na postajah
  - Doda realističen šum in variabilno moč signala
  - Shranjuje podatke v CSV format
- **Izhod**: CSV datoteke v `data/` z formatom:
  ```
  user_id, route, lat, lon, timestamp, signal_strength, 
  accuracy_meters, user_type, bus_lat, bus_lon
  ```

#### **Master skripta** (`run_all.py`)
- **Namen**: Avtomatski izvajalec celotnega pipeline-a
- **Funkcionalnost**:
  1. Kompajliranje C++ simulatorja
  2. Generiranje simulacijskih podatkov
  3. Priprava treningskih podatkov
  4. Treniranje nevronske mreže
  5. Interaktivna vizualizacija
- **Značilnosti**:
  - Avtomatska zaznava sistema (Windows/Linux/Mac)
  - Barvni izpis in error handling
  - Interaktivni vnos parametrov

#### **Priprava podatkov** (`prepare_training_data.py`)
- **Namen**: Transformacija simulacijskih podatkov v format za učenje
- **Proces**:
  - Filtriranje uporabnikov (samo `on_bus`)
  - Ustvarjanje parov (X, y)
  - Padding in normalizacija
  - Razdelitev na train/val/test (70%/10%/20%)
- **Izhod**: `.npy` datoteke, `.pkl` scalerji, `metadata.json`

#### **Treniranje modela** (`train_model.py`)
- **Namen**: Učenje nevronske mreže
- **Arhitektura**:
  ```
  Input(N) → Dense(128) → Dropout(0.2) → 
  Dense(64) → Dropout(0.2) → 
  Dense(32) → Dropout(0.1) → 
  Dense(16) → Output(2: lat, lon)
  ```
- **Parametri**: Adam optimizer, MSE loss, 50 epochs, batch size 16
- **Izhod**: Model (`.h5`), metrike (`.json`), vizualizacije (`.png`)

#### **CNN pristop** (`heatmap_cnn.py`)
- **Namen**: Alternativni pristop z konvolucijsko nevronsko mrežo
- **Koncept**: Discretizacija GPS koordinat v 64x64 heatmap, nato CNN za detekcijo
- **Možnosti**:
  - Single Bus: Napoved ene lokacije
  - Multi-Bus: Napoved več avtobusov hkrati
  - Segmentation: Verjetnostna maska lokacij

#### **Multi-bus generator** (`generate_multibus.py`)
- **Namen**: Generiranje podatkov z več avtobusi na isti liniji
- **Funkcionalnost**: Realistična simulacija 4 avtobusov z razpršenimi uporabniki

### 1.4 Podatkovni formati

#### **CSV podatki** (`data/*.csv`)
```csv
user_id,route,lat,lon,timestamp,signal_strength,accuracy_meters,user_type,bus_lat,bus_lon
1,G1,46.5595,15.6560,1768973065,85.2,12.5,on_bus,46.5595,15.6560
2,G1,46.5594,15.6561,1768973065,78.3,15.2,on_bus,46.5595,15.6560
...
```

#### **Treningski podatki** (`.npy`)
- **X**: Normalizirani vektorji z GPS koordinatami in signal strength
- **y**: Normalizirane koordinate avtobusa (lat, lon)

#### **Modeli**
- **TensorFlow**: `.h5` format (Keras)
- **PyTorch**: `.pt` format (za CNN pristop)

### 1.5 Rezultati
- **MAE**: 187.8 metrov na testnih podatkih
- **RMSE**: 286.2 metrov
- **Uspešnost**: Model napove lokacijo avtobusa s povprečno napako < 200m

---

## 2. PeopleNN - Zaznavanje ljudi v avtobusu

### 2.1 Opis
Sistem za zaznavanje in štetje ljudi v avtobusu z uporabo računalniškega vida. Uporablja Faster R-CNN model za detekcijo oseb in klasificira zasedenost kot: prazno, pol-prazno, polno.

### 2.2 Struktura direktorija

```
PeopleNN/
├── __pycache__/                    # Python cache
├── emptyBus/                       # Test slike: prazni avtobusi
│   ├── empty_1.jpg
│   ├── empty_2.jpg
│   └── ... (9 slik)
├── semiFull/                        # Test slike: pol-prazni avtobusi
│   ├── semiFull_1.jpg
│   ├── semiFull_2.jpg
│   └── ... (10 slik)
├── fullBus/                         # Test slike: polni avtobusi
│   ├── full_1.jpg
│   ├── full_2.jpg
│   └── ... (11 slik)
├── results/                         # Rezultati detekcije
│   ├── empty_*_result.png          # Vizualizacije za prazne
│   ├── semiFull_*_result.png       # Vizualizacije za pol-prazne
│   └── full_*_result.png            # Vizualizacije za polne
├── report_results/                  # Poročila in rezultati
│   ├── *_result.png                # Vse vizualizacije
│   └── results.md                   # Poročilo o uspešnosti
└── people_detector.py               # Glavni skript za detekcijo
```

### 2.3 Ključne datoteke

#### **Detektor ljudi** (`people_detector.py`)
- **Model**: Faster R-CNN ResNet-50 FPN v2 (PyTorch)
- **Funkcionalnost**:
  - Detekcija oseb v slikah avtobusa
  - Štetje zaznanih oseb
  - Klasifikacija zasedenosti:
    - **empty**: 0 oseb
    - **semi_full**: 1-8 oseb
    - **full**: 9+ oseb
  - Vizualizacija z bounding boxi
- **Parametri**:
  - Confidence threshold: 0.7
  - Semi-full range: 1-8 oseb
  - Max image dimension: 1024px

### 2.4 Rezultati

**Uspešnost klasifikacije**:
- **emptyBus**: 8/9 (88.9%)
- **semiFull**: 9/10 (90.0%)
- **fullBus**: 10/11 (90.9%)
- **Skupna natančnost**: ~90%

### 2.5 Testni podatki
- **9 slik** praznih avtobusov
- **10 slik** pol-praznih avtobusov
- **11 slik** polnih avtobusov
- **Skupaj**: 30 testnih slik

---

## 3. Adrian_PROJEKT - Različica GPS_clustring

### 3.1 Opis
Direktorij, ki vsebuje kopijo ali različico GPS_clustring projekta. Verjetno razvojna verzija ali backup.

### 3.2 Struktura

```
Adrian_PROJEKT/
├── __pycache__/
├── main_v2.cpp                      # C++ simulator
├── main_v2                          # Kompajlirani simulator (Unix)
├── json.hpp                         # JSON knjižnica
├── routes.json                      # Definicije prog
├── run_all.py                       # Master skripta
├── prepare_training_data.py         # Priprava podatkov
├── train_model.py                  # Treniranje
├── interactive_viz.py             # Interaktivna vizualizacija
├── simple_viz.py                   # Enostavna vizualizacija
├── generate_multibus.py            # Multi-bus generator
├── heatmap_cnn.py                  # CNN pristop
├── model_predictions_viz_sl.py    # Vizualizacija z sliderjem
└── README.md                        # Dokumentacija
```

### 3.3 Razlike od GPS_clustring
- Vsebuje `interactive_viz.py` (v GPS_clustring morda manjka)
- Vsebuje `model_predictions_viz_sl.py` (dodatna vizualizacija)
- Manjka `requirements.txt`
- Manjka `izvlecek.md` in `CLAUDE.md`

---

## 4. venv - Virtualno okolje

### 4.1 Opis
Python virtualno okolje z nameščenimi paketi za projekt.

### 4.2 Struktura

```
venv/
├── Include/                         # C header datoteke
├── Lib/                             # Python knjižnice
│   └── site-packages/              # Nameščeni paketi
│       ├── numpy/
│       ├── pandas/
│       ├── matplotlib/
│       ├── tensorflow/
│       ├── torch/
│       └── ...
├── Scripts/                          # Izvršljive datoteke
│   ├── activate.bat                # Aktivacija (Windows)
│   ├── Activate.ps1                # Aktivacija (PowerShell)
│   ├── python.exe                  # Python interpreter
│   └── pip.exe                     # Package manager
├── share/                           # Deljene datoteke
└── pyvenv.cfg                       # Konfiguracija okolja
```

### 4.3 Nameščeni paketi (iz `requirements.txt`)
- `pandas >= 2.0.0`
- `numpy >= 1.24.0`
- `matplotlib >= 3.7.0`
- `seaborn >= 0.12.0`
- `scikit-learn >= 1.3.0`
- `tensorflow >= 2.13.0`
- `torch` (za PeopleNN)
- `torchvision` (za PeopleNN)

---

## 5. Povezave med komponentami

### 5.1 GPS_clustring → PeopleNN
- **Potencialna integracija**: Kombinacija napovedi lokacije z zaznavanjem zasedenosti
- **Uporaba**: Celostna analiza avtobusa (kje je + koliko ljudi)

### 5.2 Skupne komponente
- **routes.json**: Definicije avtobusnih prog (uporablja GPS_clustring)
- **venv**: Skupno virtualno okolje za vse Python projekte

---

## 6. Datotečni tipi in njihova uporaba

### 6.1 Python datoteke (`.py`)
- **Skripte**: `run_all.py`, `train_model.py`, `people_detector.py`
- **Moduli**: Različni moduli za obdelavo podatkov in vizualizacijo

### 6.2 C++ datoteke
- **`.cpp`**: Izvorna koda simulatorja (`main_v2.cpp`)
- **`.exe` / brez končnice**: Kompajlirane izvršljive datoteke
- **`.hpp`**: Header datoteke (`json.hpp`)

### 6.3 Podatkovne datoteke
- **`.csv`**: Simulirani GPS podatki
- **`.npy`**: NumPy array datoteke (treningski podatki)
- **`.pkl`**: Pickle datoteke (scalerji, objekti)
- **`.json`**: Konfiguracije in metapodatki
- **`.h5`**: TensorFlow/Keras modeli
- **`.pt`**: PyTorch modeli

### 6.4 Vizualizacije
- **`.png`**: Slike rezultatov, grafov, detekcij
- **`.jpg`**: Test slike avtobusov

### 6.5 Dokumentacija
- **`.md`**: Markdown dokumentacija (README, CLAUDE, izvleček, poročilo)

---

## 7. Velikost in statistika

### 7.1 Približne velikosti
- **GPS_clustring/data/**: Več CSV datotek (vsaka ~1-10 MB)
- **GPS_clustring/results/**: Modeli in vizualizacije (~50-100 MB)
- **GPS_clustring/training_data/**: Treningski podatki (~10-50 MB)
- **PeopleNN/**: Slike in rezultati (~5-20 MB)
- **venv/**: Virtualno okolje (~500 MB - 2 GB)

### 7.2 Število datotek
- **GPS_clustring**: ~50-70 datotek
- **PeopleNN**: ~60-80 datotek (večinoma slike)
- **Adrian_PROJEKT**: ~15-20 datotek
- **Skupaj**: ~150-200 datotek

---

## 8. Delovni tok projekta

### 8.1 GPS_clustring pipeline
```
1. routes.json (definicije prog)
   ↓
2. main_v2.cpp → main_v2.exe (kompajliranje)
   ↓
3. Simulacija → data/*.csv
   ↓
4. prepare_training_data.py → training_data/*.npy
   ↓
5. train_model.py → results/*.h5, *.png, *.json
   ↓
6. interactive_viz.py (vizualizacija)
```

### 8.2 PeopleNN pipeline
```
1. Test slike (emptyBus/, semiFull/, fullBus/)
   ↓
2. people_detector.py
   ↓
3. Detekcija → results/*_result.png
   ↓
4. Poročilo → report_results/results.md
```

---

## 9. Odvisnosti in zahteve

### 9.1 Programske zahteve
- **Python**: 3.10+ (verjetno 3.13)
- **C++ Compiler**: `g++` (Windows) ali `clang++` (Unix/Mac)
- **CUDA**: Opcijsko za GPU pospešitev (TensorFlow, PyTorch)

### 9.2 Python paketi
Vsi paketi so navedeni v `GPS_clustring/requirements.txt`:
- Osnovni: numpy, pandas, matplotlib, seaborn
- Machine Learning: scikit-learn, tensorflow
- Computer Vision: torch, torchvision (za PeopleNN)

### 9.3 Sistemne zahteve
- **RAM**: Najmanj 8 GB (priporočeno 16 GB)
- **Disk**: ~2-5 GB prostora
- **GPU**: Opcijsko (pospešitev učenja)

---

## 10. Varnost in vzdrževanje

### 10.1 Gitignore
- `__pycache__/`: Python cache datoteke
- `*.pyc`: Kompajlirane Python datoteke
- `venv/`: Virtualno okolje (običajno)
- `*.exe`: Kompajlirane izvršljive datoteke (morda)

### 10.2 Backup
- **Pomembne datoteke**: `routes.json`, izvorna koda (`.py`, `.cpp`)
- **Modeli**: `results/*.h5`, `results/*.pt` (lahko ponovno trenirati)
- **Podatki**: `data/*.csv` (lahko ponovno generirati)

### 10.3 Vzdrževanje
- Redno čiščenje `__pycache__/`
- Arhiviranje starih rezultatov
- Posodabljanje odvisnosti (`pip install -r requirements.txt --upgrade`)

---

## 11. Zaključek

Projekt UVRV je dobro organiziran sistem z jasno strukturo in ločenimi komponentami:

1. **GPS_clustring**: Napredni sistem za napoved lokacije avtobusa
2. **PeopleNN**: Sistem za zaznavanje zasedenosti avtobusa
3. **Adrian_PROJEKT**: Razvojna/backup verzija
4. **venv**: Skupno virtualno okolje

Projekt uporablja moderne tehnike strojnega učenja (TensorFlow, PyTorch) in računalniškega vida za reševanje praktičnih problemov v javnem prevozu.

---

**Avtor poročila**: Generirano avtomatično  
**Datum**: 2026-01-21  
**Verzija**: 1.0
