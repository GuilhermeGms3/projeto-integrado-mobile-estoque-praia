# Gestão local da loja — versão 1.1.0

## Apresentação — versão 1.2.0

As telas de contas exibem barras proporcionais para total pago/recebido, saldo a vencer e vencido, excluindo contas canceladas. O financeiro compara recebimentos e pagamentos acumulados e apresenta o volume de movimentações dos últimos sete dias, explicitamente identificado como entradas + saídas (não saldo). Os gráficos são derivados do mesmo estado das listas e atualizam após baixas.

O formulário de encomenda é uma tela própria, com nome, contato, seleção de produto/variação, quantidade, total combinado, prazo DD/MM/AAAA e observações. Total em branco usa preço cadastrado × quantidade. Validação de data é estrita e erros mantêm o formulário aberto.

O botão Demo adiciona seis produtos, cinco vendas, quatro contas a pagar e três encomendas fictícias, com pagamentos parciais e totais e vencimentos relativos ao dia da carga. A gravação é atômica, não altera registros existentes e é idempotente pelo identificador da carga. Produtos demo são identificados por SKU DEMO-* e há aviso na interface. Clientes e contatos são fictícios. A carga é explícita, nunca automática para uma instalação nova. As contas locais do mesmo aparelho compartilham os dados, conforme o modelo atual.

## Fluxos

- Estoque: cadastro, pesquisa e detalhes por variação. Os detalhes permitem editar dados/preço/mínimo, registrar reposição e consultar todo o histórico do produto. O SKU permanece estável.
- Vendas: cada venda registra uma variação, cliente, quantidade, valor total e vencimento. A transação baixa o estoque, insere a movimentação e gera uma conta a receber. A opção recebido agora também cria a baixa financeira na mesma transação.
- Histórico de vendas: apresenta cliente, produto, quantidade, total, data e situação do recebimento. Uma venda sem recebimentos pode ser cancelada com confirmação; isso devolve o estoque, cancela o recebível e encerra a encomenda de origem, se houver. Vendas recebidas exigiriam fluxo de devolução financeira e não têm cancelamento nesta versão.
- Contas a pagar e receber: lançamentos manuais, vencimento, saldo, situação, baixas parciais ou totais. Valores maiores que o saldo são rejeitados. Contas manuais sem baixa podem ser canceladas, preservando o registro. Contas de venda são canceladas pela venda de origem.
- Financeiro: entradas recebidas menos pagamentos realizados, acumulados desde o início do uso. Não inclui saldo inicial nem lucro. A lista de caixa é derivada das baixas; vendas e dívidas não duplicam entradas.
- Encomendas: uma variação por encomenda, cliente, contato, quantidade, total, prazo e observação. Situações: aberta, em preparação, entregue ou cancelada. Não reserva saldo ou cobra antecipadamente. Entregar verifica estoque e gera venda/recebível, uma única vez. Saldo insuficiente mantém a encomenda aberta/em preparação.

## Persistência e migração

SQLite `inventory.db`, versão 2. Migração aditiva da versão 1: preserva produtos e movimentações e cria sales, bills, payments e orders, com constraints e índices. Contas locais de acesso permanecem intactas. Movimentações antigas não viram vendas retroativas, pois não possuem preço/cliente suficientes para reconstrução financeira confiável.

Valores novos de vendas e finanças são guardados como centavos inteiros. Quantidades e valores positivos, unicidade do recebível por venda e vínculo da encomenda com a venda são garantidos também por constraints. Datas de vencimento são ISO AAAA-MM-DD e exibidas em formato brasileiro. Não há sincronização entre celulares ou integração fiscal/bancária.

Rollback para versão 1 não é suportado automaticamente: o esquema 2 contém registros que a versão anterior não entende. Não desinstalar para atualizar. Distribuir APK com a mesma assinatura e código de versão maior; preservar cópia dos dados antes de qualquer intervenção manual.

## Verificação

`assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, `assembleRelease`, `lintRelease`. Testes instrumentados em arquivo isolado `integration-inventory.db` (não alteram `inventory.db` do usuário): migração de banco v1 com produto existente; venda e baixa parcial; excesso de recebimento; saldo insuficiente sem gravação parcial; contas pagas e caixa; encomenda sem reserva, entrega única e cancelamento com reposição; edição e reabertura dos dados. Não são usados dados fictícios no aplicativo instalado.

Verificação executada em Samsung SM-S906E: `OK (2 tests)` com os cenários acima; builds debug e release aprovados; lint debug e release aprovados. APK 1.1.0 assinado com a mesma chave da versão 1.0.0, código 3. O build debug local também usa essa assinatura quando `signing.properties` existe para permitir atualização durante a verificação sem desinstalar. CI sem essa chave continua usando a assinatura debug padrão.
