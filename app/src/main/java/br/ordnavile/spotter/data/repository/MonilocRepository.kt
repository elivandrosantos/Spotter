package br.ordnavile.spotter.data.repository

import br.ordnavile.spotter.data.model.Veiculo
import br.ordnavile.spotter.data.repository.VeiculoDao
import br.ordnavile.spotter.data.model.EntradaRequest
import br.ordnavile.spotter.data.model.GerarPixRequest
import br.ordnavile.spotter.data.model.GerarPixResponse
import br.ordnavile.spotter.data.remote.MonilocApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MonilocRepository(
    private val api: MonilocApi,
    private val dao: VeiculoDao
) {
    suspend fun listarTodos(): List<Veiculo> = withContext(Dispatchers.IO) {
        dao.listarTodos()
    }

    suspend fun inserir(veiculo: Veiculo) = withContext(Dispatchers.IO) {
        dao.inserir(veiculo)
    }

    suspend fun deletarPorPlaca(placa: String) = withContext(Dispatchers.IO) {
        dao.deletarPorPlaca(placa)
    }

    suspend fun gerarPix(placa: String, valor: Double): Result<GerarPixResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.gerarPix(GerarPixRequest(placa, valor))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.idPagamento != null && body.qrCode != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.erro ?: "Erro desconhecido ao gerar Pix"))
                }
            } else {
                Result.failure(Exception("Erro no servidor: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erro de conexão.", e))
        }
    }

    suspend fun verificarPagamento(idPagamento: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.verificarPagamento(idPagamento)
            if (response.isSuccessful) {
                response.body()?.pago == true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun registrarEntradaRemota(placa: String) = withContext(Dispatchers.IO) {
        try {
            api.entrada(EntradaRequest(placa))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
