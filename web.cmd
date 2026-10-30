@echo off
call "%~dp0scripts\caixa.cmd" executar web "%~1"
exit /b %errorlevel%
