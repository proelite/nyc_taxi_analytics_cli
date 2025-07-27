#!/usr/bin/env bash
set -euo pipefail

# Use the Gradle wrapper if you have one; otherwise just call `gradle`
GRADLE_CMD="./gradlew"
if [ ! -x "$GRADLE_CMD" ]; then
  GRADLE_CMD="gradle"
fi

echo "🔄 Cleaning & building Java project…"
./gradlew clean build

echo "✅ Java build complete!"

echo "⏳  Running parquet download and import…"
$GRADLE_CMD downloadInsertParquetsIntoDBs

echo "✅  Done."