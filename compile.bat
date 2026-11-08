@echo off
setlocal
echo ================================================================================
echo  Compiling Deep-Sea Subsea Pipeline Integrity Diagnostics System (Student 2)
echo ================================================================================

if not exist bin mkdir bin
if not exist reports mkdir reports

set JAVAC_BIN=javac
if exist "C:\Program Files\Java\jdk-21.0.11\bin\javac.exe" (
    set "JAVAC_BIN=C:\Program Files\Java\jdk-21.0.11\bin\javac.exe"
) else if defined JAVA_HOME (
    set "JAVAC_BIN=%JAVA_HOME%\bin\javac.exe"
)

echo Compiling Java source files using: %JAVAC_BIN%
"%JAVAC_BIN%" -d bin -sourcepath src src\com\subsea\pipeline\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Compilation successful! Classes compiled to bin\
echo ================================================================================
endlocal
