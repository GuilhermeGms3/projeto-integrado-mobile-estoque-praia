package br.edu.uniceplac.modapraia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProductRegistrationDialog(
    onDismiss: () -> Unit,
    onSave: (
        productName: String,
        category: String,
        size: String,
        color: String,
        sku: String,
        price: String,
        initialQuantity: String,
        minimumQuantity: String
    ) -> String?
) {
    var productName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var initialQuantity by remember { mutableStateOf("") }
    var minimumQuantity by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun save() {
        error = onSave(
            productName,
            category,
            size,
            color,
            sku,
            price,
            initialQuantity,
            minimumQuantity
        )
        if (error == null) onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cadastrar produto") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { OutlinedTextField(productName, { productName = it }, label = { Text("Produto") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(category, { category = it }, label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(size, { size = it }, label = { Text("Tamanho") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(color, { color = it }, label = { Text("Cor") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(sku, { sku = it.uppercase() }, label = { Text("SKU ou código") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item {
                    OutlinedTextField(
                        price,
                        { price = it.filter { character -> character.isDigit() || character == ',' || character == '.' } },
                        label = { Text("Preço de venda") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        initialQuantity,
                        { initialQuantity = it.filter(Char::isDigit) },
                        label = { Text("Estoque inicial") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        minimumQuantity,
                        { minimumQuantity = it.filter(Char::isDigit) },
                        label = { Text("Estoque mínimo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                error?.let { message ->
                    item { Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = { Button(onClick = ::save) { Text("Cadastrar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
