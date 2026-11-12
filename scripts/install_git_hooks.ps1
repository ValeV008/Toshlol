$ErrorActionPreference = "Stop"

[String]$repositoryRoot = (& git rev-parse --show-toplevel).Trim()
if ($LASTEXITCODE -ne 0) {
    throw "Run this script from inside the repository."
}

[String]$hooksPath = (Join-Path $repositoryRoot ".githooks").Replace("\", "/")
& git config --local core.hooksPath $hooksPath
if ($LASTEXITCODE -ne 0) {
    throw "Could not configure the repository's local Git hooks path."
}

Write-Output "Git hooks enabled from $hooksPath"
