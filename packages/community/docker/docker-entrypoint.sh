#!/bin/sh
#
# Runs the command as the orientdb user. When the container starts as root (the default), the data
# directories are first handed over to orientdb, so volumes written by older images, which ran the
# server as root, keep working after an upgrade. Set ORIENTDB_RUN_AS_ROOT=true to keep running as
# root, or start the container with --user to skip this step.
set -e

if [ "$(id -u)" = "0" ] && [ "${ORIENTDB_RUN_AS_ROOT:-false}" != "true" ]; then
  for dir in databases config backup log; do
    if [ -d "/orientdb/$dir" ]; then
      find "/orientdb/$dir" \! -user orientdb -exec chown orientdb:orientdb {} +
    fi
  done
  exec setpriv --reuid=orientdb --regid=orientdb --init-groups -- "$@"
fi

exec "$@"
