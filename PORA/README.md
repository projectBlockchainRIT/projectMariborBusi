
## M-Busi mobilna aplikacija

### Glavne funkcionalnosti aplikacije
    - zajemanje podatkov iz različnih senzorjev (kamera, mikrofon, geosensor...)
    - objavljanje podatkov na strežnik
    - za vsak senzor se lahko določi frekvenco zajemanja podatkov
    - za objavo podatkov na strežnik se uporablja knjižnica MQTT (to se da tudi v flutterju, mqtt je bolj protokol)
    - vmesnik za objavljanje poljubnih sporočil (npr. prometna nesreča)
    - dogodke shranjujemo idealno v verigo blokov, lahko tudi v bazo
    - aplikacija mora tudi sama zaznavati ekstremne dogodke in jih dodaja v verigo
    - aplikacija mora podpirati tudi simulirano delovanje (enquire for further information pri asistentu)
    - protokol MQTT
      - lahek protokol, zasnovan za naprave z omejenimi viri in slaba omrežja
      - v mobilni aplikaciji moramo implementirati ustreznega odjemalca (whatever that means)
        - za vzpostavljanje povezav in objavljanje podatkov
      - na strežniku bo potrebno narediti MQTT posrednika



### Prvi koraki
    - določitev, katere senzorje bo aplikacija uporabljala
    - vzpostavitev razvojnega okolja
    - določitev frameworka, upam, da bo lahko flutter



### Zaključek projektnega dela
    - priprava videa
    - priprava predstavitve