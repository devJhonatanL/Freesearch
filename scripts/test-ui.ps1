$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    $sdkPath = $env:ANDROID_HOME
    if (-not $sdkPath) { $sdkPath = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
    $adbPath = Join-Path $sdkPath 'platform-tools/adb.exe'
    if (-not (Test-Path -LiteralPath $adbPath)) { throw 'Configure ANDROID_HOME para seu SDK Android.' }
    & .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação.' }
    & $adbPath install -r app/build/outputs/apk/debug/app-debug.apk
    if ($LASTEXITCODE -ne 0) { throw 'Falha na instalação do app. Conecte um único dispositivo.' }
    & $adbPath install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    if ($LASTEXITCODE -ne 0) { throw 'Falha na instalação dos testes.' }
    $testOutput = & $adbPath shell am instrument -w br.edu.ueg.freesearch.test/androidx.test.runner.AndroidJUnitRunner
    $testOutput | Write-Output
    if (($testOutput -join "`n") -notmatch 'OK \(8 tests\)') { throw 'Os oito testes não concluíram com sucesso.' }
} finally { Pop-Location }
