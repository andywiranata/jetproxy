#!/bin/bash

# JetProxy Unified Development Runner
# Usage: ./run.sh [--config=path/to/config.yaml] [--dev]

# --- Default values ---
CONFIG_ARG="--config=demo/config.yaml" # Default config path
DEV_MODE=false

# --- Argument Parsing ---
for arg in "$@"
do
  case $arg in
    --config=*) # Handles --config=path/to/file.yaml
      CONFIG_ARG="$arg"
      shift
      ;;
    --dev) # Handles --dev flag
      DEV_MODE=true
      shift
      ;;
  esac
done

echo "[RUNNER] Configuration: $CONFIG_ARG"
echo "[RUNNER] Dev Mode: $DEV_MODE"

# --- Cleanup Function ---
cleanup() {
    echo "[RUNNER] Shutting down services..."
    kill 0 # Kills all processes in the script's process group
}
trap cleanup EXIT

# --- Main Logic ---
if [ "$DEV_MODE" = true ]; then
  # --- Development Mode ---
  echo "[RUNNER] Starting in DEV mode..."

  echo "[RUNNER] Starting frontend (Vite) in background..."
  (cd frontend && npm install && npm run dev) &

  echo "[RUNNER] Starting backend (JetProxy)..."
  ./gradlew run --args="$CONFIG_ARG --dev"

else
  # --- Production Mode ---
  echo "[RUNNER] Starting in PROD mode..."

  echo "[RUNNER] Building frontend for production..."
  (cd frontend && npm install && npm run build)

  echo "[RUNNER] Starting backend (JetProxy) to serve static files..."
  ./gradlew run --args="$CONFIG_ARG"

fi

echo "[RUNNER] All services have been shut down."
