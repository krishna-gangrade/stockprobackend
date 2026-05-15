param(
  [string]$SonarHostUrl = $(if ($env:SONAR_HOST_URL) { $env:SONAR_HOST_URL } else { 'http://localhost:9000' }),
  [string]$SonarToken = $env:SONAR_TOKEN
)

if (-not $SonarToken) {
  throw 'SONAR_TOKEN is not set. In PowerShell run: $env:SONAR_TOKEN="your_token_here"'
}

$env:SONAR_HOST_URL = $SonarHostUrl
$env:SONAR_TOKEN = $SonarToken

mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar "-Dsonar.token=$SonarToken"
