@echo off
set TITLE=StockPro Microservices Builder
title %TITLE%
color 0B

echo.
echo  ==========================================================
echo            BUILDING STOCKPRO MICROSERVICES
echo  ==========================================================
echo.

call mvnw.cmd clean install -DskipTests

echo.
echo ==========================================================
echo  BUILD COMPLETE
echo ==========================================================
echo.
pause
