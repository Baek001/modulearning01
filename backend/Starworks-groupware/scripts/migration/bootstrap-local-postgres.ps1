param(
    [string]$ContainerName = "starworks-pg",
    [string]$Image = "postgres:16",
    [string]$PgHost = "127.0.0.1",
    [int]$PgPort = 5432,
    [string]$PgDatabase = "starworks",
    [string]$PgUser = "postgres",
    [string]$PgPassword = "postgres",
    [string]$DdlDir = ".\migration-input\ddl",
    [switch]$ResetSchema
)

$ErrorActionPreference = "Stop"

function Test-Docker {
    try {
        $null = docker info --format "{{.ServerVersion}}" 2>$null
        return $true
    } catch {
        return $false
    }
}

function Wait-PostgresReady {
    param(
        [string]$Container,
        [string]$User,
        [string]$Database,
        [int]$TimeoutSeconds = 90
    )

    $start = Get-Date
    while (((Get-Date) - $start).TotalSeconds -lt $TimeoutSeconds) {
        & docker exec $Container pg_isready -U $User -d $Database 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "PostgreSQL container was not ready within $TimeoutSeconds seconds."
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

    Write-Host "[SQL] $FilePath"
    & docker cp $FilePath "$Container`:$target"
    if ($LASTEXITCODE -ne 0) { throw "docker cp failed: $FilePath" }

    & docker exec -e "PGPASSWORD=$PgPassword" $Container psql -U $User -d $Database -v ON_ERROR_STOP=1 -f $target
    if ($LASTEXITCODE -ne 0) { throw "psql failed: $leaf" }
}

function Invoke-SqlCommandInContainer {
    param(
        [string]$Container,
        [string]$Sql,
        [string]$User,
        [string]$Database
    )

    & docker exec -e "PGPASSWORD=$PgPassword" $Container psql -U $User -d $Database -v ON_ERROR_STOP=1 -c $Sql
    if ($LASTEXITCODE -ne 0) { throw "psql failed for inline SQL command." }
}

if (-not (Test-Docker)) {
    throw "Docker Desktop is not running. Start Docker and retry."
}

$ddlPath = (Resolve-Path -LiteralPath $DdlDir).Path
$schemaSql = Join-Path $ddlPath "schema_postgres.sql"
$constraintsSql = Join-Path $ddlPath "constraints_postgres.sql"
$indexesSql = Join-Path $ddlPath "indexes_postgres.sql"
$seedSql = Join-Path $ddlPath "seed_sample.sql"

$exists = (& docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $ContainerName })
if (-not $exists) {
    Write-Host "Creating PostgreSQL container: $ContainerName"
    & docker run -d `
        --name $ContainerName `
        -e "POSTGRES_USER=$PgUser" `
        -e "POSTGRES_PASSWORD=$PgPassword" `
        -e "POSTGRES_DB=$PgDatabase" `
        -p "$PgPort`:5432" `
        $Image | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Failed to create container: $ContainerName" }
} else {
    Write-Host "Starting existing container: $ContainerName"
    & docker start $ContainerName | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Failed to start container: $ContainerName" }
}

Wait-PostgresReady -Container $ContainerName -User $PgUser -Database $PgDatabase

if ($ResetSchema) {
    Write-Host "Resetting schema: public"
    $resetSql = "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO $PgUser; GRANT ALL ON SCHEMA public TO public;"
    Invoke-SqlCommandInContainer -Container $ContainerName -Sql $resetSql -User $PgUser -Database $PgDatabase
}

Invoke-SqlFileInContainer -Container $ContainerName -FilePath $schemaSql -User $PgUser -Database $PgDatabase

if (Test-Path -LiteralPath $constraintsSql) {
    Invoke-SqlFileInContainer -Container $ContainerName -FilePath $constraintsSql -User $PgUser -Database $PgDatabase
}
if (Test-Path -LiteralPath $indexesSql) {
    Invoke-SqlFileInContainer -Container $ContainerName -FilePath $indexesSql -User $PgUser -Database $PgDatabase
}
if (Test-Path -LiteralPath $seedSql) {
    Invoke-SqlFileInContainer -Container $ContainerName -FilePath $seedSql -User $PgUser -Database $PgDatabase
}

Write-Host ""
Write-Host "Local PostgreSQL bootstrap complete."
Write-Host "Container : $ContainerName"
Write-Host "Endpoint  : ${PgHost}:$PgPort/$PgDatabase"
Write-Host "User      : $PgUser"
