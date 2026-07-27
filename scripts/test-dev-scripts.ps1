$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$startScriptPath = Join-Path $repositoryRoot 'start-dev.bat'
$stopScriptPath = Join-Path $repositoryRoot 'stop-dev.bat'
$startScript = Get-Content -LiteralPath $startScriptPath -Raw
$stopScript = Get-Content -LiteralPath $stopScriptPath -Raw

function Assert-Matches {
    param(
        [string] $Content,
        [string] $Pattern,
        [string] $Message
    )

    if ($Content -notmatch $Pattern) {
        throw $Message
    }
}

function Assert-DoesNotMatch {
    param(
        [string] $Content,
        [string] $Pattern,
        [string] $Message
    )

    if ($Content -match $Pattern) {
        throw $Message
    }
}

Assert-Matches $startScript '%~dp0' 'start-dev.bat must resolve the repository from its own location.'
Assert-Matches $stopScript '%~dp0' 'stop-dev.bat must resolve the repository from its own location.'
Assert-DoesNotMatch $startScript '[A-Za-z]:\\' 'start-dev.bat must not contain a machine-specific absolute path.'
Assert-DoesNotMatch $stopScript '[A-Za-z]:\\' 'stop-dev.bat must not contain a machine-specific absolute path.'

Assert-Matches $startScript '--strictPort' 'The frontend must fail instead of silently changing its configured port.'
Assert-Matches $startScript '/actuator/health' 'The startup script must wait for the backend health endpoint.'
Assert-Matches $startScript '\.dev-runtime' 'The startup script must record owned process IDs.'

$backendReadyIndex = $startScript.IndexOf('call :wait_for_url "http://127.0.0.1:8080/actuator/health"')
$frontendLaunchIndex = $startScript.IndexOf("'pnpm.cmd dev --host 127.0.0.1 --strictPort'")
if ($backendReadyIndex -lt 0 -or $frontendLaunchIndex -lt 0 -or $backendReadyIndex -gt $frontendLaunchIndex) {
    throw 'The backend health check must complete before the frontend process is launched.'
}

Assert-Matches $stopScript '\.dev-runtime' 'The stop script must use the process IDs recorded by start-dev.bat.'
Assert-Matches $stopScript 'taskkill[^\r\n]*/T' 'The stop script must terminate each owned process tree.'
Assert-DoesNotMatch $stopScript 'netstat' 'The stop script must not kill arbitrary processes merely because they own a port.'
Assert-Matches $stopScript 'FAILED_COUNT' 'The stop script must track failures and return a non-zero exit code.'

Write-Host 'Development script contract checks passed.'
