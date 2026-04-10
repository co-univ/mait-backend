#!/usr/bin/env bash

set -euo pipefail

MYSQL_CONTAINER="${MYSQL_CONTAINER:-mysql8}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_DATABASE="${MYSQL_DATABASE:-mait}"

mysql_cmd() {
  if [[ -n "${MYSQL_PASSWORD}" ]]; then
    podman exec -i "${MYSQL_CONTAINER}" mysql -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" "$@"
  else
    podman exec -i "${MYSQL_CONTAINER}" mysql -u"${MYSQL_USER}" "${MYSQL_DATABASE}" "$@"
  fi
}

mysql_cmd_no_db() {
  if [[ -n "${MYSQL_PASSWORD}" ]]; then
    podman exec -i "${MYSQL_CONTAINER}" mysql -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "$@"
  else
    podman exec -i "${MYSQL_CONTAINER}" mysql -u"${MYSQL_USER}" "$@"
  fi
}

show_help() {
  cat <<'EOF'
Usage:
  ./tools/question-set-delete-benchmark/mysql-metrics.sh <action> [args]

Actions:
  reset-digest
  top-digest [limit]
  enable-slow [seconds]
  disable-slow
  show-slow-config
  enable-general
  disable-general
  show-general-config
EOF
}

action="${1:-help}"
limit="${2:-20}"
threshold="${2:-0.05}"

case "${action}" in
  reset-digest)
    mysql_cmd_no_db -e "TRUNCATE TABLE performance_schema.events_statements_summary_by_digest;"
    ;;
  top-digest)
    mysql_cmd_no_db -e "
SELECT
  DIGEST_TEXT,
  COUNT_STAR,
  ROUND(AVG_TIMER_WAIT / 1000000000, 3) AS avg_ms,
  ROUND(SUM_TIMER_WAIT / 1000000000, 3) AS total_ms
FROM performance_schema.events_statements_summary_by_digest
WHERE DIGEST_TEXT IS NOT NULL
ORDER BY COUNT_STAR DESC, SUM_TIMER_WAIT DESC
LIMIT ${limit};"
    ;;
  enable-slow)
    mysql_cmd_no_db -e "SET GLOBAL slow_query_log = 'ON'; SET GLOBAL long_query_time = ${threshold};"
    ;;
  disable-slow)
    mysql_cmd_no_db -e "SET GLOBAL slow_query_log = 'OFF';"
    ;;
  show-slow-config)
    mysql_cmd_no_db -e "
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';
SHOW VARIABLES LIKE 'slow_query_log_file';"
    ;;
  enable-general)
    mysql_cmd_no_db -e "SET GLOBAL general_log = 'ON';"
    ;;
  disable-general)
    mysql_cmd_no_db -e "SET GLOBAL general_log = 'OFF';"
    ;;
  show-general-config)
    mysql_cmd_no_db -e "
SHOW VARIABLES LIKE 'general_log';
SHOW VARIABLES LIKE 'general_log_file';"
    ;;
  help|--help|-h)
    show_help
    ;;
  *)
    echo "Unknown action: ${action}" >&2
    show_help
    exit 1
    ;;
esac
