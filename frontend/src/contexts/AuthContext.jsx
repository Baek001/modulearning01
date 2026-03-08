import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { authAPI, STORAGE_KEYS } from '../services/api';

/* eslint-disable react-refresh/only-export-components */

const AuthContext = createContext(null);

function clearStoredUser() {
    localStorage.removeItem(STORAGE_KEYS.user);
}

function normalizeUser(data, fallbackUserId = '') {
    return {
        userId: data?.userId || fallbackUserId,
        userNm: data?.userNm || fallbackUserId || '모두의 러닝 사용자',
        deptNm: data?.deptNm || '',
        jbgdNm: data?.jbgdNm || '',
        userEmail: data?.userEmail || '',
        hireYmd: data?.hireYmd || '',
        userRole: data?.userRole || '',
        workSttsCd: data?.workSttsCd || '',
        extTel: data?.extTel || '',
    };
}

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let active = true;

        async function initializeAuth() {
            const stored = localStorage.getItem(STORAGE_KEYS.user);
            const shouldAttemptSession = Boolean(stored) || window.location.pathname !== '/login';
            let storedUserId = '';

            if (stored) {
                try {
                    const parsedUser = JSON.parse(stored);
                    storedUserId = parsedUser.userId || '';
                    setUser(parsedUser);
                } catch {
                    clearStoredUser();
                }
            }

            if (!shouldAttemptSession) {
                if (active) {
                    setLoading(false);
                }
                return;
            }

            try {
                const response = await authAPI.session();
                if (!active) {
                    return;
                }

                const nextUser = normalizeUser(response.data, storedUserId);
                localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(nextUser));
                setUser(nextUser);
            } catch {
                if (!active) {
                    return;
                }

                clearStoredUser();
                setUser(null);
            } finally {
                if (active) {
                    setLoading(false);
                }
            }
        }

        initializeAuth();

        return () => {
            active = false;
        };
    }, []);

    const login = async (userId, password) => {
        await authAPI.login(userId, password);
        const response = await authAPI.session();
        const nextUser = normalizeUser(response.data, userId);

        localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(nextUser));
        setUser(nextUser);

        return nextUser;
    };

    const logout = async () => {
        try {
            await authAPI.logout();
        } catch {
            // Ignore revoke errors during local cleanup.
        }

        clearStoredUser();
        setUser(null);
    };

    const value = useMemo(() => ({
        user,
        login,
        logout,
        loading,
    }), [user, loading]);

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
}

export const useAuth = () => useContext(AuthContext);
