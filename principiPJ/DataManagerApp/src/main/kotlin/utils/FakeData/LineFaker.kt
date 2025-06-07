package utils.FakeData

import model.Line
import kotlin.random.Random

fun generateFakeLine(): Line {
    val letter = ('A'..'Z').random()
    val number = (1..99).random()

    val lineCode = "$letter$number"

    return Line(
        id = null,
        lineCode = lineCode
    )
}
