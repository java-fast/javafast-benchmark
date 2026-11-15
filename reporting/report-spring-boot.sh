#!/usr/bin/env bash

set -euo pipefail

if [[ $# -lt 2 ]]; then
  printf 'Usage: %s <simulation> <reportDir>\n' "$0" >&2
  printf 'Error: simulation and reportDir are required.\n' >&2
  exit 1
fi

simulation="$1"
reportDir="$2"

scriptDir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$scriptDir/../"

./gradlew benchmarkReport \
  -Pserver=spring-boot \
  -PserverName="Spring Boot" \
  -Psimulation="$simulation" \
  -PexecutionModel="Blocking" \
  -Pthreads="200 (Max threads)" \
  -PrequestExecution="Tomcat Threads" \
  -Pprotocol="HTTP/1.1" \
  -PreportDir="load-tests/gatling/build/reports/gatling/$reportDir"
