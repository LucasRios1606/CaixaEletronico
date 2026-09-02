import java.util.Scanner;

public class CaixaEletronico {
 
  public static void main(String[] args) {
  }
    
    
    String nome = "Lucas Rios de Souza Jordão";
    String banco = "Santander";
    String bancoInput;
    String nomeInput;
    int agencia = 12345;
    int agenciaInput;
     double saldo = 50000;
     double saldoFinal;
     double saque;
     double deposito;
     
 
 /**
  * O método identificadorUsuario foi criado para validar o usuário, o nome do banco e a agência. 
  */
     public void identificadorUsuario() {
         Scanner scanner = new Scanner(System.in);
 
         while (nomeInput != nome) {
             System.out.println("Informe o seu nome:");
                     nomeInput = scanner.nextLine();
                  if (nomeInput.equals(nome)) {      
                     System.out.println("Seja bem vindo, " + nome + ".");
                         break;
                 } else {
                     System.out.println("Usuário não reconhecido!");
                 }
         }  /**  Neste while, criei um sistema verificador do usuário, se o usuário informado for incorreto, ele vai me perguntar o nome de usuário até eu colocar o input esperado, após receber
         o input esperado, ele vai me dar boas vindas para o usuário e vai sair do loop.
         */
 
         while (bancoInput != banco) {
             System.out.println("Informe o nome do banco:");
               bancoInput = scanner.next();
 
         if (bancoInput.equals(banco)) {
             System.out.println("Seja bem-vindo, " + nome + " ao " + banco);
                 break;
         }else {
             System.out.println("Banco não reconhecido!");;
         }
         }
 
         while (agenciaInput != agencia) {
             System.out.println("Informe o número da agência:");
              agenciaInput = scanner.nextInt();
 
              System.out.println("Conta localizada com sucesso, " + nome + " bem vindo ao " + banco);
 
              
         if (agenciaInput ==agencia) {
             System.out.println("Conta localizada com sucesso, " + nome + " bem vindo ao " + banco);                
                 break;
         } else {
             System.out.println("Senhor cliente, " + nome + " o banco " + banco + " não conseguiu localizar sua conta, confira seus dados." );
         }   
         }
 
 }
     /**
      * O método saldo foi criado para criar o sistema de saque, ele pergunta o valor que deseja ser sacado e informa se a operação for concluída ou não.
      */
 
 
     public void saldo() {
         Scanner scanner = new Scanner(System.in);
 
         System.out.println(nome + " Informe o valor que deseja sacar!");
                 saque = scanner.nextDouble();
 
         if (saque > saldo) {
             System.out.println("Saldo indisponível");
         } else {
             saldoFinal = saldo - saque;
             System.out.println("Saque realizado com sucesso, seu saldo atual é de: " + " R$ " + saldoFinal);
         }
      
     }
 
     public void deposito() {
         Scanner scanner = new Scanner(System.in);
 
         System.out.println("Informe o valor do depósito:");
             deposito = scanner.nextDouble();
             saldoFinal = saldoFinal + deposito;
         System.out.println(nome + " seu depósito de " + " R$ " + deposito + " foi realizado com sucesso!");
         System.out.println();
         System.out.println("Saldo atual de sua conta " + " R$ " + saldoFinal);
 
 
 
     }
 
     
 }
