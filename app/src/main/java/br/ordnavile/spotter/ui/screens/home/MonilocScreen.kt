package br.ordnavile.spotter.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.ordnavile.spotter.data.model.Veiculo
import br.ordnavile.spotter.data.state.MonilocUiState
import br.ordnavile.spotter.data.state.PaymentState
import br.ordnavile.spotter.data.state.Screen
import br.ordnavile.spotter.utils.QrCodeUtil
import br.ordnavile.spotter.viewmodel.MonilocViewModel
import org.koin.androidx.compose.koinViewModel

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonilocScreen(
    modifier: Modifier = Modifier,
    viewModel: MonilocViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(uiState.configuracao.nomeEstacionamento)
                        Text(
                            "Saldo: ${uiState.configuracao.saldoCreditos} cred.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
//                actions = {
//                    if (uiState.currentScreen == Screen.Configuracao) {
//                        IconButton(onClick = viewModel::salvarConfiguracao) {
//                            Icon(Icons.Default.Check, contentDescription = "Salvar")
//                        }
//                    }
//                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.currentScreen == Screen.Home,
                    onClick = { viewModel.setCurrentScreen(Screen.Home) },
                    label = { Text("Início") },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.setShowAddDialog(true) },
                    label = { Text("Adicionar") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Nova Entrada") }
                )
                NavigationBarItem(
                    selected = uiState.currentScreen == Screen.Dashboard,
                    onClick = { viewModel.setCurrentScreen(Screen.Dashboard) },
                    label = { Text("Dashboard") },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }
                )
                NavigationBarItem(
                    selected = uiState.currentScreen == Screen.Configuracao,
                    onClick = { viewModel.setCurrentScreen(Screen.Configuracao) },
                    label = { Text("Config") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (uiState.currentScreen) {
                Screen.Home -> {
                    HomeScreenContent(uiState = uiState, viewModel = viewModel)
                }
                Screen.Dashboard -> {
                    val historico by viewModel.historico.collectAsStateWithLifecycle()
                    DashboardScreen(
                        historico = historico,
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::refreshData
                    )
                }
                Screen.Configuracao -> {
                    ConfiguracaoScreen(
                        idEstacionamentoInput = uiState.idEstacionamentoInput,
                        nomeInput = uiState.nomeEstacionamentoInput,
                        primeiraHoraInput = uiState.valorPrimeiraHoraInput,
                        horaAdicionalInput = uiState.valorHoraAdicionalInput,
                        fixo12hInput = uiState.valorFixo12HorasInput,
                        toleranciaInput = uiState.tempoToleranciaInput,
                        tokenMpInput = uiState.tokenMpInput,
                        chavePixInput = uiState.chavePixInput,
                        saldo = uiState.configuracao.saldoCreditos,
                        showTutorial = uiState.showMpTutorial,
                        onIdEstacionamentoChange = viewModel::onIdEstacionamentoInputChanged,
                        onNomeChange = viewModel::onNomeEstacionamentoInputChanged,
                        onPrimeiraHoraChange = viewModel::onValorPrimeiraHoraInputChanged,
                        onHoraAdicionalChange = viewModel::onValorHoraAdicionalInputChanged,
                        onFixo12hChange = viewModel::onValorFixo12HorasInputChanged,
                        onToleranciaChange = viewModel::onTempoToleranciaInputChanged,
                        onTokenMpChange = viewModel::onTokenMpInputChanged,
                        onChavePixChange = viewModel::onChavePixInputChanged,
                        onShowTutorial = viewModel::setShowMpTutorial,
                        onComprarCreditos = viewModel::comprarCreditos,
                        onSave = viewModel::salvarConfiguracao
                    )
                }
            }
        }
    }

    // Diálogos devem ficar dentro do MonilocScreen para acessar uiState e viewModel
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
            placa = uiState.novaPlaca,
            modelo = uiState.novoModelo,
            valorFixo12h = uiState.configuracao.valorFixo12Horas,
            onPlacaChange = viewModel::onNovaPlacaChanged,
            onModeloChange = viewModel::onNovoModeloChanged,
            onDismiss = { viewModel.setShowAddDialog(false) },
            onConfirm = { valor -> viewModel.registrarEntrada(valor) }
        )
    }

    if (uiState.showCompraCreditosDialog) {
        CompraCreditosDialog(
            quantidade = uiState.quantidadeCreditosInput,
            onQuantidadeChange = viewModel::onQuantidadeCreditosChanged,
            onDismiss = { viewModel.setShowCompraCreditosDialog(false) },
            onConfirm = viewModel::confirmarCompraCreditos
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
                msg = status.message,
                onDismiss = { viewModel.cancelarSaida() }
            )
        }
        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: MonilocUiState,
    viewModel: MonilocViewModel
) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = viewModel::refreshData
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        
        OutlinedTextField(
            value = uiState.filtro,
            onValueChange = viewModel::onFiltroChanged,
            label = { Text("Buscar placa...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        val filtrados by viewModel.veiculosFiltrados.collectAsStateWithLifecycle()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtrados) { veiculo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            if (veiculo.modelo.isNotBlank()) {
                                Text(text = "Modelo: ${veiculo.modelo}", style = MaterialTheme.typography.bodySmall)
                            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVeiculoDialog(
    modifier: Modifier = Modifier,
    placa: String,
    modelo: String,
    valorFixo12h: Double,
    onPlacaChange: (String) -> Unit,
    onModeloChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Double?) -> Unit
) {

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Nova Entrada") },
        text = {
            Column {
                Text(
                    text = "* Campos obrigatórios",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = placa,
                    onValueChange = { if (it.length <= 7) onPlacaChange(it.uppercase()) },
                    label = { Text("Placa *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        if (placa.length == 7) {
                            Text("Máximo de 7 caracteres atingido", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelo,
                    onValueChange = onModeloChange,
                    label = { Text("Modelo *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onConfirm(valorFixo12h) },
                    enabled = placa.isNotBlank() && modelo.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Fixo 12h", maxLines = 1)
                }
                Button(
                    onClick = { onConfirm(null) },
                    enabled = placa.isNotBlank() && modelo.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Confirmar", maxLines = 1)
                }
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
                Text(if (valorTotal > 0.0) "Pagar Pix" else "Liberar Saída")
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
    val clipboardManager = LocalClipboardManager.current
    var copiado by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Pix - R$ ${"%.2f".format(valor)}") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
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

                // --- Pix copia e cola ---
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pix copia e cola:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = qrCodeText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(qrCodeText))
                        copiado = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (copiado) "Copiado! ✓" else "Copiar código")
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

@Composable
fun CompraCreditosDialog(
    quantidade: String,
    onQuantidadeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Créditos") },
        text = {
            Column {
                Text("Quanto créditos você deseja comprar?")
                Text("Valor: R$ 0,10 por veículo", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = quantidade,
                    onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) onQuantidadeChange(it) },
                    label = { Text("Quantidade de Veículos") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                val valor = (quantidade.toDoubleOrNull() ?: 0.0) * 0.10
                Text(
                    "Total: R$ ${"%.2f".format(valor)}",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Gerar PIX")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
