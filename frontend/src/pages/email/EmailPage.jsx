import { useEffect, useMemo, useState } from 'react';
import { emailAPI } from '../../services/api';

const FOLDERS = [
    { key: 'inbox', code: 'G101', label: '받은편지함' },
    { key: 'sent', code: 'G102', label: '보낸편지함' },
    { key: 'draft', code: 'G103', label: '임시보관함' },
    { key: 'important', code: 'G104', label: '중요 메일' },
    { key: 'trash', code: 'G105', label: '휴지통' },
];

function formatDate(value) {
    if (!value) {
        return '-';
    }

    return new Date(value).toLocaleString('ko-KR', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    });
}

export default function EmailPage() {
    const [activeFolder, setActiveFolder] = useState('inbox');
    const [counts, setCounts] = useState({});
    const [emails, setEmails] = useState([]);
    const [selectedEmailId, setSelectedEmailId] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const selectedFolder = useMemo(
        () => FOLDERS.find((folder) => folder.key === activeFolder) || FOLDERS[0],
        [activeFolder]
    );

    const selectedEmail = emails.find((email) => email.emailContId === selectedEmailId) || null;

    useEffect(() => {
        loadCounts();
    }, []);

    useEffect(() => {
        loadEmails(selectedFolder.code);
    }, [selectedFolder.code]);

    async function loadCounts() {
        try {
            const response = await emailAPI.counts();
            setCounts(response.data || {});
        } catch {
            setCounts({});
        }
    }

    async function loadEmails(mailboxTypeCd) {
        setLoading(true);
        setError('');

        try {
            const response = await emailAPI.list(mailboxTypeCd);
            const nextEmails = response.data?.emailList || [];
            setEmails(nextEmails);
            setSelectedEmailId(nextEmails[0]?.emailContId || '');
        } catch (err) {
            setError(err.response?.data?.message || '메일 목록을 불러오지 못했습니다.');
        } finally {
            setLoading(false);
        }
    }

    async function handleToggleImportance(emailContId) {
        try {
            await emailAPI.toggleImportance(emailContId);
            await Promise.all([loadCounts(), loadEmails(selectedFolder.code)]);
        } catch (err) {
            setError(err.response?.data?.message || '중요 메일 표시 변경에 실패했습니다.');
        }
    }

    async function handleDeleteSelected() {
        if (!selectedEmail) {
            return;
        }

        try {
            if (selectedFolder.code === 'G105') {
                await emailAPI.restoreSelected([selectedEmail.emailContId]);
            } else {
                await emailAPI.deleteSelected([selectedEmail.emailContId], selectedFolder.code);
            }
            await Promise.all([loadCounts(), loadEmails(selectedFolder.code)]);
        } catch (err) {
            setError(err.response?.data?.message || '메일 처리에 실패했습니다.');
        }
    }

    const folderCounts = {
        inbox: counts.inboxCount || 0,
        sent: counts.sentCount || 0,
        draft: counts.draftsCount || 0,
        important: counts.importantCount || 0,
        trash: counts.trashCount || 0,
    };

    return (
        <div className="animate-slide-up">
            <div className="page-header" style={{ marginBottom: 'var(--spacing-md)' }}>
                <div>
                    <h2>이메일</h2>
                    <p>베타 화면입니다. 폴더별 목록과 기본 정리는 실데이터로 동작합니다.</p>
                </div>
                <div className="page-header-actions">
                    <span className="badge badge-orange">Beta</span>
                </div>
            </div>

            {error && (
                <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
                    <div className="card-body" style={{ color: 'var(--danger)' }}>{error}</div>
                </div>
            )}

            <div className="email-layout">
                <div className="email-folders">
                    {FOLDERS.map((folder) => (
                        <div
                            key={folder.key}
                            className={`email-folder-item ${activeFolder === folder.key ? 'active' : ''}`}
                            onClick={() => setActiveFolder(folder.key)}
                        >
                            <span>{folder.label}</span>
                            {folderCounts[folder.key] > 0 && <span className="folder-count">{folderCounts[folder.key]}</span>}
                        </div>
                    ))}
                </div>

                <div className="email-list">
                    {emails.length === 0 && !loading ? (
                        <div className="empty-state" style={{ minHeight: '220px' }}>
                            <div className="empty-icon">-</div>
                            <h3>{selectedFolder.label}이 비어 있습니다.</h3>
                            <p>데이터가 쌓이면 목록과 상세가 이 화면에 표시됩니다.</p>
                        </div>
                    ) : (
                        emails.map((email) => (
                            <div
                                key={email.emailContId}
                                className={`email-item ${email.readYn === 'N' ? 'unread' : ''} ${selectedEmailId === email.emailContId ? 'active' : ''}`}
                                onClick={() => setSelectedEmailId(email.emailContId)}
                            >
                                <div className="email-item-from">{email.senderName || email.userId || '-'}</div>
                                <div className="email-item-subject">{email.subject || '(제목 없음)'}</div>
                                <div className="email-item-preview">{email.content || '내용 없음'}</div>
                                <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--gray-400)', marginTop: '4px' }}>
                                    {formatDate(email.sendDate)}
                                </div>
                            </div>
                        ))
                    )}
                </div>

                <div className="email-detail">
                    {selectedEmail ? (
                        <div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 'var(--spacing-sm)', marginBottom: 'var(--spacing-md)' }}>
                                <h2 style={{ fontSize: 'var(--font-size-xl)', fontWeight: 700 }}>{selectedEmail.subject || '(제목 없음)'}</h2>
                                <span className="badge badge-orange">Beta</span>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-sm)', marginBottom: 'var(--spacing-lg)', paddingBottom: 'var(--spacing-md)', borderBottom: '1px solid var(--gray-100)' }}>
                                <div>
                                    <div style={{ fontWeight: 600, fontSize: 'var(--font-size-sm)' }}>{selectedEmail.senderName || selectedEmail.userId || '-'}</div>
                                    <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--gray-400)' }}>{formatDate(selectedEmail.sendDate)}</div>
                                </div>
                            </div>
                            <p style={{ lineHeight: 1.8, color: 'var(--gray-700)', whiteSpace: 'pre-wrap' }}>{selectedEmail.content || '내용이 없습니다.'}</p>
                            <div style={{ marginTop: 'var(--spacing-xl)', display: 'flex', gap: 'var(--spacing-sm)' }}>
                                <button className="btn btn-outline" onClick={() => handleToggleImportance(selectedEmail.emailContId)}>중요 표시</button>
                                <button className="btn btn-secondary" onClick={handleDeleteSelected}>
                                    {selectedFolder.code === 'G105' ? '복원' : '삭제'}
                                </button>
                            </div>
                        </div>
                    ) : (
                        <div className="empty-state">
                            <div className="empty-icon">-</div>
                            <h3>메일을 선택하세요.</h3>
                            <p>실제 메일 데이터가 있으면 이곳에서 내용을 확인할 수 있습니다.</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
