# Dokumentacija
#### Zaključna dokumentacija pri predmetu Sistemska administracija


## Prva stran

`Ime projekta:` M-Busi

`Člani skupine:`
- Adrian Cvetko, vodja
- Timotej Maučec
- Blaž Kolman

[`Povezava do GitHub repozitorija`](https://github.com/projectBlockchainRIT/projectMariborBusi)

```mermaid
gantt
    title Vodenje projekta
    dateFormat  YYYY-MM-DD
    
    section Začetek razvoja
    Vzpostavitev okolja za opravljanje razvoja       :a1, 2025-04-12, 14d
    Web scrapping       :after a1, 13d

    section Implementacija
    Izdelava domensko specifičnega jezika za opis infrastrukture v mestu       :a3, 2025-05-10, 28d
    Izdelava namizne aplikacije       :a4, 2025-05-10, 28d
    Implementacija spletne storitve       :a5, 2025-05-10, 28d
    Izdelava Spletne aplikacije (frontend)       :a8, 2025-05-20, 19d

    section Deployment
    Vzpostavitev okolja Azure       :a6, 2025-05-16, 9d
    Namestitev Dockerja in aplikacije       :a7, 2025-05-10, 15d
    Vzpostavitev CI/CD       :a9, 2025-06-4, 5d
```

## Primeri uporabe

Naša programska rešitev predstavlja spletno storitev za spremljanje avtobusnega prometa v Mariboru. 

Deluje na javnih podatkih o avtobusnem prometu v Mariboru, ki jih pridobimo z [Marproma.](https://vozniredi.marprom.si/) 

Omogoča prikaz voznih redov in linij na interaktivnem zemljevidu. Uporabniku posreduje analizo raznih podatkov o avtobusnih vožnjah s pomočjo grafov, kot je recimo zasedenost linij ali pa zamude določene trase. 

Glavna funkcionalnost pa je simuliranje predvidene realnočasovne lokacije avtobusov za izbrano linijo.

### Prikazi uporabe s pomočjo sekvenčnih diagramov

#### Prikaz voznega reda za izbrano linijo

<br>

```mermaid
sequenceDiagram
    participant Uporabnik
    participant SpletnaStoritev
    participant MarpromAPI
    Uporabnik->>SpletnaStoritev: Izbere avtobusno linijo
    SpletnaStoritev->>MarpromAPI: Pošlje zahtevo za voznim redom
    MarpromAPI-->>SpletnaStoritev: Vrne podatke o voznem redu
    SpletnaStoritev->>SpletnaStoritev: Obdeluje podatke
    SpletnaStoritev-->>Uporabnik: Prikaže vozel red na zemljevidu
```

<br>

#### Analiza zasedenosti linije

```mermaid
sequenceDiagram
    participant Uporabnik
    participant SpletnaStoritev
    participant MarpromAPI
    participant PodatkovnaBaza
    Uporabnik->>SpletnaStoritev: Zahteva analizo zasedenosti
    SpletnaStoritev->>MarpromAPI: Pridobi zgodovinske podatke
    MarpromAPI-->>SpletnaStoritev: Vrne podatke o prometu
    SpletnaStoritev->>PodatkovnaBaza: Shrani podatke za analizo
    PodatkovnaBaza-->>SpletnaStoritev: Potrdi shranjevanje
    SpletnaStoritev->>SpletnaStoritev: Analizira zasedenost
    SpletnaStoritev-->>Uporabnik: Prikaže graf zasedenosti
```

<br>

#### Simulacija realnočasovne lokacije avtobusa

```mermaid
sequenceDiagram
    participant Uporabnik
    participant SpletnaStoritev
    participant MarpromAPI
    participant SimulacijskiModul
    Uporabnik->>SpletnaStoritev: Zažene simulacijo izbrane linije
    SpletnaStoritev->>MarpromAPI: Pridobi trenutne podatke
    MarpromAPI-->>SpletnaStoritev: Vrne realnočasovne podatke
    SpletnaStoritev->>SimulacijskiModul: Posreduje podatke
    SimulacijskiModul->>SimulacijskiModul: Izračuna pozicije avtobusov
    SimulacijskiModul-->>SpletnaStoritev: Vrne simulacijske podatke
    SpletnaStoritev-->>Uporabnik: Prikaže simulacijo na zemljevidu
```

<br>

## Primeri uporabe

```mermaid
architecture-beta
    group architecture(server)[Architecture]

    service db(database)[Database] in architecture
    service server(server)[Server] in architecture
    service frontend(server)[Frontend] in architecture
    service internet(internet)[Public access] in architecture
    service webhook(internet)[Webhook] in architecture

    db:L -- R:server
    server:L -- R:frontend
    frontend:L -- R:internet
    webhook:T -- B:server
  
```

### Uporabljene tehnologije
Za zaledni del oz. spletno storitev smo se odločiti za programski jezik GO. Za ta programski jezik smo se odločili, ker ima zelo močen "error handling", je zelo učinkovit in prevajan, ampak kljub temu omogoča zelo veliko funkcionalnosti za grajenje robustnih spletnih storitev. 

Za podatkovno bazo smo se odločili za PostgreSQL. Ima veliko razširitev, kot je recimo PostGIS ter je tudi zelo učinkovita podatkovna baza za obdelavo velike količine podatkov. 

Za komunikacijo se najbol zanašamo na HTTP protokol, z bazo pa komuniciramo tudi seveda s SQL protokolom. Uporabljamo tudi spletne vtiče (web sockets), saj nam to omogoča realnočasovno podajanje podatkov odjemalcem. 

Za implementacijjo spletnega strežnika se zanašamo kar na paket `net/http`, ki je del standarne GO knjižnice. Storitev pa prevajamo z GO prevajalnikom

Glede dodatnij knjižnic se naša spletna storitev najbolj zanaša na GO knjižnice:

| Knjižnica       | Namen                          | Ključna funkcionalnost                          | Zakaj smo jo izbrali?                     |
|-----------------|-------------------------------|-----------------------------------------------|------------------------------------------|
| `net/http`      | HTTP strežnik in klient        | Osnovna komunikacija preko HTTP/HTTPS          | Standardna knjižnica Go, zanesljiva      |
| `go-chi`        | Routing za API                 | Modularno usmerjanje zahtev (`/api/v1/...`)    | Enostavna integracija, podpora middleware |
| `swaggo`        | Generiranje API dokumentacije  | Avtomatska dokumentacija (OpenAPI/Swagger)     | Sinhronizacija s kodo, minimalen vzdrževanje |
| `crypto`        | Šifriranje in varnost          | Šifriranje gesel, digitalni podpisi            | Vgrajena v Go, podpora TLS/SSL           |
| `pq`            | Dostop do PostgreSQL           | SQL poizvedbe in transakcije                   | Specializirana za PostgreSQL, stabilna   |


Za vse omenjene knjižnice smo se odločili, ker so del širše GO standardne knjižnice ter so tudi standardni in priporočeni načini reševanja omenjenih problemov v GO razvojnem okolju.

Glede komunikacije pa se še v trenutnem obdobju razvoja zanašamo na naslednja vrata:

| Port  | Protokol  | Komponenta         | Namen                          | Varnostni ukrepi                  |
|-------|-----------|--------------------|--------------------------------|----------------------------------|
| 8080  | HTTP      | Backend (Go)       | Glavni API dostop              |  CORS      |
| 8081  | HTTP      | Webhooks | Webhook klici       | Webhook Secret                   |
| 3000  | HTTP      | Frontend (React)   | Razvojni strežnik (dev mode)   | Samo za lokalni razvoj            |
| 5432  | SQL      | Podatkovna baza   | Lokalna podatkovna baza   | Samo za lokalni dostop            |


### Razredni diagam

```mermaid
classDiagram
    direction LR

    class users {
        +id: serial
        +username: character varying(50)
        +email: character varying(100)
        +password: character varying(100)
        +created_at: timestamp(0) with time zone
        +last_login: timestamp with time zone
        ---
        Create(context.Context, *User) error
		GetByEmail(context.Context, string) (*User, error)
		GetById(context.Context, int) (*User, error)
		UpdateById(context.Context, int, *UpdateUserPayload) error
		GetByIDForClient(context.Context, int) (*UserForClient, error)
    }
    
    class stops {
        +id: integer
        +number: character varying(10)
        +name: character varying(100)
        +latitude: numeric(9,6)
        +longitude: numeric(9,6)
        ---
        ReadStation(context.Context, int64) (*Stop, error)
		ReadList(context.Context) ([]Stop, error)
		ReadStationMetadata(context.Context, int64) (*StopMetadata, error)
		ReadStationsCloseBy(context.Context, *Location) ([]Stop, error)
		ReadThreeStationsAtDestination(context.Context, *PathLocation) ([]Stop, error)
		ReadStationLines(context.Context, []Stop) ([]Line, error)
		ReadThreeStationsAtLocation(context.Context, *PathLocation, []Line) ([]Stop, error)
    }

    class lines {
        +id: serial
        +line_code: character varying(10)
        ---
        Methods()
    }

    class directions {
        +id: serial
        +line_id: integer
        +name: text
        ---
        Methods()
    }

    class departures {
        +id: serial
        +stop_id: integer
        +direction_id: integer
        +date: date
        +line_id: integer
        ---
        Methods()
    }

    class routes {
        +id: serial
        +name: character varying(10)
        +path: jsonb
        +line_id: integer
        ---
        ReadRoute(context.Context, int64) (*Route, error)
		ReadRouteStations(context.Context, int64) ([]Stop, error)
		ReadRoutesList(context.Context) ([]Route, error)
		ReadActiveLines(context.Context) (int, error)
		FetchActiveRuns(context.Context, int) ([]ActiveRun, error)
    }

    class arrivals {
        +id: serial
        +departure_time: time[]
        +departures_id: integer
        ---
        Methods()
    }

    class delays {
        +id: serial
        +date: date
        +delay_min: integer
        +stop_id: integer
        +line_id: integer
        +user_id: integer
        ---
        GetDelaysByStop(context.Context, int64) ([]Delay, error)
		GetRecentDelaysByLine(context.Context, int64) ([]DelayEntry, error)
		GetDelaysByUser(context.Context, int64) ([]UserDelay, error)
		GetMostRecentDelays(context.Context) ([]MostRecentDelay, error)
		GetDelayCountsByLine(context.Context) ([]LineDelayCount, error)
		GetAverageDelayForLine(context.Context, int64) (*LineAverageDelay, error)
		GetOverallAverageDelay(context.Context) (float64, error)
		InsertDelay(context.Context, DelayReportInputUnMarshaled) error
    }

    class occupancy {
        +id: serial
        +line_id: integer
        +date: date
        +time: time
        +occupancy_level: integer
        ---
        GetOccupancyForLineByDate(context.Context, int, string) ([]OccupancyRecord, error)
		GetOccupancyForLineByDateAndHour(context.Context, int, string, int) ([]OccupancyRecord, error)
		GetAvgOccupancyAllLinesByHour(context.Context, int) (*AvgOccupancyByHour, error)
		GetAvgDailyOccupancyAllLines(context.Context, string) (*AvgDailyOccupancy, error)
    }

    users -- delays
    stops -- delays
    lines -- delays
    lines -- routes
    lines -- occupancy
    lines -- directions
    lines -- departures
    arrivals -- departures
    directions -- departures
    stops -- departures
```

## CI/CD

### Pregled CI/CD poteka
Naš CI/CD potek dela se začne z vsako potisnjeno spremembo na doočeno vejo našega GitHub repozitorija, posebej tisto, ki vpliva na mapo backend. Ta sprememba sproži serijo avtomatiziranih korakov, ki so združeni v "job-e" znotraj GitHub Actions. Ti koraki vključujejo izgradnjo Docker slike, njeno nalaganje na Docker Hub in obveščanje našega  strežnika o novi različici. Strežnik nato samodejno posodobi in zažene najnovejšo različico aplikacije.

```mermaid
graph TD
    A[Razvijalec potisne kodo na GitHub] --> B{Spremembe v 'backend' mapi na veji 'main'?};
    B -- Da --> C(GitHub Actions Workflow Sprožen);
    C --> D[Job: build_and_push_backend_image];
    D --> D1[Checkout koda];
    D1 --> D2[Prijava v Docker Hub];
    D2 --> D3[Zgraditev Docker slike za Backend];
    D3 --> D4[Nalaganje Docker slike na Docker Hub];
    D4 -- Uspeh --> E[Job: notify_server];
    E --> E1[Pošiljanje Webhook sporočila na Azure VM];
    E1 -- Webhook sprejet --> F[Azure VM - Webhook Listener];
    F -- Potrjen podpis --> G[Zagon deploy.sh skripte na VM];
    G --> G1[Prenos najnovejše Docker slike iz Docker Huba];
    G1 --> G2[Zaustavitev in odstranitev starega Docker kontejnerja];
    G2 --> G3[Zagon novega Docker kontejnerja];
    G3 --> H[Aplikacija posodobljena na Azure VM];
    D -- Neuspeh --> I(Neuspešen CI/CD Zagon);
    E -- Neuspeh --> I;
    G -- Neuspeh --> I;
```

### Povezava med GitHub Actions in Azure VM
Webhook omogoča komunikacijo med GitHub Actions in našim strežnikom. Ko GitHub Actions uspešno zgradi in naloži novo Docker sliko, pošlje HTTP POST zahtevo na določen URL na našem strežniku, s čimer ga obvesti o novi verziji aplikacije.

```mermaid
graph TD
    A[GitHub Actions 'notify_server' Job] --> B{Pošiljanje HTTP POST na Webhook URL};
    B --> C[Azure VM - Webhook Listener];
    C --> D{Preverjanje avtentičnosti sporočila};
    D -- Podpis veljaven --> E[Zagon 'deploy.sh' skripte];
    D -- Podpis neveljaven --> F[Zavrnitev sporočila];
    E --> G[Posodobitev aplikacije];
    G --> H[Odgovor HTTP 200 OK GitHub Actionsu];
```

## Varnost programske rešitve

Uporabljamo ufw (Uncomplicated Firewall). Trenutni izpis:
```bash 
sudo ufw status

Status: active

To                         Action      From
--                         ------      ----
OpenSSH                    ALLOW       Anywhere
8080/tcp                   ALLOW       Anywhere
8081                       ALLOW       Anywhere
8081/tcp                   ALLOW       Anywhere
OpenSSH (v6)               ALLOW       Anywhere (v6)
8080/tcp (v6)              ALLOW       Anywhere (v6)
8081 (v6)                  ALLOW       Anywhere (v6)
8081/tcp (v6)              ALLOW       Anywhere (v6)
```

OpenSSH se uporablja za administracijo preko SSH, 8080 in 8081 pa za dostop do spletne storitve ter Webhook.

Glede uporabnikov imamo 4 glavne skupine:
- `BitBanditi` - glavni uporabnik in admin skupina z vsemi pravicami
- `dockerAdmins` – za skrbnike Docker okolja
- `readonlyUsers` – dostop do branja
- `supportUsers` – dostop do logov in diagnostike, brez admin pravic


### Podrobnejši opis varnostnih vlog
Admin (`BitBanditi`):
- polni dostop do aplikacije, admin UI in baze.
- upravljajo konfiguracije, deploy, Docker, SSH.

Docker admin (`dockerAdmins`):
- lahko izvajajo Docker ukaze, pognati/ustaviti kontejnarje, dostop do logov, brez SSH ali produkcijskih admin pravic.

Support user (`supportUsers`): 
- dostop do diagnostike

Read‑only user (`readonlyUsers`):
- to so vsi ostali uporabniki, ki nimajo nastavljene druge skupine