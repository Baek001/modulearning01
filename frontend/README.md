# Modulearning frontend

## Commands
```powershell
npm install
npm run dev
npm run lint
npm run build
```

## Env file
Copy `frontend/.env.example` to `frontend/.env.local` for local overrides.

- `VITE_APP_BASE_URL=https://modulearning01.pages.dev`
- `VITE_API_BASE_URL=` for same-origin routing, or `https://modulearning01-api.<your-cloudflare-subdomain>.workers.dev` for direct calls
- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_PUBLISHABLE_KEY`

Leave `VITE_API_BASE_URL` blank for same-origin routing, or set it to the `modulearning01-api` worker origin when Pages must call the backend directly.
