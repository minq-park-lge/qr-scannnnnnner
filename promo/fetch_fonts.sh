#!/usr/bin/env bash
# 한국어 폰트(Black Han Sans, Noto Sans KR)는 용량이 커서 저장소에 넣지 않고 npm 에서 받는다.
set -euo pipefail
cd "$(dirname "$0")"
tmp=$(mktemp -d)
for pkg in black-han-sans noto-sans-kr; do
  (cd "$tmp" && npm pack -q "@fontsource/$pkg" >/dev/null)
  mkdir -p "fonts/$pkg"
  tar xzf "$tmp"/fontsource-$pkg-*.tgz -C "fonts/$pkg" --strip-components=1
done
rm -rf "$tmp"
echo "fonts ready"
