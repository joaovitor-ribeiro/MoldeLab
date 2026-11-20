# Sobe o MoldeLab: backend (http://localhost:8080) e frontend (http://localhost:5173), cada um numa janela.
# Uso: clique com o botão direito > "Executar com PowerShell", ou no terminal: .\start.ps1
# Para habilitar a leitura de imagens por IA, defina antes: $env:ANTHROPIC_API_KEY = "sua-chave"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path

if (-not (Test-Path "$root\frontend\node_modules")) {
    Write-Host "Instalando dependências do frontend..."
    Push-Location "$root\frontend"; npm install; Pop-Location
}

Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root\backend'; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root\frontend'; npm run dev"

Write-Host "Aguardando o backend subir..."
for ($i = 0; $i -lt 120; $i++) {
    try { Invoke-RestMethod http://localhost:8080/actuator/health -TimeoutSec 2 | Out-Null; break } catch { Start-Sleep -Seconds 1 }
}
Start-Process "http://localhost:5173"
