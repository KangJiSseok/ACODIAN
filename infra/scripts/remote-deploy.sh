#!/usr/bin/env sh
set -eu

require_var() {
  var_name="$1"
  eval "var_value=\${$var_name:-}"

  if [ -z "$var_value" ]; then
    echo "[remote-deploy] required variable is missing: $var_name" >&2
    exit 1
  fi
}

require_var APP_ENV
require_var DEPLOY_ROOT
require_var API_IMAGE
require_var WEB_IMAGE
require_var GHCR_USER
require_var GHCR_TOKEN

APP_DIR="${DEPLOY_ROOT%/}/${APP_ENV}"
COMPOSE_FILE="$APP_DIR/compose.deploy.yml"
ENV_FILE="$APP_DIR/.env"
PROJECT_NAME="axwms-${APP_ENV}"

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "[remote-deploy] compose file not found: $COMPOSE_FILE" >&2
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  echo "[remote-deploy] env file not found: $ENV_FILE" >&2
  exit 1
fi

echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin

export APP_ENV
export API_IMAGE
export WEB_IMAGE

echo "[remote-deploy] pre-deploy state"
docker compose \
  --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
  -p "$PROJECT_NAME" \
  ps || true

docker compose \
  --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
  -p "$PROJECT_NAME" \
  pull api web

docker compose \
  --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
  -p "$PROJECT_NAME" \
  rm -sf api web || true

docker compose \
  --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
  -p "$PROJECT_NAME" \
  up -d --remove-orphans postgres redis api web

echo "[remote-deploy] post-deploy state"
docker compose \
  --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
  -p "$PROJECT_NAME" \
  ps
