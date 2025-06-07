package dao.postgres

import dao.ArrivalDao
import db.DatabaseConnector
import model.Arrival
import java.sql.Time
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PostgreArrivalDao : ArrivalDao {
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    override fun getById(id: Int): Arrival? {
        val query = "SELECT * FROM arrivals WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, id)
                val rs = stmt.executeQuery()
                return if (rs.next()) {
                    val times = rs.getArray("departure_time").array as Array<Time>
                    Arrival(
                        id = rs.getInt("id"),
                        departureTimes = times.map { it.toLocalTime().format(timeFormatter) },
                        departuresId = rs.getInt("departures_id")
                    )
                } else null
            }
        }
    }

    override fun getArrivalsForDeparture(departureId: Int): Arrival? {
        val query = "SELECT * FROM arrivals WHERE departures_id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, departureId)
                val rs = stmt.executeQuery()
                return if (rs.next()) {
                    val times = rs.getArray("departure_time").array as Array<Time>
                    Arrival(
                        id = rs.getInt("id"),
                        departureTimes = times.map { it.toLocalTime().format(timeFormatter) },
                        departuresId = rs.getInt("departures_id")
                    )
                } else null
            }
        }
    }

    override fun getAll(): List<Arrival> {
        val query = "SELECT * FROM arrivals"
        val arrivals = mutableListOf<Arrival>()

        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val times = rs.getArray("departure_time").array as Array<Time>
                    arrivals.add(
                        Arrival(
                            id = rs.getInt("id"),
                            departureTimes = times.map { it.toLocalTime().format(timeFormatter) },
                            departuresId = rs.getInt("departures_id")
                        )
                    )
                }
            }
        }
        return arrivals
    }

    override fun insert(entity: Arrival): Boolean {
        val query = "INSERT INTO arrivals (departure_time, departures_id) VALUES (?, ?)"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                val timeArray = entity.departureTimes
                    .map { LocalTime.parse(it, timeFormatter) }
                    .map { Time.valueOf(it) }
                    .toTypedArray()
                val pgArray = conn.createArrayOf("time", timeArray)
                stmt.setArray(1, pgArray)
                stmt.setInt(2, entity.departuresId)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun update(entity: Arrival): Boolean {
        val query = "UPDATE arrivals SET departure_time = ? WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                val timeArray = entity.departureTimes
                    .map { LocalTime.parse(it, timeFormatter) }
                    .map { Time.valueOf(it) }
                    .toTypedArray()
                val pgArray = conn.createArrayOf("time", timeArray)
                stmt.setArray(1, pgArray)
                stmt.setInt(2, entity.id!!)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun delete(id: Int): Boolean {
        val query = "DELETE FROM arrivals WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, id)
                return stmt.executeUpdate() > 0
            }
        }
    }
} 