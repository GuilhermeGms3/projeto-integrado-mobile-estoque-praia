package br.edu.uniceplac.modapraia.data

import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal fun createCommerceTables(db: SQLiteDatabase) {
    db.execSQL("CREATE TABLE sales (id TEXT PRIMARY KEY, product TEXT NOT NULL REFERENCES products(id), label TEXT NOT NULL, customer TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity>0), total INTEGER NOT NULL CHECK(total>0), occurred TEXT NOT NULL, canceled INTEGER NOT NULL DEFAULT 0 CHECK(canceled IN (0,1)))")
    db.execSQL("CREATE TABLE bills (id TEXT PRIMARY KEY, direction TEXT NOT NULL CHECK(direction IN ('PAY','RECEIVE')), description TEXT NOT NULL, person TEXT NOT NULL, total INTEGER NOT NULL CHECK(total>0), due TEXT NOT NULL, sale TEXT UNIQUE REFERENCES sales(id), canceled INTEGER NOT NULL DEFAULT 0 CHECK(canceled IN (0,1)))")
    db.execSQL("CREATE TABLE payments (id TEXT PRIMARY KEY, bill TEXT NOT NULL REFERENCES bills(id), amount INTEGER NOT NULL CHECK(amount>0), occurred TEXT NOT NULL)")
    db.execSQL("CREATE TABLE orders (id TEXT PRIMARY KEY, product TEXT NOT NULL REFERENCES products(id), label TEXT NOT NULL, customer TEXT NOT NULL, contact TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity>0), total INTEGER NOT NULL CHECK(total>0), due TEXT NOT NULL, note TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'Aberta' CHECK(status IN ('Aberta','Em preparação','Entregue','Cancelada')), sale TEXT UNIQUE REFERENCES sales(id))")
    db.execSQL("CREATE INDEX bills_due ON bills(direction,canceled,due)")
    db.execSQL("CREATE INDEX payments_bill ON payments(bill)")
    db.execSQL("CREATE INDEX orders_due ON orders(status,due)")
}

data class Sale(val id: String, val product: String, val label: String, val customer: String, val quantity: Int, val total: Long, val date: String, val canceled: Boolean)
data class Bill(val id: String, val direction: String, val description: String, val person: String, val total: Long, val due: String, val sale: String?, val canceled: Boolean, val paid: Long) {
    val remaining get() = total-paid
    val status get() = when { canceled -> "Cancelada"; remaining == 0L -> "Quitada"; due < LocalDate.now().toString() -> "Vencida"; paid > 0 -> "Parcial"; else -> "Em aberto" }
}
data class Payment(val description: String, val direction: String, val amount: Long, val date: String)
data class Order(val id: String, val product: String, val label: String, val customer: String, val contact: String, val quantity: Int, val total: Long, val due: String, val note: String, val status: String)
data class CommerceState(val sales: List<Sale> = emptyList(), val bills: List<Bill> = emptyList(), val payments: List<Payment> = emptyList(), val orders: List<Order> = emptyList())

/** Valores em centavos. Venda, baixa de estoque e conta são uma única transação. */
class CommerceRepository(private val inventory: LocalInventoryRepository) {
    private val db get() = inventory.database
    private val mutable = MutableStateFlow(CommerceState())
    val state = mutable.asStateFlow()
    init { refresh() }
    private fun id() = UUID.randomUUID().toString()
    private fun now() = LocalDateTime.now().toString()

    @Synchronized
    fun sell(productId: String, customer: String, quantity: Int, total: Long, due: String, paid: Boolean): Result<Unit> = change {
        val saleId = insertSale(productId, customer, quantity, total, due)
        if (paid) {
            val bill = db.rawQuery("SELECT id FROM bills WHERE sale=?", arrayOf(saleId)).use { it.moveToFirst(); it.getString(0) }
            db.execSQL("INSERT INTO payments VALUES (?,?,?,?)", arrayOf(id(),bill,total,now()))
        }
    }

