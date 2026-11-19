package br.edu.uniceplac.modapraia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.edu.uniceplac.modapraia.data.Bill
import br.edu.uniceplac.modapraia.data.Payment
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private fun chartMoney(value: Long) = NumberFormat.getCurrencyInstance(Locale("pt","BR")).format(value/100.0)
data class ChartValue(val label: String, val cents: Long, val color: Color)

@Composable
private fun MoneyBarChart(title: String, entries: List<ChartValue>, note: String) {
    val maximum=entries.maxOfOrNull {it.cents} ?: 0L
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
            entries.forEach { item ->
                Column(Modifier.semantics(mergeDescendants=true){contentDescription="${item.label}: ${chartMoney(item.cents)}"}) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(item.label);Text(chartMoney(item.cents),fontWeight=FontWeight.SemiBold)}
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.fillMaxWidth().height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        if(item.cents>0 && maximum>0) Box(Modifier.fillMaxWidth((item.cents.toDouble()/maximum).toFloat().coerceIn(0f,1f)).fillMaxHeight().background(item.color))
                    }
                }
            }
            Text(if(maximum==0L) "Sem valores para exibir. $note" else note,style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AccountsChart(bills: List<Bill>, payable: Boolean) {
    val active=bills.filterNot {it.canceled}
    val today=LocalDate.now().toString()
    MoneyBarChart(if(payable) "Situação das contas a pagar" else "Situação das contas a receber",listOf(
        ChartValue(if(payable) "Pago" else "Recebido",active.sumOf {it.paid},MaterialTheme.colorScheme.primary),
        ChartValue("A vencer",active.filter {it.due>=today}.sumOf {it.remaining},MaterialTheme.colorScheme.secondary),
        ChartValue("Vencido",active.filter {it.due<today}.sumOf {it.remaining},MaterialTheme.colorScheme.error)
    ),"Total acumulado. Valores em reais; contas canceladas excluídas. Barras na mesma escala.")
}

@Composable
fun CashChart(payments: List<Payment>) {
    val incoming=payments.filter {it.direction=="RECEIVE"}.sumOf {it.amount}
    val outgoing=payments.filter {it.direction=="PAY"}.sumOf {it.amount}
    MoneyBarChart("Entradas e saídas realizadas",listOf(
        ChartValue("Recebimentos",incoming,MaterialTheme.colorScheme.primary),
        ChartValue("Pagamentos",outgoing,MaterialTheme.colorScheme.error)
    ),"Acumulado desde o início. Contas ainda em aberto não entram no caixa.")
    val from=LocalDate.now().minusDays(6)
    val recent=payments.filter {runCatching {LocalDate.parse(it.date.take(10)) in from..LocalDate.now()}.getOrDefault(false)}
    MoneyBarChart("Movimento dos últimos 7 dias",(0L..6L).map { day ->
        val date=from.plusDays(day)
        val total=recent.filter {it.date.startsWith(date.toString())}.sumOf {it.amount}
        ChartValue("%02d/%02d".format(date.dayOfMonth,date.monthValue),total,MaterialTheme.colorScheme.primary)
    },"Volume diário: recebimentos + pagamentos, sem compensar entradas e saídas.")
}
