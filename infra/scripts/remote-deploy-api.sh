#!/usr/bin/env sh
set -eu

require_var() {
  var_name="$1"
  eval "var_value=\${$var_name:-}"

  if [ -z "$var_value" ]; then
    echo "[remote-deploy-api] required variable is missing: $var_name" >&2
    exit 1
  fi
}

require_var APP_ENV
require_var DEPLOY_ROOT
require_var API_IMAGE
require_var CI_REGISTRY
require_var CI_REGISTRY_USER
require_var CI_REGISTRY_PASSWORD

APP_DIR="${DEPLOY_ROOT%/}/${APP_ENV}"
COMPOSE_FILE="$APP_DIR/compose.deploy.yml"
ENV_FILE="$APP_DIR/.env"
PROJECT_NAME="axwms-${APP_ENV}"

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "[remote-deploy-api] compose file not found: $COMPOSE_FILE" >&2
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  echo "[remote-deploy-api] env file not found: $ENV_FILE" >&2
  exit 1
fi

echo "$CI_REGISTRY_PASSWORD" | docker login "$CI_REGISTRY" -u "$CI_REGISTRY_USER" --password-stdin

export APP_ENV
export API_IMAGE

docker compose \
  --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
  -p "$PROJECT_NAME" \
  pull api

docker compose \
  --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
  -p "$PROJECT_NAME" \
  up -d postgres redis api

docker compose \
  --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
  -p "$PROJECT_NAME" \
  ps
