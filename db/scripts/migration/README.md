# PostgreSQL Migration Scripts

## 1) Open-source local bootstrap (no Oracle real-data migration)

This is the default path for this repository.

### Scripts
- `generate-ddl-from-mappers.ps1`
  - Scans MyBatis mapper SQL and generates:
    - `migration-input/ddl/schema_postgres.sql`
    - `migration-input/ddl/constraints_postgres.sql`
    - `migration-input/ddl/indexes_postgres.sql`
    - `migration-input/ddl/seed_sample.sql`
- `bootstrap-local-postgres.ps1`
  - Starts Docker PostgreSQL and applies generated SQL files.
- `apply-local-demo-backfill.ps1`
  - Re-applies runtime fixes plus demo seed data to an already-running local PostgreSQL container.
- `run-app-postgres.ps1`
  - Runs Spring Boot with PostgreSQL env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER`).
- `install-jdk21.ps1`
  - Installs JDK 21 via `winget`.

### Quick run
```powershell
# repo root
.\scripts\migration\generate-ddl-from-mappers.ps1
.\scripts\migration\bootstrap-local-postgres.ps1 -PgPassword "postgres" -ResetSchema
.\scripts\migration\run-app-postgres.ps1 -PgPassword "postgres"
```

If `5432` is already used, run with another host port:
```powershell
.\scripts\migration\bootstrap-local-postgres.ps1 -PgPort 5435 -PgPassword "postgres"
.\scripts\migration\run-app-postgres.ps1 -PgPort 5435 -PgPassword "postgres"
```

If DDL changed and you need a clean re-apply, include `-ResetSchema`.
If your default Java is not 21, pass `-JavaHome "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"` to `run-app-postgres.ps1`.

## 2) CSV-based migration (when Oracle export already exists)

### Scripts
- `run-postgres-migration.ps1`: end-to-end orchestrator
- `import-csv.ps1`: table CSV loader (`\copy`)
- `verify-csv-counts.ps1`: CSV row count vs PostgreSQL count
- `reset-sequences.sql`: set each serial sequence to `MAX(pk)+1`
- `generate-oracle-count-sql.ps1`: Oracle table count SQL generator
- `load-order.sample.txt`: optional load order template

### Expected input structure
```
<workdir>\
  ddl\
    schema_postgres.sql
    constraints_postgres.sql   (optional)
    indexes_postgres.sql       (optional)
  data\
    USERS.csv
    DEPARTMENT.csv
    ...
```

### Run (PowerShell)
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
  -Schema "public" `
  -LoadOrderFile ".\scripts\migration\load-order.sample.txt"
```

## Notes
- `migration-input/` is gitignored by design.
- Generated DDL is a mapper-driven baseline for local execution; if you need strict production schema parity, refine DDL manually.
- `seed_sample.sql` now includes 8 default approval templates (`hr/finance/sales/it/pro/logistics/trip` categories).
- `seed_operational_demo.sql` populates approval, meeting, and attendance demo data so the core local pages do not load empty.
- 신규 시드 데이터(템플릿명/설명/코드명 노출 텍스트)는 한국어를 기본값으로 유지합니다.
