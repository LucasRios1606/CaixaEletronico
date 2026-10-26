# CaixaEletronico

Simulador de caixa eletrônico em Java, executado no terminal, para praticar entrada de dados, validações e operações com uma conta de exemplo.

## Como executar

Tenha um JDK instalado e os comandos `java` e `javac` disponíveis no terminal. A execução e os testes também foram verificados com JDK 25. Use `java` e `javac` do mesmo JDK.

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

## Testes automatizados

Na pasta principal do repositório, compile a aplicação e os dois testes:

```sh
javac -encoding UTF-8 -d out ExercicioCaixaEletronico/src/CaixaEletronico.java ExercicioCaixaEletronico/test/CaixaEletronicoCentavosTest.java ExercicioCaixaEletronico/test/CaixaEletronicoFluxosTest.java
java -cp out CaixaEletronicoCentavosTest
java -cp out CaixaEletronicoFluxosTest
```

Os testes não precisam de bibliotecas adicionais nem da opção `-ea`. Cada classe imprime `OK` quando termina com sucesso; uma falha encerra a execução com erro.

- **Precisão dos centavos:** deposita R$ 0,10 e R$ 0,20 e saca R$ 0,30, conferindo saldo, totais e quantidade de operações.
- **43 cenários de fluxo:** verificam cancelamento por zero, confirmação negativa e fim da entrada; valores não numéricos, negativos, frações de centavo e valores excessivos; saldo insuficiente e zerado; correção de entradas; separador de milhar; identificação; consultas e encerramento pelo menu.
- **Sessão completa:** identifica a conta, deposita, saca, consulta saldo, histórico, dados e resumo, cancela uma saída e depois encerra. Confere o estado final e as mensagens esperadas.

## Escopo da primeira versão

Esta versão é um projeto de estudo em Java no terminal, com uma única conta de exemplo. A identificação usa os dados informados acima e não possui senha. O saldo e as movimentações são mantidos somente durante a sessão.

Persistência em arquivo ou banco de dados, várias contas e interface gráfica ficam como possíveis evoluções. Os arquivos `App.java`, `ForArray.java` e `joguinho.java` são exercícios separados; o ponto de entrada do caixa é `CaixaEletronico`.
