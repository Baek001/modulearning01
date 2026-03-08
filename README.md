# StarWorks Local Stack

Local Docker Compose setup for the StarWorks React frontend, Spring Boot backend, and PostgreSQL database.

## Services
- `frontend`: React app served by Nginx on `http://localhost:5173`
- `backend`: Spring Boot app on `http://localhost:18080`
- `db`: PostgreSQL 16 on `localhost:55432` by default

## One-command startup
```powershell
docker compose up --build
```

Open the app at http://localhost:5173. Host ports can be overridden with STARWORKS_FRONTEND_PORT, STARWORKS_BACKEND_PORT, and STARWORKS_DB_PORT.

## Seed login accounts
- `admin / admin1234`
- `user01 / user1234`

## Reset the database
```powershell
docker compose down -v
docker compose up --build
```

## Production deploy
1. Copy `.env.production.example` to `.env.production` and fill in the real Supabase and domain values.
2. Point your Cloudflare proxied DNS record to the VPS.
3. Start the production stack:

```powershell
docker compose -f compose.prod.yaml --env-file .env.production up --build -d
```

The production stack serves the app from a single origin. Only the frontend port is published; the backend stays inside the Docker network and is reached through the frontend proxy.

## Cloudflare + Supabase deploy
1. Apply `db/migration-input/ddl/schema_postgres.sql`, `runtime_fixes_postgres.sql`, `constraints_postgres.sql`, and `indexes_postgres.sql` to your Supabase Postgres instance in that order.
2. Build the frontend bundle once:

```powershell
cd frontend
npm run build
```

3. Copy [cloudflare/.dev.vars.example](C:/swb/cloudflare/.dev.vars.example) to `cloudflare/.dev.vars` and fill in the real Supabase, cookie, JWT, and R2 values.
4. Deploy the Cloudflare worker and container:

```powershell
cd cloudflare
npm install
npm run secrets:sync
npx wrangler deploy
```

This path keeps the frontend same-origin and routes `/rest`, `/common`, `/mail`, `/chat`, `/file`, `/folder`, and `/starworks-groupware-websocket` to the Spring Boot container. The initial container config is intentionally single-instance because messenger currently uses Spring's in-memory STOMP broker.

## Current integration status
- Fully live today: login, organization, project, calendar, board, attendance, meeting, mypage basics
- Beta but real-data based: dashboard, approval, email, messenger

## Notes
- Database initialization uses `db/migration-input/ddl/schema_postgres.sql`, `runtime_fixes_postgres.sql`, `constraints_postgres.sql`, `indexes_postgres.sql`, and `seed_sample.sql` in that order.
- If you are connecting to an existing PostgreSQL or Supabase database that was initialized before the meeting schema fixes, apply `db/migration-input/ddl/runtime_fixes_postgres.sql` once before starting the app.
- Frontend login is wired to the real backend at `/common/auth`.
- `/rest`, `/common`, `/mail`, and `/chat` are proxied through the frontend container.
- Cookie security, CORS, file storage path, and logging are now environment-driven for local and production runs.
- The default host DB port is `55432` to avoid common PostgreSQL conflicts on developer machines.


