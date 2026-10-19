@echo off
setlocal enableextensions

REM -----------------------------------------------------------------
REM Publicacao na Hetzner via Docker Hub + Kamal (Windows .bat)
REM Uso:
REM   deploy-kamal.bat setup   -> primeira publicacao: prepara o servidor, sobe Keycloak + API e o front
REM   deploy-kamal.bat         -> nova versao da API e do front (padrao)
REM   deploy-kamal.bat api     -> nova versao apenas da API
REM   deploy-kamal.bat front   -> nova versao apenas do front
REM Producao sai da master: os dois repositorios precisam estar sem alteracoes pendentes.
REM -----------------------------------------------------------------

set "ROOT=%~dp0"
cd /d "%ROOT%"

set "MODO=%~1"
if "%MODO%"=="" set "MODO=deploy"
if /i not "%MODO%"=="setup" if /i not "%MODO%"=="deploy" if /i not "%MODO%"=="api" if /i not "%MODO%"=="front" (
  echo ERRO: modo invalido "%MODO%". Use: setup, deploy, api ou front.
  exit /b 1
)

set "FRONT_DIR=%ROOT%..\task-frontend"
set "API_IMAGEM=thiagojfb/task-backend"

echo [1/6] Validando pre-requisitos...
if not exist "%ROOT%config\deploy.yml" (
  echo ERRO: config\deploy.yml nao encontrado.
  exit /b 1
)
if not exist "%ROOT%.kamal\secrets" (
  echo ERRO: .kamal\secrets nao encontrado. Copie .kamal\secrets.example e preencha.
  exit /b 1
)
findstr /r /c:"^KAMAL_REGISTRY_PASSWORD=..*" "%ROOT%.kamal\secrets" >nul || (
  echo ERRO: preencha KAMAL_REGISTRY_PASSWORD em .kamal\secrets com o token do Docker Hub.
  exit /b 1
)
findstr /c:"SERVIDOR_IP" "%ROOT%config\deploy.yml" >nul && (
  echo ERRO: defina o IP do servidor em config\deploy.yml no lugar de SERVIDOR_IP.
  exit /b 1
)
if not exist "%FRONT_DIR%\Dockerfile" (
  echo ERRO: Dockerfile do front nao encontrado em %FRONT_DIR%.
  exit /b 1
)
where docker >nul 2>nul || (
  echo ERRO: docker nao encontrado no PATH.
  exit /b 1
)
where kamal >nul 2>nul || (
  echo ERRO: kamal nao encontrado no PATH. Instale com: gem install kamal
  exit /b 1
)

echo [2/6] Conferindo os repositorios...
call :conferir_repositorio "%ROOT%." || exit /b 1
call :conferir_repositorio "%FRONT_DIR%" || exit /b 1
for /f %%v in ('git -C "%ROOT%." rev-parse --short HEAD') do set "API_VERSAO=%%v"
for /f %%v in ('git -C "%FRONT_DIR%" rev-parse --short HEAD') do set "FRONT_VERSAO=%%v"
echo       API: %API_VERSAO%   Front: %FRONT_VERSAO%

echo [3/6] Login no Docker Hub com o token do .kamal\secrets...
call kamal registry login --skip-remote || (
  echo ERRO: falha no login do Docker Hub.
  exit /b 1
)

if /i "%MODO%"=="front" goto publicar
echo [4/6] Gerando e enviando a imagem da API: %API_IMAGEM%:%API_VERSAO%
docker build --platform linux/amd64 -t %API_IMAGEM%:%API_VERSAO% -t %API_IMAGEM%:latest "%ROOT%." || (
  echo ERRO: falha no build da API.
  exit /b 1
)
docker push %API_IMAGEM%:%API_VERSAO% || (
  echo ERRO: falha no push da API.
  exit /b 1
)
docker push %API_IMAGEM%:latest || (
  echo ERRO: falha no push da API.
  exit /b 1
)

:publicar
echo [5/6] Publicando com o Kamal (modo %MODO%)...
REM API_VERSAO e lida pelo config\deploy.yml (tag da imagem do accessory "api")
if /i "%MODO%"=="setup" (
  REM Instala Docker e kamal-proxy no servidor, sobe Keycloak + API e publica o front (gerado pelo Kamal)
  call kamal setup --version %FRONT_VERSAO% || goto falha_kamal
)
if /i "%MODO%"=="deploy" (
  call kamal accessory reboot api || goto falha_kamal
  call kamal deploy --version %FRONT_VERSAO% || goto falha_kamal
)
if /i "%MODO%"=="api" (
  call kamal accessory reboot api || goto falha_kamal
)
if /i "%MODO%"=="front" (
  call kamal deploy --version %FRONT_VERSAO% || goto falha_kamal
)

echo [6/6] Situacao final...
call kamal details

echo.
echo Publicacao finalizada.
endlocal
exit /b 0

:falha_kamal
echo ERRO: o Kamal falhou no modo %MODO%. Veja a saida acima (logs: kamal app logs / kamal accessory logs api).
exit /b 1

:conferir_repositorio
set "PENDENTES="
for /f %%s in ('git -C "%~1" status --porcelain') do set "PENDENTES=1"
if defined PENDENTES (
  echo ERRO: ha alteracoes sem commit em %~1. Producao so recebe codigo commitado.
  exit /b 1
)
for /f %%b in ('git -C "%~1" branch --show-current') do (
  if /i not "%%b"=="master" echo       AVISO: %~1 esta na branch %%b ^(producao deveria sair da master^).
)
exit /b 0
