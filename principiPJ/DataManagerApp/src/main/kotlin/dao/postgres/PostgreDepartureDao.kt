package dao.postgres

import dao.DepartureDao
import model.Departure
import db.DatabaseConnector
import java.sql.Date
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PostgreDepartureDao : DepartureDao {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun getById(id: Int): Departure? {
        val query = "SELECT * FROM departures WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, id)
                val rs = stmt.executeQuery()
                return if (rs.next()) {
                    Departure(
                        id = rs.getInt("id"),
                        stopId = rs.getInt("stop_id"),
                        directionId = rs.getInt("direction_id"),
                        date = rs.getDate("date").toLocalDate().format(dateFormatter)
                    )
                } else null
            }
        }
    }

    override fun getDeparturesForDirection(directionId: Int): List<Departure> {
        val departures = mutableListOf<Departure>()
        val query = "SELECT id, stop_id, direction_id, date FROM departures WHERE direction_id = ?"

        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, directionId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        departures.add(
                            Departure(
                                id = rs.getInt("id"),
                                stopId = rs.getInt("stop_id"),
                                directionId = rs.getInt("direction_id"),
                                date = rs.getDate("date").toLocalDate().format(dateFormatter)
                            )
                        )
                    }
                }
            }
        }
        return departures
    }

    override fun deleteAllForDirection(directionId: Int): Boolean {
        val query = "DELETE FROM departures WHERE direction_id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, directionId)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun deleteAllForStop(stopId: Int): Boolean {
        val query = "DELETE FROM departures WHERE stop_id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, stopId)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun getAll(): List<Departure> {
        val query = "SELECT * FROM departures"
        val departures = mutableListOf<Departure>()

        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    departures.add(
                        Departure(
                            id = rs.getInt("id"),
                            stopId = rs.getInt("stop_id"),
                            directionId = rs.getInt("direction_id"),
                            date = rs.getDate("date").toLocalDate().format(dateFormatter)
                        )
                    )
                }
            }
        }
        return departures
    }

    override fun insert(entity: Departure): Boolean {
        val query = "INSERT INTO departures (stop_id, direction_id, date) VALUES (?, ?, ?)"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, entity.stopId)
                stmt.setInt(2, entity.directionId)
                stmt.setDate(3, Date.valueOf(LocalDate.parse(entity.date, dateFormatter)))
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun update(entity: Departure): Boolean {
        val query = "UPDATE departures SET stop_id = ?, direction_id = ?, date = ? WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, entity.stopId)
                stmt.setInt(2, entity.directionId)
                stmt.setDate(3, Date.valueOf(LocalDate.parse(entity.date, dateFormatter)))
                stmt.setInt(4, entity.id!!)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun delete(id: Int): Boolean {
        val query = "DELETE FROM departures WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, id)
                return stmt.executeUpdate() > 0
            }
        }
    }
}
