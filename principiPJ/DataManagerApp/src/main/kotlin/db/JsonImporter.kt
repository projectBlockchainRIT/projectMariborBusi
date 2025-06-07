package db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.core.type.TypeReference
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.regex.Pattern
import java.sql.PreparedStatement
import java.sql.Time
import java.sql.Date
import java.time.LocalTime
import scraper.*
import java.util.*

// ======== podatkovne strukture ========

data class Stop(
    val id: Int,
    val number: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class DepartureInfo(
    val line: String,
    val direction: String,
    val times: List<String>
)

data class StopDepartures(
    val id: Int,
    val number: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val departures: List<DepartureInfo>
)

data class Route(
    val route: String,
    val date: String,
    val path: List<List<Double>>
)

// ======== funkcije za branje JSON in popravek formata ========

fun fixJsonFormat(jsonStr: String): String {
    var fixed = jsonStr.trim()

    //doda začetni oglat oklepaj, če manjka
    if (!fixed.startsWith("[")) {
        fixed = "[$fixed"
    }
    //doda končni oglat oklepaj, če manjka
    if (!fixed.endsWith("]")) {
        fixed = "$fixed]"
    }
    //popravi manjkajoče vejice med objekti (}){ -> },{
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

fun loadJsonArrivals(path: String): Map<String, List<StopDepartures>> {
    return try {
        val raw = File(path).readText()
        val fixed = fixJsonFormat(raw)
        val mapper = jacksonObjectMapper()
        val typeRef = object : TypeReference<Map<String, List<StopDepartures>>>() {}
        mapper.readValue(fixed, typeRef)
    } catch (e: Exception) {
        println("Error loading JSON arrivals from $path: ${e.message}")
        emptyMap()
    }
}

fun loadJsonStops(path: String): List<Stop> = loadJsonWithFix(path)
fun loadJsonRoutes(path: String): List<Route> = loadJsonWithFix(path)

// ======== povezava z bazo ========

fun connectToDatabase(): Connection {
    val url = "jdbc:postgresql://localhost:5432/bus_base"
    val user = "postgres"
    val password = "1234" // Če imaš geslo, ga vpiši tukaj
    return DriverManager.getConnection(url, user, password)
}

// ======== vstavljanje postaj (stops) ========

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

// ======== vstavljanje odhodov (departures) z datumom in TEXT[] ali TIME[] ========

fun insertDepartures(
    conn: Connection,
    arrivalsByDate: Map<String, List<StopDepartures>>
) {
    //številniki za poročanje
    var linesCount = 0
    var directionsCount = 0
    var departureRunsCount = 0
    var arrivalsCount = 0

    fun insertOrGetLineId(lineCode: String): Int {
        val insertSQL = "INSERT INTO lines (line_code) VALUES (?) ON CONFLICT (line_code) DO NOTHING RETURNING id"
        conn.prepareStatement(insertSQL).use { stmt ->
            stmt.setString(1, lineCode)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                return rs.getInt(1).also { linesCount++ }
            }
        }
        //če ni vrnilo id-ja, poiščemo obstoječega
        val selectSQL = "SELECT id FROM lines WHERE line_code = ?"
        conn.prepareStatement(selectSQL).use { stmt ->
            stmt.setString(1, lineCode)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                return rs.getInt(1)
            }
        }
        error("Unable to insert or find line_code = $lineCode")
    }

    //podobno za direkcijo (direction)
    fun insertOrGetDirectionId(lineId: Int, directionName: String): Int {
        val insertSQL = """
            INSERT INTO directions (line_id, name)
            VALUES (?, ?)
            ON CONFLICT (line_id, name) DO NOTHING
            RETURNING id
        """.trimIndent()
        conn.prepareStatement(insertSQL).use { stmt ->
            stmt.setInt(1, lineId)
            stmt.setString(2, directionName)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                return rs.getInt(1).also { directionsCount++ }
            }
        }
        val selectSQL = "SELECT id FROM directions WHERE line_id = ? AND name = ?"
        conn.prepareStatement(selectSQL).use { stmt ->
            stmt.setInt(1, lineId)
            stmt.setString(2, directionName)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                return rs.getInt(1)
            }
        }
        error("Unable to insert or find direction = $directionName for line_id = $lineId")
    }

    //glavna logika: gremo po vseh parih (datum, seznam postaj)
    for ((dateStr, stopsOnDate) in arrivalsByDate) {
        val currentDate = Date.valueOf(dateStr)

        for (stopInfo in stopsOnDate) {
            val stopId = stopInfo.id
            val departuresList = stopInfo.departures

            if (departuresList.isEmpty()) continue

            for (departureInfo in departuresList) {
                val lineCode = departureInfo.line
                val directionName = departureInfo.direction
                val timesList = departureInfo.times

                if (lineCode.isBlank() || directionName.isBlank() || timesList.isEmpty()) continue

                try {
                    // 1) vstavi/poišči line_id
                    val lineId = insertOrGetLineId(lineCode)
                    conn.commit()

                    // 2) vstavi/poišči direction_id
                    val directionId = insertOrGetDirectionId(lineId, directionName)
                    conn.commit()

                    // 3) vstavi v tabelo departures (stop_id, direction_id, date)
                    val insertDepRunsSQL = """
                        INSERT INTO departures (stop_id, direction_id, date)
                        VALUES (?, ?, ?)
                        ON CONFLICT (stop_id, direction_id, date) DO NOTHING
                        RETURNING id
                    """.trimIndent()

                    val departuresId: Int = conn.prepareStatement(insertDepRunsSQL).use { stmt ->
                        stmt.setInt(1, stopId)
                        stmt.setInt(2, directionId)
                        stmt.setDate(3, currentDate)
                        val rs = stmt.executeQuery()
                        if (rs.next()) {
                            departureRunsCount++
                            rs.getInt(1)
                        } else {
                            //če ni novega vrnjenega ID-ja, ga poiščemo
                            stmt.close()
                            val selectDepRunsSQL =
                                "SELECT id FROM departures WHERE stop_id = ? AND direction_id = ? AND date = ?"
                            conn.prepareStatement(selectDepRunsSQL).use { selStmt ->
                                selStmt.setInt(1, stopId)
                                selStmt.setInt(2, directionId)
                                selStmt.setDate(3, currentDate)
                                val rs2 = selStmt.executeQuery()
                                if (rs2.next()) {
                                    rs2.getInt(1)
                                } else {
                                    error("Unable to retrieve departures_id for stop_id=$stopId, direction_id=$directionId on $currentDate")
                                }
                            }
                        }
                    }
                    conn.commit()

                    // 4) pripravimo TIME[] polje z vsemi časi
                    //    uporabimo createArrayOf za JDBC, da tvorimo realen PG TIME[]
                    val timeObjects = timesList
                        .map { LocalTime.parse(it) }
                        .map { Time.valueOf(it) }
                        .toTypedArray()

                    val pgTimeArray = conn.createArrayOf("time", timeObjects)

                    // 5) vstavi v tabelo arrivals (departure_time TIME[], departures_id)
                    val insertArrivalsSQL = """
                        INSERT INTO arrivals (departure_time, departures_id)
                        VALUES (?, ?)
                        ON CONFLICT (departures_id) DO NOTHING
                    """.trimIndent()

                    conn.prepareStatement(insertArrivalsSQL).use { stmtArr ->
                        stmtArr.setArray(1, pgTimeArray)
                        stmtArr.setInt(2, departuresId)
                        stmtArr.executeUpdate()
                    }
                    conn.commit()
                    arrivalsCount++

                } catch (e: Exception) {
                    println("Error importing departure for stop $stopId, line $lineCode on $currentDate: ${e.message}")
                    conn.rollback()
                }
            }
        }
    }

    println(
        "Imported $linesCount new lines, $directionsCount new directions, " +
                "$departureRunsCount new departure runs, and $arrivalsCount new arrival time sets."
    )
}

