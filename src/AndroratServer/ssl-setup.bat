@echo off
REM SSL/TLS Setup Script for Androrat Server (Windows)
REM This script generates a self-signed certificate for testing purposes.
REM For production use, obtain a certificate from a trusted Certificate Authority.

set KEYSTORE_FILE=keystore.jks
set KEYSTORE_PASSWORD=changeit
set KEY_ALIAS=androrat-server
set VALIDITY_DAYS=365

echo === Androrat Server SSL/TLS Setup ===
echo.
echo This script will generate a self-signed certificate for testing.
echo WARNING: Self-signed certificates should NOT be used in production!
echo.

REM Check if keytool is available
where keytool >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: keytool not found. Please install Java JDK and add it to PATH.
    pause
    exit /b 1
)

REM Check if keystore already exists
if exist "%KEYSTORE_FILE%" (
    echo WARNING: %KEYSTORE_FILE% already exists.
    set /p OVERWRITE="Do you want to overwrite it? (y/N): "
    if /i not "%OVERWRITE%"=="y" (
        echo Aborted.
        pause
        exit /b 0
    )
    del /f "%KEYSTORE_FILE%"
)

REM Generate keystore with self-signed certificate
echo Generating keystore with self-signed certificate...
keytool -genkeypair ^
    -alias "%KEY_ALIAS%" ^
    -keyalg RSA ^
    -keysize 2048 ^
    -validity %VALIDITY_DAYS% ^
    -keystore "%KEYSTORE_FILE%" ^
    -storepass "%KEYSTORE_PASSWORD%" ^
    -keypass "%KEYSTORE_PASSWORD%" ^
    -dname "CN=Androrat Server, OU=Security, O=Organization, L=City, ST=State, C=US"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo SUCCESS: Keystore created successfully!
    echo.
    echo Keystore file: %KEYSTORE_FILE%
    echo Keystore password: %KEYSTORE_PASSWORD%
    echo.
    echo To use this keystore, you can either:
    echo 1. Place it in the same directory as the server JAR
    echo 2. Specify its location using system properties:
    echo    java -Dandrorat.keystore.path=C:\path\to\keystore.jks ^
    echo         -Dandrorat.keystore.password=%KEYSTORE_PASSWORD% ^
    echo         -jar AndroratServer.jar
    echo.
    echo To disable SSL/TLS (NOT RECOMMENDED):
    echo    java -Dandrorat.ssl.enabled=false -jar AndroratServer.jar
    echo.
    echo IMPORTANT: For production use, obtain a certificate from a trusted CA!
) else (
    echo ERROR: Failed to generate keystore.
    pause
    exit /b 1
)

pause
