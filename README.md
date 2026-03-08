# Modulearning Cloudflare + Supabase Stack

This repository is the Cloudflare + Supabase only version of Modulearning.

## Services
- `frontend`: React + Vite client
- `backend`: Spring Boot API and WebSocket server
- `cloudflare`: Worker + Container deployment config for `modulearning01-api`
- `db`: PostgreSQL / Supabase migration SQL

## Seed login accounts
- `admin / admin1234`
- `user01 / user1234`

The seeded admin account is defined in `db/migration-input/ddl/seed_sample.sql`.

## Local development
```powershell
docker compose up --build
```

Default URLs:
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:18080`
- Database: `localhost:55432`

## Frontend env
Copy `frontend/.env.example` to `frontend/.env.local` when you need local overrides.

- `VITE_APP_BASE_URL`: frontend origin, default target `https://modulearning01.pages.dev`
- `VITE_API_BASE_URL`: optional direct backend origin such as `https://modulearning01-api.<your-cloudflare-subdomain>.workers.dev`
- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_PUBLISHABLE_KEY`

Leave `VITE_API_BASE_URL` empty when the frontend is reverse-proxied to the backend on the same origin.

## Supabase database bootstrap
Apply these files to Supabase Postgres in order:
1. `db/migration-input/ddl/schema_postgres.sql`
2. `db/migration-input/ddl/runtime_fixes_postgres.sql`
3. `db/migration-input/ddl/constraints_postgres.sql`
4. `db/migration-input/ddl/indexes_postgres.sql`
5. `db/migration-input/ddl/seed_sample.sql`

## Deployment targets
- Frontend Pages project: `modulearning01`
- Backend Worker/Container project: `modulearning01-api`

## Cloudflare deployment
1. Build the frontend bundle.
2. Copy `cloudflare/.dev.vars.example` to `cloudflare/.dev.vars`.
3. Fill in real Supabase, JWT, cookie, and storage values.
4. Deploy from `cloudflare/`:

```powershell
npm install
npm run secrets:sync
npx wrangler deploy
```

The worker forwards `/rest`, `/common`, `/mail`, `/chat`, `/file`, `/folder`, and `/starworks-groupware-websocket` to the Spring backend.

## Storage
Use Supabase Storage through its S3-compatible endpoint.

- `FILE_STORAGE_MODE=s3`
- `AWS_ENDPOINT=https://<project-ref>.storage.supabase.co/storage/v1/s3`
- `AWS_PUBLIC_BASE_URL=https://<project-ref>.supabase.co/storage/v1/object/public/<bucket>`
- `AWS_REGION=<supabase-project-region>`

## Notes
- Do not commit `.env.production`, `cloudflare/.dev.vars`, or any real credentials.
- Messenger is still single-instance because the backend uses an in-memory STOMP broker.
