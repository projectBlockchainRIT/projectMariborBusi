# Izvleček: Napoved Lokacije Avtobusa z Nevronsko Mrežo

## Pregled sistema

Sistem za napoved lokacije avtobusa na podlagi GPS podatkov uporabnikov, ki se nahajajo v avtobusu. Sistem uporablja simulirane podatke in nevronsko mrežo za učenje ter napovedovanje prave lokacije avtobusa.

---

## Struktura in delovanje `run_all.py`

### Opis
`run_all.py` je master skripta, ki avtomatično izvaja celoten pipeline projekta od simulacije do vizualizacije. Skripta koordinira vse korake in zagotavlja, da so podatki pravilno pripravljeni in procesirani.

### Arhitektura skripte

Skripta je strukturirana v **5 glavnih korakov**:

#### **KORAK 1: Kompajliranje C++ simulatorja**
- **Namen**: Priprava simulatorja za generiranje GPS podatkov
- **Proces**:
  - Zaznava operacijskega sistema (Windows/Linux/Mac)
  - Izbere ustrezen compiler (`g++` za Windows, `clang++` za Unix)
  - Kompajlira `main_v2.cpp` v izvršljivo datoteko (`main_v2.exe` ali `main_v2`)
- **Izhod**: Izvršljiva datoteka simulatorja

#### **KORAK 2: Generiranje simulacijskih podatkov**
- **Namen**: Ustvarjanje realističnih GPS podatkov uporabnikov
- **Proces**:
  - Zažene C++ simulator z izbrano avtobusno linijo (npr. G1)
  - Simulator generira CSV datoteke z GPS merjenji
- **Izhod**: CSV datoteke v `data/` mapi z formatom:
  ```
  user_id, route, lat, lon, timestamp, signal_strength, accuracy_meters, 
  user_type, bus_lat, bus_lon
  ```

#### **KORAK 3: Priprava treningskih podatkov**
- **Namen**: Transformacija simulacijskih podatkov v format primeren za učenje
- **Proces** (izvede `prepare_training_data.py`):
  1. **Nalaganje podatkov**: Branje CSV datotek
  2. **Filtriranje**: Izbira samo uporabnikov z `user_type == 'on_bus'` (ignorira uporabnike na postajah)
  3. **Ustvarjanje parov (X, y)**:
     - **X (vhod)**: Vektor z GPS koordinatami in močjo signala za vse uporabnike v avtobusu na določenem časovnem trenutku
     - **y (izhod)**: Prava lokacija avtobusa (`bus_lat`, `bus_lon`) iz simulatorja
  4. **Padding**: Normalizacija dolžine vektorjev z zero-padding (ker je različno število uporabnikov)
  5. **Normalizacija**: StandardScaler za X in y
  6. **Razdelitev**: Train (70%) / Validation (10%) / Test (20%)
- **Izhod**: 
  - `.npy` datoteke z normaliziranimi podatki
  - `.pkl` datoteke s scalerji za denormalizacijo
  - `metadata.json` z metapodatki

#### **KORAK 4: Treniranje nevronske mreže**
- **Namen**: Učenje modela za napoved lokacije avtobusa
- **Proces** (izvede `train_model.py`):
  1. Nalaganje pripravljenih podatkov
  2. Gradnja arhitekture mreže
  3. Treniranje z Adam optimizerjem
  4. Evaluacija na testnih podatkih
  5. Shranjevanje modela in rezultatov
- **Izhod**: 
  - Trenirani model (`{route}_model.h5`)
  - Metrike (MAE, RMSE)
  - Vizualizacije (loss krivulje, scatter ploti)

#### **KORAK 5: Interaktivna vizualizacija**
- **Namen**: Prikaz rezultatov z interaktivnim časovnim drsnikom
- **Proces** (izvede `interactive_viz.py`):
  - Nalaganje modela in podatkov
  - Generiranje napovedi za vsak časovni trenutek
  - Interaktivni prikaz z možnostjo premikanja skozi čas
