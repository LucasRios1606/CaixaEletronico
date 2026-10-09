# CaixaEletronico

Simulador de caixa eletrônico em Java, executado no terminal, para praticar entrada de dados, validações e operações com uma conta de exemplo.

## Como executar

Tenha um JDK instalado e os comandos `java` e `javac` disponíveis no terminal. A execução abaixo foi verificada com JDK 21.

Na pasta principal do repositório, compile e execute:

```sh
javac -encoding UTF-8 -d out ExercicioCaixaEletronico/src/CaixaEletronico.java
java -cp out CaixaEletronico
```

A pasta `out` recebe as classes compiladas. Compile novamente após alterar o código.

## Dados da conta de exemplo

Informe os dados abaixo quando o programa solicitar:

| Campo | Valor |
| --- | --- |
| Nome | Lucas Rios de Souza Jordão |
| Banco | Santander |
| Agência | 12345 |
| Saldo inicial | R$ 50.000,00 |

O nome e o banco aceitam letras maiúsculas ou minúsculas. O nome também aceita espaços repetidos entre as palavras. Digite `SAIR` ao informar nome, banco ou agência para encerrar a identificação. Esses dados estão definidos no código e servem para a simulação.

## Opções do menu

| Opção | Ação |
| --- | --- |
| 1 | Consultar saldo |
| 2 | Sacar |
| 3 | Depositar |
| 4 | Ver histórico de movimentações e totais |
| 5 | Consultar nome, banco e agência |
| 6 | Consultar a última movimentação confirmada |
| 7 | Consultar movimentações pelo número (0 encerra a consulta) |
| 8 | Consultar o resumo da sessão |
| 0 | Sair, após confirmação |

## Saques e depósitos

- Digite valores no padrão brasileiro, como `10,50` ou `1.234,56`, sem o símbolo `R$`.
- Use valores positivos, sem frações de centavo. Digite `0` para cancelar e voltar ao menu.
- Confira o valor e o saldo previsto. Digite `S` ou `SIM` para confirmar, ou `N`, `NÃO` ou `NAO` para cancelar (maiúsculas ou minúsculas). Respostas diferentes fazem o programa pedir a confirmação novamente.
- O saldo atual aparece antes de informar o valor da operação. Valores inválidos podem ser corrigidos sem voltar ao menu.
- Saques acima do saldo são recusados, com indicação do saldo disponível e de quanto falta. Com saldo zerado, a opção de saque avisa e retorna ao menu.
- Cada operação confirmada recebe um número correspondente à sua posição no histórico.

O histórico mostra as operações confirmadas, com data, hora, valor e saldos anterior e posterior a cada movimentação, além das quantidades, totais e variação do saldo. Operações canceladas ou recusadas não entram no histórico.

A consulta por número permanece aberta para consultar outras movimentações até digitar `0`. Se não houver movimentações, o programa avisa e retorna ao menu.

O resumo da sessão (opção 8) mostra o saldo inicial, a quantidade de operações, os totais depositado e sacado, o saldo final e sua variação. Ele também aparece ao encerrar a sessão. Para confirmar a saída pela opção 0, use as mesmas respostas de confirmação ou cancelamento das operações.

## Dados durante a execução

O saldo e o histórico ficam apenas na memória. Ao encerrar o programa, eles não são salvos. Uma nova execução começa com R$ 50.000,00 e o histórico vazio.

## Teste de precisão dos centavos

Na pasta principal do repositório, execute:

```sh
javac -encoding UTF-8 -d out ExercicioCaixaEletronico/src/CaixaEletronico.java ExercicioCaixaEletronico/test/CaixaEletronicoCentavosTest.java
java -cp out CaixaEletronicoCentavosTest
```

O teste usa uma conta com saldo inicial zerado, deposita R$ 0,10 e R$ 0,20 e saca R$ 0,30. Ele confere o saldo exato, os totais e a quantidade de operações. Ao passar, exibe uma mensagem `OK`; se houver divergência, encerra com erro. Não precisa de bibliotecas adicionais nem da opção `-ea`.
