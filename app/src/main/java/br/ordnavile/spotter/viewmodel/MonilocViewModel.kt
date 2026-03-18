package br.ordnavile.spotter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.ordnavile.spotter.data.model.Veiculo
import br.ordnavile.spotter.data.model.HistoricoVeiculo
import br.ordnavile.spotter.data.state.MonilocUiState
import br.ordnavile.spotter.data.state.PaymentState
import br.ordnavile.spotter.data.repository.MonilocRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import br.ordnavile.spotter.data.model.ConfiguracaoEstacionamento
import br.ordnavile.spotter.data.state.Screen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil


class MonilocViewModel(
    private val repository: MonilocRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonilocUiState())
    val uiState: StateFlow<MonilocUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val veiculosFiltrados: StateFlow<List<Veiculo>> = combine(
        repository.listarTodos(),
        _uiState
    ) { lista, state ->
        if (state.filtro.isBlank()) {
            lista
        } else {
            lista.filter { it.placa.lowercase().contains(state.filtro.lowercase()) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historico: StateFlow<List<HistoricoVeiculo>> = repository.listarHistorico()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- CORREÇÃO BUG 2: init com fluxo sequencial e persistência local ---
    init {
        viewModelScope.launch {
            try {
                // 1. Primeiro, carrega a configuração persistida localmente (SharedPreferences/DataStore)
                // Certifique-se de implementar este método no repositório.
                val configLocal = repository.carregarConfiguracaoLocal()
                if (configLocal != null) {
                    _uiState.update { it.copy(configuracao = configLocal) }
                }

                // 2. Agora é seguro buscar no Firestore, pois o idEstacionamento está correto
                carregarSaldoFirestore()

                // 3. Tenta recuperar dados da nuvem se o banco local estiver vazio
                val veiculosLocais = repository.listarTodos().first()
                if (veiculosLocais.isEmpty()) {
                    recuperarDadosFirebase()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun carregarSaldoFirestore() {
        try {
            val estacionamentoId = uiState.value.configuracao.idEstacionamento

            // Se o ID for vazio, não tenta buscar saldo para não criar lixo no Firestore
            if (estacionamentoId.isBlank()) return

            val saldoRemoto = repository.recuperarSaldoFirestore(estacionamentoId)
            if (saldoRemoto != null) {
                // Saldo encontrado no Firestore – usa o valor persistido
                val novaConfig = uiState.value.configuracao.copy(saldoCreditos = saldoRemoto)
                _uiState.update { it.copy(configuracao = novaConfig) }
            } else {
                // Primeiro acesso – persiste o saldo inicial de cortesia (10 créditos)
                val saldoInicial = uiState.value.configuracao.saldoCreditos
                repository.salvarSaldoFirestore(estacionamentoId, saldoInicial)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var isRecovering = false

    // --- CORREÇÃO BUG 3: Refatorado para suspend fun (sem coroutine interna solta) ---
    private suspend fun recuperarDadosFirebase() {
        if (isRecovering) return
        val estacionamentoId = uiState.value.configuracao.idEstacionamento

        // Proteção contra ID vazio
        if (estacionamentoId.isBlank()) return

        try {
            isRecovering = true
            _uiState.update { it.copy(showLoading = true) }

            val veiculosNuvem = repository.recuperarVeiculosFirestore(estacionamentoId)
            if (veiculosNuvem.isNotEmpty()) {
                repository.inserirVarios(veiculosNuvem)
            }

            val historicoNuvem = repository.recuperarHistoricoFirestore(estacionamentoId)
            if (historicoNuvem.isNotEmpty()) {
                repository.inserirVariosHistorico(historicoNuvem)
            }

            _uiState.update { it.copy(showLoading = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(showLoading = false) }
            e.printStackTrace()
        } finally {
            isRecovering = false
        }
    }

    fun onFiltroChanged(filtro: String) {
        _uiState.update { it.copy(filtro = filtro.uppercase()) }
    }

    // --- CORREÇÃO BUG 3: refreshData agora aguarda recuperarDadosFirebase() ---
    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            // Como agora é uma suspend fun, esta linha pausa a coroutine até terminar a busca
            recuperarDadosFirebase()

            // Pequeno delay apenas para o indicador visual ser perceptível se a rede for muito rápida
            delay(500)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun setErrorMessage(message: String?) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun setCurrentScreen(screen: Screen) {
        _uiState.update { state ->
            if (screen == Screen.Configuracao) {
                state.copy(
                    currentScreen = screen,
                    idEstacionamentoInput = state.configuracao.idEstacionamento,
                    nomeEstacionamentoInput = state.configuracao.nomeEstacionamento,
                    valorPrimeiraHoraInput = state.configuracao.valorPrimeiraHora.toString(),
                    valorHoraAdicionalInput = state.configuracao.valorHoraAdicional.toString(),
                    valorFixo12HorasInput = state.configuracao.valorFixo12Horas.toString(),
                    tempoToleranciaInput = state.configuracao.tempoToleranciaMinutos.toString(),
                    tokenMpInput = state.configuracao.tokenMercadoPago,
                    chavePixInput = state.configuracao.chavePix
                )
            } else {
                state.copy(currentScreen = screen)
            }
        }
    }

    fun onIdEstacionamentoInputChanged(id: String) {
        _uiState.update { it.copy(idEstacionamentoInput = id) }
    }

    fun onNomeEstacionamentoInputChanged(nome: String) {
        _uiState.update { it.copy(nomeEstacionamentoInput = nome) }
    }

    fun onValorPrimeiraHoraInputChanged(valor: String) {
        _uiState.update { it.copy(valorPrimeiraHoraInput = valor) }
    }

    fun onValorHoraAdicionalInputChanged(valor: String) {
        _uiState.update { it.copy(valorHoraAdicionalInput = valor) }
    }

    fun onValorFixo12HorasInputChanged(valor: String) {
        _uiState.update { it.copy(valorFixo12HorasInput = valor) }
    }

    fun onTempoToleranciaInputChanged(valor: String) {
        _uiState.update { it.copy(tempoToleranciaInput = valor) }
    }

    fun onTokenMpInputChanged(token: String) {
        _uiState.update { it.copy(tokenMpInput = token) }
    }

    fun onChavePixInputChanged(chave: String) {
        _uiState.update { it.copy(chavePixInput = chave) }
    }

    fun setShowMpTutorial(show: Boolean) {
        _uiState.update { it.copy(showMpTutorial = show) }
    }

    fun onQuantidadeCreditosChanged(quantidade: String) {
        _uiState.update { it.copy(quantidadeCreditosInput = quantidade) }
    }

    fun setShowCompraCreditosDialog(show: Boolean) {
        _uiState.update { it.copy(showCompraCreditosDialog = show) }
    }

    // --- CORREÇÃO BUG 1 & 2: Sincronização após salvar config e persistência local ---
    fun salvarConfiguracao() {
        val stateAtual = _uiState.value
        val idAntigo = stateAtual.configuracao.idEstacionamento
        val idNovo = stateAtual.idEstacionamentoInput

        val novaConfig = stateAtual.configuracao.copy(
            idEstacionamento = idNovo,
            nomeEstacionamento = stateAtual.nomeEstacionamentoInput,

            // Se estiver vazio ou inválido, agora assume 0.0 (zerado) em vez de puxar o antigo
            valorPrimeiraHora = stateAtual.valorPrimeiraHoraInput.toDoubleOrNull() ?: 0.0,
            valorHoraAdicional = stateAtual.valorHoraAdicionalInput.toDoubleOrNull() ?: 0.0,
            valorFixo12Horas = stateAtual.valorFixo12HorasInput.toDoubleOrNull() ?: 0.0,

            // A correção da tolerância: assume 0 (zero minutos) se o campo estiver vazio
            tempoToleranciaMinutos = stateAtual.tempoToleranciaInput.toIntOrNull() ?: 0,

            tokenMercadoPago = stateAtual.tokenMpInput,
            chavePix = stateAtual.chavePixInput
        )

//        val novaConfig = stateAtual.configuracao.copy(
//            idEstacionamento = idNovo,
//            nomeEstacionamento = stateAtual.nomeEstacionamentoInput,
//            valorPrimeiraHora = stateAtual.valorPrimeiraHoraInput.toDoubleOrNull() ?: stateAtual.configuracao.valorPrimeiraHora,
//            valorHoraAdicional = stateAtual.valorHoraAdicionalInput.toDoubleOrNull() ?: stateAtual.configuracao.valorHoraAdicional,
//            valorFixo12Horas = stateAtual.valorFixo12HorasInput.toDoubleOrNull() ?: stateAtual.configuracao.valorFixo12Horas,
//            tempoToleranciaMinutos = stateAtual.tempoToleranciaInput.toIntOrNull() ?: stateAtual.configuracao.tempoToleranciaMinutos,
//            tokenMercadoPago = stateAtual.tokenMpInput,
//            chavePix = stateAtual.chavePixInput
//        )

        _uiState.update { it.copy(configuracao = novaConfig, currentScreen = Screen.Home) }

        viewModelScope.launch {
            // Persiste a configuração localmente para não perder ao reiniciar o app (Bug 2)
            // Certifique-se de implementar este método no repositório.
            repository.salvarConfiguracaoLocal(novaConfig)

            // Sincroniza com o Firestore se o ID do estacionamento mudou (Bug 1)
            if (idAntigo != idNovo && idNovo.isNotBlank()) {
                carregarSaldoFirestore()
                recuperarDadosFirebase()
            }
        }
    }

    fun setShowAddDialog(show: Boolean) {
        _uiState.update { it.copy(showAddDialog = show, novaPlaca = "", novoModelo = "") }
    }

    fun onNovaPlacaChanged(placa: String) {
        _uiState.update { it.copy(novaPlaca = placa.uppercase()) }
    }

    fun onNovoModeloChanged(modelo: String) {
        _uiState.update { it.copy(novoModelo = modelo.uppercase()) }
    }

    fun registrarEntrada(valorFixo: Double? = null) {
        val state = uiState.value
        val placa = state.novaPlaca
        val modelo = state.novoModelo
        if (placa.isBlank()) return

        if (state.configuracao.saldoCreditos < state.configuracao.custoPorEntrada) {
            setErrorMessage("Saldo insuficiente! Adicione créditos para continuar usando o aplicativo.")
            return
        }

        val horaAtual = timeFormat.format(Date())
        val entradaUnix = System.currentTimeMillis()

        viewModelScope.launch {
            val existente = repository.buscarPorPlaca(placa)
            if (existente != null) {
                setErrorMessage("Este veículo já está no estacionamento!")
                return@launch
            }

            setShowAddDialog(false)

            repository.inserir(
                Veiculo(placa = placa, modelo = modelo, entrada = horaAtual, valorFixo = valorFixo, entradaUnix = entradaUnix),
                estacionamentoId = state.configuracao.idEstacionamento
            )
            repository.registrarEntradaRemota(placa)

            val novoSaldo = state.configuracao.saldoCreditos - state.configuracao.custoPorEntrada
            val novaConfig = state.configuracao.copy(saldoCreditos = novoSaldo)
            _uiState.update { it.copy(configuracao = novaConfig) }
            repository.salvarSaldoFirestore(state.configuracao.idEstacionamento, novoSaldo)
        }
    }

    fun iniciarSaida(veiculo: Veiculo) {
        val agora = Calendar.getInstance()

        val minutos: Long = if (veiculo.entradaUnix > 0L) {
            val elapsed = agora.timeInMillis - veiculo.entradaUnix
            if (elapsed < 0) 0L else elapsed / 60000L
        } else {
            val entrada = Calendar.getInstance().apply {
                time = timeFormat.parse(veiculo.entrada) ?: Date()
                set(Calendar.YEAR, agora.get(Calendar.YEAR))
                set(Calendar.MONTH, agora.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, agora.get(Calendar.DAY_OF_MONTH))
            }
            val elapsed = agora.timeInMillis - entrada.timeInMillis
            if (elapsed < 0) 0L else elapsed / 60000L
        }

        val config = uiState.value.configuracao
        val valorTotal = veiculo.valorFixo ?: calcularValor(minutos, config)

        _uiState.update { it.copy(paymentStatus = PaymentState.Confirm(veiculo, valorTotal)) }
    }

    private fun calcularValor(minutos: Long, config: ConfiguracaoEstacionamento): Double {
        if (minutos <= config.tempoToleranciaMinutos) return 0.0

        if (minutos >= 12 * 60) return config.valorFixo12Horas

        if (minutos <= 60) return config.valorPrimeiraHora

        val horasAdicionais = ceil((minutos - 60) / 60.0).toInt()
        val valor = config.valorPrimeiraHora + (horasAdicionais * config.valorHoraAdicional)

        return valor.coerceAtMost(config.valorFixo12Horas)
    }

    fun cancelarSaida() {
        pollJob?.cancel()
        _uiState.update { it.copy(paymentStatus = PaymentState.Idle) }
    }

    fun confirmarSaidaEGerarPix(placa: String, valor: Double) {
        if (valor <= 0.0) {
            viewModelScope.launch {
                processarSucessoPagamento(placa, "Saída liberada! (Dentro da tolerância)")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(showLoading = true) }
            val config = uiState.value.configuracao
            val isCredito = placa.startsWith("CREDITOS_")
            val result = repository.gerarPix(
                placa = placa,
                valor = valor,
                token = if (isCredito) null else config.tokenMercadoPago.ifBlank { null },
                chavePix = if (isCredito) null else config.chavePix.ifBlank { null }
            )
            val veiculo = (uiState.value.paymentStatus as? PaymentState.Confirm)?.veiculo
            result.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        showLoading = false,
                        paymentStatus = PaymentState.AwaitingPayment(
                            response.qrCode!!, valor, response.idPagamento!!.toString(), placa, veiculo ?: Veiculo(placa = placa)
                        )
                    )
                }
                iniciarPollingPagamento(response.idPagamento.toString(), placa)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(showLoading = false, paymentStatus = PaymentState.Idle, errorMessage = error.message)
                }
            }
        }
    }

    private fun iniciarPollingPagamento(idPagamento: String, placa: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                try {
                    val isPago = repository.verificarPagamento(idPagamento)
                    if (isPago) {
                        processarSucessoPagamento(placa, "Pagamento confirmado! Carro liberado.")
                        break
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun processarSucessoPagamento(placa: String, message: String) {
        var displayMessage = message
        if (placa.startsWith("CREDITOS_")) {
            val qtd = placa.removePrefix("CREDITOS_").toIntOrNull() ?: 0
            val valor = qtd * 0.10
            val novoSaldo = uiState.value.configuracao.saldoCreditos + qtd
            val novaConfig = uiState.value.configuracao.copy(saldoCreditos = novoSaldo)
            _uiState.update { it.copy(configuracao = novaConfig) }
            repository.salvarSaldoFirestore(uiState.value.configuracao.idEstacionamento, novoSaldo)
            displayMessage = "Recarga concluída! Adicionado $qtd créditos ao seu saldo.\nValor: R$ ${"%.2f".format(valor)}"
        } else {
            val payment = uiState.value.paymentStatus
            val veiculoParaHistorico = when (payment) {
                is PaymentState.Confirm -> payment.veiculo
                is PaymentState.AwaitingPayment -> payment.veiculo
                else -> null
            }

            if (veiculoParaHistorico != null) {
                val historico = HistoricoVeiculo(
                    placa = veiculoParaHistorico.placa,
                    modelo = veiculoParaHistorico.modelo,
                    entrada = veiculoParaHistorico.entrada,
                    saida = timeFormat.format(Date()),
                    valorPago = (payment as? PaymentState.Confirm)?.valor
                        ?: (payment as? PaymentState.AwaitingPayment)?.valor ?: 0.0
                )
                repository.inserirHistorico(historico, uiState.value.configuracao.idEstacionamento)
            }
        }

        repository.deletarPorPlaca(placa, uiState.value.configuracao.idEstacionamento)
        _uiState.update { it.copy(paymentStatus = PaymentState.Success(displayMessage)) }
    }

    fun comprarCreditos() {
        _uiState.update { it.copy(showCompraCreditosDialog = true, quantidadeCreditosInput = "100") }
    }

    fun confirmarCompraCreditos() {
        val qtd = uiState.value.quantidadeCreditosInput.toIntOrNull() ?: 0
        if (qtd <= 0) return

        val valor = qtd * 0.10
        val placa = "CREDITOS_$qtd"

        setShowCompraCreditosDialog(false)

        viewModelScope.launch {
            repository.registrarEntradaRemota(placa)
            confirmarSaidaEGerarPix(placa, valor)
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}





//class MonilocViewModel(
//    private val repository: MonilocRepository
//) : ViewModel() {
//
//    private val _uiState = MutableStateFlow(MonilocUiState())
//    val uiState: StateFlow<MonilocUiState> = _uiState.asStateFlow()
//
//    private var pollJob: Job? = null
//    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
//
//    val veiculosFiltrados: StateFlow<List<Veiculo>> = combine(
//        repository.listarTodos(),
//        _uiState
//    ) { lista, state ->
//        if (state.filtro.isBlank()) {
//            lista
//        } else {
//            lista.filter { it.placa.lowercase().contains(state.filtro.lowercase()) }
//        }
//    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
//
//    val historico: StateFlow<List<HistoricoVeiculo>> = repository.listarHistorico()
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
//
//    init {
//        viewModelScope.launch {
//            // Carrega saldo de créditos persistido no Firestore
//            carregarSaldoFirestore()
//        }
//        viewModelScope.launch {
//            // Tenta recuperar dados da nuvem se o banco local estiver vazio (pós limpeza de cache/reinstalação)
//            try {
//                val veiculosLocais = repository.listarTodos().first()
//                if (veiculosLocais.isEmpty()) {
//                    recuperarDadosFirebase()
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    private suspend fun carregarSaldoFirestore() {
//        try {
//            val estacionamentoId = uiState.value.configuracao.idEstacionamento
//            val saldoRemoto = repository.recuperarSaldoFirestore(estacionamentoId)
//            if (saldoRemoto != null) {
//                // Saldo encontrado no Firestore – usa o valor persistido
//                val novaConfig = uiState.value.configuracao.copy(saldoCreditos = saldoRemoto)
//                _uiState.update { it.copy(configuracao = novaConfig) }
//            } else {
//                // Primeiro acesso – persiste o saldo inicial de cortesia (10 créditos)
//                val saldoInicial = uiState.value.configuracao.saldoCreditos
//                repository.salvarSaldoFirestore(estacionamentoId, saldoInicial)
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    private var isRecovering = false
//
//    private fun recuperarDadosFirebase() {
//        if (isRecovering) return
//        val estacionamentoId = uiState.value.configuracao.idEstacionamento
//        viewModelScope.launch {
//            try {
//                isRecovering = true
//                _uiState.update { it.copy(showLoading = true) }
//
//                val veiculosNuvem = repository.recuperarVeiculosFirestore(estacionamentoId)
//                if (veiculosNuvem.isNotEmpty()) {
//                    repository.inserirVarios(veiculosNuvem)
//                }
//
//                val historicoNuvem = repository.recuperarHistoricoFirestore(estacionamentoId)
//                if (historicoNuvem.isNotEmpty()) {
//                    repository.inserirVariosHistorico(historicoNuvem)
//                }
//
//                _uiState.update { it.copy(showLoading = false) }
//            } catch (e: Exception) {
//                _uiState.update { it.copy(showLoading = false) }
//                e.printStackTrace()
//            } finally {
//                isRecovering = false
//            }
//        }
//    }
//
//    fun onFiltroChanged(filtro: String) {
//        _uiState.update { it.copy(filtro = filtro.uppercase()) }
//    }
//
//    fun refreshData() {
//        viewModelScope.launch {
//            _uiState.update { it.copy(isRefreshing = true) }
//            recuperarDadosFirebase()
//            // Pequeno delay apenas para o indicador visual ser perceptível se a rede for muito rápida
//            delay(500)
//            _uiState.update { it.copy(isRefreshing = false) }
//        }
//    }
//
//    fun setErrorMessage(message: String?) {
//        _uiState.update { it.copy(errorMessage = message) }
//    }
//
//    fun setCurrentScreen(screen: Screen) {
//        _uiState.update { state ->
//            if (screen == Screen.Configuracao) {
//                state.copy(
//                    currentScreen = screen,
//                    idEstacionamentoInput = state.configuracao.idEstacionamento,
//                    nomeEstacionamentoInput = state.configuracao.nomeEstacionamento,
//                    valorPrimeiraHoraInput = state.configuracao.valorPrimeiraHora.toString(),
//                    valorHoraAdicionalInput = state.configuracao.valorHoraAdicional.toString(),
//                    valorFixo12HorasInput = state.configuracao.valorFixo12Horas.toString(),
//                    tempoToleranciaInput = state.configuracao.tempoToleranciaMinutos.toString(),
//                    tokenMpInput = state.configuracao.tokenMercadoPago,
//                    chavePixInput = state.configuracao.chavePix
//                )
//            } else {
//                state.copy(currentScreen = screen)
//            }
//        }
//    }
//
//    fun onIdEstacionamentoInputChanged(id: String) {
//        _uiState.update { it.copy(idEstacionamentoInput = id) }
//    }
//
//    fun onNomeEstacionamentoInputChanged(nome: String) {
//        _uiState.update { it.copy(nomeEstacionamentoInput = nome) }
//    }
//
//    fun onValorPrimeiraHoraInputChanged(valor: String) {
//        _uiState.update { it.copy(valorPrimeiraHoraInput = valor) }
//    }
//
//    fun onValorHoraAdicionalInputChanged(valor: String) {
//        _uiState.update { it.copy(valorHoraAdicionalInput = valor) }
//    }
//
//    fun onValorFixo12HorasInputChanged(valor: String) {
//        _uiState.update { it.copy(valorFixo12HorasInput = valor) }
//    }
//
//    fun onTempoToleranciaInputChanged(valor: String) {
//        _uiState.update { it.copy(tempoToleranciaInput = valor) }
//    }
//
//    fun onTokenMpInputChanged(token: String) {
//        _uiState.update { it.copy(tokenMpInput = token) }
//    }
//
//    fun onChavePixInputChanged(chave: String) {
//        _uiState.update { it.copy(chavePixInput = chave) }
//    }
//
//    fun setShowMpTutorial(show: Boolean) {
//        _uiState.update { it.copy(showMpTutorial = show) }
//    }
//
//    fun onQuantidadeCreditosChanged(quantidade: String) {
//        _uiState.update { it.copy(quantidadeCreditosInput = quantidade) }
//    }
//
//    fun setShowCompraCreditosDialog(show: Boolean) {
//        _uiState.update { it.copy(showCompraCreditosDialog = show) }
//    }
//
//    fun salvarConfiguracao() {
//        _uiState.update { state ->
//            val novaConfig = state.configuracao.copy(
//                idEstacionamento = state.idEstacionamentoInput,
//                nomeEstacionamento = state.nomeEstacionamentoInput,
//                valorPrimeiraHora = state.valorPrimeiraHoraInput.toDoubleOrNull() ?: state.configuracao.valorPrimeiraHora,
//                valorHoraAdicional = state.valorHoraAdicionalInput.toDoubleOrNull() ?: state.configuracao.valorHoraAdicional,
//                valorFixo12Horas = state.valorFixo12HorasInput.toDoubleOrNull() ?: state.configuracao.valorFixo12Horas,
//                tempoToleranciaMinutos = state.tempoToleranciaInput.toIntOrNull() ?: state.configuracao.tempoToleranciaMinutos,
//                tokenMercadoPago = state.tokenMpInput,
//                chavePix = state.chavePixInput
//            )
//            state.copy(configuracao = novaConfig, currentScreen = Screen.Home)
//        }
//    }
//
//    fun setShowAddDialog(show: Boolean) {
//        _uiState.update { it.copy(showAddDialog = show, novaPlaca = "", novoModelo = "") }
//    }
//
//    fun onNovaPlacaChanged(placa: String) {
//        _uiState.update { it.copy(novaPlaca = placa.uppercase()) }
//    }
//
//    fun onNovoModeloChanged(modelo: String) {
//        _uiState.update { it.copy(novoModelo = modelo.uppercase()) }
//    }
//
//    fun registrarEntrada(valorFixo: Double? = null) {
//        val state = uiState.value
//        val placa = state.novaPlaca
//        val modelo = state.novoModelo
//        if (placa.isBlank()) return
//
//        // Validação de Saldo
//        if (state.configuracao.saldoCreditos < state.configuracao.custoPorEntrada) {
//            setErrorMessage("Saldo insuficiente! Adicione créditos para continuar usando o aplicativo.")
//            return
//        }
//
//        val horaAtual = timeFormat.format(Date())
//        val entradaUnix = System.currentTimeMillis()
//
//        viewModelScope.launch {
//            val existente = repository.buscarPorPlaca(placa)
//            if (existente != null) {
//                setErrorMessage("Este veículo já está no estacionamento!")
//                return@launch
//            }
//
//            // Fechar o diálogo imediatamente para melhor UX
//            setShowAddDialog(false)
//
//            // As operações de rede e banco ocorrem em segundo plano
//            repository.inserir(
//                Veiculo(placa = placa, modelo = modelo, entrada = horaAtual, valorFixo = valorFixo, entradaUnix = entradaUnix),
//                estacionamentoId = state.configuracao.idEstacionamento
//            )
//            repository.registrarEntradaRemota(placa)
//
//            // Debitar Saldo e persistir no Firestore
//            val novoSaldo = state.configuracao.saldoCreditos - state.configuracao.custoPorEntrada
//            val novaConfig = state.configuracao.copy(saldoCreditos = novoSaldo)
//            _uiState.update { it.copy(configuracao = novaConfig) }
//            repository.salvarSaldoFirestore(state.configuracao.idEstacionamento, novoSaldo)
//        }
//    }
//
//    fun iniciarSaida(veiculo: Veiculo) {
//        val agora = Calendar.getInstance()
//
//        // Bug fix: usa entradaUnix (epoch ms) quando disponível para cálculo correto em
//        // cenários overnight ou com app fechado por dias. Fallback para HH:mm em registros antigos.
//        val minutos: Long = if (veiculo.entradaUnix > 0L) {
//            val elapsed = agora.timeInMillis - veiculo.entradaUnix
//            if (elapsed < 0) 0L else elapsed / 60000L
//        } else {
//            val entrada = Calendar.getInstance().apply {
//                time = timeFormat.parse(veiculo.entrada) ?: Date()
//                set(Calendar.YEAR, agora.get(Calendar.YEAR))
//                set(Calendar.MONTH, agora.get(Calendar.MONTH))
//                set(Calendar.DAY_OF_MONTH, agora.get(Calendar.DAY_OF_MONTH))
//            }
//            val elapsed = agora.timeInMillis - entrada.timeInMillis
//            if (elapsed < 0) 0L else elapsed / 60000L
//        }
//
//        val config = uiState.value.configuracao
//        val valorTotal = veiculo.valorFixo ?: calcularValor(minutos, config)
//
//        _uiState.update { it.copy(paymentStatus = PaymentState.Confirm(veiculo, valorTotal)) }
//    }
//
//    private fun calcularValor(minutos: Long, config: ConfiguracaoEstacionamento): Double {
//        if (minutos <= config.tempoToleranciaMinutos) return 0.0
//
//        // Se passar de 12 horas, valor fixo
//        if (minutos >= 12 * 60) return config.valorFixo12Horas
//
//        if (minutos <= 60) return config.valorPrimeiraHora
//
//        val horasAdicionais = ceil((minutos - 60) / 60.0).toInt()
//        val valor = config.valorPrimeiraHora + (horasAdicionais * config.valorHoraAdicional)
//
//        // Garante que não ultrapasse o valor de 12h
//        return valor.coerceAtMost(config.valorFixo12Horas)
//    }
//
//    fun cancelarSaida() {
//        pollJob?.cancel()
//        _uiState.update { it.copy(paymentStatus = PaymentState.Idle) }
//    }
//
//    fun confirmarSaidaEGerarPix(placa: String, valor: Double) {
//        if (valor <= 0.0) {
//            viewModelScope.launch {
//                processarSucessoPagamento(placa, "Saída liberada! (Dentro da tolerância)")
//            }
//            return
//        }
//
//        viewModelScope.launch {
//            _uiState.update { it.copy(showLoading = true) }
//            val config = uiState.value.configuracao
//            val isCredito = placa.startsWith("CREDITOS_")
//            val result = repository.gerarPix(
//                placa = placa,
//                valor = valor,
//                token = if (isCredito) null else config.tokenMercadoPago.ifBlank { null },
//                chavePix = if (isCredito) null else config.chavePix.ifBlank { null }
//            )
//            val veiculo = (uiState.value.paymentStatus as? PaymentState.Confirm)?.veiculo
//            result.onSuccess { response ->
//                _uiState.update {
//                    it.copy(
//                        showLoading = false,
//                        paymentStatus = PaymentState.AwaitingPayment(
//                            response.qrCode!!, valor, response.idPagamento!!.toString(), placa, veiculo ?: Veiculo(placa = placa)
//                        )
//                    )
//                }
//                iniciarPollingPagamento(response.idPagamento.toString(), placa)
//            }.onFailure { error ->
//                _uiState.update {
//                    it.copy(showLoading = false, paymentStatus = PaymentState.Idle, errorMessage = error.message)
//                }
//            }
//        }
//    }
//
//    private fun iniciarPollingPagamento(idPagamento: String, placa: String) {
//        pollJob?.cancel()
//        pollJob = viewModelScope.launch {
//            while (true) {
//                delay(3000)
//                try {
//                    val isPago = repository.verificarPagamento(idPagamento)
//                    if (isPago) {
//                        processarSucessoPagamento(placa, "Pagamento confirmado! Carro liberado.")
//                        break
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
//        }
//    }
//
//    private suspend fun processarSucessoPagamento(placa: String, message: String) {
//        var displayMessage = message
//        if (placa.startsWith("CREDITOS_")) {
//            val qtd = placa.removePrefix("CREDITOS_").toIntOrNull() ?: 0
//            val valor = qtd * 0.10
//            val novoSaldo = uiState.value.configuracao.saldoCreditos + qtd
//            val novaConfig = uiState.value.configuracao.copy(saldoCreditos = novoSaldo)
//            _uiState.update { it.copy(configuracao = novaConfig) }
//            // Persiste saldo atualizado no Firestore
//            repository.salvarSaldoFirestore(uiState.value.configuracao.idEstacionamento, novoSaldo)
//            displayMessage = "Recarga concluída! Adicionado $qtd créditos ao seu saldo.\nValor: R$ ${"%.2f".format(valor)}"
//        } else {
//            // Salvar no Histórico para o fluxo de PIX
//            val payment = uiState.value.paymentStatus
//            val veiculoParaHistorico = when (payment) {
//                is PaymentState.Confirm -> payment.veiculo
//                is PaymentState.AwaitingPayment -> payment.veiculo
//                else -> null
//            }
//
//            if (veiculoParaHistorico != null) {
//                val historico = HistoricoVeiculo(
//                    placa = veiculoParaHistorico.placa,
//                    modelo = veiculoParaHistorico.modelo,
//                    entrada = veiculoParaHistorico.entrada,
//                    saida = timeFormat.format(Date()),
//                    valorPago = (payment as? PaymentState.Confirm)?.valor
//                        ?: (payment as? PaymentState.AwaitingPayment)?.valor ?: 0.0
//                )
//                repository.inserirHistorico(historico, uiState.value.configuracao.idEstacionamento)
//            }
//        }
//
//        // Em ambos os casos de sucesso (Crédito ou Carro), deletamos a placa
//        repository.deletarPorPlaca(placa, uiState.value.configuracao.idEstacionamento)
//        _uiState.update { it.copy(paymentStatus = PaymentState.Success(displayMessage)) }
//    }
//
//    fun comprarCreditos() {
//        _uiState.update { it.copy(showCompraCreditosDialog = true, quantidadeCreditosInput = "100") }
//    }
//
//    fun confirmarCompraCreditos() {
//        val qtd = uiState.value.quantidadeCreditosInput.toIntOrNull() ?: 0
//        if (qtd <= 0) return
//
//        // Exemplo: R$ 0,10 por crédito
//        val valor = qtd * 0.10
//        val placa = "CREDITOS_$qtd"
//
//        setShowCompraCreditosDialog(false)
//
//        viewModelScope.launch {
//            // Registra a "entrada" da placa de créditos para que o backend a encontre
//            repository.registrarEntradaRemota(placa)
//            confirmarSaidaEGerarPix(placa, valor)
//        }
//    }
//
//    override fun onCleared() {
//        pollJob?.cancel()
//        super.onCleared()
//    }
//}
