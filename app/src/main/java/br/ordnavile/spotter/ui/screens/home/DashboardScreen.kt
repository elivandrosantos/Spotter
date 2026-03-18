package br.ordnavile.spotter.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.ordnavile.spotter.data.model.HistoricoVeiculo
import java.text.SimpleDateFormat
import java.util.*

// Classe auxiliar para estruturar os dados do gráfico
data class DayData(val dayName: String, val value: Double, val isToday: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    historico: List<HistoricoVeiculo>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    // Processamento dos dados da SEMANA VIGENTE (Segunda a Domingo)
    val (weekData, totalWeek) = remember(historico) {
        val dataList = mutableListOf<DayData>()
        var total = 0.0

        val today = Calendar.getInstance()

        // Configura o calendário para o início da semana atual (Segunda-feira)
        val cal = Calendar.getInstance(Locale.forLanguageTag("pt-BR"))
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Loop de 0 a 6 (Segunda = 0, Terça = 1 ... Domingo = 6)
        for (i in 0..6) {
            val dayStart = cal.clone() as Calendar
            dayStart.add(Calendar.DAY_OF_YEAR, i)

            // Fim do dia (23:59:59)
            val dayEnd = dayStart.timeInMillis + 86400000L

            // Filtra o histórico do dia específico e aplica a taxa de repasse de 95%
            val daySum = historico
                .filter { it.dataUnix in dayStart.timeInMillis until dayEnd }
                .sumOf { it.valorPago * 0.95 }

            total += daySum

            // Verifica se este dia do loop é exatamente o dia de hoje
            val isToday = dayStart.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                    dayStart.get(Calendar.YEAR) == today.get(Calendar.YEAR)

            // Formata o nome do dia (ex: "Seg", "Ter")
            val dayName = SimpleDateFormat("EEE", Locale.forLanguageTag("pt-BR")).format(dayStart.time)
                .replace(".", "")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            dataList.add(DayData(dayName, daySum, isToday))
        }
        Pair(dataList, total)
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Título
            Text(
                text = "Resumo da Semana",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Card principal com Número Grande e Gráfico
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Seu repasse estimado (Líquido)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "R$ ${"%.2f".format(totalWeek)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Gráfico de Barras Customizado
                    WeeklyBarChart(
                        data = weekData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Últimas Saídas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Lista de Histórico
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historico.take(20)) { item ->
                    Card(
                        modifier = modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.placa,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.modelo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    // Valor bruto que o cliente pagou na cancela
                                    text = "R$ ${"%.2f".format(item.valorPago)}",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.saida,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyBarChart(data: List<DayData>, modifier: Modifier = Modifier) {
    // Encontra o maior valor da semana para definir a escala máxima (altura 100%)
    val maxValue = data.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom // Alinha as barras pela base
    ) {
        data.forEach { dayData ->
            // Calcula a altura da barra (de 0f a 1f)
            val fraction = (dayData.value / maxValue).toFloat()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                // A barra em si
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f) // Largura da barra em relação à coluna
                        .fillMaxHeight(fraction.coerceAtLeast(0.05f)) // Altura mínima para não sumir se for zero
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (dayData.isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondaryContainer
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Rótulo do dia (Seg, Ter...)
                Text(
                    text = dayData.dayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (dayData.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (dayData.isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}


//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun DashboardScreen(
//    modifier: Modifier = Modifier,
//    historico: List<HistoricoVeiculo>,
//    isRefreshing: Boolean,
//    onRefresh: () -> Unit
//) {
//    PullToRefreshBox(
//        isRefreshing = isRefreshing,
//        onRefresh = onRefresh
//    ) {
//    val hoje = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
//
//    val estatisticasHoje = historico.filter {
//        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it.dataUnix)) == hoje
//    }
//
//    val totalGanhosHoje = estatisticasHoje.sumOf { it.valorPago }
//    val totalVeiculosHoje = estatisticasHoje.size
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        Text(
//            text = "Estatísticas de Hoje",
//            style = MaterialTheme.typography.headlineSmall,
//            fontWeight = FontWeight.Bold,
//            modifier = Modifier.padding(bottom = 16.dp)
//        )
//
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            StatCard(
//                label = "Ganhos",
//                value = "R$ ${"%.2f".format(totalGanhosHoje)}",
//                modifier = Modifier.weight(1f),
//                containerColor = MaterialTheme.colorScheme.primaryContainer
//            )
//            StatCard(
//                label = "Veículos",
//                value = totalVeiculosHoje.toString(),
//                modifier = Modifier.weight(1f),
//                containerColor = MaterialTheme.colorScheme.secondaryContainer
//            )
//        }
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        Text(
//            text = "Últimas Saídas",
//            style = MaterialTheme.typography.titleMedium,
//            fontWeight = FontWeight.Bold,
//            modifier = Modifier.padding(bottom = 8.dp)
//        )
//
//        LazyColumn(
//            modifier = Modifier.weight(1f),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            items(historico.take(20)) { item ->
//                Card(
//                    modifier = modifier.fillMaxWidth(),
//                    // --- CORREÇÃO 1: Fundo dinâmico ---
//                    // Removi Color.White. Agora ele usa a cor surface do seu tema M3.
//                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
//                    // Adicionei uma borda para um visual M3 mais robusto (opcional)
//                    border = CardDefaults.outlinedCardBorder(),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(16.dp), // Aumentei um pouco o padding para 16dp (padrão M3)
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Column(
//                            modifier = Modifier.weight(1f) // Garante que a coluna da esquerda ocupe o espaço necessário
//                        ) {
//                            Text(
//                                text = item.placa,
//                                style = MaterialTheme.typography.titleMedium, // Usei tipografia semântica
//                                fontWeight = FontWeight.Bold,
//                                color = MaterialTheme.colorScheme.onSurface // Letra dinâmica: escura no claro, clara no escuro
//                            )
//                            Text(
//                                text = item.modelo,
//                                style = MaterialTheme.typography.bodySmall, // Tipografia semântica
//                                color = MaterialTheme.colorScheme.onSurfaceVariant // Letra variante para menos ênfase
//                            )
//                        }
//                        Column(horizontalAlignment = Alignment.End) {
//                            Text(
//                                text = "R$ ${"%.2f".format(item.valorPago)}",
//                                // --- CORREÇÃO 2: Verde dinâmico ---
//                                // Troquei o verde fixo por uma cor do esquema (Tertiary).
//                                // Para que isso funcione, garanta que no seu arquivo de Tema (Theme.kt)
//                                // a cor 'tertiary' seja um verde nos esquemas de tema claro e escuro.
//                                color = MaterialTheme.colorScheme.tertiary,
//                                style = MaterialTheme.typography.titleMedium, // Tipografia semântica
//                                fontWeight = FontWeight.Bold
//                            )
//                            Text(
//                                text = item.saida,
//                                style = MaterialTheme.typography.labelSmall, // Tipografia semântica
//                                color = MaterialTheme.colorScheme.onSurfaceVariant // Letra variante
//                            )
//                        }
//                    }
//                }
//            }
//        }
//        }
//    }
//}
//
//@Composable
//fun StatCard(
//    label: String,
//    value: String,
//    modifier: Modifier = Modifier,
//    containerColor: Color
//) {
//    Card(
//        modifier = modifier,
//        colors = CardDefaults.cardColors(containerColor = containerColor)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(text = label, style = MaterialTheme.typography.labelMedium)
//            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
//        }
//    }
//}
