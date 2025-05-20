package dao.postgres

import dao.DirectionDao
import db.DatabaseConnector
import model.Direction

class PostgreDirectionDao : DirectionDao {

    override fun getById(id: Int): Direction? {
        val query = "SELECT id, line_id, name FROM directions WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, id)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return Direction(
                        id = rs.getInt("id"),
                        lineId = rs.getInt("line_id"),
                        name = rs.getString("name")
                    )
                }
            }
        }
        return null
    }

    override fun insert(entity: Direction): Boolean {
        val query = "INSERT INTO directions (line_id, name) VALUES (?, ?)"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, entity.lineId)
                stmt.setString(2, entity.name)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun update(entity: Direction): Boolean {
        val query = "UPDATE directions SET line_id = ?, name = ? WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, entity.lineId)
                stmt.setString(2, entity.name)
                stmt.setInt(3, entity.id!!)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun delete(id: Int): Boolean {
        val query = "DELETE FROM directions WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, id)
                return stmt.executeUpdate() > 0
            }
        }
    }
}
