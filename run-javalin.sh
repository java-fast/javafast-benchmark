#!/usr/bin/env bash

set -euo pipefail

version="${1:-7.2.4}"

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

./gradlew :servers:javalin:clean :servers:javalin:jar -Pversion="$version"

echo "Running Javalin $version on:"
java --version

exec java "${jvmArgs[@]}" "${systemProperties[@]}" -jar "servers/javalin/build/libs/javalin-all.jar"
