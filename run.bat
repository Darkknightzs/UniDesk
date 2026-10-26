@echo off
setlocal

:: Find Java executable
where java >nul 2>nul
if %ERRORLEVEL% equ 0 (
    set JAVA_CMD=java
    set JAVAC_CMD=javac
) else if exist "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\java.exe"
    set "JAVAC_CMD=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\javac.exe"
) else if defined JAVA_HOME (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
) else (
    echo [ERROR] Java was not found on your system!
    pause
    exit /b 1
)

:: Ensure out directory exists
if not exist "out" mkdir "out"

echo Compiling Student Information System...
"%JAVAC_CMD%" -cp "lib/*" -d out sis\*.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo Starting Student Information System...
start "" "%JAVA_CMD%" -cp "out;lib/*" sis.Main
