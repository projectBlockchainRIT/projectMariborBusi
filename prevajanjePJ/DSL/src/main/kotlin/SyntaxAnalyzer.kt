class Token(val type: String, val value: String) {
    override fun toString(): String = "$type($value)"
}

class SyntaxAnalyzer (private val tokens: List<String>) {
    private var currentTokenIndex = 0
    private val tokenStack = mutableListOf<Token>()

    // Parse tokens from the input format
    fun parseTokens() {
        for (tokenString in tokens) {
            val parts = tokenString.split("(", limit = 2)
            if (parts.size == 2) {
                val type = parts[0]
                val value = parts[1].removeSuffix("\")").removePrefix("\"")
                tokenStack.add(Token(type, value))
            }
        }
    }

    // Start the recursive descent parsing
    fun parse(): Boolean {
        try {
            program()
            return currentTokenIndex == tokenStack.size
        } catch (e: Exception) {
            println("Syntax error: ${e.message}")
            return false
        }
    }

    // Get the current token without consuming it
    private fun peek(): Token? {
        return if (currentTokenIndex < tokenStack.size) tokenStack[currentTokenIndex] else null
    }

    // Consume the current token and move to the next
    private fun consume(): Token? {
        return if (currentTokenIndex < tokenStack.size) tokenStack[currentTokenIndex++] else null
    }

    // Match a token of a specific type, throw exception if not matched
    private fun match(type: String) {
        val token = peek()
        if (token != null && token.type == type) {
            consume()
        } else {
            throw Exception("Expected $type but found ${token?.type ?: "end of input"}")
        }
    }

    // <program> ::= <stmt>*
    private fun program() {
        while (peek() != null) {
            stmt()
        }
    }

    // <stmt> ::= <city> | <include_stmt> | <assignment> | <if_stmt> | <for_stmt>
    private fun stmt() {
        when (peek()?.type) {
            "city" -> city()
            "import" -> includeStmt()  // Using 'import' since that was in the lexer
            "variable" -> {
                // Look ahead to see if this is an assignment
                val savedIndex = currentTokenIndex
                consume() // variable
                if (peek()?.type == "assign") {
                    currentTokenIndex = savedIndex
                    assignment()
                } else {
                    currentTokenIndex = savedIndex
                    throw Exception("Expected assignment after variable")
                }
            }
            "if" -> ifStmt()
            "for" -> forStmt()
            else -> throw Exception("Invalid statement starting with ${peek()?.type}")
        }
    }

    // <include_stmt> ::= 'include' <string> ';'
    private fun includeStmt() {
        match("import")  // Using 'import' as that was in the lexer
        match("quote")    // String is represented as a "quote" token
        match("semi")     // semicolon
    }

    // <city> ::= 'city' <string> '{' <city_body> '}'
    private fun city() {
        match("city")
        match("quote")
        match("variable")
        match("quote")
        match("lcurly")    // '{'
        cityBody()
        match("rcurly")    // '}'
    }

    // <city_body> ::= <element>*
    private fun cityBody() {
        while (peek()?.type in listOf("road", "building", "bus_stop", "bus_line")) {
            element()
        }
    }

    // <element> ::= <road> | <building> | <station> | <busline>
    private fun element() {
        when (peek()?.type) {
            "road" -> road()
            "building" -> building()
            "bus_stop" -> station()
            "bus_line" -> busline()  // Using bus_line as that was in the lexer
            else -> throw Exception("Invalid element starting with ${peek()?.type}")
        }
    }

    // <road> ::= 'road' <string> '(' <style> ')' '{' <command>* '}'
    private fun road() {
        match("road")
        match("quote")
        match("variable") // String
        match("quote")
        if (peek()?.type == "lparen") {
            match("lparen")
            style()
            match("rparen")
        }
        match("lcurly")   // '{'
        while (isCommandStart()) {
            command()
        }
        match("rcurly")   // '}'
    }

    // <building> ::= 'building' <string> '{' <command>* '}'
    private fun building() {
        match("building")
        match("quote")
        match("variable") // String
        match("quote")
        match("lcurly")   // '{'
        while (isCommandStart()) {
            command()
        }
        match("rcurly")   // '}'
    }

    // <station> ::= 'bus_stop' <string> '{' 'location' '(' <point> ')' ';' '}'
    private fun station() {
        match("bus_stop")
        match("quote")
        match("variable") // String
        match("quote")
        match("lcurly")   // '{'
        match("variable") // 'location'
        match("lparen")   // '('
        point()
        match("rparen")   // ')'
        match("semi")     // ';'
        match("rcurly")   // '}'
    }

    // <busline> ::= 'busline' <string> '{' <command>* '}'
    private fun busline() {
        match("bus_line") // Using bus_line from lexer
        match("quote")
        match("variable") // String
        match("quote")
        if (peek()?.type == "lparen") {
            match("lparen")
            style()
            match("rparen")
        }
        match("lcurly")   // '{'
        while (isCommandStart()) {
            command()
        }
        match("rcurly")   // '}'
    }

    // <style> ::= <color> | <line_style>
    private fun style() {
        when (peek()?.type) {
            "color" -> match("color")
            "solid", "dashed", "dotted" -> match(peek()!!.type)
            else -> throw Exception("Invalid style: ${peek()?.type}")
        }
    }

