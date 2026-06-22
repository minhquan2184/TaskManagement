# Run the backend and ngrok concurrently
Write-Host "Starting Node.js Backend..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "npm run dev"

Write-Host "Starting Ngrok on port 3000..." -ForegroundColor Cyan
Write-Host "Please wait for the URL"
npx ngrok http --domain=ducking-vastly-blatancy.ngrok-free.dev 3000
