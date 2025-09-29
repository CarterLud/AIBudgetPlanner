package DBHandler

import sql.Transactions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import sql.BudgetDividers


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
        generateSchema()
    }

    fun generateSchema() {
        transaction {
            SchemaUtils.create(Transactions.Transactions)
            SchemaUtils.create(BudgetDividers.BudgetDivider)
        }
    }
}