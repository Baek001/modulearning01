export function isFirebasePlatformEnabled() {
    return String(import.meta.env.VITE_PLATFORM_BACKEND || '').trim().toLowerCase() === 'firebase';
}

export function firebaseEmulatorsEnabled() {
    return String(import.meta.env.VITE_FIREBASE_USE_EMULATORS || '').trim().toLowerCase() === 'true';
}

export function firebaseFunctionRegion() {
    return import.meta.env.VITE_FIREBASE_FUNCTIONS_REGION || 'asia-northeast3';
}

export function firebaseLoginDomain() {
    return String(import.meta.env.VITE_FIREBASE_LOGIN_DOMAIN || '').trim();
}

export function firebaseUserCollection() {
    return String(import.meta.env.VITE_FIREBASE_USER_COLLECTION || 'users').trim() || 'users';
}

export function resolveFirebaseLoginEmail(identifier) {
    const trimmed = String(identifier || '').trim();

    if (!trimmed) {
        return '';
    }

    if (trimmed.includes('@')) {
        return trimmed;
    }

    const defaultDomain = firebaseLoginDomain();
    if (!defaultDomain) {
        throw new Error('Firebase 로그인은 이메일 형식 계정이 필요합니다. 또는 VITE_FIREBASE_LOGIN_DOMAIN 값을 설정해 주세요.');
    }

    return `${trimmed}@${defaultDomain}`;
}

export function createFirebaseConfig() {
    return {
        apiKey: import.meta.env.VITE_FIREBASE_API_KEY || '',
        authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || '',
        projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || '',
        storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || '',
        messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || '',
        appId: import.meta.env.VITE_FIREBASE_APP_ID || '',
    };
}
