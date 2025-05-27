package utils.FakeData

import io.github.serpro69.kfaker.Faker
import model.Stop
import kotlin.random.Random


fun generateFakeStop(
    id: Int,
    numberRange: IntRange = 100..999,
    latRange: ClosedFloatingPointRange<Double> = 45.0..46.0,
    lonRange: ClosedFloatingPointRange<Double> = 14.0..15.0
): Stop {

    val faker = Faker()

    return Stop(
        id = id,
        number = numberRange.random().toString(),
        name = faker.address.city(),
        latitude = Random.nextDouble(latRange.start, latRange.endInclusive),
        longitude = Random.nextDouble(lonRange.start, lonRange.endInclusive)
    )
}
