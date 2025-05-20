package db
import dao.UserDao
import model.User
import model.Line
import model.Route
import dao.postgres.PostgreUserDao
import dao.postgres.PostgreLineDao
import dao.postgres.PostgreRouteDao
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

fun main() {
    val conn = DatabaseConnector.getConnection()
    if (conn != null) {
        println("Povezava ok!")

        val userDao = PostgreUserDao()
        val lineDao = PostgreLineDao()
        val routeDao = PostgreRouteDao()

        val user = User (
            username = "test",
            email = "test@example.com",
            password = "1234",
            createdAt = "2025-05-20T12:00:00+02:00",
            lastLogin = null
        )

        //val userInserted = userDao.insert(user)
        //println("Uporabnik vstavljen: $userInserted")

        //val user2 = userDao.getById(1)
        //println(user2.toString())

        conn.close()
    }
}
