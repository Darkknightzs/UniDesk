@echo off
setlocal

:: Find Java & Jar executables
where jar >nul 2>nul
if %ERRORLEVEL% equ 0 (
    set JAVA_CMD=java
    set JAVAC_CMD=javac
    set JAR_CMD=jar
) else if exist "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\jar.exe" (
    set "JAVA_CMD=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\java.exe"
    set "JAVAC_CMD=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\javac.exe"
    set "JAR_CMD=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\jar.exe"
) else if defined JAVA_HOME (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
    set "JAR_CMD=%JAVA_HOME%\bin\jar.exe"
) else (
    echo [ERROR] JDK was not found on your system!
    pause
    exit /b 1
)

if exist "out" (
    echo [0/3] Cleaning stale build artifacts in out\...
    rd /s /q "out"
)
mkdir "out"

echo [1/3] Compiling UniDesk classes...
"%JAVAC_CMD%" -cp "lib/*" -d out sis\*.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/3] Bundling dependencies...
if exist "lib\flatlaf.jar" (
    cd out
    "%JAR_CMD%" xf "..\lib\flatlaf.jar"
    cd ..
)

echo [3/3] Creating standalone UniDesk.jar...
"%JAR_CMD%" cfm UniDesk.jar manifest.txt -C out .

if exist "UniDesk.jar" (
    echo.
    echo ========================================================
    echo  SUCCESS! Created self-contained executable UniDesk.jar
    echo  You can share UniDesk.jar directly with your teacher.
    echo ========================================================
    echo.
) else (
    echo [ERROR] JAR creation failed.
)
pause
