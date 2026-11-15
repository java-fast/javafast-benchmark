#!/usr/bin/env bash

set -euo pipefail

version="${1:-4.1.2}"

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

./gradlew :servers:spring-boot:clean :servers:spring-boot:bootJar -Pversion="$version"

echo "Running Spring Boot $version on:"
java --version

exec java "${jvmArgs[@]}" "${systemProperties[@]}" -jar "servers/spring-boot/build/libs/spring-boot-all.jar"
