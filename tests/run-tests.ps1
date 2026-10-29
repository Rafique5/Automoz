$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$driver = Join-Path $env:USERPROFILE '.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar'
if (-not (Test-Path -LiteralPath $driver)) {
    throw 'MySQL Connector/J nao encontrado. Importe pom.xml no IntelliJ/Maven primeiro.'
}

$classes = Join-Path $env:TEMP 'automoz-test-classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sources = & rg --files (Join-Path $projectRoot 'src') (Join-Path $projectRoot 'tests') -g '*.java'
& javac -encoding UTF-8 -source 8 -target 8 -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'A compilacao falhou.' }

& java -ea -cp $classes tests.AutomozDomainTests
if ($LASTEXITCODE -ne 0) { throw 'Os testes de dominio falharam.' }

& java -ea -cp "$classes;$driver" tests.AutomozDatabaseIntegrationTests
if ($LASTEXITCODE -ne 0) { throw 'Os testes de integracao falharam.' }
