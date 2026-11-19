package br.edu.uniceplac.modapraia.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import br.edu.uniceplac.modapraia.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Estoque local persistente; cada gravação de saldo e histórico é atômica. */
class LocalInventoryRepository(context: Context) : InventoryRepository {
    internal val database = object : SQLiteOpenHelper(context.applicationContext, "inventory.db", null, 2) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE products (id TEXT PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL, size TEXT NOT NULL, color TEXT NOT NULL, sku TEXT NOT NULL UNIQUE, quantity INTEGER NOT NULL CHECK(quantity >= 0), minimum INTEGER NOT NULL CHECK(minimum >= 0), price REAL NOT NULL CHECK(price > 0))")
            db.execSQL("CREATE TABLE movements (id TEXT PRIMARY KEY, variant TEXT NOT NULL REFERENCES products(id), label TEXT NOT NULL, type TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity > 0), occurred TEXT NOT NULL, note TEXT NOT NULL)")
            createCommerceTables(db)
        }
        override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) createCommerceTables(db)
        }
    }.writableDatabase
    private val productState = MutableStateFlow<List<ProductVariant>>(emptyList())
    private val movementState = MutableStateFlow<List<StockMovement>>(emptyList())
    override val products = productState.asStateFlow()
    override val movements = movementState.asStateFlow()
    init { refresh() }

    @Synchronized
    override fun addProduct(product: NewProduct): Result<Unit> = runCatching {
        require(listOf(product.productName, product.category, product.size, product.color, product.sku).none { it.isBlank() }) { "Preencha todos os dados do produto." }
        require(product.salePrice.isFinite() && product.salePrice > 0) { "Informe um preço maior que zero." }
        require(product.initialQuantity >= 0 && product.minimumQuantity >= 0) { "As quantidades não podem ser negativas." }
        val sku = product.sku.trim().uppercase()
        require(productState.value.none { it.sku == sku }) { "Já existe um produto com este SKU." }
        val id = UUID.randomUUID().toString()
        transaction {
            database.execSQL("INSERT INTO products VALUES (?,?,?,?,?,?,?,?,?)", arrayOf(id, product.productName.trim(), product.category.trim(), product.size.trim(), product.color.trim(), sku, product.initialQuantity, product.minimumQuantity, product.salePrice))
            if (product.initialQuantity > 0) insertMovement(id, "${product.productName.trim()} · ${product.size.trim()} · ${product.color.trim()}", MovementType.ENTRY, product.initialQuantity, "Estoque inicial")
        }
        refresh()
    }

    @Synchronized
    override fun registerMovement(variantId: String, type: MovementType, quantity: Int, note: String): Result<Unit> = runCatching {
        require(quantity > 0) { "Informe uma quantidade maior que zero." }
        val product = productState.value.firstOrNull { it.id == variantId } ?: error("Produto não encontrado.")
        val saldo = product.quantity.toLong() + if (type == MovementType.ENTRY) quantity.toLong() else -quantity.toLong()
        require(saldo in 0..Int.MAX_VALUE.toLong()) { "A movimentação ultrapassa os limites do estoque." }
        transaction {
            database.execSQL("UPDATE products SET quantity = ? WHERE id = ?", arrayOf(saldo, variantId))
            insertMovement(variantId, "${product.productName} · ${product.size} · ${product.color}", type, quantity, note.ifBlank { "Movimentação registrada no aplicativo" })
        }
        refresh()
    }

    private fun insertMovement(id: String, label: String, type: MovementType, quantity: Int, note: String) {
        database.execSQL("INSERT INTO movements VALUES (?,?,?,?,?,?,?)", arrayOf(UUID.randomUUID().toString(), id, label, type.name, quantity, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")), note))
    }
    internal fun transaction(block: () -> Unit) {
        database.beginTransaction()
        try { block(); database.setTransactionSuccessful() } finally { database.endTransaction() }
    }
    internal fun refresh() {
        productState.value = database.rawQuery("SELECT * FROM products ORDER BY rowid DESC", null).use { c ->
            buildList { while(c.moveToNext()) add(ProductVariant(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getInt(6), c.getInt(7), c.getDouble(8))) }
        }
        movementState.value = database.rawQuery("SELECT * FROM movements ORDER BY rowid DESC", null).use { c ->
            buildList { while(c.moveToNext()) add(StockMovement(c.getString(0), c.getString(1), c.getString(2), MovementType.valueOf(c.getString(3)), c.getInt(4), c.getString(5), c.getString(6))) }
        }
    }
}
