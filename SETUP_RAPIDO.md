# Setup rápido no notebook

## Pelo Android Studio

1. Clone o repositório.
2. No Android Studio, escolha **Open** e selecione a pasta clonada.
3. Aguarde o Gradle Sync. O projeto usa Android SDK 35 e Java 17.
4. Crie ou selecione um emulador com Android 8.0 (API 26) ou superior.
5. Execute o módulo `app`.

O Android Studio cria o `local.properties` automaticamente com o caminho do SDK.

## Setup automático no Windows

Com o Android Studio e o SDK instalados no local padrão, execute no PowerShell:

```powershell
.\setup-windows.ps1
```

O script configura o SDK apenas para esta cópia e gera o APK de desenvolvimento em:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Login de demonstração

- E-mail: `gestora@estoquepraia.com`
- Senha: `12345678`

O cadastro e os dados são locais e reiniciados quando o aplicativo é fechado.
