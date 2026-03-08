param(
    [Parameter(Mandatory = $true)]
    [string]$CsvDir,
    [string]$OutputFile = ".\oracle_table_counts.sql"
)

$ErrorActionPreference = "Stop"

$csvPath = (Resolve-Path -LiteralPath $CsvDir).Path
$tables = Get-ChildItem -LiteralPath $csvPath -Filter *.csv |
    Sort-Object Name |
    ForEach-Object { $_.BaseName.ToUpperInvariant() }

if (-not $tables -or $tables.Count -eq 0) {
    throw "No CSV files found in: $csvPath"
}

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("SET PAGESIZE 0 FEEDBACK OFF VERIFY OFF HEADING OFF ECHO OFF;")
$lines.Add("SPOOL table_counts.csv")
$lines.Add("SELECT 'TABLE_NAME,ROW_COUNT' FROM DUAL;")
foreach ($table in $tables) {
    $lines.Add(("SELECT '{0},' || COUNT(*) FROM {0};" -f $table))
}
$lines.Add("SPOOL OFF")
$lines.Add("EXIT;")

[System.IO.File]::WriteAllLines($OutputFile, $lines, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "Generated: $OutputFile"
