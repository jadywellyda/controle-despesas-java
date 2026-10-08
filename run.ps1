param(
    [ValidateSet('executar','testar','compilar')][string]$Acao = 'executar',
    [string]$Arquivo = ''
)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force -Path 'build/main' | Out-Null
    $taskMainSources = @(Get-ChildItem -LiteralPath 'src/main/java' -Recurse -Filter '*.java' | ForEach-Object FullName)
    & javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d 'build/main' @taskMainSources
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação.' }
    if ($Acao -eq 'testar') {
        New-Item -ItemType Directory -Force -Path 'build/test' | Out-Null
        $taskTestSources = @(Get-ChildItem -LiteralPath 'src/test/java' -Recurse -Filter '*.java' | ForEach-Object FullName)
        & javac --release 21 -encoding UTF-8 -Xlint:all -Werror -cp 'build/main' -d 'build/test' @taskTestSources
        if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação dos testes.' }
        & java '-Dfile.encoding=UTF-8' -cp 'build/main;build/test' br.dev.jady.despesas.ControleDespesasTest
        if ($LASTEXITCODE -ne 0) { throw 'Os testes falharam.' }
    } elseif ($Acao -eq 'executar') {
        if ($Arquivo) { & java '-Dfile.encoding=UTF-8' -cp 'build/main' br.dev.jady.despesas.Main $Arquivo }
        else { & java '-Dfile.encoding=UTF-8' -cp 'build/main' br.dev.jady.despesas.Main }
        if ($LASTEXITCODE -ne 0) { throw 'A aplicação terminou com erro.' }
    }
} finally { Pop-Location }
