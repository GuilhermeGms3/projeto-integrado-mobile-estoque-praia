package br.edu.uniceplac.modapraia

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import br.edu.uniceplac.modapraia.data.*
import br.edu.uniceplac.modapraia.domain.*

@Suppress("DEPRECATION")
class CommerceIntegrationTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun isolatedContext() = object : android.content.ContextWrapper(instrumentation.targetContext) {
        override fun getApplicationContext(): android.content.Context = this
        override fun getDatabasePath(name: String): java.io.File = super.getDatabasePath("integration-$name").also { it.parentFile?.mkdirs() }
        override fun openOrCreateDatabase(name: String, mode: Int, factory: android.database.sqlite.SQLiteDatabase.CursorFactory?): android.database.sqlite.SQLiteDatabase = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name),factory)
        override fun deleteDatabase(name: String): Boolean = android.database.sqlite.SQLiteDatabase.deleteDatabase(getDatabasePath(name))
    }
    @Test
    fun testSaleAccountsCashAndOrderTransactions() {
        val context=isolatedContext()
        context.deleteDatabase("inventory.db")
        val inventory=LocalInventoryRepository(context)
        val commerce=CommerceRepository(inventory)
        inventory.addProduct(NewProduct("Peça teste","Biquíni","M","Azul","TEST-1",29.9,10,2)).getOrThrow()
        val product=inventory.products.value.single()
        commerce.sell(product.id,"Cliente teste",2,5980,"2026-12-01",false).getOrThrow()
        assertEquals(8,inventory.products.value.single().quantity)
        val bill=commerce.state.value.bills.single()
        assertEquals(5980L,bill.remaining)
        commerce.settle(bill.id,2000).getOrThrow()
        assertEquals(3980L,commerce.state.value.bills.single().remaining)
        assertTrue(commerce.settle(bill.id,4000).isFailure)
        assertTrue(commerce.cancelSale(commerce.state.value.sales.single().id).isFailure)
        assertEquals(1,commerce.state.value.payments.size)
        assertTrue(commerce.sell(product.id,"Cliente",99,1000,"2026-12-01",false).isFailure)
        assertEquals(1,commerce.state.value.sales.size)
        assertEquals(8,inventory.products.value.single().quantity)
        commerce.addBill("PAY","Fornecedor","Fornecedor teste",1000,"2026-12-01").getOrThrow()
        commerce.settle(commerce.state.value.bills.first {it.direction=="PAY"}.id,1000).getOrThrow()
        assertEquals(1000L,commerce.state.value.payments.sumOf {if(it.direction=="RECEIVE")it.amount else -it.amount})
        commerce.order(product.id,"Encomenda teste","Contato teste",3,8970,"2026-12-01","").getOrThrow()
        assertEquals(8,inventory.products.value.single().quantity)
        val order=commerce.state.value.orders.single()
        commerce.updateOrder(order.id,"Entregue").getOrThrow()
        assertEquals(5,inventory.products.value.single().quantity)
        assertEquals(2,commerce.state.value.sales.size)
        assertTrue(commerce.updateOrder(order.id,"Entregue").isFailure)
        val deliverySale=commerce.state.value.sales.first {it.customer=="Encomenda teste"}
        commerce.cancelSale(deliverySale.id).getOrThrow()
        assertEquals(8,inventory.products.value.single().quantity)
        assertTrue(commerce.state.value.bills.first {it.sale==deliverySale.id}.canceled)
        assertEquals("Cancelada",commerce.state.value.orders.single().status)
        commerce.order(product.id,"Sem saldo","Contato",99,1000,"2026-12-01","").getOrThrow()
        val impossible=commerce.state.value.orders.first { it.customer=="Sem saldo" }
        assertTrue(commerce.updateOrder(impossible.id,"Entregue").isFailure)
        assertEquals("Aberta",commerce.state.value.orders.first {it.id==impossible.id}.status)
        assertEquals(2,commerce.state.value.sales.size)
        commerce.updateOrder(impossible.id,"Cancelada").getOrThrow()
        commerce.addBill("PAY","Cancelar teste","Fornecedor",500,"2026-12-01").getOrThrow()
        val canceledBill=commerce.state.value.bills.first {it.description=="Cancelar teste"}
        commerce.cancelBill(canceledBill.id).getOrThrow()
        assertTrue(commerce.settle(canceledBill.id,100).isFailure)
        commerce.updateProduct(product.id,"Peça editada","Moda","G","Preto",3990,3).getOrThrow()
        val reopened=LocalInventoryRepository(context)
        assertEquals("Peça editada",reopened.products.value.single().productName)
        assertEquals(8,reopened.products.value.single().quantity)
        assertEquals(123L,moneyInput("1,23"))
        assertTrue(runCatching {moneyInput("1,234")}.isFailure)
    }

    @Test
    fun testVersionOneMigrationPreservesProducts() {
        val context=isolatedContext()
        context.deleteDatabase("inventory.db")
        context.openOrCreateDatabase("inventory.db",0,null).use { db ->
            db.execSQL("CREATE TABLE products (id TEXT PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL, size TEXT NOT NULL, color TEXT NOT NULL, sku TEXT NOT NULL UNIQUE, quantity INTEGER NOT NULL CHECK(quantity >= 0), minimum INTEGER NOT NULL CHECK(minimum >= 0), price REAL NOT NULL CHECK(price > 0))")
            db.execSQL("CREATE TABLE movements (id TEXT PRIMARY KEY, variant TEXT NOT NULL REFERENCES products(id), label TEXT NOT NULL, type TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity > 0), occurred TEXT NOT NULL, note TEXT NOT NULL)")
            db.execSQL("INSERT INTO products VALUES ('old','Produto anterior','Moda','P','Azul','OLD',7,2,20)")
            db.version=1
        }
        val inventory=LocalInventoryRepository(context)
        assertEquals(7,inventory.products.value.single().quantity)
        val commerce=CommerceRepository(inventory)
        assertTrue(commerce.state.value.bills.isEmpty())
        assertTrue(commerce.addBill("PAY","Compra","Fornecedor",1000,"2026-02-30").isFailure)
        commerce.sell("old","Cliente",1,2000,"2026-12-01",true).getOrThrow()
        assertEquals(6,inventory.products.value.single().quantity)
        assertEquals(0L,commerce.state.value.bills.single().remaining)
        assertEquals(2000L,commerce.state.value.payments.single().amount)
    }
}
