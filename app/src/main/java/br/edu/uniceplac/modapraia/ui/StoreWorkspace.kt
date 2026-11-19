package br.edu.uniceplac.modapraia.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.edu.uniceplac.modapraia.data.*
import br.edu.uniceplac.modapraia.domain.*
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private fun currency(cents: Long) = NumberFormat.getCurrencyInstance(Locale("pt","BR")).format(cents/100.0)
private fun displayDate(value: String) = runCatching { LocalDate.parse(value.take(10)).let { "%02d/%02d/%04d".format(it.dayOfMonth,it.monthValue,it.year) } }.getOrDefault(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreWorkspace(inventory: LocalInventoryRepository, viewModel: InventoryViewModel, onLogout: () -> Unit) {
    val commerce = remember { CommerceRepository(inventory) }
    val state by commerce.state.collectAsStateWithLifecycle()
    val stock by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf("Estoque") }
    var form by remember { mutableStateOf<String?>(null) }
    var detailId by remember { mutableStateOf<String?>(null) }
    var selectedBill by remember { mutableStateOf<Bill?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<Pair<String, () -> Result<Unit>>?>(null) }
    fun result(value: Result<Unit>) { message=value.fold({"Operação concluída."},{it.message ?: "Não foi possível concluir."}) }
    Scaffold(topBar={ TopAppBar(title={Text("Estoque Praia",fontWeight=FontWeight.Bold)}, actions={ TextButton(onClick={confirmation="Adicionar dados fictícios de apresentação sem alterar seus registros? A carga é feita uma única vez." to {commerce.loadDemoData()}}){Text("Demo")}; TextButton(onClick=onLogout){Text("Sair")} }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if(stock.products.any {it.sku.startsWith("DEMO-")}) Text("Contém dados fictícios de demonstração",style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(horizontal=16.dp),color=MaterialTheme.colorScheme.secondary)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Estoque","Vendas","Financeiro","A pagar","A receber","Encomendas").forEach { name -> FilterChip(selected=tab==name,onClick={tab=name},label={Text(name)}) }
            }
            if(tab=="Estoque") {
                Button(onClick={form="Produto"},modifier=Modifier.padding(horizontal=16.dp)){Text("Cadastrar produto")}
                InventoryScreen(stock, viewModel::updateSearch, {detailId=it.id},Modifier.weight(1f))
            } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                when(tab) {
                    "Vendas" -> {
                        Button(onClick={form="Venda"},enabled=stock.products.isNotEmpty()){Text("Registrar venda")}
                        Text("Vendas ativas: ${currency(state.sales.filterNot { it.canceled }.sumOf { it.total })}")
                        if(state.sales.isEmpty()) Text("Nenhuma venda registrada. Cadastre um produto e registre sua primeira venda.")
                        state.sales.forEach { sale -> RecordCard(sale.label,"${sale.customer} · ${sale.quantity} un.\n${displayDate(sale.date)} · ${currency(sale.total)}\n${if(sale.canceled) "Cancelada" else "Registrada"}") {
                            val bill=state.bills.firstOrNull { it.sale==sale.id }
                            if(!sale.canceled && bill?.paid==0L) TextButton(onClick={confirmation="Cancelar venda e devolver ${sale.quantity} unidades ao estoque?" to {commerce.cancelSale(sale.id)}}){Text("Cancelar venda")}
                            if(bill!=null && !bill.canceled) Text("Recebido: ${currency(bill.paid)} · Falta: ${currency(bill.remaining)}")
                        } }
                    }
                    "A pagar", "A receber" -> {
                        val direction=if(tab=="A pagar") "PAY" else "RECEIVE"
                        Button(onClick={form=tab}){Text("Nova conta ${tab.lowercase()}")}
                        val bills=state.bills.filter { it.direction==direction }
                        AccountsChart(bills,direction=="PAY")
                        Text("Saldo em aberto: ${currency(bills.filterNot { it.canceled }.sumOf { it.remaining })}",fontWeight=FontWeight.Bold)
                        if(bills.isEmpty()) Text("Nenhuma conta cadastrada.")
                        bills.forEach { bill -> RecordCard(bill.description,"${bill.person}\nVencimento: ${displayDate(bill.due)} · ${bill.status}\nTotal: ${currency(bill.total)} · Baixado: ${currency(bill.paid)}\nRestante: ${currency(bill.remaining)}") {
                            if(!bill.canceled && bill.remaining>0) Button(onClick={selectedBill=bill}){Text(if(direction=="PAY") "Registrar pagamento" else "Registrar recebimento")}
                            if(!bill.canceled && bill.paid==0L && bill.sale==null) TextButton(onClick={confirmation="Cancelar esta conta sem movimentar o caixa?" to {commerce.cancelBill(bill.id)}}){Text("Cancelar conta")}
                        } }
                    }
                    "Financeiro" -> {
                        val incoming=state.payments.filter { it.direction=="RECEIVE" }.sumOf { it.amount }
                        val outgoing=state.payments.filter { it.direction=="PAY" }.sumOf { it.amount }
                        CashChart(state.payments)
                        RecordCard("Caixa acumulado", "Entradas recebidas: ${currency(incoming)}\nSaídas pagas: ${currency(outgoing)}\nSaldo: ${currency(incoming-outgoing)}"){}
                        Text("O caixa considera somente baixas realizadas, sem saldo inicial. Vendas e contas em aberto não são somadas novamente.",style=MaterialTheme.typography.bodySmall)
                        Text("A receber: ${currency(state.bills.filter { it.direction=="RECEIVE" && !it.canceled }.sumOf { it.remaining })}")
                        Text("A pagar: ${currency(state.bills.filter { it.direction=="PAY" && !it.canceled }.sumOf { it.remaining })}")
                        Text("Fluxo de caixa",style=MaterialTheme.typography.titleMedium)
                        if(state.payments.isEmpty()) Text("Registre pagamentos ou recebimentos para iniciar o fluxo de caixa.")
                        state.payments.forEach { RecordCard(it.description,"${displayDate(it.date)} · ${if(it.direction=="RECEIVE") "Entrada" else "Saída"} · ${currency(it.amount)}"){} }
                    }
                    "Encomendas" -> {
                        Button(onClick={form="Encomenda"},enabled=stock.products.isNotEmpty()){Text("Nova encomenda")}
                        Text("A encomenda não reserva estoque. Na entrega, o saldo é verificado, a venda é gerada e a conta a receber é criada.",style=MaterialTheme.typography.bodySmall)
                        if(state.orders.isEmpty()) Text("Nenhuma encomenda cadastrada.")
                        state.orders.forEach { order -> RecordCard(order.label,"${order.customer} · ${order.contact}\n${order.quantity} un. · ${currency(order.total)}\nPrazo: ${displayDate(order.due)} · ${order.status}\n${order.note}") {
                            if(order.status=="Aberta") TextButton(onClick={result(commerce.updateOrder(order.id,"Em preparação"))}){Text("Iniciar preparação")}
                            if(order.status in listOf("Aberta","Em preparação")) {
                                Button(onClick={confirmation="Entregar encomenda, baixar estoque e gerar venda/recebível?" to {commerce.updateOrder(order.id,"Entregue")}}){Text("Entregar e gerar venda")}
                                TextButton(onClick={confirmation="Cancelar encomenda? O estoque não será alterado." to {commerce.updateOrder(order.id,"Cancelada")}}){Text("Cancelar encomenda")}
                            }
                        } }
                    }
                }
            }
        }
    }
    val product=stock.products.firstOrNull { it.id==detailId }
    if(form==null) product?.let { p -> ProductDetails(p,stock.movements.filter { it.variantId==p.id },{detailId=null},{form="Editar"},{form="Movimentar"},{form="Venda"}) }
    if(form=="Produto") ProductRegistrationDialog({form=null},viewModel::addProduct)
    if(form=="Encomenda") OrderForm(stock.products,commerce,{form=null})
    if(form=="Venda") {
        val isOrder=form=="Encomenda"
        ProductOperationForm(form!!,stock.products,product, {form=null}) { chosen, values ->
            runCatching {
                val qty=values["Quantidade"]?.toIntOrNull() ?: error("Quantidade inválida.")
                val total=moneyInput(values["Valor total (R$)"].orEmpty())
                if(isOrder) commerce.order(chosen,values["Cliente"].orEmpty(),values["Contato"].orEmpty(),qty,total,values["Data (AAAA-MM-DD)"].orEmpty(),values["Observação"].orEmpty()).getOrThrow()
                else commerce.sell(chosen,values["Cliente"].orEmpty(),qty,total,values["Data (AAAA-MM-DD)"].orEmpty(),values["Recebido agora"]=="Sim").getOrThrow()
            }.exceptionOrNull()?.message
        }
    }
    if(form in listOf("A pagar","A receber")) EntryForm("Nova conta ${form!!.lowercase()}",listOf("Descrição","Pessoa ou fornecedor","Valor (R$)","Vencimento (AAAA-MM-DD)"),mapOf("Vencimento (AAAA-MM-DD)" to LocalDate.now().toString()),{form=null}) { v -> runCatching { commerce.addBill(if(form=="A pagar") "PAY" else "RECEIVE",v["Descrição"].orEmpty(),v["Pessoa ou fornecedor"].orEmpty(),moneyInput(v["Valor (R$)"].orEmpty()),v["Vencimento (AAAA-MM-DD)"].orEmpty()).getOrThrow() }.exceptionOrNull()?.message }
    selectedBill?.let { bill -> EntryForm(if(bill.direction=="PAY") "Pagamento" else "Recebimento",listOf("Valor (R$)"),mapOf("Valor (R$)" to java.math.BigDecimal.valueOf(bill.remaining,2).toPlainString()),{selectedBill=null}) { v -> runCatching {commerce.settle(bill.id,moneyInput(v["Valor (R$)"].orEmpty())).getOrThrow()}.exceptionOrNull()?.message } }
    if(form=="Editar" && product!=null) EntryForm("Editar produto",listOf("Produto","Categoria","Tamanho","Cor","Preço (R$)","Estoque mínimo"),mapOf("Produto" to product.productName,"Categoria" to product.category,"Tamanho" to product.size,"Cor" to product.color,"Preço (R$)" to product.salePrice.toString(),"Estoque mínimo" to product.minimumQuantity.toString()),{form=null}) { v -> runCatching {commerce.updateProduct(product.id,v["Produto"].orEmpty(),v["Categoria"].orEmpty(),v["Tamanho"].orEmpty(),v["Cor"].orEmpty(),moneyInput(v["Preço (R$)"].orEmpty()),v["Estoque mínimo"]?.toIntOrNull() ?: error("Mínimo inválido.")).getOrThrow()}.exceptionOrNull()?.message }
    if(form=="Movimentar" && product!=null) EntryForm("Entrada de estoque",listOf("Quantidade","Observação"),emptyMap(),{form=null}) { v -> inventory.registerMovement(product.id,MovementType.ENTRY,v["Quantidade"]?.toIntOrNull() ?: 0,v["Observação"].orEmpty()).exceptionOrNull()?.message }
    message?.let { text -> AlertDialog(onDismissRequest={message=null},title={Text("Resultado")},text={Text(text)},confirmButton={TextButton(onClick={message=null}){Text("OK")}}) }
    confirmation?.let { pending -> AlertDialog(onDismissRequest={confirmation=null},title={Text("Confirmar operação")},text={Text(pending.first)},confirmButton={Button(onClick={result(pending.second());confirmation=null}){Text("Confirmar")}},dismissButton={TextButton(onClick={confirmation=null}){Text("Voltar")}}) }
}

