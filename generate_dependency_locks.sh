#!/bin/bash

# This script generates or deletes dependency lock files for all modules in the project.
#
# USAGE:
#   ./generate_dependency_locks.sh generate   - Generates or updates lockfiles.
#   ./generate_dependency_locks.sh clean      - Deletes all existing lockfiles.

set -e # Exit immediately if a command exits with a non-zero status.

# --- Configuration ---
# Add your module names here
modules=("app" "veview-sdk")

# --- Functions ---

# Print usage instructions
usage() {
  echo "Usage: $0 [generate|clean]"
  echo "  generate: Generates or updates dependency lock files for all modules."
  echo "  clean:    Deletes all existing dependency lock files."
  exit 1
}

# Generate lock files for all modules
generate_locks() {
  echo "Generating dependency locks (this will override existing files)..."
  for module in "${modules[@]}"; do
    echo "-> Generating locks for the :$module module..."
    ./gradlew ":$module:dependencies" --write-locks
  done
  echo "Dependency lock files generated successfully."
}

# Delete all lock files in the project
clean_locks() {
  echo "Deleting all dependency lock files..."
  # Find all files named gradle.lockfile and delete them
  find . -name "gradle.lockfile" -print -delete
  echo "All lock files have been deleted."
}

# --- Main script ---

# Check if a command argument is provided
if [ -z "$1" ]; then
  echo "Error: No command specified."
  usage
fi

# Process the command
case "$1" in
  generate)
    generate_locks
    ;;
  clean)
    clean_locks
    ;;
  *)
    echo "Error: Invalid command '$1'"
    usage
    ;;
esac
