import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Servidor HTTP local, sem dependências externas e sem acesso a bancos reais. */
public final class CaixaWeb implements AutoCloseable {
    private final CaixaEletronico conta;
    private final HttpServer servidor;
    private final ExecutorService tarefas = Executors.newFixedThreadPool(4);
    private final Map<String, String> pedidos = new HashMap<>();
    private final String csrf = UUID.randomUUID().toString();
    private final AtomicBoolean encerrado = new AtomicBoolean();

    public CaixaWeb(CaixaEletronico conta, int porta) throws IOException {
        this.conta = conta;
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), porta), 0);
        servidor.setExecutor(tarefas);
        servidor.createContext("/", this::atender);
    }

    public void iniciar() { servidor.start(); }
    public int porta() { return servidor.getAddress().getPort(); }

    private void atender(HttpExchange requisicao) throws IOException {
        try {
            String host = requisicao.getRequestHeaders().getFirst("Host");
            if (!("localhost:" + porta()).equalsIgnoreCase(host) && !("127.0.0.1:" + porta()).equals(host)) {
                responderJson(requisicao, 403, "{\"erro\":\"Acesso permitido somente pelo endereço local.\"}");
                return;
            }
            String caminho = requisicao.getRequestURI().getPath();
            if ("/api/conta".equals(caminho)) {
                if (!metodo(requisicao, "GET")) return;
                responderJson(requisicao, 200, resumoJson());
            } else if ("/api/extrato".equals(caminho)) {
                if (!metodo(requisicao, "GET")) return;
                requisicao.getResponseHeaders().set("Content-Disposition", "attachment; filename=extrato.txt");
                responder(requisicao, 200, "text/plain; charset=utf-8",
                        (String.join("\n", conta.linhasExtrato()) + "\n").getBytes(StandardCharsets.UTF_8));
            } else if ("/api/sacar".equals(caminho) || "/api/depositar".equals(caminho)) {
                if (!metodo(requisicao, "POST")) return;
                String token = requisicao.getRequestHeaders().getFirst("X-CSRF-Token");
                String origem = requisicao.getRequestHeaders().getFirst("Origin");
                boolean origemValida = origem == null || origem.equals("http://localhost:" + porta())
                    || origem.equals("http://127.0.0.1:" + porta());
                if (!origemValida || token == null || !MessageDigest.isEqual(csrf.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
                    responderJson(requisicao, 403, "{\"erro\":\"Sessão inválida. Atualize a página antes de continuar.\"}");
                    return;
                }
                String tipoConteudo = requisicao.getRequestHeaders().getFirst("Content-Type");
                if (tipoConteudo == null || !tipoConteudo.toLowerCase(Locale.ROOT).startsWith("application/x-www-form-urlencoded")) {
                    responderJson(requisicao, 415, "{\"erro\":\"Formato de envio não suportado.\"}");
                    return;
                }
                byte[] corpo = requisicao.getRequestBody().readNBytes(2049);
                if (corpo.length > 2048) {
                    responderJson(requisicao, 413, "{\"erro\":\"Requisição muito grande.\"}");
                    return;
                }
                Map<String, String> dados = formulario(new String(corpo, StandardCharsets.UTF_8));
                String valorTexto = dados.get("valor");
                String pedidoId = dados.get("pedidoId");
                if (valorTexto == null || !valorTexto.matches("[0-9]+([,.][0-9]{1,2})?")
                        || pedidoId == null || !pedidoId.matches("[A-Za-z0-9-]{1,80}")) {
                    throw new IllegalArgumentException("Valor ou identificador inválido.");
                }
                java.math.BigDecimal valor = new java.math.BigDecimal(valorTexto.replace(',', '.'));
                String assinatura = caminho + ":" + valor.stripTrailingZeros().toPlainString();
                synchronized (conta) {
                    String anterior = pedidos.get(pedidoId);
                    if (anterior != null && !anterior.equals(assinatura)) {
                        throw new IllegalArgumentException("Identificador já usado em outra operação.");
                    }
                    if (anterior == null) {
                        conta.movimentar(caminho.equals("/api/depositar"), valor);
                        pedidos.put(pedidoId, assinatura);
                    }
                }
                responderJson(requisicao, 200, resumoJson());
            } else {
                if (!metodo(requisicao, "GET")) return;
                String arquivo;
                String mime;
                switch (caminho) {
                    case "/": arquivo = "index.html"; mime = "text/html; charset=utf-8"; break;
                    case "/style.css": arquivo = "style.css"; mime = "text/css; charset=utf-8"; break;
                    case "/app.js": arquivo = "app.js"; mime = "application/javascript; charset=utf-8"; break;
                    case "/favicon.svg": arquivo = "favicon.svg"; mime = "image/svg+xml"; break;
                    default: responderJson(requisicao, 404, "{\"erro\":\"Página não encontrada.\"}"); return;
                }
                responder(requisicao, 200, mime, recurso(arquivo));
            }
        } catch (IllegalArgumentException e) {
            responderJson(requisicao, 400, "{\"erro\":" + json(e.getMessage()) + "}");
        } catch (Exception e) {
            System.err.println("Falha ao atender uma requisição: " + e.getClass().getSimpleName());
            responderJson(requisicao, 500, "{\"erro\":\"Não foi possível concluir. Confira o extrato antes de repetir.\"}");
        } finally {
            requisicao.close();
        }
    }

    private boolean metodo(HttpExchange requisicao, String permitido) throws IOException {
        if (permitido.equals(requisicao.getRequestMethod())) return true;
        requisicao.getResponseHeaders().set("Allow", permitido);
        responderJson(requisicao, 405, "{\"erro\":\"Método não permitido.\"}");
        return false;
    }

    private Map<String, String> formulario(String corpo) {
        Map<String, String> dados = new HashMap<>();
        for (String par : corpo.split("&")) {
            String[] partes = par.split("=", 2);
            if (partes.length != 2) throw new IllegalArgumentException("Dados incompletos.");
            String chave = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
            if ((!chave.equals("valor") && !chave.equals("pedidoId")) || dados.containsKey(chave)) {
                throw new IllegalArgumentException("Campos da operação inválidos.");
            }
            dados.put(chave, URLDecoder.decode(partes[1], StandardCharsets.UTF_8));
        }
        return dados;
    }

    private byte[] recurso(String arquivo) throws IOException {
        try (InputStream entrada = CaixaWeb.class.getResourceAsStream("/web/" + arquivo)) {
            if (entrada != null) return entrada.readAllBytes();
        }
        // Execução direta pelo VS Code, sem empacotamento em JAR.
        Path caminho = Path.of("web", arquivo);
        if (!Files.isRegularFile(caminho)) caminho = Path.of("ExercicioCaixaEletronico", "web", arquivo);
        return Files.readAllBytes(caminho);
    }

    private String resumoJson() {
        synchronized (conta) {
            StringBuilder texto = new StringBuilder("{")
                .append("\"titular\":").append(json(conta.nome))
                .append(",\"boasVindas\":").append(json("Saldo e histórico salvos entre execuções. Terminal e navegador usam a mesma conta."))
                .append(",\"saldo\":").append(json(conta.saldoFinal.toPlainString()))
                .append(",\"entradas\":").append(json(conta.totalDepositado.toPlainString()))
                .append(",\"saidas\":").append(json(conta.totalSacado.toPlainString()))
                .append(",\"totalOperacoes\":").append(conta.movimentacoes.size())
                .append(",\"csrfToken\":").append(json(csrf)).append(",\"movimentos\":[");
            for (int i = conta.movimentacoes.size() - 1; i >= 0; i--) {
                if (i < conta.movimentacoes.size() - 1) texto.append(',');
                String registro = conta.movimentacoes.get(i);
                String[] partes = registro.split(" \\| ", 4);
                boolean deposito = partes.length > 1 && partes[1].startsWith("Depósito: ");
                String valor = partes.length > 1 && partes[1].contains(": ")
                        ? partes[1].substring(partes[1].indexOf(": ") + 2) : "—";
                String saldo = partes.length > 3 && partes[3].startsWith("Saldo: ")
                        ? partes[3].substring(7) : "—";
                texto.append("{\"id\":").append(i + 1)
                    .append(",\"tipo\":").append(json(deposito ? "DEPOSITO" : "SAQUE"))
                    .append(",\"valorTexto\":").append(json(valor))
                    .append(",\"saldoTexto\":").append(json(saldo))
                    .append(",\"data\":").append(json(partes[0])).append('}');
            }
            return texto.append("]}").toString();
        }
    }

    private static String json(String texto) {
        StringBuilder saida = new StringBuilder("\"");
        for (char letra : texto.toCharArray()) {
            if (letra == '"' || letra == '\\') saida.append('\\').append(letra);
            else if (letra < 32) saida.append(String.format("\\u%04x", (int) letra));
            else saida.append(letra);
        }
        return saida.append('"').toString();
    }

    private void responderJson(HttpExchange requisicao, int status, String json) throws IOException {
        responder(requisicao, status, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private void responder(HttpExchange requisicao, int status, String mime, byte[] corpo) throws IOException {
        requisicao.getResponseHeaders().set("Content-Type", mime);
        requisicao.getResponseHeaders().set("Cache-Control", "no-store");
        requisicao.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        requisicao.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        requisicao.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
        requisicao.sendResponseHeaders(status, corpo.length);
        requisicao.getResponseBody().write(corpo);
    }

    @Override public void close() {
        if (encerrado.compareAndSet(false, true)) {
            servidor.stop(0);
            tarefas.shutdownNow();
        }
    }
}
