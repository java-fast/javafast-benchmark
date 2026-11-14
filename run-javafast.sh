#!/usr/bin/env bash

set -euo pipefail

version="${1:-1.0.0-SNAPSHOT}"

host="127.0.0.1"
port="8080"

jvmArgs=(
  "-Xms512m"
  "-Xmx512m"
  "-XX:+UseG1GC"
  "-XX:+AlwaysPreTouch"
  "--add-modules=jdk.incubator.vector"
#  "-XX:StartFlightRecording=filename=javafast.jfr,settings=profile,dumponexit=true"
)

systemProperties=(
  "-Dserver.host=${host}"
  "-Dserver.port=${port}"
#  "-Djavafast.selector.thread.count=10"
#  "-Djavafast.vectorized.reader=false"
#  "-Djavafast.instrumentation=false"
#  "-Djavafast.transport.buffer.block.size=256"
#  "-Djavafast.transport.buffer.block.count=8192"
#  "-Djavafast.request.pool.size=512"
)

scriptDir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$scriptDir"

./gradlew :servers:javafast:clean :servers:javafast:jar -Pversion="$version"

echo "Running JavaFast $version on:"
java --version

exec java "${jvmArgs[@]}" "${systemProperties[@]}" -jar "servers/javafast/build/libs/javafast-all.jar"
