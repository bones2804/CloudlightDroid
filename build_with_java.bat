@echo off
set "ANDROID_STUDIO_PATH=C:\Program Files\Android\Android Studio"
set "JAVA_HOME=%ANDROID_STUDIO_PATH%\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using Java from: %JAVA_HOME%

echo Building project...
call .\gradlew.bat --no-daemon assembleDebug
echo Build complete!
pause 