import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { myPageAPI, STORAGE_KEYS } from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';

export default function MyPage() {
    const navigate = useNavigate();
    const { user, logout } = useAuth();
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [savingProfile, setSavingProfile] = useState(false);
    const [savingPassword, setSavingPassword] = useState(false);
    const [error, setError] = useState('');
    const [notice, setNotice] = useState('');
    const [profileForm, setProfileForm] = useState({
        userNm: '',
        userEmail: '',
        userTelno: '',
        extTel: '',
        profileImage: null,
    });
    const [passwordForm, setPasswordForm] = useState({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
    });

    useEffect(() => {
        loadProfile();
    }, []);

    const currentUser = profile || user || {};
    const avatarLabel = (currentUser.userNm || currentUser.userId || '?').charAt(0);
    const selectedFileName = profileForm.profileImage?.name || '선택된 파일이 없습니다.';
    const infoRows = useMemo(
        () => ([
            ['아이디', currentUser.userId || '-'],
            ['권한', currentUser.userRole || '-'],
            ['입사일', currentUser.hireYmd || '-'],
            ['부서', currentUser.deptNm || '-'],
            ['직급', currentUser.jbgdNm || '-'],
        ]),
        [currentUser]
    );

    async function loadProfile() {
        setLoading(true);
        setError('');

        try {
            const response = await myPageAPI.profile();
            const nextProfile = response.data;
            setProfile(nextProfile);
            setProfileForm({
                userNm: nextProfile.userNm || '',
                userEmail: nextProfile.userEmail || '',
                userTelno: nextProfile.userTelno || '',
                extTel: nextProfile.extTel || '',
                profileImage: null,
            });
        } catch (requestError) {
            setError(requestError.response?.data?.message || '마이페이지 정보를 불러오지 못했습니다.');
        } finally {
            setLoading(false);
        }
    }

    async function handleProfileSubmit(event) {
        event.preventDefault();
        setSavingProfile(true);
        setError('');
        setNotice('');

        try {
            const response = await myPageAPI.updateProfile(profileForm);
            const nextProfile = response.data;
            setProfile(nextProfile);
            setProfileForm({
                userNm: nextProfile.userNm || '',
                userEmail: nextProfile.userEmail || '',
                userTelno: nextProfile.userTelno || '',
                extTel: nextProfile.extTel || '',
                profileImage: null,
            });
            setNotice('프로필 정보를 저장했습니다.');

            const stored = localStorage.getItem(STORAGE_KEYS.user);
            if (stored) {
                try {
                    const parsed = JSON.parse(stored);
                    localStorage.setItem(STORAGE_KEYS.user, JSON.stringify({
                        ...parsed,
                        userNm: nextProfile?.userNm || parsed.userNm,
                        userEmail: nextProfile?.userEmail || parsed.userEmail,
                        userTelno: nextProfile?.userTelno || parsed.userTelno,
                        extTel: nextProfile?.extTel || parsed.extTel,
                    }));
                } catch {
                    // Ignore malformed local storage.
                }
            }
        } catch (requestError) {
            setError(requestError.response?.data?.message || '프로필 저장에 실패했습니다.');
        } finally {
            setSavingProfile(false);
        }
    }

    async function handlePasswordSubmit(event) {
        event.preventDefault();

        if (!passwordForm.currentPassword || !passwordForm.newPassword) {
            setError('현재 비밀번호와 새 비밀번호를 입력하세요.');
            return;
        }

        if (passwordForm.newPassword !== passwordForm.confirmPassword) {
            setError('새 비밀번호와 확인 값이 일치하지 않습니다.');
            return;
        }

        setSavingPassword(true);
        setError('');
        setNotice('');

        try {
            await myPageAPI.changePassword({
                currentPassword: passwordForm.currentPassword,
                newPassword: passwordForm.newPassword,
            });
            await logout();
            navigate('/login');
        } catch (requestError) {
            setError(requestError.response?.data?.message || '비밀번호 변경에 실패했습니다.');
        } finally {
            setSavingPassword(false);
        }
    }

    return (
        <div className="animate-slide-up">
            <div className="page-header">
                <div>
                    <h2>마이페이지</h2>
                    <p>프로필과 비밀번호를 현재 계정 정보에 맞춰 안전하게 관리합니다.</p>
                </div>
            </div>

            {error && (
                <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
                    <div className="card-body" style={{ color: 'var(--danger)' }}>{error}</div>
                </div>
            )}
            {notice && (
                <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
                    <div className="card-body" style={{ color: 'var(--success)' }}>{notice}</div>
                </div>
            )}

            <div className="grid-2" style={{ alignItems: 'start' }}>
                <div className="card">
                    <div className="card-body" style={{ padding: 'var(--spacing-2xl)' }}>
                        <div style={{ display: 'grid', gap: 'var(--spacing-lg)' }}>
                            <div style={{ textAlign: 'center' }}>
                                <div
                                    className="avatar avatar-lg"
                                    style={{
                                        width: '96px',
                                        height: '96px',
                                        borderRadius: '28px',
                                        margin: '0 auto var(--spacing-md)',
                                        background: 'linear-gradient(135deg, #0f766e, #0284c7)',
                                        fontSize: '40px',
                                    }}
                                >
                                    {avatarLabel}
                                </div>
                                <h3 style={{ fontSize: 'var(--font-size-xl)', fontWeight: 700, marginBottom: 'var(--spacing-xs)' }}>
                                    {loading ? '불러오는 중...' : currentUser.userNm || '-'}
                                </h3>
                                <p style={{ color: 'var(--gray-500)' }}>
                                    {(currentUser.deptNm || '-') + ' / ' + (currentUser.jbgdNm || '-')}
                                </p>
                            </div>

                            <div className="form-section">
                                <div className="form-section-title">계정 정보</div>
                                <div className="form-grid">
                                    {infoRows.map(([label, value]) => (
                                        <div key={label} className="form-group">
                                            <label className="form-label">{label}</label>
                                            <div className="form-static">{value}</div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="card">
                    <div className="card-header">
                        <h3>프로필 수정</h3>
                    </div>
                    <div className="card-body">
                        <form className="form-shell" onSubmit={handleProfileSubmit}>
                            <div className="form-section">
                                <div className="form-grid">
                                    <div className="form-group">
                                        <label className="form-label" htmlFor="mypage-user-name">이름</label>
                                        <input
                                            id="mypage-user-name"
                                            type="text"
                                            className="form-input"
                                            value={profileForm.userNm}
                                            onChange={(event) => setProfileForm((current) => ({ ...current, userNm: event.target.value }))}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label" htmlFor="mypage-user-email">이메일</label>
                                        <input
                                            id="mypage-user-email"
                                            type="email"
                                            className="form-input"
                                            value={profileForm.userEmail}
                                            onChange={(event) => setProfileForm((current) => ({ ...current, userEmail: event.target.value }))}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label" htmlFor="mypage-user-phone">휴대폰</label>
                                        <input
                                            id="mypage-user-phone"
                                            type="text"
                                            className="form-input"
                                            value={profileForm.userTelno}
                                            onChange={(event) => setProfileForm((current) => ({ ...current, userTelno: event.target.value }))}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label" htmlFor="mypage-user-ext">내선 번호</label>
                                        <input
                                            id="mypage-user-ext"
                                            type="text"
                                            className="form-input"
                                            value={profileForm.extTel}
                                            onChange={(event) => setProfileForm((current) => ({ ...current, extTel: event.target.value }))}
                                        />
                                    </div>
                                    <div className="form-group form-span-2">
                                        <label className="form-label">프로필 이미지</label>
                                        <div className="form-file">
                                            <label className="form-file-control" htmlFor="mypage-profile-image">
                                                <span className="form-file-button">파일 선택</span>
                                                <span className="form-file-name">{selectedFileName}</span>
                                            </label>
                                            <input
                                                id="mypage-profile-image"
                                                className="form-file-input"
                                                type="file"
                                                accept="image/*"
                                                onChange={(event) => setProfileForm((current) => ({ ...current, profileImage: event.target.files?.[0] || null }))}
                                            />
                                            <div className="form-helper">이미지 파일만 업로드할 수 있습니다.</div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="form-actions">
                                <button type="submit" className="btn btn-primary" disabled={savingProfile || loading}>
                                    {savingProfile ? '저장 중...' : '프로필 저장'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>

            <div className="card" style={{ marginTop: 'var(--spacing-md)' }}>
                <div className="card-header">
                    <h3>비밀번호 변경</h3>
                </div>
                <div className="card-body">
                    <form className="form-shell" onSubmit={handlePasswordSubmit}>
                        <div className="form-section">
                            <div className="form-grid">
                                <div className="form-group">
                                    <label className="form-label" htmlFor="mypage-current-password">현재 비밀번호</label>
                                    <input
                                        id="mypage-current-password"
                                        type="password"
                                        className="form-input"
                                        value={passwordForm.currentPassword}
                                        onChange={(event) => setPasswordForm((current) => ({ ...current, currentPassword: event.target.value }))}
                                    />
                                </div>
                                <div className="form-group">
                                    <label className="form-label" htmlFor="mypage-new-password">새 비밀번호</label>
                                    <input
                                        id="mypage-new-password"
                                        type="password"
                                        className="form-input"
                                        value={passwordForm.newPassword}
                                        onChange={(event) => setPasswordForm((current) => ({ ...current, newPassword: event.target.value }))}
                                    />
                                </div>
                                <div className="form-group form-span-2">
                                    <label className="form-label" htmlFor="mypage-confirm-password">새 비밀번호 확인</label>
                                    <input
                                        id="mypage-confirm-password"
                                        type="password"
                                        className="form-input"
                                        value={passwordForm.confirmPassword}
                                        onChange={(event) => setPasswordForm((current) => ({ ...current, confirmPassword: event.target.value }))}
                                    />
                                    <div className="form-helper">비밀번호를 변경하면 보안을 위해 다시 로그인합니다.</div>
                                </div>
                            </div>
                        </div>

                        <div className="form-actions">
                            <button type="submit" className="btn btn-secondary" disabled={savingPassword || loading}>
                                {savingPassword ? '변경 중...' : '비밀번호 변경 후 재로그인'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}
