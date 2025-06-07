package utils.FakeData

import model.Departure
import java.time.LocalTime
import kotlin.random.Random

fun generateFakeDeparture(stopId: Int, directionId: Int, from: String, to: String): Departure {
    val fromTime = LocalTime.parse(from)
    val toTime = LocalTime.parse(to)

    val fromSeconds = fromTime.toSecondOfDay()
    val toSeconds = toTime.toSecondOfDay()

    val randomSecond = Random.nextInt(fromSeconds, toSeconds)
    val generatedTime = LocalTime.ofSecondOfDay(randomSecond.toLong())

    val formattedTime = "%02d:%02d:%02d".format(generatedTime.hour, generatedTime.minute, generatedTime.second)

    return Departure(
        stopId = stopId,
        directionId = directionId,
        departure = formattedTime
    )
}


