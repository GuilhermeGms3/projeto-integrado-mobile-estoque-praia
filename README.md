# Estoque Praia

Primeira entrega do aplicativo Android nativo do projeto de Moda Praia da UNICEPLAC. O foco deste incremento é o controle de estoque por variação de produto.

## O que já está implementado

- painel com totais e alertas de estoque baixo;
- busca por produto, categoria, cor, tamanho ou SKU;
- visualização de saldo por variação;
- registro de entrada e saída por venda;
- regra que impede saldo negativo;
- histórico recente de movimentações.

Também há um protótipo funcional local de Login e Cadastro: valida nome completo, e-mail, senha e confirmação, permite criar uma conta apenas durante a sessão e libera o painel de estoque. Para testar diretamente, use `gestora@estoquepraia.com` e `12345678`.

Os registros atuais são dados de demonstração mantidos em memória apenas para validar o fluxo de interface. Eles são reiniciados quando o aplicativo fecha. As interfaces `InventoryRepository` e `AuthRepository` separam a tela das fontes de dados: a próxima etapa é substituir as implementações em memória pela API REST em Kotlin/Spring Boot e PostgreSQL prevista no trabalho escrito.

## Como abrir

1. Abra esta pasta no Android Studio.
2. Informe o Android SDK quando solicitado (a referência usada é API 35).
3. Aguarde a sincronização do Gradle e execute em um emulador ou dispositivo Android com API 26 ou superior.

Para preparar e compilar rapidamente em outro computador Windows, consulte [`SETUP_RAPIDO.md`](SETUP_RAPIDO.md) ou execute `./setup-windows.ps1` no PowerShell.

## Escopo deste incremento

Está deliberadamente fora deste primeiro corte: autenticação real, clientes, pedidos, vendas completas, finanças e integração REST. Esses módulos permanecem descritos no documento acadêmico, mas o estoque é a prioridade funcional solicitada.
