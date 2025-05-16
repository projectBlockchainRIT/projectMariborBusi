class LexicalAnalyzer {
    private val table = initializeTable()
    private val symbols = listOf("int", "double", "quote", "lparen", "rparen", "comma", "semi", "lcurly", "rcurly", "variable", "variable", "variable", "variable", "city",
        "variable", "circ", "variable", "variable", "variable", "road", "variable", "variable", "variable", "line", "variable", "variable", "box", "variable", "variable",
        "bend", "variable", "variable", "variable", "variable", "variable", "variable", "building", "variable", "variable", "variable", "variable", "variable", "bus_line",
        "variable", "variable", "variable", "bus_stop", "assign", "color", "variable", "variable", "variable", "variable", "solid", "variable", "variable", "variable", "variable",
        "variable", "dashed", "variable", "variable", "variable", "variable", "dotted", "plus", "minus", "multiply", "divide", "variable", "variable", "variable", "variable", "variable",
        "variable", "distance", "variable", "variable", "variable", "variable", "variable", "variable", "variable", "midpoint", "variable", "if", "variable", "variable", "variable", "else",
        "variable", "variable", "variable", "variable", "import", "variable", "variable", "for")
    private val terminalSymbols = setOf('"', ';', ',', '(', ')', '{', '}', '=')

    private fun initializeTable(): Array<IntArray> {
        val table = Array(256) { IntArray(99) { 100 } }

        //int
        for (i in 48..57) {
            table[i][0] = 1
            table[i][1] = 1
            table[i][2] = 2
            table[i][10] = 10
            table[i][49] = 49
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

        table[35][0] = 49 //#


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
            for (j in 10..65) {
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

                if (state == 100) {
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
                state == 100 -> {
                    processLexemeIfExists(tokens, lexeme, prevState)
                    if (lexeme.isEmpty()) {
                        throw IllegalArgumentException("Invalid token: $char")
                    }
                }

                // Final states
                state >= 99 -> {
                    processLexemeIfExists(tokens, lexeme, prevState)

                    if (state == 100) {
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