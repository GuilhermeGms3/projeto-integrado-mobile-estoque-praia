package br.edu.uniceplac.modapraia.data

import br.edu.uniceplac.modapraia.domain.MovementType
import br.edu.uniceplac.modapraia.domain.ProductVariant
import br.edu.uniceplac.modapraia.domain.StockMovement
import kotlinx.coroutines.flow.StateFlow

interface InventoryRepository {
    val products: StateFlow<List<ProductVariant>>
    val movements: StateFlow<List<StockMovement>>
    fun registerMovement(variantId: String, type: MovementType, quantity: Int, note: String): Result<Unit>
}
