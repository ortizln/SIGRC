<#
.SYNOPSIS
    Aplica la migración de la base de datos SIGRC en producción a la nueva
    estructura (Talento Humano: niveles, unidades, puestos, empleados, asignaciones).

.DESCRIPTION
    Realiza en orden:
      1) Respaldo completo (pg_dump custom) verificado.
      2) Migración de datos usuarios -> empleados (migracion-usuarios-empleados.sql).
      3) Módulos Talento Humano (manual-funciones, gestion-personal, delegaciones,
         seguridad-confidencialidad, repositorio-documentos-expediente).
      4) Migraciones de correspondencia.
      5) Fixes (auditoria, triggers ticket/correspondencia, FK responsables).
      6) Verificación de consistencia (SQL).

    Requiere psql y pg_dump/pg_restore de PostgreSQL 16+ en el PATH (o -PsqlBin).
    Todos los scripts son idempotentes y el núcleo es transaccional.

.PARAMETER Server
    Host de la BD. Default: 192.168.1.43 (producción).
.PARAMETER Port
    Puerto de PostgreSQL. Default: 5432.
.PARAMETER Database
    Nombre de la BD. Default: sigrc.
.PARAMETER Username
    Usuario de PostgreSQL. Default: postgres.
.PARAMETER Password
    Contraseña. Si no se provee, se solicita de forma segura (no queda en el historial).
.PARAMETER PsqlBin
    Carpeta bin de PostgreSQL (opcional). Si se omite, se busca psql/pg_dump en el PATH.
.PARAMETER RepoRoot
    Carpeta raíz del repositorio donde están los scripts .sql. Default: carpeta de este script.
.PARAMETER BackupDir
    Carpeta donde se guarda el respaldo. Default: subcarpeta 'backups' junto a este script.
.PARAMETER SkipBackup
    Omite el respaldo (NO recomendado en producción).
.PARAMETER SkipVerification
    Omite las verificaciones finales.
.PARAMETER Restore
    Restaura el último respaldo encontrado en BackupDir en lugar de migrar.

.EXAMPLE
    # Producción con password por prompt
    .\migrar-produccion.ps1

.EXAMPLE
    # Con parámetros explícitos y sin backup (solo pruebas locales)
    .\migrar-produccion.ps1 -Server localhost -Password 12345 -SkipBackup

.EXAMPLE
    # Restaurar el respaldo más reciente
    .\migrar-produccion.ps1 -Restore
#>

[CmdletBinding()]
param(
    [string]$Server = '192.168.1.43',
    [int]$Port = 5432,
    [string]$Database = 'sigrc',
    [string]$Username = 'postgres',
    [string]$Password = '',
    [string]$PsqlBin = '',
    [string]$RepoRoot = $(if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }),
    [string]$BackupDir = '',
    [switch]$SkipBackup,
    [switch]$SkipVerification,
    [switch]$Restore
)

# ============================================================
# Configuración
# ============================================================
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 3.0

if (-not $BackupDir) { $BackupDir = Join-Path $RepoRoot 'backups' }
if (-not (Test-Path $BackupDir)) { New-Item -ItemType Directory -Path $BackupDir | Out-Null }

# Resolver bin de PostgreSQL
function Get-PostgresBin {
    param([string]$Binary)
    if ($PsqlBin -and (Test-Path (Join-Path $PsqlBin $Binary))) {
        return Join-Path $PsqlBin $Binary
    }
    $found = Get-Command $Binary -ErrorAction SilentlyContinue
    if ($found) { return $found.Source }
    $auto = Get-ChildItem 'C:\Program Files\PostgreSQL' -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($auto) {
        $candidate = Join-Path (Join-Path $auto.FullName 'bin') $Binary
        if (Test-Path $candidate) { return $candidate }
    }
    throw "No se encontró '$Binary'. Usa -PsqlBin para indicar la carpeta bin de PostgreSQL."
}

$psqlExe  = Get-PostgresBin 'psql.exe'
$pgDumpExe = Get-PostgresBin 'pg_dump.exe'
$pgRestoreExe = Get-PostgresBin 'pg_restore.exe'

# Credenciales
if (-not $Password) {
    $sec = Read-Host -Prompt "Contraseña de PostgreSQL para $Username" -AsSecureString
    $Password = (New-Object System.Net.NetworkCredential("", $sec)).Password
    Remove-Variable sec
}
$env:PGPASSWORD = $Password

