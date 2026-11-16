# Projeto Integrado Mobile — Estoque Praia

Aplicativo Android nativo desenvolvido para apoiar o controle de estoque de uma loja de moda praia. O sistema permite cadastrar produtos e suas variações, registrar entradas e saídas e acompanhar itens com estoque baixo.

## Equipe

- Guilherme Aires Gomes
- Matheus Sousa
- Samuel Pinheiro Soares
- Matheus Augusto

## Tecnologias

- Kotlin 2.0.21;
- Android SDK 35 (mínimo 26);
- Jetpack Compose e Material 3;
- arquitetura organizada em UI, domínio e repositórios;
- Gradle Wrapper 8.7 e Java 17.

## O que já está implementado

- painel com totais e alertas de estoque baixo;
- busca por produto, categoria, cor, tamanho ou SKU;
- visualização de saldo por variação;
- cadastro local de produto com categoria, tamanho, cor, SKU, preço, estoque inicial e estoque mínimo;
- registro de entrada e saída por venda;
- regra que impede saldo negativo;
- histórico recente de movimentações.

O Login e Cadastro usam contas persistidas no aparelho, com senhas derivadas por PBKDF2. Crie uma conta no primeiro acesso. A conta de demonstração das versões anteriores não faz parte da release 1.0.0.

Produtos e movimentações são persistidos em SQLite no armazenamento privado do aparelho. O estoque inicia vazio. As interfaces `InventoryRepository` e `AuthRepository` separam a tela das fontes de dados. Esta versão funciona localmente, sem sincronização, e todas as contas do aparelho acessam o mesmo estoque. A integração REST permanece uma evolução futura; consulte `docs/RELEASE_MVP.md`.

## Como abrir

1. Instale o Android Studio e o JDK 17.
2. Clone o repositório: `git clone https://github.com/GuilhermeGms3/projeto-integrado-mobile-estoque-praia.git`.
3. Abra a pasta clonada no Android Studio.
4. Informe o Android SDK quando solicitado (a referência usada é API 35).
5. Aguarde a sincronização do Gradle e execute o módulo `app` em um emulador ou dispositivo Android com API 26 ou superior.

Para preparar e compilar rapidamente em outro computador Windows, consulte [`SETUP_RAPIDO.md`](SETUP_RAPIDO.md) ou execute `./setup-windows.ps1` no PowerShell.

### Validação pela linha de comando

No Windows:

```powershell
.\gradlew.bat test lintDebug assembleDebug
```

No Linux ou macOS:

```bash
./gradlew test lintDebug assembleDebug
```

## Estrutura do repositório

- `app/`: código-fonte e recursos do aplicativo Android;
- `assets/`: recursos visuais externos e orientações de inclusão;
- `docs/`: decisões, diagramas e documentação acadêmica;
- `tests/`: documentação e artefatos complementares de testes;
- `.github/`: template de Pull Request e integração contínua.

## Fluxo de versionamento

- `main`: versão estável e aprovada;
- `develop`: integração das funcionalidades;
- `feature/*` e `docs/*`: branches de trabalho criadas a partir de `develop`.

Toda alteração deve chegar à `develop` por Pull Request. Uma versão aprovada segue de `develop` para `main` por outro Pull Request. Consulte [`docs/ESTRUTURA_E_VERSIONAMENTO.md`](docs/ESTRUTURA_E_VERSIONAMENTO.md).

## Escopo deste incremento

Está deliberadamente fora deste primeiro corte: autenticação real, clientes, pedidos, vendas completas, finanças e integração REST. Esses módulos permanecem descritos no documento acadêmico, mas o estoque é a prioridade funcional solicitada.
