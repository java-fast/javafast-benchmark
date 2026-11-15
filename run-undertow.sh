#!/usr/bin/env bash

set -euo pipefail

version="${1:-2.4.4.Final}"

host="127.0.0.1"
port="8080"

jvmArgs=(
  "-Xms512m"
  "-Xmx512m"
  "-XX:+UseG1GC"
  "-XX:+AlwaysPreTouch"
)

systemProperties=(
  "-Dserver.host=${host}"
  "-Dserver.port=${port}"
)

scriptDir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$scriptDir"

./gradlew :servers:undertow:clean :servers:undertow:jar -Pversion="$version"

echo "Running Undertow $version on:"
java --version

exec java "${jvmArgs[@]}" "${systemProperties[@]}" -jar "servers/undertow/build/libs/undertow-all.jar"
