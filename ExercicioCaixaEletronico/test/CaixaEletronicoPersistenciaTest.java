import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class CaixaEletronicoPersistenciaTest {
    private static int testes;

    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("caixa-persistencia-");
        PrintStream original = System.out;
        try (PrintStream captura = new PrintStream(new ByteArrayOutputStream())) {
            System.setOut(captura);
            testarReabertura(pasta.resolve("conta.properties"));
            testarCancelamentos(pasta.resolve("cancelamentos.properties"));
            testarFalhaDeGravacao(pasta.resolve("falha.properties"));
            testarBloqueio(pasta.resolve("bloqueio.properties"));
            testarInvalidos(pasta);
            testarProgramaCompleto(pasta);
        } finally {
            System.setOut(original);
        }
        System.out.println("OK: " + testes + " cenários de persistência, recuperação e falhas.");
    }

    private static void testarReabertura(Path caminho) throws Exception {
        List<String> historico;
        try (CaixaEletronico caixa = new CaixaEletronico(caminho)) {
            valor("50000.00", caixa.saldoFinal);
            verificar(caixa.movimentacoes.isEmpty(), "Primeira conta deve começar sem histórico");
            entrada(caixa, "0,10 S 0,20 SIM 0,30 S");
            caixa.deposito();
            verificar(Files.exists(caminho), "Depósito deve salvar antes de encerrar");
            caixa.deposito();
            caixa.sacar();
            valor("50000.00", caixa.saldoFinal);
            historico = caixa.movimentacoes;
        }
        try (CaixaEletronico caixa = new CaixaEletronico(caminho)) {
            valor("50000.00", caixa.saldoFinal);
            valor("0.30", caixa.totalDepositado);
            valor("0.30", caixa.totalSacado);
            verificar(caixa.quantidadeDepositos == 2 && caixa.quantidadeSaques == 1, "Contagens devem persistir");
            verificar(caixa.movimentacoes.equals(historico), "Histórico com acentos e datas deve ser preservado");
            entrada(caixa, "10 S");
            caixa.deposito();
            verificar(caixa.movimentacoes.size() == 4, "Nova operação deve continuar o histórico");
        }
        try (CaixaEletronico caixa = new CaixaEletronico(caminho)) {
            valor("50010.00", caixa.saldoFinal);
            valor("10.30", caixa.totalDepositado);
            verificar(caixa.movimentacoes.size() == 4, "Nova operação deve persistir");
            entrada(caixa, "50010 S");
            caixa.sacar();
        }
        try (CaixaEletronico caixa = new CaixaEletronico(caminho)) {
            valor("0", caixa.saldoFinal);
            verificar(caixa.movimentacoes.size() == 5, "Saldo zerado deve reabrir sem reinicializar");
        }
        testes += 4;
    }

    private static void testarCancelamentos(Path caminho) throws Exception {
        try (CaixaEletronico caixa = new CaixaEletronico(caminho)) {
            entrada(caixa, "10 N");
            caixa.deposito();
            verificar(!Files.exists(caminho), "Cancelamento inicial não deve criar arquivo");
            entrada(caixa, "1 S");
            caixa.deposito();
            byte[] antes = Files.readAllBytes(caminho);
            entrada(caixa, "-1 0,001 0 10 N 50002 0 10");
            caixa.deposito();
            caixa.sacar();
            caixa.sacar();
            caixa.deposito();
            valor("50001", caixa.saldoFinal);
            verificar(Arrays.equals(antes, Files.readAllBytes(caminho)), "Cancelamentos não devem regravar dados");
        }
        testes++;
    }

    private static void testarFalhaDeGravacao(Path caminho) throws Exception {
        try (CaixaEletronico caixa = new CaixaEletronico(caminho)) {
            entrada(caixa, "1 S");
            caixa.deposito();
            byte[] antes = Files.readAllBytes(caminho);
            Path copia = caminho.resolveSibling("estado-anterior.properties");
            Files.move(caminho, copia);
            Files.createDirectory(caminho);
            Files.write(caminho.resolve("obstaculo"), new byte[] {1});
            entrada(caixa, "10 S 10 S");
            caixa.deposito();
            caixa.sacar();
            valor("50001", caixa.saldoFinal);
            valor("1", caixa.totalDepositado);
            valor("0", caixa.totalSacado);
            verificar(caixa.quantidadeDepositos == 1 && caixa.quantidadeSaques == 0, "Falha não pode contar operação");
            verificar(caixa.movimentacoes.size() == 1, "Falha não pode registrar operação");
            verificar(Arrays.equals(antes, Files.readAllBytes(copia)), "Dados anteriores devem permanecer íntegros");
            try (java.util.stream.Stream<Path> arquivos = Files.list(caminho.getParent())) {
                verificar(arquivos.noneMatch(p -> p.toString().endsWith(".tmp")), "Não deixar gravação temporária");
            }
        }
        testes++;
    }

    private static void testarBloqueio(Path caminho) throws Exception {
        try (CaixaEletronico primeiro = new CaixaEletronico(caminho)) {
            valor("50000", primeiro.saldoFinal);
            boolean bloqueado = false;
            try (CaixaEletronico segundo = new CaixaEletronico(caminho)) {
                throw new AssertionError("Não pode abrir duas vezes: " + segundo.saldoFinal);
            } catch (IOException esperado) {
                bloqueado = true;
            }
            verificar(bloqueado, "Segundo caixa deve ser bloqueado");
        }
        try (CaixaEletronico reaberto = new CaixaEletronico(caminho)) {
            valor("50000", reaberto.saldoFinal);
        }
        testes++;
    }

    private static void testarInvalidos(Path pasta) throws Exception {
        String base = "versao=1\nsaldoInicial=50000.00\nsaldo=50000.00\n"
                + "totalDepositado=0\ntotalSacado=0\nquantidadeDepositos=0\nquantidadeSaques=0\nmovimentacoes=0\n";
        String[] invalidos = {
            "", "arquivo quebrado", base.replace("versao=1", "versao=2"),
            base.replace("saldo=50000.00", "saldo=-1"),
            base.replace("saldo=50000.00", "saldo=50001.00"),
            base.replace("saldo=50000.00", "saldo=1.001"),
            base.replace("saldo=50000.00", "saldo=NaN"),
            base.replace("quantidadeDepositos=0", "quantidadeDepositos=-1"),
            base.replace("movimentacoes=0", "movimentacoes=1"),
            base.replace("saldo=50000.00\n", ""),
            base.replace("saldo=50000.00", "saldo=50001.00").replace("totalDepositado=0", "totalDepositado=1")
                .replace("quantidadeDepositos=0", "quantidadeDepositos=1").replace("movimentacoes=0", "movimentacoes=1")
        };
        for (int i = 0; i < invalidos.length; i++) {
            Path caminho = pasta.resolve("invalido-" + i + ".properties");
            byte[] original = invalidos[i].getBytes(StandardCharsets.UTF_8);
            Files.write(caminho, original);
            for (int tentativa = 0; tentativa < 2; tentativa++) {
                try (CaixaEletronico caixa = new CaixaEletronico(caminho)) {
                    throw new AssertionError("Arquivo inválido foi aceito: " + i + ", saldo=" + caixa.saldoFinal);
                } catch (IOException esperado) {
                    verificar(esperado.getMessage().contains("inválido"), "Deve rejeitar conteúdo, sem manter bloqueio");
                }
            }
            verificar(Arrays.equals(original, Files.readAllBytes(caminho)), "Não sobrescrever arquivo inválido");
            testes++;
        }
    }

    private static void testarProgramaCompleto(Path pasta) throws Exception {
        Path caminho = pasta.resolve("programa.properties");
        String identificacao = "1\n";
        executarPrograma(caminho, identificacao + "3 1050 1 0 1\n", 0);
        executarPrograma(caminho, identificacao + "2 525 1 4 8 0 1\n", 0);
        try (CaixaEletronico caixa = new CaixaEletronico(caminho)) {
            valor("50005.25", caixa.saldoFinal);
            verificar(caixa.movimentacoes.size() == 2, "Execuções separadas devem compartilhar a conta");
        }
        Path invalido = pasta.resolve("programa-invalido.properties");
        Files.write(invalido, "corrompido".getBytes(StandardCharsets.UTF_8));
        executarPrograma(invalido, "", 1);
        verificar(new String(Files.readAllBytes(invalido), StandardCharsets.UTF_8).equals("corrompido"),
                "Programa não deve substituir arquivo inválido");
        testes += 2;
    }

    private static void executarPrograma(Path caminho, String entrada, int codigoEsperado) throws Exception {
        String executavelJava = java.nio.file.Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        Process processo = new ProcessBuilder(executavelJava, "-Dfile.encoding=UTF-8", "-Dcaixa.arquivo=" + caminho,
                "-cp", System.getProperty("java.class.path"), "CaixaEletronico", "terminal")
                .redirectErrorStream(true).redirectOutput(caminho.resolveSibling(caminho.getFileName() + ".log").toFile())
                .start();
        try (java.io.OutputStream entradaProcesso = processo.getOutputStream()) {
            entradaProcesso.write(entrada.getBytes(StandardCharsets.UTF_8));
        }
        if (!processo.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)) {
            processo.destroyForcibly();
            throw new AssertionError("Programa não encerrou no tempo esperado");
        }
        verificar(processo.exitValue() == codigoEsperado, "Código de saída inesperado: " + processo.exitValue());
    }
    private static void entrada(CaixaEletronico caixa, String texto) {
        caixa.scanner = new Scanner(texto).useLocale(Locale.forLanguageTag("pt-BR"));
    }

    private static void valor(String esperado, BigDecimal atual) {
        verificar(atual.compareTo(new BigDecimal(esperado)) == 0, "Esperado " + esperado + ", recebido " + atual);
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }
}
