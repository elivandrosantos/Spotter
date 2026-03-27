# Spotter

Spotter é um aplicativo Android desenvolvido em Kotlin com Jetpack Compose. O principal objetivo do aplicativo é o gerenciamento e monitoramento do fluxo de veículos, oferecendo um controle detalhado de entradas e saídas e calculando repasses financeiros semanais.

## Funcionalidades Principais

- **Dashboard Semanal**: Resumo financeiro com gráficos de barras das estimativas de repasse semanais (valor pago com desconto das taxas).
- **Histórico de Veículos**: Acompanhamento detalhado das últimas saídas registradas, como placa, modelo do carro, valor bruto pago e horário de saída.
- **Leitura de Código QR**: Suporte integrado à leitura de códigos usando a biblioteca **ZXing**.
- **Sincronização Nuvem e Local**: Integração contínua e persistência de dados utilizando o **Firebase Firestore** e **Room**.
- **Arquitetura Reativa**: Atualizações em tempo real das coleções e dados graças ao uso nativo de **Coroutines**, **Flows** e arquitetura **MVVM**.

## Tecnologias e Bibliotecas

O projeto se apoia no que há de mais moderno no ecossistema de desenvolvimento Android:

- [**Kotlin**](https://kotlinlang.org/) - Linguagem principal.
- [**Jetpack Compose**](https://developer.android.com/jetpack/compose) - Construção da interface gráfica de forma nativa e reativa em Material 3.
- [**Koin**](https://insert-koin.io/) - Injeção de dependências leve, ágil e voltada ao desenvolvimento multiplataforma.
- [**Room**](https://developer.android.com/training/data-storage/room) - Abstração e controle simplificado para bases SQLite locais.
- [**Firebase Firestore**](https://firebase.google.com/docs/firestore) - Banco de dados de nuvem NoSQL altamente escalável.
- [**Retrofit**](https://square.github.io/retrofit/) e [**OkHttp**](https://square.github.io/okhttp/) - Clientes e validadores robustos de requests HTTP.
- [**Coroutines**](https://kotlinlang.org/docs/coroutines-overview.html) - Organização elegante e de fácil leitura de requisições e threads em segundo plano.
- [**ZXing**](https://github.com/zxing/zxing) - Framework robusto e testado para captura e processamento de formatos de códigos de barra (QrCode).

## Estrutura do Projeto (Clean Architecture + MVVM)

O aplicativo divide muito bem as responsabilidades de rede e lógica de layout, em conformidade com as Boas Práticas atuais.

- `data/`: Modelos de dados, repositórios locais e em nuvem, migrações e entidades.
- `di/`: Módulos de injeção de dependências do Koin.
- `services/`: Classes de requisição para integrações via rede (Retrofit).
- `ui/`:
    - `components/`: Componentes visuais desacoplados e baseados no Material Design.
    - `navigation/`: Gerenciamento lógico de fluxo de telas (Jetpack Navigation).
    - `screens/`: Telas e agregadores de componentes (Ex: DashboardScreen, MonilocScreen).
    - `theme/`: Paletas de cores semânticas e baseadas nas recomendações de Design System.
- `utils/`: Funções utilitárias.
- `viewmodel/`: Responsáveis pelo controle de estado lógico entre o Repositório (`data`) e as telas (`ui`), servindo de ponte para reatividade e manipulação do fluxo de negócio.

## Requisitos para build

- **JDK 11+**
- **Android Studio** Ladybug, Iguana ou compatíveis.
- **Android SDK:** 24 mínimo, compilação direcionada para API 36.

## Contribuição

Contribuições são bem-vindas! Sinta-se à vontade para abrir uma _Issue_ relatando bugs, dúvidas ou propor melhorias enviando um _Pull Request_.

## Licença

Este projeto está sob a [Licença MIT](LICENSE). Veja o arquivo `LICENSE` para mais detalhes.


