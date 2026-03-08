#!/bin/sh
set -eu

seed_dir="/docker-entrypoint-initdb-seed"

echo "Initializing database from $seed_dir"

run_sql() {
  file_path="$1"
  if [ -f "$file_path" ]; then
    echo "Applying $(basename "$file_path")"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f "$file_path"
  else
    echo "Skipping missing file $file_path"
  fi
}

run_sql "$seed_dir/schema_postgres.sql"
run_sql "$seed_dir/runtime_fixes_postgres.sql"
run_sql "$seed_dir/constraints_postgres.sql"
run_sql "$seed_dir/indexes_postgres.sql"
run_sql "$seed_dir/seed_sample.sql"
run_sql "$seed_dir/seed_operational_demo.sql"
run_sql "$seed_dir/seed_community_demo.sql"
run_sql "$seed_dir/seed_board_demo.sql"
run_sql "$seed_dir/seed_calendar_demo.sql"
run_sql "$seed_dir/seed_contract_demo.sql"
