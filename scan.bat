@echo off
REM ================================================================
REM  scan.bat — Лабораторна Робота №1: Запуск SonarCloud аналізу
REM  Використання: scan.bat [SONAR_TOKEN]
REM  Або встановіть SONAR_TOKEN у змінних середовища
REM ================================================================

echo.
echo ============================================
echo   BeatStream — SonarCloud Static Analysis
echo   ЛР №1: Автоматизований контроль якості
echo ============================================
echo.

REM Перевіряємо наявність токена
IF "%SONAR_TOKEN%"=="" (
    IF "%1"=="" (
        echo [ERROR] Не вказано SONAR_TOKEN!
        echo Використання: scan.bat YOUR_SONAR_TOKEN
        echo Або: set SONAR_TOKEN=YOUR_TOKEN ^&^& scan.bat
        exit /b 1
    ) ELSE (
        SET SONAR_TOKEN=%1
    )
)

echo [1/3] Збираємо проєкт (debug build)...
call gradlew.bat assembleDebug --no-daemon -q
IF ERRORLEVEL 1 (
    echo [ERROR] Збірка не вдалась. Перевірте помилки компіляції.
    exit /b 1
)
echo      OK

echo [2/3] Запускаємо unit-тести для Coverage...
call gradlew.bat testDebugUnitTest --no-daemon -q
echo      OK (ігноруємо помилки тестів для цілей аналізу)

echo [3/3] Запускаємо SonarCloud аналіз...
call gradlew.bat sonar ^
    -Dsonar.token=%SONAR_TOKEN% ^
    --no-daemon
IF ERRORLEVEL 1 (
    echo [ERROR] Аналіз SonarCloud не вдався.
    exit /b 1
)

echo.
echo ============================================
echo   Аналіз завершено!
echo   Результати: https://sonarcloud.io
echo ============================================
echo.
