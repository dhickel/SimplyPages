#!/usr/bin/env bash
set -euo pipefail

if (( $# != 0 )); then
    printf '%s\n' 'Remote deployment is no longer supported. Run ./build-site.sh without arguments for a local artifact.' >&2
    exit 64
fi

exec "$(dirname "$0")/build-site.sh"
