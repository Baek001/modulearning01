import { useEffect, useEffectEvent, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { contractAPI } from '../../services/api';

function emptyField() {
    return { fieldId: `field-${Date.now()}`, fieldKey: '', label: '', fieldTypeCd: 'text', assignmentCd: 'creator', signerOrder: 1, x: 72, y: 160, width: 240, height: 44 };
}

function emptyForm() {
    return {
        templateNm: '',
        templateCode: '',
        templateDesc: '',
        templateCategoryCd: 'general',
        versionLabel: 'v1.0',
        versionNote: '',
        templateVersionId: '',
        fields: [emptyField()],
    };
}

export default function ContractTemplateDesignerPage() {
    const navigate = useNavigate();
    const { templateId } = useParams();
    const [form, setForm] = useState(emptyForm());
    const [loading, setLoading] = useState(Boolean(templateId));
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');

    const schema = useMemo(() => ({ pages: [{ pageId: 'page-1', label: '1' }], fields: form.fields }), [form.fields]);
    const layout = useMemo(() => ({ canvas: { width: 820, height: 1160, padding: 48, backgroundColor: '#ffffff' } }), []);

    const loadTemplateEffect = useEffectEvent(async (id) => {
        await loadTemplate(id);
    });

    useEffect(() => {
        if (!templateId) return;
        void loadTemplateEffect(templateId);
    }, [templateId]);

    async function loadTemplate(id) {
        setLoading(true);
        try {
            const response = await contractAPI.templateDetail(id);
            const detail = response.data || {};
            const currentVersion = detail.currentVersion || detail.versions?.[0] || {};
            setForm({
                templateNm: detail.templateNm || '',
                templateCode: detail.templateCode || '',
                templateDesc: detail.templateDesc || '',
                templateCategoryCd: detail.templateCategoryCd || 'general',
                versionLabel: currentVersion.versionLabel || `v${currentVersion.versionNo || 1}.0`,
                versionNote: currentVersion.versionNote || '',
                templateVersionId: currentVersion.templateVersionId || '',
                fields: currentVersion.schema?.fields?.length ? currentVersion.schema.fields : [emptyField()],
            });
        } catch (requestError) {
            setError(requestError.response?.data?.message || '양식 상세를 불러오지 못했습니다.');
        } finally {
            setLoading(false);
        }
    }

    async function submit(publishNow = false) {
        setSaving(true);
        setError('');
        setMessage('');
        try {
            const payload = {
                templateNm: form.templateNm,
                templateCode: form.templateCode,
                templateDesc: form.templateDesc,
                templateCategoryCd: form.templateCategoryCd,
                versionLabel: form.versionLabel,
                versionNote: form.versionNote,
                templateVersionId: form.templateVersionId || null,
                schema,
                layout,
                publishNow,
            };
            const response = templateId
                ? await contractAPI.updateTemplate(templateId, payload)
                : await contractAPI.createTemplate(payload);
            setMessage(publishNow ? '양식을 게시했습니다.' : '양식을 저장했습니다.');
            if (!templateId) {
                navigate(`/approval/contracts/templates/${response.data?.templateId}`);
            } else {
                await loadTemplate(templateId);
            }
        } catch (requestError) {
            setError(requestError.response?.data?.message || '양식 저장에 실패했습니다.');
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="animate-slide-up">
            <div className="page-header">
                <div>
                    <h2>전자계약 양식 디자이너</h2>
                    <p>필드 배치와 버전 관리를 현재 앱 UI 안에서 편집합니다.</p>
                </div>
                <div className="page-header-actions">
                    <button type="button" className="btn btn-outline" onClick={() => navigate('/approval/contracts')}>목록으로</button>
                    <button type="button" className="btn btn-secondary" disabled={saving} onClick={() => submit(false)}>저장</button>
                    <button type="button" className="btn btn-primary" disabled={saving} onClick={() => submit(true)}>게시</button>
                </div>
            </div>

            {error && <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}><div className="card-body" style={{ color: 'var(--danger)' }}>{error}</div></div>}
            {message && <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}><div className="card-body" style={{ color: 'var(--success)' }}>{message}</div></div>}

            <div className="contract-designer-layout">
                <section className="card">
                    <div className="card-header"><h3>양식 정보</h3></div>
                    <div className="card-body contract-designer-meta">
                        <div className="form-group"><label className="form-label">양식명</label><input className="form-input" value={form.templateNm} onChange={(event) => setForm((prev) => ({ ...prev, templateNm: event.target.value }))} /></div>
                        <div className="form-group"><label className="form-label">코드</label><input className="form-input" value={form.templateCode} onChange={(event) => setForm((prev) => ({ ...prev, templateCode: event.target.value }))} /></div>
                        <div className="form-group"><label className="form-label">카테고리</label><input className="form-input" value={form.templateCategoryCd} onChange={(event) => setForm((prev) => ({ ...prev, templateCategoryCd: event.target.value }))} /></div>
                        <div className="form-group"><label className="form-label">버전 라벨</label><input className="form-input" value={form.versionLabel} onChange={(event) => setForm((prev) => ({ ...prev, versionLabel: event.target.value }))} /></div>
                        <div className="form-group" style={{ gridColumn: '1 / -1' }}><label className="form-label">설명</label><textarea className="form-input" rows="3" value={form.templateDesc} onChange={(event) => setForm((prev) => ({ ...prev, templateDesc: event.target.value }))} /></div>
                    </div>
                </section>

                <section className="card">
                    <div className="card-header"><h3>필드 디자이너</h3></div>
                    <div className="card-body contract-designer-fields">
                        {loading ? <p>양식을 불러오는 중입니다.</p> : form.fields.map((field, index) => (
                            <div key={field.fieldId} className="contract-designer-field">
                                <div className="contract-inline-actions">
                                    <strong>{index + 1}. {field.label || '새 필드'}</strong>
                                    <button type="button" className="btn btn-sm btn-outline" onClick={() => setForm((prev) => ({ ...prev, fields: prev.fields.filter((item) => item.fieldId !== field.fieldId) }))}>삭제</button>
                                </div>
                                <div className="contract-designer-grid">
                                    <div className="form-group"><label className="form-label">키</label><input className="form-input" value={field.fieldKey} onChange={(event) => setForm((prev) => ({ ...prev, fields: prev.fields.map((item) => item.fieldId === field.fieldId ? { ...item, fieldKey: event.target.value } : item) }))} /></div>
                                    <div className="form-group"><label className="form-label">라벨</label><input className="form-input" value={field.label} onChange={(event) => setForm((prev) => ({ ...prev, fields: prev.fields.map((item) => item.fieldId === field.fieldId ? { ...item, label: event.target.value } : item) }))} /></div>
                                    <div className="form-group"><label className="form-label">타입</label><input className="form-input" value={field.fieldTypeCd} onChange={(event) => setForm((prev) => ({ ...prev, fields: prev.fields.map((item) => item.fieldId === field.fieldId ? { ...item, fieldTypeCd: event.target.value } : item) }))} /></div>
                                    <div className="form-group"><label className="form-label">담당</label><input className="form-input" value={field.assignmentCd} onChange={(event) => setForm((prev) => ({ ...prev, fields: prev.fields.map((item) => item.fieldId === field.fieldId ? { ...item, assignmentCd: event.target.value } : item) }))} /></div>
                                    <div className="form-group"><label className="form-label">서명 순서</label><input className="form-input" type="number" value={field.signerOrder || 1} onChange={(event) => setForm((prev) => ({ ...prev, fields: prev.fields.map((item) => item.fieldId === field.fieldId ? { ...item, signerOrder: Number(event.target.value) } : item) }))} /></div>
                                </div>
                            </div>
                        ))}
                        <button type="button" className="btn btn-outline" onClick={() => setForm((prev) => ({ ...prev, fields: [...prev.fields, emptyField()] }))}>필드 추가</button>
                    </div>
                </section>
            </div>
        </div>
    );
}
