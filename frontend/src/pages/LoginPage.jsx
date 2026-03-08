import { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

export default function LoginPage() {
    const { login } = useAuth();
    const navigate = useNavigate();
    const [userId, setUserId] = useState('');
    const [userPw, setUserPw] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        if (!userId.trim() || !userPw.trim()) {
            setError('아이디와 비밀번호를 입력해 주세요.');
            return;
        }
        setError('');
        setLoading(true);
        try {
            await login(userId, userPw);
            navigate('/', { replace: true });
        } catch (err) {
            setError(err.response?.data?.message || '로그인에 실패했습니다. 아이디와 비밀번호를 확인해 주세요.');
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="login-container">
            <div className="login-card">
                <div className="login-logo">
                    <div className="logo-mark">ML</div>
                    <h2>모두의 러닝</h2>
                    <p>스마트 그룹웨어에 오신 것을 환영합니다</p>
                </div>

                <form onSubmit={handleSubmit}>
                    {error && (
                        <div className="alert alert-error" style={{ marginBottom: 'var(--spacing-md)' }}>
                            {error}
                        </div>
                    )}

                    <div className="form-group">
                        <label className="form-label" htmlFor="userId">아이디</label>
                        <input
                            id="userId"
                            className="form-input"
                            type="text"
                            placeholder="아이디를 입력해 주세요"
                            value={userId}
                            onChange={(e) => setUserId(e.target.value)}
                            autoComplete="username"
                            autoFocus
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="userPw">비밀번호</label>
                        <input
                            id="userPw"
                            className="form-input"
                            type="password"
                            placeholder="비밀번호를 입력해 주세요"
                            value={userPw}
                            onChange={(e) => setUserPw(e.target.value)}
                            autoComplete="current-password"
                        />
                    </div>

                    <button type="submit" className="btn btn-primary" disabled={loading}>
                        {loading ? '로그인 중...' : '로그인'}
                    </button>
                </form>

                <div style={{ marginTop: 'var(--spacing-lg)', textAlign: 'center', fontSize: 'var(--font-size-xs)', color: 'var(--gray-400)' }}>
                    © 2026 모두의 러닝. All rights reserved.
                </div>
            </div>
        </div>
    );
}