- **Izhod**: Interaktivno okno z vizualizacijo

### Funkcionalnosti skripte

- **Avtomatska zaznava sistema**: Prilagaja se Windows/Linux/Mac
- **Error handling**: Preverjanje obstoja datotek in uspešnosti korakov
- **Barvni izpis**: Uporaba ANSI barv za boljšo berljivost
- **Interaktivni vnos**: Vprašanja za izbiro linije in parametrov

---

## Nevronska mreža: Kako deluje

### Arhitektura modela

Model je **feedforward nevronska mreža** (Sequential) z naslednjo strukturo:

```
Input Layer (N features)
    ↓
Dense(128) + ReLU + Dropout(0.2)
    ↓
Dense(64) + ReLU + Dropout(0.2)
    ↓
Dense(32) + ReLU + Dropout(0.1)
    ↓
Dense(16) + ReLU
    ↓
Output Layer(2) - Linear activation
    ↓
[lat, lon]
```

### Parametri učenja

- **Optimizer**: Adam (learning rate = 0.001)
- **Loss funkcija**: Mean Squared Error (MSE)
- **Metrika**: Mean Absolute Error (MAE)
- **Epochs**: 50 (konfigurabilno)
- **Batch size**: 16
- **Dropout**: Za preprečevanje overfittinga

### Kako se uči

1. **Forward pass**: 
   - Vhodni vektor (GPS koordinate + signal strength vseh uporabnikov) gre skozi mrežo
   - Vsaka plast izračuna uteženo vsoto in uporabi aktivacijsko funkcijo
   - Izhod je napovedana lokacija `[pred_lat, pred_lon]`

2. **Backward pass**:
   - Izračuna se napaka med napovedjo in pravo lokacijo (ground truth)
   - Gradient se propagira nazaj skozi mrežo
   - Uteži se posodobijo z Adam optimizerjem

3. **Cilj učenja**:
   - Minimizirati MSE med napovedano in pravo lokacijo avtobusa
   - Doseči čim manjšo MAE (npr. < 200m)

---

## Nad čim se uči

### Vhodni podatki (X)

**Format**: Vektor z dolžino N (odvisno od števila uporabnikov)

Za vsakega uporabnika v avtobusu na določenem časovnem trenutku:
- `lat` - Latitude GPS koordinata uporabnika
- `lon` - Longitude GPS koordinata uporabnika  
- `signal_strength / 100.0` - Normalizirana moč GPS signala (0.0 - 1.0)

**Primer**: Če je v avtobusu 5 uporabnikov, je vhodni vektor dolg 15 elementov (5 × 3).

**Pomembno**: 
- Uporabljajo se **samo uporabniki v avtobusu** (`user_type == 'on_bus'`)
- Uporabniki na postajah se ignorirajo (ker so predaleč)
- Podatki so normalizirani z StandardScaler

### Izhodni podatki (y) - Ground Truth

**Format**: Vektor z 2 elementoma
- `bus_lat` - Prava latitude lokacije avtobusa (iz simulatorja)
- `bus_lon` - Prava longitude lokacije avtobusa (iz simulatorja)

**Pomembno**: 
- Ground truth je **prava lokacija avtobusa** iz simulatorja, ne povprečje uporabnikov
- To je ključna razlika, ki omogoča natančno učenje

### Podatki za učenje

- **Train set**: 70% podatkov - za učenje modela
- **Validation set**: 10% podatkov - za prilagajanje hiperparametrov
- **Test set**: 20% podatkov - za končno evaluacijo

---

## Produkt/Rezultat

### 1. Trenirani model
- **Datoteka**: `results/{route}_model.h5`
- **Format**: TensorFlow/Keras model
- **Uporaba**: Napoved lokacije avtobusa iz GPS podatkov uporabnikov

