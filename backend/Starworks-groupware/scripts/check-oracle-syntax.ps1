param(
    [string]$MapperRoot = ".\src\main\resources\kr\or\ddit\mybatis\mapper"
)

$patterns = @(
    "ROWNUM",
    "CONNECT BY",
    "START WITH",
    "LISTAGG",
    "NVL\(",
    "SYSDATE",
    "FROM DUAL",
    "TRUNC\(",
    "INSERT ALL"
)

$hasError = $false
foreach ($pattern in $patterns) {
    $matches = Get-ChildItem -LiteralPath $MapperRoot -Recurse -Filter *.xml |
        Select-String -Pattern $pattern -CaseSensitive:$false

    if ($matches) {
        $hasError = $true
        Write-Host "`n[FOUND] $pattern" -ForegroundColor Yellow
        $matches | ForEach-Object {
            Write-Host ("{0}:{1}" -f $_.Path, $_.LineNumber)
        }
    }
}

if ($hasError) {
    Write-Host "`nOracle-specific patterns still exist." -ForegroundColor Red
    exit 1
}

Write-Host "No Oracle-specific patterns detected in mapper XML files." -ForegroundColor Green
exit 0
