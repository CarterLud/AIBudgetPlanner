package Handler

import sql.Divider
import sql.BudgetDividers.BudgetRepository

class BudgetTypeHandler {

    /**
     * Creates or updates a budget divider with the specified parameters.
     *
     * This method attempts to find an existing budget divider by name. If found, it updates
     * the existing divider; otherwise, it creates a new one. The divider is then persisted
     * to the repository.
     *
     * @param budgetType The name of the budget divider to create or update
     * @param description Optional description for the budget divider
     * @param maxBudget Optional maximum budget amount for this divider, defaults to 0 if not provided
     * @return true if the operation was successful, false if an exception occurred during persistence
     */
    fun execute(budgetType: String, description: String?, maxBudget: Int?): Boolean {
        println("Received request to create a new budget divider.")
        val dividerId = BudgetRepository
            .getBudgetDividerIdByName(budgetType)

        val divider = Divider(
            id = dividerId ?: 0,
            name = budgetType,
            description = description,
            maxBudget = maxBudget ?: 0
        )

        try {
            BudgetRepository.persist(divider)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
