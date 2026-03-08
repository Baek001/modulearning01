param(
    [Parameter(Mandatory = $true)]
    [string]$PsqlPath,
    [Parameter(Mandatory = $true)]
    [string]$PgHost,
    [int]$PgPort = 5432,
    [Parameter(Mandatory = $true)]
    [string]$PgDatabase,
    [Parameter(Mandatory = $true)]
    [string]$PgUser,
    [Parameter(Mandatory = $true)]
    [string]$PgPassword,
    [Parameter(Mandatory = $true)]
    [string]$DdlDir,
    [Parameter(Mandatory = $true)]
    [string]$CsvDir,
    [string]$Schema = "public",
    [string]$LoadOrderFile,
    [string]$LogDir = ".\migration-logs",
    [switch]$SkipSchema,
    [switch]$SkipImport,
    [switch]$SkipConstraints,
    [switch]$SkipSequenceReset,
    [switch]$SkipCountVerify
)

$ErrorActionPreference = "Stop"

function Invoke-PsqlFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath
    )

    Write-Host "[SQL] $FilePath"
    & $PsqlPath `
        -h $PgHost `
        -p $PgPort `
        -d $PgDatabase `
        -U $PgUser `
        -v ON_ERROR_STOP=1 `
        -f $FilePath

    if ($LASTEXITCODE -ne 0) {
        throw "psql failed: $FilePath"
    }
}

if (-not (Test-Path -LiteralPath $PsqlPath)) {
    throw "psql not found: $PsqlPath"
}

$ddlPath = (Resolve-Path -LiteralPath $DdlDir).Path
$csvPath = (Resolve-Path -LiteralPath $CsvDir).Path
$scriptRoot = $PSScriptRoot
$resetSql = Join-Path $scriptRoot "reset-sequences.sql"
$importScript = Join-Path $scriptRoot "import-csv.ps1"
$verifyScript = Join-Path $scriptRoot "verify-csv-counts.ps1"

if (-not (Test-Path -LiteralPath $resetSql)) {
    throw "Missing reset SQL: $resetSql"
}
if (-not (Test-Path -LiteralPath $importScript)) {
    throw "Missing import script: $importScript"
}
if (-not (Test-Path -LiteralPath $verifyScript)) {
    throw "Missing verify script: $verifyScript"
}

New-Item -ItemType Directory -Path $LogDir -Force | Out-Null

$schemaSql = Join-Path $ddlPath "schema_postgres.sql"
$constraintsSql = Join-Path $ddlPath "constraints_postgres.sql"
$indexesSql = Join-Path $ddlPath "indexes_postgres.sql"

$env:PGPASSWORD = $PgPassword

Write-Host "==== PostgreSQL migration start ===="
Write-Host "DB: $PgHost:$PgPort/$PgDatabase  USER: $PgUser  SCHEMA: $Schema"
Write-Host "DDL: $ddlPath"
Write-Host "CSV: $csvPath"

try {
    if (-not $SkipSchema) {
        if (-not (Test-Path -LiteralPath $schemaSql)) {
            throw "Missing schema DDL: $schemaSql"
        }
        Invoke-PsqlFile -FilePath $schemaSql
    }

    if (-not $SkipImport) {
        & $importScript `
            -PsqlPath $PsqlPath `
            -PgHost $PgHost `
            -PgPort $PgPort `
            -PgDatabase $PgDatabase `
            -PgUser $PgUser `
            -PgPassword $PgPassword `
            -CsvDir $csvPath `
            -Schema $Schema `
            -LoadOrderFile $LoadOrderFile
        if ($LASTEXITCODE -ne 0) {
            throw "CSV import failed."
        }
    }

    if (-not $SkipConstraints -and (Test-Path -LiteralPath $constraintsSql)) {
        Invoke-PsqlFile -FilePath $constraintsSql
    }

    if (-not $SkipConstraints -and (Test-Path -LiteralPath $indexesSql)) {
        Invoke-PsqlFile -FilePath $indexesSql
    }

    if (-not $SkipSequenceReset) {
        Invoke-PsqlFile -FilePath $resetSql
    }

    if (-not $SkipCountVerify) {
        & $verifyScript `
            -PsqlPath $PsqlPath `
            -PgHost $PgHost `
            -PgPort $PgPort `
            -PgDatabase $PgDatabase `
            -PgUser $PgUser `
            -PgPassword $PgPassword `
            -CsvDir $csvPath `
            -Schema $Schema `
            -LoadOrderFile $LoadOrderFile
        if ($LASTEXITCODE -ne 0) {
            throw "CSV/PostgreSQL count verification failed."
        }
    }
}
finally {
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}

Write-Host "==== PostgreSQL migration completed successfully ===="
