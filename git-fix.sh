#!/bin/bash

echo "executing git format"
set -e

SCRIPT_DIR=$( cd $(dirname $(readlink ${BASH_SOURCE[0]} || echo "${BASH_SOURCE[0]}") ) && pwd )
echo "script directory: $SCRIPT_DIR"
LINT_SCRIPT=$SCRIPT_DIR/lint.py
cd $(git rev-parse --show-toplevel)

changed_files=$(git status --no-renames --porcelain=1 | grep '^[MARC]\|^.[MARC]' | awk '{ print $2 }' || true)

python $LINT_SCRIPT --fix $changed_files
