# =====================================================================
# Subsea Pipeline - Remote Database Connection Tester & Launcher
# =====================================================================
param(
    [string]$FriendIP = "",
    [string]$Action = ""
)

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " SUBSEA PIPELINE - REMOTE DATABASE DIAGNOSTIC & CONNECTION UTILITY              " -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

if (-not $FriendIP) {
    if ([System.Console]::IsInputRedirected) {
        Write-Host "[ERROR] No IP address provided via command line. Example: .\test_remote_db.bat 192.168.1.50" -ForegroundColor Red
        exit 1
    }
    $FriendIP = Read-Host "Enter your friend's Laptop IP Address (e.g., 192.168.1.50)"
}

if (-not $FriendIP) {
    Write-Host "[ERROR] No IP address provided. Exiting." -ForegroundColor Red
    exit 1
}

Write-Host "`n[STEP 1/3] Testing TCP Port 3306 Reachability on $FriendIP..." -ForegroundColor Yellow
$tcpTest = Test-NetConnection -ComputerName $FriendIP -Port 3306 -WarningAction SilentlyContinue

if ($tcpTest.TcpTestSucceeded) {
    Write-Host "[SUCCESS] Port 3306 on $FriendIP is OPEN and REACHABLE!" -ForegroundColor Green
} else {
    Write-Host "[WARNING] Port 3306 on $FriendIP did not respond." -ForegroundColor Yellow
    Write-Host "--------------------------------------------------------------------------------"
    Write-Host "CHECKLIST FOR YOUR FRIEND'S LAPTOP:" -ForegroundColor Magenta
    Write-Host " 1. Make sure both laptops are on the SAME Wi-Fi or Mobile Hotspot."
    Write-Host " 2. Ensure MySQL Server is running on your friend's laptop."
    Write-Host " 3. Your friend must allow Port 3306 through Windows Firewall:"
    Write-Host "    New-NetFirewallRule -DisplayName 'MySQL 3306' -Direction Inbound -LocalPort 3306 -Protocol TCP -Action Allow"
    Write-Host " 4. In MySQL Workbench on your friend's laptop, execute:"
    Write-Host "    CREATE USER IF NOT EXISTS 'subsea_user'@'%' IDENTIFIED BY 'SubseaSecurePass2026!';"
    Write-Host "    GRANT ALL PRIVILEGES ON *.* TO 'subsea_user'@'%'; FLUSH PRIVILEGES;"
    Write-Host "--------------------------------------------------------------------------------"
}

Write-Host "`n[STEP 2/3] Updating config/application.properties with Friend IP ($FriendIP)..." -ForegroundColor Yellow
$configPath = "config/application.properties"
if (Test-Path $configPath) {
    $content = Get-Content $configPath
    $content = $content -replace "^db\.host=.*", "db.host=$FriendIP"
    Set-Content -Path $configPath -Value $content
    Write-Host "[OK] Saved db.host=$FriendIP into config/application.properties" -ForegroundColor Green
}

$env:DB_HOST = $FriendIP

if (-not $Action) {
    if ([System.Console]::IsInputRedirected) {
        $Action = "server"
    } else {
        Write-Host "`n[STEP 3/3] Ready to Launch!" -ForegroundColor Cyan
        Write-Host "Choose what to launch:"
        Write-Host "  1. Launch Web Dashboard Server (http://localhost:8080)"
        Write-Host "  2. Run Mission Control Ingestion Engine (Student 2 pipeline)"
        Write-Host "  3. Run Student 1 Integrity Monitoring Engine (com.subsea.MainApp)"
        Write-Host "  4. Exit (Configuration saved only)"
        $choice = Read-Host "Select option (1-4) [Default: 1]"
        if (-not $choice) { $choice = "1" }
        switch ($choice) {
            "1" { $Action = "server" }
            "2" { $Action = "mission" }
            "3" { $Action = "student1" }
            default { $Action = "none" }
        }
    }
}

switch ($Action.ToLower()) {
    "server" {
        Write-Host "`n[LAUNCH] Starting Web Dashboard Server on http://localhost:8080 connected to $FriendIP:3306..." -ForegroundColor Green
        Start-Process "http://localhost:8080"
        java -cp "bin;lib/*" "-Ddb.host=$FriendIP" com.subsea.pipeline.SubseaDashboardServer
    }
    "web" {
        Write-Host "`n[LAUNCH] Starting Web Dashboard Server on http://localhost:8080 connected to $FriendIP:3306..." -ForegroundColor Green
        Start-Process "http://localhost:8080"
        java -cp "bin;lib/*" "-Ddb.host=$FriendIP" com.subsea.pipeline.SubseaDashboardServer
    }
    "mission" {
        Write-Host "`n[LAUNCH] Running Mission Control Telemetry Ingestion against $FriendIP:3306..." -ForegroundColor Green
        java -cp "bin;lib/*" "-Ddb.host=$FriendIP" com.subsea.pipeline.MainApp
    }
    "student1" {
        Write-Host "`n[LAUNCH] Running Student 1 Monitoring Engine against $FriendIP:3306..." -ForegroundColor Green
        java -cp "bin;lib/*" "-Ddb.host=$FriendIP" com.subsea.MainApp
    }
    "test" {
        Write-Host "`n[TEST] Running Student 2 Remote DB Verification against $FriendIP:3306..." -ForegroundColor Green
        java -cp "bin;lib/*" "-Ddb.host=$FriendIP" com.subsea.pipeline.Student2_DBVerificationTest
    }
    default {
        Write-Host "`nConfiguration updated successfully with Friend IP: $FriendIP" -ForegroundColor Green
        Write-Host "You can run '.\run.bat server' to start the web dashboard anytime." -ForegroundColor Cyan
    }
}
