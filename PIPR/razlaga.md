# Analiza skladnosti kode s kriteriji

## 1. Struktura bloka, povezovanje blokov v verigo (5 t)

### Struktura bloka
**Datoteka:** `src/Block.h`
- **Vrstice 8-14:** Definicija strukture bloka z vsemi zahtevanimi elementi:
  - `index` (vrstica 8)
  - `data` (vrstica 9)
  - `timestamp` (vrstica 10)
  - `hash` (vrstica 11)
  - `previousHash` (vrstica 12)
  - `difficulty` (vrstica 13)
  - `nonce` (vrstica 14)

**Datoteka:** `src/Block.cpp`
- **Vrstice 48-65:** Konstruktor bloka, ki inicializira vse elemente

### Povezovanje blokov v verigo
**Datoteka:** `src/Blockchain.cpp`
- **Vrstice 19-30:** Ustvarjanje genesis bloka z `previousHash = "0"` (vrstica 25)
- **Vrstice 42-56:** Metoda `AddBlock`, ki doda nov blok v verigo
- **Vrstice 32-40:** Metoda `GetChain`, ki vrne celotno verigo blokov

## 2. Validacija zgoščenih vrednosti, blokov in verige (5 t)

### Izračun zgoščene vrednosti
**Datoteka:** `src/Block.cpp`
- **Vrstice 67-73:** Metoda `toStringForHash` - serializacija za hash v formatu: `indeks + podatki + časovna značka + zgoščena vrednost prejšnjega bloka + težavnost + žeton`
- **Vrstice 75-77:** Metoda `computeHash` - izračun SHA256 hasha

**Datoteka:** `src/Crypto.cpp`
- **Vrstice 5-7:** Implementacija SHA256 hash funkcije

### Validacija blokov
**Datoteka:** `src/Blockchain.cpp`
- **Vrstice 58-90:** Metoda `isValidNewBlock` - validacija novega bloka:
  - **Vrstica 59:** Preverjanje, da je indeks trenutnega bloka za 1 večji od prejšnjega
  - **Vrstica 63:** Preverjanje, da se zgoščena vrednost prejšnjega bloka ujema z `previousHash` trenutnega bloka
  - **Vrstice 67-69:** Preverjanje, da se shranjena zgoščena vrednost trenutnega bloka ujema z izračunano
  - **Vrstice 76-87:** Preverjanje, da zgoščena vrednost ima zahtevano število začetnih ničel (težavnost)

### Validacija verige
**Datoteka:** `src/Blockchain.cpp`
- **Vrstice 177-238:** Metoda `ValidateChain` - validacija celotne verige:
  - **Vrstice 184-215:** Validacija genesis bloka (index=0, previousHash="0")
  - **Vrstice 217-235:** Validacija vseh blokov v verigi z uporabo `isValidNewBlock`

## 3. Ustvarjanje novih blokov (rudarjenje) s pomočjo paralelizacije na enem računalniku (20 t)

**Datoteka:** `src/Mining.cpp`
- **Vrstice 51-114:** Metoda `mineBlockThreaded` - paralelizirano rudarjenje z več nitmi:
  - **Vrstice 64-67:** Uporaba `std::atomic<bool>` za koordinacijo med nitmi
  - **Vrstice 69-96:** Worker funkcija, ki izvaja algoritem rudarjenja:
    - **Vrstice 74-75:** Ustvarjanje kandidatnega bloka z različnimi vrednostmi nonce
    - **Vrstica 75:** Izračun zgoščene vrednosti
    - **Vrstica 77:** Preverjanje, ali ima hash zahtevano število začetnih ničel
    - **Vrstice 79-86:** Ko prva nit najde veljavno rešitev, shrani rezultat
    - **Vrstica 90:** Povečanje nonce za stride (razdelitev prostora nonce med nitmi)
  - **Vrstice 98-107:** Ustvarjanje in zaganjanje več niti za paralelizacijo

**Datoteka:** `src/Mining.cpp`
- **Vrstice 28-49:** Metoda `mineBlock` - sekvenčno rudarjenje (osnovna implementacija algoritma):
  - **Vrstica 35:** Začetna vrednost nonce = 0
  - **Vrstice 38-48:** While zanka z algoritmom rudarjenja:
    - **Vrstice 39-40:** Ustvarjanje kandidatnega bloka in izračun hasha
    - **Vrstice 42-45:** Preverjanje začetnih ničel in vračanje bloka, če je veljaven
    - **Vrstica 47:** Povečanje nonce

## 4. Ustvarjanje novih blokov (rudarjenje) s pomočjo paralelizacije v porazdeljenem okolju in na nivoju lokalnega računalnika (20 t)

