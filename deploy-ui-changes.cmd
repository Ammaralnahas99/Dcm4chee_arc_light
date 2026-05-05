@echo off
echo ============================================
echo  DCM4CHEE UI Deploy Script
echo ============================================

echo.
echo [1/5] Building Angular UI...
cd /d C:\Users\USER\dcm4chee-arc-light\dcm4chee-arc-ui2
call npm run build
if %ERRORLEVEL% neq 0 (
    echo ERROR: npm build failed!
    pause
    exit /b 1
)

echo.
echo [2/6] Cleaning temp folders from previous runs...
rmdir /s /q C:\temp\war-with-schema 2>nul
rmdir /s /q C:\temp\ear-redeploy 2>nul
del /f /q C:\temp\dcm4chee-arc-ui2-5.34.3-patched.war 2>nul
del /f /q C:\temp\dcm4chee-arc-ear-5.34.3-psql-secure-new.ear 2>nul
echo Cleaned.

echo.
echo [3/6] Patching schema files into WAR...
mkdir C:\temp\war-with-schema
cd /d C:\temp\war-with-schema
"C:\Program Files\Java\jdk-21\bin\jar.exe" xf "C:\Users\USER\dcm4chee-arc-light\dcm4chee-arc-ui2\target\dcm4chee-arc-ui2-5.34.3.war"
xcopy /E /I /Y C:\temp\official-schema-extract\en\assets\schema en\assets\schema
if %ERRORLEVEL% neq 0 (
    echo ERROR: Schema copy failed! Make sure C:\temp\official-schema-extract exists.
    echo Run the schema extraction step first.
    pause
    exit /b 1
)
"C:\Program Files\Java\jdk-21\bin\jar.exe" cf C:\temp\dcm4chee-arc-ui2-5.34.3-patched.war .

echo.
echo [3/5] Rebuilding EAR with patched WAR...
rmdir /s /q C:\temp\ear-redeploy 2>nul
mkdir C:\temp\ear-redeploy
cd /d C:\temp\ear-redeploy
"C:\Program Files\Java\jdk-21\bin\jar.exe" xf C:\wildfly\wildfly-37.0.0.Final\standalone\deployments\dcm4chee-arc-ear-5.34.3-psql-secure.ear
copy /Y C:\temp\dcm4chee-arc-ui2-5.34.3-patched.war dcm4chee-arc-ui2-5.34.3.war
"C:\Program Files\Java\jdk-21\bin\jar.exe" cf C:\temp\dcm4chee-arc-ear-5.34.3-psql-secure-new.ear .

echo.
echo [4/5] Deploying to WildFly...
copy /Y C:\temp\dcm4chee-arc-ear-5.34.3-psql-secure-new.ear C:\wildfly\wildfly-37.0.0.Final\standalone\deployments\dcm4chee-arc-ear-5.34.3-psql-secure.ear
echo. > C:\wildfly\wildfly-37.0.0.Final\standalone\deployments\dcm4chee-arc-ear-5.34.3-psql-secure.ear.dodeploy

echo.
echo [5/5] Waiting for deployment...
echo Watching log - press Ctrl+C when you see "Deployed dcm4chee-arc-ear"
powershell -Command "Get-Content C:\wildfly\wildfly-37.0.0.Final\standalone\log\server.log -Tail 20 -Wait"

echo.
echo ============================================
echo  Done! Open: http://localhost:8080/dcm4chee-arc/ui2/en/
echo  Hard-refresh with Ctrl+F5 to bypass cache.
echo ============================================
pause
