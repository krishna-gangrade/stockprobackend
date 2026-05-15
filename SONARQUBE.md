# SonarQube Quick Start

This repo is wired for two SonarQube scans:

1. Backend Maven scan for all Java services
2. Frontend scan for `stockpro-frontend`

## 1. Start SonarQube locally

```powershell
docker compose -f docker-compose.sonarqube.yml up -d
```

Open `http://localhost:9000`

Sign in with:

- username: `admin`
- password: `admin`

Change the password when prompted.

## 2. Create a token

In SonarQube:

`My Account > Security > Generate Token`

Then set these variables in PowerShell:

```powershell
$env:SONAR_HOST_URL="http://localhost:9000"
$env:SONAR_TOKEN="<your-sonar-token>"
```

## 3. Run backend scan

From `stockpro-backend`:

```powershell
.\run-backend-sonar.ps1
```

If you want the raw PowerShell command, use the quoted JVM property form:

```powershell
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar "-Dsonar.token=$env:SONAR_TOKEN"
```

This publishes the backend project as:

- project key: `stockpro-backend`
- project name: `StockPro Backend`

## 4. Run frontend validation and coverage

From the repo root:

```powershell
Set-Location stockpro-frontend
npm install
npm run build
npm run test:coverage
```

## 5. Run frontend scan

From `stockpro-frontend`:

```powershell
docker run --rm `
  -e SONAR_HOST_URL="http://host.docker.internal:9000" `
  -e SONAR_TOKEN="$env:SONAR_TOKEN" `
  -v "${PWD}:/usr/src" `
  sonarsource/sonar-scanner-cli `
  -Dproject.settings=sonar-project.properties
```

This publishes the frontend project as:

- project key: `stockpro-frontend`
- project name: `StockPro Frontend`

If this workspace is not a real Git checkout and does not contain a `.git` directory, disable SCM for the frontend scan:

```properties
sonar.scm.disabled=true
```

This avoids both:

- `SCM provider autodetection failed`
- `Not inside a Git work tree`

If you later run the scan from a proper Git clone, you can remove that line and let SonarQube use SCM metadata normally.

## 6. Backend validation before scan

From `stockpro-backend`:

```powershell
mvn clean verify
```

This runs tests, JaCoCo coverage, and the backend validation gates before publishing to SonarQube.

## 7. Tonight's shortest path

If you just want it working fast:

```powershell
docker compose -f docker-compose.sonarqube.yml up -d
```

Wait for SonarQube to open on `http://localhost:9000`, create the token, then run:

```powershell
$env:SONAR_HOST_URL="http://localhost:9000"
$env:SONAR_TOKEN="<your-sonar-token>"
.\run-backend-sonar.ps1
```

Then for frontend:

```powershell
Set-Location stockpro-frontend
npm install
npm run build
npm run test:coverage
docker run --rm `
  -e SONAR_HOST_URL="http://host.docker.internal:9000" `
  -e SONAR_TOKEN="$env:SONAR_TOKEN" `
  -v "${PWD}:/usr/src" `
  sonarsource/sonar-scanner-cli `
  -Dproject.settings=sonar-project.properties
```
