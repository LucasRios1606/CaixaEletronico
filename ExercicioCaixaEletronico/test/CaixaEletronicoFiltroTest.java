import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class CaixaEletronicoFiltroTest {
    private static int testes;

    public static void main(String[] args) throws Exception {
        try (CaixaEletronico caixa = new CaixaEletronico()) {
            String saida = consultar(caixa, "1", false);
            contem(saida, "Nenhuma movimentação realizada.");
            verificar(!saida.contains("Histórico de saques:"), "Conta vazia deve voltar diretamente");
            testes++;
        }
        try (CaixaEletronico caixa = new CaixaEletronico()) {
            preparar(caixa);
            List<String> antes = new ArrayList<>(caixa.movimentacoes);
            BigDecimal saldoAntes = caixa.saldoFinal;
            String saques = consultar(caixa, "1 0", false);
            contem(saques, "2 - " + antes.get(1));
            verificar(!saques.contains(antes.get(0)) && !saques.contains(antes.get(2)), "Filtro de saques inclui depósitos");
            contem(saques, "Movimentações encontradas: 1");
            contem(saques, "Total sacado: " + caixa.moeda.format(new BigDecimal("5.25")));
            testes++;

            String depositos = consultar(caixa, "2 0", false);
            contem(depositos, "1 - " + antes.get(0));
            contem(depositos, "3 - " + antes.get(2));
            verificar(!depositos.contains(antes.get(1)), "Filtro de depósitos inclui saque");
            contem(depositos, "Movimentações encontradas: 2");
            contem(depositos, "Total depositado: " + caixa.moeda.format(new BigDecimal("10.60")));
            testes++;

            String alternado = consultar(caixa, "abc -1 3 1,5 999999999999 1 2 0", false);
            contem(alternado, "Filtro inválido. Digite 1, 2 ou 0.");
            contem(alternado, "Histórico de saques:");
            contem(alternado, "Histórico de depósitos:");
            contem(alternado, "Consulta encerrada.");
            verificar(caixa.movimentacoes.equals(antes), "Consultar não deve alterar histórico");
            verificar(caixa.saldoFinal.compareTo(saldoAntes) == 0, "Consultar não deve alterar saldo");
            verificar(caixa.quantidadeSaques == 1 && caixa.quantidadeDepositos == 2, "Consultar não deve alterar contagens");
            testes++;

            String cancelado = consultar(caixa, "0", false);
            contem(cancelado, "Consulta encerrada.");
            verificar(!cancelado.contains("Histórico de saques:"), "Voltar não deve consultar");
            testes++;

            String fimEntrada = consultar(caixa, "1", false);
            contem(fimEntrada, "2 - " + antes.get(1));
            consultar(caixa, "", false);
            testes++;

            String menu = consultar(caixa, "10 2 0 7 3 0 1 0 SIM", true);
            contem(menu, "Histórico de depósitos:");
            contem(menu, "Movimentação consultada:");
            contem(menu, "3 - " + antes.get(2));
            contem(menu, "Saldo atual de sua conta");
            contem(menu, "Obrigado por utilizar");
            testes++;
        }
        try (CaixaEletronico caixa = new CaixaEletronico()) {
            capturar(() -> {
                entrada(caixa, "10 S");
                caixa.deposito();
            });
            String saida = consultar(caixa, "1 0", false);
            contem(saida, "Nenhum saque realizado.");
            contem(saida, "Movimentações encontradas: 0");
            contem(saida, "Total sacado: " + caixa.moeda.format(BigDecimal.ZERO));
            testes++;
        }
        try (CaixaEletronico caixa = new CaixaEletronico()) {
            capturar(() -> {
                entrada(caixa, "10 S");
                caixa.sacar();
            });
            String saida = consultar(caixa, "2 0", false);
            contem(saida, "Nenhum depósito realizado.");
            contem(saida, "Movimentações encontradas: 0");
            contem(saida, "Total depositado: " + caixa.moeda.format(BigDecimal.ZERO));
            testes++;
        }
        Path conta = Files.createTempDirectory("caixa-filtro-").resolve("conta.properties");
        try (CaixaEletronico caixa = new CaixaEletronico(conta)) {
            preparar(caixa);
        }
        byte[] arquivoAntes = Files.readAllBytes(conta);
        try (CaixaEletronico caixa = new CaixaEletronico(conta)) {
            String saida = consultar(caixa, "2 1 0", false);
            for (int i = 0; i < caixa.movimentacoes.size(); i++) {
                contem(saida, (i + 1) + " - " + caixa.movimentacoes.get(i));
            }
            verificar(Arrays.equals(arquivoAntes, Files.readAllBytes(conta)), "Consulta não deve regravar a conta");
            testes++;
        }
        System.out.println("OK: " + testes + " cenários de filtros do histórico.");
    }

    private static void preparar(CaixaEletronico caixa) throws Exception {
        capturar(() -> {
            entrada(caixa, "10,50 S 5,25 S 0,10 S 99 N");
            caixa.deposito();
            caixa.sacar();
            caixa.deposito();
            caixa.deposito();
        });
    }

    private static String consultar(CaixaEletronico caixa, String texto, boolean menu) throws Exception {
        entrada(caixa, texto);
        return capturar(() -> {
            if (menu) {
                caixa.menu();
            } else {
                caixa.consultarHistoricoPorTipo();
            }
        });
    }

    private static String capturar(Runnable acao) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream captura = new PrintStream(buffer, true, StandardCharsets.UTF_8.name())) {
            System.setOut(captura);
            acao.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private static void entrada(CaixaEletronico caixa, String texto) {
        caixa.scanner = new Scanner(texto).useLocale(Locale.forLanguageTag("pt-BR"));
    }

    private static void contem(String texto, String esperado) {
        verificar(texto.contains(esperado), "Conteúdo ausente: " + esperado);
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }
}
