package db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.regex.Pattern
import java.sql.PreparedStatement
import java.sql.Time
import java.time.LocalTime
import scraper.*

data class Stop(val id: Int, val number: String, val name: String, val latitude: Double, val longitude: Double)
data class DepartureInfo(val line: String, val direction: String, val times: List<String>)
data class StopDepartures(
    val id: Int,
    val number: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val departures: List<DepartureInfo>
)
data class Route(val route: String, val date: String, val path: List<List<Double>>)

fun fixJsonFormat(jsonStr: String): String {
    var fixed = jsonStr.trim()

    // Dodaj začetni oglat oklepaj, če manjka
    if (!fixed.startsWith("[")) {
        fixed = "[$fixed"
    }
    // Dodaj končni oglat oklepaj, če manjka
    if (!fixed.endsWith("]")) {
        fixed = "$fixed]"
    }
    // Popravi manjkajoče vejice med objekti (}){ -> },{
    val pattern = Pattern.compile("}\\s*\\{")
    val matcher = pattern.matcher(fixed)
    fixed = matcher.replaceAll("},{")

    return fixed
}

inline fun <reified T> loadJsonWithFix(path: String): List<T> {
    return try {
        val raw = File(path).readText()
        val fixed = fixJsonFormat(raw)
        jacksonObjectMapper().readValue(fixed)
    } catch (e: Exception) {
        println("Error loading JSON from $path: ${e.message}")
        emptyList()
    }
}

fun connectToDatabase(): Connection {
    val url = "jdbc:postgresql://localhost:5432/bus_base"
    val user = "postgres"
    val password = "1234" // Če imaš geslo, ga vpiši tukaj
    return DriverManager.getConnection(url, user, password)
}

fun loadJsonStops(path: String): List<Stop> = loadJsonWithFix(path)
fun loadJsonArrivals(path: String): List<StopDepartures> = loadJsonWithFix(path)
fun loadJsonRoutes(path: String): List<Route> = loadJsonWithFix(path)

fun insertStops(conn: Connection, stops: List<Stop>) {
    val sql = """
        INSERT INTO stops (id, number, name, latitude, longitude)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE
        SET number = EXCLUDED.number,
            name = EXCLUDED.name,
            latitude = EXCLUDED.latitude,
            longitude = EXCLUDED.longitude
    """.trimIndent()

    val stmt = conn.prepareStatement(sql)

    for (stop in stops) {
        stmt.setInt(1, stop.id)
        stmt.setString(2, stop.number)
        stmt.setString(3, stop.name)
        stmt.setDouble(4, stop.latitude)
        stmt.setDouble(5, stop.longitude)
        stmt.executeUpdate()
    }

    stmt.close()
    println("Inserted ${stops.size} stops.")
}

fun insertDepartures(conn: Connection, arrivals: List<StopDepartures>) {
    for (stop in arrivals) {
        val stopId = stop.id
        for (departure in stop.departures) {
            val lineCode = departure.line
            val directionName = departure.direction

            val lineId = insertOrGetId(conn, "lines", "line_code", lineCode)
            val directionId = insertOrGetDirection(conn, lineId, directionName)

            for (time in departure.times) {
                val sql = """
                    INSERT INTO departures (stop_id, direction_id, departure)
                    VALUES (?, ?, ?)
                    ON CONFLICT DO NOTHING
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setInt(1, stopId)
                    stmt.setInt(2, directionId)
                    stmt.setTime(3, Time.valueOf(LocalTime.parse(time)))
                    stmt.executeUpdate()
                }
            }
        }
    }
    println("Departures inserted.")
}

fun insertRoutes(conn: Connection, routes: List<Route>) {
    for (route in routes) {
        if (route.path.isEmpty()) continue
        val lineId = insertOrGetId(conn, "lines", "line_code", route.route)

        val sql = """
            INSERT INTO routes (name, path, line_id)
            VALUES (?, ?::jsonb, ?)
            ON CONFLICT ON CONSTRAINT uq_routes_name_line
            DO UPDATE SET path = EXCLUDED.path
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, route.route)
            stmt.setString(2, jacksonObjectMapper().writeValueAsString(route.path))
            stmt.setInt(3, lineId)
            stmt.executeUpdate()
        }
    }
    println("Routes inserted.")
}

fun insertOrGetId(conn: Connection, table: String, column: String, value: String): Int {
    val insertSQL = "INSERT INTO $table ($column) VALUES (?) ON CONFLICT ($column) DO NOTHING RETURNING id"
    conn.prepareStatement(insertSQL).use { stmt ->
        stmt.setString(1, value)
        val rs = stmt.executeQuery()
        if (rs.next()) return rs.getInt(1)
    }

    val selectSQL = "SELECT id FROM $table WHERE $column = ?"
    conn.prepareStatement(selectSQL).use { stmt ->
        stmt.setString(1, value)
        val rs = stmt.executeQuery()
        if (rs.next()) return rs.getInt(1)
    }

    error("Unable to insert or find $value in $table")
}

fun insertOrGetDirection(conn: Connection, lineId: Int, name: String): Int {
    val insertSQL = """
        INSERT INTO directions (line_id, name)
        VALUES (?, ?)
        ON CONFLICT (line_id, name) DO NOTHING
        RETURNING id
    """.trimIndent()
    conn.prepareStatement(insertSQL).use { stmt ->
        stmt.setInt(1, lineId)
        stmt.setString(2, name)
        val rs = stmt.executeQuery()
        if (rs.next()) return rs.getInt(1)
    }

    val selectSQL = "SELECT id FROM directions WHERE line_id = ? AND name = ?"
    conn.prepareStatement(selectSQL).use { stmt ->
        stmt.setInt(1, lineId)
        stmt.setString(2, name)
        val rs = stmt.executeQuery()
        if (rs.next()) return rs.getInt(1)
    }

    error("Unable to insert or find direction $name for line $lineId")
}

fun importJsonToDatabase(stopsFile: String, arrivalsFile: String, routesFile: String) {
    val conn = connectToDatabase()
    conn.autoCommit = true

    val stops = loadJsonStops(stopsFile)
    val arrivals = loadJsonArrivals(arrivalsFile)
    val routes = loadJsonRoutes(routesFile)

    println("conn ok")

    insertStops(conn, stops)
    insertDepartures(conn, arrivals)
    insertRoutes(conn, routes)

    conn.close()
}

suspend fun importDirectToDatabase() {
    val conn = connectToDatabase()
    conn.autoCommit = true

    val stopsJson = runStopsScraperAsJson()

    val departuresJson = runDeparturesScraperAsJson()

    val routesJson = runRoutesScraperAsJson()

    val gson = Gson()

    val fixedStopsJson = fixJsonFormat(stopsJson)
    val fixedDeparturesJson = fixJsonFormat(departuresJson)
    val fixedRoutesJson = fixJsonFormat(routesJson)

    val stopListType = object : TypeToken<List<Stop>>() {}.type
    val departureListType = object : TypeToken<List<StopDepartures>>() {}.type
    val routeListType = object : TypeToken<List<Route>>() {}.type // če imaš Route

    val stops: List<Stop> = gson.fromJson(fixedStopsJson, stopListType)
    val departures: List<StopDepartures> = gson.fromJson(fixedDeparturesJson, departureListType)
    val routes: List<Route> = gson.fromJson(fixedRoutesJson, routeListType)
    println("conn ok")

    insertStops(conn, stops)
    insertDepartures(conn, departures)
    insertRoutes(conn, routes)

    conn.close()
}