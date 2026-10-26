import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/** Guarda a conta em UTF-8 e impede duas instâncias de alterarem o mesmo arquivo. */
final class ArquivoConta implements AutoCloseable {
    private final Path caminho;
    private final FileChannel canal;
    private final FileLock bloqueio;

    ArquivoConta(Path caminho) throws IOException {
        this.caminho = caminho.toAbsolutePath().normalize();
        Files.createDirectories(this.caminho.getParent());
        canal = FileChannel.open(this.caminho.resolveSibling(this.caminho.getFileName() + ".lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock adquirido;
        try {
            adquirido = canal.tryLock();
            if (adquirido == null) {
                throw new IOException("O arquivo da conta já está aberto em outro caixa.");
            }
        } catch (IOException | OverlappingFileLockException erro) {
            canal.close();
            throw new IOException("Não foi possível abrir a conta. Verifique se outro caixa está aberto.", erro);
        }
        bloqueio = adquirido;
    }

    Estado carregar() throws IOException {
        if (Files.notExists(caminho)) {
            return null;
        }
        Properties dados = new Properties();
        try (Reader leitor = Files.newBufferedReader(caminho, StandardCharsets.UTF_8)) {
            dados.load(leitor);
            if (!"1".equals(dados.getProperty("versao"))) {
                throw new IllegalArgumentException("Versão de arquivo desconhecida.");
            }
            BigDecimal inicial = dinheiro(dados, "saldoInicial");
            BigDecimal saldo = dinheiro(dados, "saldo");
            BigDecimal depositado = dinheiro(dados, "totalDepositado");
            BigDecimal sacado = dinheiro(dados, "totalSacado");
            int depositos = quantidade(dados, "quantidadeDepositos");
            int saques = quantidade(dados, "quantidadeSaques");
            int quantidade = quantidade(dados, "movimentacoes");
            if (!Double.isFinite(inicial.doubleValue()) || !Double.isFinite(saldo.doubleValue())
                    || (long) depositos + saques != quantidade
                    || inicial.add(depositado).subtract(sacado).compareTo(saldo) != 0
                    || (depositos == 0) != (depositado.signum() == 0)
                    || (saques == 0) != (sacado.signum() == 0)
                    || quantidade > dados.size()) {
                throw new IllegalArgumentException("Saldo ou totais inconsistentes.");
            }
            List<String> historico = new ArrayList<>();
            for (int i = 0; i < quantidade; i++) {
                String registro = dados.getProperty("movimentacao." + i);
                if (registro == null || registro.trim().isEmpty()
                        || registro.contains("\n") || registro.contains("\r")) {
                    throw new IllegalArgumentException("Histórico incompleto ou inválido.");
                }
                historico.add(registro);
            }
            return new Estado(inicial, saldo, depositado, sacado, depositos, saques, historico);
        } catch (IllegalArgumentException erro) {
            throw new IOException("Arquivo da conta inválido. O conteúdo original foi preservado.", erro);
        }
    }

    void salvar(Estado estado) throws IOException {
        Properties dados = new Properties();
        dados.setProperty("versao", "1");
        dados.setProperty("saldoInicial", estado.inicial.toPlainString());
        dados.setProperty("saldo", estado.saldo.toPlainString());
        dados.setProperty("totalDepositado", estado.depositado.toPlainString());
        dados.setProperty("totalSacado", estado.sacado.toPlainString());
        dados.setProperty("quantidadeDepositos", Integer.toString(estado.depositos));
        dados.setProperty("quantidadeSaques", Integer.toString(estado.saques));
        dados.setProperty("movimentacoes", Integer.toString(estado.historico.size()));
        for (int i = 0; i < estado.historico.size(); i++) {
            dados.setProperty("movimentacao." + i, estado.historico.get(i));
        }
        Path temporario = Files.createTempFile(caminho.getParent(), "conta-", ".tmp");
        try {
            try (Writer escritor = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8)) {
                dados.store(escritor, "Caixa eletronico - dados da conta");
            }
            // Sem troca atômica disponível, falha sem substituir o arquivo anterior.
            Files.move(temporario, caminho, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    private static BigDecimal dinheiro(Properties dados, String chave) {
        String texto = dados.getProperty(chave);
        if (texto == null) {
            throw new IllegalArgumentException("Campo ausente: " + chave);
        }
        BigDecimal valor = new BigDecimal(texto);
        if (valor.signum() < 0 || valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Valor inválido: " + chave);
        }
        return valor;
    }

    private static int quantidade(Properties dados, String chave) {
        int valor = Integer.parseInt(dados.getProperty(chave));
        if (valor < 0) {
            throw new IllegalArgumentException("Quantidade negativa: " + chave);
        }
        return valor;
    }

    @Override
    public void close() throws IOException {
        try {
            bloqueio.release();
        } finally {
            canal.close();
        }
    }

    static final class Estado {
        final BigDecimal inicial;
        final BigDecimal saldo;
        final BigDecimal depositado;
        final BigDecimal sacado;
        final int depositos;
        final int saques;
        final List<String> historico;

        Estado(BigDecimal inicial, BigDecimal saldo, BigDecimal depositado, BigDecimal sacado,
                int depositos, int saques, List<String> historico) {
            this.inicial = inicial;
            this.saldo = saldo;
            this.depositado = depositado;
            this.sacado = sacado;
            this.depositos = depositos;
            this.saques = saques;
            this.historico = Collections.unmodifiableList(new ArrayList<>(historico));
        }
    }
}
