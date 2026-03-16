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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    fun setShowAddDialog(show: Boolean) {
        _uiState.update { it.copy(showAddDialog = show) }
    }

    fun registrarEntrada(placa: String, modelo: String) {
        if (placa.isBlank()) return
        val horaAtual = timeFormat.format(Date())
        val novoVeiculo = Veiculo(placa = placa.uppercase(), modelo = modelo, entrada = horaAtual)
        
        viewModelScope.launch {
            repository.inserir(novoVeiculo)
            repository.registrarEntradaRemota(novoVeiculo.placa)
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
        var valorTotal = (minutos / 60.0) * 10.0
        if (valorTotal < 5.0) valorTotal = 5.0

        _uiState.update { it.copy(paymentStatus = PaymentState.Confirm(veiculo, valorTotal)) }
    }

    fun cancelarSaida() {
        pollJob?.cancel()
        _uiState.update { it.copy(paymentStatus = PaymentState.Idle) }
    }

    fun confirmarSaidaEGerarPix(placa: String, valor: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(showLoading = true) }
            val result = repository.gerarPix(placa, valor)
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
                    processarSucessoPagamento(placa)
                    break
                }
            }
        }
    }

    private suspend fun processarSucessoPagamento(placa: String) {
        repository.deletarPorPlaca(placa)
        carregarVeiculos()
        _uiState.update { it.copy(paymentStatus = PaymentState.Success(placa)) }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
