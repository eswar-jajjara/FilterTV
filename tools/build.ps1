param(
    [string]$Sdk = $env:ANDROID_HOME,
    [string]$Jdk = $env:JAVA_HOME,
    [switch]$Lint
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $Sdk -or -not (Test-Path -LiteralPath $Sdk)) { throw 'Pass -Sdk with your Android SDK directory.' }
if (-not $Jdk -or -not (Test-Path -LiteralPath "$Jdk\bin\java.exe")) { throw 'Pass -Jdk with your JDK 17 directory.' }
$env:JAVA_HOME = $Jdk
$env:ANDROID_HOME = (Resolve-Path -LiteralPath $Sdk).Path
$env:ANDROID_USER_HOME = Join-Path $projectRoot '.gradle-local\android-user'
$env:ANDROID_SDK_HOME = Join-Path $projectRoot '.gradle-local'
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle-local'
New-Item -ItemType Directory -Force $env:ANDROID_USER_HOME | Out-Null
Push-Location $projectRoot
try {
    $buildTasks = @('assembleStfdroidDebug')
    if ($Lint) { $buildTasks += 'lintStfdroidDebug' }
    & .\gradlew.bat @buildTasks --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
} finally { Pop-Location }
