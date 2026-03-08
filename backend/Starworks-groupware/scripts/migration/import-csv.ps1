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
    [string]$CsvDir,
    [string]$Schema = "public",
    [string]$LoadOrderFile
)

$ErrorActionPreference = "Stop"

function Get-TableOrder {
    param(
        [string]$CsvPath,
        [string]$OrderFile
    )

    if ($OrderFile -and (Test-Path -LiteralPath $OrderFile)) {
        return Get-Content -LiteralPath $OrderFile |
            ForEach-Object { $_.Trim() } |
            Where-Object { $_ -and -not $_.StartsWith("#") }
    }

    return Get-ChildItem -LiteralPath $CsvPath -Filter *.csv |
        Sort-Object Name |
        ForEach-Object { $_.BaseName }
}

if (-not (Test-Path -LiteralPath $PsqlPath)) {
    throw "psql not found: $PsqlPath"
}

$csvPath = (Resolve-Path -LiteralPath $CsvDir).Path
$tables = Get-TableOrder -CsvPath $csvPath -OrderFile $LoadOrderFile

if (-not $tables -or $tables.Count -eq 0) {
    throw "No tables resolved for CSV import."
}

$env:PGPASSWORD = $PgPassword

try {
    foreach ($table in $tables) {
        $csvFile = Join-Path $csvPath ("{0}.csv" -f $table)
        if (-not (Test-Path -LiteralPath $csvFile)) {
            Write-Host "[SKIP] Missing CSV for table: $table ($csvFile)"
            continue
        }

        $escapedPath = $csvFile.Replace("\", "\\").Replace("'", "''")
        $copySql = "\copy `"$Schema`".`"$table`" FROM '$escapedPath' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8', NULL '')"

        Write-Host "[LOAD] $table <- $csvFile"
        & $PsqlPath `
            -h $PgHost `
            -p $PgPort `
            -d $PgDatabase `
            -U $PgUser `
            -v ON_ERROR_STOP=1 `
            -c $copySql

        if ($LASTEXITCODE -ne 0) {
            throw "CSV import failed for table: $table"
        }
    }
}
finally {
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}

Write-Host "CSV import finished."
exit 0
