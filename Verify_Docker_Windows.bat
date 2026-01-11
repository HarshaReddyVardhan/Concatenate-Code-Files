@echo off
setlocal EnableDelayedExpansion

echo ==========================================
echo  Checking Docker and WSL Setup (Windows)
echo ==========================================
echo.

:: 1. Check for Docker
where docker >nul 2>nul
if %errorlevel% neq 0 (
    echo [X] Docker is NOT installed or not in PATH.
    echo     Hint: Download Docker Desktop from https://docs.docker.com/desktop/install/windows-install/
) else (
    for /f "tokens=*" %%i in ('docker --version') do set DOCKER_VER=%%i
    echo [OK] Docker is installed: !DOCKER_VER!

    :: 2. Check if Docker daemon is running
    docker info >nul 2>nul
    if !errorlevel! neq 0 (
        echo [X] Docker daemon is NOT running.
        echo     Hint: Please start Docker Desktop application.
    ) else (
        echo [OK] Docker daemon is running.
    )
)

echo.
echo ------------------------------------------
echo  Checking WSL (Windows Subsystem for Linux)
echo ------------------------------------------

:: 3. Check for WSL
where wsl >nul 2>nul
if %errorlevel% neq 0 (
    echo [X] WSL is NOT found in PATH.
    echo     Hint: Ensure you are on Windows 10 version 2004+ or Windows 11.
    echo           Run 'wsl --install' in an Administrator terminal.
) else (
    echo [OK] WSL command is available.
    
    :: Check specific WSL status
    echo.
    echo Checking WSL Status...
    wsl --status
    
    echo.
    echo Installed Distributions:
    wsl -l -v
    
    echo.
    echo NOTE: For Docker Desktop on Windows, WSL 2 is recommended.
)

echo.
echo ==========================================
echo Check complete.
pause