    private fun insertSale(productId: String, customer: String, quantity: Int, total: Long, due: String): String {
        require(customer.isNotBlank()) { "Informe o cliente." }; validateMoney(total); date(due)
        require(quantity > 0) { "Quantidade deve ser maior que zero." }
        val product = inventory.products.value.firstOrNull { it.id == productId } ?: error("Selecione um produto.")
        require(product.quantity >= quantity) { "Estoque insuficiente: ${product.quantity} unidades disponíveis." }
        val saleId = id(); val label = "${product.productName} · ${product.size} · ${product.color}"
        db.execSQL("UPDATE products SET quantity=quantity-? WHERE id=?", arrayOf(quantity,productId))
        db.execSQL("INSERT INTO movements VALUES (?,?,?,?,?,?,?)",arrayOf(id(),productId,label,"SALE",quantity,now(),"Venda $saleId"))
        db.execSQL("INSERT INTO sales VALUES (?,?,?,?,?,?,?,0)",arrayOf(saleId,productId,label,customer.trim(),quantity,total,now()))
        db.execSQL("INSERT INTO bills VALUES (?,?,?,?,?,?,?,0)",arrayOf(id(),"RECEIVE","Venda: $label",customer.trim(),total,due,saleId))
        return saleId
    }

    @Synchronized
    fun cancelSale(saleId: String): Result<Unit> = change {
        val sale = mutable.value.sales.firstOrNull { it.id==saleId } ?: error("Venda não encontrada.")
        require(!sale.canceled) { "Venda já cancelada." }
        val bill = mutable.value.bills.first { it.sale==saleId }
        require(bill.paid==0L) { "Venda com recebimento não pode ser cancelada. É necessário tratar a devolução financeira." }
        db.execSQL("UPDATE products SET quantity=quantity+? WHERE id=?",arrayOf(sale.quantity,sale.product))
        db.execSQL("INSERT INTO movements VALUES (?,?,?,?,?,?,?)",arrayOf(id(),sale.product,sale.label,"ENTRY",sale.quantity,now(),"Cancelamento da venda $saleId"))
        db.execSQL("UPDATE sales SET canceled=1 WHERE id=?",arrayOf(saleId))
        db.execSQL("UPDATE bills SET canceled=1 WHERE sale=?",arrayOf(saleId))
        db.execSQL("UPDATE orders SET status='Cancelada' WHERE sale=?",arrayOf(saleId))
    }

    @Synchronized
    fun addBill(direction: String, description: String, person: String, amount: Long, due: String): Result<Unit> = change {
        require(direction in listOf("PAY","RECEIVE")); validateMoney(amount); date(due)
        require(description.isNotBlank() && person.isNotBlank()) { "Informe descrição e pessoa/fornecedor." }
        db.execSQL("INSERT INTO bills VALUES (?,?,?,?,?,?,NULL,0)",arrayOf(id(),direction,description.trim(),person.trim(),amount,due))
    }

    @Synchronized
    fun settle(billId: String, amount: Long): Result<Unit> = change {
        val bill = mutable.value.bills.firstOrNull { it.id==billId } ?: error("Conta não encontrada.")
        require(!bill.canceled) { "Conta cancelada." }; validateMoney(amount)
        require(amount <= bill.remaining) { "Valor maior que o saldo da conta." }
        db.execSQL("INSERT INTO payments VALUES (?,?,?,?)",arrayOf(id(),billId,amount,now()))
    }

    @Synchronized
    fun cancelBill(billId: String): Result<Unit> = change {
        val bill = mutable.value.bills.first { it.id==billId }
        require(bill.sale==null) { "Cancele pela venda de origem." }
        require(!bill.canceled && bill.paid==0L) { "Somente contas sem baixa podem ser canceladas." }
        db.execSQL("UPDATE bills SET canceled=1 WHERE id=?",arrayOf(billId))
    }

