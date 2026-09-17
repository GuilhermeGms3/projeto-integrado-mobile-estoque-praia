$ErrorActionPreference = "Stop"

$projectDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$androidSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"

if (-not (Test-Path -LiteralPath $androidSdk)) {
    throw "Android SDK não encontrado em $androidSdk. Instale o Android Studio e o SDK 35 primeiro."
}

$escapedSdk = $androidSdk.Replace("\", "\\")
$localProperties = Join-Path $projectDirectory "local.properties"
[System.IO.File]::WriteAllText($localProperties, "sdk.dir=$escapedSdk`r`n")

$env:ANDROID_HOME = $androidSdk
& (Join-Path $projectDirectory "gradlew.bat") :app:assembleDebug

if ($LASTEXITCODE -ne 0) {
    throw "O build do Android falhou."
}

Write-Host "Projeto pronto. APK: app\build\outputs\apk\debug\app-debug.apk"
