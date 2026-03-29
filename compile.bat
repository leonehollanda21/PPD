@echo off
echo Compilando projeto Dara...

if not exist "bin" mkdir bin

javac -d bin -cp src\main\java src\main\java\com\dara\*.java

if %errorlevel% == 0 (
    echo Compilacao concluida com sucesso!
    echo.
    echo Para executar o jogo, use:
    echo   java -cp bin com.dara.DaraGameGUI
    echo.
    echo Ou execute: run.bat
) else (
    echo Erro na compilacao!
)
