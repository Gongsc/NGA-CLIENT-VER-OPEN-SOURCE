#!/usr/bin/env bash
# 检查发布包中包含各模块的关键类，防止混淆或打包配置删除代码后发布出启动即闪退的安装包。
# 用法：check-apk-classes.sh <apk> <dexdump 路径>
set -euo pipefail

apk="$1"
dexdump="$2"
required=(
  "Lgov/anzong/androidnga/NgaClientApp;"
  "Lcom/justwen/androidnga/cloud/CloudServerManager;"
  "Lgov/anzong/androidnga/core/HtmlConvertFactory;"
  "Lgov/anzong/androidnga/http/OnHttpCallBack;"
  "Lgov/anzong/androidnga/base/json/NgaJson;"
)

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
unzip -q "$apk" 'classes*.dex' -d "$work"
for dex in "$work"/classes*.dex; do
  "$dexdump" "$dex" | sed -n "s/^  Class descriptor  : '\(.*\)'$/\1/p"
done > "$work/classes.txt"

missing=0
for cls in "${required[@]}"; do
  if ! grep -qxF "$cls" "$work/classes.txt"; then
    echo "::error::APK is missing $cls"
    missing=1
  fi
done
[ "$missing" -eq 0 ] && echo "All required classes present ($(wc -l < "$work/classes.txt") classes)"
exit "$missing"
