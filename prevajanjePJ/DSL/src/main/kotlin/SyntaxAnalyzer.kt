class Token(val type: String, val value: String) {
    override fun toString(): String = "$type($value)"
}

// src/main/kotlin/SyntaxAnalyzer.kt

class SyntaxAnalyzer(private val tokens: List<String>) {
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

    // Start the recursive descent parsing and build AST
    fun parse(): ProgramNode {
        parseTokens()
        return program()
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
    private fun match(type: String): Token {
        val token = peek()
        if (token != null && token.type == type) {
            return consume()!!
        } else {
            throw Exception("Expected $type but found ${token?.type ?: "end of input"}")
        }
    }

    // <program> ::= <stmt>*
    private fun program(): ProgramNode {
        val programNode = ProgramNode()
        while (peek() != null) {
            programNode.statements.add(stmt())
        }
        return programNode
    }

    // <stmt> ::= <city> | <include_stmt> | <assignment> | <if_stmt> | <for_stmt>
    private fun stmt(): StatementNode {
        return when (peek()?.type) {
            "city" -> city()
            "import" -> includeStmt()
            "variable" -> {
                val savedIndex = currentTokenIndex
                val varToken = consume()!!
                if (peek()?.type == "assign") {
                    currentTokenIndex = savedIndex
                    assignment()
                } else {
                    currentTokenIndex = savedIndex
                    throw Exception("Expected assignment after variable")
                }
            }

            "if" -> ifStmt() as StatementNode
            "for" -> forStmt() as StatementNode
            else -> throw Exception("Invalid statement starting with ${peek()?.type}")
        } as StatementNode
    }

    // <include_stmt> ::= 'include' <string> ';'
    private fun includeStmt(): ImportNode {
        match("import")
        match("quote")
        val path = consume()!!.value
        match("quote")
        match("semi")
        return ImportNode(path)
    }

    // <city> ::= 'city' <string> ('(' <style> (',' <style>)? ')')? '{' <city_body> '}'
    private fun city(): CityNode {
        match("city")
        match("quote")
        val name = consume()!!.value
        match("quote")

        val styles = mutableListOf<StyleNode>()
        if (peek()?.type == "lparen") {
            match("lparen")
            styles.add(style())
            if (peek()?.type != "rparen") {
                styles.add(style())
            }
            match("rparen")
        }

        match("lcurly")
        val cityNode = CityNode(name, styles)
        cityBody(cityNode)
        match("rcurly")

        return cityNode
    }

    // <city_body> ::= <element>*
    private fun cityBody(cityNode: CityNode) {
        while (peek()?.type in listOf("road", "building", "bus_stop", "bus_line", "for", "if")) {
            cityNode.elements.add(element())
        }
    }

    // <element> ::= <road> | <building> | <station> | <busline> | <if_stmt> | <for_stmt>
    private fun element(): ElementNode {
        return when (peek()?.type) {
            "road" -> road()
            "building" -> building()
            "bus_stop" -> station()
            "bus_line" -> busline()
            "for" -> forStmt()
            "if" -> ifStmt()
            else -> throw Exception("Invalid element starting with ${peek()?.type}")
        } as ElementNode
    }

    // <road> ::= 'road' <string> ('(' <style> (',' <style>)? ')')? '{' <command>* '}'
    private fun road(): RoadNode {
        match("road")
        match("quote")
        val name = consume()!!.value
        match("quote")

        val styles = mutableListOf<StyleNode>()
        if (peek()?.type == "lparen") {
            match("lparen")
            styles.add(style())
            if (peek()?.type != "rparen") {
                styles.add(style())
            }
            match("rparen")
        }

        match("lcurly")
        val roadNode = RoadNode(name, styles)
        while (isCommandStart()) {
            roadNode.commands.add(command())
        }
        match("rcurly")

        return roadNode
    }

    // <building> ::= 'building' <string> ('(' <style> (',' <style>)? ')')? '{' <command>* '}'
    private fun building(): BuildingNode {
        match("building")
        match("quote")
        val name = consume()!!.value
        match("quote")

        val styles = mutableListOf<StyleNode>()
        if (peek()?.type == "lparen") {
            match("lparen")
            styles.add(style())
            if (peek()?.type != "rparen") {
                styles.add(style())
            }
            match("rparen")
        }

        match("lcurly")
        val buildingNode = BuildingNode(name, styles)
        while (isCommandStart()) {
            buildingNode.commands.add(command())
        }
        match("rcurly")

        return buildingNode
    }

    // <station> ::= 'bus_stop' <string> ('(' <style> (',' <style>)? ')')? '{' 'location' '(' <point> ')' ';' '}'
    private fun station(): BusStopNode {
        match("bus_stop")
        match("quote")
        val name = consume()!!.value
        match("quote")

        val styles = mutableListOf<StyleNode>()
        if (peek()?.type == "lparen") {
            match("lparen")
            styles.add(style())
            if (peek()?.type != "rparen") {
                styles.add(style())
            }
            match("rparen")
        }

        match("lcurly")
        match("location")
        match("lparen")
        val location = point()
        match("rparen")
        match("semi")
        match("rcurly")

        return BusStopNode(name, styles, location)
    }

    // <busline> ::= 'busline' <string> ('(' <style> (',' <style>)? ')')? '{' <command>* '}'
    private fun busline(): BusLineNode {
        match("bus_line")
        match("quote")
        val name = consume()!!.value
        match("quote")

        val styles = mutableListOf<StyleNode>()
        if (peek()?.type == "lparen") {
            match("lparen")
            styles.add(style())
            if (peek()?.type != "rparen") {
                styles.add(style())
            }
            match("rparen")
        }

        match("lcurly")
        val busLineNode = BusLineNode(name, styles)
        while (isCommandStart()) {
            busLineNode.commands.add(command())
        }
        match("rcurly")

        return busLineNode
    }

    // <style> ::= <color> | <line_style>
    private fun style(): StyleNode {
        return when (peek()?.type) {
            "color" -> {
                val token = match("color")
                ColorNode(token.value)
            }
            "solid", "dashed", "dotted" -> {
                val token = match(peek()!!.type)
                LineStyleNode(token.value)
            }
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

    // <command> ::= <draw_cmd> | <assignment> | <function_call> | <if_stmt> | <for_stmt>
    private fun command(): CommandNode {
        return when (peek()?.type) {
            "line", "bend", "box", "circ" -> drawCmd()
            "variable" -> {
                // Check if assignment or variable reference
                val savedIndex = currentTokenIndex
                val varName = consume()!!.value
                if (peek()?.type == "assign") {
                    match("assign")
                    val value = expression()
                    match("semi")
                    AssignmentNode(varName, value)
                } else {
                    currentTokenIndex = savedIndex
                    val expr = expression()
                    match("semi")
                    throw Exception("Expected assignment after variable") // This should probably be handled differently
                }
            }

            "distance", "midpoint" -> {
                val funcCall = functionCall()
                match("semi")
                throw Exception("Function call as command not implemented") // This should be handled properly
            }

            "if" -> ifStmt()
            "for" -> forStmt()
            else -> throw Exception("Invalid command starting with ${peek()?.type}")
        } as CommandNode
    }

    // <draw_cmd> ::= <line_cmd> | <bend_cmd> | <box_cmd> | <circ_cmd>
    private fun drawCmd(): CommandNode {
        return when (peek()?.type) {
            "line" -> lineCmd()
            "bend" -> bendCmd()
            "box" -> boxCmd()
            "circ" -> circCmd()
            else -> throw Exception("Invalid draw command: ${peek()?.type}")
        }
    }

    // <line_cmd> ::= 'line' '(' <point> ',' <point> ')' ';'
    private fun lineCmd(): LineCommandNode {
        match("line")
        match("lparen")
        val start = point()
        match("comma")
        val end = point()
        match("rparen")
        match("semi")
        return LineCommandNode(start, end)
    }

    // <bend_cmd> ::= 'bend' '(' <point> ',' <point> ',' <expression> ')' ';'
    private fun bendCmd(): BendCommandNode {
        match("bend")
        match("lparen")
        val start = point()
        match("comma")
        val end = point()
        match("comma")
        val angle = expression()
        match("rparen")
        match("semi")
        return BendCommandNode(start, end, angle)
    }

    // <box_cmd> ::= 'box' '(' <point> ',' <point> ')' ';'
    private fun boxCmd(): BoxCommandNode {
        match("box")
        match("lparen")
        val start = point()
        match("comma")
        val end = point()
        match("rparen")
        match("semi")
        return BoxCommandNode(start, end)
    }

    // <circ_cmd> ::= 'circ' '(' <point> ',' <expression> ')' ';'
    private fun circCmd(): CircCommandNode {
        match("circ")
        match("lparen")
        val center = point()
        match("comma")
        val radius = expression()
        match("rparen")
        match("semi")
        return CircCommandNode(center, radius)
    }

    // <assignment> ::= <identifier> '=' <expression> ';'
    private fun assignment(): AssignmentNode {
        val varName = match("variable").value
        match("assign")
        val value = expression()
        match("semi")
        return AssignmentNode(varName, value)
    }

    // <expression> ::= <term> (<op> <term>)*
    private fun expression(): ExpressionNode {
        var left = term()

        while (peek()?.type in listOf("plus", "minus", "multiply", "divide", "less", "greater", "equal", "not_equal")) {
            val op = match(peek()!!.type).type
            val right = term()
            left = BinaryOpNode(left, op, right)
        }

        return left
    }

    // Helper for parsing terms in expressions
    private fun term(): ExpressionNode {
        return when (peek()?.type) {
            "int", "double" -> {
                val token = match(peek()!!.type)
                NumberNode(token.value.toDouble())
            }
            "variable" -> {
                val token = match("variable")
                VariableNode(token.value)
            }
            "distance", "midpoint" -> functionCall()
            "lparen" -> {
                match("lparen")
                val first = expression()

                if (peek()?.type == "comma") {
                    // This is a point
                    match("comma")
                    val second = expression()
                    match("rparen")
                    PointNode(first, second)
                } else {
                    // This is just a parenthesized expression
                    match("rparen")
                    first
                }
            }
            else -> throw Exception("Invalid term: ${peek()?.type}")
        }
    }

    // <function_call> ::= <func_name> '(' <arg_list>? ')'
    private fun functionCall(): FunctionCallNode {
        val funcName = match(peek()!!.type).type
        match("lparen")

        val args = mutableListOf<ExpressionNode>()
        if (peek()?.type != "rparen") {
            args.addAll(argList())
        }

        match("rparen")
        return FunctionCallNode(funcName, args)
    }

    // <arg_list> ::= <expression> (',' <expression>)*
    private fun argList(): List<ExpressionNode> {
        val args = mutableListOf<ExpressionNode>()
        args.add(expression())

        while (peek()?.type == "comma") {
            match("comma")
            args.add(expression())
        }

        return args
    }

    // <if_stmt> ::= 'if' '(' <expression> ')' '{' (<stmt> | <command>*) '}' ('else' '{' (<stmt> | <command>*) '}')?
    private fun ifStmt(): IfNode {
        match("if")
        match("lparen")
        val condition = expression()
        match("rparen")
        match("lcurly")

        val thenBody = mutableListOf<ASTNode>()
        if (peek()?.type == "city") {
            thenBody.add(stmt())
        } else {
            while (isCommandStart()) {
                thenBody.add(command())
            }
        }

        match("rcurly")

        val elseBody = if (peek()?.type == "else") {
            match("else")
            match("lcurly")

            val body = mutableListOf<ASTNode>()
            if (peek()?.type == "city") {
                body.add(stmt())
            } else {
                while (isCommandStart()) {
                    body.add(command())
                }
            }

            match("rcurly")
            body
        } else null

        return IfNode(condition, thenBody, elseBody)
    }

    // <for_stmt> ::= 'for' '(' <assignment> 'to' <expression> ')' '{' (<element> | <command>*) '}'
    private fun forStmt(): ForNode {
        match("for")
        match("lparen")

        val varNode = match("variable")
        val variable = varNode.value
        match("assign")
        val start = expression()

        match("to")
        val end = expression()

        match("rparen")
        match("lcurly")

        val body = mutableListOf<ASTNode>()
        if (peek()?.type in listOf("road", "building", "bus_stop", "bus_line", "if", "for")) {
            body.add(element())
        } else {
            while (isCommandStart()) {
                body.add(command())
            }
        }

        match("rcurly")

        return ForNode(variable, start, end, body)
    }

    // <point> ::= '(' <expression> ',' <expression> ')' or a variable reference
    private fun point(): PointNode {
        if (peek()?.type == "variable") {
            // Variable referring to a point
            val varName = match("variable").value
            return PointNode(VariableNode(varName), VariableNode("dummy"))  // This is a placeholder - actual handling would be more complex
        } else {
            match("lparen")
            val x = expression()
            match("comma")
            val y = expression()
            match("rparen")
            return PointNode(x, y)
        }
    }

    // Get the AST tree as a string representation
    fun getAstTreeString(): String {
        val sb = StringBuilder()
        sb.append("AST Tree:\n")
        for (token in tokenStack) {
            sb.append(token.toString()).append("\n")
        }
        return sb.toString()
    }
}