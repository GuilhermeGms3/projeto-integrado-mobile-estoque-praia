package br.edu.uniceplac.modapraia.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.edu.uniceplac.modapraia.data.InMemoryInventoryRepository
import br.edu.uniceplac.modapraia.data.InventoryRepository
import br.edu.uniceplac.modapraia.domain.MovementType
import br.edu.uniceplac.modapraia.domain.NewProduct
import br.edu.uniceplac.modapraia.domain.ProductVariant
import br.edu.uniceplac.modapraia.domain.StockMovement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class InventoryUiState(
    val products: List<ProductVariant> = emptyList(),
    val movements: List<StockMovement> = emptyList(),
    val search: String = "",
    val message: String? = null
) {
    val filteredProducts: List<ProductVariant>
        get() = products.filter {
            "${it.productName} ${it.category} ${it.size} ${it.color} ${it.sku}".contains(search, ignoreCase = true)
        }
    val lowStock: List<ProductVariant> get() = products.filter { it.isLowStock }
    val totalUnits: Int get() = products.sumOf { it.quantity }
}

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {
    private val search = MutableStateFlow("")
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<InventoryUiState> = combine(repository.products, repository.movements, search, message) {
            products, movements, currentSearch, currentMessage ->
        InventoryUiState(products, movements, currentSearch, currentMessage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    fun updateSearch(value: String) { search.value = value }
    fun clearMessage() { message.value = null }

    fun addProduct(
        productName: String,
        category: String,
        size: String,
        color: String,
        sku: String,
        priceText: String,
        initialQuantityText: String,
        minimumQuantityText: String
    ): String? {
        val price = priceText.replace(',', '.').toDoubleOrNull()
            ?: return "Informe um preço válido."
        val initialQuantity = initialQuantityText.toIntOrNull()
            ?: return "Informe o estoque inicial."
        val minimumQuantity = minimumQuantityText.toIntOrNull()
            ?: return "Informe o estoque mínimo."

        return repository.addProduct(
            NewProduct(
                productName = productName,
                category = category,
                size = size,
                color = color,
                sku = sku,
                salePrice = price,
                initialQuantity = initialQuantity,
                minimumQuantity = minimumQuantity
            )
        ).fold(
            onSuccess = {
                message.value = "Produto cadastrado com sucesso."
                null
            },
            onFailure = { it.message ?: "Não foi possível cadastrar o produto." }
        )
    }

    fun registerMovement(variantId: String, type: MovementType, quantityText: String, note: String) {
        val quantity = quantityText.toIntOrNull()
        if (quantity == null) {
            message.value = "Informe uma quantidade inteira válida."
            return
        }
        repository.registerMovement(variantId, type, quantity, note)
            .onSuccess { message.value = "${type.label} registrada com sucesso." }
            .onFailure { message.value = it.message ?: "Não foi possível registrar a movimentação." }
    }

    companion object {
        fun factory(repository: InventoryRepository = InMemoryInventoryRepository()) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = InventoryViewModel(repository) as T
            }
    }
}
