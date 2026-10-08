package br.dev.jady.despesas;

import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Scanner;
import java.util.UUID;

public final class Main {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    private record Entrada(Lancamento.Tipo tipo, String descricao, String categoria, BigDecimal valor, LocalDate data) {}

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        Path arquivo = Path.of(args.length > 0 ? args[0] : "dados/lancamentos.tsv");
        try (Scanner entrada = new Scanner(System.in, StandardCharsets.UTF_8)) {
            ControleDespesas controle = new ControleDespesas(arquivo);
            System.out.println("CONTROLE DE DESPESAS JAVA | Demonstração para estudo");
            System.out.println("Arquivo: " + arquivo.toAbsolutePath());
            while (true) {
                System.out.println("\n1 Registrar  2 Listar mês  3 Resumo mensal  4 Editar  5 Excluir  0 Sair");
                String opcao = ler(entrada, "Opção");
                if (opcao == null || opcao.equals("0")) break;
                try {
                    switch (opcao) {
                        case "1" -> {
                            Entrada e = formulario(entrada);
                            Lancamento novo = controle.registrar(e.tipo(), e.descricao(), e.categoria(), e.valor(), e.data());
                            System.out.println("Registrado. ID: " + novo.id());
                        }
                        case "2" -> {
                            var lista = controle.listarMes(mes(entrada));
                            if (lista.isEmpty()) System.out.println("Nenhum lançamento neste mês.");
                            for (Lancamento l : lista) System.out.println(l.id() + " | " + DATA.format(l.data()) + " | "
                                    + l.tipo() + " | " + l.descricao() + " | " + l.categoria() + " | " + MOEDA.format(l.valor()));
                        }
                        case "3" -> {
                            var resumo = controle.resumo(mes(entrada));
                            System.out.println("Receitas: " + MOEDA.format(resumo.receitas()));
                            System.out.println("Despesas: " + MOEDA.format(resumo.despesas()));
                            System.out.println("Saldo: " + MOEDA.format(resumo.saldo()));
                            System.out.println("Despesas por categoria:");
                            resumo.despesasPorCategoria().forEach((categoria, valor) -> System.out.println("  " + categoria + ": " + MOEDA.format(valor)));
                        }
                        case "4" -> {
                            UUID id = id(entrada);
                            Entrada e = formulario(entrada);
                            controle.editar(id, e.tipo(), e.descricao(), e.categoria(), e.valor(), e.data());
                            System.out.println("Lançamento atualizado.");
                        }
                        case "5" -> {
                            UUID id = id(entrada);
                            if ("sim".equalsIgnoreCase(obrigatorio(entrada, "Digite sim para excluir"))) {
                                controle.excluir(id); System.out.println("Lançamento excluído.");
                            } else System.out.println("Exclusão cancelada.");
                        }
                        default -> System.out.println("Escolha uma opção do menu.");
                    }
                } catch (FimEntrada erro) { break; }
                catch (IOException erro) { System.out.println("Não foi possível salvar: " + erro.getMessage()); }
                catch (RuntimeException erro) { System.out.println("Entrada inválida: " + erro.getMessage()); }
            }
            System.out.println("Até a próxima!");
        } catch (IOException erro) {
            System.err.println("Controle não iniciado: " + erro.getMessage());
            System.exit(1);
        }
    }

    private static Entrada formulario(Scanner entrada) {
        String tipo = obrigatorio(entrada, "Tipo (1 receita, 2 despesa)");
        Lancamento.Tipo t = switch (tipo) {
            case "1" -> Lancamento.Tipo.RECEITA;
            case "2" -> Lancamento.Tipo.DESPESA;
            default -> throw new IllegalArgumentException("Tipo deve ser 1 ou 2.");
        };
        String descricao = obrigatorio(entrada, "Descrição");
        String categoria = obrigatorio(entrada, "Categoria");
        String valor = obrigatorio(entrada, "Valor em reais (ex.: 19,90, sem separador de milhar)");
        if (!valor.matches("\\d{1,10}([.,]\\d{1,2})?")) throw new IllegalArgumentException("Use um valor com até duas casas decimais.");
        LocalDate data = LocalDate.parse(obrigatorio(entrada, "Data (dd/MM/aaaa)"), DATA);
        return new Entrada(t, descricao, categoria, new BigDecimal(valor.replace(',', '.')), data);
    }
    private static String ler(Scanner entrada, String mensagem) {
        System.out.print(mensagem + ": ");
        return entrada.hasNextLine() ? entrada.nextLine().strip() : null;
    }
    private static String obrigatorio(Scanner entrada, String mensagem) {
        String valor = ler(entrada, mensagem);
        if (valor == null) throw new FimEntrada();
        return valor;
    }
    private static UUID id(Scanner entrada) { return UUID.fromString(obrigatorio(entrada, "ID do lançamento")); }
    private static YearMonth mes(Scanner entrada) { return YearMonth.parse(obrigatorio(entrada, "Mês (aaaa-MM)")); }
    private static final class FimEntrada extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
