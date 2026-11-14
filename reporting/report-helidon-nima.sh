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
  -Pserver=helidon-nima \
  -PserverName="Helidon Nima" \
  -Psimulation="$simulation" \
  -PexecutionModel="Virtual Threads" \
  -Pthreads="10 (ForkJoinPool)" \
  -PrequestExecution="Virtual Thread per Connection" \
  -Pprotocol="HTTP/1.1" \
  -PreportDir="load-tests/gatling/build/reports/gatling/$reportDir"