    // Helper to determine if token is the start of a command
    private fun isCommandStart(): Boolean {
        return when (peek()?.type) {
            "line", "bend", "box", "circ", // draw commands
            "variable",  // could be assignment or function call
            "if", "for", // control structures
            "distance", "midpoint" -> true // function calls
            else -> false
        }
    }

    // <command> ::= <draw_cmd> | <assignment> | <function_call> | <if_stmt> | <for_stmt> | ε
    private fun command() {
        when (peek()?.type) {
            "line", "bend", "box", "circ" -> drawCmd()
            "variable" -> {
                // Check if assignment or variable reference
                val savedIndex = currentTokenIndex
                consume() // variable
                if (peek()?.type == "assign") {
                    currentTokenIndex = savedIndex
                    assignment()
                } else {
                    currentTokenIndex = savedIndex
                    expression()
                    match("semi")
                }
            }
            "distance", "midpoint" -> functionCall()
            "if" -> ifStmt()
            "for" -> forStmt()
            else -> throw Exception("Invalid command starting with ${peek()?.type}")
        }
    }

    // <draw_cmd> ::= <line_cmd> | <bend_cmd> | <box_cmd> | <circ_cmd>
    private fun drawCmd() {
        when (peek()?.type) {
            "line" -> lineCmd()
            "bend" -> bendCmd()
            "box" -> boxCmd()
            "circ" -> circCmd()
            else -> throw Exception("Invalid draw command: ${peek()?.type}")
        }
    }

    // <line_cmd> ::= 'line' '(' <point> ',' <point> ')' ';'
    private fun lineCmd() {
        match("line")
        match("lparen")
        point()
        match("comma")
        point()
        match("rparen")
        match("semi")
    }

    // <bend_cmd> ::= 'bend' '(' <point> ',' <point> ',' <number> ')' ';'
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

    // <box_cmd> ::= 'box' '(' <point> ',' <point> ')' ';'
    private fun boxCmd() {
        match("box")
        match("lparen")
        point()
        match("comma")
        point()
        match("rparen")
        match("semi")
    }

    // <circ_cmd> ::= 'circ' '(' <point> ',' <number> ')' ';'
    private fun circCmd() {
        match("circ")
        match("lparen")
        point()
        match("comma")
        number()
        match("rparen")
        match("semi")
    }

    // <assignment> ::= <identifier> '=' <expression> ';'
    private fun assignment() {
        match("variable") // identifier
        match("assign")   // '='
        expression()
        match("semi")     // ';'
    }

    // <expression> ::= <number> | <identifier> | <expression> <op> <expression> | <function_call> | '(' <expression> ')'
    private fun expression() {
        term()
        while (peek()?.type in listOf("plus", "minus", "multiply", "divide")) {
            match(peek()!!.type) // operator
            term()
        }
    }

    // Helper for parsing terms in expressions
    private fun term() {
        when (peek()?.type) {
            "int", "double" -> number()
            "variable" -> match("variable") // identifier
            "distance", "midpoint" -> functionCall()
            "lparen" -> {
                match("lparen")
                expression()
                match("rparen")
            }
            else -> throw Exception("Invalid term: ${peek()?.type}")
        }
    }

    // <number> ::= int | int '.' int
    private fun number() {
        when (peek()?.type) {
            "int", "double" -> match(peek()!!.type)
            else -> throw Exception("Expected number but found ${peek()?.type}")
        }
    }

    // <function_call> ::= <func_name> '(' <arg_list>? ')' ';'
    private fun functionCall() {
        when (peek()?.type) {
            "distance", "midpoint" -> match(peek()!!.type)
            else -> throw Exception("Invalid function name: ${peek()?.type}")
        }
        match("lparen")

        // Optional arguments
        if (peek()?.type != "rparen") {
            argList()
        }

        match("rparen")
        // Function calls inside expressions don't end with semicolon
        if (peek()?.type == "semi") {
            match("semi")
        }
    }

    // <arg_list> ::= <expression> (',' <expression>)*
    private fun argList() {
        expression()
        while (peek()?.type == "comma") {
            match("comma")
            expression()
        }
    }

    // <if_stmt> ::= 'if' '(' <expression> ')' '{' <command>* '}' ('else' '{' <command>* '}')?
    private fun ifStmt() {
        match("if")
        match("lparen")
        expression()
        match("rparen")
        match("lcurly")

        while (isCommandStart()) {
            command()
        }

        match("rcurly")

        // Optional else part
        if (peek()?.type == "else") {
            match("else")
            match("lcurly")

            while (isCommandStart()) {
                command()
            }

            match("rcurly")
        }
    }

    // <for_stmt> ::= 'for' '(' <assignment> 'to' <assignment> ')' '{' <command>* '}'
    private fun forStmt() {
        match("for")
        match("lparen")
        assignment()

        // In the grammar it says 'to' here, but there's no token for this in lexer
        // We'll assume it's a variable with 'to' value
        match("variable") // 'to'

        assignment()
        match("rparen")
        match("lcurly")

        while (isCommandStart()) {
            command()
        }

        match("rcurly")
    }

    // <point> ::= '(' <number> ',' <number> ')'
    private fun point() {
        match("lparen")
        number()
        match("comma")
        number()
        match("rparen")
    }
}