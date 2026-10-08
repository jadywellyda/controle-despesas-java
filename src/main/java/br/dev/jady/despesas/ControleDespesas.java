package br.dev.jady.despesas;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/** Regras e relatórios independentes da interface de console. */
public final class ControleDespesas {
    private final RepositorioLancamentos repositorio;
    private List<Lancamento> lancamentos;

    public record ResumoMensal(BigDecimal receitas, BigDecimal despesas, BigDecimal saldo,
                               Map<String, BigDecimal> despesasPorCategoria) {
        public ResumoMensal {
            despesasPorCategoria = Collections.unmodifiableMap(new TreeMap<>(despesasPorCategoria));
        }
    }

    public ControleDespesas(Path arquivo) throws IOException {
        repositorio = new RepositorioLancamentos(Objects.requireNonNull(arquivo));
        lancamentos = repositorio.carregar();
    }

    public List<Lancamento> listarMes(YearMonth mes) {
        Objects.requireNonNull(mes, "Mês obrigatório.");
        return lancamentos.stream().filter(l -> YearMonth.from(l.data()).equals(mes))
                .sorted(Comparator.comparing(Lancamento::data).thenComparing(Lancamento::descricao).thenComparing(Lancamento::id)).toList();
    }

    public Lancamento registrar(Lancamento.Tipo tipo, String descricao, String categoria,
                                 BigDecimal valor, LocalDate data) throws IOException {
        Lancamento novo = new Lancamento(UUID.randomUUID(), tipo, descricao, categoria, valor, data);
        List<Lancamento> proxima = new ArrayList<>(lancamentos);
        proxima.add(novo);
        confirmar(proxima);
        return novo;
    }

    public void editar(UUID id, Lancamento.Tipo tipo, String descricao, String categoria,
                       BigDecimal valor, LocalDate data) throws IOException {
        encontrar(id);
        Lancamento novo = new Lancamento(id, tipo, descricao, categoria, valor, data);
        List<Lancamento> proxima = new ArrayList<>(lancamentos);
        proxima.replaceAll(l -> l.id().equals(id) ? novo : l);
        confirmar(proxima);
    }

    public void excluir(UUID id) throws IOException {
        Lancamento atual = encontrar(id);
        List<Lancamento> proxima = new ArrayList<>(lancamentos);
        proxima.remove(atual);
        confirmar(proxima);
    }

    public ResumoMensal resumo(YearMonth mes) {
        BigDecimal receitas = new BigDecimal("0.00"), despesas = new BigDecimal("0.00");
        Map<String, BigDecimal> porCategoria = new TreeMap<>();
        for (Lancamento l : listarMes(mes)) {
            if (l.tipo() == Lancamento.Tipo.RECEITA) receitas = receitas.add(l.valor());
            else {
                despesas = despesas.add(l.valor());
                porCategoria.merge(l.categoria(), l.valor(), BigDecimal::add);
            }
        }
        return new ResumoMensal(receitas, despesas, receitas.subtract(despesas), porCategoria);
    }

    private Lancamento encontrar(UUID id) {
        return lancamentos.stream().filter(l -> l.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado."));
    }

    private void confirmar(List<Lancamento> proxima) throws IOException {
        repositorio.salvar(proxima);
        lancamentos = List.copyOf(proxima);
    }
}
