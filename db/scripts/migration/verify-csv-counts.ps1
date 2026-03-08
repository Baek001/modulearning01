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

function Get-DataLineCount {
    param([string]$FilePath)

    $lineCount = 0L
    $reader = [System.IO.File]::OpenText($FilePath)
    try {
        while ($null -ne $reader.ReadLine()) {
            $lineCount++
        }
    }
    finally {
        $reader.Close()
    }

    if ($lineCount -eq 0) { return 0L }
    return ($lineCount - 1L)  # header 제외
}

if (-not (Test-Path -LiteralPath $PsqlPath)) {
    throw "psql not found: $PsqlPath"
}

$csvPath = (Resolve-Path -LiteralPath $CsvDir).Path
$tables = Get-TableOrder -CsvPath $csvPath -OrderFile $LoadOrderFile

if (-not $tables -or $tables.Count -eq 0) {
    throw "No tables resolved for count verification."
}

$env:PGPASSWORD = $PgPassword
$hasMismatch = $false

try {
    foreach ($table in $tables) {
        $csvFile = Join-Path $csvPath ("{0}.csv" -f $table)
        if (-not (Test-Path -LiteralPath $csvFile)) {
            Write-Host "[SKIP] Missing CSV for table: $table"
            continue
        }

        $csvCount = Get-DataLineCount -FilePath $csvFile
        $sql = "SELECT COUNT(*) FROM `"$Schema`".`"$table`";"
        $pgCountRaw = & $PsqlPath -h $PgHost -p $PgPort -d $PgDatabase -U $PgUser -t -A -v ON_ERROR_STOP=1 -c $sql

        if ($LASTEXITCODE -ne 0) {
            Write-Host ("[FAIL] table={0} csv={1} pg=QUERY_ERROR" -f $table, $csvCount)
            $hasMismatch = $true
            continue
        }

        $pgCount = ($pgCountRaw | Select-Object -First 1).Trim()
        if ([string]::IsNullOrWhiteSpace($pgCount)) {
            $pgCount = "0"
        }

        if ([int64]$pgCount -ne [int64]$csvCount) {
            Write-Host ("[MISMATCH] table={0} csv={1} pg={2}" -f $table, $csvCount, $pgCount)
            $hasMismatch = $true
        }
        else {
            Write-Host ("[OK] table={0} count={1}" -f $table, $pgCount)
        }
    }
}
finally {
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}

if ($hasMismatch) {
    Write-Host "Count verification failed."
    exit 1
}

Write-Host "Count verification passed."
exit 0
