#!/usr/bin/env bash
# ==============================================================================
# ER Triage Assistant - Linux / macOS Launcher Script
# ==============================================================================
set -e

echo "=============================================================================="
echo "                 ER TRIAGE ASSISTANT - LAUNCHER SCRIPT"
echo "=============================================================================="

if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed or not in PATH."
    exit 1
fi

mkdir -p java-backend/bin
echo "[1/2] Compiling Java Backend Classes..."
javac -d java-backend/bin java-backend/src/*.java
echo "[SUCCESS] Java compilation completed."

echo "[2/2] Starting ER Triage Assistant Server..."
echo "Web Dashboard available at: http://localhost:8080"
echo "Press Ctrl+C to stop the server."
echo "=============================================================================="

java -cp java-backend/bin Main
