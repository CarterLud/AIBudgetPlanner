package DBHandler

import Storage.Transactions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


class DBConn {
    fun connect(usePostgres: Boolean = false) {
        if (usePostgres) {
            Database.connect(
                url = "jdbc:postgresql://localhost:5432/budgetdb",
                driver = "org.postgresql.Driver",
                user = System.getenv("DB_USER"),
                password = System.getenv("DB_PASSWORD")
            )
        } else {
            Database.connect(
                url = "jdbc:sqlite:budget.db",
                driver = "org.sqlite.JDBC"
            )
        }
    }

    fun generateSchema() {

    }
}