import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class CaixaEletronico {
 
  public static void main(String[] args) {
      CaixaEletronico caixa = new CaixaEletronico();
      if (caixa.identificadorUsuario()) {
          caixa.menu();
      }
  }
    
    
    String nome = "Lucas Rios de Souza Jordão";
    String banco = "Santander";
    String bancoInput;
    String nomeInput;
    int agencia = 12345;
    int agenciaInput;
     BigDecimal saldo = new BigDecimal("50000.00");
     BigDecimal saldoFinal = saldo;
     BigDecimal saque;
     BigDecimal deposito;
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
         while (!nome.equalsIgnoreCase(nomeInput)) {
             System.out.println("Informe o seu nome:");
             if (!scanner.hasNextLine()) {
                 return false;
             }
                     nomeInput = scanner.nextLine().trim();
                  if (nome.equalsIgnoreCase(nomeInput)) {
                     System.out.println("Seja bem vindo, " + nome + ".");
                 } else {
                     System.out.println("Usuário não reconhecido!");
                 }
         }  /**  Neste while, criei um sistema verificador do usuário, se o usuário informado for incorreto, ele vai me perguntar o nome de usuário até eu colocar o input esperado, após receber
         o input esperado, ele vai me dar boas vindas para o usuário e vai sair do loop.
         */
 
         while (!banco.equalsIgnoreCase(bancoInput)) {
             System.out.println("Informe o nome do banco:");
             if (!scanner.hasNextLine()) {
                 return false;
             }
               bancoInput = scanner.nextLine().trim();
 
         if (banco.equalsIgnoreCase(bancoInput)) {
             System.out.println("Seja bem-vindo, " + nome + " ao " + banco);
         }else {
             System.out.println("Banco não reconhecido!");;
         }
         }
 
         while (agenciaInput != agencia) {
             System.out.println("Informe o número da agência:");
             if (!scanner.hasNext()) {
                 return false;
             }
             if (!scanner.hasNextInt()) {
                 scanner.next();
                 System.out.println("Informe um número inteiro válido para a agência.");
                 continue;
             }
              agenciaInput = scanner.nextInt();
 
 
              
         if (agenciaInput ==agencia) {
             System.out.println("Conta localizada com sucesso, " + nome + " bem vindo ao " + banco);                
                 break;
         } else {
             System.out.println("Senhor cliente, " + nome + " o banco " + banco + " não conseguiu localizar sua conta, confira seus dados." );
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
             System.out.println("0 - Sair");
             System.out.println("Escolha uma opção:");

             if (!scanner.hasNext()) {
                 mostrarResumoSessao();
                 return;
             }
             if (!scanner.hasNextInt()) {
                 scanner.next();
                 System.out.println("Opção inválida! Digite um número inteiro de 0 a 7.");
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
                 case 0:
                     System.out.println("Ao sair, o saldo e o histórico desta execução não serão salvos.");
                     System.out.println("Deseja sair? Digite S ou SIM para confirmar ou outro valor para voltar ao menu:");
                     if (!scanner.hasNext()) {
                         mostrarResumoSessao();
                         return;
                     }
                     if (!confirmarOperacao()) {
                         System.out.println("Saída cancelada.");
                         break;
                     }
                     mostrarResumoSessao();
                     return;
                 default:
                     System.out.println("Opção inválida! Digite um número inteiro de 0 a 7.");
             }
         } while (true);
     }

     private void mostrarResumoSessao() {
         System.out.println("Resumo da sessão:");
         System.out.println("Saldo inicial: " + moeda.format(saldo));
         System.out.println("Operações confirmadas: " + movimentacoes.size());
         System.out.println("Depósitos confirmados: " + quantidadeDepositos);
         System.out.println("Saques confirmados: " + quantidadeSaques);
         System.out.println("Total depositado: " + moeda.format(totalDepositado));
         System.out.println("Total sacado: " + moeda.format(totalSacado));
         System.out.println("Saldo final: " + moeda.format(saldoFinal));
         System.out.println("Obrigado por utilizar nosso caixa eletrônico!");
     }

     private boolean confirmarOperacao() {
         if (!scanner.hasNext()) {
             return false;
         }
         String resposta = scanner.next();
         return resposta.equalsIgnoreCase("S") || resposta.equalsIgnoreCase("SIM");
     }

     private void registrarMovimentacao(String tipo, BigDecimal valor) {
         String dataHora = LocalDateTime.now().format(formatoDataHora);
         movimentacoes.add(dataHora + " | " + tipo + ": " + moeda.format(valor)
                 + " | Saldo: " + moeda.format(saldoFinal));
     }

     private void mostrarUltimaMovimentacao() {
         System.out.println("Última movimentação desta execução:");
         if (movimentacoes.isEmpty()) {
             System.out.println("Nenhuma movimentação realizada.");
             return;
         }
         int numero = movimentacoes.size();
         System.out.println(numero + " - " + movimentacoes.get(numero - 1));
     }

     private void consultarMovimentacao() {
         if (movimentacoes.isEmpty()) {
             System.out.println("Nenhuma movimentação realizada.");
             return;
         }
         while (true) {
             System.out.println("Informe o número da movimentação (1 a " + movimentacoes.size() + ") ou 0 para cancelar:");
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
                 System.out.println("Consulta cancelada.");
                 return;
             }
             if (numero < 1 || numero > movimentacoes.size()) {
                 System.out.println("Movimentação não encontrada.");
                 continue;
             }
             System.out.println("Movimentação consultada:");
             System.out.println(numero + " - " + movimentacoes.get(numero - 1));
             return;
         }
     }

     public void mostrarHistorico() {
         System.out.println("Histórico de movimentações desta execução:");
         if (movimentacoes.isEmpty()) {
             System.out.println("Nenhuma movimentação realizada.");
         }
         for (int indice = 0; indice < movimentacoes.size(); indice++) {
             System.out.println((indice + 1) + " - " + movimentacoes.get(indice));
         }
         System.out.println("Operações confirmadas: " + movimentacoes.size());
         System.out.println("Total depositado: " + moeda.format(totalDepositado));
         System.out.println("Total sacado: " + moeda.format(totalSacado));
         System.out.println("Saldo atual: " + moeda.format(saldoFinal));
     }

     public void sacar() {
         System.out.println(nome + " Informe o valor que deseja sacar (exemplo: 10,50) ou 0 para cancelar:");
         if (!scanner.hasNext()) {
             System.out.println("Saque cancelado.");
             return;
         }
         if (!scanner.hasNextBigDecimal()) {
             scanner.next();
             System.out.println("Informe um valor numérico para o saque.");
             return;
         }
         saque = scanner.nextBigDecimal();
         if (!Double.isFinite(saque.doubleValue())) {
             System.out.println("Informe um valor válido para o saque.");
             return;
         }
 
         if (saque.stripTrailingZeros().scale() > 2) {
             System.out.println("Informe um valor com no máximo duas casas decimais para o saque.");
             return;
         }

         if (saque.signum() == 0) {
             System.out.println("Saque cancelado.");
             return;
         }

         if (saque.signum() < 0) {
             System.out.println("O valor do saque deve ser maior que zero");
         } else if (saque.compareTo(saldoFinal) > 0) {
             System.out.println("Saldo insuficiente para realizar o saque.");
             System.out.println("Saldo disponível: " + moeda.format(saldoFinal));
             System.out.println("Valor que falta: " + moeda.format(saque.subtract(saldoFinal)));
         } else {
             BigDecimal novoSaldo = saldoFinal.subtract(saque);
             System.out.println("Saldo após o saque, se confirmado: " + moeda.format(novoSaldo));
             System.out.println("Confirmar saque de " + moeda.format(saque)
                     + "? Digite S ou SIM para confirmar ou outro valor para cancelar:");
             if (!confirmarOperacao()) {
                 System.out.println("Saque cancelado.");
                 return;
             }
             saldoFinal = novoSaldo;
             totalSacado = totalSacado.add(saque);
             registrarMovimentacao("Saque", saque);
             quantidadeSaques++;
             System.out.println("Saque realizado com sucesso, seu saldo atual é de: " + moeda.format(saldoFinal));
             System.out.println("Número da movimentação no histórico: " + movimentacoes.size());
         }
      
     }
 
     public void deposito() {
         BigDecimal novoSaldo;
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
             if (!Double.isFinite(deposito.doubleValue())) {
                 System.out.println("Informe um valor válido para o depósito.");
                 continue;
             }

             if (deposito.stripTrailingZeros().scale() > 2) {
                 System.out.println("Informe um valor com no máximo duas casas decimais para o depósito.");
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
                 + "? Digite S ou SIM para confirmar ou outro valor para cancelar:");
         if (!confirmarOperacao()) {
             System.out.println("Depósito cancelado.");
             return;
         }

         saldoFinal = novoSaldo;
         totalDepositado = totalDepositado.add(deposito);
         registrarMovimentacao("Depósito", deposito);
         quantidadeDepositos++;
         System.out.println(nome + " seu depósito de " + moeda.format(deposito) + " foi realizado com sucesso!");
         System.out.println("Número da movimentação no histórico: " + movimentacoes.size());
         System.out.println();
         System.out.println("Saldo atual de sua conta " + moeda.format(saldoFinal));
 
 
 
     }
 
     
 }
