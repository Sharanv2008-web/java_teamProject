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
if "%1"=="mission" goto run_mission

:run_all
echo ================================================================================
echo  RUNNING FULL INTEGRATED SUBSEA PIPELINE PROJECT ON LOCALHOST
echo  Execution Suite: Student 2 Database Host + Student 1 Application Host
echo ================================================================================
echo.
echo --------------------------------------------------------------------------------
echo [STAGE 1/2] Executing Student 2 Mission Control ^& Database Ingestion Engine
echo --------------------------------------------------------------------------------
"%JAVA_BIN%" -cp "bin;lib/*" com.subsea.pipeline.MainApp
echo.
echo --------------------------------------------------------------------------------
echo [STAGE 2/2] Executing Student 1 Multithreaded Monitoring Engine
echo --------------------------------------------------------------------------------
"%JAVA_BIN%" -cp "bin;lib/*" com.subsea.MainApp
goto done

:run_mission
echo ================================================================================
echo  Running Set 30 Mission Control (Student 2 DB Host Integration)
echo ================================================================================
"%JAVA_BIN%" -cp "bin;lib/*" com.subsea.pipeline.MainApp
goto done

:run_test
echo ================================================================================
echo  Running Student 2 Database Host Remote Verification Test Suite
echo ================================================================================
"%JAVA_BIN%" -cp "bin;lib/*" com.subsea.pipeline.Student2_DBVerificationTest
goto done

:run_student1
echo ================================================================================
echo  Running Student 1 Application Host Engine (com.subsea.MainApp)
echo ================================================================================
"%JAVA_BIN%" -cp "bin;lib/*" com.subsea.MainApp
goto done

:done
endlocal
