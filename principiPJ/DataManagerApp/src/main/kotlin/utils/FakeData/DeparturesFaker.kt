package utils.FakeData

import model.Departure
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

fun generateFakeDeparture(stopId: Int, directionId: Int, from: String, to: String): Departure {
    val fromTime = LocalTime.parse(from)
    val toTime = LocalTime.parse(to)

    val fromSeconds = fromTime.toSecondOfDay()
    val toSeconds = toTime.toSecondOfDay()

    val randomSecond = Random.nextInt(fromSeconds, toSeconds)
    val generatedTime = LocalTime.ofSecondOfDay(randomSecond.toLong())

    val formattedTime = generatedTime.format(DateTimeFormatter.ISO_LOCAL_TIME)
    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    return Departure(
        stopId = stopId,
        directionId = directionId,
        date = today
    )
}


