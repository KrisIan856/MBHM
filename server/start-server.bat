@echo off
setlocal
title MBHM Sync Server
cd /d "%~dp0"

netstat -ano | findstr /R /C:":3000 .*LISTENING" >nul
if not errorlevel 1 (
    echo.
    echo MBHM sync server is already running on port 3000.
    echo.
    echo Dashboard: http://127.0.0.1:3000/
    echo Username : mbhm
    echo Password : open server\.sync-token in Notepad
    echo.
    echo Do not start a second copy. Use the running server.
    echo To stop it, run: Get-NetTCPConnection -LocalPort 3000 -State Listen ^| Stop-Process
    echo.
    pause
    exit /b 0
)

echo Starting MBHM sync server...
echo Dashboard: http://127.0.0.1:3000/
echo Username : mbhm
echo Password : open server\.sync-token in Notepad
echo.
node server.js
if errorlevel 1 (
    echo.
    echo Server exited with an error. Review the output above.
)
pause
endlocal
