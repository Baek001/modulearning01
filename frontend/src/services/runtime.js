function readEnv(name) {
    return String(import.meta.env[name] || '').trim();
}

export const appBaseUrl = readEnv('VITE_APP_BASE_URL');
export const apiBaseUrl = readEnv('VITE_API_BASE_URL');
export const supabaseUrl = readEnv('VITE_SUPABASE_URL');
export const supabasePublishableKey = readEnv('VITE_SUPABASE_PUBLISHABLE_KEY') || readEnv('VITE_SUPABASE_ANON_KEY');
