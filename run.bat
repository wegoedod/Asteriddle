@echo off
rem Byeolitgi build & run (JDK 17+). Usage: run.bat  or  run.bat check
setlocal
cd /d "%~dp0"
set JAVAC=javac
set JAVA=java
if defined JAVA_HOME (
    set "JAVAC=%JAVA_HOME%\bin\javac"
    set "JAVA=%JAVA_HOME%\bin\java"
)
if exist out rmdir /s /q out
"%JAVAC%" -encoding UTF-8 -d out src\byeolitgi\*.java src\byeolitgi\model\*.java src\byeolitgi\data\*.java src\byeolitgi\policy\*.java src\byeolitgi\astro\*.java src\byeolitgi\game\*.java src\byeolitgi\save\*.java src\byeolitgi\ui\*.java test\byeolitgi\*.java
if errorlevel 1 goto :fail
xcopy /y /q src\byeolitgi\data\*.txt out\byeolitgi\data\ > nul
if "%1"=="check" (
    "%JAVA%" -Dstdout.encoding=UTF-8 -cp out byeolitgi.LogicCheck
) else (
    "%JAVA%" -cp out byeolitgi.Main
)
goto :eof
:fail
echo Compile failed.
pause
