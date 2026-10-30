# Caixa Eletrônico — terminal e localhost

Simulador em Java com duas interfaces: terminal operado por números e tela no navegador. As duas usam as mesmas operações, saldo e histórico, com salvamento em arquivo. Não há dinheiro real, senha ou conexão com bancos.

## Iniciar no Windows

É necessário um **JDK 11 ou superior**, com `java` e `javac` da mesma instalação. Validado com JDK 25. Abra o PowerShell na raiz do repositório:

| Comando | O que abre |
| --- | --- |
| `.\executar.cmd` | Terminal e navegador juntos |
| `.\terminal.cmd` | Somente o terminal |
| `.\web.cmd` | Somente a tela no navegador |
| `.\testar.cmd` | Compila e executa todos os testes |

No modo web ou conjunto, a tela fica em **http://localhost:8081/**. Os scripts tentam abrir o navegador padrão; se ele não abrir, acesse esse endereço manualmente. O terminal precisa continuar aberto enquanto você usa a página.

Para escolher outra porta: `.\web.cmd 8090` ou `.\executar.cmd ambos 8090`. Se a porta estiver ocupada, o programa avisa e encerra. O servidor aceita apenas conexões da própria máquina.

No modo conjunto, sair do terminal com `0` e confirmar com `1` também encerra o servidor. No modo somente web, use `Ctrl+C` no terminal. Fechar a aba não encerra o programa.

Os scripts compilam antes de iniciar e usam primeiro `JAVA_HOME`. Se ela não estiver definida, procuram `javac.exe` no `PATH` e usam o `java.exe` da mesma pasta. Na ausência de ambos, tentam o JDK incluído na extensão Java do VS Code. Se necessário, indique o seu JDK no terminal atual:

```powershell
$env:JAVA_HOME = 'C:\caminho\do\seu\jdk'
.\executar.cmd
```

Os scripts executam sempre na raiz do projeto, mesmo chamados de outra pasta, e restauram a pasta original do terminal ao terminar. Classes geradas ficam em `out/` e não são versionadas.

## Terminal: números e Enter

Na entrada, digite `1` para acessar a conta demonstrativa ou `0` para encerrar. Os dados da conta são apresentados automaticamente, sem pedir nome ou banco por texto.

Todas as confirmações usam **1 — Confirmar** e **0 — Cancelar**. Valores são digitados em **centavos, somente com algarismos**, sem vírgula, ponto ou símbolo:

| Digite | Valor |
| --- | --- |
| `1` | R$ 0,01 |
| `1050` | R$ 10,50 |
| `10000` | R$ 100,00 |
| `0` | Cancelar a operação |

Exemplo de depósito: acesse com `1`, escolha `3`, digite `1050` e confirme com `1`. O terminal mostra o valor em reais antes de confirmar. Letras, negativos e separadores são recusados no fluxo numérico.

| Opção | Ação |
| --- | --- |
| 1 | Consultar saldo |
| 2 | Sacar |
| 3 | Depositar |
| 4 | Histórico completo e totais |
| 5 | Dados da conta |
| 6 | Última movimentação |
| 7 | Consultar pelo número; 0 encerra a consulta |
| 8 | Resumo acumulado |
| 9 | Exportar extrato em texto |
| 10 | Filtrar histórico: 1 saques, 2 depósitos, 0 voltar |
| 0 | Encerrar, após confirmação |

Os filtros preservam os números originais das movimentações. Operações canceladas ou recusadas não alteram saldo, totais nem histórico.

## Tela no navegador

A tela mostra saldo, identificação da conta, saques, depósitos e histórico. Selecione a operação, informe o valor em reais (por exemplo, `10,50` ou `10.50`) e confira a prévia antes de confirmar. O botão **Voltar** cancela sem movimentar dinheiro.

Você pode ocultar os valores, filtrar saques ou depósitos e baixar um extrato completo em `.txt`. A página atualiza o saldo automaticamente a cada três segundos e oferece atualização manual. Se o servidor parar, ela informa a desconexão.

No modo **ambos**, uma operação feita em qualquer interface aparece na outra. A validação usa o saldo no instante da confirmação; se ele tiver mudado, um saque sem saldo é recusado. Pedidos repetidos com o mesmo identificador não duplicam uma operação na mesma sessão do servidor.

## Conta e persistência

A conta demonstrativa é de Lucas Rios de Souza Jordão, banco Santander, agência 12345. **Somente uma conta nova** começa com R$ 50.000,00 fictícios. Uma conta existente mantém o saldo e o histórico já salvos; a integração web não reinicia os dados.

- Conta: `dados/conta.properties` na raiz do projeto.
- Extratos exportados pelo terminal: `dados/extratos/`, com nomes exclusivos.
- Extratos baixados pela web: pasta de downloads escolhida pelo navegador.
- Testes: contas temporárias em `out/testes-temporarios/`.

Saques e depósitos são salvos antes de anunciar sucesso. Se a gravação falhar, a operação é cancelada. Um arquivo inválido ou inacessível não é sobrescrito nem substituído por uma conta nova. O sistema de arquivos deve suportar substituição atômica.

Somente um processo pode abrir o mesmo arquivo de conta por vez. Para usar as duas interfaces simultaneamente, escolha **ambos**, em vez de iniciar `terminal.cmd` e `web.cmd` separados. O arquivo `.lock` pode permanecer no disco após sair; o bloqueio é liberado ao encerrar.

Se você usava uma conta em outra pasta, execute manualmente com `-Dcaixa.arquivo`, ou copie seu arquivo para a raiz com o programa fechado, sem sobrescrever outra conta existente. Faça backups com o programa fechado. Arquivos em `dados/` ficam fora do GitHub; o conteúdo é local e não criptografado.

## Execução manual

A partir da raiz do repositório:

```sh
javac -encoding UTF-8 -d out ExercicioCaixaEletronico/src/CaixaEletronico.java ExercicioCaixaEletronico/src/ArquivoConta.java ExercicioCaixaEletronico/src/CaixaWeb.java
java -cp out CaixaEletronico ambos
java -cp out CaixaEletronico terminal
java -cp out CaixaEletronico web 8090
```

Execute apenas um desses comandos de inicialização por vez para a mesma conta. Sem argumento, o modo é `ambos`. A pasta `ExercicioCaixaEletronico/web/` deve acompanhar os fontes; ela é servida pelo Java, sem instalar Node ou dependências externas.

Para uma conta separada, use um caminho absoluto:

```sh
java "-Dcaixa.arquivo=C:/caminho/minha-conta.properties" -cp out CaixaEletronico ambos
```

## Testes

Execute `.\testar.cmd`. O script para na primeira falha e retorna código diferente de zero; uma execução aprovada termina com `Todos os testes passaram.` Não é necessário habilitar `-ea`.

Os testes cobrem centavos exatos; cancelamentos e entradas inválidas; identificação e menus; persistência e arquivos inválidos; exportação e filtros; terminal numérico; API HTTP; proteção contra pedidos duplicados e saques concorrentes; compartilhamento e recuperação de dados entre interfaces. Os testes HTTP precisam permitir conexões locais.

## Escopo

Projeto de estudo com uma conta demonstrativa. Não oferece autenticação bancária, várias contas ou proteção contra adulteração intencional dos arquivos locais. O navegador fica restrito ao localhost. Os arquivos `App.java`, `ForArray.java` e `joguinho.java` são exercícios separados; inicie o caixa pelos comandos acima.
