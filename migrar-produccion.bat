@echo off
rem ============================================================
rem SIGRC - Migracion de base de datos en produccion (wrapper)
rem Doble clic = ejecuta migrar-produccion.ps1 en la carpeta local
rem ============================================================
setlocal
cd /d "%~dp0"

rem Verificar que PowerShell existe
where powershell.exe >nul 2>&1
if errorlevel 1 (
    echo [ERROR] No se encontro PowerShell en el PATH.
    pause
    exit /b 1
)

echo ============================================================
echo  SIGRC - Migracion de base de datos en produccion
echo  Se abrira una ventana de PowerShell.
echo  La contrasena se solicitara de forma segura si no se envia.
echo ============================================================
echo.

rem Invoca el script con la carpeta del .bat como raiz del repo.
rem Agrega parametros despues de "migrar-produccion.ps1" si los necesitas,
rem por ejemplo: -Server 192.168.1.43 -Username postgres
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0migrar-produccion.ps1"

echo.
echo Proceso finalizado (revise el codigo de salida: %errorlevel%).
pause
endlocal
