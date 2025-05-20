package db

fun main() {
    val conn = DatabaseConnector.getConnection()
    if (conn != null) {
        println("Povezava ok!")
        conn.close()
    }
}
