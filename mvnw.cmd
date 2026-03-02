@ECHO OFF
REM ----------------------------------------------------------------------------
REM Maven Wrapper Startup Script for Windows
REM
REM This script allows running Maven without requiring a pre-installed
REM Maven distribution. It will download the specified Maven version and
REM the Maven Wrapper JAR if necessary, then delegate to the wrapper.
REM ----------------------------------------------------------------------------

SETLOCAL ENABLEEXTENSIONS

SET "MVNW_VERBOSE=%MVNW_VERBOSE%"
IF "%MVNW_VERBOSE%"=="" SET "MVNW_VERBOSE=false"

SET "WRAPPER_DIR=%~dp0"
SET "WRAPPER_DIR=%WRAPPER_DIR:~0,-1%"

IF "%JAVA_HOME%"=="" (
  SET "JAVA_EXE=java"
) ELSE (
  SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

IF "%MVNW_VERBOSE%"=="true" (
  ECHO MVNW: Wrapper directory: %WRAPPER_DIR%
  ECHO MVNW: Java executable: %JAVA_EXE%
)

SET "WRAPPER_JAR=%WRAPPER_DIR%\.mvn\wrapper\maven-wrapper.jar"
SET "WRAPPER_PROPERTIES=%WRAPPER_DIR%\.mvn\wrapper\maven-wrapper.properties"
SET "BASE_DIR=%WRAPPER_DIR%"

IF NOT EXIST "%WRAPPER_PROPERTIES%" (
  ECHO MVNW: Cannot find "%WRAPPER_PROPERTIES%" 1>&2
  EXIT /B 1
)

IF "%MVNW_VERBOSE%"=="true" (
  ECHO MVNW: Using wrapper properties file: %WRAPPER_PROPERTIES%
)

IF "%MAVEN_USER_HOME%"=="" (
  SET "MAVEN_USER_HOME=%USERPROFILE%\.m2"
)

REM
REM Load configuration from maven-wrapper.properties
REM
SET "WRAPPER_URL="
SET "DISTRIBUTION_URL="

FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%WRAPPER_PROPERTIES%") DO (
  IF "%%A"=="wrapperUrl" SET "WRAPPER_URL=%%B"
  IF "%%A"=="distributionUrl" SET "DISTRIBUTION_URL=%%B"
)

IF "%WRAPPER_URL%"=="" (
  ECHO MVNW: wrapperUrl is not set in "%WRAPPER_PROPERTIES%" 1>&2
  EXIT /B 1
)

IF "%DISTRIBUTION_URL%"=="" (
  ECHO MVNW: distributionUrl is not set in "%WRAPPER_PROPERTIES%" 1>&2
  EXIT /B 1
)

REM
REM Download the Maven Wrapper JAR if needed
REM
IF NOT EXIST "%WRAPPER_JAR%" (
  IF "%MVNW_VERBOSE%"=="true" (
    ECHO MVNW: Wrapper JAR not found at "%WRAPPER_JAR%"
    ECHO MVNW: Downloading Maven Wrapper from: %WRAPPER_URL%
  )

  IF NOT EXIST "%WRAPPER_DIR%\.mvn\wrapper" (
    MKDIR "%WRAPPER_DIR%\.mvn\wrapper" 2>NUL
  )

  SET DOWNLOAD_CMD=

  WHERE /Q curl
  IF NOT ERRORLEVEL 1 (
    SET "DOWNLOAD_CMD=curl -fsSL %WRAPPER_URL% -o \"%WRAPPER_JAR%\""
  ) ELSE (
    WHERE /Q wget
    IF NOT ERRORLEVEL 1 (
      SET "DOWNLOAD_CMD=wget -q %WRAPPER_URL% -O \"%WRAPPER_JAR%\""
    )
  )

  IF "%DOWNLOAD_CMD%"=="" (
    ECHO MVNW: Could not find curl or wget to download Maven Wrapper 1>&2
    EXIT /B 1
  )

  IF "%MVNW_VERBOSE%"=="true" ECHO MVNW: Using download command: %DOWNLOAD_CMD%

  CALL %DOWNLOAD_CMD%

  IF ERRORLEVEL 1 (
    ECHO MVNW: Failed to download Maven Wrapper from %WRAPPER_URL% 1>&2
    EXIT /B 1
  )

  IF "%MVNW_VERBOSE%"=="true" ECHO MVNW: Maven Wrapper downloaded successfully.
)

SET "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

SET "JAVA_ARGS=%JAVA_TOOL_OPTIONS% %MAVEN_OPTS%"

IF "%MVNW_VERBOSE%"=="true" (
  ECHO MVNW: Launching Maven Wrapper
  ECHO MVNW:   JAVA_EXE: %JAVA_EXE%
  ECHO MVNW:   JAVA_ARGS: %JAVA_ARGS%
  ECHO MVNW:   WRAPPER_JAR: %WRAPPER_JAR%
  ECHO MVNW:   BASE_DIR: %BASE_DIR%
)

"%JAVA_EXE%" %JAVA_ARGS% ^
  "-Dmaven.multiModuleProjectDirectory=%BASE_DIR%" ^
  "-Dmaven.home=%MAVEN_USER_HOME%" ^
  -cp "%WRAPPER_JAR%" ^
  %WRAPPER_LAUNCHER% %*

ENDLOCAL

