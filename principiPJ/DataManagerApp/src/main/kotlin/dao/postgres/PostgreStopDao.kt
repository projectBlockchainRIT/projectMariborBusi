package dao.postgres

import dao.StopDao
import model.Stop
import db.DatabaseConnector

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