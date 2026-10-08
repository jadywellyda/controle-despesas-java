package br.dev.jady.despesas;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

public final class ControleDespesasTest {
    private static int verificacoes;
    @FunctionalInterface interface Acao { void executar() throws Exception; }
    private static BigDecimal valor(String texto) { return new BigDecimal(texto); }

    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("despesas-test-");
        try {
            Path arquivo = pasta.resolve("lancamentos.tsv");
            ControleDespesas c = new ControleDespesas(arquivo);
            YearMonth outubro = YearMonth.of(2026, 10);
            LocalDate data = LocalDate.of(2026, 10, 8);
            checar(c.resumo(outubro).saldo().equals(valor("0.00")), "Mês vazio");
            c.registrar(Lancamento.Tipo.RECEITA, "Bolsa fictícia", "Renda", valor("1000"), data);
            Lancamento gasto = c.registrar(Lancamento.Tipo.DESPESA, "  Livro  ", "Estudos", valor("19.90"), data);
            c.registrar(Lancamento.Tipo.DESPESA, "Caderno", "Estudos", valor("0.10"), data);
            c.registrar(Lancamento.Tipo.DESPESA, "Outra compra", "Estudos", valor("500"), data.plusMonths(1));
            var resumo = c.resumo(outubro);
            checar(resumo.receitas().equals(valor("1000.00")), "Receita com duas casas");
            checar(resumo.despesas().equals(valor("20.00")), "Soma exata de centavos");
            checar(resumo.saldo().equals(valor("980.00")), "Saldo correto");
            checar(resumo.despesasPorCategoria().get("Estudos").equals(valor("20.00")), "Agrupamento de despesas");
            checar(c.listarMes(outubro).size() == 3, "Isolamento entre meses");
            checar(gasto.descricao().equals("Livro"), "Normalização de texto");
            falha(UnsupportedOperationException.class, () -> resumo.despesasPorCategoria().put("Alteração", valor("1")));
            falha(IllegalArgumentException.class, () -> c.registrar(Lancamento.Tipo.DESPESA, "Compra", "Categoria", valor("0"), data));
            falha(IllegalArgumentException.class, () -> c.registrar(Lancamento.Tipo.DESPESA, "Compra", "Categoria", valor("-1"), data));
            falha(IllegalArgumentException.class, () -> c.registrar(Lancamento.Tipo.DESPESA, "Compra", "Categoria", valor("1.001"), data));
            falha(IllegalArgumentException.class, () -> c.registrar(Lancamento.Tipo.DESPESA, "Compra", " ", valor("1"), data));
            falha(IllegalArgumentException.class, () -> c.registrar(Lancamento.Tipo.DESPESA, "Compra", "Categoria", valor("1000000000.01"), data));
            checar(c.listarMes(outubro).size() == 3, "Entradas inválidas não adicionadas");
            checar(new ControleDespesas(arquivo).resumo(outubro).saldo().equals(valor("980.00")), "Persistência de valores e acentos");
            c.editar(gasto.id(), Lancamento.Tipo.DESPESA, "Livro revisado", "Estudos", valor("29.90"), data);
            checar(c.resumo(outubro).despesas().equals(valor("30.00")), "Edição recalcula relatório");
            c.excluir(gasto.id());
            checar(new ControleDespesas(arquivo).resumo(outubro).despesas().equals(valor("0.10")), "Exclusão persistida");
            falha(IllegalArgumentException.class, () -> c.excluir(UUID.randomUUID()));

            // Identificadores duplicados também são corrupção, não um registro a ignorar.
            var linhas = Files.readAllLines(arquivo);
            linhas.add(linhas.get(1));
            Files.write(arquivo, linhas);
            String original = Files.readString(arquivo);
            falha(IOException.class, () -> new ControleDespesas(arquivo));
            checar(Files.readString(arquivo).equals(original), "Arquivo inválido preservado");

            Path bloqueio = pasta.resolve("bloqueio");
            ControleDespesas semGravacao = new ControleDespesas(bloqueio.resolve("lancamentos.tsv"));
            Files.writeString(bloqueio, "Este caminho é um arquivo.");
            falha(IOException.class, () -> semGravacao.registrar(Lancamento.Tipo.RECEITA, "Não salvo", "Exemplo", valor("10"), data));
            checar(semGravacao.listarMes(outubro).isEmpty(), "Falha de gravação preserva estado em memória");
            System.out.println("ControleDespesasTest: " + verificacoes + " verificações passaram.");
        } finally {
            try (var caminhos = Files.walk(pasta)) {
                for (Path p : caminhos.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
    }
    private static void checar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
        verificacoes++;
    }
    private static void falha(Class<? extends Throwable> esperado, Acao acao) throws Exception {
        try { acao.executar(); } catch (Exception erro) {
            if (!esperado.isInstance(erro)) throw erro;
            verificacoes++; return;
        }
        throw new AssertionError("Esperada exceção: " + esperado.getSimpleName());
    }
}
