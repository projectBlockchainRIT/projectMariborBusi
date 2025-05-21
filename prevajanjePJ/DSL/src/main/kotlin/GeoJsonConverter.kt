// src/main/kotlin/GeoJsonConverter.kt

import java.io.File
import java.util.*
import kotlin.math.*
import org.json.JSONObject
import org.json.JSONArray

class GeoJsonConverter {
    private var globalVariables: MutableMap<String, PointNode> = mutableMapOf()


    fun convertToGeoJson(program: ProgramNode): String {
        val featureCollection = mutableMapOf<String, Any>()
        featureCollection["type"] = "FeatureCollection"

        val features = mutableListOf<Map<String, Any>>()

        // Process all statements in the program
        for (statement in program.statements) {
            when (statement) {
                is CityNode -> processCity(statement, features)
                is ImportNode -> {
                    val LA = LexicalAnalyzer()
                    var tokens = listOf<String>()

                    File(statement.path).bufferedReader().use { reader ->
                        reader.forEachLine { line ->
                            tokens += LA.getTokens(line)
                        }
                    }

                    val SA = SyntaxAnalyzer(tokens)
                    val ast = SA.parse()
                    val geoJsonConverter = GeoJsonConverter()
                    val geoJsonString = geoJsonConverter.convertToGeoJson(ast)

                    // Parse string to JSONObject
                    val parsed = JSONObject(geoJsonString)
                    val importedFeatures = parsed.getJSONArray("features")

                    // Append each feature from imported file into the current feature list
                    for (i in 0 until importedFeatures.length()) {
                        val featureObj = importedFeatures.getJSONObject(i)
                        features.add(jsonToMap(featureObj))
                    }
                }
                // Other top-level statements don't produce GeoJSON features
            }
        }
        featureCollection["features"] = features
        return jsonToString(featureCollection)
    }

    private fun processCity(city: CityNode, features: MutableList<Map<String, Any>>) {
        features.add(
            mapOf(
                "type" to "Feature",
                "properties" to mapOf(
                    "name" to city.name,
                    "type" to "city"
                ),
                "geometry" to mapOf(
                    "type" to "Point",
                    "coordinates" to listOf(0, 0)  // Default city center
                )
            )
        )

        // Process all elements in the city
        for (element in city.elements) {
            when (element) {
                is RoadNode -> processRoad(element, features)
                is BuildingNode -> processBuilding(element, features)
                is BusStopNode -> processBusStop(element, features)
                is BusLineNode -> processBusLine(element, features)
                // Control structures are handled recursively
            }
        }
    }

    private fun processRoad(road: RoadNode, features: MutableList<Map<String, Any>>) {
        val coordinates = mutableListOf<List<Double>>()

        // Extract points from road commands
        for (command in road.commands) {
            when (command) {
                is LineCommandNode -> {
                    coordinates.add(evaluatePoint(command.start))
                    coordinates.add(evaluatePoint(command.end))
                }

                is BendCommandNode -> {

                }
                // Other commands don't directly contribute to the road path
            }
        }

        if (coordinates.isNotEmpty()) {
            features.add(
                mapOf(
                    "type" to "Feature",
                    "properties" to mapOf(
                        "name" to road.name,
                        "type" to "road"
                    ),
                    "geometry" to mapOf(
                        "type" to "LineString",
                        "coordinates" to coordinates
                    )
                )
            )
        }
    }

    private fun processBuilding(building: BuildingNode, features: MutableList<Map<String, Any>>) {
        for (command in building.commands) {
            when (command) {
                is BoxCommandNode -> {
                    val start = evaluatePoint(command.start)
                    val end = evaluatePoint(command.end)

                    val coordinates = listOf(
                        listOf(
                            listOf(start[0], start[1]),
                            listOf(end[0], start[1]),
                            listOf(end[0], end[1]),
                            listOf(start[0], end[1]),
                            listOf(start[0], start[1])  // Close the polygon
                        )
                    )

                    features.add(
                        mapOf(
                            "type" to "Feature",
                            "properties" to mapOf(
                                "name" to building.name,
                                "type" to "building"
                            ),
                            "geometry" to mapOf(
                                "type" to "Polygon",
                                "coordinates" to coordinates
                            )
                        )
                    )
                }

                is CircCommandNode -> {
                    val center = evaluatePoint(command.center)
                    val radius = evaluateExpression(command.radius)

                    // Create a circle by approximating with points
                    val coordinates = createCircleCoordinates(center[0], center[1], radius)

                    features.add(
                        mapOf(
                            "type" to "Feature",
                            "properties" to mapOf(
                                "name" to building.name,
                                "type" to "building"
                            ),
                            "geometry" to mapOf(
                                "type" to "Polygon",
                                "coordinates" to listOf(coordinates)
                            )
                        )
                    )
                }
                // Other commands don't create buildings
            }
        }
    }

    private fun processBusStop(busStop: BusStopNode, features: MutableList<Map<String, Any>>) {
        val coordinates = evaluatePoint(busStop.location)

        features.add(
            mapOf(
                "type" to "Feature",
                "properties" to mapOf(
                    "name" to busStop.name,
                    "type" to "bus_stop"
                ),
                "geometry" to mapOf(
                    "type" to "Point",
                    "coordinates" to coordinates
                )
            )
        )
    }

