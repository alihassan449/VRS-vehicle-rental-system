@echo off
cd /d "%~dp0"
if not exist bin mkdir bin
powershell -NoProfile -Command "javac -cp 'lib/*' -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName"
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)
java -cp "bin;lib/*" com.rental.Main %*
