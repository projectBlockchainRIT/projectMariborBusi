# projectMariborBusi

## Člani skupine

`Ime skupine:`
BitBanditi

`Ime projekta:`
M-Busi

`Adrian Cvetko`
Št. ID: 1002623871
Kontakt: adrian.cvetko@student.um.si

`Blaž Kolman`
Št. ID: 1002614686
Kontakt: blaz.kolman1@student.um.si

`Timotej Maučec`
Št. ID: 1002610222
Kontakt: timotej.maucec@student.um.si

## 1. Opis projekta

Cilj projekta je izgradnja digitalnega dvojčka mestnih avtobusov, ki bo omogočal spremljanje prihodov na postaje, prikaz trenutne ali ocenjene lokacije avtobusov in simulacijo njihovega gibanja po mestu. Digitalni dvojček bo uporabljen za:

- **Sprotno spremljanje avtobusov**
- **Napovedovanje prihodov na postaje** (z uporabo ML, če bo izvedljivo)
- **Analizo učinkovitosti prometa** (možna razširitev)
- **Vizualizacijo linij in postaj na zemljevidu s pomočjo domensko specifičnega jezika (DSL)**

### Dodatne funkcionalnosti / uporabe

- **Pregled nad avtobusnim prometom** 
  Pogled nad vsemi avtobusnimi linijami na interaktivnem zemljevidu, da lahko hitro pregledamo stanje javnega prometa po mestu

- **Fokusiranje za posamezno linijo** 
  Izbira posameznih linih, da lahko spremljamo samo tiste avtobuse, ki nas zanimajo.

- **Prikaz avtobusnih postaj** 
  Oznaka avtobusnih postaj na zemljevidu, da vemo, kje lahko vstopimo ali izstopimo.

- **Omogočanje vnosa zamud s strani uporabnika**
  Omogočanje uporabnikom ročno vnašanje zamud avtobusov, da sistem upošteva dejansko stanje in pososobi projektice prihodov.

- **Prikaz nesreč na zemljevidu**
  Označitev prometnih nesreč na zemljevidu, da uporabnik razume vzroke zamude

- **Razdelitev uporabnikov po vlogah**
  Razdeliev uporabnikov po vlogah (gledalec, upravitelj, razvijalec), da omejimo privilegije vnosa in brisanja podatkov

- **Simulacija različnih scenarijev**  
  Omogoča testiranje odziva sistema v primeru prometnih konic, zapor cest, izpadov vozil ali drugih izrednih razmer.

- **Statistična analiza zgodovinskih podatkov**  
  Analiza zamud, pogostosti prihodov, učinkovitosti linij v različnih delih dneva ali tedna.

- **Obveščanje uporabnikov**  
  Simulacija push notifikacij (npr. “Avtobus 6 bo zamujal 4 min”) ali drugih obvestil.

- **Prikaz zasedenosti avtobusov**  
  Vizualni prikaz (dejanskih ali simuliranih) podatkov o zasedenosti avtobusov, kar pomaga pri načrtovanju poti.

- **Spremljanje ogljičnega odtisa**  
  Ocena prihranka CO₂ ob uporabi javnega prevoza v primerjavi z osebnimi vozili – uporabno za trajnostne analize. 

- **Upravljanje dogodkov/izjem**  
  Vnos izrednih dogodkov (npr. izpad avtobusa, zapora postaje) in simulacija posledic na promet.

- **Sistem za administratorje**  
  Interni uporabniški vmesnik za urejanje voznih redov, ročno simulacijo dogodkov, nadzor statusa avtobusov itd.

---

## 2. Nabor podatkov

Za potrebe digitalnega dvojčka bomo obdelovali naslednje vrste podatkov:

| Tip podatkov                | Opis                                                                               | kje?                          |
| --------------------------- | ---------------------------------------------------------------------------------- | ----------------------------- |
| GPS lokacija avtobusov      | Trenutna ali ocenjena lokacija vozil (vezana na geografsko lokacijo)               | Simulacija lokacija z ML      |
| Čas prihoda na postajo      | Načrtovan in dejanski čas prihoda                                                  | Strgalnik za Marprom          |
| Vozni redi                  | Statični podatki o progah in urnikih                                               | Strgalnik za Marprom          |
| Podatki o postajah          | Imena, ID-ji, geografske koordinate                                                | GitHub GeoJSON datoteka       |
| (Dodatno) Podatki o prometu | Če bi želeli vključiti napovedovanje zamud                                         | Strgalnik za Promet.si        |
| Zasedenost avtobusa         | Pokazali zasedenost avtobusa z večimi stopnjami (predvidevali glede na čas in dan) | Google Maps API               |

---

## 3. Pridobivanje podatkov

- **Uporaba API-jev**, če obstajajo za Mariborski mestni promet (npr. GTFS real-time ali odprti podatki)
- **Web scraping** podatkov z uradne spletne strani mestnega prometa Maribor
- **Simuliranje podatkov** v primeru odsotnosti odprtih podatkov (recimo simuliranje trenutne lokacije avtobusa glede na čase odhoda)

---

## 4. Načrtovane funkcionalnosti

### Spletna storitev

- Vnos in dostop do podatkov (REST API)
- Shranjevanje v podatkovno bazo
- Endpointi za podatke o avtobusih, postajah, linijah, napovedih

### Uporabniški vmesnik

- Interaktivni zemljevid z vizualizacijo linij, postaj in avtobusov
- Pregled napovedi prihodov na posamezne postaje
- Možnost simulacije ("kaj če" scenariji)

### Domensko specifični jezik (DSL)

- DSL za opisovanje linij in njihove logike (proga, vrstni red postaj itd.)
- Vizualizacija definiranih linij preko DSL
- mogoče vizualizacija celotnega mesta (, but i am no sure)

### (Opcijsko) Napovedovanje prihodov

- Preprost ML model za oceno časa prihodov glede na zgodovinske podatke
- Vizualizacija odstopanj od načrta

---

## 5. Tech stack (predlogi)

| Sloj               | Predlogi                                                |
| ------------------ | ------------------------------------------------------- |
| Frontend           | next.js + Google Maps API za zemljevid                  |
| Backend            | Node.js                                                 |
| Baza podatkov      | PostgreSQL                                              |
| DSL interpretacija | ?                                                       |
| Strojno učenje     | Scikit-learn / TensorFlow (če gremo v to smer)          |

---

## 6. Razdelitev dela v mikroskupini

- Zbiranje podatkov: scraping / API-ji, ML
- DSL: definicija jezika, parser, vizualizacija
- Frontend: uporabniški vmesnik, zemljevid
- Backend: API-ji, baza, model podatkov

---

## 7. Interni roki

| Faza                           | Rok               |
| ------------------------------ | ----------------- |
| Načrt podatkov in arhitekture  | do 27.4           |
| Zbiranje / simulacija podatkov | do [vstavi datum] |
| Backend API in baza            | do [vstavi datum] |
| Frontend + zemljevid           | do [vstavi datum] |
| DSL in integracija             | do [vstavi datum] |
| Testiranje in zaključek        | do [vstavi datum] |

---

## 8. začetek

- zbiranje API storitev in zasnova potrebnih spletnih strgalnikov
- vzpostavitev podatkovne baze
- začetek zasnove DSL
- začetek čelnega in zalednega dela
- povezava spletne aplikacije s podatkovno bazo

---