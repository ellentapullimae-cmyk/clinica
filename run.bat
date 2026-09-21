@echo off
rem =====================================================
rem Sistema de Gestion para una Clinica
rem Compila el proyecto y lo ejecuta (JDK 17+)
rem Si javac no esta en el PATH, busca un JDK en las
rem ubicaciones habituales y lo usa para esta sesion.
rem =====================================================
setlocal

set RAIZ=%~dp0
cd /d "%RAIZ%"

if not exist bin mkdir bin
if not exist lib\openpdf-1.3.30.jar (
    echo [ERROR] No se encontro la libreria OpenPDF en lib\openpdf-1.3.30.jar
    pause
    exit /b 1
)

call :detectar_jdk
if errorlevel 1 exit /b 1

echo Compilando el proyecto...
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "bin;lib\*" -d bin @sources.txt
if errorlevel 1 (
    del sources.txt
    echo [ERROR] Fallo la compilacion. Verifique que Java JDK este instalado y en el PATH.
    pause
    exit /b 1
)
del sources.txt

rem Copiar la configuracion centralizada al classpath de salida
copy /y db.properties bin\ >nul 2>nul

echo Ejecutando el sistema...
java -Dfile.encoding=UTF-8 -cp "bin;lib\*" clinica.Main

endlocal
pause
exit /b 0

rem =====================================================
rem Detecta un JDK (javac) si no esta en el PATH.
rem Devuelve errorlevel 0 si hay javac, 1 si no.
rem =====================================================
:detectar_jdk
where javac >nul 2>nul
if not errorlevel 1 exit /b 0
echo javac no esta en el PATH. Buscando un JDK instalado...

for %%D in (
    "%RAIZ%jdk"
    "%USERPROFILE%\java\jdk-17.0.20.1+1\bin"
    "C:\Program Files\Eclipse Adoptium"
    "C:\Program Files\Java"
    "C:\Program Files\Microsoft"
    "C:\Program Files\Amazon Corretto"
) do (
    if exist "%%~D\bin\javac.exe" (
        set "JDK_BIN=%%~D\bin"
        goto :jdk_encontrado
    )
)

for %%B in (
    "%RAIZ%jdk"
    "%USERPROFILE%\java"
    "C:\Program Files\Eclipse Adoptium"
    "C:\Program Files\Java"
    "C:\Program Files\Microsoft"
    "C:\Program Files\Amazon Corretto"
) do (
    if exist "%%~B" (
        for /d %%D in ("%%~B\*") do (
            if exist "%%~D\bin\javac.exe" (
                set "JDK_BIN=%%~D\bin"
                goto :jdk_encontrado
            )
        )
    )
)

echo [ERROR] No se encontro ningun JDK 17+ en el sistema.
echo Agregue un JDK al PATH o coloquelo en una carpeta jdk\ del proyecto,
echo o en %USERPROFILE%\java\ y vuelva a ejecutar run.bat.
exit /b 1

:jdk_encontrado
set "JAVA_HOME=%JDK_BIN%\.."
set "PATH=%JDK_BIN%;%PATH%"
echo JDK encontrado en %JDK_BIN% (agregado al PATH para esta sesion).
exit /b 0