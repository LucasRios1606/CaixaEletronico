import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class CaixaEletronico implements AutoCloseable {

    public static void main(String[] args) {
        Path caminho = Paths.get(System.getProperty("caixa.arquivo", "dados/conta.properties"));
        try (CaixaEletronico caixa = new CaixaEletronico(caminho)) {
            if (caixa.identificadorUsuario()) {
                System.out.println("Dados da conta: " + caminho.toAbsolutePath().normalize());
                caixa.menu();
            }
        } catch (IOException erro) {
            System.err.println("Não foi possível abrir ou fechar o arquivo da conta: " + erro.getMessage());
            System.err.println("Verifique o arquivo e as permissões antes de tentar novamente.");
            System.exit(1);
        }
    }

    private ArquivoConta arquivo;
    Path pastaExtratos = Paths.get("dados", "extratos");

    /** Conta em memória para exercícios e testes que não precisam gravar dados. */
    public CaixaEletronico() {
    }

    public CaixaEletronico(Path caminho) throws IOException {
        arquivo = new ArquivoConta(caminho);
        pastaExtratos = caminho.toAbsolutePath().normalize().getParent().resolve("extratos");
        try {
            ArquivoConta.Estado estado = arquivo.carregar();
            if (estado != null) {
                aplicarEstado(estado);
            }
        } catch (IOException | RuntimeException erro) {
            try {
                arquivo.close();
            } catch (IOException erroFechamento) {
                erro.addSuppressed(erroFechamento);
            }
            throw erro;
        }
    }

    @Override
    public void close() throws IOException {
        if (arquivo != null) {
            arquivo.close();
        }
    }

    private void aplicarEstado(ArquivoConta.Estado estado) {
        saldo = estado.inicial;
        saldoFinal = estado.saldo;
        totalDepositado = estado.depositado;
        totalSacado = estado.sacado;
        quantidadeDepositos = estado.depositos;
        quantidadeSaques = estado.saques;
        movimentacoes = new ArrayList<>(estado.historico);
    }

    String nome = "Lucas Rios de Souza Jordão";
    String banco = "Santander";
    int agencia = 12345;
    BigDecimal saldo = new BigDecimal("50000.00");
    BigDecimal saldoFinal = saldo;
    BigDecimal totalSacado = BigDecimal.ZERO;
    BigDecimal totalDepositado = BigDecimal.ZERO;
    int quantidadeSaques = 0;
    int quantidadeDepositos = 0;
    Scanner scanner = new Scanner(System.in).useLocale(Locale.forLanguageTag("pt-BR"));
    NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    List<String> movimentacoes = new ArrayList<>();
    DateTimeFormatter formatoDataHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
    * O método identificadorUsuario foi criado para validar o usuário, o nome do banco e a agência.
    */
    public boolean identificadorUsuario() {
        String nomeInput = null;
        String bancoInput = null;
        int agenciaInput = 0;
        while (!nome.equalsIgnoreCase(nomeInput)) {
            System.out.println("Informe o seu nome ou digite SAIR para encerrar:");
            if (!scanner.hasNextLine()) {
                return false;
            }
            nomeInput = scanner.nextLine().trim().replaceAll("\\s+", " ");
            if (nomeInput.equalsIgnoreCase("SAIR")) {
                System.out.println("Identificação encerrada.");
                return false;
            }
            if (nome.equalsIgnoreCase(nomeInput)) {
                System.out.println("Seja bem vindo, " + nome + ".");
            } else {
                System.out.println("Usuário não reconhecido!");
            }
        }

        while (!banco.equalsIgnoreCase(bancoInput)) {
            System.out.println("Informe o nome do banco ou digite SAIR para encerrar:");
            if (!scanner.hasNextLine()) {
                return false;
            }
            bancoInput = scanner.nextLine().trim();
            if (bancoInput.equalsIgnoreCase("SAIR")) {
                System.out.println("Identificação encerrada.");
                return false;
            }

            if (banco.equalsIgnoreCase(bancoInput)) {
                System.out.println("Seja bem-vindo, " + nome + " ao " + banco);
            } else {
                System.out.println("Banco não reconhecido!");
            }
        }

        while (agenciaInput != agencia) {
            System.out.println("Informe o número da agência ou digite SAIR para encerrar:");
            if (!scanner.hasNext()) {
                return false;
            }
            if (!scanner.hasNextInt()) {
                String entrada = scanner.next();
                if (entrada.equalsIgnoreCase("SAIR")) {
                    System.out.println("Identificação encerrada.");
                    return false;
                }
                System.out.println("Informe um número inteiro válido para a agência.");
                continue;
            }
            agenciaInput = scanner.nextInt();

            if (agenciaInput == agencia) {
                System.out.println("Conta localizada com sucesso, " + nome + " bem vindo ao " + banco);
                break;
            } else {
                System.out.println("Senhor cliente, " + nome + " o banco " + banco + " não conseguiu localizar sua conta, confira seus dados.");
            }
        }

        return true;
    }
    /**
    * O método menu permite escolher as operações após identificar o usuário.
    */

    public void menu() {
        int opcao;
        do {
            System.out.println("1 - Consultar saldo");
            System.out.println("2 - Sacar");
            System.out.println("3 - Depositar");
            System.out.println("4 - Ver histórico de movimentações");
            System.out.println("5 - Consultar dados da conta");
            System.out.println("6 - Consultar última movimentação");
            System.out.println("7 - Consultar movimentação pelo número");
            System.out.println("8 - Consultar resumo da conta");
            System.out.println("9 - Exportar extrato em arquivo de texto");
            System.out.println("0 - Sair");
            System.out.println("Escolha uma opção:");

            if (!scanner.hasNext()) {
                encerrarSessao();
                return;
            }
            if (!scanner.hasNextInt()) {
                scanner.next();
                System.out.println("Opção inválida! Digite um número inteiro de 0 a 9.");
                continue;
            }
            opcao = scanner.nextInt();

            switch (opcao) {
                case 1:
                System.out.println("Saldo atual de sua conta " + moeda.format(saldoFinal));
                break;
                case 2:
                sacar();
                break;
                case 3:
                deposito();
                break;
                case 4:
                mostrarHistorico();
                break;
                case 5:
                System.out.println("Dados da conta:");
                System.out.println("Nome: " + nome);
                System.out.println("Banco: " + banco);
                System.out.println("Agência: " + agencia);
                break;
                case 6:
                mostrarUltimaMovimentacao();
                break;
                case 7:
                consultarMovimentacao();
                break;
                case 8:
                mostrarResumoConta();
                break;
                case 9:
                exportarExtrato();
                break;
                case 0:
                System.out.println(arquivo == null ? "Conta em memória: os dados não serão salvos." : "As operações confirmadas já estão salvas.");
                System.out.println("Deseja sair? Digite S ou SIM para confirmar ou N, NÃO ou NAO para voltar ao menu:");
                if (!scanner.hasNext()) {
                    encerrarSessao();
                    return;
                }
                if (!confirmarOperacao()) {
                    System.out.println("Saída cancelada.");
                    break;
                }
                encerrarSessao();
                return;
                default:
                System.out.println("Opção inválida! Digite um número inteiro de 0 a 9.");
            }
        } while (true);
    }

    private List<String> linhasTotaisMovimentacoes() {
        List<String> linhas = new ArrayList<>();
        linhas.add("Operações confirmadas: " + movimentacoes.size());
        linhas.add("Depósitos confirmados: " + quantidadeDepositos);
        linhas.add("Saques confirmados: " + quantidadeSaques);
        linhas.add("Total depositado: " + moeda.format(totalDepositado));
        linhas.add("Total sacado: " + moeda.format(totalSacado));
        return linhas;
    }

    private void mostrarTotaisMovimentacoes() {
        linhasTotaisMovimentacoes().forEach(System.out::println);
    }

    private void exportarExtrato() {
        try {
            Path destino = exportarExtrato(pastaExtratos);
            System.out.println("Extrato exportado com sucesso: " + destino);
        } catch (IOException erro) {
            System.out.println("Não foi possível exportar o extrato. Verifique a pasta e as permissões.");
        }
    }

    /** Gera uma cópia legível da conta, sem alterar saldo, histórico ou extratos anteriores. */
    public Path exportarExtrato(Path pasta) throws IOException {
        LocalDateTime agora = LocalDateTime.now();
        List<String> linhas = new ArrayList<>();
        linhas.add("EXTRATO DO CAIXA ELETRÔNICO - SIMULAÇÃO");
        linhas.add("Emitido em: " + agora.format(formatoDataHora));
        linhas.add("Nome: " + nome);
        linhas.add("Banco: " + banco);
        linhas.add("Agência: " + agencia);
        linhas.add("");
        linhas.add("Histórico completo de movimentações:");
        if (movimentacoes.isEmpty()) {
            linhas.add("Nenhuma movimentação realizada.");
        }
        for (int i = 0; i < movimentacoes.size(); i++) {
            linhas.add((i + 1) + " - " + movimentacoes.get(i));
        }
        linhas.add("");
        linhas.add("Resumo acumulado da conta:");
        linhas.add("Saldo inicial: " + moeda.format(saldo));
        linhas.addAll(linhasTotaisMovimentacoes());
        linhas.add("Saldo atual: " + moeda.format(saldoFinal));
        linhas.add("Variação do saldo: " + moeda.format(saldoFinal.subtract(saldo)));
        Path diretorio = pasta.toAbsolutePath().normalize();
        Files.createDirectories(diretorio);
        String prefixo = "extrato-" + agora.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-";
        Path destino = Files.createTempFile(diretorio, prefixo, ".txt");
        try {
            Files.write(destino, linhas, StandardCharsets.UTF_8);
        } catch (IOException erro) {
            try {
                Files.deleteIfExists(destino);
            } catch (IOException erroLimpeza) {
                erro.addSuppressed(erroLimpeza);
            }
            throw erro;
        }
        return destino;
    }

    private void mostrarResumoConta() {
        System.out.println("Resumo da conta:");
        System.out.println("Saldo inicial: " + moeda.format(saldo));
        mostrarTotaisMovimentacoes();
        System.out.println("Saldo final: " + moeda.format(saldoFinal));
        System.out.println("Variação do saldo: " + moeda.format(saldoFinal.subtract(saldo)));
    }

    private void encerrarSessao() {
        mostrarResumoConta();
        System.out.println("Obrigado por utilizar nosso caixa eletrônico!");
    }

    private boolean validarValorMonetario(BigDecimal valor, String operacao) {
        if (!Double.isFinite(valor.doubleValue())) {
            System.out.println("Informe um valor válido para o " + operacao + ".");
            return false;
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            System.out.println("Informe um valor com no máximo duas casas decimais para o " + operacao + ".");
            return false;
        }
        return true;
    }

    private boolean confirmarOperacao() {
        while (scanner.hasNext()) {
            String resposta = scanner.next();
            if (resposta.equalsIgnoreCase("S") || resposta.equalsIgnoreCase("SIM")) {
                return true;
            }
            if (resposta.equalsIgnoreCase("N") || resposta.equalsIgnoreCase("NÃO")
            || resposta.equalsIgnoreCase("NAO")) {
                return false;
            }
            System.out.println("Resposta inválida. Digite S ou SIM para confirmar, ou N, NÃO ou NAO para cancelar:");
        }
        return false;
    }

    private boolean registrarMovimentacao(String tipo, BigDecimal valor, BigDecimal novoSaldo) {
        boolean deposito = tipo.equals("Depósito");
        List<String> historico = new ArrayList<>(movimentacoes);
        String dataHora = LocalDateTime.now().format(formatoDataHora);
        historico.add(dataHora + " | " + tipo + ": " + moeda.format(valor)
                + " | Saldo anterior: " + moeda.format(saldoFinal)
                + " | Saldo: " + moeda.format(novoSaldo));
        ArquivoConta.Estado estado = new ArquivoConta.Estado(saldo, novoSaldo,
                deposito ? totalDepositado.add(valor) : totalDepositado,
                deposito ? totalSacado : totalSacado.add(valor),
                quantidadeDepositos + (deposito ? 1 : 0),
                quantidadeSaques + (deposito ? 0 : 1), historico);
        if (arquivo != null) {
            try {
                arquivo.salvar(estado);
            } catch (IOException erro) {
                System.out.println("Não foi possível salvar. Operação cancelada; saldo e histórico mantidos.");
                return false;
            }
        }
        aplicarEstado(estado);
        return true;
    }

    private void mostrarMovimentacao(int numero) {
        System.out.println(numero + " - " + movimentacoes.get(numero - 1));
    }

    private void mostrarUltimaMovimentacao() {
        System.out.println("Última movimentação da conta:");
        if (movimentacoes.isEmpty()) {
            System.out.println("Nenhuma movimentação realizada.");
            return;
        }
        int numero = movimentacoes.size();
        mostrarMovimentacao(numero);
    }

    private void consultarMovimentacao() {
        if (movimentacoes.isEmpty()) {
            System.out.println("Nenhuma movimentação realizada.");
            return;
        }
        while (true) {
            System.out.println("Informe o número da movimentação (1 a " + movimentacoes.size() + ") ou 0 para encerrar a consulta:");
            if (!scanner.hasNext()) {
                return;
            }
            if (!scanner.hasNextInt()) {
                scanner.next();
                System.out.println("Informe um número inteiro válido para a movimentação.");
                continue;
            }
            int numero = scanner.nextInt();
            if (numero == 0) {
                System.out.println("Consulta encerrada.");
                return;
            }
            if (numero < 1 || numero > movimentacoes.size()) {
                System.out.println("Movimentação não encontrada.");
                continue;
            }
            System.out.println("Movimentação consultada:");
            mostrarMovimentacao(numero);
        }
    }

    public void mostrarHistorico() {
        System.out.println("Histórico de movimentações da conta:");
        if (movimentacoes.isEmpty()) {
            System.out.println("Nenhuma movimentação realizada.");
        }
        for (int indice = 0; indice < movimentacoes.size(); indice++) {
            mostrarMovimentacao(indice + 1);
        }
        mostrarTotaisMovimentacoes();
        System.out.println("Saldo atual: " + moeda.format(saldoFinal));
        System.out.println("Variação do saldo: " + moeda.format(saldoFinal.subtract(saldo)));
    }

    public void sacar() {
        System.out.println("Saldo atual: " + moeda.format(saldoFinal));
        if (saldoFinal.signum() == 0) {
            System.out.println("Seu saldo está zerado. Faça um depósito antes de realizar um saque.");
            return;
        }

        while (true) {
            System.out.println(nome + " Informe o valor que deseja sacar (exemplo: 10,50) ou 0 para cancelar:");
            if (!scanner.hasNext()) {
                System.out.println("Saque cancelado.");
                return;
            }
            if (!scanner.hasNextBigDecimal()) {
                scanner.next();
                System.out.println("Informe um valor numérico para o saque.");
                continue;
            }
            BigDecimal saque = scanner.nextBigDecimal();
            if (!validarValorMonetario(saque, "saque")) {
                continue;
            }

            if (saque.signum() == 0) {
                System.out.println("Saque cancelado.");
                return;
            }

            if (saque.signum() < 0) {
                System.out.println("O valor do saque deve ser maior que zero");
                continue;
            }
            if (saque.compareTo(saldoFinal) > 0) {
                System.out.println("Saldo insuficiente para realizar o saque.");
                System.out.println("Saldo disponível: " + moeda.format(saldoFinal));
                System.out.println("Valor que falta: " + moeda.format(saque.subtract(saldoFinal)));
                continue;
            }

            BigDecimal novoSaldo = saldoFinal.subtract(saque);
            System.out.println("Saldo após o saque, se confirmado: " + moeda.format(novoSaldo));
            System.out.println("Confirmar saque de " + moeda.format(saque)
            + "? Digite S ou SIM para confirmar ou N, NÃO ou NAO para cancelar:");
            if (!confirmarOperacao()) {
                System.out.println("Saque cancelado.");
                return;
            }
            if (!registrarMovimentacao("Saque", saque, novoSaldo)) {
                return;
            }
            System.out.println("Saque realizado com sucesso, seu saldo atual é de: " + moeda.format(saldoFinal));
            System.out.println("Número da movimentação no histórico: " + movimentacoes.size());
            return;
        }
    }

    public void deposito() {
        System.out.println("Saldo atual: " + moeda.format(saldoFinal));
        BigDecimal novoSaldo;
        BigDecimal deposito;
        while (true) {
            System.out.println("Informe o valor do depósito (exemplo: 10,50) ou 0 para cancelar:");
            if (!scanner.hasNext()) {
                System.out.println("Depósito cancelado.");
                return;
            }
            if (!scanner.hasNextBigDecimal()) {
                scanner.next();
                System.out.println("Informe um valor numérico para o depósito.");
                continue;
            }
            deposito = scanner.nextBigDecimal();
            if (!validarValorMonetario(deposito, "depósito")) {
                continue;
            }

            if (deposito.signum() == 0) {
                System.out.println("Depósito cancelado.");
                return;
            }

            if (deposito.signum() < 0) {
                System.out.println("O valor do depósito deve ser maior que zero");
                continue;
            }

            novoSaldo = saldoFinal.add(deposito);
            if (!Double.isFinite(novoSaldo.doubleValue())) {
                System.out.println("O depósito ultrapassa o limite de saldo permitido.");
                continue;
            }
            break;
        }

        System.out.println("Saldo após o depósito, se confirmado: " + moeda.format(novoSaldo));
        System.out.println("Confirmar depósito de " + moeda.format(deposito)
        + "? Digite S ou SIM para confirmar ou N, NÃO ou NAO para cancelar:");
        if (!confirmarOperacao()) {
            System.out.println("Depósito cancelado.");
            return;
        }

        if (!registrarMovimentacao("Depósito", deposito, novoSaldo)) {
            return;
        }
        System.out.println(nome + " seu depósito de " + moeda.format(deposito) + " foi realizado com sucesso!");
        System.out.println("Número da movimentação no histórico: " + movimentacoes.size());
        System.out.println();
        System.out.println("Saldo atual de sua conta " + moeda.format(saldoFinal));

    }

}
