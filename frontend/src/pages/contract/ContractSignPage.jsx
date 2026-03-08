import { useEffect, useEffectEvent, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { contractAPI } from '../../services/api';

function fmt(value) {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '-';
    return date.toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

function saveBlob(blob, fileName) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    URL.revokeObjectURL(url);
}

export default function ContractSignPage() {
    const { token } = useParams();
    const [payload, setPayload] = useState(null);
    const [fieldValues, setFieldValues] = useState({});
    const [claim, setClaim] = useState({ signerNm: '', signerEmail: '', signerTelno: '' });
    const [signature, setSignature] = useState('');
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const fields = useMemo(() => payload?.contract?.templateSchema?.fields || [], [payload]);

    const loadDetailEffect = useEffectEvent(async () => {
        await loadDetail();
    });

    useEffect(() => {
        if (!token) return;
        void loadDetailEffect();
    }, [token]);

    async function loadDetail() {
        setLoading(true);
        setError('');
        try {
            const response = await contractAPI.publicDetail(token);
            const nextPayload = response.data || null;
            setPayload(nextPayload);
            setFieldValues(nextPayload?.contract?.fieldMap || {});
            setClaim({
                signerNm: nextPayload?.signer?.claimedNm || nextPayload?.signer?.signerNm || '',
                signerEmail: nextPayload?.signer?.claimedEmail || nextPayload?.signer?.signerEmail || '',
                signerTelno: nextPayload?.signer?.claimedTelno || nextPayload?.signer?.signerTelno || '',
            });
        } catch (requestError) {
            setError(requestError.response?.data?.message || '서명 링크 정보를 불러오지 못했습니다.');
        } finally {
            setLoading(false);
        }
    }

    async function submit(event) {
        event.preventDefault();
        setSaving(true);
        setError('');
        try {
            await contractAPI.publicClaim(token, claim);
            await contractAPI.publicSubmit(token, {
                ...claim,
                signatureData: signature,
                initialData: claim.signerNm?.slice(0, 2) || '',
                fieldValues,
            });
            await loadDetail();
        } catch (requestError) {
            setError(requestError.response?.data?.message || '서명 제출에 실패했습니다.');
        } finally {
            setSaving(false);
        }
    }

    async function downloadFinalPdf() {
        try {
            const response = await contractAPI.publicDownload(token);
            saveBlob(response.data, `${payload?.contract?.contractRef || 'contract'}.pdf`);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '완료 PDF를 내려받지 못했습니다.');
        }
    }

    return (
        <div className="contract-sign-shell">
            <div className="contract-sign-card">
                {loading && <p>계약 정보를 불러오는 중입니다.</p>}
                {!loading && error && <div className="alert alert-error">{error}</div>}
                {!loading && payload && (
                    <>
                        <div className="contract-sign-header">
                            <span className={`badge ${payload.contract?.statusCd === 'completed' ? 'badge-green' : payload.canSubmit ? 'badge-blue' : 'badge-gray'}`}>{payload.contract?.statusCd || '상태'}</span>
                            <h2>{payload.contract?.contractTitle}</h2>
                            <p>{payload.contract?.contractMessage || '안내 문구가 없습니다.'}</p>
                            <div className="contract-sign-meta">
                                <span>만료: {fmt(payload.invitation?.expiresDt)}</span>
                                <span>현재 차례: {payload.contract?.currentSignOrder || 1}</span>
                                <span>서명자: {payload.signer?.claimedNm || payload.signer?.signerNm}</span>
                            </div>
                        </div>

                        <form onSubmit={submit} className="contract-sign-form">
                            <div className="form-group"><label className="form-label">이름</label><input className="form-input" value={claim.signerNm} onChange={(event) => setClaim((prev) => ({ ...prev, signerNm: event.target.value }))} /></div>
                            <div className="form-group"><label className="form-label">이메일</label><input className="form-input" value={claim.signerEmail} onChange={(event) => setClaim((prev) => ({ ...prev, signerEmail: event.target.value }))} /></div>
                            <div className="form-group"><label className="form-label">전화번호</label><input className="form-input" value={claim.signerTelno} onChange={(event) => setClaim((prev) => ({ ...prev, signerTelno: event.target.value }))} /></div>
                            {fields.map((field) => (
                                <div key={field.fieldId || field.fieldKey} className="form-group">
                                    <label className="form-label">{field.label}</label>
                                    <input className="form-input" value={fieldValues[field.fieldKey] || ''} onChange={(event) => setFieldValues((prev) => ({ ...prev, [field.fieldKey]: event.target.value }))} />
                                </div>
                            ))}
                            <div className="form-group">
                                <label className="form-label">서명</label>
                                <textarea className="form-input" rows="3" placeholder="이름 또는 서명 문구를 입력해 주세요." value={signature} onChange={(event) => setSignature(event.target.value)} />
                            </div>
                            <div className="contract-inline-actions">
                                <button type="submit" className="btn btn-primary" disabled={saving || !payload.canSubmit}>{payload.canSubmit ? '서명 제출' : '제출 불가'}</button>
                                {payload.downloadReady && <button type="button" className="btn btn-outline" onClick={downloadFinalPdf}>완료 PDF 다운로드</button>}
                            </div>
                            {!payload.canSubmit && <p className="text-muted">{payload.blockedReason || '현재 제출할 수 없는 상태입니다.'}</p>}
                        </form>
                    </>
                )}
            </div>
        </div>
    );
}