    private fun processBusLine(busLine: BusLineNode, features: MutableList<Map<String, Any>>) {
        val coordinates = mutableListOf<List<Double>>()

        for (command in busLine.commands) {
            when (command) {
                is LineCommandNode -> {
                    coordinates.add(evaluatePoint(command.start))
                    coordinates.add(evaluatePoint(command.end))
                }

                is BendCommandNode -> {

                    val start = evaluatePoint(command.start)
                    val end = evaluatePoint(command.end)
                    val angle = evaluateExpression(command.angle)

                    // Calculate bend points
                    val bendPoints = calculateBendPoints(start, end, angle)
                    coordinates.addAll(bendPoints)
                }
                // Other commands don't directly contribute to the bus line path
            }
        }

        if (coordinates.isNotEmpty()) {
            features.add(
                mapOf(
                    "type" to "Feature",
                    "properties" to mapOf(
                        "name" to busLine.name,
                        "type" to "bus_line"
                    ),
                    "geometry" to mapOf(
                        "type" to "LineString",
                        "coordinates" to coordinates
                    )
                )
            )
        }
    }

    // Helper methods for evaluating expressions and points
    private fun calculateBendPoints(start: List<Double>, end: List<Double>, angleDegrees: Double, segments: Int = 10): List<List<Double>> {
        val points = mutableListOf<List<Double>>()
        val x1 = start[0]
        val y1 = start[1]
        val x2 = end[0]
        val y2 = end[1]

        // Calculate the midpoint
        val mx = (x1 + x2) / 2
        val my = (y1 + y2) / 2

        // Calculate the vector from start to end
        val dx = x2 - x1
        val dy = y2 - y1

        // Calculate the length of the segment
        val length = Math.sqrt(dx * dx + dy * dy)

        // Calculate the normal vector (perpendicular to the segment)
        val nx = -dy / length
        val ny = dx / length

        // Calculate the distance from the midpoint to the arc center
        val angleRad = Math.toRadians(angleDegrees)
        val radius = length / (2 * Math.sin(angleRad / 2))
        val h = Math.sqrt(radius * radius - (length / 2) * (length / 2))

        // Determine the center of the arc
        val direction = if (angleDegrees > 0) 1 else -1
        val cx = mx + direction * nx * h
        val cy = my + direction * ny * h

        // Calculate start and end angles
        val startAngle = Math.atan2(y1 - cy, x1 - cx)
        val endAngle = Math.atan2(y2 - cy, x2 - cx)

        // Ensure the arc is drawn in the correct direction
        val sweep = if (direction > 0 && endAngle < startAngle) endAngle + 2 * Math.PI else endAngle
        val step = (sweep - startAngle) / segments

        for (i in 0..segments) {
            val theta = startAngle + i * step
            val px = cx + radius * Math.cos(theta)
            val py = cy + radius * Math.sin(theta)
            points.add(listOf(px, py))
        }

        return points
    }

    private fun jsonToMap(jsonObject: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)
            map[key] = when (value) {
                is JSONObject -> jsonToMap(value)
                is JSONArray -> jsonToList(value)
                else -> value
            }
        }
        return map
    }

    private fun jsonToList(jsonArray: JSONArray): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until jsonArray.length()) {
            val value = jsonArray.get(i)
            list.add(
                when (value) {
                    is JSONObject -> jsonToMap(value)
                    is JSONArray -> jsonToList(value)
                    else -> value
                }
            )
        }
        return list
    }



    private fun evaluatePoint(point: PointNode): List<Double> {
        val x = evaluateExpression(point.x)
        val y = evaluateExpression(point.y)
        return listOf(x, y)
    }

    private fun evaluateExpression(expr: ExpressionNode): Double {
        return when (expr) {
            is NumberNode -> expr.value
            is VariableNode -> 0.0  // In a full implementation, this would look up variable values
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

            is FunctionCallNode -> {
                // A simple implementation for common functions
                when (expr.name) {
                    "distance" -> {
                        if (expr.args.size >= 2 && expr.args[0] is PointNode && expr.args[1] is PointNode) {
                            val p1 = evaluatePoint(expr.args[0] as PointNode)
                            val p2 = evaluatePoint(expr.args[1] as PointNode)
                            Math.sqrt(Math.pow(p2[0] - p1[0], 2.0) + Math.pow(p2[1] - p1[1], 2.0))
                        } else 0.0
                    }

                    else -> 0.0
                }
            }

            else -> 0.0
        }
    }

    private fun createCircleCoordinates(centerX: Double, centerY: Double, radius: Double): List<List<Double>> {
        val coordinates = mutableListOf<List<Double>>()
        val steps = 36  // Number of points to approximate the circle

        for (i in 0..steps) {
            val angle = 2 * Math.PI * i / steps
            val x = centerX + radius * Math.cos(angle)
            val y = centerY + radius * Math.sin(angle)
            coordinates.add(listOf(x, y))
        }

        return coordinates
    }

    // A simple JSON string converter
    private fun jsonToString(obj: Any?): String {
        return when (obj) {
            null -> "null"
            is Map<*, *> -> {
                val entries = obj.entries.joinToString(", ") { (k, v) ->
                    "\"${k}\": ${jsonToString(v)}"
                }
                "{$entries}"
            }

            is List<*> -> {
                val items = obj.joinToString(", ") { jsonToString(it) }
                "[$items]"
            }

            is String -> "\"$obj\""
            is Number, is Boolean -> obj.toString()
            else -> "\"$obj\""
        }
    }

    // Add a method to save GeoJSON to a file
    fun saveToFile(geoJson: String, filePath: String) {
        File(filePath).writeText(geoJson)
    }
}