package br.ordnavile.spotter.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import kotlinx.coroutines.launch
import br.ordnavile.spotter.data.state.PaymentState
import br.ordnavile.spotter.viewmodel.MonilocViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.ordnavile.spotter.data.model.Veiculo
import br.ordnavile.spotter.utils.QrCodeUtil
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonilocScreen(viewModel: MonilocViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Moniloc Menu",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Início") },
                    selected = true,
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Configurações") },
                    selected = false,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚗 Moniloc Parking") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu Principal")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.setShowAddDialog(true) }) {
                Icon(Icons.Default.Add, contentDescription = "Nova Entrada")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PaddingValues(16.dp).let {
                OutlinedTextField(
                    value = uiState.filtro,
                    onValueChange = viewModel::onFiltroChanged,
                    label = { Text("Buscar placa...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            val filtrados = uiState.veiculos.filter {
                it.placa.lowercase().contains(uiState.filtro.lowercase())
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtrados) { veiculo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Placa: ${veiculo.placa}", style = MaterialTheme.typography.titleMedium)
                                Text(text = "Entrada: ${veiculo.entrada}", style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { viewModel.iniciarSaida(veiculo) }) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Saída", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
    }

    uiState.errorMessage?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { viewModel.setErrorMessage(null) },
            title = { Text("Erro") },
            text = { Text(errorMsg) },
            confirmButton = {
                TextButton(onClick = { viewModel.setErrorMessage(null) }) {
                    Text("OK")
                }
            }
        )
    }

    if (uiState.showAddDialog) {
        AddVeiculoDialog(
            onDismiss = { viewModel.setShowAddDialog(false) },
            onConfirm = viewModel::registrarEntrada
        )
    }

    when (val status = uiState.paymentStatus) {
        is PaymentState.Confirm -> {
            ConfirmExitDialog(
                veiculo = status.veiculo,
                valorTotal = status.valor,
                onDismiss = viewModel::cancelarSaida,
                onConfirm = { viewModel.confirmarSaidaEGerarPix(status.veiculo.placa, status.valor) }
            )
        }
        is PaymentState.AwaitingPayment -> {
            PixQrDialog(
                qrCodeText = status.qrCodeText,
                valor = status.valor,
                onDismiss = viewModel::cancelarSaida
            )
        }
        is PaymentState.Success -> {
            SuccessPaymentDialog(
                msg = "Pagamento confirmado! Carro liberado.",
                onDismiss = { viewModel.cancelarSaida() }
            )
        }
        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVeiculoDialog(modifier: Modifier = Modifier, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var placa by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Nova Entrada") },
        text = {
            Column {
                OutlinedTextField(
                    value = placa,
                    onValueChange = { placa = it.uppercase() },
                    label = { Text("Placa") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelo,
                    onValueChange = { modelo = it },
                    label = { Text("Modelo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(placa, modelo) }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Sair")
            }
        }
    )
}

@Composable
fun ConfirmExitDialog(modifier: Modifier = Modifier, veiculo: Veiculo, valorTotal: Double, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Saída") },
        text = {
            Text("Placa: ${veiculo.placa}\nValor: R$ ${"%.2f".format(valorTotal)}")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Pagar Pix")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Voltar")
            }
        }
    )
}

@Composable
fun PixQrDialog(modifier: Modifier = Modifier, qrCodeText: String, valor: Double, onDismiss: () -> Unit) {
    val bitmap = remember(qrCodeText) { QrCodeUtil.generateQrCode(qrCodeText, 512) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Pix - R$ ${"%.2f".format(valor)}") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Aguardando pagamento...")
                Spacer(modifier = Modifier.height(16.dp))
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "QR Code Pix",
                        modifier = Modifier.size(200.dp)
                    )
                } else {
                    Text("Erro ao gerar QR Code", color = Color.Red)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun SuccessPaymentDialog(modifier: Modifier = Modifier, msg: String, onDismiss: () -> Unit) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color.Green, modifier = Modifier.size(60.dp))
        },
        text = {
            Text(msg, fontSize = 18.sp, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
