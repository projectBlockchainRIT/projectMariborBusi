
class StationQueryHandler(private val program: ProgramNode) {

    fun handleQuery() {
        val busStops = collectBusStops()

        for (statement in program.statements) {
            if (statement is QueryNode) {
                if (statement.query == "radius") {
                    handleRadiusQuery(statement, busStops)
                } else if (statement.query == "nearest") {
                    handleNearestQuery(statement, busStops)
                } else {
                    println("Unknown query type: ${statement.query}")
                }
            }
        }
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
     * Handles a radius query to find bus stops within a certain distance.
     */
    private fun handleRadiusQuery(query: QueryNode, busStops: List<BusStopNode>) {
        val center = query.args[0] as PointNode
        val radius = (query.args[1] as NumberNode).value

        println("Bus stops within radius $radius from ${(center.x as NumberNode).value}, ${(center.y as NumberNode).value}:")
        for (stop in busStops) {
            val distance = calculateDistance(center, stop.location)
            if (distance <= radius as Double) {
                println(" - ${stop.name} at ${(stop.location.x as NumberNode).value}, ${(stop.location.y as NumberNode).value} (distance: $distance)")
            }
        }

    }

    /**
     * Handles a nearest query to find the nearest bus stop to a given point.
     */
    private fun handleNearestQuery(query: QueryNode, busStops: List<BusStopNode>) {
        val point = query.args[0] as PointNode
        var nearestStop: BusStopNode? = null
        var minDistance = Double.MAX_VALUE

        for (stop in busStops) {
            val distance = calculateDistance(point, stop.location)
            if (distance < minDistance) {
                minDistance = distance
                nearestStop = stop
            }
        }

        val x1 = (point.x as NumberNode).value

        if (nearestStop != null) {
            println("Nearest bus stop to ${(point.x as NumberNode).value}, ${(point.x as NumberNode).value} is ${nearestStop.name} at ${(nearestStop.location.x as NumberNode).value}, ${(nearestStop.location.y as NumberNode).value} with distance $minDistance")
        } else {
            println("No bus stops found.")
        }
    }


    /**
     * Calculates the distance between two points.
     * This is a placeholder implementation; replace with actual distance calculation logic.
     */
    private fun calculateDistance(point1: PointNode, point2: PointNode): Double {
        val e1 = point1.x as NumberNode
        val x1 = e1.value
        val f1 = point1.y as NumberNode
        val y1 = f1.value

        val e2 = point2.x as NumberNode
        val x2 = e2.value
        val f2 = point2.y as NumberNode
        val y2 = f2.value

        val distance = Math.sqrt(Math.pow((x2.toDouble() - x1.toDouble()), 2.0) + Math.pow((y2.toDouble() - y1.toDouble()), 2.0))
        return distance
    }

}