// ======== vstavljanje poti (routes) ========

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

suspend fun importDirectToDatabase() {
    val conn = connectToDatabase()
    conn.autoCommit = false

    try {
        val stopsJson = runStopsScraperAsJson()
        val departuresJson = runDeparturesScraperAsJson()
        val routesJson = runRoutesScraperAsJson()

        val mapper = jacksonObjectMapper()
        val gson = com.google.gson.Gson()

        //poprava formata in preverjanje strukture
        val fixedStopsJson = fixJsonFormat(stopsJson)
        val fixedDeparturesJson = fixJsonFormat(departuresJson)
        val fixedRoutesJson = fixJsonFormat(routesJson)

        //preverjanje in parsiranje postaj
        val stopListType = object : com.google.gson.reflect.TypeToken<List<Stop>>() {}.type
        val stops: List<Stop> = try {
            gson.fromJson(fixedStopsJson, stopListType) ?: emptyList()
        } catch (e: Exception) {
            println("Napaka pri parsiranju postaj: ${e.message}")
            println("JSON postaj: $fixedStopsJson")
            emptyList()
        }

        //preverjanje in parsiranje poti
        val routeListType = object : com.google.gson.reflect.TypeToken<List<Route>>() {}.type
        val routes: List<Route> = try {
            gson.fromJson(fixedRoutesJson, routeListType) ?: emptyList()
        } catch (e: Exception) {
            println("Napaka pri parsiranju poti: ${e.message}")
            println("JSON poti: $fixedRoutesJson")
            emptyList()
        }

        //preverjanje in parsiranje odhodov
        val type = object : TypeToken<List<Map<String, List<StopDepartures>>>>() {}.type
        val arrivalsByDate: Map<String, List<StopDepartures>> = try {
            val rawData: List<Map<String, List<StopDepartures>>> = gson.fromJson(fixedDeparturesJson, type) ?: emptyList()
            //pretvorimo seznam map v eno mapo
            rawData.flatMap { it.entries }.associate { it.toPair() }
        } catch (e: Exception) {
            println("Napaka pri parsiranju odhodov: ${e.message}")
            println("JSON odhodov: $fixedDeparturesJson")
            emptyMap()
        }

        println("Povezava do baze vzpostavljena.")

        if (stops.isNotEmpty()) {
            insertStops(conn, stops)
        } else {
            println("Opozorilo: Ni bilo najdenih postaj za uvoz.")
        }

        if (arrivalsByDate.isNotEmpty()) {
            insertDepartures(conn, arrivalsByDate)
        } else {
            println("Opozorilo: Ni bilo najdenih odhodov za uvoz.")
        }

        if (routes.isNotEmpty()) {
            insertRoutes(conn, routes)
        } else {
            println("Opozorilo: Ni bilo najdenih poti za uvoz.")
        }

        conn.commit()
        println("Uspešno izveden neposreden uvoz podatkov.")
    } catch (e: Exception) {
        println("Napaka med neposrednim uvozom: ${e.message}")
        e.printStackTrace() //dodano za boljše razumevanje napake
        conn.rollback()
    } finally {
        conn.close()
        println("Povezava do baze zaprta.")
    }
}