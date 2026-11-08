@echo off
setlocal

set "JAVA_BIN=java"
if exist "C:\Program Files\Java\jdk-21.0.11\bin\java.exe" (
    set "JAVA_BIN=C:\Program Files\Java\jdk-21.0.11\bin\java.exe"
) else if defined JAVA_HOME (
    set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
)

if "%1"=="test" goto run_test
if "%1"=="student1" goto run_student1

:run_main
echo ================================================================================
echo  Running Set 30 Full System Simulation (Student 2 DB Host Integration)
echo ================================================================================
"%JAVA_BIN%" -cp bin com.subsea.pipeline.MainApp
goto done

:run_test
echo ================================================================================
echo  Running Student 2 Database Host Remote Verification Test Suite
echo ================================================================================
"%JAVA_BIN%" -cp bin com.subsea.pipeline.Student2_DBVerificationTest
goto done

:run_student1
echo ================================================================================
echo  Running Student 1 Application Host Engine (com.subsea.MainApp)
echo ================================================================================
"%JAVA_BIN%" -cp bin com.subsea.MainApp
goto done

:done
endlocal
