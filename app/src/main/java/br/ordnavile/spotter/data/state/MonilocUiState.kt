package br.ordnavile.spotter.data.state

import br.ordnavile.spotter.data.model.ConfiguracaoEstacionamento
import br.ordnavile.spotter.data.model.Veiculo

data class MonilocUiState(
    val veiculos: List<Veiculo> = emptyList(),
    val filtro: String = "",
    val errorMessage: String? = null,
    val showLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val showAddDialog: Boolean = false,
    val novaPlaca: String = "",
    val novoModelo: String = "",
    val paymentStatus: PaymentState = PaymentState.Idle,
    val configuracao: ConfiguracaoEstacionamento = ConfiguracaoEstacionamento(),
    val idEstacionamentoInput: String = "",
    val nomeEstacionamentoInput: String = "",
    val valorPrimeiraHoraInput: String = "",
    val valorHoraAdicionalInput: String = "",
    val valorFixo12HorasInput: String = "",
    val tempoToleranciaInput: String = "",
    val tokenMpInput: String = "",
    val chavePixInput: String = "",
    val showMpTutorial: Boolean = false,
    val showCompraCreditosDialog: Boolean = false,
    val quantidadeCreditosInput: String = "100",
    val currentScreen: Screen = Screen.Home
)

enum class Screen {
    Home,
    Configuracao,
    Dashboard
}

sealed class PaymentState {
    object Idle : PaymentState()
    data class Confirm(val veiculo: Veiculo, val valor: Double) : PaymentState()
    data class AwaitingPayment(
        val qrCodeText: String,
        val valor: Double,
        val idPagamento: String,
        val placa: String,
        val veiculo: Veiculo
    ) : PaymentState()
    data class Success(val message: String) : PaymentState()
}
