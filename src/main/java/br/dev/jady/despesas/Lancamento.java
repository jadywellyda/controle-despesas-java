package br.dev.jady.despesas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Lancamento(UUID id, Tipo tipo, String descricao, String categoria, BigDecimal valor, LocalDate data) {
    public enum Tipo { RECEITA, DESPESA }

    public Lancamento {
        Objects.requireNonNull(id, "Identificador obrigatório.");
        Objects.requireNonNull(tipo, "Tipo obrigatório.");
        Objects.requireNonNull(data, "Data obrigatória.");
        descricao = texto(descricao, "Descrição", 120);
        categoria = texto(categoria, "Categoria", 60);
        if (valor == null || valor.signum() <= 0) throw new IllegalArgumentException("Valor deve ser positivo.");
        try { valor = valor.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException erro) { throw new IllegalArgumentException("Valor não pode exigir arredondamento de centavos.", erro); }
        if (valor.compareTo(new BigDecimal("1000000000.00")) > 0) {
            throw new IllegalArgumentException("Limite por lançamento: R$ 1.000.000.000,00.");
        }
    }

    private static String texto(String valor, String campo, int limite) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(campo + " obrigatória.");
        String limpo = valor.strip();
        if (limpo.length() > limite || limpo.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(campo + " deve ter até " + limite + " caracteres, sem controles.");
        }
        return limpo;
    }
}
