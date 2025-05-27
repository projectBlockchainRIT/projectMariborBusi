package utils.FakeData

import model.Direction
import io.github.serpro69.kfaker.Faker


fun generateFakeDirection(lineId: Int): Direction {
    val faker = Faker()

    return Direction(
        id = null,
        lineId = lineId,
        name = faker.address.city()
    )
}
