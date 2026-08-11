#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: $0 <ecr-registry> <image-tag> <aws-region>" >&2
  exit 1
fi

export ECR_REGISTRY="$1"
export IMAGE_TAG="$2"
AWS_REGION="$3"

cd "$(dirname "$0")/.."

aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY"

compose_files=(
  --env-file .env.prod
  -f infrastructure/docker-compose.yml
  -f infrastructure/docker-compose.prod.yml
  -f infrastructure/docker-compose.ecr.yml
)

docker compose "${compose_files[@]}" config --quiet
docker compose "${compose_files[@]}" pull
docker compose "${compose_files[@]}" up -d --no-build --remove-orphans
