# Cloudflare deployment

This directory runs the existing React build and Spring Boot backend behind one Cloudflare hostname.

## What stays the same
- React still calls relative API paths like `/rest`, `/common`, `/mail`, and `/chat`.
- Messenger still uses the same-host WebSocket path `/starworks-groupware-websocket`.
- The backend still uses the same JWT cookie flow and the same PostgreSQL schema.

## What changes
- The database moves to Supabase Postgres.
- File storage moves to Cloudflare R2 through the S3-compatible API.
- The Spring Boot backend runs inside Cloudflare Containers Beta.
- A Worker serves `frontend/dist` and forwards backend paths to the container.

## Deploy steps
1. Apply the PostgreSQL SQL files in `db/migration-input/ddl` to Supabase in this order:
   - `schema_postgres.sql`
   - `runtime_fixes_postgres.sql`
   - `constraints_postgres.sql`
   - `indexes_postgres.sql`
2. Build the frontend bundle.
3. Copy `.dev.vars.example` to `.dev.vars` and fill the real values.
4. Install the worker dependencies.
5. Sync the values in `.dev.vars` into Cloudflare secrets.
6. Run `npx wrangler deploy`.

## Required values
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET_KEY`
- `APP_FRONTEND_BASE_URL`, `CORS_ALLOW_ORIGINS`, `COOKIE_DOMAIN`
- `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_BUCKET`, `AWS_ENDPOINT`

Non-secret defaults such as `FILE_STORAGE_MODE=s3`, `AWS_REGION=auto`, `COOKIE_SAME_SITE=Lax`, and the backend port stay in `wrangler.jsonc`.

## R2 note
Set `AWS_PUBLIC_BASE_URL` to a public R2 custom domain if you want image and attachment URLs to stay fast and directly usable in the existing UI.

## Scaling note
`wrangler.jsonc` is pinned to a single backend container instance for now. Messenger uses Spring's in-memory STOMP broker, so multi-instance scale would need a separate broker migration first.
