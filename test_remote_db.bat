@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\test_remote_db.ps1" -FriendIP "%1" -Action "%2"
endlocal
