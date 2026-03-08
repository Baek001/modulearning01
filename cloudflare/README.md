# Cloudflare deployment for `modulearning01-api`

This directory contains the Cloudflare Worker + Container deployment for the Spring backend.

## Runtime contract
- Frontend pages project: `modulearning01`
- Backend worker project: `modulearning01-api`
- Backend routes forwarded by the worker:
  - `/rest`
  - `/common`
  - `/mail`
  - `/chat`
  - `/file`
  - `/folder`
  - `/actuator`
  - `/starworks-groupware-websocket`

## Required values
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET_KEY`
- `APP_FRONTEND_BASE_URL`
- `CORS_ALLOW_ORIGINS`
- `COOKIE_DOMAIN`
- `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_BUCKET`, `AWS_ENDPOINT`, `AWS_REGION`
- `AWS_PUBLIC_BASE_URL`

## Recommended storage
Supabase Storage through the S3-compatible endpoint:

- `AWS_ENDPOINT=https://<project-ref>.storage.supabase.co/storage/v1/s3`
- `AWS_PUBLIC_BASE_URL=https://<project-ref>.supabase.co/storage/v1/object/public/<bucket>`
- `AWS_REGION=<supabase-project-region>`

## Deploy
```powershell
cd cloudflare
npm install
npm run secrets:sync
npx wrangler deploy
```

## Notes
- `wrangler.jsonc` uses `modulearning01-api` as the worker name.
- The backend container is still single-instance because messenger uses the in-memory STOMP broker.
