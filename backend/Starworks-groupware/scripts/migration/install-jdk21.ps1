param(
    [string]$PackageId = "EclipseAdoptium.Temurin.21.JDK"
)

$ErrorActionPreference = "Stop"

Write-Host "Installing JDK 21 via winget package: $PackageId"
& winget install --id $PackageId --exact --accept-source-agreements --accept-package-agreements
if ($LASTEXITCODE -ne 0) {
    throw "winget install failed for $PackageId"
}

Write-Host ""
Write-Host "Installed JDK 21."
Write-Host "Open a new terminal and run: java -version"
