# Controle de Despesas Java

Aplicação de console para registrar receitas e despesas, consultar lançamentos por mês e acompanhar totais por categoria. Demonstra Java 21, objetos imutáveis, coleções, datas, cálculos decimais e persistência local.

## Funcionalidades

- Registrar, editar e excluir receitas e despesas.
- Listar lançamentos de um mês em ordem de data.
- Calcular receitas, despesas e saldo mensal.
- Agrupar despesas por categoria.
- Salvar dados em arquivo e recuperar os registros na próxima execução.

## Regras de negócio

Os valores devem ser positivos, com precisão de centavos e limite de R$ 1.000.000.000,00 por lançamento. O tipo define se o valor é uma receita ou uma despesa. O saldo pode ser negativo quando as despesas superam as receitas.

Os cálculos usam `BigDecimal`, sem conversão para `double` e sem arredondar centavos de valores inválidos. Por exemplo, `19,90 + 0,10 = 20,00`. A listagem e o resumo consideram apenas o mês e o ano solicitados; categorias são agrupadas pelo texto exato informado.

## Requisitos

- JDK 21 ou superior, com `java` e `javac` disponíveis no terminal.
- PowerShell no Windows ou um shell compatível com `sh` no Linux/macOS.

Não usa Maven, banco de dados nem bibliotecas externas.

## Executar

Baixe o repositório e abra um terminal na pasta do projeto.

**Windows / PowerShell:**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1
```

**Linux / macOS:**

```sh
sh run.sh
```

O script compila o código e abre o menu. O programa começa sem lançamentos; os registros ficam em `dados/lancamentos.tsv`, pasta ignorada pelo Git.

Para escolher outro arquivo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 -Arquivo dados/exemplo.tsv
```

```sh
sh run.sh executar dados/exemplo.tsv
```

## Exemplo de uso

1. Escolha **1 Registrar** e **1 receita**. Informe `Bolsa fictícia`, categoria `Renda`, valor `1000` e data `08/10/2026`.
2. Registre duas despesas na categoria `Estudos`, com valores `19,90` e `0,10` na mesma data.
3. Escolha **3 Resumo mensal** e informe `2026-10`.

Resultado esperado:

```text
Receitas: R$ 1.000,00
Despesas: R$ 20,00
Saldo: R$ 980,00
Despesas por categoria:
  Estudos: R$ 20,00
```

Use datas em `dd/MM/aaaa`, meses em `aaaa-MM` e valores sem separador de milhar, com vírgula ou ponto decimal. A edição e a exclusão usam o UUID exibido na listagem mensal. Use um terminal configurado para UTF-8.

## Testes

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 -Acao testar
```

```sh
sh run.sh testar
```

São **22 verificações automáticas** de valores inválidos, somas exatas, saldo, agrupamento, separação entre meses, edição, exclusão, persistência, corrupção do arquivo e falha de gravação. Os testes usam arquivos temporários, sem alterar os dados de uso.

**Validação realizada:** compilação em JDK 21 com `-Xlint:all -Werror`, execução das verificações e teste dos menus reais no Windows, incluindo valores com vírgula, acentos, data impossível e reinício com persistência. O script para Linux/macOS é fornecido, mas não foi executado neste ambiente.

## Organização do código

```text
src/main/java/br/dev/jady/despesas/
  Lancamento.java                 Modelo, tipo e validações
  ControleDespesas.java           Operações e resumo mensal
  RepositorioLancamentos.java     Leitura e gravação do arquivo
  Main.java                       Menu e entrada de dados
src/test/java/br/dev/jady/despesas/
  ControleDespesasTest.java        Verificações sem dependências
run.ps1                           Compilação e execução no Windows
run.sh                            Compilação e execução no Linux/macOS
```

O arquivo possui cabeçalho de versão, campos separados por tabulação e textos codificados em Base64; essa codificação não é criptografia. Valores são armazenados em representação decimal. A gravação usa arquivo temporário e tenta uma substituição atômica; há alternativa para sistemas de arquivos que não a suportam. Uma falha de gravação não confirma a alteração no estado em memória.

Um arquivo inválido ou com identificadores duplicados impede a inicialização e não é sobrescrito automaticamente.

## Limitações e contexto

Projeto de estudo desenvolvido com assistência de IA e exemplos fictícios. Não conecta contas bancárias, realiza pagamentos, fornece recomendações financeiras ou emite documentos fiscais. É uma aplicação local de console, para uma instância de cada vez, sem autenticação, sincronização ou controle de alterações simultâneas. Use dados fictícios; o arquivo local não é criptografado.

## Referências técnicas

- [API Java 21 — BigDecimal](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html)
- [API Java 21 — YearMonth](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/YearMonth.html)
- [API Java 21 — Files](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html)
