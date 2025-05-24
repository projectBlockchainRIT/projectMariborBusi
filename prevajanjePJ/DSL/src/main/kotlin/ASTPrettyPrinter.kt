class ASTPrettyPrinter {
    fun print(node: ASTNode, indent: Int = 0): String {
        val sb = StringBuilder()
        val indentStr = "  ".repeat(indent)

        when (node) {
            is ProgramNode -> {
                sb.appendLine("${indentStr}Program {")
                node.statements.forEach {
                    sb.append(print(it, indent + 1))
                }
                sb.appendLine("${indentStr}}")
            }
            is CityNode -> {
                sb.appendLine("${indentStr}City \"${node.name}\" {")
                if (node.styles.isNotEmpty()) {
                    sb.appendLine("${indentStr}  Styles [")
                    node.styles.forEach { sb.append(print(it, indent + 2)) }
                    sb.appendLine("${indentStr}  ]")
                }
                node.elements.forEach { sb.append(print(it, indent + 1)) }
                sb.appendLine("${indentStr}}")
            }
            is ImportNode -> {
                sb.appendLine("${indentStr}Import \"${node.path}\"")
            }
            is RoadNode -> {
                sb.appendLine("${indentStr}Road \"${node.name}\" {")
                if (node.styles.isNotEmpty()) {
                    sb.appendLine("${indentStr}  Styles [")
                    node.styles.forEach { sb.append(print(it, indent + 2)) }
                    sb.appendLine("${indentStr}  ]")
                }
                node.commands.forEach { sb.append(print(it, indent + 1)) }
                sb.appendLine("${indentStr}}")
            }
            is BuildingNode -> {
                sb.appendLine("${indentStr}Building \"${node.name}\" {")
                if (node.styles.isNotEmpty()) {
                    sb.appendLine("${indentStr}  Styles [")
                    node.styles.forEach { sb.append(print(it, indent + 2)) }
                    sb.appendLine("${indentStr}  ]")
                }
                node.commands.forEach { sb.append(print(it, indent + 1)) }
                sb.appendLine("${indentStr}}")
            }
            is BusStopNode -> {
                sb.appendLine("${indentStr}BusStop \"${node.name}\" {")
                if (node.styles.isNotEmpty()) {
                    sb.appendLine("${indentStr}  Styles [")
                    node.styles.forEach { sb.append(print(it, indent + 2)) }
                    sb.appendLine("${indentStr}  ]")
                }
                sb.appendLine("${indentStr}  Location: ${print(node.location, 0)}")
                sb.appendLine("${indentStr}}")
            }
            is BusLineNode -> {
                sb.appendLine("${indentStr}BusLine \"${node.name}\" {")
                if (node.styles.isNotEmpty()) {
                    sb.appendLine("${indentStr}  Styles [")
                    node.styles.forEach { sb.append(print(it, indent + 2)) }
                    sb.appendLine("${indentStr}  ]")
                }
                node.commands.forEach { sb.append(print(it, indent + 1)) }
                sb.appendLine("${indentStr}}")
            }
            is IfNode -> {
                sb.appendLine("${indentStr}If (${print(node.condition, 0)}) {")
                node.thenBody.forEach { sb.append(print(it, indent + 1)) }
                sb.appendLine("${indentStr}}")
                if (node.elseBody != null) {
                    sb.appendLine("${indentStr}Else {")
                    node.elseBody.forEach { sb.append(print(it, indent + 1)) }
                    sb.appendLine("${indentStr}}")
                }
            }
            is ForNode -> {
                sb.appendLine("${indentStr}For ${node.variable} = ${print(node.start, 0)} to ${print(node.end, 0)} {")
                node.body.forEach { sb.append(print(it, indent + 1)) }
                sb.appendLine("${indentStr}}")
            }
            is AssignmentNode -> {
                sb.appendLine("${indentStr}${node.variableName} = ${print(node.value, 0)};")
            }
            is LineCommandNode -> {
                sb.appendLine("${indentStr}line(${print(node.start, 0)}, ${print(node.end, 0)});")
            }
            is BendCommandNode -> {
                sb.appendLine("${indentStr}bend(${print(node.start, 0)}, ${print(node.end, 0)}, ${print(node.angle, 0)});")
            }
            is BoxCommandNode -> {
                sb.appendLine("${indentStr}box(${print(node.start, 0)}, ${print(node.end, 0)});")
            }
            is CircCommandNode -> {
                sb.appendLine("${indentStr}circ(${print(node.center, 0)}, ${print(node.radius, 0)});")
            }
            is ColorNode -> {
                sb.appendLine("${indentStr}Color: ${node.colorValue}")
            }
            is LineStyleNode -> {
                sb.appendLine("${indentStr}LineStyle: ${node.style}")
            }
            is NumberNode -> {
                return node.value.toString()
            }
            is VariableNode -> {
                return node.name
            }
            is BinaryOpNode -> {
                return "(${print(node.left, 0)} ${node.operator} ${print(node.right, 0)})"
            }
            is FunctionCallNode -> {
                val args = node.args.joinToString(", ") { print(it, 0) }
                return "${node.name}($args)"
            }
            is PointNode -> {
                return "(${print(node.x, 0)}, ${print(node.y, 0)})"
            }
            else -> {
                sb.appendLine("${indentStr}Unknown: ${node::class.simpleName}")
            }
        }

        return sb.toString()
    }
}
