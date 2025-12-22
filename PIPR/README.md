# Blockchain Mining Project

Blockchain implementacija s podporo za porazdeljeno rudarjenje z MPI in multi-threading.

## Gradnja projekta

### Windows

```powershell
# Konfiguriraj (samo prvič ali po spremembah CMakeLists.txt)
cmake -B build -S .

# Zgradi
cmake --build build --config Debug
```

### Linux/Mac

```bash
cmake -B build -S .
cmake --build build
```

## Zagon programa

### Osnovni zagon (brez MPI)

Program deluje kot navaden single-process program z multi-threading:

```powershell
# Windows
.\build\Debug\nal5_cplus.exe --threads 4

# Linux/Mac
./build/nal5_cplus --threads 4
```

**Parametri:**
- `--threads N`: Število niti za rudarjenje (privzeto: število CPU jedri)

### Zagon z MPI (porazdeljeno rudarjenje)

#### 1. En proces, ena nit (sekvenčno rudarjenje)
```powershell
# Windows
mpiexec -np 1 .\build\Debug\nal5_cplus.exe --threads 1

# Linux/Mac
mpirun -np 1 ./build/nal5_cplus --threads 1
```
**Rezultat:** Deluje kot navaden sekvenčni program (brez paralelizacije)

#### 2. En proces, več niti (multi-threaded rudarjenje)
```powershell
# Windows
mpiexec -np 1 .\build\Debug\nal5_cplus.exe --threads 4

# Linux/Mac
mpirun -np 1 ./build/nal5_cplus --threads 4
```
**Rezultat:** En MPI proces z 4 nitmi - deluje kot navaden multi-threaded program

#### 3. Več procesov, vsak z eno nitjo
```powershell
# Windows
mpiexec -np 4 .\build\Debug\nal5_cplus.exe --threads 1

# Linux/Mac
mpirun -np 4 ./build/nal5_cplus --threads 1
```
**Rezultat:** 4 MPI procesi, vsak z 1 nitjo = 4 procesi rudarijo vzporedno

#### 4. Več procesov, vsak z več nitmi (porazdeljeno + multi-threaded)
```powershell
# Windows
mpiexec -np 2 .\build\Debug\nal5_cplus.exe --threads 2

# Linux/Mac
mpirun -np 2 ./build/nal5_cplus --threads 2
```
**Rezultat:** 2 MPI procesa × 2 niti = 4 skupne "rudarske enote"

#### 5. Več procesov, različno število niti
```powershell
# Windows
mpiexec -np 3 .\build\Debug\nal5_cplus.exe --threads 4

# Linux/Mac
mpirun -np 3 ./build/nal5_cplus --threads 4
```
**Rezultat:** 3 MPI procesi × 4 niti = 12 skupnih "rudarskih enot"

## Primeri konfiguracij

| MPI procesi | Niti/proces | Skupne rudarske enote | Uporaba |
|-------------|-------------|----------------------|---------|
| 1 | 1 | 1 | Testiranje, debug |
| 1 | 4 | 4 | Single-process multi-threaded |
| 2 | 1 | 2 | Dva procesa, vsak z 1 nitjo |
| 2 | 2 | 4 | Dva procesa, vsak z 2 nitmi |
| 4 | 2 | 8 | Štirje procesi, vsak z 2 nitmi |
| 8 | 4 | 32 | Osem procesov, vsak s 4 nitmi |

## Kako deluje porazdeljeno rudarjenje

1. **Delitev nonce prostora:**
   - Skupni stride = `numProcesses × threadsPerProcess`
   - Vsak proces začne pri: `rank × threadsPerProcess`
   - Vsaka nit v procesu začne pri: `processOffset + threadId`
   - Vsaka nit povečuje nonce za: `totalStride`

2. **Ko en proces najde rešitev:**
   - Pošlje signal vsem ostalim procesom
   - Broadcasta najdeni blok vsem procesom
   - Vsi procesi se ustavijo

3. **Sinhronizacija:**
   - Rank 0 broadcasta končni blok vsem procesom
   - Vsi procesi dobijo isti blok za validacijo

## Namestitev MPI

### Windows

1. Prenesi Microsoft MPI:
   - https://www.microsoft.com/en-us/download/details.aspx?id=57467
   - Namesti oba paketa: MS-MPI Runtime in MS-MPI SDK

2. Ali z vcpkg:
   ```powershell
   vcpkg install msmpi
   ```

3. Dodaj MPI v PATH (če CMake ne najde MPI):
   - Odpri System Environment Variables
   - Dodaj: `C:\Program Files\Microsoft MPI\Bin`
   - Dodaj: `C:\Program Files (x86)\Microsoft SDKs\MPI`

### Linux

```bash
# Ubuntu/Debian
sudo apt-get install libopenmpi-dev openmpi-bin

# Fedora/CentOS
sudo dnf install openmpi-devel
```

### Mac

```bash
brew install open-mpi
```

## Komande v programu

Ko program teče, lahko uporabljaš naslednje komande:

- `mine` - Začne rudarjenje novega bloka
- `show` - Prikaže trenutno verigo
- `connect <port>` - Poveže se s peer-om na določenem portu
- `exit` - Zapre program

## Opombe

- Program avtomatsko zazna, če je MPI na voljo
- Če MPI ni nameščen, program deluje normalno (single-process)
- Pri več procesih se uporabniško ime broadcasta iz rank 0 na vse procese
- Vsi procesi morajo imeti dostop do iste verige (za konsistentnost)
