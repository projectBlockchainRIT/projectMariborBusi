package db
import dao.UserDao
import model.User
import model.Line
import model.Route
import model.Stop
import model.Departure
import model.Direction
import dao.postgres.PostgreUserDao
import dao.postgres.PostgreLineDao
import dao.postgres.PostgreRouteDao
import dao.postgres.PostgreDirectionDao
import dao.postgres.PostgreDepartureDao
import dao.postgres.PostgreStopDao
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

fun main() {
    val conn = DatabaseConnector.getConnection()
    if (conn != null) {
        println("Povezava ok!")

        val userDao = PostgreUserDao()
        val lineDao = PostgreLineDao()
        val routeDao = PostgreRouteDao()
        val stopDao = PostgreStopDao()
        val departureDao = PostgreDepartureDao()
        val directionDao = PostgreDirectionDao()

        val user = User (
            username = "test",
            email = "test@example.com",
            password = "1234",
            createdAt = "2025-05-20T12:00:00+02:00",
            lastLogin = null
        )

        val line = Line (
            lineCode = "G13"
        )

        val stop = Stop (
            id = 1,
            number = "13",
            name = "test postaja",
            latitude = 2.3,
            longitude = 2.1
        )

        val direction = Direction (
            lineId = 4,
            name = "smer"
        )

        val departure = Departure (
            stopId = 1,
            directionId = 1,
            departure = "13:30:10"
        )

        val geoJsonPath = """
        [
            [46.5606, 15.6459],
            [46.5610, 15.6465],
            [46.5620, 15.6475]
        ]
        """

        val pathElement: JsonElement = Json.parseToJsonElement(geoJsonPath)

        val route = Route(
            name = "P99 route",
            path = pathElement,
            lineId = 4
        )

        /*
        val userIns = userDao.insert(user)
        println("Ins user: $userIns")
        val lineIns = lineDao.insert(line)
        println("Ins line: $lineIns")
        val stopIns = stopDao.insert(stop)
        println("Ins stop: $stopIns")

        val directionIns = directionDao.insert(direction)
        println("Ins direction: $directionIns")
        val departureIns = departureDao.insert(departure)
        println("Ins departure: $departureIns")

        val routeIns = routeDao.insert(route)
        println("Ins route: $routeIns")
        */

        println(userDao.getById(5))
        println(stopDao.getById(1))
        println(lineDao.getById(4))
        println(routeDao.getById(1))
        println(directionDao.getById(1))
        println(departureDao.getById(1))

        val updatedDirection = direction.copy(id = 1, lineId = 4, name = "nova smer")

        println(directionDao.update(updatedDirection))

        conn.close()
    }
}
