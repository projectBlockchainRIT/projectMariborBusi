class LexicalAnalyzer {
    private val table = initializeTable()
    private val symbols = listOf("int", "double", "quote", "lparen", "rparen", "comma", "semi", "rcurly", "lcurly", "variable", "variable", "variable", "variable", "city",
        "variable", "circ", "variable", "variable", "variable", "road", "variable", "variable", "variable", "line", "variable", "variable", "box", "variable", "variable",
        "bend", "variable", "variable", "variable", "variable", "variable", "variable", "building", "variable", "variable", "variable", "variable", "variable", "bus_line",
        "variable", "variable", "variable", "bus_stop", "assign")
    private val terminalSymbols = setOf('"', ';', ',', '(', ')', '{', '}', '=')

    private fun initializeTable(): Array<IntArray> {
        val table = Array(256) { IntArray(48) { 49 } }

        //int
        for (i in 48..57) {
            table[i][0] = 1
            table[i][1] = 1
            table[i][2] = 2
            table[i][10] = 10
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

        //a-z
        for (i in 97..122) {
            table[i][0] = 10
            table[i][10] = 10
            table[i][11] = 10
            table[i][12] = 10
            table[i][13] = 10
            table[i][14] = 10
            table[i][15] = 10
            table[i][16] = 10
            table[i][17] = 10
            table[i][18] = 10
            table[i][19] = 10
            table[i][20] = 10
            table[i][21] = 10
            table[i][22] = 10
            table[i][23] = 10
            table[i][24] = 10
            table[i][25] = 10
            table[i][26] = 10
            table[i][27] = 10
            table[i][28] = 10
            table[i][29] = 10
            table[i][30] = 10
            table[i][31] = 10
            table[i][32] = 10
            table[i][33] = 10
            table[i][34] = 10
            table[i][35] = 10
            table[i][36] = 10
            table[i][37] = 10
            table[i][38] = 10
            table[i][39] = 10
            table[i][40] = 10
            table[i][41] = 10
            table[i][42] = 10
            table[i][43] = 10
            table[i][44] = 10
            table[i][45] = 10
            table[i][46] = 10
            table[i][47] = 10
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

                if (state == 49) {
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
                state == 49 -> {
                    processLexemeIfExists(tokens, lexeme, prevState)
                    if (lexeme.isEmpty()) {
                        throw IllegalArgumentException("Invalid token: $char")
                    }
                }

                // Final states
                state >= 48 -> {
                    processLexemeIfExists(tokens, lexeme, prevState)

                    if (state == 49) {
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
            tokens.add("${symbols[prevState - 1]}(\"$lexeme\")")
            lexeme.clear()
        }
    }
}