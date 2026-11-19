# MVP 1.0.0 e distribuição

O MVP é um aplicativo Android de estoque para uso local em um aparelho por loja. Produtos, variações e histórico são armazenados em SQLite no armazenamento privado. O cadastro de contas usa armazenamento privado e PBKDF2-HMAC-SHA256 com salt aleatório por senha. Não existe sincronização entre aparelhos, recuperação de senha ou backend remoto nesta versão. Todas as contas locais acessam o mesmo estoque do aparelho.

## Primeiro acesso

Use Criar conta e registre seus próprios dados. A conta de demonstração das versões anteriores não faz parte da release. O estoque começa vazio. As alterações persistem após fechar e reabrir; desinstalar o aplicativo apaga os dados locais. Backup automático está desativado.

## Build

- `./gradlew assembleRelease bundleRelease lintRelease`
- `app/build/outputs/apk/release/app-release.apk`: instalação direta/Firebase App Distribution.
- `app/build/outputs/bundle/release/app-release.aab`: pacote para Play Console.
- R8 e redução de recursos ativados; preservar o arquivo de mapeamento para diagnóstico.
- API mínima 26, alvo 35, Kotlin 2.0.21, Java 17.

O arquivo `signing.properties` deve existir somente no computador responsável pela assinatura. Ele contém `storeFile`, `storePassword`, `keyAlias` e `keyPassword`; a chave e esse arquivo não podem ser enviados ao GitHub. Preserve uma cópia privada segura da chave para assinar todas as atualizações.

## Firebase App Distribution

Criar um projeto Firebase e registrar o pacote Android `br.edu.uniceplac.modapraia`. App Distribution é o canal de distribuição; seu uso não transforma o banco local em banco na nuvem. O upload do APK pode ser feito pelo console, sem adicionar SDK Firebase ao aplicativo.

Criar um grupo de testadores com ao menos 15 e-mails reais, enviar convites, registrar o link de convite e acompanhar aceite e download no console. Não publicar os e-mails dos testadores no repositório ou no relatório. Capturas reais do console, certificado, build e instalação devem compor o relatório final.

## Critérios de validação

Validar cadastro, login correto e incorreto, produto com SKU duplicado, entrada, saída, bloqueio de saldo negativo e permanência dos dados após encerrar o processo. Registrar instalação física e ausência de falhas fatais no log durante esses fluxos. A publicação só está concluída quando houver release disponível no Firebase e link real de acesso.
