@echo off
setlocal EnableDelayedExpansion
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
call :ensure_infra stockpro-db stockpro-db
if errorlevel 1 goto :infra_failed
call :ensure_infra stockpro-auth-redis auth-redis
if errorlevel 1 goto :infra_failed
call :ensure_infra stockpro-zookeeper zookeeper
if errorlevel 1 goto :infra_failed
call :ensure_infra stockpro-kafka kafka
if errorlevel 1 goto :infra_failed

echo     StockPro infrastructure is ready.
goto :infra_done

:infra_failed
echo.
echo [!] Failed to start Docker infrastructure.
echo     If you already started the full Docker stack, stop it first with:
echo     .\docker-start.ps1 -Stop
echo     Then retry run-all.bat, or just use .\docker-start.ps1 for the full stack.
pause
exit /b

:ensure_infra
set CONTAINER_NAME=%~1
set COMPOSE_SERVICE=%~2

docker container inspect !CONTAINER_NAME! >nul 2>&1
if errorlevel 1 (
    echo     Creating !CONTAINER_NAME!...
    docker compose -f docker-compose.infra.yml up -d !COMPOSE_SERVICE!
    if !ERRORLEVEL! neq 0 exit /b 1
) else (
    docker inspect -f "{{.State.Running}}" !CONTAINER_NAME! 2>nul | findstr /i "true" >nul
    if errorlevel 1 (
        echo     Starting existing container !CONTAINER_NAME!...
        docker start !CONTAINER_NAME! >nul
        if !ERRORLEVEL! neq 0 (
            echo     Existing container !CONTAINER_NAME! is stale. Recreating it...
            docker rm -f !CONTAINER_NAME! >nul 2>&1
            docker compose -f docker-compose.infra.yml up -d !COMPOSE_SERVICE!
            if !ERRORLEVEL! neq 0 exit /b 1
        )
    ) else (
        echo     Reusing running container !CONTAINER_NAME!...
    )
)
exit /b 0

:infra_done

echo Waiting for infrastructure to be healthy...
timeout /t 10 >nul

REM 3. Start Core Services (Eureka, Admin, Gateway)
echo [3/4] Starting Core Platform Services...

set APP_CONTAINERS=stockpro-eureka stockpro-admin stockpro-gateway stockpro-auth-service stockpro-product-service stockpro-warehouse-service stockpro-purchase-service stockpro-payment-service stockpro-alert-service stockpro-movement-service stockpro-report-service stockpro-supplier-service stockpro-frontend
for %%c in (%APP_CONTAINERS%) do (
    docker ps --format "{{.Names}}" | findstr /x "%%c" >nul
    if not errorlevel 1 (
        echo.
        echo [!] Detected running Docker app container: %%c
        echo     run-all.bat starts services locally on the same ports, so it cannot run together with the full Docker stack.
        echo     Stop the Docker app stack first with:
        echo     .\docker-start.ps1 -Stop
        pause
        exit /b
    )
)

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
