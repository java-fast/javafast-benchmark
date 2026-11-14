#!/usr/bin/env bash

set -euo pipefail

version="${1:-4.5.6}"

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

./gradlew :servers:helidon-nima:clean :servers:helidon-nima:jar -Pversion="$version"

echo "Running Helidon Nima $version on:"
java --version

exec java "${jvmArgs[@]}" "${systemProperties[@]}" -jar "servers/helidon-nima/build/libs/helidon-nima-all.jar"
