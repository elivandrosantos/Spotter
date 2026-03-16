package br.ordnavile.spotter.data.remote

import retrofit2.Response
import br.ordnavile.spotter.data.model.EntradaRequest
import br.ordnavile.spotter.data.model.GerarPixRequest
import br.ordnavile.spotter.data.model.GerarPixResponse
import br.ordnavile.spotter.data.model.VerificarPagamentoResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MonilocApi {

    @POST("/gerar-pix")
    suspend fun gerarPix(@Body request: GerarPixRequest): Response<GerarPixResponse>

    @GET("/verificar-pagamento/{id}")
    suspend fun verificarPagamento(@Path("id") idPagamento: String): Response<VerificarPagamentoResponse>

    @POST("/entrada")
    suspend fun entrada(@Body request: EntradaRequest): Response<Unit>
}
