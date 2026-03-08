param(
    [string]$ContainerName = "starworks-db",
    [string]$PgDatabase = "starworks",
    [string]$PgUser = "postgres",
    [string]$PgPassword = "postgres",
    [string]$DdlDir
)

$ErrorActionPreference = "Stop"

if (-not $DdlDir) {
    $DdlDir = Join-Path $PSScriptRoot "..\..\migration-input\ddl"
}

function Invoke-SqlFileInContainer {
    param(
        [string]$Container,
        [string]$FilePath,
        [string]$User,
        [string]$Database
    )

    if (-not (Test-Path -LiteralPath $FilePath)) {
        throw "Missing SQL file: $FilePath"
    }

    $leaf = [IO.Path]::GetFileName($FilePath)
    $target = "/tmp/$leaf"

    Write-Host "[SQL] $leaf"
    & docker cp $FilePath "$Container`:$target"
    if ($LASTEXITCODE -ne 0) { throw "docker cp failed: $FilePath" }

    & docker exec -e "PGPASSWORD=$PgPassword" $Container psql -U $User -d $Database -v ON_ERROR_STOP=1 -f $target
    if ($LASTEXITCODE -ne 0) { throw "psql failed: $leaf" }
}

$exists = & docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $ContainerName }
if (-not $exists) {
    throw "Container not found: $ContainerName"
}

$isRunning = (& docker inspect -f "{{.State.Running}}" $ContainerName).Trim()
if ($isRunning -ne "true") {
    Write-Host "Starting container: $ContainerName"
    & docker start $ContainerName | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Failed to start container: $ContainerName" }
}

$ddlPath = (Resolve-Path -LiteralPath $DdlDir).Path
$files = @(
    (Join-Path $ddlPath "runtime_fixes_postgres.sql"),
    (Join-Path $ddlPath "seed_sample.sql"),
    (Join-Path $ddlPath "seed_operational_demo.sql"),
    (Join-Path $ddlPath "seed_community_demo.sql"),
    (Join-Path $ddlPath "seed_board_demo.sql"),
    (Join-Path $ddlPath "seed_calendar_demo.sql"),
    (Join-Path $ddlPath "seed_contract_demo.sql")
)

foreach ($file in $files) {
    Invoke-SqlFileInContainer -Container $ContainerName -FilePath $file -User $PgUser -Database $PgDatabase
}

Write-Host ""
Write-Host "Local demo backfill complete."
Write-Host "Container : $ContainerName"
Write-Host "Database  : $PgDatabase"
