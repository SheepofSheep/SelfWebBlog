param(
    [string]$BackendUrl = $(if ($env:BACKEND_URL) { $env:BACKEND_URL } else { "http://localhost:8080" }),
    [string]$FrontendUrl = $(if ($env:FRONTEND_URL) { $env:FRONTEND_URL } else { "http://localhost:5174" }),
    [string]$UploadPath = $(if ($env:UPLOAD_PATH) { $env:UPLOAD_PATH } else { Join-Path $HOME ".selfwebblog/uploads" })
)

$ErrorActionPreference = "Stop"
$failures = New-Object System.Collections.Generic.List[string]

function Add-Failure {
    param([string]$Message)
    $failures.Add($Message) | Out-Null
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

function Add-Pass {
    param([string]$Message)
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Test-HttpEndpoint {
    param(
        [string]$Name,
        [string]$Uri,
        [scriptblock]$Validate
    )

    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Uri -TimeoutSec 10
        if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 300) {
            Add-Failure "$Name returned HTTP $($response.StatusCode): $Uri"
            return
        }
        if ($Validate -and -not (& $Validate $response)) {
            Add-Failure "$Name response validation failed: $Uri"
            return
        }
        Add-Pass "$Name ok: $Uri"
    } catch {
        Add-Failure "$Name request failed: $Uri - $($_.Exception.Message)"
    }
}

Write-Host "SelfWebBlog smoke test"
Write-Host "Backend : $BackendUrl"
Write-Host "Frontend: $FrontendUrl"
Write-Host "Uploads : $UploadPath"
Write-Host ""

Test-HttpEndpoint "Backend health" "$BackendUrl/actuator/health" {
    param($Response)
    $Response.Content -match '"status"\s*:\s*"UP"'
}

Test-HttpEndpoint "Posts API" "$BackendUrl/posts" {
    param($Response)
    $Response.Content -match '"code"\s*:\s*200'
}

Test-HttpEndpoint "Frontend home" $FrontendUrl {
    param($Response)
    $Response.Content -match '<div id="app"></div>' -or $Response.Content -match "Anon's Blog"
}

try {
    if (-not (Test-Path -LiteralPath $UploadPath -PathType Container)) {
        Add-Failure "Upload directory does not exist: $UploadPath"
    } else {
        $probeFile = Join-Path $UploadPath ".smoke-test-$PID.tmp"
        Set-Content -LiteralPath $probeFile -Value "ok" -NoNewline
        Remove-Item -LiteralPath $probeFile
        Add-Pass "Upload directory is writable"
    }
} catch {
    Add-Failure "Upload directory write check failed: $($_.Exception.Message)"
}

Write-Host ""
if ($failures.Count -gt 0) {
    Write-Host "Smoke test failed. Check service logs, environment variables, reverse proxy, and upload directory permissions before keeping this release online." -ForegroundColor Red
    exit 1
}

Write-Host "Smoke test passed." -ForegroundColor Green
