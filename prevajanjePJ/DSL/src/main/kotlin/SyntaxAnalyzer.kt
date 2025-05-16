class SyntaxAnalyzer {
    private var tokens = listOf<String>()
    private var currentTokenIndex = 0

    fun analyze(tokenList: List<String>): Boolean {
        tokens = tokenList
        currentTokenIndex = 0

        try {
            program()

            // Check if we consumed all tokens
            if (currentTokenIndex < tokens.size) {
                println("reject")
                return false
            }

            println("accept")
            return true
        } catch (e: Exception) {
            println("reject")
            println("Syntax error: ${e.message}")
            return false
        }
    }

    // <program> ::= <city> | <program> <city>
    private fun program() {
        city()

        // Try to parse more cities if available
        while (currentTokenIndex < tokens.size && peekToken().startsWith("city")) {
            city()
        }
    }

    // <city> ::= 'city' <string> '{' <city_body> '}'
    private fun city() {
        match("city")
        stringLiteral()
        match("lcurly")
        cityBody()
        match("rcurly")
    }

    // <city_body> ::= <element> | <city_body> <element>
    private fun cityBody() {
        // Check if there are elements to parse
        if (isElementStart(peekToken())) {
            element()

            // Parse more elements if available
            while (isElementStart(peekToken())) {
                element()
            }
        }
    }

    // <element> ::= <road> | <building> | <bus_stop> | <bus_line>
    private fun element() {
        when {
            peekToken().startsWith("road") -> road()
            peekToken().startsWith("building") -> building()
            peekToken().startsWith("bus_stop") -> busStop()
            peekToken().startsWith("bus_line") -> busLine()
            else -> throw Exception("Expected element but found ${peekToken()}")
        }
    }

    // <road> ::= 'road' <string> '{' <command>* '}'
    private fun road() {
        match("road")
        stringLiteral()
        match("lcurly")

        // Parse commands if any
        while (isCommandStart(peekToken())) {
            command()
        }

        match("rcurly")
    }

    // <building> ::= 'building' <string> '{' <command> '}'
    private fun building() {
        match("building")
        stringLiteral()
        match("lcurly")

        // At least one command is required
        if (isCommandStart(peekToken())) {
            command()
        } else {
            throw Exception("Expected command in building block")
        }

        // Parse more commands if available
        while (isCommandStart(peekToken())) {
            command()
        }

        match("rcurly")
    }

    // <bus_stop> ::= 'bus_stop' <string> int '{' <command> '}'
    private fun busStop() {
        match("bus_stop")
        stringLiteral()
        intLiteral()
        match("lcurly")

        // At least one command is required
        if (isCommandStart(peekToken())) {
            command()
        } else {
            throw Exception("Expected command in bus_stop block")
        }

        // Parse more commands if available
        while (isCommandStart(peekToken())) {
            command()
        }

        match("rcurly")
    }

    // <bus_line> ::= 'bus_line' <string> int '{' <command> '}'
    private fun busLine() {
        match("bus_line")
        stringLiteral()
        intLiteral()
        match("lcurly")

        // At least one command is required
        if (isCommandStart(peekToken())) {
            command()
        } else {
            throw Exception("Expected command in bus_line block")
        }

        // Parse more commands if available
        while (isCommandStart(peekToken())) {
            command()
        }

        match("rcurly")
    }

    // <command> ::= <line_cmd> | <bend_cmd> | <box_cmd> | <circ_cmd>
    private fun command() {
        when {
            peekToken().startsWith("line") -> lineCmd()
            peekToken().startsWith("bend") -> bendCmd()
            peekToken().startsWith("box") -> boxCmd()
            peekToken().startsWith("circ") -> circCmd()
            else -> throw Exception("Expected command but found ${peekToken()}")
        }
    }

    // <line_cmd> ::= 'line' '(' <point> ',' <point> ')'';'
    private fun lineCmd() {
        match("line")
        match("lparen")
        point()
        match("comma")
        point()
        match("rparen")
        match("semi")
    }

    // <bend_cmd> ::= 'bend' '(' <point> ',' <point> ',' <number> ')'';'
    private fun bendCmd() {
        match("bend")
        match("lparen")
        point()
        match("comma")
        point()
        match("comma")
        number()
        match("rparen")
        match("semi")
    }

    // <box_cmd> ::= 'box' '(' <point> ',' <point> ')'';'
    private fun boxCmd() {
        match("box")
        match("lparen")
        point()
        match("comma")
        point()
        match("rparen")
        match("semi")
    }

    // <circ_cmd> ::= 'circ' '(' <point> ',' <number> ')'';'
    private fun circCmd() {
        match("circ")
        match("lparen")
        point()
        match("comma")
        number()
        match("rparen")
        match("semi")
    }

    // <point> ::= '(' <number> ',' <number> ')'
    private fun point() {
        match("lparen")
        number()
        match("comma")
        number()
        match("rparen")
    }

    // <number> ::= int | int . int
    private fun number() {
        if (peekToken().startsWith("int") || peekToken().startsWith("double")) {
            consumeToken()
        } else {
            throw Exception("Expected number but found ${peekToken()}")
        }
    }

    // <string> ::= '"' <char>* '"'
    private fun stringLiteral() {
        if (peekToken().startsWith("quote")) {
            consumeToken()
        } else {
            throw Exception("Expected string but found ${peekToken()}")
        }
    }

    private fun intLiteral() {
        if (peekToken().startsWith("int")) {
            consumeToken()
        } else {
            throw Exception("Expected integer but found ${peekToken()}")
        }
    }

    // Helper methods
    private fun match(tokenType: String) {
        if (currentTokenIndex < tokens.size) {
            val currentToken = tokens[currentTokenIndex]
            if (currentToken.startsWith(tokenType)) {
                consumeToken()
            } else {
                throw Exception("Expected $tokenType but found $currentToken")
            }
        } else {
            throw Exception("Expected $tokenType but reached end of input")
        }
    }

    private fun peekToken(): String {
        return if (currentTokenIndex < tokens.size) tokens[currentTokenIndex] else ""
    }

    private fun consumeToken() {
        currentTokenIndex++
    }

    private fun isElementStart(token: String): Boolean {
        return token.startsWith("road") ||
                token.startsWith("building") ||
                token.startsWith("bus_stop") ||
                token.startsWith("bus_line")
    }

    private fun isCommandStart(token: String): Boolean {
        return token.startsWith("line") ||
                token.startsWith("bend") ||
                token.startsWith("box") ||
                token.startsWith("circ")
    }
}