#!/usr/bin/env sh
set -e

# LIMS docker entrypoint. Currently a passthrough: the runtime image's
# COPY chmods are already correct, the spring user owns /var/log/lims,
# and JAVA_OPTS is already in the env. The script exists so future
# per-boot work (waiting for the DB, applying secrets, generating
# config, etc.) has a place to live without rebuilding the image.

exec java ${JAVA_OPTS} -cp "/app:/app/lib/*" "$@"
