@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "REPOSITORY_ROOT=%~dp0"
if "!REPOSITORY_ROOT:~-1!"=="\" set "REPOSITORY_ROOT=!REPOSITORY_ROOT:~0,-1!"
set "BACKEND_DIR=!REPOSITORY_ROOT!\SelfWebBlog"
set "FRONTEND_DIR=!BACKEND_DIR!\blog-frontend"
set "RUNTIME_DIR=!REPOSITORY_ROOT!\.dev-runtime"
set "BACKEND_STATE=!RUNTIME_DIR!\backend.pid"
set "FRONTEND_STATE=!RUNTIME_DIR!\frontend.pid"
set "BACKEND_OUT=!RUNTIME_DIR!\backend.out.log"
set "BACKEND_ERR=!RUNTIME_DIR!\backend.err.log"
set "FRONTEND_OUT=!RUNTIME_DIR!\frontend.out.log"
set "FRONTEND_ERR=!RUNTIME_DIR!\frontend.err.log"

echo ========================================
echo   SelfWebBlog - Development Environment
echo ========================================
echo.

if not exist "!BACKEND_DIR!\pom.xml" (
    echo [ERROR] Backend project not found: !BACKEND_DIR!
    exit /b 1
)

if not exist "!FRONTEND_DIR!\package.json" (
    echo [ERROR] Frontend project not found: !FRONTEND_DIR!
    exit /b 1
)

call :require_command mvn.cmd Maven || exit /b 1
call :require_command pnpm.cmd pnpm || exit /b 1
call :require_command powershell.exe PowerShell || exit /b 1

call :require_free_port 8080 Backend || exit /b 1
call :require_free_port 5174 Frontend || exit /b 1

if not exist "!RUNTIME_DIR!" mkdir "!RUNTIME_DIR!"
del /q "!BACKEND_STATE!" "!FRONTEND_STATE!" >nul 2>&1
del /q "!BACKEND_OUT!" "!BACKEND_ERR!" "!FRONTEND_OUT!" "!FRONTEND_ERR!" >nul 2>&1

echo [1/2] Starting backend on http://127.0.0.1:8080 ...
set "SELFWEBBLOG_WORKING_DIR=!BACKEND_DIR!"
set "SELFWEBBLOG_STDOUT=!BACKEND_OUT!"
set "SELFWEBBLOG_STDERR=!BACKEND_ERR!"
set "SELFWEBBLOG_STATE=!BACKEND_STATE!"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$p = Start-Process -FilePath $env:ComSpec -ArgumentList @('/d','/s','/c','mvn.cmd -Dspring-boot.run.profiles=dev spring-boot:run') -WorkingDirectory $env:SELFWEBBLOG_WORKING_DIR -WindowStyle Hidden -RedirectStandardOutput $env:SELFWEBBLOG_STDOUT -RedirectStandardError $env:SELFWEBBLOG_STDERR -PassThru; ('{0}|{1}' -f $p.Id, $p.StartTime.ToUniversalTime().ToFileTimeUtc()) | Set-Content -LiteralPath $env:SELFWEBBLOG_STATE -Encoding Ascii"
if errorlevel 1 (
    echo [ERROR] Could not launch the backend process.
    exit /b 1
)

echo     Waiting for the backend health check...
call :wait_for_url "http://127.0.0.1:8080/actuator/health" "!BACKEND_STATE!" 90 Backend
if errorlevel 1 goto :startup_failed

echo [2/2] Starting frontend on http://127.0.0.1:5174 ...
set "SELFWEBBLOG_WORKING_DIR=!FRONTEND_DIR!"
set "SELFWEBBLOG_STDOUT=!FRONTEND_OUT!"
set "SELFWEBBLOG_STDERR=!FRONTEND_ERR!"
set "SELFWEBBLOG_STATE=!FRONTEND_STATE!"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$p = Start-Process -FilePath $env:ComSpec -ArgumentList @('/d','/s','/c','pnpm.cmd dev --host 127.0.0.1 --strictPort') -WorkingDirectory $env:SELFWEBBLOG_WORKING_DIR -WindowStyle Hidden -RedirectStandardOutput $env:SELFWEBBLOG_STDOUT -RedirectStandardError $env:SELFWEBBLOG_STDERR -PassThru; ('{0}|{1}' -f $p.Id, $p.StartTime.ToUniversalTime().ToFileTimeUtc()) | Set-Content -LiteralPath $env:SELFWEBBLOG_STATE -Encoding Ascii"
if errorlevel 1 (
    echo [ERROR] Could not launch the frontend process.
    call "!REPOSITORY_ROOT!\stop-dev.bat" >nul 2>&1
    exit /b 1
)

echo     Waiting for the frontend...
call :wait_for_url "http://127.0.0.1:5174/" "!FRONTEND_STATE!" 45 Frontend
if errorlevel 1 goto :startup_failed

echo.
echo ========================================
echo   Backend : http://127.0.0.1:8080
echo   Frontend: http://127.0.0.1:5174
echo   Logs    : !RUNTIME_DIR!
echo ========================================
echo Use stop-dev.bat to stop both services.
exit /b 0

:startup_failed
echo.
echo [ERROR] Development environment did not become ready.
echo         Backend log: !BACKEND_ERR!
echo         Frontend log: !FRONTEND_ERR!
call "!REPOSITORY_ROOT!\stop-dev.bat" >nul 2>&1
exit /b 1

:require_command
where.exe %~1 >nul 2>&1
if errorlevel 1 (
    echo [ERROR] %~2 is not available on PATH.
    exit /b 1
)
exit /b 0

:require_free_port
set "SELFWEBBLOG_PORT=%~1"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, [int]$env:SELFWEBBLOG_PORT); try { $listener.Start(); $listener.Stop(); exit 0 } catch { exit 1 }" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] %~2 port %~1 is already in use. Stop that service first.
    exit /b 1
)
exit /b 0

:wait_for_url
set "SELFWEBBLOG_URL=%~1"
set "SELFWEBBLOG_STATE=%~2"
set "SELFWEBBLOG_TIMEOUT=%~3"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$parts = (Get-Content -LiteralPath $env:SELFWEBBLOG_STATE -Raw).Trim().Split('|'); $deadline = [DateTime]::UtcNow.AddSeconds([int]$env:SELFWEBBLOG_TIMEOUT); while ([DateTime]::UtcNow -lt $deadline) { $process = Get-Process -Id ([int]$parts[0]) -ErrorAction SilentlyContinue; if ($null -eq $process) { exit 2 }; try { $response = Invoke-WebRequest -UseBasicParsing -Uri $env:SELFWEBBLOG_URL -TimeoutSec 2; if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) { exit 0 } } catch {}; Start-Sleep -Milliseconds 500 }; exit 1" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] %~4 failed to become ready at %~1.
    exit /b 1
)
echo [OK] %~4 is ready.
exit /b 0
