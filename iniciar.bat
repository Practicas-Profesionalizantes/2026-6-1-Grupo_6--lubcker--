@echo off
rem Levanta Lubcker completo: MySQL (Docker), backend y frontend.
cd /d "%~dp0"

echo [1/3] Verificando Docker...
docker info >nul 2>&1
if errorlevel 1 (
    echo Docker no esta corriendo, abriendo Docker Desktop...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    set /a intentos=0
    goto esperar_docker
)
goto iniciar_mysql

:esperar_docker
timeout /t 5 /nobreak >nul
docker info >nul 2>&1
if not errorlevel 1 goto iniciar_mysql
set /a intentos+=1
if %intentos% geq 36 (
    echo Docker no arranco a tiempo. Abrilo a mano y volve a ejecutar este archivo.
    pause
    exit /b 1
)
goto esperar_docker

:iniciar_mysql
echo [2/3] Iniciando MySQL (contenedor mysql-lubcker)...
docker start mysql-lubcker >nul
if errorlevel 1 (
    echo No se pudo iniciar el contenedor mysql-lubcker.
    pause
    exit /b 1
)
echo Esperando a que MySQL este listo...
timeout /t 15 /nobreak >nul

echo [3/3] Iniciando backend y frontend en ventanas nuevas...
start "Lubcker - Backend" cmd /k "cd /d %~dp0ecommerce && mvnw.cmd spring-boot:run"
start "Lubcker - Frontend" cmd /k "cd /d %~dp0ProyectoLubcker-front-14 && npm start"

echo.
echo Listo. La tienda se abre sola en http://localhost:3000 cuando termine de compilar.
echo Para apagar todo, cerra las dos ventanas nuevas (y Docker si queres).
timeout /t 10 >nul
