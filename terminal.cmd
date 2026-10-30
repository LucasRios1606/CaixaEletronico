@echo off
call "%~dp0scripts\caixa.cmd" executar terminal "%~1"
exit /b %errorlevel%
