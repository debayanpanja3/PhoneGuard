#!/bin/sh
# Standard Gradle wrapper launcher.
DIR="$(cd "$(dirname "$0")" && pwd)"
CLASSPATH="$DIR/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$CLASSPATH" ]; then
  echo "gradle-wrapper.jar not found — open this project in Android Studio once (with internet access) and it will be generated automatically, or run: gradle wrapper"
  exit 1
fi
exec java -Dorg.gradle.appname="gradlew" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
