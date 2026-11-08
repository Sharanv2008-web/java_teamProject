@echo off
setlocal
echo ================================================================================
echo  Compiling Deep-Sea Subsea Pipeline Integrity Diagnostics System (Student 1 + Student 2)
echo ================================================================================

if not exist bin mkdir bin
if not exist reports mkdir reports

set "JAVAC_BIN=javac"
if exist "C:\Program Files\Java\jdk-21.0.11\bin\javac.exe" (
    set "JAVAC_BIN=C:\Program Files\Java\jdk-21.0.11\bin\javac.exe"
) else if defined JAVA_HOME (
    set "JAVAC_BIN=%JAVA_HOME%\bin\javac.exe"
)

echo Compiling Java source files using: %JAVAC_BIN%

echo [1/2] Compiling Student 2 Pipeline Core Package (com.subsea.pipeline)...
"%JAVAC_BIN%" -d bin -sourcepath src src\com\subsea\pipeline\*.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation of Student 2 pipeline package failed!
    exit /b %ERRORLEVEL%
)

echo [2/2] Compiling Student 1 Main Application Tree (com.subsea)...
"%JAVAC_BIN%" -d bin -sourcepath src\main\java src\main\java\com\subsea\model\*.java src\main\java\com\subsea\config\*.java src\main\java\com\subsea\dao\*.java src\main\java\com\subsea\calculation\*.java src\main\java\com\subsea\report\*.java src\main\java\com\subsea\worker\*.java src\main\java\com\subsea\service\*.java src\main\java\com\subsea\MainApp.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation of Student 1 app tree failed!
    exit /b %ERRORLEVEL%
)

echo.
echo [SUCCESS] All Student 1 and Student 2 classes compiled successfully into bin\
echo ================================================================================
endlocal
