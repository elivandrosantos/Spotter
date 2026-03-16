package br.ordnavile.spotter.data.state

import br.ordnavile.spotter.data.model.Veiculo

data class MonilocUiState(
    val veiculos: List<Veiculo> = emptyList(),
    val filtro: String = "",
    val errorMessage: String? = null,
    val showLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val paymentStatus: PaymentState = PaymentState.Idle
)

sealed class PaymentState {
    object Idle : PaymentState()
    data class Confirm(val veiculo: Veiculo, val valor: Double) : PaymentState()
    data class AwaitingPayment(
        val qrCodeText: String,
        val valor: Double,
        val idPagamento: String,
        val placa: String
    ) : PaymentState()
    data class Success(val placa: String) : PaymentState()
}
