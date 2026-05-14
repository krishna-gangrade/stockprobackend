@echo off
title StockPro Microservices Runner
color 0B

echo ==========================================================
echo           STOCKPRO MICROSERVICES ARCHITECTURE
echo ==========================================================
echo.

REM 1. Load environment variables
if exist .env (
    echo [1/4] Loading environment variables from .env...
    for /f "usebackq eol=# tokens=*" %%i in (".env") do set %%i
) else (
    echo [!] .env file not found!
    pause
    exit /b
)

REM 2. Start Docker Infrastructure
echo [2/4] Starting Docker Infrastructure (MySQL, Redis, Kafka)...
docker compose -f docker-compose.infra.yml up -d
if %ERRORLEVEL% neq 0 (
    echo.
    echo [!] Failed to start Docker infrastructure. 
    echo     Please make sure Docker Desktop is running!
    pause
    exit /b
)

echo Waiting for infrastructure to be healthy...
timeout /t 10 >nul

REM 3. Start Core Services (Eureka, Admin, Gateway)
echo [3/4] Starting Core Platform Services...

echo Starting Eureka Server...
start "Eureka Server" cmd /k "cd eureka-server && ..\mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments=\"-Xmx384m -Xms192m\""
timeout /t 12 >nul

echo Starting Admin Server...
start "Admin Server" cmd /k "cd admin-server && ..\mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments=\"-Xmx256m -Xms128m\""
timeout /t 5 >nul

echo Starting API Gateway...
start "API Gateway" cmd /k "cd api-gateway && ..\mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments=\"-Xmx384m -Xms192m\""
timeout /t 5 >nul

REM 4. Start Microservices
echo [4/4] Starting Microservices...

set SERVICES=auth-service product-service warehouse-service purchase-service payment-service alert-service movement-service report-service supplier-service

for %%s in (%SERVICES%) do (
    echo Starting %%s...
    start "%%s" cmd /k "cd %%s && ..\mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments=\"-Xmx256m -Xms128m\""
    timeout /t 5 >nul
)

echo.
echo ==========================================================
echo  ALL SERVICES STARTING...
echo ==========================================================
echo.
echo  - Eureka Dashboard   : http://localhost:8761
echo  - Admin Dashboard    : http://localhost:9090
echo  - API Gateway        : http://localhost:8082
echo  - MailHog (Email)    : http://localhost:8025
echo.
echo  TIP: To stop everything, run 'stop-all.bat'
echo.
pause
