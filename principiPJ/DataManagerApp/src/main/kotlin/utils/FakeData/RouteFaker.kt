package utils.FakeData

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import model.Route
import kotlin.random.Random

fun generateFakeRoute(lineId: Int): Route {
    val coordinates = buildJsonArray {
        repeat(Random.nextInt(3, 10)) {
            add(
                JsonArray(
                    listOf(
                        JsonPrimitive(Random.nextDouble(46.0, 47.0)), // lat
                        JsonPrimitive(Random.nextDouble(15.0, 16.0))  // lon
                    )
                )
            )
        }
    }

    return Route(
        id = null,
        name = "Pot ${Random.nextInt(1000)}",
        path = coordinates,
        lineId = lineId
    )
}

