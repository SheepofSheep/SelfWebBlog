@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "REPOSITORY_ROOT=%~dp0"
if "!REPOSITORY_ROOT:~-1!"=="\" set "REPOSITORY_ROOT=!REPOSITORY_ROOT:~0,-1!"
set "RUNTIME_DIR=!REPOSITORY_ROOT!\.dev-runtime"

echo ========================================
echo   SelfWebBlog - Stop Dev Environment
echo ========================================

set "STOPPED_COUNT=0"
set "FAILED_COUNT=0"
call :stop_tracked "Backend" "!RUNTIME_DIR!\backend.pid"
call :stop_tracked "Frontend" "!RUNTIME_DIR!\frontend.pid"

if "!STOPPED_COUNT!"=="0" (
    echo No tracked development services are running.
) else (
    echo Stopped !STOPPED_COUNT! development service^(s^).
)

if not "!FAILED_COUNT!"=="0" (
    echo [ERROR] !FAILED_COUNT! development service^(s^) could not be stopped.
    exit /b 1
)

exit /b 0

:stop_tracked
set "SERVICE_NAME=%~1"
set "STATE_FILE=%~2"

if not exist "!STATE_FILE!" (
    echo [SKIP] !SERVICE_NAME!: no process record.
    exit /b 0
)

set "PROCESS_PID="
set "PROCESS_STARTED_AT="
for /f "usebackq tokens=1,2 delims=|" %%A in ("!STATE_FILE!") do (
    set "PROCESS_PID=%%A"
    set "PROCESS_STARTED_AT=%%B"
)

if not defined PROCESS_PID (
    echo [SKIP] !SERVICE_NAME!: invalid process record removed.
    del /q "!STATE_FILE!" >nul 2>&1
    exit /b 0
)

set "SELFWEBBLOG_PROCESS_PID=!PROCESS_PID!"
set "SELFWEBBLOG_PROCESS_STARTED_AT=!PROCESS_STARTED_AT!"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$process = Get-Process -Id ([int]$env:SELFWEBBLOG_PROCESS_PID) -ErrorAction SilentlyContinue; if ($null -eq $process) { exit 2 }; if ($process.StartTime.ToUniversalTime().ToFileTimeUtc() -ne [long]$env:SELFWEBBLOG_PROCESS_STARTED_AT) { exit 3 }; exit 0" >nul 2>&1
set "PROCESS_CHECK=!ERRORLEVEL!"

if "!PROCESS_CHECK!"=="2" (
    echo [SKIP] !SERVICE_NAME!: process already stopped.
    del /q "!STATE_FILE!" >nul 2>&1
    exit /b 0
)

if not "!PROCESS_CHECK!"=="0" (
    echo [SKIP] !SERVICE_NAME!: stale process record removed; no process was killed.
    del /q "!STATE_FILE!" >nul 2>&1
    exit /b 0
)

taskkill /PID "!PROCESS_PID!" /T /F >nul 2>&1
if errorlevel 1 (
    set /a FAILED_COUNT+=1
    echo [WARN] !SERVICE_NAME!: process could not be stopped.
    echo        Process record kept for another stop attempt.
    exit /b 0
)

set /a STOPPED_COUNT+=1
echo [OK] !SERVICE_NAME! stopped.
del /q "!STATE_FILE!" >nul 2>&1
exit /b 0
