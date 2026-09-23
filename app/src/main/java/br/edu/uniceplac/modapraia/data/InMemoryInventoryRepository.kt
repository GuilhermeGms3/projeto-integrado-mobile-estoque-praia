package br.edu.uniceplac.modapraia.data

import br.edu.uniceplac.modapraia.domain.MovementType
import br.edu.uniceplac.modapraia.domain.NewProduct
import br.edu.uniceplac.modapraia.domain.ProductVariant
import br.edu.uniceplac.modapraia.domain.StockMovement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Dados de demonstração. A troca pela API REST ocorrerá atrás desta interface. */
class InMemoryInventoryRepository : InventoryRepository {
    private val _products = MutableStateFlow(
        listOf(
            ProductVariant("1", "Biquíni Aurora", "Biquínis", "M", "Verde", "BIA-VER-M", 3, 4, 159.90),
            ProductVariant("2", "Biquíni Aurora", "Biquínis", "G", "Verde", "BIA-VER-G", 8, 4, 159.90),
            ProductVariant("3", "Maiô Sol", "Maiôs", "P", "Preto", "MAS-PRE-P", 2, 3, 189.90),
            ProductVariant("4", "Saída Brisa", "Saídas", "Único", "Off-white", "SAB-OFF-U", 12, 3, 219.90),
            ProductVariant("5", "Canga Mar", "Acessórios", "Único", "Azul", "CAM-AZU-U", 5, 3, 79.90)
        )
    )
    private val _movements = MutableStateFlow(
        listOf(
            StockMovement("m1", "1", "Biquíni Aurora · M · Verde", MovementType.SALE, 1, "20/08 10:30", "Venda presencial"),
            StockMovement("m2", "4", "Saída Brisa · Único · Off-white", MovementType.ENTRY, 6, "19/08 15:10", "Reposição de fornecedor")
        )
    )

    override val products: StateFlow<List<ProductVariant>> = _products.asStateFlow()
    override val movements: StateFlow<List<StockMovement>> = _movements.asStateFlow()

    override fun addProduct(product: NewProduct): Result<Unit> {
        val productName = product.productName.trim()
        val category = product.category.trim()
        val size = product.size.trim()
        val color = product.color.trim()
        val sku = product.sku.trim().uppercase()

        if (listOf(productName, category, size, color, sku).any(String::isBlank)) {
            return Result.failure(IllegalArgumentException("Preencha todos os dados do produto."))
        }
        if (product.salePrice <= 0) {
            return Result.failure(IllegalArgumentException("Informe um preço maior que zero."))
        }
        if (product.initialQuantity < 0 || product.minimumQuantity < 0) {
            return Result.failure(IllegalArgumentException("As quantidades não podem ser negativas."))
        }
        if (_products.value.any { it.sku.equals(sku, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Já existe um produto com este SKU."))
        }

        val variant = ProductVariant(
            id = UUID.randomUUID().toString(),
            productName = productName,
            category = category,
            size = size,
            color = color,
            sku = sku,
            quantity = product.initialQuantity,
            minimumQuantity = product.minimumQuantity,
            salePrice = product.salePrice
        )
        _products.value = listOf(variant) + _products.value

        if (product.initialQuantity > 0) {
            _movements.value = listOf(
                StockMovement(
                    id = UUID.randomUUID().toString(),
                    variantId = variant.id,
                    productLabel = "${variant.productName} · ${variant.size} · ${variant.color}",
                    type = MovementType.ENTRY,
                    quantity = product.initialQuantity,
                    occurredAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")),
                    note = "Estoque inicial"
                )
            ) + _movements.value
        }
        return Result.success(Unit)
    }

    override fun registerMovement(variantId: String, type: MovementType, quantity: Int, note: String): Result<Unit> {
        if (quantity <= 0) return Result.failure(IllegalArgumentException("Informe uma quantidade maior que zero."))
        val variant = _products.value.firstOrNull { it.id == variantId }
            ?: return Result.failure(IllegalArgumentException("Produto não encontrado."))
        val delta = if (type == MovementType.ENTRY) quantity else -quantity
        if (variant.quantity + delta < 0) return Result.failure(IllegalArgumentException("A saída não pode deixar o estoque negativo."))

        _products.value = _products.value.map {
            if (it.id == variantId) it.copy(quantity = it.quantity + delta) else it
        }
        val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
        _movements.value = listOf(
            StockMovement(
                id = UUID.randomUUID().toString(),
                variantId = variantId,
                productLabel = "${variant.productName} · ${variant.size} · ${variant.color}",
                type = type,
                quantity = quantity,
                occurredAt = LocalDateTime.now().format(formatter),
                note = note.ifBlank { "Movimentação registrada no aplicativo" }
            )
        ) + _movements.value
        return Result.success(Unit)
    }
}
