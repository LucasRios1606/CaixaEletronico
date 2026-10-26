import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class CaixaEletronicoExtratoTest {
    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("caixa-extrato-");
        PrintStream original = System.out;
        try (PrintStream captura = new PrintStream(new ByteArrayOutputStream())) {
            System.setOut(captura);
            contaVazia(pasta.resolve("vazia"));
            conteudoERepeticao(pasta.resolve("repeticao"));
            contaRecuperada(pasta.resolve("recuperada"));
            menuComSucesso(pasta.resolve("menu"));
            falhaDeExportacao(pasta.resolve("obstaculo"));
        } finally {
            System.setOut(original);
        }
        System.out.println("OK: 6 cenários de exportação de extrato.");
    }

    private static void contaVazia(Path pasta) throws Exception {
        try (CaixaEletronico caixa = new CaixaEletronico()) {
            Path arquivo = caixa.exportarExtrato(pasta);
            String texto = ler(arquivo);
            contem(texto, "EXTRATO DO CAIXA ELETRÔNICO - SIMULAÇÃO");
            contem(texto, "Emitido em: ");
            contem(texto, "Nome: Lucas Rios de Souza Jordão");
            contem(texto, "Banco: Santander");
            contem(texto, "Agência: 12345");
            contem(texto, "Nenhuma movimentação realizada.");
            contem(texto, "Operações confirmadas: 0");
            contem(texto, "Saldo atual: " + caixa.moeda.format(new BigDecimal("50000")));
            verificar(arquivo.getFileName().toString().endsWith(".txt"), "Deve gerar texto");
            verificar(caixa.movimentacoes.isEmpty(), "Exportar não deve registrar operação");
        }
    }

    private static void conteudoERepeticao(Path pasta) throws Exception {
        try (CaixaEletronico caixa = new CaixaEletronico()) {
            entrada(caixa, "10,50 S 5,25 S 99 N");
            caixa.deposito();
            caixa.sacar();
            caixa.deposito();
            Path primeiro = caixa.exportarExtrato(pasta);
            byte[] original = Files.readAllBytes(primeiro);
            String texto = ler(primeiro);
            contem(texto, "1 - " + caixa.movimentacoes.get(0));
            contem(texto, "2 - " + caixa.movimentacoes.get(1));
            contem(texto, "Operações confirmadas: 2");
            contem(texto, "Depósitos confirmados: 1");
            contem(texto, "Saques confirmados: 1");
            contem(texto, "Total depositado: " + caixa.moeda.format(new BigDecimal("10.50")));
            contem(texto, "Total sacado: " + caixa.moeda.format(new BigDecimal("5.25")));
            contem(texto, "Saldo atual: " + caixa.moeda.format(new BigDecimal("50005.25")));
            contem(texto, "Variação do saldo: " + caixa.moeda.format(new BigDecimal("5.25")));
            verificar(!texto.contains("3 - "), "Cancelamento não deve entrar no extrato");
            Path segundo = caixa.exportarExtrato(pasta);
            verificar(!primeiro.equals(segundo), "Exportações seguidas devem ter nomes distintos");
            entrada(caixa, "1 S");
            caixa.deposito();
            Path terceiro = caixa.exportarExtrato(pasta);
            contem(ler(terceiro), "Operações confirmadas: 3");
            verificar(Arrays.equals(original, Files.readAllBytes(primeiro)), "Extrato antigo não pode ser alterado");
            verificar(caixa.saldoFinal.compareTo(new BigDecimal("50006.25")) == 0, "Exportar não deve alterar saldo");
        }
    }

    private static void contaRecuperada(Path pasta) throws Exception {
        Path conta = pasta.resolve("conta.properties");
        try (CaixaEletronico caixa = new CaixaEletronico(conta)) {
            entrada(caixa, "0,10 S");
            caixa.deposito();
        }
        byte[] antes = Files.readAllBytes(conta);
        try (CaixaEletronico caixa = new CaixaEletronico(conta)) {
            Path arquivo = caixa.exportarExtrato(caixa.pastaExtratos);
            verificar(arquivo.getParent().equals(pasta.resolve("extratos").toAbsolutePath()),
                    "Extratos devem ficar junto ao arquivo configurado da conta");
            contem(ler(arquivo), "1 - " + caixa.movimentacoes.get(0));
            contem(ler(arquivo), "Saldo atual: " + caixa.moeda.format(new BigDecimal("50000.10")));
        }
        verificar(Arrays.equals(antes, Files.readAllBytes(conta)), "Exportar não deve regravar a conta");
    }

    private static void menuComSucesso(Path pasta) throws Exception {
        try (CaixaEletronico caixa = new CaixaEletronico()) {
            caixa.pastaExtratos = pasta;
            entrada(caixa, "9 1 0 SIM");
            String saida = executarMenu(caixa);
            contem(saida, "Extrato exportado com sucesso: ");
            contem(saida, "Saldo atual de sua conta");
            contem(saida, "Obrigado por utilizar");
            try (java.util.stream.Stream<Path> arquivos = Files.list(pasta)) {
                verificar(arquivos.count() == 1, "Opção 9 deve gerar um extrato");
            }
        }
    }

    private static void falhaDeExportacao(Path caminho) throws Exception {
        Files.write(caminho, "arquivo existente".getBytes(StandardCharsets.UTF_8));
        try (CaixaEletronico caixa = new CaixaEletronico()) {
            caixa.pastaExtratos = caminho;
            entrada(caixa, "9 1 0 SIM");
            String saida = executarMenu(caixa);
            contem(saida, "Não foi possível exportar o extrato.");
            verificar(!saida.contains("Extrato exportado com sucesso"), "Falha não pode anunciar sucesso");
            contem(saida, "Saldo atual de sua conta");
            contem(saida, "Obrigado por utilizar");
            verificar(caixa.saldoFinal.compareTo(new BigDecimal("50000")) == 0, "Falha deve preservar saldo");
            verificar(caixa.movimentacoes.isEmpty(), "Falha deve preservar histórico");
            verificar(ler(caminho).equals("arquivo existente"), "Destino existente deve ser preservado");
        }
    }

    private static String executarMenu(CaixaEletronico caixa) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream captura = new PrintStream(buffer, true, "UTF-8")) {
            System.setOut(captura);
            caixa.menu();
        } finally {
            System.setOut(original);
        }
        return buffer.toString("UTF-8");
    }

    private static void entrada(CaixaEletronico caixa, String texto) {
        caixa.scanner = new Scanner(texto).useLocale(Locale.forLanguageTag("pt-BR"));
    }

    private static String ler(Path arquivo) throws IOException {
        return new String(Files.readAllBytes(arquivo), StandardCharsets.UTF_8);
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
