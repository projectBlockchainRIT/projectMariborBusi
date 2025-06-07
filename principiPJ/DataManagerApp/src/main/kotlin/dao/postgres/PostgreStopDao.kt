package dao.postgres

import dao.StopDao
import model.Stop
import db.DatabaseConnector
import model.Departure

class PostgreStopDao : StopDao {

    override fun getById(id: Int): Stop? {
        val query = "SELECT id, number, name, latitude, longitude FROM stops WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, id)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return Stop(
                        id = rs.getInt("id"),
                        number = rs.getString("number"),
                        name = rs.getString("name"),
                        latitude = rs.getDouble("latitude"),
                        longitude = rs.getDouble("longitude")
                    )
                }
            }
        }
        return null
    }

    override fun getDeparturesForStop(stopId: Int): List<Departure> {
        val departures = mutableListOf<Departure>()
        val query = "SELECT id, stop_id, direction_id, departure FROM departures WHERE stop_id = ?"

        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, stopId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        departures.add(
                            Departure(
                                id = rs.getInt("id"),
                                stopId = rs.getInt("stop_id"),
                                directionId = rs.getInt("direction_id"),
                                departure = rs.getString("departure") // pričakovan format: "HH:mm:ss"
                            )
                        )
                    }
                }
            }
        }
        return departures
    }

    override fun getAll(): List<Stop> {
        val query = "SELECT * FROM stops"
        val stops = mutableListOf<Stop>()

        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    stops.add(
                        Stop(
                            id = rs.getInt("id"),
                            number = rs.getString("number"),
                            name = rs.getString("name"),
                            latitude = rs.getDouble("latitude"),
                            longitude = rs.getDouble("longitude")
                        )
                    )
                }
            }
        }
        return stops
    }

    override fun insert(entity: Stop): Boolean {
        val query = """
            INSERT INTO stops (id, number, name, latitude, longitude)
            VALUES (?, ?, ?, ?, ?)
        """
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, entity.id)
                stmt.setString(2, entity.number)
                stmt.setString(3, entity.name)
                stmt.setDouble(4, entity.latitude)
                stmt.setDouble(5, entity.longitude)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun update(entity: Stop): Boolean {
        val query = """
            UPDATE stops SET number = ?, name = ?, latitude = ?, longitude = ?
            WHERE id = ?
        """
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setString(1, entity.number)
                stmt.setString(2, entity.name)
                stmt.setDouble(3, entity.latitude)
                stmt.setDouble(4, entity.longitude)
                stmt.setInt(5, entity.id!!)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun delete(id: Int): Boolean {
        val query = "DELETE FROM stops WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, id)
                return stmt.executeUpdate() > 0
            }
        }
    }
}