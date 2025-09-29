@file:UseSerializers(LocalDateAsStringSerializer::class)
package sql

import kotlinx.serialization.UseSerializers
import kotlinx.serialization.KSerializer
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insertAndGetId
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import java.time.LocalDate
import org.jetbrains.exposed.sql.javatime.date
import java.time.format.DateTimeFormatter

class Transactions {
    object Transactions : IntIdTable() {
        val cardNumber = varchar("card_number", 4)
        val start = date("start")
        val end = date("end")
        val amount = varchar("amount", 20)
        val vendor = varchar("vendor", 255)
        val budgetDivider = reference(
            "budget_divider_id",
            BudgetDividers.BudgetDivider,
            onDelete = ReferenceOption.SET_NULL
        ).nullable()
    }

    object TransactionRepository {
        fun persist(t: Transaction): Int = transaction {
            // Common field assignments for insert/update
            val assign: Transactions.(UpdateBuilder<*>) -> Unit = {
                it[cardNumber] = t.cardNumber
                it[start] = t.start
                it[end] = t.end
                it[amount] = t.amount
                it[vendor] = t.vendor
                it[budgetDivider] = t.budgetDividerId?.let { id ->
                    EntityID(id, BudgetDividers.BudgetDivider)
                }
            }

            if (t.id == 0) {
                return@transaction Transactions.insertAndGetId { assign(it) }.value
            }

            val updated = Transactions.update({ Transactions.id eq t.id }) { assign(it) }
            if (updated == 0) error("No transaction found with id=${t.id}") // optional safety
            t.id
        }

        fun persist(t: List<Transaction>): List<Int> = transaction {
            t.map { persist(it) }
        }

        fun getAllTransactions(): List<Transaction> = transaction {
            Transactions
                .selectAll()
                .map {
                    Transaction(
                        id = it[Transactions.id].value,
                        cardNumber = it[Transactions.cardNumber],
                        start = it[Transactions.start],
                        end = it[Transactions.end],
                        amount = it[Transactions.amount],
                        vendor = it[Transactions.vendor],
                        budgetDividerId = it[Transactions.budgetDivider]?.value
                    )
            }
        }

        fun transactionsByCardNumber(cardNumber: String): List<Transaction> = transaction {
            Transactions
                .select( Transactions.cardNumber eq cardNumber )
                .map {
                    Transaction(
                        id = it[Transactions.id].value,
                        cardNumber = it[Transactions.cardNumber],
                        start = it[Transactions.start],
                        end = it[Transactions.end],
                        amount = it[Transactions.amount],
                        vendor = it[Transactions.vendor],
                        budgetDividerId = it[Transactions.budgetDivider]?.value
                    )
                }
        }

        fun transactionsByCardNumberAndDateRange(cardNumber: String, start: LocalDate, end: LocalDate): List<Transaction> = transaction {
            Transactions
                .select( (Transactions.cardNumber eq cardNumber) and (Transactions.start.between(start, end)) and (Transactions.end.between(start, end)) )
                .map {
                    Transaction(
                        id = it[Transactions.id].value,
                        cardNumber = it[Transactions.cardNumber],
                        start = it[Transactions.start],
                        end = it[Transactions.end],
                        amount = it[Transactions.amount],
                        vendor = it[Transactions.vendor],
                        budgetDividerId = it[Transactions.budgetDivider]?.value
                    )
                }
        }
    }
}

@Serializable
data class Transaction(
    val id: Int = 0,
    val cardNumber: String,
    val start: LocalDate,
    val end: LocalDate,
    val amount: String,
    val vendor: String,
    val budgetDividerId: Int? = null
)

@Serializable
data class TransactionDTO(
    val transaction: Map<String, List<Transaction>>
)


object LocalDateAsStringSerializer : KSerializer<LocalDate> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE // "yyyy-MM-dd"

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString(), formatter)
    }
}
