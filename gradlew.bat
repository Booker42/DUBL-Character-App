@echo off
setlocal
set VERSION=9.6.0
if "%GRADLE_USER_HOME%"=="" (
  set BASE=%USERPROFILE%\.gradle\dubl-bootstrap
) else (
  set BASE=%GRADLE_USER_HOME%\dubl-bootstrap
)
set HOME_DIR=%BASE%\gradle-%VERSION%
set ZIP=%BASE%\gradle-%VERSION%-bin.zip
set URL=https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip

if not exist "%HOME_DIR%\bin\gradle.bat" (
  if not exist "%BASE%" mkdir "%BASE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%URL%' -OutFile '%ZIP%'; Expand-Archive -Path '%ZIP%' -DestinationPath '%BASE%' -Force"
  if errorlevel 1 exit /b 1
)
call "%HOME_DIR%\bin\gradle.bat" %*
