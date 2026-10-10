#!/usr/bin/env bash
# 사용: bash deploy/build-frontend.sh /절대/프론트/경로
set -euo pipefail
FRONTEND_DIR="${1:?프론트 저장소 경로를 지정하세요}"
cd "$FRONTEND_DIR"
[ -f package-lock.json ] || { echo 'package-lock.json이 필요합니다.' >&2; exit 1; }
npm ci
npm run build
[ -f dist/index.html ] || { echo 'dist/index.html이 없습니다.' >&2; exit 1; }
echo "프론트 빌드 완료: $PWD/dist"
