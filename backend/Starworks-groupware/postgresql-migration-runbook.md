# StarWorks Oracle -> PostgreSQL Migration Runbook

## 1) Scope

This runbook supports two paths:
- **Path A (default for this open-source repo):** No Oracle real-data migration, local PostgreSQL bootstrap for execution/testing.
- **Path B:** CSV-based migration when Oracle export already exists.

---

## 2) Path A: Local bootstrap (no Oracle data export)

### Goal
- Run StarWorks on PostgreSQL locally.
- Keep backend/API behavior unchanged after Oracle-to-PostgreSQL SQL conversion.

### Prerequisites
1. Docker Desktop running.
2. JDK 21 available (`java -version` should show 21.x).

### Steps
```powershell
# 1) (Optional) install JDK 21
.\scripts\migration\install-jdk21.ps1

# 2) Generate mapper-driven PostgreSQL DDL + seed
.\scripts\migration\generate-ddl-from-mappers.ps1

# 3) Start PostgreSQL container and apply SQL
.\scripts\migration\bootstrap-local-postgres.ps1 -PgPassword "postgres" -ResetSchema

# 4) Start application with PostgreSQL connection
.\scripts\migration\run-app-postgres.ps1 -PgPassword "postgres"
```

When DDL generation changes, keep `-ResetSchema` enabled to avoid leftover columns from prior runs.
If the default `java -version` is not 21.x, pass `-JavaHome` to `run-app-postgres.ps1`.

### Verification checklist
1. Application starts without Oracle syntax errors.
2. Login works with seeded account:
   - `admin / admin1234`
3. Approval template modal shows seeded templates by category (`hr/finance/sales/it/pro/logistics/trip`).
4. Core smoke tests:
   - Org chart/user list
   - Approval document list/create
   - Board list/paging
   - Mail/Messenger basic access
5. 신규로 추가하는 템플릿/기본 코드의 사용자 노출 문구는 한국어로 작성한다.

---

## 3) Path B: CSV-based migration (Oracle export exists)

### Prerequisites
1. PostgreSQL + `psql` installed.
2. PostgreSQL target DB created (`starworks`).
3. Input files:
   - `migration-input\ddl\schema_postgres.sql`
   - `migration-input\ddl\constraints_postgres.sql` (optional)
   - `migration-input\ddl\indexes_postgres.sql` (optional)
   - `migration-input\data\*.csv`

### Run
```powershell
.\scripts\migration\run-postgres-migration.ps1 `
  -PsqlPath "C:\Program Files\PostgreSQL\16\bin\psql.exe" `
  -PgHost "127.0.0.1" `
  -PgPort 5432 `
  -PgDatabase "starworks" `
  -PgUser "postgres" `
  -PgPassword "<password>" `
  -DdlDir ".\migration-input\ddl" `
  -CsvDir ".\migration-input\data" `
  -Schema "public"
```

Optional:
- `-LoadOrderFile .\scripts\migration\load-order.sample.txt`
- `-SkipConstraints` / `-SkipCountVerify` (not recommended for production cutover)

### Cross-check helper
```powershell
.\scripts\migration\generate-oracle-count-sql.ps1 `
  -CsvDir ".\migration-input\data" `
  -OutputFile ".\migration-input\oracle_table_counts.sql"
```

---

## 4) Completion criteria
1. PostgreSQL schema is applied successfully.
2. Backend runs with `DB_DRIVER=org.postgresql.Driver`.
3. Core business screens execute without SQL grammar errors.
4. (Path B only) CSV vs PostgreSQL counts are matched.
