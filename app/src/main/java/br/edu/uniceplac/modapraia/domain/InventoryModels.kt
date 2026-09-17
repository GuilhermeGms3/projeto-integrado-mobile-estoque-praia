package br.edu.uniceplac.modapraia.domain

enum class MovementType(val label: String) {
    ENTRY("Entrada"),
    SALE("Saída por venda"),
    ADJUSTMENT("Ajuste")
}

data class ProductVariant(
    val id: String,
    val productName: String,
    val category: String,
    val size: String,
    val color: String,
    val sku: String,
    val quantity: Int,
    val minimumQuantity: Int,
    val salePrice: Double
) {
    val isLowStock: Boolean get() = quantity <= minimumQuantity
}

data class StockMovement(
    val id: String,
    val variantId: String,
    val productLabel: String,
    val type: MovementType,
    val quantity: Int,
    val occurredAt: String,
    val note: String
)
