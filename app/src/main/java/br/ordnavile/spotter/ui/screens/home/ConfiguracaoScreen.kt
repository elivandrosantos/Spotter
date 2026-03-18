package br.ordnavile.spotter.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracaoScreen(
    idEstacionamentoInput: String,
    nomeInput: String,
    primeiraHoraInput: String,
    horaAdicionalInput: String,
    fixo12hInput: String,
    toleranciaInput: String,
    tokenMpInput: String,
    chavePixInput: String,
    saldo: Int,
    showTutorial: Boolean,
    onIdEstacionamentoChange: (String) -> Unit,
    onNomeChange: (String) -> Unit,
    onPrimeiraHoraChange: (String) -> Unit,
    onHoraAdicionalChange: (String) -> Unit,
    onFixo12hChange: (String) -> Unit,
    onToleranciaChange: (String) -> Unit,
    onTokenMpChange: (String) -> Unit,
    onChavePixChange: (String) -> Unit,
    onShowTutorial: (Boolean) -> Unit,
    onComprarCreditos: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card de Créditos
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Saldo de Créditos", style = MaterialTheme.typography.titleMedium)
                    Text("$saldo veículos restantes", style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = onComprarCreditos) {
                    Text("Comprar")
                }
            }
        }
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Informações Gerais",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = idEstacionamentoInput,
                        onValueChange = onIdEstacionamentoChange,
                        label = { Text("ID do Estacionamento (Para sincronização)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nomeInput,
                        onValueChange = onNomeChange,
                        label = { Text("Nome do Estacionamento") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Valores de Cobrança",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ConfigField(
                        label = "Valor Primeira Hora (R$)",
                        value = primeiraHoraInput,
                        onValueChange = onPrimeiraHoraChange
                    )

                    ConfigField(
                        label = "Valor Hora Adicional (R$)",
                        value = horaAdicionalInput,
                        onValueChange = onHoraAdicionalChange
                    )

                    ConfigField(
                        label = "Valor Fixo 12 Horas (R$)",
                        value = fixo12hInput,
                        onValueChange = onFixo12hChange
                    )

                    ConfigField(
                        label = "Tempo Tolerância (Minutos)",
                        value = toleranciaInput,
                        onValueChange = onToleranciaChange,
                        isInteger = true
                    )
                }
            }

//            HorizontalDivider()
//
//            // Card Mercado Pago
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//            ) {
//                Column(modifier = Modifier.padding(16.dp)) {
//                    Text(
//                        text = "Integração Mercado Pago",
//                        style = MaterialTheme.typography.titleMedium,
//                        color = MaterialTheme.colorScheme.primary
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        text = "Configure suas credenciais para receber os pagamentos diretamente na sua conta.",
//                        style = MaterialTheme.typography.bodySmall,
//                        modifier = Modifier.padding(bottom = 8.dp)
//                    )
//
//                    OutlinedTextField(
//                        value = tokenMpInput,
//                        onValueChange = onTokenMpChange,
//                        label = { Text("Production Access Token") },
//                        modifier = Modifier.fillMaxWidth(),
//                        singleLine = true
//                    )
//
//                    OutlinedTextField(
//                        value = chavePixInput,
//                        onValueChange = onChavePixChange,
//                        label = { Text("Chave PIX (Mercado Pago)") },
//                        modifier = Modifier.fillMaxWidth(),
//                        singleLine = true
//                    )
//
//                    TextButton(
//                        onClick = { onShowTutorial(true) },
//                        modifier = Modifier.align(Alignment.End)
//                    ) {
//                        Text("Como obter meu Token?")
//                    }
//                }
//            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvar Configurações")
            }
        }

        if (showTutorial) {
        AlertDialog(
            onDismissRequest = { onShowTutorial(false) },
            title = { Text("Como pegar seu Token?") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("1. Acesse o painel de desenvolvedores do Mercado Pago.")
                    Text("2. Vá em 'Suas Aplicações'.")
                    Text("3. Selecione sua aplicação (ou crie uma nova).")
                    Text("4. No menu lateral, clique em 'Credenciais de Produção'.")
                    Text("5. Copie o 'Access Token' e cole no campo acima.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("IMPORTANTE: Sua chave PIX deve ser a mesma cadastrada na conta vinculada a este token.")
                }
            },
            confirmButton = {
                TextButton(onClick = { onShowTutorial(false) }) {
                    Text("Entendi")
                }
            }
        )
    }
}


@Composable
fun ConfigField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isInteger: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Troca vírgula por ponto para facilitar a digitação no teclado brasileiro
            val formattedValue = newValue.replace(',', '.')

            // Valida a entrada em tempo real usando Regex
            val isValid = if (isInteger) {
                // Permite apenas números (vazio ou sequência de dígitos)
                formattedValue.matches(Regex("^\\d*$"))
            } else {
                // Permite números, opcionalmente seguidos por um único ponto e mais números
                // Ex: "", "10", "10.", "10.5", ".5"
                formattedValue.matches(Regex("^\\d*\\.?\\d*$"))
            }

            if (isValid) {
                onValueChange(formattedValue)
            }
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        // Exibe o teclado numérico simples ou o decimal dependendo da necessidade
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isInteger) KeyboardType.Number else KeyboardType.Decimal
        ),
        singleLine = true
    )
}



//@Composable
//fun ConfigField(
//    label: String,
//    value: String,
//    onValueChange: (String) -> Unit,
//    isInteger: Boolean = false
//) {
//    OutlinedTextField(
//        value = value,
//        onValueChange = {
//            if (it.isEmpty() || it.toDoubleOrNull() != null || (isInteger && it.toIntOrNull() != null)) {
//                onValueChange(it)
//            }
//        },
//        label = { Text(label) },
//        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
//        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//        singleLine = true
//    )
//}
