package Storage

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insertAndGetId
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class Transactions {
    object Transactions : IntIdTable() {
        val cardNumber = varchar("card_number", 4)
        val startMonth = varchar("start_month", 3)
        val startDay = integer("start_day")
        val endMonth = varchar("end_month", 3)
        val endDay = integer("end_day")
        val amount = varchar("amount", 20)
        val vendor = varchar("vendor", 255)
    }

    object TransactionRepository {
        fun addTransaction(transaction: Transaction): Int = transaction {
            Transactions.insertAndGetId {
                it[Transactions.cardNumber] = transaction.cardNumber
                it[Transactions.startMonth] = transaction.startMonth
                it[Transactions.startDay] = transaction.startDay.toInt()
                it[Transactions.endMonth] = transaction.endMonth
                it[Transactions.endDay] = transaction.endDay.toInt()
                it[Transactions.amount] = transaction.amount
                it[Transactions.vendor] = transaction.vendor
            }.value
        }

        fun getAllTransactions(): List<Transaction> = transaction {
            Transactions
                .selectAll()
                .map {
                    Transaction(
                        cardNumber = it[Transactions.cardNumber],
                        startMonth = it[Transactions.startMonth],
                        startDay = it[Transactions.startDay].toString(),
                        endMonth = it[Transactions.endMonth],
                        endDay = it[Transactions.endDay].toString(),
                        amount = it[Transactions.amount],
                        vendor = it[Transactions.vendor]
                    )
            }
        }

        fun getTransactionsByCardNumber(cardNumber: String): List<Transaction> = transaction {
            Transactions
                .selectAll()
                .where { Transactions.cardNumber eq cardNumber }
                .map {
                    Transaction(
                        cardNumber = it[Transactions.cardNumber],
                        startMonth = it[Transactions.startMonth],
                        startDay = it[Transactions.startDay].toString(),
                        endMonth = it[Transactions.endMonth],
                        endDay = it[Transactions.endDay].toString(),
                        amount = it[Transactions.amount],
                        vendor = it[Transactions.vendor]
                    )
                }
        }

        fun updateTransaction(id: Int, transaction: Transaction) = transaction {
            Transactions.update({ Transactions.id eq id }) {
                it[Transactions.cardNumber] = transaction.cardNumber
                it[Transactions.startMonth] = transaction.startMonth
                it[Transactions.startDay] = transaction.startDay.toInt()
                it[Transactions.endMonth] = transaction.endMonth
                it[Transactions.endDay] = transaction.endDay.toInt()
                it[Transactions.amount] = transaction.amount
                it[Transactions.vendor] = transaction.vendor
            }
        }
    }
}

@Serializable
data class Transaction(
    val cardNumber: String,
    val startMonth: String,
    val startDay: String,
    val endMonth: String,
    val endDay: String,
    val amount: String,
    val vendor: String
)

@Serializable
data class TransactionDTO(
    val transaction: Map<String, List<Transaction>>
)