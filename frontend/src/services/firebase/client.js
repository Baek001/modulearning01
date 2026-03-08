import { getApp, getApps, initializeApp } from 'firebase/app';
import { connectAuthEmulator, getAuth } from 'firebase/auth';
import { connectFirestoreEmulator, getFirestore } from 'firebase/firestore';
import { connectFunctionsEmulator, getFunctions } from 'firebase/functions';
import { connectStorageEmulator, getStorage } from 'firebase/storage';
import { createFirebaseConfig, firebaseEmulatorsEnabled, firebaseFunctionRegion, isFirebasePlatformEnabled } from './platform';

let emulatorBound = false;

function assertFirebaseEnabled() {
    if (!isFirebasePlatformEnabled()) {
        throw new Error('Firebase 플랫폼이 비활성화되어 있습니다. VITE_PLATFORM_BACKEND=firebase 로 설정해 주세요.');
    }
}

function assertFirebaseConfig() {
    const config = createFirebaseConfig();
    const requiredKeys = ['apiKey', 'authDomain', 'projectId', 'appId'];
    const missingKeys = requiredKeys.filter((key) => !config[key]);

    if (missingKeys.length > 0) {
        throw new Error(`Firebase 설정이 부족합니다: ${missingKeys.join(', ')}`);
    }

    return config;
}

function parseHostPort(rawValue, fallbackPort) {
    const value = String(rawValue || '').trim();
    if (!value) {
        return { host: '127.0.0.1', port: fallbackPort };
    }

    const [hostPart, portPart] = value.split(':');
    return {
        host: hostPart || '127.0.0.1',
        port: Number(portPart || fallbackPort),
    };
}

function bindEmulators(app) {
    if (!firebaseEmulatorsEnabled() || emulatorBound) {
        return;
    }

    const auth = getAuth(app);
    const firestore = getFirestore(app);
    const functions = getFunctions(app, firebaseFunctionRegion());
    const storage = getStorage(app);

    const authTarget = parseHostPort(import.meta.env.VITE_FIREBASE_AUTH_EMULATOR_HOST, 9099);
    const firestoreTarget = parseHostPort(import.meta.env.VITE_FIREBASE_FIRESTORE_EMULATOR_HOST, 8080);
    const functionsTarget = parseHostPort(import.meta.env.VITE_FIREBASE_FUNCTIONS_EMULATOR_HOST, 5001);
    const storageTarget = parseHostPort(import.meta.env.VITE_FIREBASE_STORAGE_EMULATOR_HOST, 9199);

    connectAuthEmulator(auth, `http://${authTarget.host}:${authTarget.port}`, { disableWarnings: true });
    connectFirestoreEmulator(firestore, firestoreTarget.host, firestoreTarget.port);
    connectFunctionsEmulator(functions, functionsTarget.host, functionsTarget.port);
    connectStorageEmulator(storage, storageTarget.host, storageTarget.port);
    emulatorBound = true;
}

export function getFirebaseApp() {
    assertFirebaseEnabled();

    const app = getApps().length ? getApp() : initializeApp(assertFirebaseConfig());
    bindEmulators(app);
    return app;
}

export function getFirebaseAuth() {
    return getAuth(getFirebaseApp());
}

export function getFirebaseDb() {
    return getFirestore(getFirebaseApp());
}

export function getFirebaseFunctions() {
    return getFunctions(getFirebaseApp(), firebaseFunctionRegion());
}

export function getFirebaseStorage() {
    return getStorage(getFirebaseApp());
}
