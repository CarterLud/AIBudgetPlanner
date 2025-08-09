package InMemory

data class BudgetDividers(
    val name: String,
    val amount: Double = 0.0,
    val description: String? = null
)

val budgetDividers = mutableListOf<BudgetDividers>()

class CreateBudgetDividers() {
    fun execute(name: String, amount: Double, description: String?): BudgetDividers {
        val budgetDivider = BudgetDividers(
            name = name,
            amount = amount,
            description = description
        )
        budgetDividers.add(budgetDivider)
        return budgetDivider
    }
}