# ============================================================
# Funciones auxiliares
# ============================================================
function Write-Step { param([string]$Title, [string]$Detail = '')
    Write-Host ''
    Write-Host ('=' * 72) -ForegroundColor Cyan
    Write-Host "[PASO] $Title" -ForegroundColor Cyan
    if ($Detail) { Write-Host "       $Detail" -ForegroundColor DarkCyan }
    Write-Host ('=' * 72) -ForegroundColor Cyan
}

function Write-Ok { Write-Host "  OK -> $($args[0])" -ForegroundColor Green }
function Write-Warn { Write-Host "  AVISO -> $($args[0])" -ForegroundColor Yellow }
function Write-Fail { Write-Host "  ERROR -> $($args[0])" -ForegroundColor Red }

function Invoke-SqlFile {
    param(
        [string]$FilePath,
        [switch]$Optional
    )
    if (-not (Test-Path $FilePath)) {
        if ($Optional) { Write-Warn "Script no encontrado, se omite: $(Split-Path $FilePath -Leaf)"; return }
        throw "No se encontró el script: $FilePath"
    }
    Write-Host "  Ejecutando: $(Split-Path $FilePath -Leaf)" -ForegroundColor Gray
    # Se captura stderr a un archivo para no confundir NOTICE (benigno) con errores reales
    $errFile = Join-Path $env:TEMP ("psql_err_" + [guid]::NewGuid().ToString('N') + ".log")
    $prevEA = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $output = & $psqlExe -h $Server -p $Port -U $Username -d $Database `
        --set ON_ERROR_STOP=1 -v ON_ERROR_STOP=1 -f $FilePath 2>$errFile
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $prevEA
    $stderr = if (Test-Path $errFile) { Get-Content $errFile -Raw } else { '' }
    Remove-Item $errFile -ErrorAction SilentlyContinue
    $fatal = ($stderr -match '(?m)^psql:.*(ERROR|FATAL|PANIC)\s*:') -or $exitCode -ne 0
    if ($fatal) {
        Write-Fail "El script '$FilePath' falló (exit=$exitCode):"
        ($stderr -split "`n") | Where-Object { $_ } | ForEach-Object { Write-Host "    $_" -ForegroundColor Red }
        throw "Migración abortada en: $(Split-Path $FilePath -Leaf)"
    }
    if ($stderr -match '(?m)^psql:.*NOTICE\s*:') {
        Write-Host "    (aviso benigno, ver detalle abajo)" -ForegroundColor DarkGray
    }
    $output | Where-Object { $_ -match '^INSERT \d|^UPDATE \d|^DELETE \d|^CREATE TABLE|^ALTER TABLE|^COMMIT|^BEGIN' } |
        ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
    Write-Ok "$(Split-Path $FilePath -Leaf)"
}

