package br.ordnavile.spotter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.ordnavile.spotter.data.model.Veiculo
import br.ordnavile.spotter.data.state.MonilocUiState
import br.ordnavile.spotter.data.state.PaymentState
import br.ordnavile.spotter.data.repository.MonilocRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    init {
        carregarVeiculos()
    }

    private fun carregarVeiculos() {
        viewModelScope.launch {
            val lista = repository.listarTodos()
            _uiState.update { it.copy(veiculos = lista) }
        }
    }

    fun onFiltroChanged(filtro: String) {
        _uiState.update { it.copy(filtro = filtro) }
    }

    fun setErrorMessage(message: String?) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun setCurrentScreen(screen: Screen) {
        _uiState.update { state ->
            if (screen == Screen.Configuracao) {
                state.copy(
                    currentScreen = screen,
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

    fun salvarConfiguracao() {
        _uiState.update { state ->
            val novaConfig = state.configuracao.copy(
                nomeEstacionamento = state.nomeEstacionamentoInput,
                valorPrimeiraHora = state.valorPrimeiraHoraInput.toDoubleOrNull() ?: state.configuracao.valorPrimeiraHora,
                valorHoraAdicional = state.valorHoraAdicionalInput.toDoubleOrNull() ?: state.configuracao.valorHoraAdicional,
                valorFixo12Horas = state.valorFixo12HorasInput.toDoubleOrNull() ?: state.configuracao.valorFixo12Horas,
                tempoToleranciaMinutos = state.tempoToleranciaInput.toIntOrNull() ?: state.configuracao.tempoToleranciaMinutos,
                tokenMercadoPago = state.tokenMpInput,
                chavePix = state.chavePixInput
            )
            state.copy(configuracao = novaConfig, currentScreen = Screen.Home)
        }
    }

    fun setShowAddDialog(show: Boolean) {
        _uiState.update { it.copy(showAddDialog = show, novaPlaca = "", novoModelo = "") }
    }

    fun onNovaPlacaChanged(placa: String) {
        _uiState.update { it.copy(novaPlaca = placa.uppercase()) }
    }

    fun onNovoModeloChanged(modelo: String) {
        _uiState.update { it.copy(novoModelo = modelo) }
    }

    fun registrarEntrada() {
        val state = uiState.value
        val placa = state.novaPlaca
        val modelo = state.novoModelo
        if (placa.isBlank()) return
        
        // Validação de Saldo
        if (state.configuracao.saldoCreditos < state.configuracao.custoPorEntrada) {
            setErrorMessage("Saldo insuficiente! Adicione créditos para continuar usando o aplicativo.")
            return
        }

        val horaAtual = timeFormat.format(Date())
        val novoVeiculo = Veiculo(placa = placa, modelo = modelo, entrada = horaAtual)
        
        viewModelScope.launch {
            repository.inserir(novoVeiculo)
            repository.registrarEntradaRemota(novoVeiculo.placa)
            
            // Debitar Saldo
            val novaConfig = state.configuracao.copy(
                saldoCreditos = state.configuracao.saldoCreditos - state.configuracao.custoPorEntrada
            )
            _uiState.update { it.copy(configuracao = novaConfig) }
            
            carregarVeiculos()
            setShowAddDialog(false)
        }
    }

    fun iniciarSaida(veiculo: Veiculo) {
        val agora = Calendar.getInstance()
        val entrada = Calendar.getInstance().apply {
            time = timeFormat.parse(veiculo.entrada) ?: Date()
            set(Calendar.YEAR, agora.get(Calendar.YEAR))
            set(Calendar.MONTH, agora.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, agora.get(Calendar.DAY_OF_MONTH))
        }

        val minutosRestantes = (agora.timeInMillis - entrada.timeInMillis) / 60000
        val minutos = if (minutosRestantes < 0) 0 else minutosRestantes
        
        val config = uiState.value.configuracao
        val valorTotal = calcularValor(minutos, config)

        _uiState.update { it.copy(paymentStatus = PaymentState.Confirm(veiculo, valorTotal)) }
    }

    private fun calcularValor(minutos: Long, config: ConfiguracaoEstacionamento): Double {
        if (minutos <= config.tempoToleranciaMinutos) return 0.0
        
        // Se passar de 12 horas, valor fixo
        if (minutos >= 12 * 60) return config.valorFixo12Horas

        if (minutos <= 60) return config.valorPrimeiraHora
        
        val horasAdicionais = ceil((minutos - 60) / 60.0).toInt()
        val valor = config.valorPrimeiraHora + (horasAdicionais * config.valorHoraAdicional)
        
        // Garante que não ultrapasse o valor de 12h
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
            result.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        showLoading = false,
                        paymentStatus = PaymentState.AwaitingPayment(
                            response.qrCode!!, valor, response.idPagamento!!.toString(), placa
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
                delay(3000) // 3 seconds
                val isPago = repository.verificarPagamento(idPagamento)
                if (isPago) {
                    processarSucessoPagamento(placa, "Pagamento confirmado! Carro liberado.")
                    break
                }
            }
        }
    }

    private suspend fun processarSucessoPagamento(placa: String, message: String) {
        var displayMessage = message
        if (placa.startsWith("CREDITOS_")) {
            val qtd = placa.removePrefix("CREDITOS_").toIntOrNull() ?: 0
            val valor = qtd * 0.10
            val novaConfig = uiState.value.configuracao.copy(
                saldoCreditos = uiState.value.configuracao.saldoCreditos + qtd
            )
            _uiState.update { it.copy(configuracao = novaConfig) }
            displayMessage = "Recarga concluída! Adicionado $qtd créditos ao seu saldo.\nValor: R$ ${"%.2f".format(valor)}"
        } else {
            repository.deletarPorPlaca(placa)
        }
        carregarVeiculos()
        _uiState.update { it.copy(paymentStatus = PaymentState.Success(displayMessage)) }
    }

    fun comprarCreditos() {
        _uiState.update { it.copy(showCompraCreditosDialog = true, quantidadeCreditosInput = "100") }
    }

    fun confirmarCompraCreditos() {
        val qtd = uiState.value.quantidadeCreditosInput.toIntOrNull() ?: 0
        if (qtd <= 0) return
        
        // Exemplo: R$ 0,10 por crédito
        val valor = qtd * 0.10
        val placa = "CREDITOS_$qtd"
        
        setShowCompraCreditosDialog(false)
        
        viewModelScope.launch {
            // Registra a "entrada" da placa de créditos para que o backend a encontre
            repository.registrarEntradaRemota(placa)
            confirmarSaidaEGerarPix(placa, valor)
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