### 2. Metrike uspešnosti
- **Datoteka**: `results/{route}_evaluation.json`
- **Vsebina**:
  - **MAE (Mean Absolute Error)**: Povprečna absolutna napaka v stopinjah in metrih
    - Primer: 187.8m na testnih podatkih
  - **RMSE (Root Mean Squared Error)**: Koren povprečne kvadratne napake
    - Primer: 286.2m na testnih podatkih

### 3. Vizualizacije
- **`{route}_training_history.png`**: 
  - Loss krivulja (train/validation)
  - MAE krivulja (train/validation)
  - Prikaz konvergence učenja

- **`{route}_predictions.png`**:
  - Scatter plot: Napovedana vs prava latitude
  - Scatter plot: Napovedana vs prava longitude
  - Geografski prikaz: Prava vs napovedana lokacija na karti
  - Histogram napak: Razporeditev napak v metrih

### 4. Interaktivna vizualizacija
- **Program**: `interactive_viz.py`
- **Funkcionalnost**:
  - Prikaz uporabnikovih GPS lokacij
  - Prikaz napovedane lokacije avtobusa
  - Prikaz prave lokacije avtobusa
  - Časovni slider za premikanje skozi simulacijo
  - Real-time napovedi za vsak časovni trenutek

### 5. Shranjeni podatki
- **Treningski podatki**: `training_data/{route}_training_*.npy`
- **Scalerji**: `training_data/{route}_training_*_scaler_*.pkl`
- **Metapodatki**: `training_data/{route}_training_*_metadata.json`

---

## Vhod v sistem

### Začetni vhod

1. **Konfiguracija linije**:
   - **Datoteka**: `routes.json`
   - **Vsebina**: Definicije avtobusnih prog z GPS koordinatami
   - **Format**: JSON z array-ji točk `[lat, lon]` za vsako linijo

2. **Uporabniški vnos** (pri zagonu `run_all.py`):
   - Izbira avtobusne linije (npr. "G1")
   - Izbira simulatorja (V1 ali V2)
   - Število epoch za treniranje (privzeto 50)

### Vhod v nevronsko mrežo (med napovedovanjem)

**Format**: Normaliziran vektor z GPS podatki uporabnikov

**Struktura**:
```
X = [
  lat_user1, lon_user1, signal_strength_user1,
  lat_user2, lon_user2, signal_strength_user2,
  ...
  lat_userN, lon_userN, signal_strength_userN,
  0, 0, 0, ...  # Zero-padding če je manj uporabnikov
]
```

**Dimenzije**:
- Dolžina: `max_users × 3` (kjer je max_users maksimalno število uporabnikov v trenutku)
- Vrednosti: Normalizirane z StandardScaler (povprečje ≈ 0, standardni odklon ≈ 1)

**Primer**:
- Če je maksimalno 5 uporabnikov: vektor dolžine 15
- Če je v trenutku samo 3 uporabniki: prvi 9 elementov so podatki, zadnji 6 so ničle

---

## Povzetek delovanja

1. **Simulacija** → Generira GPS podatke uporabnikov in prave lokacije avtobusa
2. **Priprava** → Transformira podatke v format (X, y) parov
3. **Učenje** → Nevronska mreža se uči povezave med GPS podatki uporabnikov in lokacijo avtobusa
4. **Napovedovanje** → Model napove lokacijo avtobusa iz novih GPS podatkov uporabnikov
5. **Vizualizacija** → Prikaz rezultatov in napovedi

**Ključna ideja**: Če več uporabnikov v avtobusu pošilja GPS podatke, lahko iz njihovih (zašumljenih) meritev izračunamo pravo lokacijo avtobusa, čeprav posamezne meritve niso natančne.

---

## Rezultati

**Uspešnost modela** (na testnih podatkih za linijo G1):
- **MAE**: 187.8 metrov
- **RMSE**: 286.2 metrov

To pomeni, da model napove lokacijo avtobusa s povprečno napako manj kot 200 metrov, kar je dovolj natančno za praktično uporabo v sistemih javnega prevoza.
