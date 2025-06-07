package dao.postgres

import dao.LineDao
import db.DatabaseConnector
import model.Line

class PostgreLineDao : LineDao {

    override fun getById(id: Int): Line? {
        val query = "SELECT * FROM lines WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        Line(
                            id = rs.getInt("id"),
                            lineCode = rs.getString("line_code")
                        )
                    } else null
                }
            }
        }
    }

    override fun getAll(): List<Line> {
        val query = "SELECT * FROM lines"
        val lines = mutableListOf<Line>()

        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    lines.add(
                        Line(
                            id = rs.getInt("id"),
                            lineCode = rs.getString("line_code")
                        )
                    )
                }
            }
        }
        return lines
    }

    override fun insert(entity: Line): Boolean {
        val query = "INSERT INTO lines (line_code) VALUES (?)"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setString(1, entity.lineCode)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun update(entity: Line): Boolean {
        val query = "UPDATE lines SET line_code = ? WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setString(1, entity.lineCode)
                stmt.setInt(2, entity.id!!)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun delete(id: Int): Boolean {
        val query = "DELETE FROM lines WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, id)
                return stmt.executeUpdate() > 0
            }
        }
    }
}