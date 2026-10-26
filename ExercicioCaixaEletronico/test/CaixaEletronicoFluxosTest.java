import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.Consumer;

/** Testes de comportamento sem bibliotecas externas. */
public class CaixaEletronicoFluxosTest {
    private static int testes;

    public static void main(String[] args) throws Exception {
        for (String entrada : new String[] {"0", "10 N", "10 NÃO", "10 NAO", "", "10"}) {
            executar("Depósito cancelado: " + entrada, entrada, caixa -> {
                caixa.deposito();
                estado(caixa, "100.00", "0", "0", 0, 0);
            });
            executar("Saque cancelado: " + entrada, entrada, caixa -> {
                caixa.sacar();
                estado(caixa, "100.00", "0", "0", 0, 0);
            });
        }
        for (String invalido : new String[] {"abc", "-10", "1,001", "NaN", "Infinity", "1e309"}) {
            executar("Depósito inválido: " + invalido, invalido + " 0", caixa -> {
                caixa.deposito();
                estado(caixa, "100.00", "0", "0", 0, 0);
            });
            executar("Saque inválido: " + invalido, invalido + " 0", caixa -> {
                caixa.sacar();
                estado(caixa, "100.00", "0", "0", 0, 0);
            });
        }
        contem(executar("Saldo insuficiente", "100,01 0", caixa -> {
            caixa.sacar();
            estado(caixa, "100.00", "0", "0", 0, 0);
        }), "Saldo insuficiente");
        executar("Correção do valor e da confirmação", "abc -1 0,001 10 talvez sim", caixa -> {
            caixa.deposito();
            estado(caixa, "110.00", "10", "0", 1, 0);
        });
        executar("Corrigir saque acima do saldo", "101 25 s", caixa -> {
            caixa.sacar();
            estado(caixa, "75.00", "0", "25", 0, 1);
        });
        executar("Separador de milhar brasileiro", "1.234,56 S", caixa -> {
            caixa.deposito();
            estado(caixa, "1334.56", "1234.56", "0", 1, 0);
        });
        contem(executar("Sacar todo o saldo e tentar novamente", "100 S", caixa -> {
            caixa.sacar();
            caixa.sacar();
            estado(caixa, "0", "0", "100", 0, 1);
        }), "Seu saldo está zerado");
        for (String entrada : new String[] {"", "SAIR", "Lucas Rios de Souza Jordão\nSAIR",
                "Lucas Rios de Souza Jordão\nSantander\nSAIR"}) {
            executar("Identificação interrompida", entrada, caixa -> {
                verificar(!caixa.identificadorUsuario(), "Identificação deveria ser interrompida");
                estado(caixa, "100.00", "0", "0", 0, 0);
            });
        }
        executar("Identificação com correções", "Pessoa\n  lucas  rios de souza jordão \nOutro\n santander \nabc 1 12345", caixa -> {
            verificar(caixa.identificadorUsuario(), "Conta deveria ser reconhecida");
        });
        String sessao = executar("Sessão completa", "Lucas Rios de Souza Jordão\nSantander\n12345\n"
                + "abc 9 1 3 10,50 S 2 5,25 SIM 4 5 6 7 abc 9 1 0 8 0 N 1 0 SIM", caixa -> {
            verificar(caixa.identificadorUsuario(), "Identificação deveria funcionar");
            caixa.menu();
            estado(caixa, "105.25", "10.50", "5.25", 1, 1);
            contem(caixa.movimentacoes.get(0), "Depósito:");
            contem(caixa.movimentacoes.get(1), "Saque:");
            contem(caixa.movimentacoes.get(1), "Saldo anterior:");
        });
        for (String mensagem : new String[] {"Opção inválida", "Movimentação não encontrada",
                "Movimentação consultada:", "Dados da conta:", "Última movimentação",
                "Saída cancelada.", "Resumo da conta:", "Obrigado por utilizar"}) {
            contem(sessao, mensagem);
        }
        contem(executar("Consultas sem movimentações", "4 6 7", caixa -> {
            caixa.menu();
            estado(caixa, "100.00", "0", "0", 0, 0);
        }), "Nenhuma movimentação realizada.");
        for (String entrada : new String[] {"", "0", "0 talvez", "2", "2 10", "3", "3 10"}) {
            contem(executar("Fim da entrada no menu: " + entrada, entrada, caixa -> {
                caixa.menu();
                estado(caixa, "100.00", "0", "0", 0, 0);
            }), "Obrigado por utilizar");
        }
        System.out.println("OK: " + testes + " cenários de operações, identificação e menu.");
    }

    private static String executar(String nome, String entrada, Consumer<CaixaEletronico> acao)
            throws Exception {
        PrintStream saidaOriginal = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream captura = new PrintStream(buffer, true, StandardCharsets.UTF_8.name());
                Scanner scanner = new Scanner(entrada).useLocale(Locale.forLanguageTag("pt-BR"))) {
            System.setOut(captura);
            CaixaEletronico caixa = new CaixaEletronico();
            caixa.scanner = scanner;
            caixa.saldo = new BigDecimal("100.00");
            caixa.saldoFinal = caixa.saldo;
            try {
                acao.accept(caixa);
            } catch (AssertionError | RuntimeException erro) {
                throw new AssertionError("Falhou: " + nome, erro);
            }
            testes++;
        } finally {
            System.setOut(saidaOriginal);
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private static void estado(CaixaEletronico caixa, String saldo, String depositado,
            String sacado, int depositos, int saques) {
        verificar(caixa.saldoFinal.compareTo(new BigDecimal(saldo)) == 0, "Saldo incorreto");
        verificar(caixa.totalDepositado.compareTo(new BigDecimal(depositado)) == 0, "Total depositado incorreto");
        verificar(caixa.totalSacado.compareTo(new BigDecimal(sacado)) == 0, "Total sacado incorreto");
        verificar(caixa.quantidadeDepositos == depositos, "Quantidade de depósitos incorreta");
        verificar(caixa.quantidadeSaques == saques, "Quantidade de saques incorreta");
        verificar(caixa.movimentacoes.size() == depositos + saques, "Histórico incorreto");
    }

    private static void contem(String texto, String esperado) {
        verificar(texto.contains(esperado), "Mensagem ausente: " + esperado);
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }
}
