package dao.postgres

import dao.DepartureDao
import model.Departure
import db.DatabaseConnector
import java.sql.Time
import java.time.LocalTime

class PostgreDepartureDao : DepartureDao {
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
                        departure = rs.getString("departure")
                    )
                } else null
            }
        }
    }

    override fun insert(entity: Departure): Boolean {
        val query = "INSERT INTO departures (stop_id, direction_id, departure) VALUES (?, ?, ?)"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, entity.stopId)
                stmt.setInt(2, entity.directionId)
                stmt.setTime(3, Time.valueOf(LocalTime.parse(entity.departure)))
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun update(entity: Departure): Boolean {
        val query = "UPDATE departures SET stop_id = ?, direction_id = ?, departure = ? WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, entity.stopId)
                stmt.setInt(2, entity.directionId)
                stmt.setString(3, entity.departure)
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
