@echo off
setlocal enabledelayedexpansion

echo Starting MuMu Player (index 0 and 1)...

rem start two MuMu instances (0 and 1)
D:\game\MuMuPlayer\nx_main\MuMuManager.exe api -v 0 launch_player
D:\game\MuMuPlayer\nx_main\MuMuManager.exe api -v 1 launch_player

echo.
echo Waiting for Android to boot up...
ping 127.0.0.1 -n 6 >nul

set "ADB=D:\AppData\Sdk\Android\platform-tools\adb.exe"
set TIMEOUT_COUNT=0
set "SCAN_START=16384"
set "SCAN_END=16432"
set "EXTRA_PORTS=5555 7555 21503 62001 62025"
set "MAX_SECONDS=30"
set "SLEEP_SECONDS=2"
set "ELAPSED_SECONDS=0"

if not exist "%ADB%" (
    echo ERROR: adb not found at "%ADB%"
    pause
    exit /b 1
)

rem restart adb server once, avoid stale state
%ADB% kill-server >nul 2>&1
%ADB% start-server >nul 2>&1

:CHECK_LOOP
call :COUNT_DEVICES
if !DEVICE_COUNT! geq 2 (
    goto SUCCESS
)

rem --- scan high-probability ports first, stop immediately when 2 devices are ready ---
for %%P in (16384 16385 !EXTRA_PORTS!) do (
    call :TRY_CONNECT %%P
    if !DEVICE_COUNT! geq 2 (
        goto SUCCESS
)

echo Detected MuMu devices: !DEVICE_COUNT!
rem --- then scan full range, stop immediately when 2 devices are ready ---
echo Scanning ports !SCAN_START!-!SCAN_END! ...
for /L %%P in (!SCAN_START!,1,!SCAN_END!) do (
    call :TRY_CONNECT %%P
    if !DEVICE_COUNT! geq 2 (
        goto SUCCESS
    )
)

call :COUNT_DEVICES

if !DEVICE_COUNT! geq 2 (
    goto SUCCESS
)

set /a TIMEOUT_COUNT+=1
if %TIMEOUT_COUNT% geq 20 (
set /a ELAPSED_SECONDS+=SLEEP_SECONDS
if !ELAPSED_SECONDS! geq !MAX_SECONDS! (
    echo Connection timed out.
    echo Connection timed out after !MAX_SECONDS! seconds.
    echo Check the actual adb ports in MuMu "Problem Diagnosis" if this keeps failing.
    pause
)

ping 127.0.0.1 -n 4 >nul
ping 127.0.0.1 -n 3 >nul


:SUCCESS
echo.
echo ========================================
echo   CONNECTED TO MuMu INSTANCES
echo ========================================
%ADB% devices
echo.
echo You can now run your app in Android Studio on multiple MuMu targets.
pause
endlocal

:TRY_CONNECT
set "TRY_PORT=%~1"
%ADB% connect 127.0.0.1:!TRY_PORT! >nul 2>&1
call :COUNT_DEVICES
exit /b 0

:COUNT_DEVICES
set DEVICE_COUNT=0
for /f "tokens=1,2" %%A in ('"%ADB%" devices') do (
    set DEV=%%A
    set STATE=%%B
    if /I "!STATE!"=="device" (
        if /I "!DEV:~0,10!"=="127.0.0.1" (
            set /a DEVICE_COUNT+=1
        )
    )
)
exit /b 0
