# ==============================================================================
# StockPro Docker Startup Script
# Usage: .\docker-start.ps1 [-Build] [-Rebuild] [-Clean] [-Logs] [-Stop] [-Status]
# ==============================================================================

param(
    [switch]$Build,    # Force rebuild of all images (keeps volumes/DB data)
    [switch]$Rebuild,  # Rebuild a single service
    [string]$Service,  # Used with -Rebuild: e.g., -Rebuild -Service auth-service
    [switch]$Clean,    # Remove volumes and rebuild from scratch
    [switch]$Logs,     # Tail logs after starting
    [switch]$Stop,     # Stop and remove all containers
    [switch]$Status    # Show current container status
)

$ErrorActionPreference = "Stop"

function Write-Header {
    param([string]$Message)
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "  $Message" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
}

function Write-Step {
    param([string]$Message)
    Write-Host "`n>> $Message" -ForegroundColor Yellow
}

function Write-Success {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Fail {
    param([string]$Message)
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

# ── Check Docker is running ────────────────────────────────────────────────────
Write-Header "StockPro Docker Manager"

Write-Step "Checking Docker..."
try {
    $null = docker info 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Docker not running" }
    Write-Success "Docker is running"
} catch {
    Write-Fail "Docker is not running. Please start Docker Desktop and try again."
    exit 1
}

# ── Status mode ───────────────────────────────────────────────────────────────
if ($Status) {
    Write-Step "Container Status:"
    docker compose ps
    exit 0
}

# ── Stop mode ─────────────────────────────────────────────────────────────────
if ($Stop) {
    Write-Step "Stopping all StockPro containers..."
    docker compose down --remove-orphans
    Write-Success "All containers stopped"
    exit 0
}

# ── Clean mode ────────────────────────────────────────────────────────────────
if ($Clean) {
    Write-Step "Cleaning up containers, images, volumes, and stale networks..."
    docker compose down -v --rmi local --remove-orphans 2>$null
    docker network prune -f 2>$null | Out-Null
    Write-Success "Cleanup complete"
    $Build = $true
}

# ── Rebuild single service ────────────────────────────────────────────────────
if ($Rebuild) {
    if (-not $Service) {
        Write-Fail "Please specify a service with -Service. Example: .\docker-start.ps1 -Rebuild -Service auth-service"
        exit 1
    }
    Write-Step "Rebuilding $Service..."
    docker compose build --no-cache $Service
    if ($LASTEXITCODE -ne 0) {
        Write-Fail "Build failed for $Service"
        exit 1
    }
    Write-Step "Restarting $Service..."
    docker compose up -d --no-deps $Service
    Write-Success "$Service rebuilt and restarted"
    exit 0
}

# ── Build mode ────────────────────────────────────────────────────────────────
if ($Build) {
    Write-Step "Building all Docker images one-by-one (avoids OOM crashes)..."
    Write-Host "  Each service is built sequentially to conserve RAM" -ForegroundColor Gray

    $services = @(
        "eureka-server",
        "admin-server",
        "api-gateway",
        "auth-service",
        "product-service",
        "supplier-service",
        "alert-service",
        "warehouse-service",
        "movement-service",
        "purchase-service",
        "payment-service",
        "report-service",
        "stockpro-frontend"
    )

    $total = $services.Count
    $i = 1
    foreach ($svc in $services) {
        Write-Host "  [$i/$total] Building $svc..." -ForegroundColor Gray
        docker compose build $svc
        if ($LASTEXITCODE -ne 0) {
            Write-Fail "Build failed on: $svc. Check the output above."
            Write-Host "  Tip: .\docker-start.ps1 -Rebuild -Service $svc  (retry just this service)" -ForegroundColor Yellow
            exit 1
        }
        Write-Success "$svc built"
        $i++
    }
    Write-Success "All images built successfully"
}

# ── Remove stale Docker networks that can block startup ────────────────────────
Write-Step "Pruning stale Docker networks..."
docker network prune -f 2>$null | Out-Null
Write-Success "Networks cleaned"

# ── Stop SonarQube if running — it shares the same Docker network ─────────────
$sonarRunning = docker ps -q -f "name=stockpro-sonarqube" 2>$null
if ($sonarRunning) {
    Write-Host "  Stopping stockpro-sonarqube (shares network)..." -ForegroundColor DarkYellow
    docker stop stockpro-sonarqube | Out-Null
}

# ── Start all services ─────────────────────────────────────────────────────────
Write-Step "Starting StockPro full stack..."
Write-Host "  Infrastructure : MySQL, Redis, Zookeeper, Kafka" -ForegroundColor Gray
Write-Host "  Platform       : Eureka, Admin Server, API Gateway" -ForegroundColor Gray
Write-Host "  Microservices  : Auth, Product, Warehouse, Purchase, Payment" -ForegroundColor Gray
Write-Host "                   Alert, Movement, Report, Supplier" -ForegroundColor Gray
Write-Host "  Frontend       : Angular 17 (Nginx)" -ForegroundColor Gray
Write-Host "  Email          : External SMTP (configured via .env)" -ForegroundColor Gray

docker compose up -d --remove-orphans

if ($LASTEXITCODE -ne 0) {
    Write-Fail "Failed to start containers. Run: docker compose logs"
    exit 1
}

# ── Wait for services to stabilize ────────────────────────────────────────────
Write-Step "Waiting 45s for all services to initialize..."
Start-Sleep -Seconds 45

Write-Step "Container Status:"
docker compose ps

# ── Print all service URLs ─────────────────────────────────────────────────────
Write-Header "Service URLs"
Write-Host ""
Write-Host "  Frontend (Angular)     -> http://localhost:4200" -ForegroundColor Green
Write-Host "  API Gateway            -> http://localhost:8082" -ForegroundColor Green
Write-Host "  Eureka Dashboard       -> http://localhost:8761  (eureka / eureka-secret)" -ForegroundColor Green
Write-Host "  Admin Server           -> http://localhost:9090  (admin / admin-secret)" -ForegroundColor Green
Write-Host ""
Write-Host "  Auth Service           -> http://localhost:8083" -ForegroundColor Cyan
Write-Host "  Product Service        -> http://localhost:8085" -ForegroundColor Cyan
Write-Host "  Warehouse Service      -> http://localhost:8084" -ForegroundColor Cyan
Write-Host "  Purchase Service       -> http://localhost:8086" -ForegroundColor Cyan
Write-Host "  Payment Service        -> http://localhost:8089" -ForegroundColor Cyan
Write-Host "  Alert Service          -> http://localhost:8087" -ForegroundColor Cyan
Write-Host "  Movement Service       -> http://localhost:8088" -ForegroundColor Cyan
Write-Host "  Report Service         -> http://localhost:8091" -ForegroundColor Cyan
Write-Host "  Supplier Service       -> http://localhost:8090" -ForegroundColor Cyan
Write-Host ""
Write-Host "  MySQL                  -> localhost:3312  (root / root_pass)" -ForegroundColor Gray
Write-Host "  Redis                  -> localhost:6380  (pass: redis_pass)" -ForegroundColor Gray
Write-Host "  Kafka                  -> localhost:29092" -ForegroundColor Gray
Write-Host ""
Write-Host "  Default Admin Login    -> admin@stockpro.local / Admin@123" -ForegroundColor Magenta
Write-Host ""

if ($Logs) {
    Write-Step "Tailing logs (Ctrl+C to exit)..."
    docker compose logs -f
}

Write-Host "Quick reference:" -ForegroundColor DarkGray
Write-Host "  .\docker-start.ps1 -Status                         -> Container health status" -ForegroundColor DarkGray
Write-Host "  .\docker-start.ps1 -Logs                           -> Stream all logs" -ForegroundColor DarkGray
Write-Host "  .\docker-start.ps1 -Stop                           -> Stop all services" -ForegroundColor DarkGray
Write-Host "  .\docker-start.ps1 -Build                          -> Rebuild images + start (keeps DB data)" -ForegroundColor DarkGray
Write-Host "  .\docker-start.ps1 -Clean -Build                   -> Wipe volumes + rebuild from scratch" -ForegroundColor DarkGray
Write-Host "  .\docker-start.ps1 -Rebuild -Service auth-service  -> Rebuild one service only" -ForegroundColor DarkGray
