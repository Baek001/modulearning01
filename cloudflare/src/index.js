import { Container } from "@cloudflare/containers";

const BACKEND_PREFIXES = [
  "/rest",
  "/common",
  "/mail",
  "/chat",
  "/file",
  "/folder",
  "/actuator",
];

function isBackendRequest(pathname) {
  if (pathname === "/starworks-groupware-websocket") {
    return true;
  }
  return BACKEND_PREFIXES.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`));
}

function pickDefined(values) {
  return Object.fromEntries(
    Object.entries(values).filter(([, value]) => typeof value === "string" && value.length > 0),
  );
}

function backendEnvVars(env) {
  return pickDefined({
    DB_URL: env.DB_URL,
    DB_USERNAME: env.DB_USERNAME,
    DB_PASSWORD: env.DB_PASSWORD,
    DB_DRIVER: env.DB_DRIVER || "org.postgresql.Driver",
    JWT_SECRET_KEY: env.JWT_SECRET_KEY,
    APP_FRONTEND_BASE_URL: env.APP_FRONTEND_BASE_URL,
    CORS_ALLOW_ORIGINS: env.CORS_ALLOW_ORIGINS,
    COOKIE_DOMAIN: env.COOKIE_DOMAIN,
    COOKIE_SECURE: env.COOKIE_SECURE || "true",
    COOKIE_SAME_SITE: env.COOKIE_SAME_SITE || "Lax",
    COOKIE_MAX_AGE: env.COOKIE_MAX_AGE || "86400",
    FILE_STORAGE_MODE: env.FILE_STORAGE_MODE || "s3",
    FILE_STORAGE_URL: env.FILE_STORAGE_URL || "/starworks_medias",
    FILE_STORAGE_PATH: env.FILE_STORAGE_PATH,
    FORWARD_HEADERS_STRATEGY: env.FORWARD_HEADERS_STRATEGY || "framework",
    AWS_ACCESS_KEY: env.AWS_ACCESS_KEY,
    AWS_SECRET_KEY: env.AWS_SECRET_KEY,
    AWS_REGION: env.AWS_REGION || "auto",
    AWS_BUCKET: env.AWS_BUCKET,
    AWS_ENDPOINT: env.AWS_ENDPOINT,
    AWS_PUBLIC_BASE_URL: env.AWS_PUBLIC_BASE_URL,
    AWS_PATH_STYLE_ACCESS: env.AWS_PATH_STYLE_ACCESS || "true",
    APP_LOG_LEVEL: env.APP_LOG_LEVEL || "info",
    REQUEST_MAPPING_LOG_LEVEL: env.REQUEST_MAPPING_LOG_LEVEL || "warn",
    SPRING_VIEW_LOG_LEVEL: env.SPRING_VIEW_LOG_LEVEL || "warn",
    SECURITY_LOG_LEVEL: env.SECURITY_LOG_LEVEL || "info",
    SPRING_AI_LOG_LEVEL: env.SPRING_AI_LOG_LEVEL || "warn",
    SPRING_AI_VERTEX_AI_GEMINI_PROJECT_ID: env.SPRING_AI_VERTEX_AI_GEMINI_PROJECT_ID,
    SPRING_AI_VERTEX_AI_GEMINI_LOCATION: env.SPRING_AI_VERTEX_AI_GEMINI_LOCATION,
    SPRING_AI_VERTEX_AI_EMBEDDING_PROJECT_ID: env.SPRING_AI_VERTEX_AI_EMBEDDING_PROJECT_ID,
    SPRING_AI_VERTEX_AI_EMBEDDING_LOCATION: env.SPRING_AI_VERTEX_AI_EMBEDDING_LOCATION,
    SERVER_PORT: env.BACKEND_INTERNAL_PORT || "18080",
  });
}

export class BackendContainer extends Container {
  defaultPort = 18080;
  sleepAfter = "20m";
  envVars = backendEnvVars(this.env);
}

async function forwardToBackend(request, env) {
  const url = new URL(request.url);
  const headers = new Headers(request.headers);
  headers.set("x-forwarded-host", url.host);
  headers.set("x-forwarded-proto", url.protocol.replace(":", ""));

  const backendRequest = new Request(request, { headers });
  const containerName = env.BACKEND_CONTAINER_NAME || "primary";
  return env.BACKEND.getByName(containerName).fetch(backendRequest);
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (isBackendRequest(url.pathname)) {
      return forwardToBackend(request, env);
    }
    return env.ASSETS.fetch(request);
  },
};
