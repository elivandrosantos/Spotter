package br.ordnavile.spotter.data.model

data class ConfiguracaoEstacionamento(
    val idEstacionamento: String = "",
    val nomeEstacionamento: String = "Meu Estacionamento",
    val valorPrimeiraHora: Double = 10.0,
    val valorHoraAdicional: Double = 5.0,
    val valorFixo12Horas: Double = 40.0,
    val tempoToleranciaMinutos: Int = 0,
    val tokenMercadoPago: String = "",
    val chavePix: String = "",
    val saldoCreditos: Int = 2, // Saldo inicial cortesia
    val custoPorEntrada: Int = 1
)
