# Sauvegarde PostgreSQL Get STORE.
# Usage :
#   $env:DB_URL = "jdbc:postgresql://host:5432/troco?sslmode=require"
#   $env:DB_USER = "user"
#   $env:DB_PASSWORD = "secret"
#   ./backup-postgres.ps1
# Le fichier est écrit dans ./backups (ignoré du dépôt si vous l'ajoutez au .gitignore).

$ErrorActionPreference = "Stop"
if (-not $env:DB_URL) { throw "DB_URL manquant (jdbc:postgresql://...)" }

$jdbc = $env:DB_URL -replace '^jdbc:', ''
$uri = [Uri]$jdbc
$db = $uri.AbsolutePath.TrimStart('/')
if (-not $db) { throw "Nom de base introuvable dans DB_URL" }

$outDir = Join-Path $PSScriptRoot "backups"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outFile = Join-Path $outDir "troco-$stamp.dump"

$env:PGPASSWORD = $env:DB_PASSWORD
& pg_dump --format=custom --no-owner --host $uri.Host --port $(if ($uri.Port -gt 0) { $uri.Port } else { 5432 }) --username $env:DB_USER --dbname $db --file $outFile
if ($LASTEXITCODE -ne 0) { throw "pg_dump a échoué ($LASTEXITCODE)" }
Write-Output "Sauvegarde écrite : $outFile"