**Datoteka:** `src/DistributedMining.cpp`
- **Vrstice 421-1028:** Metoda `mineBlockMPI` - porazdeljeno rudarjenje z MPI:
  - **Vrstice 491-525:** Sinhronizacija verige pred rudarjenjem (snapshot)
  - **Vrstice 529-530:** Broadcast parametrov rudarjenja vsem procesom
  - **Vrstice 537-538:** Razdelitev prostora nonce med MPI procese (nonceStart, nonceStride)
  - **Vrstice 545-550:** Sinhronizacijske spremenljivke za koordinacijo med procesi
  - **Vrstice 609-714:** Komunikacijska nit na rank 0 za sprejemanje najdenih blokov
  - **Vrstice 717-807:** Worker funkcija za vsako nit v vsakem procesu:
    - **Vrstice 719-721:** Razdelitev prostora nonce med nitmi in procesi
    - **Vrstice 759-760:** Ustvarjanje kandidatnega bloka in izračun hasha
    - **Vrstice 762-797:** Ko najde veljavno rešitev, pošlje sporočilo rank 0 (ali doda v lokalno vrsto za rank 0)
  - **Vrstice 809-820:** Ustvarjanje in zaganjanje niti na vsakem procesu
  - **Vrstice 871-885:** Rank 0 distribuira zmagovalni blok vsem procesom
  - **Vrstice 886-1025:** Drugi procesi prejemajo zmagovalni blok

**Datoteka:** `src/MiningService.cpp`
- **Vrstice 49-81:** Integracija lokalnega in porazdeljenega rudarjenja:
  - **Vrstice 51-61:** Če je uporabljen MPI, kliče `DistributedMining::mineBlockMPI`
  - **Vrstice 64-77:** Če ni MPI, uporablja lokalno paralelizirano rudarjenje (`Mining::mineBlockThreaded` ali `Mining::mineBlock`)

## 5. Določanje trenutne težavnosti (10 t)

**Datoteka:** `src/Blockchain.cpp`
- **Vrstice 124-150:** Metoda `GetAdjustedDifficulty`:
  - **Vrstica 131:** Preverjanje, ali je potreben popravek težavnosti (vsakih `difficultyAdjustmentInterval` blokov)
  - **Vrstice 135-136:** Določitev prilagoditvenega bloka (blok na poziciji `dolžina verige - interval popravka`)
  - **Vrstice 137-139:** Izračun pričakovanega časa (`čas ustvarjanja bloka * interval popravka`) in dejanskega časa (`časovna značka zadnjega bloka - časovna značka prilagoditvenega bloka`)
  - **Vrstice 141-142:** Povečanje težavnosti za 1, če je dejanski čas < pričakovani čas / 2
  - **Vrstice 145-146:** Zmanjšanje težavnosti za 1, če je dejanski čas > pričakovani čas * 2
  - **Vrstica 149:** Vrnitev trenutne težavnosti, če ni potreben popravek

**Datoteka:** `src/Blockchain.h`
- **Vrstice 10-12:** Konstruktor z nastavitvami `blockGenerationInterval` (privzeto 10 sekund) in `difficultyAdjustmentInterval` (privzeto 10 blokov)

## 6. Validacija časovnih značk (10 t)

**Datoteka:** `src/Blockchain.cpp`
- **Vrstice 90-99:** Validacija časovnih značk v metodi `isValidNewBlock`:
  - **Vrstice 91-94:** Preverjanje, da je časovna značka bloka največ 1 minuto večja od trenutnega časa
  - **Vrstice 96-99:** Preverjanje, da je časovna značka bloka največ 1 minuto manjša od časovne značke prejšnjega bloka
- **Vrstice 217-235:** Validacija časovnih značk v verigi - metoda `ValidateChain` uporablja `isValidNewBlock`, ki že vključuje validacijo časovnih značk za vsak blok v verigi

## 7. Validacija verige s kumulativno težavnostjo (10 t)

**Datoteka:** `src/Blockchain.cpp`
- **Vrstice 152-159:** Metoda `CalculateCumulativeDifficulty`:
  - **Vrstice 154-157:** Izračun kumulativne težavnosti kot vsota `2^težavnost` za vsak blok v verigi
- **Vrstice 161-175:** Metoda `IsBetterChain`:
  - **Vrstice 162-163:** Izračun kumulativne težavnosti za trenutno in kandidatno verigo
  - **Vrstice 165-166:** Izbira verige z večjo kumulativno težavnostjo
  - **Vrstice 169-171:** V primeru enake kumulativne težavnosti, izbira daljše verige

**Datoteka:** `src/Blockchain.cpp`
- **Vrstice 101-122:** Metoda `ReplaceChain`:
  - **Vrstica 103:** Validacija kandidatne verige
  - **Vrstica 116:** Uporaba `IsBetterChain` za preverjanje, ali ima kandidatna veriga večjo kumulativno težavnost
  - **Vrstica 120:** Zamenjava verige, če je kandidatna veriga boljša
