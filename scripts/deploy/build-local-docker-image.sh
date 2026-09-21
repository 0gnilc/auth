#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPOSITORY_ROOT=$(cd -- "${SCRIPT_DIR}/../.." && pwd)
VERSION=$(node -p "require(process.argv[1]).version" "${REPOSITORY_ROOT}/package.json")
REVISION=$(git -C "${REPOSITORY_ROOT}" rev-parse HEAD)
IMAGE_NAME="gnilc-auth-admin:${VERSION}-dev-${REVISION:0:12}"

docker build "${REPOSITORY_ROOT}" -f "${SCRIPT_DIR}/Dockerfile" --build-arg "BUILD_REVISION=${REVISION}" -t "${IMAGE_NAME}"

echo "Built ${IMAGE_NAME}. The gnilc-auth network must contain a backend named server listening on port 3888."
echo "docker run -d --network gnilc-auth -p 8010:8080 --name gnilc-auth-admin ${IMAGE_NAME}"
