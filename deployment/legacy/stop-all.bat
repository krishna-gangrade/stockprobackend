@echo off
set TITLE=StockPro Microservices Stopper
title %TITLE%
color 0C

echo.
echo  ==========================================================
echo            STOPPING STOCKPRO MICROSERVICES
echo  ==========================================================
echo.

echo [1/3] Killing Java processes...
:: This kills all java processes started by the .bat file
taskkill /F /IM java.exe /T 2>nul

echo.
echo [2/3] Stopping Docker Infrastructure...
:: Using the infra-specific compose file
docker compose -f docker-compose.infra.yml stop

echo.
echo [3/3] Cleaning up potential stale containers...
:: If they were using the full docker-compose.yml before, we should stop those too
docker compose down 2>nul

echo.
echo ==========================================================
echo  ALL SERVICES STOPPED
echo ==========================================================
echo.
echo  Note: If Docker Desktop still shows containers running, 
echo  try clicking the "Trash" icon in Docker Desktop to 
echo  clear any stuck states.
echo.
pause
