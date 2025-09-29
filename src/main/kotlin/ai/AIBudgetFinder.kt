package ai

import sql.BudgetDividers
import sql.Transaction
import sql.Transactions
import java.time.LocalDate

class AIBudgetFinder(
    val cardNumber: String,
    val startTime: LocalDate,
    val endTime: LocalDate,
) {

    /**
     * Executes the process of fetching transactions for a specific card within a date range,
     * associates them with the corresponding budget dividers, and persists updated transactions.
     *
     * The method retrieves transactions by card number and date range, identifies associated
     * budget dividers, processes and maps them to updated transaction instances, and finally
     * persists the modified transactions.
     *
     * @return a list of updated transactions with their associated budget divider information.
     */
    fun execute(): List<Transaction> {
        val transactions = Transactions.TransactionRepository.transactionsByCardNumberAndDateRange(cardNumber, startTime, endTime)

        val budgetIds = transactions.mapNotNull { it.budgetDividerId }.distinct()

        val budgets = BudgetDividers.BudgetRepository.getAllBudgetDividersFromIDs(budgetIds)

        val promptResponse = ""

        if (promptResponse.trim().isEmpty()) {
            return emptyList()
        }

        val newBudgetType = promptResponse.split(",").map {
            it.split(":")[0].trim() to it.split(":")[1].trim()
        }

        val budgetMap = budgets.associateBy { it.id }

        val newTransactions = transactions.map {
            val budgetDividerId = it.budgetDividerId
            if (budgetDividerId != null) {
                val associatedBudget = budgetMap[budgetDividerId]
                if (associatedBudget != null) {
                    it.copy(budgetDividerId = associatedBudget.id)
                } else {
                    it
                }
            } else {
                it
            }
        }

        Transactions.TransactionRepository.persist(newTransactions)
        return newTransactions
    }
}
