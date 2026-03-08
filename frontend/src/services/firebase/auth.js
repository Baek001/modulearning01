import {
    EmailAuthProvider,
    onAuthStateChanged,
    reauthenticateWithCredential,
    signInWithEmailAndPassword,
    signOut,
    updatePassword,
} from 'firebase/auth';
import { doc, getDoc, serverTimestamp, setDoc } from 'firebase/firestore';
import { STORAGE_KEYS } from '../api';
import { getFirebaseAuth, getFirebaseDb } from './client';
import { callFirebaseFunction } from './functions';
import { firebaseUserCollection, resolveFirebaseLoginEmail } from './platform';

function extractUserIdFromEmail(email = '') {
    return String(email || '').split('@')[0] || '';
}

function normalizeFirebaseUser(profile = {}, authUser = null, fallbackUserId = '') {
    const authEmail = authUser?.email || '';
    const resolvedUserId = profile.userId || fallbackUserId || extractUserIdFromEmail(authEmail) || authUser?.uid || '';

    return {
        userId: resolvedUserId,
        userNm: profile.userNm || authUser?.displayName || resolvedUserId || '스타웍스 사용자',
        deptNm: profile.deptNm || '',
        jbgdNm: profile.jbgdNm || '',
        userEmail: profile.userEmail || authEmail,
        hireYmd: profile.hireYmd || '',
        userRole: profile.userRole || '',
        workSttsCd: profile.workSttsCd || '',
        extTel: profile.extTel || '',
        userTelno: profile.userTelno || '',
        profileImageUrl: profile.profileImageUrl || '',
        profileImagePath: profile.profileImagePath || '',
        firebaseUid: authUser?.uid || '',
    };
}

function mapFirebaseAuthError(error) {
    const code = error?.code || '';

    switch (code) {
        case 'auth/invalid-credential':
        case 'auth/user-not-found':
        case 'auth/wrong-password':
            return new Error('아이디 또는 비밀번호를 확인해 주세요.');
        case 'auth/invalid-email':
            return new Error('Firebase 로그인용 이메일 형식이 올바르지 않습니다.');
        case 'auth/too-many-requests':
            return new Error('로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.');
        default:
            return error instanceof Error ? error : new Error('Firebase 로그인에 실패했습니다.');
    }
}

async function ensureProfileDocument(authUser, fallbackUserId = '') {
    const profileRef = doc(getFirebaseDb(), firebaseUserCollection(), authUser.uid);
    await setDoc(profileRef, {
        userId: fallbackUserId || extractUserIdFromEmail(authUser.email),
        userNm: authUser.displayName || fallbackUserId || extractUserIdFromEmail(authUser.email),
        userEmail: authUser.email || '',
        lastLoginAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
    }, { merge: true });
}

async function readProfileDocument(authUser, fallbackUserId = '') {
    const profileRef = doc(getFirebaseDb(), firebaseUserCollection(), authUser.uid);
    const snapshot = await getDoc(profileRef);

    if (!snapshot.exists()) {
        await ensureProfileDocument(authUser, fallbackUserId);
        return normalizeFirebaseUser({}, authUser, fallbackUserId);
    }

    return normalizeFirebaseUser(snapshot.data(), authUser, fallbackUserId);
}

async function resolveSessionProfile(authUser, fallbackUserId = '') {
    if (!authUser) {
        return null;
    }

    try {
        const sessionData = await callFirebaseFunction('sessionProfile');
        return normalizeFirebaseUser(sessionData, authUser, fallbackUserId);
    } catch {
        return readProfileDocument(authUser, fallbackUserId);
    }
}

export const firebaseAuthService = {
    observe(onNext, onError = () => { }) {
        return onAuthStateChanged(
            getFirebaseAuth(),
            async (authUser) => {
                try {
                    const nextUser = await resolveSessionProfile(authUser);
                    onNext(nextUser);
                } catch (error) {
                    localStorage.removeItem(STORAGE_KEYS.user);
                    onError(error);
                }
            },
            onError
        );
    },

    async login(identifier, password) {
        try {
            const email = resolveFirebaseLoginEmail(identifier);
            const credential = await signInWithEmailAndPassword(getFirebaseAuth(), email, password);
            const nextUser = await resolveSessionProfile(credential.user, identifier);
            localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(nextUser));
            return nextUser;
        } catch (error) {
            throw mapFirebaseAuthError(error);
        }
    },

    async logout() {
        await signOut(getFirebaseAuth());
        localStorage.removeItem(STORAGE_KEYS.user);
    },

    async session() {
        return resolveSessionProfile(getFirebaseAuth().currentUser);
    },

    async changePassword(currentPassword, newPassword) {
        const auth = getFirebaseAuth();
        const currentUser = auth.currentUser;

        if (!currentUser?.email) {
            throw new Error('비밀번호를 변경할 계정을 찾을 수 없습니다.');
        }

        const credential = EmailAuthProvider.credential(currentUser.email, currentPassword);
        await reauthenticateWithCredential(currentUser, credential);
        await updatePassword(currentUser, newPassword);
    },
};
