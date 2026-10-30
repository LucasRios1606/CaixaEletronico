@echo off
call "%~dp0scripts\caixa.cmd" executar "%~1" "%~2"
exit /b %errorlevel%
