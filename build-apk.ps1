<#
.SYNOPSIS
    Script de compilación y despliegue del APK de SIGRC Móvil
.DESCRIPTION
    Compila la app Android en modo release, genera el APK firmado
    y lo sube al servidor SIGRC backend.
.PARAMETER Version
    Versión de la app (ej: 1.0.0)
.PARAMETER ProjectPath
    Ruta al proyecto Android
.PARAMETER Upload
    Si se especifica, sube el APK al backend
.PARAMETER ApiUrl
    URL del backend para subir el APK (ej: http://192.168.100.215/sigrc/api/app-movil)
.EXAMPLE
    .\build-apk.ps1 -Version 1.0.0 -Upload -ApiUrl "http://192.168.100.215/sigrc/api/app-movil"
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$Version,

    [Parameter(Mandatory = $false)]
    [string]$ProjectPath = "$PSScriptRoot\app-movil",

    [Parameter(Mandatory = $false)]
    [switch]$Upload,

    [Parameter(Mandatory = $false)]
    [string]$ApiUrl = "http://192.168.100.215/sigrc/api/app-movil"
)

$ErrorActionPreference = "Stop"

# Colores para output
function Write-Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-OK($msg)   { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Err($msg)  { Write-Host "  [ERROR] $msg" -ForegroundColor Red; exit 1 }

# ─── 1. Validar proyecto ───
Write-Step "1. Verificando proyecto Android en: $ProjectPath"

if (-not (Test-Path "$ProjectPath")) {
    Write-Err "No se encontró el proyecto Android en $ProjectPath"
}

if (-not (Test-Path "$ProjectPath\gradlew.bat") -and -not (Test-Path "$ProjectPath\gradlew")) {
    Write-Err "No se encontró gradlew en el proyecto. Asegúrese de que sea un proyecto Android estándar."
}

# ─── 2. Establecer versión ───
Write-Step "2. Estableciendo versión: $Version"

# Buscar app/build.gradle o app/build.gradle.kts
$gradleFiles = @()
if (Test-Path "$ProjectPath\app\build.gradle") { $gradleFiles += "$ProjectPath\app\build.gradle" }
if (Test-Path "$ProjectPath\app\build.gradle.kts") { $gradleFiles += "$ProjectPath\app\build.gradle.kts" }

if ($gradleFiles.Count -eq 0) {
    Write-Warning "  No se encontró app/build.gradle(.kts). La versión deberá configurarse manualmente."
} else {
    foreach ($file in $gradleFiles) {
        $content = Get-Content $file -Raw
        # Actualizar versionName
        if ($content -match 'versionName\s+"[\d.]+"') {
            $content = $content -replace 'versionName\s+"[\d.]+"', "versionName `"$Version`""
            Set-Content -Path $file -Value $content
            Write-OK "versionName actualizado a $Version en $file"
        }
    }
}

# ─── 3. Limpiar y compilar ───
Write-Step "3. Limpiando compilación anterior"
Set-Location -Path $ProjectPath
if (Test-Path "$ProjectPath\gradlew.bat") {
    & .\gradlew.bat clean 2>&1 | Out-Null
} else {
    & .\gradlew clean 2>&1 | Out-Null
}
if ($LASTEXITCODE -ne 0) { Write-Err "Error al limpiar el proyecto" }
Write-OK "Proyecto limpiado"

Write-Step "4. Compilando APK Release"
if (Test-Path "$ProjectPath\gradlew.bat") {
    & .\gradlew.bat assembleRelease 2>&1 | Out-Null
} else {
    & .\gradlew assembleRelease 2>&1 | Out-Null
}
if ($LASTEXITCODE -ne 0) { Write-Err "Error en la compilación. Revise los errores." }
Write-OK "APK compilado exitosamente"

# ─── 4. Buscar APK generado ───
Write-Step "5. Buscando APK generado"

$apkPath = Get-ChildItem -Path "$ProjectPath\app\build\outputs\apk\release" -Filter "*.apk" `
    | Sort-Object LastWriteTime -Descending `
    | Select-Object -First 1

if (-not $apkPath) {
    Write-Err "No se encontró el APK en app\build\outputs\apk\release\"
}

Write-OK "APK encontrado: $($apkPath.FullName)"
Write-Host "    Tamaño: $('{0:N2}' -f ($apkPath.Length / 1MB)) MB"

# ─── 5. Renombrar APK ───
Write-Step "6. Renombrando APK"
$versionSinPuntos = $Version -replace '\.', '-'
$destino = "$PSScriptRoot\sigrc-movil-v$versionSinPuntos.apk"
Copy-Item -Path $apkPath.FullName -Destination $destino -Force
Write-OK "APK copiado a: $destino"

# ─── 6. Subir al backend (opcional) ───
if ($Upload) {
    Write-Step "7. Subiendo APK al servidor"
    Write-Host "    URL: $ApiUrl"

    try {
        $form = @{
            version   = $Version
            descripcion = "APK generado automaticamente el $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
            archivo   = Get-Item -Path $destino
        }
        $response = Invoke-RestMethod -Uri $ApiUrl -Method Post -Form $form
        Write-OK "APK subido exitosamente. ID: $($response.idAppMovil)"
    } catch {
        Write-Err "Error al subir APK: $_"
    }
}

# ─── 7. Resumen ───
Write-Step "8. Resumen"
Write-Host "  Versión : $Version" -ForegroundColor Green
Write-Host "  APK     : $destino" -ForegroundColor Green
Write-Host "  Tamaño  : $('{0:N2}' -f ((Get-Item $destino).Length / 1MB)) MB" -ForegroundColor Green
if ($Upload) { Write-Host "  Subido  : Sí" -ForegroundColor Green }

Set-Location -Path $PSScriptRoot
Write-Host "`n¡Compilación completada!" -ForegroundColor Green
