# StockPro Backend

This folder contains the full backend workspace for StockPro: the Spring Boot microservices, shared Maven parent, Docker Compose files, environment files, and backend helper scripts.

## Services

- `eureka-server`
- `admin-server`
- `api-gateway`
- `auth-service`
- `product-service`
- `warehouse-service`
- `purchase-service`
- `payment-service`
- `alert-service`
- `movement-service`
- `report-service`
- `supplier-service`

## Key Files

- `pom.xml`: parent Maven aggregator for all backend services
- `.env`: backend environment variables used by Docker and helper scripts
- `docker-compose.infra.yml`: infrastructure-only stack
- `docker-compose.all.yml`: full stack including the frontend sibling app
- `docker-start.ps1`: full Docker lifecycle helper
- `run-all.bat`: local microservice startup helper
- `build-all.bat`: backend Maven build helper

## Run Backend Only

From this folder:

```powershell
.\mvnw.cmd clean install
.\run-all.bat
```

## Run Backend With Docker

From this folder:

```powershell
.\docker-start.ps1
```

To stop everything:

```powershell
.\docker-start.ps1 -Stop
```

## Run A Single Service Test

From this folder:

```powershell
.\mvnw.cmd -pl auth-service test
```

## Notes

- Dockerfiles still build correctly because the backend services, parent `pom.xml`, wrapper files, and compose files were moved together into the same backend build context.
- The frontend remains a sibling app at `../stockpro-frontend`, and the full-stack compose file points there explicitly.
