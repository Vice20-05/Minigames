@echo off
rem Starts Mini Games on Windows. Needs Java 21 or newer (https://adoptium.net).
cd /d "%~dp0"

where javaw >/dev/null 2>nul
if errorlevel 1 (
    echo Java was not found. Install Java 21 or newer from https://adoptium.net and try again.
    pause
    exit /b 1
)

rem javaw = Java without a black console window, "start" lets this window close
start "" javaw -jar Minigames.jar