@Composable
private fun RecordCard(title: String, subtitle: String, actions: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text(title,fontWeight=FontWeight.Bold); Text(subtitle); actions() } }
}

@Composable
private fun ProductDetails(product: ProductVariant,movements: List<StockMovement>,dismiss:()->Unit,edit:()->Unit,entry:()->Unit,sale:()->Unit) {
    AlertDialog(onDismissRequest=dismiss,title={Text(product.productName)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("${product.category} · ${product.size} · ${product.color}\nSKU: ${product.sku}\nSaldo: ${product.quantity} · Mínimo: ${product.minimumQuantity}\nPreço: ${currency(kotlin.math.round(product.salePrice*100).toLong())}")
        TextButton(onClick=edit){Text("Editar dados do produto")}
        Button(onClick=entry){Text("Registrar entrada")}; Button(onClick=sale){Text("Registrar venda / saída")}
        Text("Histórico do produto",fontWeight=FontWeight.Bold)
        if(movements.isEmpty()) Text("Sem movimentações.")
        movements.forEach { Text("${displayDate(it.occurredAt)} · ${it.type.label} · ${it.quantity} un.\n${it.note}",style=MaterialTheme.typography.bodySmall); HorizontalDivider() }
    }},confirmButton={TextButton(onClick=dismiss){Text("Fechar")}})
}

