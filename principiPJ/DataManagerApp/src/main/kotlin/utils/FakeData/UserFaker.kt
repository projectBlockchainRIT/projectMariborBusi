package utils.FakeData

import io.github.serpro69.kfaker.Faker
import model.User
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random


fun generateFakeUser(id: Int): User {
    val faker = Faker()

    val now = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    return User(
        id = id,
        username = faker.name.name(),
        email = faker.internet.safeEmail(),
        password = (1000..9999).random().toString(),
        createdAt = now,
        lastLogin = null
    )
}

