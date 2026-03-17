package br.ordnavile.spotter.data.model

import com.google.gson.annotations.SerializedName

data class GerarPixRequest(
    val placa: String,
    val valor: Double,
    val token: String? = null,
    val chavePix: String? = null
)

data class GerarPixResponse(
    @SerializedName("id_pagamento") val idPagamento: String?,
    @SerializedName("qr_code") val qrCode: String?,
    @SerializedName("erro") val erro: String?
)

data class VerificarPagamentoResponse(
    @SerializedName("pago") val pago: Boolean?
)

data class EntradaRequest(
    val placa: String
)
