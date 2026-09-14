import java.text.NumberFormat;
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
     double saldo = 50000;
     double saldoFinal = saldo;
     double saque;
     double deposito;
     Scanner scanner = new Scanner(System.in);
     NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
     
 
 /**
  * O método identificadorUsuario foi criado para validar o usuário, o nome do banco e a agência. 
  */
     public boolean identificadorUsuario() {
         while (!nome.equals(nomeInput)) {
             System.out.println("Informe o seu nome:");
             if (!scanner.hasNextLine()) {
                 return false;
             }
                     nomeInput = scanner.nextLine().trim();
                  if (nomeInput.equals(nome)) {      
                     System.out.println("Seja bem vindo, " + nome + ".");
                 } else {
                     System.out.println("Usuário não reconhecido!");
                 }
         }  /**  Neste while, criei um sistema verificador do usuário, se o usuário informado for incorreto, ele vai me perguntar o nome de usuário até eu colocar o input esperado, após receber
         o input esperado, ele vai me dar boas vindas para o usuário e vai sair do loop.
         */
 
         while (!banco.equals(bancoInput)) {
             System.out.println("Informe o nome do banco:");
             if (!scanner.hasNextLine()) {
                 return false;
             }
               bancoInput = scanner.nextLine().trim();
 
         if (bancoInput.equals(banco)) {
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
             System.out.println("0 - Sair");
             System.out.println("Escolha uma opção:");

             if (!scanner.hasNext()) {
                 return;
             }
             if (!scanner.hasNextInt()) {
                 scanner.next();
                 System.out.println("Opção inválida!");
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
                 case 0:
                     System.out.println("Obrigado por utilizar nosso caixa eletrônico!");
                     return;
                 default:
                     System.out.println("Opção inválida!");
             }
         } while (true);
     }

     public void sacar() {
         System.out.println(nome + " Informe o valor que deseja sacar!");
         if (!scanner.hasNextDouble()) {
             if (scanner.hasNext()) {
                 scanner.next();
             }
             System.out.println("Informe um valor numérico para o saque.");
             return;
         }
                 saque = scanner.nextDouble();
         if (!Double.isFinite(saque)) {
             System.out.println("Informe um valor válido para o saque.");
             return;
         }
 
         if (saque <= 0) {
             System.out.println("O valor do saque deve ser maior que zero");
         } else if (saque > saldoFinal) {
             System.out.println("Saldo indisponível");
         } else {
             saldoFinal = saldoFinal - saque;
             System.out.println("Saque realizado com sucesso, seu saldo atual é de: " + moeda.format(saldoFinal));
         }
      
     }
 
     public void deposito() {
         System.out.println("Informe o valor do depósito:");
         if (!scanner.hasNextDouble()) {
             if (scanner.hasNext()) {
                 scanner.next();
             }
             System.out.println("Informe um valor numérico para o depósito.");
             return;
         }
             deposito = scanner.nextDouble();
         if (!Double.isFinite(deposito)) {
             System.out.println("Informe um valor válido para o depósito.");
             return;
         }

         if (deposito <= 0) {
             System.out.println("O valor do depósito deve ser maior que zero");
             return;
         }

         double novoSaldo = saldoFinal + deposito;
         if (!Double.isFinite(novoSaldo)) {
             System.out.println("O depósito ultrapassa o limite de saldo permitido.");
             return;
         }

         saldoFinal = novoSaldo;
         System.out.println(nome + " seu depósito de " + moeda.format(deposito) + " foi realizado com sucesso!");
         System.out.println();
         System.out.println("Saldo atual de sua conta " + moeda.format(saldoFinal));
 
 
 
     }
 
     
 }
