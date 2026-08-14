#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: $0 <ecr-registry> <image-tag> <aws-region>" >&2
  exit 1
fi

export ECR_REGISTRY="$1"
export IMAGE_TAG="$2"
AWS_REGION="$3"

cd "$(dirname "$0")/.."

readonly SHARED_NETWORK="logistics-platform_logistics"
readonly STATE_DIR="${DEPLOY_STATE_DIR:-$HOME/.logistics-platform-deploy}"
readonly ACTIVE_COLOR_FILE="$STATE_DIR/active-color"
readonly UPSTREAM_FILE="infrastructure/caddy/active-upstream.caddy"

app_services=(
  eureka-server
  config-server
  api-gateway
  user-service
  hub-service
  company-service
  order-service
  delivery-service
  ai-notification-service
)

base_files=(
  --env-file .env.prod
  -f infrastructure/docker-compose.yml
  -f infrastructure/docker-compose.prod.yml
  -f infrastructure/docker-compose.ecr.yml
  -f infrastructure/docker-compose.blue-green.yml
)

color_files=("${base_files[@]}")

mkdir -p "$STATE_DIR" "$(dirname "$UPSTREAM_FILE")"
exec 9>"$STATE_DIR/deploy.lock"
if ! flock -n 9; then
  echo "Another deployment is already running." >&2
  exit 1
fi

docker network inspect "$SHARED_NETWORK" >/dev/null 2>&1 \
  || docker network create "$SHARED_NETWORK" >/dev/null

aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY"

active_color="legacy"
if [[ -f "$ACTIVE_COLOR_FILE" ]]; then
  active_color="$(<"$ACTIVE_COLOR_FILE")"
fi

case "$active_color" in
  blue) candidate_color="green" ;;
  green|legacy) candidate_color="blue" ;;
  *)
    echo "Invalid active color: $active_color" >&2
    exit 1
    ;;
esac

export DEPLOYMENT_COLOR="$candidate_color"
export EUREKA_HOST="${candidate_color}-eureka-server"
export CONFIG_SERVER_HOST="${candidate_color}-config-server"
candidate_project="logistics-${candidate_color}"

if [[ ! -f "$UPSTREAM_FILE" ]]; then
  printf '%s\n' 'reverse_proxy api-gateway:8080' > "$UPSTREAM_FILE"
fi
previous_upstream="$(<"$UPSTREAM_FILE")"

# One-time migration: add the dynamic upstream file mount to the running Caddy.
caddy_id="$(docker compose "${base_files[@]}" ps -q caddy)"
if [[ -z "$caddy_id" ]] \
  || ! docker inspect "$caddy_id" --format '{{range .Mounts}}{{println .Destination}}{{end}}' \
      | grep -qx '/etc/caddy/active-upstream.caddy'; then
  docker compose "${base_files[@]}" up -d --no-deps --force-recreate caddy
  caddy_id="$(docker compose "${base_files[@]}" ps -q caddy)"
fi

switched=false
cleanup_candidate() {
  if [[ "$switched" == false ]]; then
    printf '%s\n' "$previous_upstream" > "$UPSTREAM_FILE"
    if [[ -n "${caddy_id:-}" ]]; then
      docker exec "$caddy_id" caddy reload \
        --config /etc/caddy/Caddyfile >/dev/null 2>&1 || true
    fi
    DEPLOYMENT_COLOR="$candidate_color" \
    EUREKA_HOST="${candidate_color}-eureka-server" \
    CONFIG_SERVER_HOST="${candidate_color}-config-server" \
      docker compose -p "$candidate_project" "${color_files[@]}" \
        stop "${app_services[@]}" >/dev/null 2>&1 || true
  fi
}
trap cleanup_candidate EXIT

docker compose -p "$candidate_project" "${color_files[@]}" config --quiet
docker compose -p "$candidate_project" "${color_files[@]}" pull "${app_services[@]}"

# Start in dependency order and switch traffic only after every service is healthy.
docker compose -p "$candidate_project" "${color_files[@]}" \
  up -d --no-deps --force-recreate --wait --wait-timeout 180 eureka-server
docker compose -p "$candidate_project" "${color_files[@]}" \
  up -d --no-deps --force-recreate --wait --wait-timeout 180 config-server
docker compose -p "$candidate_project" "${color_files[@]}" \
  up -d --no-deps --force-recreate --wait --wait-timeout 300 \
  api-gateway user-service hub-service company-service order-service \
  delivery-service ai-notification-service

docker exec "$caddy_id" wget -qO- \
  "http://${candidate_color}-api-gateway:9091/actuator/health" \
  | grep -q '"status":"UP"'

# Keep the bind-mounted file itself and replace only its contents.
printf 'reverse_proxy %s-api-gateway:8080\n' "$candidate_color" > "$UPSTREAM_FILE"

docker exec "$caddy_id" caddy validate --config /etc/caddy/Caddyfile
docker exec "$caddy_id" caddy reload --config /etc/caddy/Caddyfile

printf '%s\n' "$candidate_color" > "$ACTIVE_COLOR_FILE"
switched=true

# Keep the previous stack alive briefly so in-flight requests can finish.
sleep "${DEPLOY_GRACE_SECONDS:-30}"

if [[ "$active_color" == "legacy" ]]; then
  docker compose "${base_files[@]}" stop "${app_services[@]}"
else
  old_project="logistics-${active_color}"
  DEPLOYMENT_COLOR="$active_color" \
  EUREKA_HOST="${active_color}-eureka-server" \
  CONFIG_SERVER_HOST="${active_color}-config-server" \
    docker compose -p "$old_project" "${color_files[@]}" stop "${app_services[@]}"
fi

trap - EXIT
echo "Deployment complete: $active_color -> $candidate_color ($IMAGE_TAG)"
