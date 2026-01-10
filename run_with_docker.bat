@echo off
echo ===========================================
echo   Starting Concatenator in Docker...
echo ===========================================

docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running or not installed.
    echo Please start Docker Desktop and try again.
    pause
    exit /b
)

echo.
echo [1/3] Building Docker image...
docker-compose build
if %errorlevel% neq 0 (
    echo [ERROR] Build failed.
    pause
    exit /b
)

echo.
echo [2/3] Starting container...
echo The app will be available at: http://localhost:8080
echo.
echo [NOTE] Your current folder is mounted to /data inside the container.
echo        Please put your projects there or update docker-compose.yml.
echo.

docker-compose up

pause
