import java.math.BigDecimal;
import java.util.Locale;
import java.util.Scanner;

public class CaixaEletronicoCentavosTest {
    public static void main(String[] args) {
        CaixaEletronico caixa = new CaixaEletronico();
        caixa.saldo = BigDecimal.ZERO;
        caixa.saldoFinal = BigDecimal.ZERO;

        try (Scanner entrada = new Scanner("0,10 S 0,20 SIM 0,30 S")
                .useLocale(Locale.forLanguageTag("pt-BR"))) {
            caixa.scanner = entrada;
            caixa.deposito();
            verificarValor("Saldo após o primeiro depósito", "0.10", caixa.saldoFinal);

            caixa.deposito();
            verificarValor("Saldo após os dois depósitos", "0.30", caixa.saldoFinal);
            verificarValor("Total depositado", "0.30", caixa.totalDepositado);

            caixa.sacar();
            verificarValor("Saldo após sacar os trinta centavos", "0.00", caixa.saldoFinal);
            verificarValor("Total sacado", "0.30", caixa.totalSacado);

            if (caixa.quantidadeDepositos != 2 || caixa.quantidadeSaques != 1
                    || caixa.movimentacoes.size() != 3) {
                throw new AssertionError("Esperadas três movimentações: dois depósitos e um saque.");
            }
        }
        System.out.println("OK: operações com centavos preservam o saldo exato.");
    }

    private static void verificarValor(String descricao, String esperado, BigDecimal atual) {
        if (atual.compareTo(new BigDecimal(esperado)) != 0) {
            throw new AssertionError(descricao + ": esperado " + esperado + ", recebido " + atual);
        }
    }
}
