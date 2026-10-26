# CaixaEletronico

Simulador de caixa eletrônico em Java, executado no terminal, para praticar entrada de dados, validações e operações com uma conta de exemplo.

## Como executar

Tenha um JDK instalado e os comandos `java` e `javac` disponíveis no terminal. A execução e os testes também foram verificados com JDK 25. Use `java` e `javac` do mesmo JDK.

Na pasta principal do repositório, compile e execute:

```sh
javac -encoding UTF-8 -d out ExercicioCaixaEletronico/src/CaixaEletronico.java ExercicioCaixaEletronico/src/ArquivoConta.java
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
| Saldo inicial da primeira execução | R$ 50.000,00 |

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
| 8 | Consultar o resumo da conta |
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

O resumo acumulado da conta (opção 8) mostra o saldo inicial, a quantidade de operações, os totais depositado e sacado, o saldo final e sua variação. Ele também aparece ao encerrar a sessão. Para confirmar a saída pela opção 0, use as mesmas respostas de confirmação ou cancelamento das operações.

## Salvamento e recuperação dos dados

Ao executar `CaixaEletronico`, o programa recupera a conta do arquivo `dados/conta.properties`, relativo à pasta em que o comando foi executado. Se o arquivo ainda não existir, começa com R$ 50.000,00 e histórico vazio. Execute sempre da mesma pasta para usar a mesma conta.

Cada saque ou depósito confirmado é salvo imediatamente, junto com saldo, histórico, totais e contagens. Fechar e reabrir o programa mantém esses dados; as consultas e o resumo incluem todas as execuções. Cancelamentos e valores recusados não alteram o arquivo.

- A gravação usa um arquivo temporário e substituição atômica. Se não for possível gravar, a operação é cancelada e o saldo em memória permanece igual. O sistema de arquivos precisa suportar substituição atômica.
- Se o arquivo estiver inválido, incompleto ou inacessível, o programa encerra com uma mensagem e código de erro, sem reiniciar o saldo nem sobrescrever o conteúdo. Corrija o problema ou restaure uma cópia válida antes de abrir novamente.
- Só uma instância pode usar o mesmo arquivo por vez. O arquivo auxiliar `.lock` pode permanecer na pasta após sair; o bloqueio é liberado ao fechar o programa.
- Para testar uma conta separada ou usar um caminho fixo, informe a propriedade antes de `-cp`: `java "-Dcaixa.arquivo=C:/caminho/minha-conta.properties" -cp out CaixaEletronico`.

A pasta `dados/` não é enviada ao GitHub. Faça uma cópia do arquivo com o programa fechado se quiser guardar um backup. O arquivo é local e não criptografado; este simulador não oferece segurança bancária nem validação contra adulteração intencional.

## Testes automatizados

Na pasta principal do repositório, compile a aplicação e os três testes:

```sh
javac -encoding UTF-8 -d out ExercicioCaixaEletronico/src/CaixaEletronico.java ExercicioCaixaEletronico/src/ArquivoConta.java ExercicioCaixaEletronico/test/CaixaEletronicoCentavosTest.java ExercicioCaixaEletronico/test/CaixaEletronicoFluxosTest.java ExercicioCaixaEletronico/test/CaixaEletronicoPersistenciaTest.java
java -cp out CaixaEletronicoCentavosTest
java -cp out CaixaEletronicoFluxosTest
java -cp out CaixaEletronicoPersistenciaTest
```

Os testes não precisam de bibliotecas adicionais nem da opção `-ea`. Cada classe imprime `OK` quando termina com sucesso; uma falha encerra a execução com erro.

- **Precisão dos centavos:** deposita R$ 0,10 e R$ 0,20 e saca R$ 0,30, conferindo saldo, totais e quantidade de operações.
- **43 cenários de fluxo:** verificam cancelamento por zero, confirmação negativa e fim da entrada; valores não numéricos, negativos, frações de centavo e valores excessivos; saldo insuficiente e zerado; correção de entradas; separador de milhar; identificação; consultas e encerramento pelo menu.
- **Persistência:** 20 cenários verificam reabertura, precisão, histórico acumulado, cancelamentos, falhas de gravação, arquivo inválido, bloqueio de acesso simultâneo e duas execuções reais do programa.
- **Sessão completa:** identifica a conta, deposita, saca, consulta saldo, histórico, dados e resumo, cancela uma saída e depois encerra. Confere o estado final e as mensagens esperadas.

## Escopo da primeira versão

Esta versão é um projeto de estudo em Java no terminal, com uma única conta de exemplo. A identificação usa os dados informados acima e não possui senha. O saldo e as movimentações são mantidos em arquivo local entre as execuções.

Banco de dados, várias contas e interface gráfica ficam como possíveis evoluções. Os arquivos `App.java`, `ForArray.java` e `joguinho.java` são exercícios separados; o ponto de entrada do caixa é `CaixaEletronico`.
