#!/usr/bin/env bash
# ==============================================================================
# VMS Backend Launcher Script
# Automatically exports environment variables from .env if present.
# ==============================================================================
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

if [ -f .env ]; then
  echo "Loading environment variables from .env..."
  set -a
  source .env
  set +a
fi

echo "Starting VMS Backend on port ${PORT:-8081}..."
./mvnw spring-boot:run