@Composable
private fun EntryForm(title:String,fields:List<String>,initial:Map<String,String>,dismiss:()->Unit,save:(Map<String,String>)->String?) {
    val values=remember { mutableStateMapOf<String,String>().apply { putAll(initial) } }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest=dismiss,title={Text(title)},text={Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        fields.forEach { field -> OutlinedTextField(values[field].orEmpty(),{values[field]=it},label={Text(field)},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(field.contains("R$")) KeyboardType.Decimal else if(field in listOf("Quantidade","Estoque mínimo")) KeyboardType.Number else KeyboardType.Text)) }
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
    }},confirmButton={Button(onClick={error=save(values.toMap());if(error==null)dismiss()}){Text("Salvar")}},dismissButton={TextButton(onClick=dismiss){Text("Cancelar")}})
}

@Composable
private fun ProductOperationForm(title:String,products:List<ProductVariant>,initial:ProductVariant?,dismiss:()->Unit,save:(String,Map<String,String>)->String?) {
    var chosen by remember { mutableStateOf(initial?.id ?: products.firstOrNull()?.id.orEmpty()) }
    var expanded by remember { mutableStateOf(false) }
    val values=remember { mutableStateMapOf("Quantidade" to "1","Data (AAAA-MM-DD)" to LocalDate.now().toString(),"Recebido agora" to "Não") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest=dismiss,title={Text(title)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        TextButton(onClick={expanded=true}){Text(products.firstOrNull{it.id==chosen}?.let { "${it.productName} · ${it.size} · ${it.color} (${it.quantity} un.)" } ?: "Selecionar produto")}
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) { products.forEach { p -> DropdownMenuItem(text={Text("${p.productName} · ${p.size} · ${p.color}")},onClick={chosen=p.id;expanded=false}) } }
        val selected=products.firstOrNull { it.id==chosen }
        Text("Preço unitário: ${currency(kotlin.math.round((selected?.salePrice ?: 0.0)*100).toLong())}",style=MaterialTheme.typography.bodySmall)
        val fields=if(title=="Encomenda") listOf("Cliente","Contato","Quantidade","Valor total (R$)","Data (AAAA-MM-DD)","Observação") else listOf("Cliente","Quantidade","Valor total (R$)","Data (AAAA-MM-DD)")
        fields.forEach { field -> OutlinedTextField(values[field].orEmpty(),{values[field]=it},label={Text(field)},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(field.contains("R$")) KeyboardType.Decimal else if(field=="Quantidade") KeyboardType.Number else KeyboardType.Text)) }
        TextButton(onClick={values["Valor total (R$)"]=runCatching { java.math.BigDecimal.valueOf(selected?.salePrice ?: 0.0).multiply(java.math.BigDecimal(values["Quantidade"].orEmpty())).setScale(2).toPlainString() }.getOrDefault("")}){Text("Usar preço cadastrado × quantidade")}
        if(title=="Venda") Row { Checkbox(checked=values["Recebido agora"]=="Sim",onCheckedChange={values["Recebido agora"]=if(it)"Sim" else "Não"}); Text("Valor recebido agora") }
        Text(if(title=="Encomenda") "Data = prazo de entrega. Sem reserva ou cobrança até entregar." else "Data = vencimento da conta a receber.",style=MaterialTheme.typography.bodySmall)
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
    }},confirmButton={Button(onClick={error=save(chosen,values.toMap());if(error==null)dismiss()}){Text("Registrar")}},dismissButton={TextButton(onClick=dismiss){Text("Cancelar")}})
}
