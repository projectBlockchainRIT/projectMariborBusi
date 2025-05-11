# Načrt za domensko-specifični jezik za opis mestne infrastrukture

## 1. Osnovni konstrukti

### `nil`
```plaintext
nil
```
nevtralni element, ne opravi nobene operacije

### `realna števila`
```
Primeri: 2, 1, 1.2
```
števila s katerimi definiramo koordinate, kote, dolžine ipd.

### `Nizi`
```
"NIZ"
```
Dvojni narekovaji označujejo besedilni niz (npr. ime mesta, zgradbe, postaje...).

### `Koordinate`
```
(X, Y)
```
Predstavlja točko na zemljevidu, kjer je `X` longituda in `Y` latituda.

### `Struktni bloki`
#### `city`
```
city "IME_MESTA" {
  BLOKI
}
```
Glavni blok, ki definira celotno mesto. Vsebuje ceste, zgradbe, postaje in avtobusne linije.

#### `road`
```
road "IME_CESTE" {
  UKAZI
}
```
Blok za opis ceste. Vsebuje ukaze za izris črt in krivulj.

#### `building`
```
building "IME_ZGRADBE" {
  UKAZI
}
```
Blok za opis zgradbe. Vsebuje ukaze za izris oblike (mora biti zaprta oblika).

#### `station`
```
station "IME_POSTAJE" {
  location(POINT)
}
```
Določa eno avtobusno postajo na točno določeni lokaciji.

#### `busline`
```
busline "IME_LINIJE" {
  route(POINT1, POINT2, ..., POINTn)
}
```
Definira avtobusno linijo, ki povezuje več točk (npr. postaje).

### `ukazi za izris`
#### `line`
```
line(POINT1, POINT2)
```
Nariše ravno črto med dvema točkama.

#### `bend`
```
bend(POINT1, POINT2, ANGLE)
```
Nariše krivuljo med točkama. Kot določa ukrivljenost:

  -  0° = ravna črta

  - 45° = četrtina kroga

  - pozitivni koti = levo, negativni = desn

#### `box`
```
box(POINT1, POINT2)
```
Nariše pravokotnik. Prva točka je zgornji levi kot, druga spodnji desni.

#### `circ`
```
circ(POINT, RADIUS)
```
Nariše krog s podanim polmerom okoli izbrane točke.

## 2. Gramatika:

```
<program> ::= <city> | <program> <city>

<city> ::= 'city' <string> '{' <city_body> '}'
<city_body> ::= <element> | <citi_body> <element>
<element> ::= <road> | <building> | <bus_stop> | <bus_line>

<road> ::= 'road' <string> '{' <command>* '}'
<building> ::= 'building' <string> '{' <command> '}'
<bus_stop> ::= 'bus stop' <string> int '{' <command> '}'
<bust_line> ::= 'bus line' <string> int '{' <command> '}'

<command> ::= <line_cmd> <command> | <bend_cmd> <command> | <box_cmd> <command> | <circ_cmd> <command> | ε
<line_cmd> ::= 'line' '(' <point> ',' <point> ')'';'
<bend_cmd> ::= 'bend' '(' <point> ',' <point> ',' <number> ')'';'
<box_cmd> ::= 'box' '(' <point> ',' <point> ')'';'
<circ_cmd> ::= 'circ' '(' <point> ',' <number> ')'';'

<point> ::= '(' <number> ',' <number> ')'
<string> ::= '"' <char>* '"'
<number> ::= int | int . int
```

## 3. Primeri:

1. Mesto z eno cesto in eno zgradbo
```city "TestCity" {
  road "Main Street" {
    line((0, 0), (10, 0));
  }
  building "Library" {
    box((2, 2), (4, 3));
  }
}
```

2. Cesta z več ukazi (line in bend)
```
city "CurvyTown" {
  road "Snake Road" {
    line((0, 0), (2, 2));
    bend((2, 2), (4, 1), 30);
  }
}
```

3. Krožni promet
```
city "circle road" {
  road "Circular line" {
    circ((3, 3), 1.5);
  }
}
```

4. Avtobusna postaja z lokacijo
```
city "BusStop" {
  bus stop "Main Station" 1 {
    circ((1, 1), 0.3);
  }
}
```

5. Avtobusna linija z ukrivljenim potekom
```
city "MetroCity" {
  bus line "Line A" 10 {
    line((0, 0), (2, 0));
    bend((2, 0), (3, 2), 45);
  }
}
```
6. Kombinacija vseh entitet
```
city "ComplexCity" {
  road "First Road" {
    line((0, 0), (1, 1));
  }
  building "Office" {
    box((1, 1), (2, 2));
  }
  bus stop "Central" 2 {
    circ((2, 2), 0.4);
  }
  bus line "B1" 5 {
    line((2, 2), (3, 3));
  }
}
```

7. Več cest in zgradb
```
city "UrbanArea" {
  road "North Road" {
    line((0, 0), (0, 5));
  }
  road "East Road" {
    line((0, 5), (5, 5));
  }
  building "Mall" {
    box((2, 2), (4, 4));
  }
  building "Warehouse" {
    box((5, 1), (6, 3));
  }
}
```

8. Zgradba z več line ukazi (npr. trikotnik)
```
city "PolygonTown" {
  building "Triangle Building" {
    line((0, 0), (1, 2));
    line((1, 2), (2, 0));
    line((2, 0), (0, 0));
  }
}
```
9. Bus stop znotraj zgradbe
```
city "IntegrationCity" {
  building "Station Complex" {
    box((0, 0), (3, 3));
  }
  bus stop "Inside Stop" 3 {
    circ((1.5, 1.5), 0.3);
  }
}
```

10. Več avtobusnih linij
```
city "TransportHub" {
  bus line "Red Line" 1 {
    line((0, 0), (5, 0));
  }
  bus line "Green Line" 2 {
    bend((5, 0), (6, 2), 20);
    line((6, 2), (7, 2));
  }
}
```