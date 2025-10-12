#!/bin/bash

# JetProxy Unified Development Runner
# Usage: ./run.sh [--config=path/to/config.yaml] [--dev] [--dashboard-only | --proxy-only]

# --- Default values ---
CONFIG_ARG="--config=demo/config.yaml" # Default config path
DEV_MODE=false
DASHBOARD_ONLY=false
PROXY_ONLY=false

# --- Argument Parsing ---
for arg in "$@"
do
  case $arg in
    --config=*) # Handles --config=path/to/file.yaml
      CONFIG_ARG="$arg"
      shift
      ;;
    --dev)
      DEV_MODE=true
      shift
      ;;
    --dashboard-only)
      DASHBOARD_ONLY=true
      shift
      ;;
    --proxy-only)
      PROXY_ONLY=true
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
if [ "$DASHBOARD_ONLY" = true ]; then
  # --- Dashboard Only Mode ---
  echo "[RUNNER] Starting dashboard ONLY (dev mode)..."
  cd frontend && npm install && npm run dev

elif [ "$PROXY_ONLY" = true ]; then
  # --- Proxy Only Mode ---
  echo "[RUNNER] Starting proxy ONLY..."
  if [ "$DEV_MODE" = true ]; then
    echo "[RUNNER] Proxy starting in dev mode (won't serve static files)."
    ./gradlew run --args="$CONFIG_ARG --dev"
  else
    echo "[RUNNER] Proxy starting in prod mode (will serve static files if built)."
    ./gradlew run --args="$CONFIG_ARG"
  fi

else
  # --- Dual Runner Mode (Default) ---
  if [ "$DEV_MODE" = true ]; then
    # --- Development Mode ---
    echo "[RUNNER] Starting in DEV mode (dashboard + proxy)..."

    echo "[RUNNER] Starting dashboard (Vite) in background..."
    (cd frontend && npm install && npm run dev) &

    echo "[RUNNER] Starting proxy (JetProxy)..."
    ./gradlew run --args="$CONFIG_ARG --dev"

  else
    # --- Production Mode ---
    echo "[RUNNER] Starting in PROD mode (dashboard build + proxy)..."

    echo "[RUNNER] Building dashboard for production..."
    (cd frontend && npm install && npm run build)

    echo "[RUNNER] Starting proxy (JetProxy) to serve static files..."
    ./gradlew run --args="$CONFIG_ARG"
  fi
fi

echo "[RUNNER] All services have been shut down."