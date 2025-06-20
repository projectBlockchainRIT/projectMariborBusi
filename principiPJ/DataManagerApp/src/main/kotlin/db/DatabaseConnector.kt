package db

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object DatabaseConnector {

    private const val url = "jdbc:postgresql://localhost:5432/bus_base"
    private const val user = "postgres"
    private const val password = "1234"

    init {
        try {
            Class.forName("org.postgresql.Driver")
        } catch (ex: ClassNotFoundException) {
            println("PostgreSQL JDBC Driver ni najden.")
            ex.printStackTrace()
        }
    }

    fun getConnection(): Connection? {
        return try {
            DriverManager.getConnection(url, user, password)
        } catch (ex: SQLException) {
            println("Povezava na bazo ni uspela.")
            ex.printStackTrace()
            null
        }
    }
}