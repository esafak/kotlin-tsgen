#!/usr/bin/env bash
set -euo pipefail

action="${1:-}"
shift || true

if [ "$#" -eq 0 ]; then
  echo "lint:ktlint: no files"
  exit 0
fi

case "$action" in
  check) exec ktlint "$@" ;;
  fix) exec ktlint -F "$@" ;;
  *) echo "usage: lint-ktlint.sh <check|fix> <file>..." >&2; exit 2 ;;
esac
