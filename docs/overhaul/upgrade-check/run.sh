#!/usr/bin/env bash
#
# Upgrade check: a database and configuration written by an older OrientDB release must be opened
# by a newer build, give the same answers, and serve both old and new clients.
#
#   run.sh OLD_IMAGE NEW_IMAGE
#
# Both images must start the server with server.sh and keep databases in /orientdb/databases and
# configuration in /orientdb/config (all OrientDB images do). The old server populates named volumes
# that the new server then starts on, as an in-place upgrade would. Results go to $OUT (a temp dir by
# default); the exit status is non-zero if any check fails.
set -uo pipefail

OLD=$1
NEW=$2
HERE=$(cd "$(dirname "$0")" && pwd)
NAME=${NAME:-odb-upgrade-check}
OUT=${OUT:-$(mktemp -d)}
PW=$(openssl rand -hex 16)
FAILED=0

log() { printf '\n== %s\n' "$*"; }
fail() { printf 'FAIL: %s\n' "$*"; FAILED=1; }

cleanup() {
  docker rm -f "$NAME-old" "$NAME-new" >/dev/null 2>&1
  docker volume rm "$NAME-db" "$NAME-config" >/dev/null 2>&1
  docker network rm "$NAME" >/dev/null 2>&1
}
trap cleanup EXIT
cleanup

start() { # container image
  docker run -d --name "$1" --network "$NAME" --memory=2g \
    -e ORIENTDB_ROOT_PASSWORD="$PW" -e ORIENTDB_OPTS_MEMORY="-Xms512m -Xmx512m" \
    -v "$NAME-db:/orientdb/databases" -v "$NAME-config:/orientdb/config" "$2" >/dev/null
  for _ in $(seq 1 90); do
    docker logs "$1" 2>&1 | grep -q "Studio available" && return 0
    [ "$(docker inspect -f '{{.State.Running}}' "$1")" = "true" ] || break
    sleep 2
  done
  fail "$1 ($2) did not start"
  docker logs "$1" 2>&1 | tail -20
  return 1
}

sql() { # container database command: prints the JSON response
  python3 -c 'import json,sys; print(json.dumps({"command": sys.argv[1]}))' "$3" |
    docker exec -i "$1" curl -sS -u "root:$PW" -X POST "http://localhost:2480/command/$2/sql" \
      -H 'Content-Type: application/json' --data-binary @-
}

