import kotlin.math.*

class BusStopValidator(private val program: ProgramNode) {

    // Threshold distance to consider a bus stop "connected" to a line
    private val epsilon = 0.1

    fun validate(): Boolean {
        val busStops = collectBusStops()
        val busLines = collectBusLines()

        var allConnected = true

        for (stop in busStops) {
            val stopCoords = evaluatePoint(stop.location)
            var isConnected = false

            for (line in busLines) {
                if (isStopConnectedToLine(stopCoords, line)) {
                    isConnected = true
                    break
                }
            }

            if (!isConnected) {
                println("WARNING: Bus stop '${stop.name}' at coordinates (${stopCoords[0]}, ${stopCoords[1]}) is not connected to any bus line")
                allConnected = false
            }
        }

        return allConnected
    }

    /**
     * Collects all bus stops from the program.
     */
    private fun collectBusStops(): List<BusStopNode> {
        val stops = mutableListOf<BusStopNode>()

        for (statement in program.statements) {
            if (statement is CityNode) {
                for (element in statement.elements) {
                    if (element is BusStopNode) {
                        stops.add(element)
                    }
                }
            }
        }

        return stops
    }

    /**
     * Collects all bus lines from the program.
     */
    private fun collectBusLines(): List<BusLineNode> {
        val lines = mutableListOf<BusLineNode>()

        for (statement in program.statements) {
            if (statement is CityNode) {
                for (element in statement.elements) {
                    if (element is BusLineNode) {
                        lines.add(element)
                    }
                }
            }
        }

        return lines
    }

    /**
     * Checks if a bus stop is connected to a specific bus line.
     * @param stopCoords The coordinates of the bus stop [x, y]
     * @param busLine The bus line to check against
     * @return true if the stop is connected to the line, false otherwise
     */
    private fun isStopConnectedToLine(stopCoords: List<Double>, busLine: BusLineNode): Boolean {
        val segments = extractLineSegments(busLine)

        for (segment in segments) {
            val distance = pointToSegmentDistance(
                stopCoords[0], stopCoords[1],
                segment.first[0], segment.first[1],
                segment.second[0], segment.second[1]
            )

            if (distance < epsilon) {
                return true
            }
        }

        return false
    }

    /**
     * Extracts all line segments from a bus line.
     * @return List of segment pairs, where each pair contains start and end coordinates
     */
    private fun extractLineSegments(busLine: BusLineNode): List<Pair<List<Double>, List<Double>>> {
        val segments = mutableListOf<Pair<List<Double>, List<Double>>>()

        for (command in busLine.commands) {
            when (command) {
                is LineCommandNode -> {
                    val start = evaluatePoint(command.start)
                    val end = evaluatePoint(command.end)
                    segments.add(Pair(start, end))
                }
                is BendCommandNode -> {
                    val start = evaluatePoint(command.start)
                    val end = evaluatePoint(command.end)
                    val angle = evaluateExpression(command.angle)

                    val bendPoints = calculateBendPoints(start, end, angle)

                    for (i in 0 until bendPoints.size - 1) {
                        segments.add(Pair(bendPoints[i], bendPoints[i + 1]))
                    }
                }
            }
        }

        return segments
    }

    /**
     * Calculates points along a bend curve.
     */
    private fun calculateBendPoints(start: List<Double>, end: List<Double>, angleDegrees: Double, segments: Int = 10): List<List<Double>> {
        val points = mutableListOf<List<Double>>()
        val x1 = start[0]
        val y1 = start[1]
        val x2 = end[0]
        val y2 = end[1]

        val mx = (x1 + x2) / 2
        val my = (y1 + y2) / 2

        val dx = x2 - x1
        val dy = y2 - y1

        val length = sqrt(dx * dx + dy * dy)

        val nx = -dy / length
        val ny = dx / length

        val angleRad = Math.toRadians(angleDegrees)
        val radius = length / (2 * sin(angleRad / 2))
        val h = sqrt(radius * radius - (length / 2) * (length / 2))

        val direction = if (angleDegrees > 0) 1 else -1
        val cx = mx + direction * nx * h
        val cy = my + direction * ny * h

        val startAngle = atan2(y1 - cy, x1 - cx)
        val endAngle = atan2(y2 - cy, x2 - cx)

        val sweep = if (direction > 0 && endAngle < startAngle) endAngle + 2 * PI else endAngle
        val step = (sweep - startAngle) / segments

        for (i in 0..segments) {
            val theta = startAngle + i * step
            val px = cx + radius * cos(theta)
            val py = cy + radius * sin(theta)
            points.add(listOf(px, py))
        }

        return points
    }

    /**
     * Calculates the minimum distance from a point to a line segment.
     */
    private fun pointToSegmentDistance(x: Double, y: Double, x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val A = x - x1
        val B = y - y1
        val C = x2 - x1
        val D = y2 - y1

        val dot = A * C + B * D
        val lenSq = C * C + D * D

        if (lenSq == 0.0) return sqrt(A * A + B * B)

        var param = dot / lenSq

        param = param.coerceIn(0.0, 1.0)

        val xx = x1 + param * C
        val yy = y1 + param * D

        val dx = x - xx
        val dy = y - yy

        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Helper method to evaluate a PointNode to its x,y coordinates.
     */
    private fun evaluatePoint(point: PointNode): List<Double> {
        val x = evaluateExpression(point.x)
        val y = evaluateExpression(point.y)
        return listOf(x, y)
    }

    /**
     * Helper method to evaluate expressions to their numeric values.
     */
    private fun evaluateExpression(expr: ExpressionNode): Double {
        return when (expr) {
            is NumberNode -> expr.value
            is VariableNode -> 0.0
            is BinaryOpNode -> {
                val left = evaluateExpression(expr.left)
                val right = evaluateExpression(expr.right)
                when (expr.operator) {
                    "plus" -> left + right
                    "minus" -> left - right
                    "multiply" -> left * right
                    "divide" -> left / right
                    else -> 0.0
                }
            }
            else -> 0.0
        }
    }
}