    @Synchronized
    fun order(productId: String, customer: String, contact: String, quantity: Int, total: Long, due: String, note: String): Result<Unit> = change {
        validateMoney(total); date(due)
        require(customer.isNotBlank() && contact.isNotBlank()) { "Informe cliente e contato." }
        require(quantity>0) { "Informe quantidade válida." }
        val product = inventory.products.value.firstOrNull { it.id==productId } ?: error("Selecione um produto.")
        db.execSQL("INSERT INTO orders VALUES (?,?,?,?,?,?,?,?,?,'Aberta',NULL)",arrayOf(id(),productId,"${product.productName} · ${product.size} · ${product.color}",customer.trim(),contact.trim(),quantity,total,due,note.trim()))
    }

    @Synchronized
    fun updateOrder(orderId: String, next: String): Result<Unit> = change {
        val order = mutable.value.orders.first { it.id==orderId }
        require(order.status in listOf("Aberta","Em preparação")) { "Encomenda já encerrada." }
        require(next in listOf("Em preparação","Entregue","Cancelada"))
        if (next=="Entregue") {
            val sale = insertSale(order.product,order.customer,order.quantity,order.total,LocalDate.now().toString())
            db.execSQL("UPDATE orders SET status=?,sale=? WHERE id=?",arrayOf(next,sale,orderId))
        } else db.execSQL("UPDATE orders SET status=? WHERE id=?",arrayOf(next,orderId))
    }

    fun updateProduct(productId: String, name: String, category: String, size: String, color: String, price: Long, minimum: Int): Result<Unit> = change {
        require(listOf(name,category,size,color).none { it.isBlank() }) { "Preencha todos os campos." }
        validateMoney(price); require(minimum>=0) { "Mínimo inválido." }
        db.execSQL("UPDATE products SET name=?,category=?,size=?,color=?,price=?,minimum=? WHERE id=?",arrayOf(name.trim(),category.trim(),size.trim(),color.trim(),price/100.0,minimum,productId))
    }

    private fun change(block: () -> Unit): Result<Unit> = runCatching {
        inventory.transaction(block); inventory.refresh(); refresh()
    }
    private fun validateMoney(amount: Long) { require(amount in 1..999_999_999_99L) { "Informe um valor positivo de até R$ 999.999.999,99." } }
    private fun date(value: String) { try { LocalDate.parse(value) } catch (_: Exception) { error("Data inválida. Use AAAA-MM-DD.") } }

    private fun refresh() {
        val sales = db.rawQuery("SELECT * FROM sales ORDER BY occurred DESC",null).use { c -> buildList { while(c.moveToNext()) add(Sale(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4),c.getLong(5),c.getString(6),c.getInt(7)==1)) } }
        val bills = db.rawQuery("SELECT b.*, COALESCE((SELECT SUM(p.amount) FROM payments p WHERE p.bill=b.id),0) FROM bills b ORDER BY due,id",null).use { c -> buildList { while(c.moveToNext()) add(Bill(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getLong(4),c.getString(5),if(c.isNull(6)) null else c.getString(6),c.getInt(7)==1,c.getLong(8))) } }
        val payments = db.rawQuery("SELECT b.description,b.direction,p.amount,p.occurred FROM payments p JOIN bills b ON b.id=p.bill ORDER BY p.occurred DESC",null).use { c -> buildList { while(c.moveToNext()) add(Payment(c.getString(0),c.getString(1),c.getLong(2),c.getString(3))) } }
        val orders = db.rawQuery("SELECT * FROM orders ORDER BY due,id",null).use { c -> buildList { while(c.moveToNext()) add(Order(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getInt(5),c.getLong(6),c.getString(7),c.getString(8),c.getString(9))) } }
        mutable.value=CommerceState(sales,bills,payments,orders)
    }
}

fun moneyInput(text: String): Long = try {
    BigDecimal(text.trim().replace(',','.')).setScale(2,RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
} catch (_: Exception) { error("Informe um valor com até duas casas decimais, por exemplo 29,90.") }
