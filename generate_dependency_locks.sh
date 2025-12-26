#!/bin/bash

# This script generates dependency lock files for all modules in the project.

set -e # Exit immediately if a command exits with a non-zero status.

modules=("app" "veview-sdk")

for module in "${modules[@]}"; do
  echo "Generating dependency locks for the :$module module..."
  ./gradlew ":$module:dependencies" --write-locks
done

echo "Dependency lock files generated successfully."
