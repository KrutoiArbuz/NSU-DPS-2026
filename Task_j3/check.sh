#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"
./gradlew --no-daemon clean test installDist "$@"
