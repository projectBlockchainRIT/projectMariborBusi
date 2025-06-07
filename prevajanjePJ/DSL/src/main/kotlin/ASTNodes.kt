// Base AST node interface
interface ASTNode

// Root node for the entire program
class ProgramNode(val statements: MutableList<StatementNode> = mutableListOf()) : ASTNode

// Statement types
interface StatementNode : ASTNode

class CityNode(
    val name: String,
    val styles: List<StyleNode> = listOf(),
    val elements: MutableList<ElementNode> = mutableListOf()
) : StatementNode

class ImportNode(val path: String) : StatementNode

// City elements
interface ElementNode : ASTNode

class RoadNode(
    val name: String,
    val styles: List<StyleNode> = listOf(),
    val commands: MutableList<CommandNode> = mutableListOf()
) : ElementNode

class BuildingNode(
    val name: String,
    val styles: List<StyleNode> = listOf(),
    val commands: MutableList<CommandNode> = mutableListOf()
) : ElementNode

class BusStopNode(
    val name: String,
    val styles: List<StyleNode> = listOf(),
    val metadata: Map<String, Any> = mapOf(),
    val location: PointNode
) : ElementNode

class BusLineNode(
    val name: String,
    val styles: List<StyleNode> = listOf(),
    val commands: MutableList<CommandNode> = mutableListOf()
) : ElementNode

class IfNode(
    val condition: ExpressionNode,
    val thenBody: MutableList<ASTNode> = mutableListOf(),
    val elseBody: MutableList<ASTNode>? = null
) : StatementNode, ElementNode, CommandNode

class ForNode(
    val variable: String,
    val start: ExpressionNode,
    val end: ExpressionNode,
    val body: MutableList<ASTNode> = mutableListOf()
) : StatementNode, ElementNode, CommandNode

class AssignmentNode(
    val variableName: String,
    val value: ExpressionNode
) : StatementNode, CommandNode

// Commands
interface CommandNode : ASTNode

class LineCommandNode(val start: PointNode, val end: PointNode) : CommandNode
class BendCommandNode(val start: PointNode, val end: PointNode, val angle: ExpressionNode) : CommandNode
class BoxCommandNode(val start: PointNode, val end: PointNode) : CommandNode
class CircCommandNode(val center: PointNode, val radius: ExpressionNode) : CommandNode

// Styles
abstract class StyleNode : ASTNode
class ColorNode(val colorValue: String) : StyleNode()
class LineStyleNode(val style: String) : StyleNode() // solid, dashed, dotted

// Expressions
abstract class ExpressionNode : ASTNode
class NumberNode(val value: Double) : ExpressionNode()
class VariableNode(val name: String) : ExpressionNode()
class BinaryOpNode(val left: ExpressionNode, val operator: String, val right: ExpressionNode) : ExpressionNode()
class FunctionCallNode(val name: String, val args: List<ExpressionNode>) : ExpressionNode()
class PointNode(val x: ExpressionNode, val y: ExpressionNode) : ExpressionNode()
class QueryNode(val query: String, val args: List<ExpressionNode>) : StatementNode