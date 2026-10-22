#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

base="${usage_base:-origin/main}"
if ! git rev-parse --verify --quiet "${base}^{commit}" >/dev/null; then
  echo "lint:ci: '$base' does not resolve to a commit" >&2
  exit 2
fi

changed_files=$(mktemp)
ktlint_files=$(mktemp)
trap 'rm -f "$changed_files" "$ktlint_files"' EXIT

git diff --name-only -z --diff-filter=d "$base...HEAD" -- \
  . ':(exclude,glob)docs/code/example/**' ':(exclude,glob)docs/code/test/**' > "$changed_files"

# Match hk's whitespace rules: report trailing/EOF whitespace, allow ordinary
# CRLF files, and allow space-before-tab indentation.
git -c core.whitespace=blank-at-eol,blank-at-eof,cr-at-eol,-space-before-tab \
  diff --check "$base...HEAD" -- \
  . ':(exclude,glob)docs/code/example/**' ':(exclude,glob)docs/code/test/**'

git diff --name-only -z --diff-filter=d "$base...HEAD" -- \
  '*.kt' '*.kts' \
  ':(exclude,glob)docs/code/example/**' \
  ':(exclude,glob)docs/code/test/**' > "$ktlint_files"

if [ -s "$changed_files" ]; then
  node scripts/check-final-newline.mjs "$changed_files"
fi

if [ ! -s "$ktlint_files" ]; then
  echo "lint:ci: no Kotlin files changed since $base"
  exit 0
fi

echo "lint:ci: Kotlin files changed since $base:"
tr '\0' '\n' < "$ktlint_files" | sed 's/^/  /'
# An empty delimiter selects NUL-separated patterns, matching git's -z output.
ktlint --patterns-from-stdin='' < "$ktlint_files"
