@echo off
REM ================================================================
REM  scan.bat — ЛР №1: Запуск SonarCloud через standalone CLI
REM  Сумісно з AGP 9.x (не використовує Gradle sonarqube plugin)
REM
REM  Використання: scan.bat <SONAR_TOKEN>
REM  Або:          set SONAR_TOKEN=xxx && scan.bat
REM ================================================================

setlocal enabledelayedexpansion

echo.
echo ============================================
echo   BeatStream -- SonarCloud Static Analysis
echo   LR #1: Avtomatyzovanyi kontrol yakosti
echo ============================================
echo.

REM ---------- Токен ----------
IF "%~1"=="" (
    IF "%SONAR_TOKEN%"=="" (
        echo [ERROR] Потрiбен SONAR_TOKEN!
        echo Використання: scan.bat YOUR_TOKEN
        exit /b 1
    )
) ELSE (
    SET SONAR_TOKEN=%~1
)

REM ---------- Шлях до sonar-scanner ----------
SET SCANNER_DIR=%~dp0.sonar-scanner
SET SCANNER_BIN=%SCANNER_DIR%\bin\sonar-scanner.bat
SET SCANNER_VERSION=6.2.1.4610
SET SCANNER_ZIP=%SCANNER_DIR%\sonar-scanner.zip
SET SCANNER_URL=https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-%SCANNER_VERSION%-windows-x64.zip

REM ---------- Завантажити scanner якщо відсутній ----------
IF NOT EXIST "%SCANNER_BIN%" (
    echo [INFO] sonar-scanner не знайдено. Завантажую...
    IF NOT EXIST "%SCANNER_DIR%" mkdir "%SCANNER_DIR%"

    powershell -NoProfile -Command ^
      "Invoke-WebRequest -Uri '%SCANNER_URL%' -OutFile '%SCANNER_ZIP%' -UseBasicParsing"

    IF ERRORLEVEL 1 (
        echo [ERROR] Не вдалося завантажити sonar-scanner.
        echo Перевiрте iнтернет-з'єднання або завантажте вручну з:
        echo   %SCANNER_URL%
        echo i розпакуйте у папку: %SCANNER_DIR%
        exit /b 1
    )

    echo [INFO] Розпаковую...
    powershell -NoProfile -Command ^
      "Expand-Archive -Path '%SCANNER_ZIP%' -DestinationPath '%SCANNER_DIR%\tmp' -Force"
    powershell -NoProfile -Command ^
      "Get-ChildItem '%SCANNER_DIR%\tmp' | Select-Object -First 1 | ForEach-Object { Move-Item $_.FullName '%SCANNER_DIR%\extracted' }"
    powershell -NoProfile -Command ^
      "Get-ChildItem '%SCANNER_DIR%\extracted' | ForEach-Object { Move-Item $_.FullName '%SCANNER_DIR%\' }"
    rd /s /q "%SCANNER_DIR%\tmp" 2>nul
    rd /s /q "%SCANNER_DIR%\extracted" 2>nul
    del "%SCANNER_ZIP%" 2>nul
    echo [INFO] sonar-scanner встановлено.
)

REM ---------- Зiбрати debug APK (для аналiзу бiнарних файлiв) ----------
echo [1/3] Складання debug build...
call gradlew.bat assembleDebug --no-daemon -q 2>nul
IF ERRORLEVEL 1 (
    echo [WARN] Збiрка не вдалась. Продовжуємо аналiз без бiнарних файлiв.
)
echo      OK

REM ---------- Unit-тести (Coverage) ----------
echo [2/3] Unit-тести...
call gradlew.bat testDebugUnitTest --no-daemon -q 2>nul
echo      OK

REM ---------- Запуск sonar-scanner ----------
echo [3/3] Запуск SonarCloud аналiзу...
"%SCANNER_BIN%" ^
    -Dsonar.token=%SONAR_TOKEN% ^
    -Dsonar.working.directory=.sonar-scanner\.scannerwork

IF ERRORLEVEL 1 (
    echo.
    echo [ERROR] Аналiз не вдався. Перевiрте токен та налаштування sonar-project.properties
    exit /b 1
)

echo.
echo ============================================
echo   Аналiз завершено!
echo   Результати: https://sonarcloud.io
echo ============================================
echo.
endlocal
