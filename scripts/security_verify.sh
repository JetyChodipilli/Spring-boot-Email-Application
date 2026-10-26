#!/usr/bin/env bash
set -euo pipefail

echo '== Compile and run all tests =='
mvn -B --no-transfer-progress clean verify

echo '== Inspect managed and transitive dependencies =='
mvn -B --no-transfer-progress dependency:tree

if command -v trivy >/dev/null 2>&1; then
  echo '== Trivy dependency and filesystem vulnerability scan =='
  trivy fs --scanners vuln,secret --severity HIGH,CRITICAL --exit-code 1 .
else
  echo 'WARNING: Trivy not installed. Run a dependency/SBOM vulnerability scan in CI.'
fi

if command -v docker >/dev/null 2>&1; then
  echo '== Docker Compose configuration =='
  docker compose config --quiet
  echo '== Build and test local Mailpit stack =='
  docker compose up --build -d
  echo 'Inspect http://localhost:8025 manually before enabling real SMTP.'
else
  echo 'WARNING: Docker not installed; container tests pending.'
fi
