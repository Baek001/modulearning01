param(
    [string]$EnvFile = ".dev.vars"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Resolve-Path (Join-Path $scriptDir "..")
$envPath = Join-Path $projectRoot $EnvFile

if (-not (Test-Path $envPath)) {
    throw "Env file not found: $envPath"
}

$lines = Get-Content $envPath | Where-Object {
    $_ -and -not $_.Trim().StartsWith("#")
}

foreach ($line in $lines) {
    $parts = $line -split "=", 2
    if ($parts.Count -ne 2) {
        continue
    }

    $name = $parts[0].Trim()
    $value = $parts[1]

    if ([string]::IsNullOrWhiteSpace($name)) {
        continue
    }

    Write-Host "Syncing Cloudflare secret: $name"
    $value | npx wrangler secret put $name | Out-Null
}

Write-Host "Cloudflare secret sync completed."
