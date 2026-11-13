#!/usr/bin/env bash

set -euo pipefail

version="${1:-5.2.2}"

host="127.0.0.1"
port="8080"
threads="8"

jvmArgs=(
  "-Xms512m"
  "-Xmx512m"
  "-XX:+UseG1GC"
#  "-XX:StartFlightRecording=filename=vertx.jfr,settings=profile,dumponexit=true"
)

systemProperties=(
  "-Dserver.host=${host}"
  "-Dserver.port=${port}"
  "-Dvertx.eventloop.threads=${threads}"
)

scriptDir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$scriptDir"

./gradlew :servers:vertx:clean :servers:vertx:jar -Pversion="$version"

echo "Running Vert.x $version on:"
java --version

exec java "${jvmArgs[@]}" "${systemProperties[@]}" -jar "servers/vertx/build/libs/vertx-all.jar"
