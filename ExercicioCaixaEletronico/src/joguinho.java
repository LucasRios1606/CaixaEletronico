public class joguinho {
    
public static void main(String[] args) {

int numero = 3; // De 0 a 2  ira aprecer uma mensagem relacianada ao jogo se colocar um numero maior que 2 vai aprecer "jogue novamente"

switch(numero){

case 0 : 
System.out.println("voce perdeu a vez ");
break; //  O comando break é usado em laços de repetição while, do/while, for e com os comandos switch/case

case 1 : 
System.out.println("Avance 1 casa");
break;

case 2 : 
System.out.println("Voce ganhou um bonus! Avance 3 casas");
break;

default : 
System.out.println("jogue novamente");
break;


}
}
}
