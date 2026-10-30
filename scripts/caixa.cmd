@echo off
setlocal DisableDelayedExpansion
if /i "%~1"=="executar" goto iniciar
if /i "%~1"=="testar" goto iniciar
echo Uso: executar.cmd ou testar.cmd
exit /b 2

:iniciar
pushd "%~dp0.."
if errorlevel 1 exit /b 1
set "MODO_CAIXA=%~1"
set "JAVAC_CAIXA="
set "JAVA_CAIXA="
if defined JAVA_HOME goto usar_java_home
for /f "delims=" %%J in ('where javac.exe 2^>nul') do if not defined JAVAC_CAIXA set "JAVAC_CAIXA=%%J"
if not defined JAVAC_CAIXA goto jdk_ausente
for %%J in ("%JAVAC_CAIXA%") do set "JAVA_CAIXA=%%~dpJjava.exe"
goto validar_jdk

:usar_java_home
set "JAVAC_CAIXA=%JAVA_HOME%\bin\javac.exe"
set "JAVA_CAIXA=%JAVA_HOME%\bin\java.exe"

:validar_jdk
if not exist "%JAVAC_CAIXA%" goto jdk_ausente
if not exist "%JAVA_CAIXA%" goto jdk_ausente
set "SAIDA_CAIXA=%CD%\out"
set "FONTES_CAIXA=%CD%\ExercicioCaixaEletronico\src"
set "TESTES_CAIXA=%CD%\ExercicioCaixaEletronico\test"
echo Compilando o caixa eletronico...
"%JAVAC_CAIXA%" -encoding UTF-8 -d "%SAIDA_CAIXA%" "%FONTES_CAIXA%\CaixaEletronico.java" "%FONTES_CAIXA%\ArquivoConta.java"
if errorlevel 1 goto falha_compilacao
if /i "%MODO_CAIXA%"=="testar" goto testes
"%JAVA_CAIXA%" -cp "%SAIDA_CAIXA%" CaixaEletronico
set "RESULTADO_CAIXA=%errorlevel%"
goto finalizar

:testes
set "TEMP_TESTES_CAIXA=%SAIDA_CAIXA%\testes-temporarios"
if not exist "%TEMP_TESTES_CAIXA%" mkdir "%TEMP_TESTES_CAIXA%"
if not exist "%TEMP_TESTES_CAIXA%\" goto falha_teste
"%JAVAC_CAIXA%" -encoding UTF-8 -cp "%SAIDA_CAIXA%" -d "%SAIDA_CAIXA%" "%TESTES_CAIXA%\CaixaEletronicoCentavosTest.java" "%TESTES_CAIXA%\CaixaEletronicoFluxosTest.java" "%TESTES_CAIXA%\CaixaEletronicoPersistenciaTest.java" "%TESTES_CAIXA%\CaixaEletronicoExtratoTest.java" "%TESTES_CAIXA%\CaixaEletronicoFiltroTest.java"
if errorlevel 1 goto falha_compilacao
for %%T in (CaixaEletronicoCentavosTest CaixaEletronicoFluxosTest CaixaEletronicoPersistenciaTest CaixaEletronicoExtratoTest CaixaEletronicoFiltroTest) do (
    echo Executando %%T...
    "%JAVA_CAIXA%" "-Djava.io.tmpdir=%TEMP_TESTES_CAIXA%" -cp "%SAIDA_CAIXA%" %%T
    if errorlevel 1 goto falha_teste
)
echo Todos os testes passaram.
set "RESULTADO_CAIXA=0"
goto finalizar

:jdk_ausente
echo Nao foi encontrado um JDK com java.exe e javac.exe.
echo Defina JAVA_HOME como a pasta do JDK, sem o trecho bin, ou adicione o bin do JDK ao PATH.
echo Se JAVA_HOME ja estiver definido, confira se aponta para um JDK valido.
set "RESULTADO_CAIXA=1"
goto finalizar

:falha_compilacao
echo Falha na compilacao. O programa nao foi iniciado.
set "RESULTADO_CAIXA=1"
goto finalizar

:falha_teste
echo Um teste falhou. Confira a mensagem acima.
set "RESULTADO_CAIXA=1"

:finalizar
popd
exit /b %RESULTADO_CAIXA%
