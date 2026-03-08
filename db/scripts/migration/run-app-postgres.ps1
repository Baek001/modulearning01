param(
    [string]$PgHost = "127.0.0.1",
    [int]$PgPort = 5432,
    [string]$PgDatabase = "starworks",
    [string]$PgUser = "postgres",
    [string]$PgPassword = "postgres",
    [string]$RepoRoot = ".",
    [string]$JavaHome = ""
)

$ErrorActionPreference = "Stop"

$resolvedRepo = (Resolve-Path -LiteralPath $RepoRoot).Path
$mvnw = Join-Path $resolvedRepo "mvnw.cmd"
if (-not (Test-Path -LiteralPath $mvnw)) {
    throw "mvnw.cmd not found under: $resolvedRepo"
}

if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
    if (-not (Test-Path -LiteralPath $JavaHome)) {
        throw "JAVA_HOME not found: $JavaHome"
    }
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;$env:Path"
}

$env:DB_URL = "jdbc:postgresql://$PgHost`:$PgPort/$PgDatabase"
$env:DB_USERNAME = $PgUser
$env:DB_PASSWORD = $PgPassword
$env:DB_DRIVER = "org.postgresql.Driver"
if (-not $env:JWT_SECRET_KEY) { $env:JWT_SECRET_KEY = "local-dev-jwt-secret-key-please-change" }
if (-not $env:AWS_ACCESS_KEY) { $env:AWS_ACCESS_KEY = "local" }
if (-not $env:AWS_SECRET_KEY) { $env:AWS_SECRET_KEY = "local" }
if (-not $env:AWS_REGION) { $env:AWS_REGION = "ap-northeast-2" }
if (-not $env:AWS_BUCKET) { $env:AWS_BUCKET = "local-bucket" }
if (-not $env:SPRING_AI_VERTEX_AI_GEMINI_PROJECT_ID) { $env:SPRING_AI_VERTEX_AI_GEMINI_PROJECT_ID = "local-dev-project" }
if (-not $env:SPRING_AI_VERTEX_AI_GEMINI_LOCATION) { $env:SPRING_AI_VERTEX_AI_GEMINI_LOCATION = "us-central1" }
if (-not $env:SPRING_AI_VERTEX_AI_EMBEDDING_PROJECT_ID) { $env:SPRING_AI_VERTEX_AI_EMBEDDING_PROJECT_ID = "local-dev-project" }
if (-not $env:SPRING_AI_VERTEX_AI_EMBEDDING_LOCATION) { $env:SPRING_AI_VERTEX_AI_EMBEDDING_LOCATION = "us-central1" }
if (-not $env:SPRING_MAIN_LAZY_INITIALIZATION) { $env:SPRING_MAIN_LAZY_INITIALIZATION = "true" }
if (-not $env:SERVER_PORT) { $env:SERVER_PORT = "18080" }

Write-Host "Running app with PostgreSQL"
Write-Host "DB_URL=$env:DB_URL"
Write-Host "DB_USERNAME=$env:DB_USERNAME"
Write-Host "SERVER_PORT=$env:SERVER_PORT"

Push-Location $resolvedRepo
try {
    & $mvnw "-DskipTests" "spring-boot:run"
    if ($LASTEXITCODE -ne 0) {
        throw "Application failed to start."
    }
}
finally {
    Pop-Location
}
