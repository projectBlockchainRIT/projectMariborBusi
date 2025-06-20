class LexicalAnalyzer {
    private val table = initializeTable()
    private val symbols = listOf("int", "double", "quote", "lparen", "rparen", "comma", "semi", "lcurly", "rcurly", "variable", "variable", "variable", "variable", "city",
        "variable", "circ", "variable", "variable", "variable", "road", "variable", "variable", "variable", "line", "variable", "variable", "box", "variable", "variable",
        "bend", "variable", "variable", "variable", "variable", "variable", "variable", "building", "variable", "variable", "variable", "variable", "variable", "bus_line",
        "variable", "variable", "variable", "bus_stop", "assign", "color", "variable", "variable", "variable", "variable", "solid", "variable", "variable", "variable", "variable",
        "variable", "dashed", "variable", "variable", "variable", "variable", "dotted", "plus", "minus", "multiply", "divide", "variable", "variable", "variable", "variable", "variable",
        "variable", "distance", "variable", "variable", "variable", "variable", "variable", "variable", "variable", "midpoint", "variable", "if", "variable", "variable", "variable", "else",
        "variable", "variable", "variable", "variable", "import", "variable", "variable", "for", "variable", "variable", "variable", "variable", "variable", "variable", "location", "variable",
        "to", "less", "greater", "assign", "equal", "variable", "not_equal", "variable", "variable", "variable", "variable", "variable", "variable", "nearest", "variable", "variable", "variable",
        "variable", "variable", "radius", "variable", "variable", "variable", "variable", "linije", "variable", "variable", "variable", "variable", "variable", "klopca", "variable", "variable",
        "variable", "variable", "variable", "variable", "variable", "variable", "variable", "nadstresek", "colon", "lbracket", "rbracket")
    private val terminalSymbols = setOf('"', ';', ',', '(', ')', '{', '}', ':', '[', ']')

    private fun initializeTable(): Array<IntArray> {
        val table = Array(256) { IntArray(200) { 201 } }

        //int
        for (i in 48..57) {
            table[i][0] = 1
            table[i][1] = 1
            table[i][2] = 2
            table[i][10] = 10
            table[i][49] = 49
            table[i][67] = 1
        }



        table[46][1] = 2 //.

        table[34][0] = 3 //"
        table[40][0] = 4 //(
        table[41][0] = 5 //)
        table[44][0] = 6 //,
        table[59][0] = 7 //;
        table[123][0] = 8 //{
        table[125][0] = 9 //}
        table[61][0] = 48 //=
        table[43][0] = 66 // +
        table[45][0] = 67 // -
        table[42][0] = 68 // *
        table[47][0] = 69 // /
        table[47][10] = 10 // /

        // < >
        table[60][0] = 108 // <
        table[62][0] = 109 // >

        // =
        table[61][0] = 110 // =
        table[61][110] = 111 // =

        // !=
        table[33][0] = 112 // !
        table[61][112] = 113 // =

        table[35][0] = 49 //#

        // .
        table[46][10] = 10 //.


        //A-Z
        for (i in 65..90) {
            table[i][0] = 10
            table[i][10] = 10
        }
        table[95][0] = 10 //_
        table[95][10] = 10
        for (i in 10..47) {
            table[95][i] = 10
        }
        for (i in 65..70) {
            table[i][49] = 49
        }

        //a-z
        for (i in 97..122) {
            table[i][0] = 10
            for (j in 10..115) {
                table[i][j] = 10
            }
        }

        //city
        table[99][0] = 11 //c
        table[105][11] = 12 //i
        table[116][12] = 13 //t
        table[121][13] = 14 //y

        //circ
        table[114][12] = 15 //r
        table[99][15] = 16 //c

        //road
        table[114][0] = 17 //r
        table[111][17] = 18 //o
        table[97][18] = 19 //a
        table[100][19] = 20 //d

        //line
        table[108][0] = 21 //l
        table[105][21] = 22 //i
        table[110][22] = 23 //n
        table[101][23] = 24 //e

        //box
        table[98][0] = 25 //b
        table[111][25] = 26 //o
        table[120][26] = 27 //x

        //bend
        table[101][25] = 28 //e
        table[110][28] = 29 //n
        table[100][29] = 30 //d

        //building
        table[117][25] = 31 //u
        table[105][31] = 32 //i
        table[108][32] = 33 //l
        table[100][33] = 34 //d
        table[105][34] = 35 //i
        table[110][35] = 36 //n
        table[103][36] = 37 //g

        //bus_line
        table[115][31] = 38 //s
        table[95][38] = 39 //_
        table[108][39] = 40 //l
        table[105][40] = 41 //i
        table[110][41] = 42 //n
        table[101][42] = 43 //e

        //bus_stop
        table[115][39] = 44 //s
        table[116][44] = 45 //t
        table[111][45] = 46 //o
        table[112][46] = 47 //p

        // style_line
        // solid, dashed, or dotted
        table[115][0] = 50 // s
        table[111][50] = 51 // o
        table[108][51] = 52 // l
        table[105][52] = 53 // i
        table[100][53] = 54 // d

        table[100][0] = 55 // d
        table[97][55] = 56 // a
        table[115][56] = 57 // s
        table[104][57] = 58 // h
        table[101][58] = 59 // e
        table[100][59] = 60 // d

        table[111][55] = 61 // o
        table[116][61] = 62 // t
        table[116][62] = 63 // t
        table[101][63] = 64 // e
        table[100][64] = 65 // d

        // functions
        table[105][55] = 70 // i
        table[115][70] = 71 // s
        table[116][71] = 72 // t
        table[97][72] = 73 // a
        table[110][73] = 74 // n
        table[99][74] = 75 // c
        table[101][75] = 76 // e

        table[109][0] = 77 // m
        table[105][77] = 78 // i
        table[100][78] = 79 // d
        table[112][79] = 80 // p
        table[111][80] = 81 // o
        table[105][81] = 82 // i
        table[110][82] = 83 // n
        table[116][83] = 84 // t

        //if
        table[105][0] = 85 // i
        table[102][85] = 86 // f

        // else
        table[101][0] = 87 // e
        table[108][87] = 88 // l
        table[115][88] = 89 // s
        table[101][89] = 90 // e

        // import
        table[109][85] = 91 // m
        table[112][91] = 92 // p
        table[111][92] = 93 // o
        table[114][93] = 94 // r
        table[116][94] = 95 // t

        // for
        table[102][0] = 96 // f
        table[111][96] = 97 // o
        table[114][97] = 98 // r

        // location
        table[111][21] = 99 // o
        table[99][99] = 100 // c
        table[97][100] = 101 // a
        table[116][101] = 102 // t
        table[105][102] = 103 // i
        table[111][103] = 104 // o
        table[110][104] = 105 // n

        //to
        table[116][0] = 106 // t
        table[111][106] = 107 // o

        // nearest
        table[110][0] = 114 // n
        table[101][114] = 115 // e
        table[97][115] = 116 // a
        table[114][116] = 117 // r
        table[101][117] = 118 // e
        table[115][118] = 119 // s
        table[116][119] = 120 // t

        // radius
        table[97][17] = 122 // a
        table[100][122] = 123 // d
        table[105][123] = 124 // i
        table[117][124] = 125 // u
        table[115][125] = 126 // s

        //linije
        table[105][23] = 129 // i
        table[106][129] = 130 // j
        table[101][130] = 131 // e

        // klopca
        table[107][0] = 132 // k
        table[108][132] = 133 // l
        table[111][133] = 134 // o
        table[112][134] = 135 // p
        table[99][135] = 136 // c
        table[97][136] = 137 // a

        //nadstresek
        table[97][114] = 139 // a
        table[100][139] = 140 // d
        table[115][140] = 141 // s
        table[116][141] = 142 // t
        table[114][142] = 143 // r
        table[101][143] = 144 // e
        table[115][144] = 145 // s
        table[101][145] = 146 // e
        table[107][146] = 147 // k

        // :
        table[58][0] = 148 // :

        // [
        table[91][0] = 149 // [
        // ]
        table[93][0] = 150 // ]


        return table
    }

    fun getTokens(inputText: String): List<String> {
        var state = 0
        var prevState = 0
        var lexeme = StringBuilder()
        val tokens = mutableListOf<String>()
        var i = 0

        while (i < inputText.length) {
            val char = inputText[i]

            if (char in terminalSymbols) {
                processLexemeIfExists(tokens, lexeme, prevState)
                state = 0
                tokens.add("${symbols[table[char.code][state] - 1]}(\"$char\")")

                i++
                state = 0
                continue
            }

            // Handle comments
            if (isCommentStart(char, inputText, i)) {
                i = skipToEndOfComment(inputText, i)
                continue
            }

            // Handle whitespace
            if (char.isWhitespace()) {
                processLexemeIfExists(tokens, lexeme, prevState)

                if (state == 200) {
                    lexeme.append(char)
                    state = table[char.code][0]
                    prevState = state
                } else {
                    state = 0
                }

                i++
                continue
            }

            // Process character based on state transition
            state = table[char.code][state]

            when {
                // Error state
                state == 201 -> {
                    processLexemeIfExists(tokens, lexeme, prevState)
                    if (lexeme.isEmpty()) {
                        throw IllegalArgumentException("Invalid token: $char")
                    }
                }

                // Final states
                state >= 200 -> {
                    processLexemeIfExists(tokens, lexeme, prevState)

                    if (state == 201) {
                        lexeme.append(char)
                        state = table[char.code][0]
                        prevState = state
                    } else {
                        state = 0
                    }
                }

                // Intermediate states
                else -> {
                    lexeme.append(char)
                    prevState = state
                }
            }

            i++
        }

        // Handle any remaining lexeme at the end
        processLexemeIfExists(tokens, lexeme, prevState)

        return tokens
    }

    // Helper functions to improve readability
    private fun isCommentStart(char: Char, text: String, index: Int): Boolean {
        return char == '/' && index + 1 < text.length && text[index + 1] == '/'
    }

    private fun skipToEndOfComment(text: String, startIndex: Int): Int {
        var i = startIndex
        while (i < text.length && text[i] != '\n') {
            i++
        }
        return i
    }

    private fun processLexemeIfExists(tokens: MutableList<String>, lexeme: StringBuilder, prevState: Int) {
        if (lexeme.isNotEmpty()) {
            if (prevState == 49 && lexeme.length == 7) {
                tokens.add("${symbols[prevState - 1]}(\"$lexeme\")")
                lexeme.clear()
            } else if (prevState == 49 && lexeme.length != 7) {
                throw IllegalArgumentException("Invalid token: $lexeme")
            } else {
                tokens.add("${symbols[prevState - 1]}(\"$lexeme\")")
                lexeme.clear()
            }
        }
    }
}