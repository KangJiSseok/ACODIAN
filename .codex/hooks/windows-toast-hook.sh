#!/usr/bin/env bash
set -euo pipefail

POWERSHELL_EXE="/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT_WSL="$SCRIPT_DIR/windows-notify.ps1"

cat >/dev/null || true

script_win="$(wslpath -w "$SCRIPT_WSL")"

"$POWERSHELL_EXE" -NoProfile -WindowStyle Hidden -Command \
  "Start-Process PowerShell -WindowStyle Hidden -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File','$script_win')" \
  >/dev/null 2>&1 || exit 1
