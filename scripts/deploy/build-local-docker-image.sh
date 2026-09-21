#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPOSITORY_ROOT=$(cd -- "${SCRIPT_DIR}/../.." && pwd)
IMAGE_NAME="gnilc-auth-admin"

docker build "${REPOSITORY_ROOT}" -f "${SCRIPT_DIR}/Dockerfile" -t "${IMAGE_NAME}"

echo "Built ${IMAGE_NAME}. The gnilc-auth network must contain a backend named server listening on port 3888."
echo "docker run -d --network gnilc-auth -p 8010:8080 --name gnilc-auth-admin ${IMAGE_NAME}"
