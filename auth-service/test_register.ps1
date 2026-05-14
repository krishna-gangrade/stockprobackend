$body = @{
    fullName = "Test User"
    email = "test@example.com"
    password = "Password123!"
    phone = "1234567890"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8083/api/v1/auth/register" -Method Post -Body $body -ContentType "application/json"
    $response | ConvertTo-Json
} catch {
    $_.Exception.Message
    $_.ErrorDetails.Message
}
