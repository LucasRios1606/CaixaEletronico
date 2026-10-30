import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CaixaInterfacesTest {
    private static int verificacoes;
    private static final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public static void main(String[] args) throws Exception {
        Path arquivo = Files.createTempDirectory("caixa-interfaces-").resolve("conta.properties");
        try (CaixaEletronico caixa = new CaixaEletronico(arquivo)) {
            String terminal = terminal(caixa, "1 3 1050 1 2 525 0 0 1", true);
            verifica(terminal.contains("Saque cancelado"), "cancelamento numérico");
            verifica(caixa.saldoFinal.compareTo(new BigDecimal("50010.50")) == 0, "depósito com centavos só numéricos");
            verifica(caixa.movimentacoes.size() == 1, "cancelamento não altera histórico");
            verifica(!terminal.contains("Informe o seu nome"), "acesso não pede letras");
            verifica(!terminal.contains("Digite S"), "confirmação não pede letras");
            terminal(caixa, "1 3 10,50 0 0 1", true);
            verifica(caixa.movimentacoes.size() == 1, "modo numérico recusa separadores");
            try (CaixaWeb web = new CaixaWeb(caixa, 0)) {
                web.iniciar();
                String base = "http://127.0.0.1:" + web.porta();
                HttpResponse<String> resumo = enviar(base, "/api/conta", null, null, null);
                verifica(resumo.statusCode() == 200 && resumo.body().contains("50010.50"), "web enxerga depósito do terminal");
                Matcher token = Pattern.compile("\"csrfToken\":\"([^\"]+)\"").matcher(resumo.body());
                verifica(token.find(), "token presente");
                String csrf = token.group(1);
                verifica(enviar(base, "/api/depositar", "valor=1&pedidoId=a", null, base).statusCode() == 403, "sem token bloqueado");
                verifica(enviar(base, "/api/depositar", "valor=1&pedidoId=a", csrf, "http://example.invalid").statusCode() == 403, "outra origem bloqueada");
                verifica(enviar(base, "/api/sacar", "valor=5.25&pedidoId=saque-web", csrf, base).statusCode() == 200, "saque web");
                verifica(terminal(caixa, "1 0 1", false).contains("50.005,25"), "terminal enxerga saque web");
                verifica(enviar(base, "/api/sacar", "valor=5.25&pedidoId=saque-web", csrf, base).statusCode() == 200, "repetição aceita sem duplicar");
                verifica(caixa.movimentacoes.size() == 2, "pedido repetido não debita novamente");
                verifica(enviar(base, "/api/sacar", "valor=6&pedidoId=saque-web", csrf, base).statusCode() == 400, "pedido conflitante recusado");
                for (String valor : new String[] {"0", "-1", "1.001", "NaN", "999999"}) {
                    verifica(enviar(base, "/api/sacar", "valor=" + valor + "&pedidoId=invalido", csrf, base).statusCode() == 400, "saque inválido " + valor);
                }
                verifica(enviar(base, "/api/sacar", "valor=1&valor=2&pedidoId=x", csrf, base).statusCode() == 400, "campos duplicados recusados");
                verifica(enviar(base, "/api/sacar", null, null, null).statusCode() == 405, "GET não movimenta");
                verifica(enviar(base, "/dados/conta.properties", null, null, null).statusCode() == 404, "dados privados não servidos");
                for (String rota : new String[] {"/", "/style.css", "/app.js", "/favicon.svg"}) {
                    verifica(enviar(base, rota, null, null, null).statusCode() == 200, "recurso " + rota);
                }
                HttpResponse<String> extrato = enviar(base, "/api/extrato", null, null, null);
                verifica(extrato.statusCode() == 200 && extrato.body().contains("Operações confirmadas: 2"), "extrato web com dados das duas interfaces");
                verifica(extrato.headers().firstValue("Content-Disposition").orElse("").contains("attachment"), "download de texto");
                verifica(resumo.headers().firstValue("Cache-Control").orElse("").equals("no-store"), "saldo sem cache");
                // Dois saques concorrentes de 40 mil não podem gastar o mesmo saldo.
                CompletableFuture<Integer> a = CompletableFuture.supplyAsync(() -> sacar(base, csrf, "concorrente-a"));
                CompletableFuture<Integer> b = CompletableFuture.supplyAsync(() -> sacar(base, csrf, "concorrente-b"));
                int primeiro = a.get(), segundo = b.get();
                verifica((primeiro == 200 && segundo == 400) || (primeiro == 400 && segundo == 200), "saldo protegido de concorrência");
                verifica(caixa.saldoFinal.compareTo(new BigDecimal("10005.25")) == 0, "saldo exato após concorrência");
                // Web continua respondendo enquanto o terminal aguarda confirmação.
                terminal(caixa, "3 100 1 10 2 0 0 1", false);
                verifica(enviar(base, "/api/conta", null, null, null).body().contains("10006.25"), "web acompanha nova operação do terminal");
            }
        }
        try (CaixaEletronico reaberto = new CaixaEletronico(arquivo); CaixaWeb web = new CaixaWeb(reaberto, 0)) {
            web.iniciar();
            String resumo = enviar("http://127.0.0.1:" + web.porta(), "/api/conta", null, null, null).body();
            verifica(resumo.contains("10006.25") && resumo.contains("\"totalOperacoes\":4"), "reinício recupera saldo e histórico de ambas as interfaces");
        }
        testarProcessoAmbos();
        System.out.println("OK: " + verificacoes + " verificações de terminal numérico, HTTP e conta compartilhada.");
    }

    private static void testarProcessoAmbos() throws Exception {
        Path pasta = Files.createTempDirectory("caixa-ambos-").toAbsolutePath();
        Path conta = pasta.resolve("conta.properties");
        Path log = pasta.resolve("terminal.log");
        int porta;
        try (java.net.ServerSocket reserva = new java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            porta = reserva.getLocalPort();
        }
        String executavel = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process processo = new ProcessBuilder(executavel, "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8",
                "-Dcaixa.arquivo=" + conta, "-cp", System.getProperty("java.class.path"),
                "CaixaEletronico", "ambos", Integer.toString(porta))
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        String base = "http://127.0.0.1:" + porta;
        try {
            HttpResponse<String> estado = null;
            for (int tentativa = 0; tentativa < 50; tentativa++) {
                try { estado = enviar(base, "/api/conta", null, null, null); break; }
                catch (java.io.IOException erro) {
                    if (!processo.isAlive()) throw new AssertionError("Inicialização falhou: " + Files.readString(log));
                    Thread.sleep(100);
                }
            }
            verifica(estado != null && estado.statusCode() == 200, "modo ambos inicia servidor real");
            processo.getOutputStream().write("1\n3\n100\n1\n".getBytes(StandardCharsets.UTF_8));
            processo.getOutputStream().flush();
            for (int tentativa = 0; tentativa < 50; tentativa++) {
                estado = enviar(base, "/api/conta", null, null, null);
                if (estado.body().contains("\"saldo\":\"50001.00\"")) break;
                Thread.sleep(100);
            }
            verifica(estado.body().contains("\"saldo\":\"50001.00\""), "processo real compartilha terminal com web");
            Matcher token = Pattern.compile("\"csrfToken\":\"([^\"]+)\"").matcher(estado.body());
            verifica(token.find(), "processo real possui token");
            verifica(enviar(base, "/api/depositar", "valor=2&pedidoId=processo", token.group(1), base).statusCode() == 200,
                    "web opera enquanto terminal aguarda entrada");
            processo.getOutputStream().write("1\n0\n1\n".getBytes(StandardCharsets.UTF_8));
            processo.getOutputStream().close();
            verifica(processo.waitFor(10, java.util.concurrent.TimeUnit.SECONDS) && processo.exitValue() == 0,
                    "saída numérica encerra terminal e servidor");
            verifica(Files.readString(log).contains("50.003,00"), "terminal real mostra saldo da web");
            try (CaixaEletronico reaberto = new CaixaEletronico(conta)) {
                verifica(reaberto.saldoFinal.compareTo(new BigDecimal("50003.00")) == 0
                        && reaberto.movimentacoes.size() == 2, "processo real persiste operações das duas interfaces");
            }
        } finally {
            if (processo.isAlive()) processo.destroyForcibly();
        }
    }
    private static int sacar(String base, String csrf, String id) {
        try { return enviar(base, "/api/sacar", "valor=40000&pedidoId=" + id, csrf, base).statusCode(); }
        catch (Exception erro) { throw new RuntimeException(erro); }
    }

    private static String terminal(CaixaEletronico caixa, String entrada, boolean identificar) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream captura = new PrintStream(buffer, true, "UTF-8")) {
            System.setOut(captura);
            caixa.modoNumerico = true;
            caixa.scanner = new Scanner(entrada);
            if (!identificar || caixa.identificadorUsuario()) caixa.menu();
        } finally { System.setOut(original); }
        return buffer.toString("UTF-8");
    }

    private static HttpResponse<String> enviar(String base, String rota, String corpo, String token, String origem) throws Exception {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create(base + rota)).timeout(Duration.ofSeconds(5));
        if (token != null) pedido.header("X-CSRF-Token", token);
        if (origem != null) pedido.header("Origin", origem);
        if (corpo == null) pedido.GET();
        else pedido.header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(corpo));
        return cliente.send(pedido.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void verifica(boolean ok, String mensagem) {
        if (!ok) throw new AssertionError(mensagem);
        verificacoes++;
    }
}