# rebuilds every Lucene index of the databases used by queries.txt
rebuild_lucene() { # container
  for db in $(cut -f1 "$HERE/queries.txt" | sort -u); do
    for index in $(sql "$1" "$db" "select name from (select expand(indexes) from metadata:indexmanager) where algorithm = 'LUCENE'" |
      python3 -c 'import json,sys; print(" ".join(r["name"] for r in json.load(sys.stdin).get("result", [])))'); do
      echo "rebuild Lucene index $db.$index: $(sql "$1" "$db" "rebuild index \`$index\`" | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d["result"][0]["totalIndexed"] if "result" in d else "ERROR " + d["errors"][0]["content"][:200])')"
    done
  done
}

# runs every query of queries.txt over HTTP and prints normalized results
rest_queries() { # container
  while IFS=$'\t' read -r db query; do
    printf '%s\t%s\t' "$db" "$query"
    python3 -c 'import json,sys; print(json.dumps({"command": sys.argv[1]}))' "$query" |
      docker exec -i "$1" curl -sS -u "root:$PW" -X POST "http://localhost:2480/command/$db/sql" \
        -H 'Content-Type: application/json' --data-binary @- |
      python3 -c '
import json, sys
raw = sys.stdin.read()
try:
    rows = json.loads(raw)["result"]
except Exception:
    print("ERROR " + raw.strip().replace("\n", " ")[:300]); sys.exit()
clean = [{k: v for k, v in r.items() if not k.startswith("@")} for r in rows]
print(json.dumps(clean, sort_keys=True))'
  done <"$HERE/queries.txt"
}

# counts Person over the binary protocol with the console of CLIENT_IMAGE
binary_count() { # client-image server-container
  local log="$OUT/console-$(echo "$1" | tr ':/' '__')-to-$2.log"
  docker run --rm --network "$NAME" "$1" console.sh \
    "connect remote:$2/compat root $PW; select count(*) as c from Person;" >"$log" 2>&1
  grep -qE '\| *1500 *\|' "$log"
}

docker network create "$NAME" >/dev/null
docker volume create "$NAME-db" >/dev/null
docker volume create "$NAME-config" >/dev/null

log "old server: $OLD"
start "$NAME-old" "$OLD" || exit 1
docker logs "$NAME-old" 2>&1 | grep -m1 "is starting up"
docker exec "$NAME-old" console.sh "create database remote:localhost/compat root $PW plocal" >"$OUT/create.log" 2>&1 ||
  fail "create database on old server (see $OUT/create.log)"
{ echo "connect remote:localhost/compat root $PW;"; cat "$HERE/dataset.osql"; } |
  docker exec -i "$NAME-old" sh -c 'cat > /tmp/dataset.osql'
docker exec "$NAME-old" console.sh /tmp/dataset.osql >"$OUT/dataset.log" 2>&1 ||
  fail "dataset on old server (see $OUT/dataset.log)"
rest_queries "$NAME-old" >"$OUT/old.txt"
if grep -q ERROR "$OUT/old.txt"; then fail "queries on old server: $(grep -m1 ERROR "$OUT/old.txt")"; fi
binary_count "$OLD" "$NAME-old" || fail "old console -> old server"
binary_count "$NEW" "$NAME-old" && echo "new console -> old server: ok" || fail "new console -> old server"

log "stop old server"
docker stop -t 60 "$NAME-old" >/dev/null
docker logs "$NAME-old" >"$OUT/old-server.log" 2>&1
docker rm "$NAME-old" >/dev/null

log "new server on the old databases and configuration: $NEW"
start "$NAME-new" "$NEW" || { docker logs "$NAME-new" >"$OUT/new-server.log" 2>&1; exit 1; }
docker logs "$NAME-new" 2>&1 | grep -m1 "is starting up"

# Upgrade step (see section 5 of docs/overhaul/2026-11-audit.md): OrientDB 3.x writes Lucene 7
# indexes, which 4.0 can't read, so every Lucene index is rebuilt from its records.
rebuild_lucene "$NAME-new"

rest_queries "$NAME-new" >"$OUT/new.txt"
if diff "$OUT/old.txt" "$OUT/new.txt" >"$OUT/diff.txt"; then
  echo "same answers to $(wc -l <"$OUT/old.txt" | tr -d ' ') queries"
else
  fail "answers differ (see $OUT/diff.txt)"
  cat "$OUT/diff.txt"
fi
binary_count "$NEW" "$NAME-new" && echo "new console -> new server: ok" || fail "new console -> new server"
binary_count "$OLD" "$NAME-new" && echo "old console -> new server: ok" || fail "old console -> new server"
docker exec "$NAME-new" console.sh "connect remote:localhost/compat root $PW; insert into Person set uid = 5000, name = 'new'; create edge Knows from (select from Person where uid = 5000) to (select from Person where uid = 0);" >"$OUT/write.log" 2>&1 ||
  fail "write on new server (see $OUT/write.log)"
docker logs "$NAME-new" >"$OUT/new-server.log" 2>&1
# the messages asking to rebuild 3.x Lucene indexes are expected until the rebuild step
grep -E "SEVER|not closed properly|Exception" "$OUT/new-server.log" | grep -v "^\s*at " |
  grep -vE "was written by an older OrientDB release|IndexFormatTooOldException" | head -10 >"$OUT/new-server-problems.txt"
if [ -s "$OUT/new-server-problems.txt" ]; then
  fail "problems in new server log:"
  cat "$OUT/new-server-problems.txt"
fi

log "rollback: old server on the databases written by the new one"
docker stop -t 60 "$NAME-new" >/dev/null
docker rm "$NAME-new" >/dev/null
BEFORE_ROLLBACK=$FAILED
if start "$NAME-old" "$OLD"; then
  # Lucene indexes rebuilt by the new release are in a format the old one can't read
  rebuild_lucene "$NAME-old"
  rest_queries "$NAME-old" >"$OUT/rollback.txt"
  if grep -q ERROR "$OUT/rollback.txt"; then
    echo "WARNING: rollback to $OLD can't read the data: $(grep -m1 ERROR "$OUT/rollback.txt" | cut -c1-200)"
  else
    echo "rollback to $OLD: all $(grep -c . "$OUT/rollback.txt") queries answered after rebuilding the Lucene indexes"
  fi
else
  FAILED=$BEFORE_ROLLBACK # rollback is reported, not required
  echo "WARNING: $OLD doesn't start on the databases written by $NEW"
fi
docker logs "$NAME-old" >"$OUT/rollback-server.log" 2>&1

log "result"
echo "details in $OUT"
[ "$FAILED" = 0 ] && echo "PASS" || echo "FAILED"
exit "$FAILED"
