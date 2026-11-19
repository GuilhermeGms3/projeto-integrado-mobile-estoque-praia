package br.edu.uniceplac.modapraia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.edu.uniceplac.modapraia.data.CommerceRepository
import br.edu.uniceplac.modapraia.data.moneyInput
import br.edu.uniceplac.modapraia.domain.ProductVariant
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderForm(products: List<ProductVariant>, repository: CommerceRepository, dismiss:()->Unit) {
    var chosen by remember {mutableStateOf(products.firstOrNull()?.id.orEmpty())}
    var customer by remember {mutableStateOf("")}
    var contact by remember {mutableStateOf("")}
    var quantity by remember {mutableStateOf("1")}
    var total by remember {mutableStateOf("")}
    var due by remember {mutableStateOf(LocalDate.now().plusDays(3).format(DateTimeFormatter.ofPattern("dd/MM/uuuu")))}
    var note by remember {mutableStateOf("")}
    var menu by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    val product=products.firstOrNull {it.id==chosen}
    val suggested=runCatching {BigDecimal.valueOf(product?.salePrice ?: 0.0).multiply(BigDecimal(quantity)).setScale(2).toPlainString()}.getOrDefault("")
    fun save() {
        error=runCatching {
            val deadline=try {LocalDate.parse(due,DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT))} catch(_:Exception){error("Informe um prazo válido no formato DD/MM/AAAA.")}
            repository.order(chosen,customer,contact,quantity.toIntOrNull() ?: error("Informe uma quantidade inteira positiva."),moneyInput(total.ifBlank {suggested}),deadline.toString(),note).getOrThrow()
        }.exceptionOrNull()?.message
        if(error==null) dismiss()
    }
    Dialog(onDismissRequest=dismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(topBar={TopAppBar(title={Text("Nova encomenda")},navigationIcon={TextButton(onClick=dismiss){Text("Voltar")}})},bottomBar={
                Surface(shadowElevation=4.dp){Button(onClick=::save,enabled=products.isNotEmpty(),modifier=Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp)){Text("Salvar encomenda")}}
            }) { padding -> Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Text("Cliente",style=MaterialTheme.typography.titleMedium)
                OutlinedTextField(customer,{customer=it},label={Text("Nome do cliente *")},singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(contact,{contact=it},label={Text("Contato / WhatsApp *")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),modifier=Modifier.fillMaxWidth())
                Text("Produto e valores",style=MaterialTheme.typography.titleMedium)
                Box {
                    OutlinedButton(onClick={menu=true},modifier=Modifier.fillMaxWidth()){Text(product?.let {"${it.productName} · ${it.size} · ${it.color}"} ?: "Selecione um produto *")}
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {products.forEach {p -> DropdownMenuItem(text={Text("${p.productName} · ${p.size} · ${p.color}")},onClick={chosen=p.id;menu=false;total=""})}}
                }
                product?.let {Text("SKU ${it.sku} · ${it.quantity} unidades disponíveis",style=MaterialTheme.typography.bodySmall)}
                OutlinedTextField(quantity,{quantity=it.filter(Char::isDigit)},label={Text("Quantidade *")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(total,{total=it},label={Text("Valor total combinado (R$)")},placeholder={Text(suggested.replace('.',','))},supportingText={Text("Em branco, usa preço cadastrado × quantidade: ${suggested.replace('.',',')}")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true,modifier=Modifier.fillMaxWidth())
                Text("Entrega",style=MaterialTheme.typography.titleMedium)
                OutlinedTextField(due,{due=it},label={Text("Prazo de entrega *")},supportingText={Text("DD/MM/AAAA")},singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(note,{note=it},label={Text("Personalização, retirada ou observações")},minLines=3,modifier=Modifier.fillMaxWidth())
                Text("A encomenda fica em aberto. A entrega gera a venda e a conta a receber, verificando o saldo disponível.",style=MaterialTheme.typography.bodySmall)
                error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            } }
        }
    }
}
