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
Glavni blok, ki definira celotno mestođ
Vsebuje ceste, zgradbe, postaje in avtobusne linije.

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

### `style`

#### `color`
```
#RRGGBB 
```
Lahko zapišemo barvo v HEX obliki (npr. #FF0000)

#### `stil črte`
```
solid   // neprekinjena črta
dashed  // črtkana črta
dotted  // pikčasta črta
```
Nastavi stil črte za vse ukaze, ki sledijo.

### `izrazi in spremenljivke`

#### `spremenljivke`
```
x = 5
```
Določimo spremenljivko `x` in ji dodelimo vrednost 5.

#### `izrazi`
```
x + y
```
Osnovni matematični izrazi, ki jih lahko uporabimo v ukazih.

#### `funkcije`
```
distance(point1, point2)  // izračuna razdaljo med točkama
midpoint(point1, point2)  // izračuna središčno točko
```
različne funkcije, ki jih lahko uporabimo v ukazih. Izračunajo različne vrednosti, ki jih lahko uporabimo v ukazih.

### `kontrolne strukture` 

#### `if`
```
if (pogoj) {
  // ukazi, če je pogoj izpolnjen
} else {
  // opcijski ukazi, če pogoj ni izpolnjen
}
```
pogojna struktura, ki omogoča izvajanje ukazov glede na izpolnjenost pogoja.

#### `for`
```
for (i = 0; i < 10; i++) {
  // ukazi, ki se ponavljajo
}
```
Zanka, ki ponavlja ukaze določenokrat. `i` se povečuje za 1 pri vsakem ponovnem izvajanju.

### `modularnost`

#### `include`
```
include "ime_datoteke"
```
Omogoča vključitev druge datoteke v trenutni program. Uporabno za ponovno uporabo kode in modularnost.


## 2. Gramatika:

```
<program> ::= <stmt>* 

<stmt> ::= <city>
        | <include_stmt>
        | <assignment>
        | <if_stmt>
        | <for_stmt>

<include_stmt> ::= 'import' <string> ';'

<city> ::= 'city' <string> '(' <style> ')' '{' <city_body> '}'
<city_body> ::= <element>* 

<element> ::= <road>
           | <building>
           | <station>
           | <busline>
           | <if_stmt>
           | <for_stmt>

<road> ::= 'road' <string> '(' <style> ')' '{' <command>* '}'
<building> ::= 'building' <string> '(' <style> ')' '{' <command>* '}'
<station> ::= 'bus_stop' <string> '(' <style> ')' '{' 'location' '(' <point> ')' ';' '}'
<busline> ::= 'busline' <string> '(' <style> ')' '{' <command>* '}'

<style> ::= <color> | <line_style>
<color> ::= '#' <hex_color>
<line_style> ::= 'solid' | 'dashed' | 'dotted'

<command> ::= <draw_cmd> 
           | <assignment>
           | <function_call>
           | <if_stmt>
           | <for_stmt>
           | ε

<draw_cmd> ::= <line_cmd>
             | <bend_cmd>
             | <box_cmd>
             | <circ_cmd>

<line_cmd> ::= 'line' '(' <point> ',' <point> ')' ';'
<bend_cmd> ::= 'bend' '(' <point> ',' <point> ',' <number> ')' ';'
<box_cmd> ::= 'box' '(' <point> ',' <point> ')' ';'
<circ_cmd> ::= 'circ' '(' <point> ',' <number> ')' ';'

<assignment> ::= <identifier> '=' <expression> ';'

<expression> ::= <number>
              | <identifier>
              | <expression> <op> <expression>
              | <function_call>
              | '(' <expression> ')'

<op> ::= '+' | '-' | '*' | '/'

<function_call> ::= <func_name> '(' <arg_list>? ')' ';'
<func_name> ::= 'distance' | 'midpoint'

<arg_list> ::= <point> ',' <arg_list>
           | <point>

<if_stmt> ::= 'if' '(' <expression> ')' '{' <command>* '}' ('else' '{' <command>* '}')?

<for_stmt> ::= 'for' '(' <assignment> 'to' <assignment> ')' '{' <command>* '}'

<point> ::= '(' <number> ',' <number> ')'

<primary> ::= <number>
          | <string>
          | <identifier>
<string> ::= '"' <char>* '"'
<identifier> ::= [a-zA-Z_][a-zA-Z0-9_]*
<number> ::= int | int '.' int
```

## 3. Primeri:

1. Mesto z eno cesto in eno zgradbo
```city "TestCity" {
city "MiniMesto" {
  road "Glavna" {
    line((0,0), (10,0));
  }

  building "Trgovina" {
    box((2,2), (4,4));
  }
}
```

2. Mesto z več postajami in avtobusno linijo
```
city "BusMesto" {
  bus_stop "Center" {
    location((5,5));
  }

  bus_stop "Postaja2" {
    location((10,10));
  }

  bus_line "Linija1" {
    line((0,0), (5,5));
    bend((5,5), (10,10), 45);
  }
}
```

3. Mesto z krivuljo
```
city "KrivuljaCity" {
  building "Muzej" {
    bend((1,1), (4,4), 45);
    bend((4,4), (1,1), -45);
  }
}
```

4. Izris parka s uporabo sredine
```
x = midpoint((2,2), (6,6));

city "SredinskiPark" {
  building "Park" {
    circ(x, 2);
  }
}
```

5. Pogojna izris
```
a = 3;

if (a > 2) {
  city "LogikaCity" {
    road "Kratka" {
      line((0,0), (1,1));
    }
  }
} else {
  city "RezervniCity" {
    road "Rezervna" {
      line((0,0), (5,5));
    }
  }
}
```

6. Uporaba zanke za gradnjo več zgradb
```
city "KrogCity" {
  for (i = 0; to 5) {
    building "Krog" {
      circ((i * 3, 0), 1);
    }
  }
}
```

7. Uporaba zanke za izračun razdalje in shranje v spremenljivko
```
d = distance((1,1), (4,5));

city "RazdaljaCity" {
  building "InfoTocka" {
    circ((2,2), d);
  }
}
```

8. Uporaba več stilov črt
```
city "StilCity" (
 #FF0000
 solid
 ){
  road "Moderna" {
    line((0,0), (5,0));
    bend((5,0), (5,5), 90);
    box((6,6), (8,8));
  }
}
```
9. Vključitev zunanje datoteke
```
import "zunanje_stavbe.dsl";

city "ModularCity" {
  road "Glavna" {
    line((0,0), (5,5));
  }
}
```

10. Dinamična izbira zgradbe glede na razdaljo
```
p1 = (0, 0);
p2 = (4, 3);
d = distance(p1, p2);

if (d > 5) {
  city "PogojCity" {
    building "VelikaZgradba" {
      box(p1, p2);
    }
  }
} else {
  city "PogojCity" {
    building "MajhnaZgradba" {
      circ(midpoint(p1, p2), 1);
    }
  }
}
```