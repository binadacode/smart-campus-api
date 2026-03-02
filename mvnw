#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper Startup Script
#
# This script allows running Maven without requiring a pre-installed
# Maven distribution. It will download the specified Maven version and
# the Maven Wrapper JAR if necessary, then delegate to the wrapper.
# ----------------------------------------------------------------------------

set -e

WRAPPER_DIR="$(cd "$(dirname "$0")" && pwd)"
MVNW_VERBOSE="${MVNW_VERBOSE:-false}"

if [ -z "$MAVEN_OPTS" ]; then
  MAVEN_OPTS=""
fi

if [ -z "$JAVA_HOME" ]; then
  JAVA_EXEC="java"
else
  JAVA_EXEC="$JAVA_HOME/bin/java"
fi

if [ "$MVNW_VERBOSE" = true ]; then
  echo "MVNW: Wrapper directory: $WRAPPER_DIR"
  echo "MVNW: Java executable: $JAVA_EXEC"
fi

WRAPPER_JAR="$WRAPPER_DIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$WRAPPER_DIR/.mvn/wrapper/maven-wrapper.properties"
BASE_DIR="$WRAPPER_DIR"

if [ ! -f "$WRAPPER_PROPERTIES" ]; then
  echo "MVNW: Cannot find $WRAPPER_PROPERTIES" >&2
  exit 1
fi

if [ "$MVNW_VERBOSE" = true ]; then
  echo "MVNW: Using wrapper properties file: $WRAPPER_PROPERTIES"
fi

MAVEN_USER_HOME="${MAVEN_USER_HOME:-$HOME/.m2}"

#
# Load configuration from maven-wrapper.properties
#
WRAPPER_URL=""
DISTRIBUTION_URL=""

while IFS='=' read -r key value; do
  case "$key" in
    wrapperUrl) WRAPPER_URL="$value" ;;
    distributionUrl) DISTRIBUTION_URL="$value" ;;
  esac
done < "$WRAPPER_PROPERTIES"

if [ -z "$WRAPPER_URL" ]; then
  echo "MVNW: wrapperUrl is not set in $WRAPPER_PROPERTIES" >&2
  exit 1
fi

if [ -z "$DISTRIBUTION_URL" ]; then
  echo "MVNW: distributionUrl is not set in $WRAPPER_PROPERTIES" >&2
  exit 1
fi

#
# Download the Maven Wrapper JAR if needed
#
if [ ! -f "$WRAPPER_JAR" ]; then
  if [ "$MVNW_VERBOSE" = true ]; then
    echo "MVNW: Wrapper JAR not found at $WRAPPER_JAR"
    echo "MVNW: Downloading Maven Wrapper from: $WRAPPER_URL"
  fi

  mkdir -p "$(dirname "$WRAPPER_JAR")"

  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
  else
    echo "MVNW: Could not find curl or wget to download Maven Wrapper" >&2
    exit 1
  fi

  if [ "$MVNW_VERBOSE" = true ]; then
    echo "MVNW: Maven Wrapper downloaded successfully."
  fi
fi

#
# Prepare JVM arguments
#
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

JAVA_ARGS=""
if [ -n "$JAVA_TOOL_OPTIONS" ]; then
  JAVA_ARGS="$JAVA_TOOL_OPTIONS"
fi

if [ -n "$MAVEN_OPTS" ]; then
  JAVA_ARGS="$JAVA_ARGS $MAVEN_OPTS"
fi

if [ "$MVNW_VERBOSE" = true ]; then
  echo "MVNW: Launching Maven Wrapper"
  echo "MVNW:   JAVA_EXEC: $JAVA_EXEC"
  echo "MVNW:   JAVA_ARGS: $JAVA_ARGS"
  echo "MVNW:   WRAPPER_JAR: $WRAPPER_JAR"
  echo "MVNW:   BASE_DIR: $BASE_DIR"
fi

exec "$JAVA_EXEC" \
  $JAVA_ARGS \
  -Dmaven.multiModuleProjectDirectory="$BASE_DIR" \
  -Dmaven.home="$MAVEN_USER_HOME" \
  -cp "$WRAPPER_JAR" \
  "$WRAPPER_LAUNCHER" "$@"

