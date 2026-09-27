@echo off
echo ==============================================================================
echo                 ER TRIAGE ASSISTANT - LAUNCHER SCRIPT
echo ==============================================================================
echo Checking system prerequisites...

where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java JDK is not detected on PATH. Please install Java (JDK 17+ or 21+).
    pause
    exit /b 1
)

where python >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [WARN] Python is not detected as 'python' on PATH. Checking 'py' or 'python3'...
)

echo [1/2] Compiling Java Backend Classes...
if not exist "java-backend\bin" mkdir "java-backend\bin"
javac -d "java-backend\bin" java-backend\src\*.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java compilation failed.
    pause
    exit /b 1
)
echo [SUCCESS] Java compilation completed.

echo [2/2] Starting ER Triage Assistant Server...
echo The Web Dashboard will be available at: http://localhost:8080
echo Press Ctrl+C in this terminal window to stop the server.
echo ==============================================================================

cd ER-Triage-Assistant 2>nul
java -cp "java-backend\bin" Main
pause
