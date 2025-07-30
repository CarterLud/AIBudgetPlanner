package Storage

import org.jetbrains.exposed.sql.Table

object Transactions {
    object Transactions : Table() {
        val id = integer("id").autoIncrement()
        val startMonth = varchar("start_month", 3)
        val startDay = integer("start_day")
        val endMonth = varchar("end_month", 3)
        val endDay = integer("end_day")
        val amount = varchar("amount", 20)
        val vendor = varchar("vendor", 255)
        override val primaryKey = PrimaryKey(id)
    }
}