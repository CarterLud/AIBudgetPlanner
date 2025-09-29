package sql

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class BudgetDividers {
    object BudgetDivider: IntIdTable() {
        val name = varchar("name", 255)
        val description = varchar("description", 255)
        val maxBudget = integer("max_budget")
    }

    object BudgetRepository {
        fun persist(divider: Divider): Int = transaction {
            transaction {
                // one reusable body: extension on the table
                val assign: BudgetDivider.(UpdateBuilder<*>) -> Unit = {
                    it[name] = divider.name
                    it[description] = divider.description ?: ""
                    it[maxBudget] = divider.maxBudget
                }

                if (divider.id == 0) {
                    // Insert expects InsertStatement<Number>; wrap and reuse `assign`
                    return@transaction BudgetDivider.insertAndGetId { stmt ->
                        assign(this, stmt) // `stmt` is a subtype of UpdateBuilder<*>
                    }.value
                }

                // Update expects UpdateBuilder<*>
                val updated = BudgetDivider.update({ BudgetDivider.id eq divider.id }, limit = 1) { ub ->
                    assign(this, ub)
                }
                if (updated == 0) error("No divider found with id=${divider.id}")
                divider.id
            }
        }

        fun getAllBudgetDividers(): List<Divider> = transaction {
            BudgetDivider
                .selectAll()
                .map {
                    Divider(
                        id = it[BudgetDivider.id].value,
                        name = it[BudgetDivider.name],
                        description = it[BudgetDivider.description],
                        maxBudget = it[BudgetDivider.maxBudget]
                    )
                }
        }

        fun getAllBudgetDividersFromIDs(ids: List<Int>): List<Divider> = transaction {
            BudgetDivider
                .selectAll()
                .where { BudgetDivider.id inList ids }
                .map {
                    Divider(
                        id = it[BudgetDivider.id].value,
                        name = it[BudgetDivider.name],
                        description = it[BudgetDivider.description],
                        maxBudget = it[BudgetDivider.maxBudget]
                    )
                }
        }

        fun getBudgetDividerById(id: Int): Divider = transaction {
            BudgetDivider
                .selectAll()
                .where(BudgetDivider.id eq id)
                .limit(1)
                .map {
                    Divider(
                        id = it[BudgetDivider.id].value,
                        name = it[BudgetDivider.name],
                        description = it[BudgetDivider.description],
                        maxBudget = it[BudgetDivider.maxBudget]
                    )
                }.firstOrNull() ?: error("No budget divider found with id=$id")
        }

        fun getBudgetDividerIdByName(name: String): Int? = transaction {
            BudgetDivider
                .selectAll()
                .where(BudgetDivider.name eq name)
                .limit(1)
                .map {
                    it[BudgetDivider.id].value
                }.firstOrNull()
        }
    }
}

@Serializable
data class Divider(
    val id: Int = 0,
    val name: String,
    val description: String? = null,
    val maxBudget: Int
)
