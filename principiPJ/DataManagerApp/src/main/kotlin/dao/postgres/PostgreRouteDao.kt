package dao.postgres

import dao.RouteDao
import model.Route
import db.DatabaseConnector
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class PostgreRouteDao : RouteDao {

    override fun getById(id: Int): Route? {
        val query = "SELECT id, name, path, line_id FROM routes WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, id)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    val pathString = rs.getString("path")
                    val jsonPath = Json.parseToJsonElement(pathString)
                    return Route(
                        id = rs.getInt("id"),
                        name = rs.getString("name"),
                        path = jsonPath,
                        lineId = rs.getInt("line_id")
                    )
                }
            }
        }
        return null
    }

    override fun getAll(): List<Route> {
        val query = "SELECT * FROM routes"
        val routes = mutableListOf<Route>()

        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val pathString = rs.getString("path")
                    val jsonPath = Json.parseToJsonElement(pathString)
                    routes.add(
                        Route(
                            id = rs.getInt("id"),
                            name = rs.getString("name"),
                            path = jsonPath,
                            lineId = rs.getInt("line_id")
                        )
                    )
                }
            }
        }
        return  routes
    }

    override fun insert(entity: Route): Boolean {
        val query = """
            INSERT INTO routes (name, path, line_id)
            VALUES (?, ?::jsonb, ?)
        """
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setString(1, entity.name)
                stmt.setString(2, Json.encodeToString(JsonElement.serializer(), entity.path))
                stmt.setInt(3, entity.lineId)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun update(entity: Route): Boolean {
        val query = """
            UPDATE routes SET name = ?, path = ?::jsonb, line_id = ?
            WHERE id = ?
        """
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setString(1, entity.name)
                stmt.setString(2, Json.encodeToString(JsonElement.serializer(), entity.path))
                stmt.setInt(3, entity.lineId)
                stmt.setInt(4, entity.id!!)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun delete(id: Int): Boolean {
        val query = "DELETE FROM routes WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, id)
                return stmt.executeUpdate() > 0
            }
        }
    }
}