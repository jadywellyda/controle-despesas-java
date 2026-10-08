package br.dev.jady.despesas;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

final class RepositorioLancamentos {
    private final Path arquivo;
    RepositorioLancamentos(Path arquivo) { this.arquivo = arquivo.toAbsolutePath(); }

    List<Lancamento> carregar() throws IOException {
        if (!Files.exists(arquivo)) return List.of();
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        if (linhas.isEmpty() || !linhas.getFirst().equals("DESPESAS_V1")) {
            throw new IOException("Arquivo de lançamentos inválido ou de versão desconhecida.");
        }
        List<Lancamento> lancamentos = new ArrayList<>();
        HashSet<UUID> ids = new HashSet<>();
        for (int i = 1; i < linhas.size(); i++) {
            try {
                String[] c = linhas.get(i).split("\t", -1);
                if (c.length != 6) throw new IllegalArgumentException("Quantidade de campos inválida.");
                UUID id = UUID.fromString(c[0]);
                if (!ids.add(id)) throw new IllegalArgumentException("Identificador duplicado.");
                lancamentos.add(new Lancamento(id, Lancamento.Tipo.valueOf(c[1]), decodificar(c[2]),
                        decodificar(c[3]), new BigDecimal(c[4]), LocalDate.parse(c[5])));
            } catch (RuntimeException erro) {
                throw new IOException("Registro inválido na linha " + (i + 1) + ". Arquivo preservado.", erro);
            }
        }
        return List.copyOf(lancamentos);
    }

    void salvar(List<Lancamento> lancamentos) throws IOException {
        Files.createDirectories(arquivo.getParent());
        Path temporario = Files.createTempFile(arquivo.getParent(), ".despesas-", ".tmp");
        try {
            List<String> linhas = new ArrayList<>();
            linhas.add("DESPESAS_V1");
            for (Lancamento l : lancamentos) {
                linhas.add(l.id() + "\t" + l.tipo() + "\t" + codificar(l.descricao()) + "\t"
                        + codificar(l.categoria()) + "\t" + l.valor().toPlainString() + "\t" + l.data());
            }
            Files.write(temporario, linhas, StandardCharsets.UTF_8);
            try {
                Files.move(temporario, arquivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException erro) {
                Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporario); }
    }

    private static String codificar(String valor) {
        return Base64.getEncoder().encodeToString(valor.getBytes(StandardCharsets.UTF_8));
    }
    private static String decodificar(String valor) {
        return new String(Base64.getDecoder().decode(valor), StandardCharsets.UTF_8);
    }
}
