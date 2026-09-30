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

O nome e o banco aceitam letras maiúsculas ou minúsculas. Esses dados estão definidos no código e servem para a simulação.

## Opções do menu

| Opção | Ação |
| --- | --- |
| 1 | Consultar saldo |
| 2 | Sacar |
| 3 | Depositar |
| 4 | Ver histórico de movimentações e totais |
| 5 | Consultar nome, banco e agência |
| 0 | Sair, após confirmação |

## Saques e depósitos

- Digite valores no padrão brasileiro, como `10,50` ou `1.234,56`, sem o símbolo `R$`.
- Use valores positivos, sem frações de centavo. Digite `0` para cancelar e voltar ao menu.
- Confira o valor e o saldo previsto. Digite `S` ou `s` para confirmar; outro valor cancela.
- Saques acima do saldo são recusados, com indicação do saldo disponível e de quanto falta.
- Cada operação confirmada recebe um número correspondente à sua posição no histórico.

O histórico mostra as operações confirmadas, com data, hora, valor e saldo após cada movimentação, além dos totais depositado e sacado. Operações canceladas ou recusadas não entram no histórico.

## Dados durante a execução

O saldo e o histórico ficam apenas na memória. Ao encerrar o programa, eles não são salvos. Uma nova execução começa com R$ 50.000,00 e o histórico vazio.
