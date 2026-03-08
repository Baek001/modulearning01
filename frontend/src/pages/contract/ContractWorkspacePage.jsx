import { useEffect, useEffectEvent, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { contractAPI } from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';

const SECTIONS = [
    ['active', '진행 중'],
    ['completed', '완료'],
    ['archive', '만료/취소'],
    ['templates', '양식'],
    ['requests', '양식 신청'],
    ['settings', '설정'],
];

const STATUS_META = {
    draft: ['작성 중', 'badge-gray'],
    ready: ['발송 준비', 'badge-gray'],
    sent: ['발송됨', 'badge-blue'],
    in_progress: ['서명 진행', 'badge-orange'],
    completed: ['완료', 'badge-green'],
    expired: ['만료', 'badge-red'],
    cancelled: ['취소', 'badge-gray'],
};

function fmt(value) {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '-';
    return date.toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

function parseBatchRows(rowsText) {
    return String(rowsText || '')
        .split('\n')
        .map((line) => line.trim())
        .filter(Boolean)
        .map((line) => {
            const [name = '', email = '', tel = ''] = line.split(',').map((item) => item.trim());
            return {
                signerNm: name,
                signerEmail: email,
                signerTelno: tel,
                recipientName: name,
                recipientEmail: email,
                recipientTelno: tel,
                contractTitle: name ? `${name} 계약 건` : '',
                fieldValues: { recipientName: name, recipientEmail: email, recipientTelno: tel },
            };
        });
}

export default function ContractWorkspacePage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const isAdmin = String(user?.userRole || '').includes('ADMIN');
    const [section, setSection] = useState('active');
    const [dashboard, setDashboard] = useState({ counts: {}, recentContracts: [] });
    const [contracts, setContracts] = useState({ items: [] });
    const [templates, setTemplates] = useState([]);
    const [requests, setRequests] = useState([]);
    const [selectedId, setSelectedId] = useState('');
    const [detail, setDetail] = useState(null);
    const [loading, setLoading] = useState(true);
    const [detailLoading, setDetailLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    const [modalType, setModalType] = useState('');
    const [form, setForm] = useState({
        templateId: '',
        contractTitle: '',
        contractMessage: '',
        sendTypeCd: 'remote',
        signingFlowCd: 'parallel',
        expiresDt: '',
        signerNm: '',
        signerEmail: '',
        signerTelno: '',
        rowsText: '홍길동,hong@example.com,010-1111-2222',
        batchTitle: '',
        requestTitle: '',
        requestNote: '',
        sourceFile: null,
        markedFile: null,
        sealFile: null,
    });

    const listItems = useMemo(() => (
        section === 'templates' ? templates : section === 'requests' ? requests : contracts.items || []
    ), [section, templates, requests, contracts.items]);

    const bootstrapEffect = useEffectEvent(async () => {
        await bootstrap();
    });

    const loadSectionEffect = useEffectEvent(async (nextSection) => {
        await loadSection(nextSection, selectedId, false);
    });

    useEffect(() => {
        void bootstrapEffect();
    }, [isAdmin]);

    useEffect(() => {
        void loadSectionEffect(section);
    }, [section]);

    async function bootstrap() {
        setLoading(true);
        setError('');
        try {
            const [dashboardRes, templatesRes, requestsRes, settingsRes] = await Promise.all([
                contractAPI.dashboard(),
                contractAPI.templates(),
                contractAPI.templateRequests(),
                isAdmin ? contractAPI.companySettings().catch(() => ({ data: null })) : Promise.resolve({ data: null }),
            ]);
            setDashboard(dashboardRes.data || { counts: {}, recentContracts: [] });
            setTemplates(templatesRes.data || []);
            setRequests(requestsRes.data || []);
            if (section === 'settings') {
                setDetail(settingsRes.data || null);
            }
        } catch (requestError) {
            setError(requestError.response?.data?.message || '전자계약 데이터를 불러오지 못했습니다.');
        } finally {
            setLoading(false);
        }
    }

    async function loadSection(nextSection, preferredId = '', toggleLoading = true) {
        if (toggleLoading) setLoading(true);
        try {
            if (['active', 'completed', 'archive'].includes(nextSection)) {
                const response = await contractAPI.list({ section: nextSection });
                const nextContracts = response.data || { items: [] };
                setContracts(nextContracts);
                const nextId = preferredId || nextContracts.items?.[0]?.contractId || '';
                setSelectedId(String(nextId || ''));
                if (nextId) await openContractDetail(nextId);
                else setDetail(null);
            } else if (nextSection === 'templates') {
                const response = await contractAPI.templates();
                const nextTemplates = response.data || [];
                setTemplates(nextTemplates);
                const nextId = preferredId || nextTemplates[0]?.templateId || '';
                setSelectedId(String(nextId || ''));
                if (nextId) await openTemplateDetail(nextId);
                else setDetail(null);
            } else if (nextSection === 'requests') {
                const response = await contractAPI.templateRequests();
                const nextRequests = response.data || [];
                setRequests(nextRequests);
                const nextId = preferredId || nextRequests[0]?.requestId || '';
                setSelectedId(String(nextId || ''));
                setDetail(nextRequests.find((item) => String(item.requestId) === String(nextId)) || null);
            } else if (nextSection === 'settings') {
                const response = isAdmin ? await contractAPI.companySettings() : { data: null };
                setDetail(response.data || null);
            }
        } catch (requestError) {
            setError(requestError.response?.data?.message || '전자계약 목록을 불러오지 못했습니다.');
        } finally {
            if (toggleLoading) setLoading(false);
        }
    }

    async function openContractDetail(contractId) {
        setDetailLoading(true);
        try {
            const response = await contractAPI.detail(contractId);
            setDetail(response.data || null);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '계약 상세를 불러오지 못했습니다.');
        } finally {
            setDetailLoading(false);
        }
    }

    async function openTemplateDetail(templateId) {
        setDetailLoading(true);
        try {
            const response = await contractAPI.templateDetail(templateId);
            setDetail(response.data || null);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '양식 상세를 불러오지 못했습니다.');
        } finally {
            setDetailLoading(false);
        }
    }

    async function selectItem(nextId) {
        setSelectedId(String(nextId));
        if (['active', 'completed', 'archive'].includes(section)) await openContractDetail(nextId);
        else if (section === 'templates') await openTemplateDetail(nextId);
        else if (section === 'requests') setDetail(requests.find((item) => String(item.requestId) === String(nextId)) || null);
    }

    async function runContractAction(action, successText) {
        if (!detail?.contractId) return;
        setSaving(true);
        try {
            await action(detail.contractId);
            setMessage(successText);
            await loadSection(section, detail.contractId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '요청 처리에 실패했습니다.');
        } finally {
            setSaving(false);
        }
    }

    async function submitModal(event) {
        event.preventDefault();
        setSaving(true);
        setError('');
        setMessage('');
        try {
            if (modalType === 'contract') {
                const response = await contractAPI.create({
                    templateId: Number(form.templateId),
                    contractTitle: form.contractTitle,
                    contractMessage: form.contractMessage,
                    sendTypeCd: form.sendTypeCd,
                    signingFlowCd: form.signingFlowCd,
                    expiresDt: form.expiresDt ? new Date(form.expiresDt).toISOString() : null,
                    signers: [{ signerNm: form.signerNm, signerEmail: form.signerEmail, signerTelno: form.signerTelno }],
                });
                setMessage('계약 초안을 저장했습니다.');
                setModalType('');
                await loadSection('active', response.data?.contractId || '');
                setSection('active');
            } else if (modalType === 'batch') {
                await contractAPI.createBatch({
                    templateId: Number(form.templateId),
                    batchTitle: form.batchTitle,
                    contractTitle: form.contractTitle,
                    contractMessage: form.contractMessage,
                    rows: parseBatchRows(form.rowsText),
                });
                setMessage('대량 전송 작업을 생성했습니다.');
                setModalType('');
            } else if (modalType === 'request') {
                await contractAPI.createTemplateRequest(
                    { requestTitle: form.requestTitle, requestNote: form.requestNote },
                    { sourceFile: form.sourceFile, markedFile: form.markedFile, sealFile: form.sealFile },
                );
                setMessage('양식 신청을 등록했습니다.');
                setModalType('');
                await loadSection('requests');
                setSection('requests');
            }
        } catch (requestError) {
            setError(requestError.response?.data?.message || '저장에 실패했습니다.');
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="animate-slide-up">
            <div className="page-header">
                <div>
                    <h2>전자계약 서비스</h2>
                    <p>계약 진행, 양식 관리, 양식 신청을 현재 UI 안에서 처리합니다.</p>
                </div>
                <div className="page-header-actions">
                    <button type="button" className="btn btn-outline" onClick={() => navigate('/approval')}>전자결재로 돌아가기</button>
                    <button type="button" className="btn btn-primary" onClick={() => setModalType('contract')}>계약 작성</button>
                    <button type="button" className="btn btn-secondary" onClick={() => setModalType('batch')}>대량 전송</button>
                    <button type="button" className="btn btn-outline" onClick={() => setModalType('request')}>양식 신청</button>
                </div>
            </div>

            {error && <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}><div className="card-body" style={{ color: 'var(--danger)' }}>{error}</div></div>}
            {message && <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}><div className="card-body" style={{ color: 'var(--success)' }}>{message}</div></div>}

            <div className="contract-layout">
                <aside className="contract-rail card">
                    <div className="card-header"><h3>전자계약</h3></div>
                    <div className="card-body">
                        <div className="contract-count-grid">
                            <div className="contract-count-card"><strong>{dashboard.counts?.active || 0}</strong><span>진행 중</span></div>
                            <div className="contract-count-card"><strong>{dashboard.counts?.completed || 0}</strong><span>완료</span></div>
                            <div className="contract-count-card"><strong>{dashboard.counts?.templates || 0}</strong><span>양식</span></div>
                            <div className="contract-count-card"><strong>{dashboard.counts?.requests || 0}</strong><span>신청</span></div>
                        </div>
                        <div className="contract-nav-list">
                            {SECTIONS.filter(([value]) => isAdmin || value !== 'settings').map(([value, label]) => (
                                <button key={value} type="button" className={`contract-nav-item ${section === value ? 'active' : ''}`} onClick={() => setSection(value)}>{label}</button>
                            ))}
                        </div>
                    </div>
                </aside>

                <section className="contract-main">
                    <div className="card">
                        <div className="card-header"><h3>{SECTIONS.find(([value]) => value === section)?.[1] || '목록'}</h3></div>
                        <div className="card-body">
                            {loading ? <p>목록을 불러오는 중입니다.</p> : (
                                <div className="contract-list">
                                    {listItems.map((item) => (
                                        <button
                                            key={item.contractId || item.templateId || item.requestId}
                                            type="button"
                                            className={`contract-list-item ${String(selectedId) === String(item.contractId || item.templateId || item.requestId) ? 'active' : ''}`}
                                            onClick={() => selectItem(item.contractId || item.templateId || item.requestId)}
                                        >
                                            <div className="contract-list-item-top">
                                                <strong>{item.contractTitle || item.templateNm || item.requestTitle}</strong>
                                                {item.statusCd && <span className={`badge ${STATUS_META[item.statusCd]?.[1] || 'badge-gray'}`}>{STATUS_META[item.statusCd]?.[0] || item.statusCd}</span>}
                                            </div>
                                            <p>{item.templateNm || item.templateDesc || item.requestNote || item.contractMessage || '설명 없음'}</p>
                                            <div className="contract-list-meta">
                                                <span>{fmt(item.sentDt || item.lastChgDt || item.crtDt)}</span>
                                                {item.signedCount !== undefined && <span>{item.signedCount}/{item.totalSignerCount || 0} 서명</span>}
                                            </div>
                                        </button>
                                    ))}
                                    {listItems.length === 0 && <div className="empty-state"><div className="empty-icon">∅</div><h3>표시할 항목이 없습니다.</h3></div>}
                                </div>
                            )}
                        </div>
                    </div>
                </section>

                <aside className="contract-detail card">
                    <div className="card-header"><h3>상세</h3></div>
                    <div className="card-body">
                        {detailLoading && <p>상세를 불러오는 중입니다.</p>}
                        {!detailLoading && !detail && <p className="text-muted">왼쪽에서 항목을 선택해 주세요.</p>}
                        {!detailLoading && detail && ['active', 'completed', 'archive'].includes(section) && (
                            <div className="contract-detail-stack">
                                <span className={`badge ${STATUS_META[detail.statusCd]?.[1] || 'badge-gray'}`}>{STATUS_META[detail.statusCd]?.[0] || detail.statusCd}</span>
                                <h3>{detail.contractTitle}</h3>
                                <p>{detail.contractMessage || '설명 없음'}</p>
                                <div className="contract-inline-actions">
                                    <button type="button" className="btn btn-sm btn-primary" onClick={() => runContractAction((id) => contractAPI.send(id), '계약 링크를 발급했습니다.')}>발송</button>
                                    <button type="button" className="btn btn-sm btn-outline" onClick={() => runContractAction((id) => contractAPI.remind(id), '리마인드 링크를 발급했습니다.')}>리마인드</button>
                                    <button type="button" className="btn btn-sm btn-outline" onClick={() => runContractAction((id) => contractAPI.cancel(id, { reason: '워크스페이스에서 취소' }), '계약을 취소했습니다.')}>취소</button>
                                </div>
                                <div className="contract-stat-grid">
                                    <div><strong>{detail.templateNm || '-'}</strong><span>양식</span></div>
                                    <div><strong>{detail.sendTypeCd || '-'}</strong><span>발송 방식</span></div>
                                    <div><strong>{detail.signingFlowCd || '-'}</strong><span>서명 흐름</span></div>
                                    <div><strong>{fmt(detail.expiresDt)}</strong><span>만료</span></div>
                                </div>
                                <div className="contract-mini-list">
                                    {(detail.signers || []).map((signer) => (
                                        <div key={signer.signerId} className="contract-mini-item">
                                            <strong>{signer.signerOrder}. {signer.claimedNm || signer.signerNm}</strong>
                                            <span>{signer.signerEmail || signer.internalUserNm || '-'}</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                        {!detailLoading && detail && section === 'templates' && (
                            <div className="contract-detail-stack">
                                <h3>{detail.templateNm}</h3>
                                <p>{detail.templateDesc || '설명 없음'}</p>
                                <div className="contract-inline-actions">
                                    <button type="button" className="btn btn-sm btn-primary" onClick={() => navigate(`/approval/contracts/templates/${detail.templateId}`)}>디자이너 열기</button>
                                    {isAdmin && <button type="button" className="btn btn-sm btn-outline" onClick={async () => { await contractAPI.publishTemplate(detail.templateId, detail.currentVersion?.templateVersionId); await loadSection('templates', detail.templateId); }}>현재 버전 게시</button>}
                                </div>
                            </div>
                        )}
                        {!detailLoading && detail && section === 'requests' && (
                            <div className="contract-detail-stack">
                                <h3>{detail.requestTitle}</h3>
                                <p>{detail.requestNote || '메모 없음'}</p>
                                {isAdmin && (
                                    <div className="contract-inline-actions">
                                        <button type="button" className="btn btn-sm btn-primary" onClick={async () => { await contractAPI.approveTemplateRequest(detail.requestId, {}); await loadSection('requests', detail.requestId); }}>승인</button>
                                        <button type="button" className="btn btn-sm btn-outline" onClick={async () => { await contractAPI.rejectTemplateRequest(detail.requestId, {}); await loadSection('requests', detail.requestId); }}>반려</button>
                                    </div>
                                )}
                            </div>
                        )}
                        {!detailLoading && detail && section === 'settings' && <div className="contract-detail-stack"><h3>{detail.companyNm || '회사 설정'}</h3><p>{detail.senderNm || '-'} / {detail.senderEmail || '-'}</p></div>}
                    </div>
                </aside>
            </div>

            {modalType && (
                <div className="modal-overlay" onClick={() => setModalType('')}>
                    <div className="modal" onClick={(event) => event.stopPropagation()}>
                        <div className="modal-header"><h3>{modalType === 'contract' ? '계약 작성' : modalType === 'batch' ? '대량 전송' : '양식 신청'}</h3><button type="button" className="btn btn-ghost" onClick={() => setModalType('')}>닫기</button></div>
                        <form onSubmit={submitModal}>
                            <div className="modal-body">
                                {(modalType === 'contract' || modalType === 'batch') && <div className="form-group"><label className="form-label">양식</label><select className="form-select" value={form.templateId} onChange={(event) => setForm((prev) => ({ ...prev, templateId: event.target.value }))}>{templates.map((item) => <option key={item.templateId} value={item.templateId}>{item.templateNm}</option>)}</select></div>}
                                {modalType === 'contract' && <>
                                    <div className="form-group"><label className="form-label">계약 제목</label><input className="form-input" value={form.contractTitle} onChange={(event) => setForm((prev) => ({ ...prev, contractTitle: event.target.value }))} /></div>
                                    <div className="form-group"><label className="form-label">안내 문구</label><textarea className="form-input" rows="3" value={form.contractMessage} onChange={(event) => setForm((prev) => ({ ...prev, contractMessage: event.target.value }))} /></div>
                                    <div className="form-group"><label className="form-label">만료일시</label><input className="form-input" type="datetime-local" value={form.expiresDt} onChange={(event) => setForm((prev) => ({ ...prev, expiresDt: event.target.value }))} /></div>
                                    <div className="form-group"><label className="form-label">서명자명</label><input className="form-input" value={form.signerNm} onChange={(event) => setForm((prev) => ({ ...prev, signerNm: event.target.value }))} /></div>
                                    <div className="form-group"><label className="form-label">서명자 이메일</label><input className="form-input" value={form.signerEmail} onChange={(event) => setForm((prev) => ({ ...prev, signerEmail: event.target.value }))} /></div>
                                    <div className="form-group"><label className="form-label">서명자 전화번호</label><input className="form-input" value={form.signerTelno} onChange={(event) => setForm((prev) => ({ ...prev, signerTelno: event.target.value }))} /></div>
                                </>}
                                {modalType === 'batch' && <>
                                    <div className="form-group"><label className="form-label">작업 제목</label><input className="form-input" value={form.batchTitle} onChange={(event) => setForm((prev) => ({ ...prev, batchTitle: event.target.value }))} /></div>
                                    <div className="form-group"><label className="form-label">기본 계약 제목</label><input className="form-input" value={form.contractTitle} onChange={(event) => setForm((prev) => ({ ...prev, contractTitle: event.target.value }))} /></div>
                                    <div className="form-group"><label className="form-label">안내 문구</label><textarea className="form-input" rows="3" value={form.contractMessage} onChange={(event) => setForm((prev) => ({ ...prev, contractMessage: event.target.value }))} /></div>
                                    <div className="form-group"><label className="form-label">대상 목록</label><textarea className="form-input" rows="6" value={form.rowsText} onChange={(event) => setForm((prev) => ({ ...prev, rowsText: event.target.value }))} /></div>
                                </>}
                                {modalType === 'request' && <>
                                    <div className="form-group"><label className="form-label">신청 제목</label><input className="form-input" value={form.requestTitle} onChange={(event) => setForm((prev) => ({ ...prev, requestTitle: event.target.value }))} /></div>
                                    <div className="form-group"><label className="form-label">신청 메모</label><textarea className="form-input" rows="3" value={form.requestNote} onChange={(event) => setForm((prev) => ({ ...prev, requestNote: event.target.value }))} /></div>
                                    <div className="form-group"><label className="form-label">원본 파일</label><input className="form-input" type="file" onChange={(event) => setForm((prev) => ({ ...prev, sourceFile: event.target.files?.[0] || null }))} /></div>
                                    <div className="form-group"><label className="form-label">표시 PDF</label><input className="form-input" type="file" onChange={(event) => setForm((prev) => ({ ...prev, markedFile: event.target.files?.[0] || null }))} /></div>
                                    <div className="form-group"><label className="form-label">직인 PNG</label><input className="form-input" type="file" onChange={(event) => setForm((prev) => ({ ...prev, sealFile: event.target.files?.[0] || null }))} /></div>
                                </>}
                            </div>
                            <div className="modal-footer"><button type="button" className="btn btn-outline" onClick={() => setModalType('')}>취소</button><button type="submit" className="btn btn-primary" disabled={saving}>저장</button></div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
