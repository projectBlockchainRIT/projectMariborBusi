package dao.postgres

import dao.UserDao
import model.User
import java.sql.*
import db.DatabaseConnector
import java.time.OffsetDateTime

class PostgreUserDao : UserDao {

    override fun getById(id: Int): User? {
        val query = "SELECT * FROM users WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        User(
                            id = rs.getInt("id"),
                            username = rs.getString("username"),
                            email = rs.getString("email"),
                            password = rs.getString("password"),
                            createdAt = rs.getString("created_at"),
                            lastLogin = rs.getString("last_login")
                        )
                    } else null
                }
            }
        }
    }

    override fun insert(entity: User): Boolean {
        val query = """
            INSERT INTO users (username, email, password, created_at, last_login)
            VALUES (?, ?, ?, ?, ?)
        """
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setString(1, entity.username)
                stmt.setString(2, entity.email)
                stmt.setString(3, entity.password)
                stmt.setObject(4, OffsetDateTime.parse(entity.createdAt))
                if (entity.lastLogin != null)
                    stmt.setString(5, entity.lastLogin)
                else stmt.setNull(5, Types.TIMESTAMP)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun update(entity: User): Boolean {
        val query = """
            UPDATE users SET username = ?, email = ?, password = ?, last_login = ?
            WHERE id = ?
        """
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setString(1, entity.username)
                stmt.setString(2, entity.email)
                stmt.setString(3, entity.password)
                if (entity.lastLogin != null)
                    stmt.setString(4, entity.lastLogin)
                else stmt.setNull(4, Types.TIMESTAMP)
                stmt.setInt(5, entity.id!!)
                return stmt.executeUpdate() > 0
            }
        }
    }

    override fun delete(id: Int): Boolean {
        val query = "DELETE FROM users WHERE id = ?"
        DatabaseConnector.getConnection().use { conn ->
            conn!!.prepareStatement(query).use { stmt ->
                stmt.setInt(1, id)
                return stmt.executeUpdate() > 0
            }
        }
    }
}