# ============================================================
# Modo RESTORE
# ============================================================
if ($Restore) {
    Write-Step 'Restauración de respaldo' "Desde: $BackupDir"
    $backup = Get-ChildItem $BackupDir -Filter 'sigrc_backup_*.dump' |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $backup) { throw "No hay respaldos en $BackupDir" }
    Write-Host "  Respaldo: $($backup.FullName)" -ForegroundColor Gray
    & $pgRestoreExe -h $Server -p $Port -U $Username -d $Database `
        --clean --if-exists --no-owner $backup.FullName 2>&1 | ForEach-Object {
            if ($_ -match '^pg_restore|error') { Write-Host "    $_" -ForegroundColor Red }
        }
    if ($LASTEXITCODE -ne 0) { Write-Warn "pg_restore terminó con exit=$LASTEXITCODE (los avisos de objetos no existentes son normales con --clean)" }
    Write-Ok 'BD restaurada.'
    exit 0
}

# ============================================================
# PASO 1 — Respaldo
# ============================================================
Write-Step 'Respaldo de base de datos' "Servidor: $Server`:$Port  BD: $Database"
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$backupFile = Join-Path $BackupDir "sigrc_backup_$stamp.dump"
if ($SkipBackup) {
    Write-Warn 'Respaldo omitido (-SkipBackup).'
} else {
    Write-Host "  Generando: $backupFile" -ForegroundColor Gray
    & $pgDumpExe -h $Server -p $Port -U $Username -d $Database `
        --format=custom --file=$backupFile 2>&1 | ForEach-Object {
            if ($_ -match 'error') { Write-Host "    $_" -ForegroundColor Red }
        }
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $backupFile)) {
        throw 'El respaldo falló. No se continúa con la migración.'
    }
    $list = & $pgRestoreExe --list $backupFile 2>&1
    if ($LASTEXITCODE -ne 0 -or -not ($list | Select-String 'Table')) {
        throw "El respaldo no es legible. Verificar antes de continuar. Archivo: $backupFile"
    }
    Write-Ok "Respaldo verificado: $backupFile"
}

# ============================================================
# PASO 2 — Migración núcleo: usuarios -> empleados
# ============================================================
Write-Step 'Migración usuarios -> empleados' 'migracion-usuarios-empleados.sql'
Invoke-SqlFile (Join-Path $RepoRoot 'migracion-usuarios-empleados.sql')

# ============================================================
# PASO 3 — Módulos Talento Humano
# ============================================================
Write-Step 'Módulos Talento Humano' 'manual-funciones, gestión personal, delegaciones, confidencialidad, repositorio documental'
$scriptsTH = @(
    'migracion-manual-funciones.sql',
    'migracion-gestion-personal.sql',
    'migracion-delegaciones.sql',
    'migracion-seguridad-confidencialidad.sql',
    'migracion-repositorio-documentos-expediente.sql'
)
foreach ($s in $scriptsTH) { Invoke-SqlFile (Join-Path $RepoRoot $s) }

# ============================================================
# PASO 4 — Correspondencia
# ============================================================
Write-Step 'Migraciones de correspondencia'
Invoke-SqlFile (Join-Path $RepoRoot 'migracion-correspondencia.sql')
Invoke-SqlFile (Join-Path $RepoRoot 'migracion-correspondencia-integracion-th.sql')

# ============================================================
# PASO 5 — Fixes
# ============================================================
Write-Step 'Correcciones y fixes'
$scriptsFix = @(
    'fix-auditoria-columns.sql',
    'fix-auditoria-checks.sql',
    'fix-auditoria-anchos.sql',
    'fix-generar-numero-ticket.sql',
    'fix-numero-interno-por-usuario.sql',
    'arreglar-fk-responsables.sql'
)
foreach ($s in $scriptsFix) { Invoke-SqlFile (Join-Path $RepoRoot $s) }

# ============================================================
# PASO 6 — Verificación
# ============================================================
if (-not $SkipVerification) {
    Write-Step 'Verificación de consistencia'
    $checks = @(
        'SELECT username, empleado_id, cargo FROM sigrc.usuarios WHERE activo AND empleado_id IS NULL;'
        'SELECT count(*) AS asignaciones_sin_unidad FROM sigrc.asignacion_puesto WHERE unidad_organizacional_id IS NULL;'
        'SELECT count(*) AS asignaciones_huerfanas FROM sigrc.asignacion_puesto ap LEFT JOIN sigrc.empleado e ON e.id_empleado=ap.empleado_id WHERE e.id_empleado IS NULL;'
        'SELECT count(*) AS empleados_sin_asignacion FROM sigrc.empleado e LEFT JOIN sigrc.asignacion_puesto ap ON ap.empleado_id=e.id_empleado AND ap.es_principal AND ap.estado=''ACTIVA'' WHERE e.activo AND ap.id_asignacion IS NULL;'
        'SELECT ''empleados'' AS entidad, count(*) FROM sigrc.empleado UNION ALL SELECT ''asignaciones'', count(*) FROM sigrc.asignacion_puesto UNION ALL SELECT ''unidades'', count(*) FROM sigrc.unidad_organizacional UNION ALL SELECT ''puestos'', count(*) FROM sigrc.puesto;'
    )
    foreach ($q in $checks) {
        Write-Host ''
        Write-Host "  SQL: $($q.Substring(0, [Math]::Min(60, $q.Length)))..." -ForegroundColor Gray
        $out = & $psqlExe -h $Server -p $Port -U $Username -d $Database -c $q 2>&1
        if ($LASTEXITCODE -ne 0) { $out | ForEach-Object { Write-Host "    $_" -ForegroundColor Red } }
        else { $out | ForEach-Object { Write-Host "    $_" } }
    }
    Write-Warn 'Revisar: si "asignaciones_sin_unidad" > 0, hay usuarios sin área que obtuvieron asignación sin unidad (documentado en MIGRACION_PRODUCCION.md).'
    Write-Warn 'Revisar: si "usuarios sin empleado" > 0, verificar si es intencional.'
} else {
    Write-Step 'Verificación omitida' '-SkipVerification'
}

# ============================================================
# Final
# ============================================================
$env:PGPASSWORD = $null
Write-Host ''
Write-Host ('=' * 72) -ForegroundColor Green
Write-Host '  MIGRACIÓN COMPLETADA' -ForegroundColor Green
Write-Host ('=' * 72) -ForegroundColor Green
Write-Host "  Respaldo:     $backupFile" -ForegroundColor Gray
Write-Host '  Siguiente:     verificar en la app (login, /auth/me, organigrama, expediente LEG-...).' -ForegroundColor Gray
Write-Host ''
