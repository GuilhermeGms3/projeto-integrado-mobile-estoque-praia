package br.edu.uniceplac.modapraia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.uniceplac.modapraia.data.InMemoryAuthRepository
import br.edu.uniceplac.modapraia.domain.LoginRequest
import br.edu.uniceplac.modapraia.domain.MovementType
import br.edu.uniceplac.modapraia.domain.ProductVariant
import br.edu.uniceplac.modapraia.domain.RegistrationRequest

@Composable
fun InventoryApp(viewModel: InventoryViewModel = viewModel(factory = InventoryViewModel.factory())) {
    var authenticated by remember { mutableStateOf(false) }
    var registrationMode by remember { mutableStateOf(false) }
    val authRepository = remember { InMemoryAuthRepository() }

    if (!authenticated) {
        AuthenticationScreen(
            registrationMode = registrationMode,
            onChangeMode = { registrationMode = it },
            onLogin = { email, password ->
                authRepository.login(LoginRequest(email, password)).isSuccess.also {
                    authenticated = it
                }
            },
            onRegister = { name, email, password ->
                authRepository.register(RegistrationRequest(name, email, password)).isSuccess.also {
                    if (it) registrationMode = false
                }
            }
        )
        return
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedProduct by remember { mutableStateOf<ProductVariant?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = { AppBar() },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { selectedProduct = state.products.firstOrNull() }) {
                Icon(Icons.Default.Add, contentDescription = "Registrar movimentação")
            }
        }
    ) { padding ->
        InventoryScreen(
            state = state,
            onSearch = viewModel::updateSearch,
            onProductClick = { selectedProduct = it },
            modifier = Modifier.padding(padding)
        )
    }
    selectedProduct?.let { product ->
        MovementDialog(
            product = product,
            onDismiss = { selectedProduct = null },
            onSave = { type, quantity, note ->
                viewModel.registerMovement(product.id, type, quantity, note)
                selectedProduct = null
            }
        )
    }
}

@Composable
private fun AuthenticationScreen(
    registrationMode: Boolean,
    onChangeMode: (Boolean) -> Unit,
    onLogin: (String, String) -> Boolean,
    onRegister: (String, String, String) -> Boolean
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    fun validEmail(value: String) = value.contains("@") && value.substringAfter("@", "").contains(".")
    fun submit() {
        if (!validEmail(email)) {
            message = "Informe um e-mail válido."
        } else if (password.length < 8) {
            message = "A senha deve ter ao menos 8 caracteres."
        } else if (registrationMode && name.isBlank()) {
            message = "Informe seu nome completo."
        } else if (registrationMode && password != confirmation) {
            message = "A confirmação da senha não confere."
        } else if (registrationMode) {
            message = if (onRegister(name, email, password)) {
                "Conta criada. Entre com seus dados."
            } else {
                "Já existe uma conta com este e-mail."
            }
        } else {
            if (!onLogin(email, password)) {
                message = "Credenciais não reconhecidas. Use a conta de demonstração ou crie uma conta."
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(46.dp))
        Spacer(Modifier.height(14.dp))
        Text("Estoque Praia", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(if (registrationMode) "Crie o acesso da gestora" else "Acesse o controle de estoque", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(28.dp))
        if (registrationMode) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome completo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        if (registrationMode) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = confirmation, onValueChange = { confirmation = it }, label = { Text("Confirmar senha") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        message?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = if (it.startsWith("Conta")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = ::submit, modifier = Modifier.fillMaxWidth()) {
            Text(if (registrationMode) "Criar conta" else "Entrar")
        }
        TextButton(
            onClick = {
                onChangeMode(!registrationMode)
                message = null
                password = ""
                confirmation = ""
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(if (registrationMode) "Já possui conta? Entrar" else "Não possui conta? Criar conta")
        }
        if (!registrationMode) {
            Text("Demonstração: gestora@estoquepraia.com / 12345678", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar() {
    TopAppBar(
        title = {
            Column {
                Text("Estoque Praia", fontWeight = FontWeight.Bold)
                Text("Moda praia · controle por variação", style = MaterialTheme.typography.labelSmall)
            }
        }
    )
}

@Composable
private fun InventoryScreen(
    state: InventoryUiState,
    onSearch: (String) -> Unit,
    onProductClick: (ProductVariant) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        item { Dashboard(state) }
        item {
            OutlinedTextField(
                value = state.search,
                onValueChange = onSearch,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                label = { Text("Buscar por produto, cor, tamanho ou código") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        if (state.lowStock.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WarningAmber, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text("Itens com estoque baixo", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (state.filteredProducts.isEmpty()) {
            item { EmptyState() }
        } else {
            items(state.filteredProducts, key = { it.id }) { product ->
                ProductCard(product, onClick = { onProductClick(product) })
            }
        }
        item { RecentMovements(state) }
    }
}

@Composable
private fun Dashboard(state: InventoryUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        MetricCard("Unidades", state.totalUnits.toString(), Modifier.weight(1f))
        MetricCard("Variações", state.products.size.toString(), Modifier.weight(1f))
        MetricCard("Alerta", state.lowStock.size.toString(), Modifier.weight(1f), isAlert = state.lowStock.isNotEmpty())
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier, isAlert: Boolean = false) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (isAlert) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ProductCard(product: ProductVariant, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.productName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${product.size} · ${product.color} · ${product.sku}", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${product.quantity} un.", fontWeight = FontWeight.Bold, color = if (product.isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                if (product.isLowStock) Text("mín. ${product.minimumQuantity}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun RecentMovements(state: InventoryUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("Movimentações recentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        state.movements.take(4).forEach { movement ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(movement.productLabel, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${movement.type.label} · ${movement.occurredAt}", style = MaterialTheme.typography.bodySmall)
                }
                Text("${if (movement.type == MovementType.ENTRY) "+" else "-"}${movement.quantity}", fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Inventory2, null, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(8.dp))
        Text("Nenhuma variação encontrada")
        Text("Ajuste sua busca ou cadastre um novo produto.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MovementDialog(product: ProductVariant, onDismiss: () -> Unit, onSave: (MovementType, String, String) -> Unit) {
    var type by remember { mutableStateOf(MovementType.ENTRY) }
    var quantity by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Movimentar estoque") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${product.productName} · ${product.size} · ${product.color}", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MovementType.entries.take(2).forEach { option ->
                        AssistChip(onClick = { type = option }, label = { Text(option.label) }, leadingIcon = if (type == option) ({ Icon(Icons.Default.Inventory2, null) }) else null)
                    }
                }
                OutlinedTextField(value = quantity, onValueChange = { quantity = it.filter(Char::isDigit) }, label = { Text("Quantidade") }, singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Motivo ou observação") })
            }
        },
        confirmButton = { Button(onClick = { onSave(type, quantity, note) }) { Text("Registrar") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancelar") } }
    )
}
