$body = @{
    fullName = "Gateway Test User"
    email = "gateway_test@example.com"
    password = "Password123!"
    phone = "0987654321"
} | ConvertTo-Json

try {
    Write-Host "Testing through API Gateway (Port 8082)..."
    $response = Invoke-RestMethod -Uri "http://localhost:8082/api/v1/auth/register" -Method Post -Body $body -ContentType "application/json"
    $response | ConvertTo-Json
} catch {
    Write-Host "Error occurred:"
    $_.Exception.Message
    if ($_.ErrorDetails) { $_.ErrorDetails.Message }
}
