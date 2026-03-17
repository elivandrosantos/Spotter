package br.ordnavile.spotter.data.repository

import br.ordnavile.spotter.data.model.*
import br.ordnavile.spotter.data.remote.MonilocApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MonilocRepository(
    private val api: MonilocApi,
    private val dao: VeiculoDao,
    private val firestore: FirebaseFirestore,
    private val deviceId: String
) {
    private fun getColecaoVeiculos(estacionamentoId: String) = 
        firestore.collection("estacionamentos").document(estacionamentoId.ifBlank { "default" })
            .collection("dispositivos").document(deviceId)
            .collection("veiculos_ativos")

    private fun getColecaoHistorico(estacionamentoId: String) = 
        firestore.collection("estacionamentos").document(estacionamentoId.ifBlank { "default" })
            .collection("dispositivos").document(deviceId)
            .collection("historico_saidas")

    fun listarTodos(): Flow<List<Veiculo>> = dao.listarTodos()

    suspend fun inserir(veiculo: Veiculo, estacionamentoId: String, syncFirestore: Boolean = true) = withContext(Dispatchers.IO) {
        dao.inserir(veiculo)
        if (syncFirestore) {
            try {
                getColecaoVeiculos(estacionamentoId).document(veiculo.placa).set(veiculo).await()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun inserirVarios(veiculos: List<Veiculo>) = withContext(Dispatchers.IO) {
        dao.inserirVarios(veiculos)
    }

    suspend fun deletarPorPlaca(placa: String, estacionamentoId: String) = withContext(Dispatchers.IO) {
        dao.deletarPorPlaca(placa)
        try {
            getColecaoVeiculos(estacionamentoId).document(placa).delete().await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun buscarPorPlaca(placa: String): Veiculo? = withContext(Dispatchers.IO) {
        dao.buscarPorPlaca(placa)
    }

    fun listarHistorico(): Flow<List<HistoricoVeiculo>> = dao.listarHistorico()

    suspend fun inserirHistorico(historico: HistoricoVeiculo, estacionamentoId: String, syncFirestore: Boolean = true) = withContext(Dispatchers.IO) {
        if (syncFirestore) {
            try {
                val docRef = getColecaoHistorico(estacionamentoId).add(historico).await()
                val historicoComId = historico.copy(firestoreId = docRef.id)
                dao.inserirHistorico(historicoComId)
            } catch (e: Exception) { 
                val historicoLocal = historico.copy(firestoreId = UUID.randomUUID().toString())
                dao.inserirHistorico(historicoLocal)
                e.printStackTrace() 
            }
        } else {
            dao.inserirHistorico(historico)
        }
    }

    suspend fun inserirVariosHistorico(historico: List<HistoricoVeiculo>) = withContext(Dispatchers.IO) {
        dao.inserirVariosHistorico(historico)
    }

    suspend fun recuperarVeiculosFirestore(estacionamentoId: String): List<Veiculo> = withContext(Dispatchers.IO) {
        try {
            val result = getColecaoVeiculos(estacionamentoId).get().await()
            result.documents.mapNotNull { it.toObject(Veiculo::class.java) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun recuperarHistoricoFirestore(estacionamentoId: String): List<HistoricoVeiculo> = withContext(Dispatchers.IO) {
        try {
            val result = getColecaoHistorico(estacionamentoId).get().await()
            result.documents.mapNotNull { doc ->
                doc.toObject(HistoricoVeiculo::class.java)?.copy(firestoreId = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun gerarPix(placa: String, valor: Double, token: String? = null, chavePix: String? = null): Result<GerarPixResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.gerarPix(GerarPixRequest(placa, valor, token, chavePix))
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
