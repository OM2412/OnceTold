#!/usr/bin/env bash
set -euo pipefail

required=(SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD APP_JWT_SECRET SPRING_AI_OPENAI_API_KEY COGNEE_API_KEY COGNEE_BASE_URL COGNEE_DATASET_PREFIX)
for key in "${required[@]}"; do
  if [[ -z "${!key:-}" ]]; then
    printf 'Missing required environment variable: %s\n' "$key" >&2
    exit 1
  fi
done
if [[ "$SPRING_DATASOURCE_URL" != jdbc:postgresql://* ]]; then
  printf 'SPRING_DATASOURCE_URL must use jdbc:postgresql://\n' >&2
  exit 1
fi
if (( ${#APP_JWT_SECRET} < 32 )); then
  printf 'APP_JWT_SECRET must contain at least 32 characters\n' >&2
  exit 1
fi
# Both children belong to this container. The memory API is loopback-only.
pids=()
cleanup() {
  trap - EXIT TERM INT
  for pid in "${pids[@]}"; do kill -TERM "$pid" 2>/dev/null || true; done
  for pid in "${pids[@]}"; do wait "$pid" 2>/dev/null || true; done
}
trap cleanup EXIT
trap 'exit 143' TERM
trap 'exit 130' INT

/opt/memory-venv/bin/python -m uvicorn main:app --app-dir /app/memory --host 127.0.0.1 --port 8000 &
pids+=("$!")
java -Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError -jar /app/app.jar &
pids+=("$!")
# A dead bridge must restart the whole instance instead of leaving a half-app.
status=0
wait -n "${pids[@]}" || status=$?
if (( status == 0 )); then status=1; fi
exit "$status"
