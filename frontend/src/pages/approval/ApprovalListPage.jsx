import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { approvalAPI, usersAPI } from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';

const SECTIONS = [
    { key: 'draft', label: '기안한 문서', countKey: 'draftProgress' },
    { key: 'inbox', label: '결재할 문서', countKey: 'inboxPending' },
    { key: 'upcoming', label: '예정된 문서', countKey: 'upcoming' },
    { key: 'reference', label: '참조 문서', countKey: 'reference' },
    { key: 'temp', label: '임시저장', countKey: 'tempSaved' },
    { key: 'archive', label: '결재 보관함', countKey: 'archive' },
];

const DRAFT_TABS = [
    { key: 'progress', label: '진행' },
    { key: 'approved', label: '완료' },
    { key: 'rejected', label: '반려' },
    { key: 'retracted', label: '기안회수' },
];

const ARCHIVE_TABS = [
    { key: 'all', label: '전체' },
    { key: 'approved', label: '완료' },
    { key: 'rejected', label: '반려' },
    { key: 'retracted', label: '회수' },
];

const STATUS_META = {
    A202: { label: '결재 대기', badge: 'badge-orange' },
    A203: { label: '결재 진행', badge: 'badge-orange' },
    A204: { label: '반려', badge: 'badge-red' },
    A205: { label: '기안 회수', badge: 'badge-gray' },
    A206: { label: '승인 완료', badge: 'badge-green' },
};

const LINE_STATUS = {
    A301: { label: '미열람', badge: 'badge-gray' },
    A302: { label: '대기', badge: 'badge-orange' },
    A303: { label: '승인', badge: 'badge-green' },
    A304: { label: '반려', badge: 'badge-red' },
};

const FILTERS = { keyword: '', templateId: '', dateFrom: '', dateTo: '' };
const VACATION_TYPES = ['연차', '반차', '병가', '공가'];
const SAVE_YEAR_OPTIONS = ['1', '3', '5', '10'];
const PRIORITY_OPTIONS = ['낮음', '보통', '높음', '긴급'];
const RANGE_LABELS = new Set(['기간', '일정']);
const MULTILINE_HINTS = ['사유', '내용', '목적', '목표', '범위', '내역', '요청'];
const NUMBER_HINTS = ['금액', '비용', '예산', '수량'];
const DATE_HINTS = ['일자', '완료일'];
const TRUTHY_VALUES = new Set(['true', 'y', 'yes', '1', 'on', 'checked', '반차 사용', '사용']);

const DOCUMENT_FORM_SCHEMAS = {
    VAC_REQ: {
        code: 'VAC_REQ',
        heading: '휴가 신청서',
        description: '휴가 일정과 인수인계를 정리해 결재선에 공유합니다.',
        categoryLabel: '인사 문서',
        automationNote: '승인 완료 후 휴가 사용 내역에 반영됩니다. 시작일과 종료일을 모두 입력해 주세요.',
        requiredKeys: ['period', 'reason'],
        fields: [
            { key: 'vacationType', label: '휴가 종류', type: 'radio', options: VACATION_TYPES, defaultValue: '연차' },
            { key: 'halfDay', label: '반차 여부', type: 'checkbox', optionLabel: '반차 사용' },
            { key: 'period', label: '기간', type: 'daterange' },
            { key: 'reason', label: '휴가 사유', type: 'textarea', placeholder: '휴가 사용 사유를 입력하세요' },
            { key: 'handoverTo', label: '업무 인수자', type: 'user-select', placeholder: '업무 인수자를 선택하세요' },
        ],
        layout: [
            ['vacationType', 'halfDay'],
            ['period'],
            ['reason'],
            ['handoverTo'],
        ],
    },
    TRIP_REQ: {
        code: 'TRIP_REQ',
        heading: '출장/외근 신청서',
        description: '방문 일정과 목적, 예상 비용을 사전에 공유합니다.',
        categoryLabel: '출장 문서',
        automationNote: '승인 완료 후 기간 정보가 출장 일정에 반영됩니다.',
        requiredKeys: ['destination', 'period', 'purpose'],
        fields: [
            { key: 'destination', label: '방문지', type: 'text', placeholder: '방문지 또는 외근 장소를 입력하세요' },
            { key: 'period', label: '기간', type: 'daterange' },
            { key: 'purpose', label: '목적', type: 'textarea', placeholder: '출장/외근 목적을 입력하세요' },
            { key: 'estimatedCost', label: '예상 비용', type: 'number', placeholder: '예상 비용을 입력하세요' },
        ],
        layout: [
            ['destination', 'period'],
            ['purpose'],
            ['estimatedCost'],
        ],
    },
    EXP_APPROVAL: {
        code: 'EXP_APPROVAL',
        heading: '지출 결의서',
        description: '지출 일자, 금액, 계정을 명확히 남기는 비용 집행 문서입니다.',
        categoryLabel: '재무 문서',
        requiredKeys: ['expenseDate', 'amount', 'detail'],
        fields: [
            { key: 'expenseDate', label: '지출 일자', type: 'date' },
            { key: 'amount', label: '금액', type: 'number', placeholder: '지출 금액을 입력하세요' },
            { key: 'costAccount', label: '비용 계정', type: 'text', placeholder: '예: 복리후생비, 교통비' },
            { key: 'detail', label: '세부 내역', type: 'textarea', placeholder: '세부 지출 내용을 입력하세요' },
        ],
        layout: [
            ['expenseDate', 'amount'],
            ['costAccount'],
            ['detail'],
        ],
    },
    PO_REQUEST: {
        code: 'PO_REQUEST',
        heading: '구매 품의서',
        description: '품목과 예산, 구매 사유를 한 문서에서 정리합니다.',
        categoryLabel: '구매 문서',
        requiredKeys: ['itemName', 'quantity', 'budget', 'requestReason'],
        fields: [
            { key: 'itemName', label: '품목', type: 'text', placeholder: '구매 품목을 입력하세요' },
            { key: 'quantity', label: '수량', type: 'number', placeholder: '수량을 입력하세요' },
            { key: 'budget', label: '예산', type: 'number', placeholder: '예산을 입력하세요' },
            { key: 'requestReason', label: '요청 사유', type: 'textarea', placeholder: '구매가 필요한 이유를 입력하세요' },
        ],
        layout: [
            ['itemName', 'quantity'],
            ['budget'],
            ['requestReason'],
        ],
    },
    PRO_PROPOSAL: {
        code: 'PRO_PROPOSAL',
        heading: '프로젝트 기안서',
        description: '프로젝트 목적, 범위, 일정 계획을 정리하는 기안 문서입니다.',
        categoryLabel: '프로젝트 문서',
        requiredKeys: ['projectName', 'goal', 'scope', 'schedule'],
        fields: [
            { key: 'projectName', label: '프로젝트명', type: 'text', placeholder: '프로젝트 이름을 입력하세요' },
            { key: 'schedule', label: '일정', type: 'daterange' },
            { key: 'goal', label: '목표', type: 'textarea', placeholder: '프로젝트 목표를 입력하세요' },
            { key: 'scope', label: '범위', type: 'textarea', placeholder: '프로젝트 범위를 입력하세요' },
        ],
        layout: [
            ['projectName', 'schedule'],
            ['goal'],
            ['scope'],
        ],
    },
    MKT_REQUEST: {
        code: 'MKT_REQUEST',
        heading: '마케팅 실행 요청서',
        description: '캠페인 대상과 기간, 기대 KPI를 공유하는 실행 요청 문서입니다.',
        categoryLabel: '마케팅 문서',
        requiredKeys: ['campaignName', 'targetAudience', 'period', 'expectedKpi'],
        fields: [
            { key: 'campaignName', label: '캠페인명', type: 'text', placeholder: '캠페인명을 입력하세요' },
            { key: 'targetAudience', label: '대상', type: 'text', placeholder: '주요 대상을 입력하세요' },
            { key: 'period', label: '기간', type: 'daterange' },
            { key: 'expectedKpi', label: '기대 KPI', type: 'textarea', placeholder: '기대 성과를 입력하세요' },
        ],
        layout: [
            ['campaignName', 'targetAudience'],
            ['period'],
            ['expectedKpi'],
        ],
    },
    IT_WORK_REQ: {
        code: 'IT_WORK_REQ',
        heading: '개발/IT 작업 요청서',
        description: '요청 시스템, 작업 내용, 우선순위를 정리하는 IT 작업 문서입니다.',
        categoryLabel: 'IT 문서',
        requiredKeys: ['systemName', 'requestDetails', 'priority', 'preferredCompletionDate'],
        fields: [
            { key: 'systemName', label: '시스템', type: 'text', placeholder: '대상 시스템을 입력하세요' },
            { key: 'priority', label: '우선순위', type: 'select', options: PRIORITY_OPTIONS, defaultValue: '보통' },
            { key: 'requestDetails', label: '요청 내용', type: 'textarea', placeholder: '필요한 작업 내용을 입력하세요' },
            { key: 'preferredCompletionDate', label: '희망 완료일', type: 'date' },
        ],
        layout: [
            ['systemName', 'priority'],
            ['requestDetails'],
            ['preferredCompletionDate'],
        ],
    },
    LOG_TRANSFER: {
        code: 'LOG_TRANSFER',
        heading: '물류 이동 요청서',
        description: '출발지와 도착지, 이동 품목을 정리하는 물류 요청 문서입니다.',
        categoryLabel: '물류 문서',
        requiredKeys: ['origin', 'destination', 'itemQuantity', 'requestReason'],
        fields: [
            { key: 'origin', label: '출발지', type: 'text', placeholder: '출발지를 입력하세요' },
            { key: 'destination', label: '도착지', type: 'text', placeholder: '도착지를 입력하세요' },
            { key: 'itemQuantity', label: '품목/수량', type: 'text', placeholder: '이동할 품목과 수량을 입력하세요' },
            { key: 'requestReason', label: '요청 사유', type: 'textarea', placeholder: '이동 요청 사유를 입력하세요' },
        ],
        layout: [
            ['origin', 'destination'],
            ['itemQuantity'],
            ['requestReason'],
        ],
    },
};

const EMPTY_COMPOSE = {
    atrzTempSqn: '',
    atrzDocTmplId: '',
    atrzDocTtl: '',
    openYn: 'N',
    urgentYn: 'N',
    saveYear: '5',
    approvalLines: [],
    receiverIds: [],
    heading: '',
    fields: [],
    values: {},
    note: '',
    existingFileId: '',
    existingFiles: [],
    newFiles: [],
};

function defaultStatus(section) {
    return section === 'draft' ? 'progress' : 'all';
}

function numberValue(value, fallback = 0) {
    const numeric = Number(value);
    return Number.isFinite(numeric) ? numeric : fallback;
}

function escapeHtml(value) {
    return String(value || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

function nl2br(value) {
    return escapeHtml(value).replaceAll('\n', '<br />');
}

function formatDateTime(value) {
    if (!value) {
        return '-';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    });
}

function formatDateOnly(value = new Date()) {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '';
    }

    return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
    });
}

function stripMeta(html) {
    if (!html || typeof window === 'undefined') {
        return html || '';
    }

    const doc = new window.DOMParser().parseFromString(html, 'text/html');
    doc.querySelectorAll('script[data-approval-meta="true"]').forEach((node) => node.remove());
    return doc.body.innerHTML || '';
}

function extractMeta(html) {
    if (!html || typeof window === 'undefined') {
        return {};
    }

    try {
        const doc = new window.DOMParser().parseFromString(html, 'text/html');
        const node = doc.querySelector('script[data-approval-meta="true"]');
        return node?.textContent ? JSON.parse(node.textContent) : {};
    } catch {
        return {};
    }
}

function buildMeta(meta) {
    return `<script type="application/json" data-approval-meta="true">${escapeHtml(JSON.stringify(meta || {}))}</script>`;
}

function inferFieldType(label) {
    if (!label) {
        return 'text';
    }
    if (RANGE_LABELS.has(label)) {
        return 'range';
    }
    if (label.includes('/') && !label.includes('기간') && !label.includes('일정')) {
        return 'text';
    }
    if (MULTILINE_HINTS.some((hint) => label.includes(hint))) {
        return 'textarea';
    }
    if (DATE_HINTS.some((hint) => label.includes(hint))) {
        return 'date';
    }
    if (NUMBER_HINTS.some((hint) => label.includes(hint))) {
        return 'number';
    }
    return 'text';
}

function parseTemplate(html, fallbackTitle) {
    if (typeof window === 'undefined') {
        return { heading: fallbackTitle || '전자결재 문서', fields: [] };
    }

    const doc = new window.DOMParser().parseFromString(html || '', 'text/html');
    const heading = doc.querySelector('h2')?.textContent?.trim() || fallbackTitle || '전자결재 문서';
    const fields = Array.from(doc.querySelectorAll('tr'))
        .map((row, index) => {
            const label = row.querySelector('th')?.textContent?.trim();
            return label ? { id: `field-${index + 1}`, label, type: inferFieldType(label) } : null;
        })
        .filter(Boolean);

    return { heading, fields };
}

function parseTableValues(html) {
    if (typeof window === 'undefined') {
        return {};
    }

    const doc = new window.DOMParser().parseFromString(stripMeta(html || ''), 'text/html');
    const values = {};
    doc.querySelectorAll('tr').forEach((row) => {
        const labels = Array.from(row.querySelectorAll('th')).map((cell) => cell.textContent?.trim()).filter(Boolean);
        const cells = Array.from(row.querySelectorAll('td'));
        labels.forEach((label, index) => {
            const value = cells[index]?.textContent?.trim() || '';
            if (label) {
                values[label] = value;
            }
        });
    });
    return values;
}

function getTemplateCode(template) {
    return template?.atrzDocCd || '';
}

function getDocumentSchema(template) {
    return DOCUMENT_FORM_SCHEMAS[getTemplateCode(template)] || null;
}

function buildFieldIndex(fields = []) {
    return new Map(fields.map((field) => [field.key || field.id, field]));
}

function parseRange(value) {
    const [start = '', end = ''] = String(value || '').split('~').map((item) => item.trim());
    return { start, end };
}

function defaultFieldValue(field) {
    if (field.defaultValue !== undefined) {
        return field.defaultValue;
    }

    if (field.type === 'daterange' || field.type === 'range') {
        return { start: '', end: '' };
    }

    if (field.type === 'checkbox') {
        return false;
    }

    return '';
}

function normalizeBooleanValue(value) {
    if (typeof value === 'boolean') {
        return value;
    }

    const normalized = String(value || '').trim();
    return TRUTHY_VALUES.has(normalized) || TRUTHY_VALUES.has(normalized.toLowerCase());
}

function normalizeFieldValue(field, rawValue) {
    if (rawValue === undefined || rawValue === null || rawValue === '') {
        return defaultFieldValue(field);
    }

    if (field.type === 'daterange' || field.type === 'range') {
        if (typeof rawValue === 'object' && rawValue !== null) {
            return {
                start: rawValue.start || '',
                end: rawValue.end || '',
            };
        }
        return parseRange(rawValue);
    }

    if (field.type === 'checkbox') {
        return normalizeBooleanValue(rawValue);
    }

    return String(rawValue);
}

function readStoredFieldValue(field, metaFieldValues, tableValues, meta) {
    const directValue = metaFieldValues?.[field.key] ?? metaFieldValues?.[field.label] ?? tableValues?.[field.label];
    if (directValue !== undefined) {
        return normalizeFieldValue(field, directValue);
    }

    if (field.key === 'vacationType') {
        return normalizeFieldValue(field, meta?.vacation?.vacationType || tableValues?.['휴가 종류']);
    }

    if (field.key === 'period') {
        const legacyRange = meta?.vacation
            ? { start: meta.vacation.startDate, end: meta.vacation.endDate }
            : tableValues?.[field.label];
        return normalizeFieldValue(field, legacyRange);
    }

    if (field.key === 'reason') {
        return normalizeFieldValue(field, meta?.vacation?.reason || tableValues?.['사유'] || tableValues?.['휴가 사유']);
    }

    if (field.key === 'handoverTo') {
        return normalizeFieldValue(field, meta?.vacation?.handoverTo || tableValues?.['업무 인수자']);
    }

    if (field.key === 'halfDay') {
        return normalizeFieldValue(field, meta?.vacation?.halfDay || tableValues?.['반차 여부']);
    }

    return defaultFieldValue(field);
}

function fieldHasValue(field, value) {
    if (field.type === 'daterange' || field.type === 'range') {
        return Boolean(value?.start) && Boolean(value?.end);
    }

    if (field.type === 'checkbox') {
        return Boolean(value);
    }

    return String(value || '').trim() !== '';
}

function missingRequiredFieldLabel(schema, values = {}) {
    if (!schema?.requiredKeys?.length) {
        return '';
    }

    const fieldMap = buildFieldIndex(schema.fields);
    const missingKey = schema.requiredKeys.find((key) => {
        const field = fieldMap.get(key);
        return field && !fieldHasValue(field, values[key]);
    });

    return missingKey ? fieldMap.get(missingKey)?.label || '' : '';
}

function serializeFieldValue(field, value) {
    if (field.type === 'daterange' || field.type === 'range') {
        return [value?.start || '', value?.end || ''].filter(Boolean).join(' ~ ');
    }

    if (field.type === 'checkbox') {
        return value ? field.optionLabel || '사용' : '해당 없음';
    }

    return value || '';
}

function calculateVacationDays(period, halfDay = false) {
    if (!period?.start || !period?.end) {
        return 0;
    }

    const start = new Date(period.start);
    const end = new Date(period.end);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end < start) {
        return 0;
    }

    if (halfDay) {
        return 0.5;
    }

    return Math.floor((end.getTime() - start.getTime()) / 86400000) + 1;
}

function buildDocumentHeadHtml(schema, compose, template) {
    return `<div class="approval-doc-header"><div><span class="approval-doc-kicker">${escapeHtml(schema.categoryLabel || template?.atrzCategory || '전자결재')}</span><h2>${escapeHtml(schema.heading || compose.heading || template?.atrzDocTmplNm || '전자결재 문서')}</h2><p>${escapeHtml(schema.description || template?.atrzDescription || '')}</p></div></div>`;
}

function buildDocumentMetaTableHtml(compose, currentUser) {
    return `<table class="approval-doc-table approval-doc-meta-table">
<tr><th>기안부서</th><td>${escapeHtml(currentUser?.deptNm || '-')}</td><th>직위</th><td>${escapeHtml(currentUser?.jbgdNm || '-')}</td></tr>
<tr><th>기안자</th><td>${escapeHtml(currentUser?.userNm || currentUser?.userId || '-')}</td><th>기안일</th><td>${escapeHtml(formatDateOnly())}</td></tr>
<tr><th>사번</th><td>${escapeHtml(currentUser?.userId || '-')}</td><th>보존연한</th><td>${escapeHtml(compose.saveYear)}년</td></tr>
</table>`;
}

function buildSchemaBodyRowsHtml(schema, compose) {
    const fieldMap = buildFieldIndex(schema.fields);
    const rows = [
        `<tr><th>제목</th><td colspan="3">${escapeHtml(compose.atrzDocTtl)}</td></tr>`,
    ];

    schema.layout.forEach((layoutRow) => {
        const fields = layoutRow.map((key) => fieldMap.get(key)).filter(Boolean);
        if (fields.length === 0) {
            return;
        }

        if (fields.length === 1) {
            const [field] = fields;
            rows.push(`<tr><th>${escapeHtml(field.label)}</th><td colspan="3">${nl2br(serializeFieldValue(field, compose.values[field.key]))}</td></tr>`);
            return;
        }

        rows.push(`<tr>${fields.map((field) => `<th>${escapeHtml(field.label)}</th><td>${nl2br(serializeFieldValue(field, compose.values[field.key]))}</td>`).join('')}</tr>`);
    });

    if (compose.note?.trim()) {
        rows.push(`<tr><th>기안 의견</th><td colspan="3">${nl2br(compose.note)}</td></tr>`);
    }

    return rows.join('');
}

function buildSchemaDocumentHtml(schema, compose, template, currentUser, remainDays) {
    const usedVacationDays = calculateVacationDays(compose.values.period, Boolean(compose.values.halfDay));
    const vacationStats = schema.code === 'VAC_REQ'
        ? `<div class="approval-doc-stat-grid">
<div class="approval-doc-stat"><strong>잔여 연차</strong><span>${escapeHtml(String(remainDays))}일</span></div>
<div class="approval-doc-stat"><strong>사용 예정</strong><span>${escapeHtml(String(usedVacationDays))}일</span></div>
<div class="approval-doc-stat"><strong>반영 방식</strong><span>승인 후 연차 내역</span></div>
</div>`
        : '';

    return `<section class="approval-doc-layout">
${buildDocumentHeadHtml(schema, compose, template)}
${buildDocumentMetaTableHtml(compose, currentUser)}
${vacationStats}
<table class="approval-doc-table approval-doc-body-table">
${buildSchemaBodyRowsHtml(schema, compose)}
</table>
</section>`;
}

function buildFallbackHtml(compose, template, currentUser) {
    const rows = compose.fields.map((field) => {
        const fieldValue = serializeFieldValue(field, compose.values[field.id]);
        return `<tr><th>${escapeHtml(field.label)}</th><td colspan="3">${nl2br(fieldValue)}</td></tr>`;
    }).join('');

    const noteRow = compose.note?.trim()
        ? `<tr><th>기안 의견</th><td colspan="3">${nl2br(compose.note)}</td></tr>`
        : '';

    return `<section class="approval-doc-layout">
<div class="approval-doc-header"><div><span class="approval-doc-kicker">${escapeHtml(template?.atrzCategory || '전자결재')}</span><h2>${escapeHtml(compose.heading || template?.atrzDocTmplNm || '전자결재 문서')}</h2><p>${escapeHtml(template?.atrzDescription || '선택한 양식에 맞춰 내용을 작성합니다.')}</p></div></div>
${buildDocumentMetaTableHtml(compose, currentUser)}
<table class="approval-doc-table approval-doc-body-table">
<tr><th>제목</th><td colspan="3">${escapeHtml(compose.atrzDocTtl)}</td></tr>
${rows}
${noteRow}
</table>
</section>`;
}

function buildComposeBodyHtml(compose, template, currentUser, remainDays) {
    const schema = getDocumentSchema(template);
    if (schema) {
        return buildSchemaDocumentHtml(schema, compose, template, currentUser, remainDays);
    }

    return buildFallbackHtml(compose, template, currentUser);
}

function fileLabel(file) {
    return file?.orgnFileNm || file?.saveFileNm || '첨부 파일';
}

function normalizeSummary(data) {
    if (data?.counts) {
        return data.counts;
    }
    return {
        draftProgress: 0,
        inboxPending: numberValue(data?.approvalCount, 0),
        upcoming: 0,
        reference: 0,
        tempSaved: 0,
        archive: 0,
        draftApproved: 0,
        draftRejected: 0,
        draftRetracted: 0,
    };
}

function makeCompose(template, options = {}) {
    const schema = getDocumentSchema(template);
    if (!schema) {
        const meta = options.meta || {};
        const parsed = parseTemplate(template?.htmlContents || '', template?.atrzDocTmplNm);
        const tableValues = parseTableValues(options.htmlData || template?.htmlContents || '');
        const fieldValues = meta.fieldValues || {};

        const values = {};
        parsed.fields.forEach((field) => {
            const storedValue = fieldValues[field.id] ?? fieldValues[field.label] ?? tableValues[field.label] ?? '';
            values[field.id] = field.type === 'range' ? parseRange(storedValue) : storedValue;
        });

        return {
            ...EMPTY_COMPOSE,
            atrzTempSqn: options.atrzTempSqn || '',
            atrzDocTmplId: template?.atrzDocTmplId || '',
            atrzDocTtl: options.atrzDocTtl || `${template?.atrzDocTmplNm || '전자결재 문서'} - ${new Date().toLocaleDateString('ko-KR')}`,
            openYn: meta.openYn || options.openYn || 'N',
            urgentYn: meta.urgentYn || 'N',
            saveYear: String(meta.saveYear || template?.atrzSaveYear || '5'),
            approvalLines: Array.from(new Set((meta.approvalLines || []).map((item) => (
                typeof item === 'string' ? item : item.userId || item.atrzApprUserId || item.atrzApprId
            )).filter(Boolean))),
            receiverIds: Array.from(new Set((meta.receiverIds || []).filter(Boolean))),
            heading: meta.heading || parsed.heading,
            fields: parsed.fields,
            values,
            note: meta.additionalNote || tableValues['기안 의견'] || tableValues['추가 메모'] || '',
            existingFileId: options.atrzFileId || '',
            existingFiles: options.existingFiles || [],
            newFiles: [],
        };
    }

    const meta = options.meta || {};
    const tableValues = parseTableValues(options.htmlData || template?.htmlContents || '');
    const metaFieldValues = meta.fieldValues || {};
    const values = {};

    schema.fields.forEach((field) => {
        values[field.key] = readStoredFieldValue(field, metaFieldValues, tableValues, meta);
    });

    return {
        ...EMPTY_COMPOSE,
        atrzTempSqn: options.atrzTempSqn || '',
        atrzDocTmplId: template?.atrzDocTmplId || '',
        atrzDocTtl: options.atrzDocTtl || `${schema.heading} - ${new Date().toLocaleDateString('ko-KR')}`,
        openYn: meta.openYn || options.openYn || 'N',
        urgentYn: meta.urgentYn || 'N',
        saveYear: String(meta.saveYear || tableValues['보존연한']?.replace('년', '') || template?.atrzSaveYear || '5'),
        approvalLines: Array.from(new Set((meta.approvalLines || []).map((item) => (
            typeof item === 'string' ? item : item.userId || item.atrzApprUserId || item.atrzApprId
        )).filter(Boolean))),
        receiverIds: Array.from(new Set((meta.receiverIds || []).filter(Boolean))),
        heading: meta.heading || schema.heading,
        fields: [],
        values,
        note: meta.additionalNote || tableValues['기안 의견'] || tableValues['추가 메모'] || '',
        existingFileId: options.atrzFileId || '',
        existingFiles: options.existingFiles || [],
        newFiles: [],
    };
}

function lineUserId(item) {
    return item?.atrzApprUserId || item?.atrzApprId || item?.userId || '';
}

function buildSearchParams(current, patch) {
    const next = new URLSearchParams(current);
    Object.entries(patch).forEach(([key, value]) => {
        if (value === undefined || value === null || value === '') {
            next.delete(key);
        } else {
            next.set(key, String(value));
        }
    });
    return next;
}

function pageNumbers(totalPages, currentPage) {
    if (totalPages <= 5) {
        return Array.from({ length: totalPages }, (_, index) => index + 1);
    }

    const start = Math.max(1, currentPage - 2);
    const end = Math.min(totalPages, start + 4);
    const adjustedStart = Math.max(1, end - 4);
    return Array.from({ length: end - adjustedStart + 1 }, (_, index) => adjustedStart + index);
}

function userLabel(user) {
    const detail = [user?.deptNm, user?.jbgdNm].filter(Boolean).join(' · ');
    return detail ? `${user?.userNm} (${detail})` : user?.userNm;
}

function pickUser(users, userId) {
    return users.find((item) => item.userId === userId);
}

export default function ApprovalListPage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();

    const section = searchParams.get('section') || 'draft';
    const status = searchParams.get('status') || defaultStatus(section);
    const page = Math.max(1, numberValue(searchParams.get('page'), 1));
    const mode = searchParams.get('mode') || 'list';
    const atrzDocId = searchParams.get('atrzDocId') || '';
    const tempId = searchParams.get('tempId') || '';

    const [templates, setTemplates] = useState([]);
    const [users, setUsers] = useState([]);
    const [bookmarks, setBookmarks] = useState([]);
    const [summary, setSummary] = useState({});
    const [tempItems, setTempItems] = useState([]);
    const [listData, setListData] = useState({
        section: 'draft',
        status: 'progress',
        items: [],
        page: 1,
        totalPages: 1,
        totalRecords: 0,
        statusCounts: {},
    });
    const [detail, setDetail] = useState(null);
    const [compose, setCompose] = useState(EMPTY_COMPOSE);
    const [filters, setFilters] = useState(FILTERS);
    const [vacationBalance, setVacationBalance] = useState(0);
    const [loadingBootstrap, setLoadingBootstrap] = useState(true);
    const [loadingList, setLoadingList] = useState(false);
    const [loadingDetail, setLoadingDetail] = useState(false);
    const [loadingComposeSupport, setLoadingComposeSupport] = useState(false);
    const [loadingTempItems, setLoadingTempItems] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [processingAction, setProcessingAction] = useState(false);
    const [bookmarkName, setBookmarkName] = useState('');
    const [actionOpinion, setActionOpinion] = useState('');
    const [otpCode, setOtpCode] = useState('');
    const [feedback, setFeedback] = useState('');
    const [errorMessage, setErrorMessage] = useState('');
    const [composeSupportLoaded, setComposeSupportLoaded] = useState(false);
    const [tempItemsLoaded, setTempItemsLoaded] = useState(false);

    const currentTemplate = useMemo(
        () => templates.find((item) => item.atrzDocTmplId === compose.atrzDocTmplId) || null,
        [templates, compose.atrzDocTmplId]
    );

    const currentSchema = useMemo(
        () => getDocumentSchema(currentTemplate),
        [currentTemplate]
    );

    const availableUsers = useMemo(
        () => users.filter((item) => item.userId !== user?.userId),
        [users, user?.userId]
    );

    const groupedBookmarks = useMemo(() => {
        const grouped = new Map();

        bookmarks.forEach((item) => {
            const name = item.cstmLineBmNm || '즐겨찾기';
            const list = grouped.get(name) || [];
            list.push(item);
            grouped.set(name, list);
        });

        return Array.from(grouped.entries()).map(([name, lines]) => ({
            name,
            lines: [...lines].sort((a, b) => numberValue(a.atrzLineSeq, 0) - numberValue(b.atrzLineSeq, 0)),
        }));
    }, [bookmarks]);

    const isVacationForm = currentSchema?.code === 'VAC_REQ';
    const usedVacationDays = calculateVacationDays(compose.values.period, Boolean(compose.values.halfDay));

    const documentBodyHtml = useMemo(() => {
        if (!compose.atrzDocTmplId || !currentTemplate) {
            return '';
        }

        return buildComposeBodyHtml(compose, currentTemplate, user, vacationBalance);
    }, [compose, currentTemplate, user, vacationBalance]);

    const documentHtml = useMemo(() => {
        if (!documentBodyHtml) {
            return '';
        }

        const meta = {
            heading: compose.heading,
            saveYear: compose.saveYear,
            openYn: compose.openYn,
            urgentYn: compose.urgentYn,
            approvalLines: compose.approvalLines,
            receiverIds: compose.receiverIds,
            additionalNote: compose.note,
            fieldValues: compose.values,
            schemaCode: currentSchema?.code || getTemplateCode(currentTemplate),
            vacation: isVacationForm ? {
                vacationType: compose.values.vacationType || '연차',
                startDate: compose.values.period?.start || '',
                endDate: compose.values.period?.end || '',
                reason: compose.values.reason || '',
                handoverTo: compose.values.handoverTo || '',
                halfDay: Boolean(compose.values.halfDay),
            } : undefined,
        };

        return `${documentBodyHtml}${buildMeta(meta)}`;
    }, [documentBodyHtml, compose, currentSchema?.code, currentTemplate, isVacationForm]);

    const detailApprovalLines = detail?.document?.approvalLines || [];
    const detailOpinions = detailApprovalLines.filter((item) => item?.atrzOpnn);
    const detailStatusMeta = STATUS_META[detail?.document?.crntAtrzStepCd] || {
        label: detail?.document?.crntAtrzStepCd || '-',
        badge: 'badge-gray',
    };
    const statusTabs = section === 'draft' ? DRAFT_TABS : section === 'archive' ? ARCHIVE_TABS : [];
    const sectionCount = summary?.[SECTIONS.find((item) => item.key === section)?.countKey] || 0;

    useEffect(() => {
        setFilters({
            keyword: searchParams.get('keyword') || '',
            templateId: searchParams.get('templateId') || '',
            dateFrom: searchParams.get('dateFrom') || '',
            dateTo: searchParams.get('dateTo') || '',
        });
    }, [searchParams]);

    const refreshSummary = useCallback(async () => {
        const response = await approvalAPI.summary().catch(() => ({ data: { approvalCount: 0, counts: {} } }));
        setSummary(normalizeSummary(response.data));
    }, []);

    const loadComposeSupport = useCallback(async (force = false) => {
        if ((composeSupportLoaded && !force) || loadingComposeSupport) {
            return;
        }

        setLoadingComposeSupport(true);
        try {
            const [usersRes, bookmarkRes] = await Promise.all([
                usersAPI.list(),
                approvalAPI.customLines().catch(() => ({ data: [] })),
            ]);
            setUsers(usersRes.data || []);
            setBookmarks(bookmarkRes.data || []);
            setComposeSupportLoaded(true);
        } catch (error) {
            setErrorMessage(error.response?.data?.message || error.message || '결재 지원 데이터를 불러오지 못했습니다.');
        } finally {
            setLoadingComposeSupport(false);
        }
    }, [composeSupportLoaded, loadingComposeSupport]);

    const loadTempItems = useCallback(async (force = false) => {
        if ((tempItemsLoaded && !force) || loadingTempItems) {
            return;
        }

        setLoadingTempItems(true);
        try {
            const response = await approvalAPI.tempList().catch(() => ({ data: [] }));
            setTempItems(response.data || []);
            setTempItemsLoaded(true);
        } catch (error) {
            setErrorMessage(error.response?.data?.message || error.message || '임시 저장 문서를 불러오지 못했습니다.');
        } finally {
            setLoadingTempItems(false);
        }
    }, [loadingTempItems, tempItemsLoaded]);

    useEffect(() => {
        let active = true;

        async function loadBootstrap() {
            setLoadingBootstrap(true);
            setErrorMessage('');

            try {
                const [templateRes, summaryRes] = await Promise.all([
                    approvalAPI.templates(),
                    approvalAPI.summary().catch(() => ({ data: { approvalCount: 0, counts: {} } })),
                ]);

                if (!active) {
                    return;
                }

                setTemplates(templateRes.data || []);
                setSummary(normalizeSummary(summaryRes.data));
            } catch (error) {
                if (!active) {
                    return;
                }
                setErrorMessage(error.response?.data?.message || error.message || '전자결재 초기 데이터를 불러오지 못했습니다.');
            } finally {
                if (active) {
                    setLoadingBootstrap(false);
                }
            }
        }

        loadBootstrap();
        return () => {
            active = false;
        };
    }, []);

    useEffect(() => {
        if (loadingBootstrap || (mode !== 'create' && mode !== 'temp')) {
            return;
        }

        loadComposeSupport();
    }, [loadingBootstrap, mode, loadComposeSupport]);

    useEffect(() => {
        if (loadingBootstrap || (section !== 'temp' && mode !== 'temp')) {
            return;
        }

        loadTempItems();
    }, [loadingBootstrap, section, mode, loadTempItems]);

    useEffect(() => {
        if (loadingBootstrap || section === 'temp') {
            return;
        }

        let active = true;

        async function loadList() {
            setLoadingList(true);
            setErrorMessage('');

            try {
                const response = await approvalAPI.list({
                    section,
                    status,
                    page,
                    keyword: filters.keyword,
                    templateId: filters.templateId,
                    dateFrom: filters.dateFrom,
                    dateTo: filters.dateTo,
                });

                if (!active) {
                    return;
                }

                setListData({
                    section: response.data?.section || section,
                    status: response.data?.status || status,
                    items: response.data?.items || [],
                    page: numberValue(response.data?.page, page),
                    totalPages: Math.max(1, numberValue(response.data?.totalPages, 1)),
                    totalRecords: numberValue(response.data?.totalRecords, 0),
                    statusCounts: response.data?.statusCounts || {},
                });
            } catch (error) {
                if (!active) {
                    return;
                }
                setListData((prev) => ({ ...prev, items: [] }));
                setErrorMessage(error.response?.data?.message || error.message || '문서 목록을 불러오지 못했습니다.');
            } finally {
                if (active) {
                    setLoadingList(false);
                }
            }
        }

        loadList();
        return () => {
            active = false;
        };
    }, [loadingBootstrap, section, status, page, filters.keyword, filters.templateId, filters.dateFrom, filters.dateTo]);

    useEffect(() => {
        if (loadingBootstrap || mode !== 'detail' || !atrzDocId) {
            setDetail(null);
            return;
        }

        let active = true;

        async function loadDetail() {
            setLoadingDetail(true);
            setErrorMessage('');

            try {
                const response = await approvalAPI.detail(atrzDocId);
                if (!active) {
                    return;
                }
                setDetail(response.data);
                setActionOpinion('');
                setOtpCode('');
            } catch (error) {
                if (!active) {
                    return;
                }
                setDetail(null);
                setErrorMessage(error.response?.data?.message || error.message || '문서 상세를 불러오지 못했습니다.');
            } finally {
                if (active) {
                    setLoadingDetail(false);
                }
            }
        }

        loadDetail();
        return () => {
            active = false;
        };
    }, [loadingBootstrap, mode, atrzDocId]);

    useEffect(() => {
        if (loadingBootstrap || mode !== 'temp' || !tempId || templates.length === 0) {
            return;
        }

        let active = true;

        async function loadTempDetail() {
            setLoadingDetail(true);
            setErrorMessage('');

            try {
                const response = await approvalAPI.tempDetail(tempId);
                if (!active) {
                    return;
                }

                const temp = response.data?.temp;
                const template = templates.find((item) => item.atrzDocTmplId === temp?.atrzDocTmplId);
                const meta = extractMeta(temp?.htmlData || '');
                setCompose(makeCompose(template, {
                    meta,
                    atrzTempSqn: temp?.atrzTempSqn,
                    atrzDocTtl: temp?.atrzDocTtl,
                    atrzFileId: temp?.atrzFileId,
                    existingFiles: response.data?.attachments || [],
                    htmlData: temp?.htmlData,
                    openYn: temp?.openYn,
                }));
            } catch (error) {
                if (!active) {
                    return;
                }
                setErrorMessage(error.response?.data?.message || error.message || '임시저장 문서를 불러오지 못했습니다.');
            } finally {
                if (active) {
                    setLoadingDetail(false);
                }
            }
        }

        loadTempDetail();
        return () => {
            active = false;
        };
    }, [loadingBootstrap, mode, tempId, templates]);

    useEffect(() => {
        if (!currentTemplate || !isVacationForm) {
            setVacationBalance(0);
            return;
        }

        let active = true;

        async function loadVacationBalance() {
            try {
                const response = await approvalAPI.vacationBalance();
                if (active) {
                    setVacationBalance(numberValue(response.data, 0));
                }
            } catch {
                if (active) {
                    setVacationBalance(0);
                }
            }
        }

        loadVacationBalance();
        return () => {
            active = false;
        };
    }, [currentTemplate, isVacationForm]);

    async function refreshSupportData(options = {}) {
        await refreshSummary();
        if (options.includeComposeSupport) {
            await loadComposeSupport(true);
        }
        if (options.includeTempItems) {
            await loadTempItems(true);
        }
    }

    function setParams(patch) {
        setSearchParams(buildSearchParams(searchParams, patch));
    }

    function resetCompose(templateId = '') {
        if (!templateId) {
            setCompose(EMPTY_COMPOSE);
            return;
        }

        const template = templates.find((item) => item.atrzDocTmplId === templateId);
        if (template) {
            setCompose(makeCompose(template));
        }
    }

    function openSection(nextSection) {
        const nextStatus = defaultStatus(nextSection);
        setFeedback('');
        setErrorMessage('');
        setDetail(null);
        setCompose(EMPTY_COMPOSE);
        setParams({
            section: nextSection,
            status: nextStatus,
            page: 1,
            mode: nextSection === 'temp' ? 'list' : '',
            atrzDocId: '',
            tempId: '',
        });
    }

    function openCreate(templateId = '') {
        setFeedback('');
        setErrorMessage('');
        resetCompose(templateId);
        setParams({
            section: 'draft',
            status: 'progress',
            page: 1,
            mode: 'create',
            atrzDocId: '',
            tempId: '',
        });
    }

    function openDetail(docId) {
        setFeedback('');
        setErrorMessage('');
        setParams({
            mode: 'detail',
            atrzDocId: docId,
            tempId: '',
        });
    }

    function closeDetailPanel() {
        setFeedback('');
        setErrorMessage('');
        setDetail(null);
        setParams({
            mode: '',
            atrzDocId: '',
        });
    }

    function openTempDocument(nextTempId) {
        setFeedback('');
        setErrorMessage('');
        setParams({
            section: 'temp',
            status: 'all',
            page: 1,
            mode: 'temp',
            tempId: nextTempId,
            atrzDocId: '',
        });
    }

    function closeCompose() {
        setCompose(EMPTY_COMPOSE);
        setParams({
            mode: '',
            tempId: '',
        });
    }

    function handleFilterChange(name, value) {
        setFilters((prev) => ({ ...prev, [name]: value }));
    }

    function applyFilters() {
        setParams({
            page: 1,
            keyword: filters.keyword,
            templateId: filters.templateId,
            dateFrom: filters.dateFrom,
            dateTo: filters.dateTo,
        });
    }

    function resetFilters() {
        setFilters(FILTERS);
        setParams({
            page: 1,
            keyword: '',
            templateId: '',
            dateFrom: '',
            dateTo: '',
        });
    }

    function handleSectionStatus(nextStatus) {
        setParams({
            status: nextStatus,
            page: 1,
            atrzDocId: '',
            mode: '',
        });
    }

    function handleTemplateSelect(templateId) {
        const template = templates.find((item) => item.atrzDocTmplId === templateId);
        if (!template) {
            setCompose(EMPTY_COMPOSE);
            return;
        }
        setCompose(makeCompose(template));
    }

    function updateComposeField(name, value) {
        setCompose((prev) => ({ ...prev, [name]: value }));
    }

    function updateGenericField(field, value) {
        const fieldKey = field.key || field.id;
        setCompose((prev) => ({
            ...prev,
            values: {
                ...prev.values,
                [fieldKey]: value,
            },
        }));
    }

    function updateRangeField(field, key, value) {
        const fieldKey = field.key || field.id;
        setCompose((prev) => ({
            ...prev,
            values: {
                ...prev.values,
                [fieldKey]: {
                    ...(prev.values[fieldKey] || { start: '', end: '' }),
                    [key]: value,
                },
            },
        }));
    }

    function updateCheckboxField(field, checked) {
        const fieldKey = field.key || field.id;
        setCompose((prev) => ({
            ...prev,
            values: {
                ...prev.values,
                [fieldKey]: checked,
            },
        }));
    }

    function toggleApprovalUser(userId) {
        setCompose((prev) => {
            const exists = prev.approvalLines.includes(userId);
            return {
                ...prev,
                approvalLines: exists
                    ? prev.approvalLines.filter((item) => item !== userId)
                    : [...prev.approvalLines, userId],
            };
        });
    }

    function moveApprovalUser(index, direction) {
        setCompose((prev) => {
            const next = [...prev.approvalLines];
            const target = direction === 'up' ? index - 1 : index + 1;
            if (target < 0 || target >= next.length) {
                return prev;
            }
            [next[index], next[target]] = [next[target], next[index]];
            return {
                ...prev,
                approvalLines: next,
            };
        });
    }

    function toggleReceiverUser(userId) {
        setCompose((prev) => {
            const exists = prev.receiverIds.includes(userId);
            return {
                ...prev,
                receiverIds: exists
                    ? prev.receiverIds.filter((item) => item !== userId)
                    : [...prev.receiverIds, userId],
            };
        });
    }

    function handleFilesSelected(event) {
        const files = Array.from(event.target.files || []);
        setCompose((prev) => ({
            ...prev,
            newFiles: files,
        }));
    }

    function buildSubmissionPayload() {
        return {
            atrzTempSqn: compose.atrzTempSqn || undefined,
            atrzDocTmplId: compose.atrzDocTmplId,
            atrzDocTtl: compose.atrzDocTtl.trim(),
            htmlData: documentHtml,
            openYn: compose.openYn,
            atrzFileId: compose.existingFileId || '',
            receiverIds: compose.receiverIds,
            approvalLines: compose.approvalLines.map((approvalUserId) => ({
                atrzApprUserId: approvalUserId,
                apprAtrzYn: 'N',
            })),
        };
    }

    async function handleSaveTemp() {
        if (!compose.atrzDocTmplId || !compose.atrzDocTtl.trim() || !documentHtml) {
            setErrorMessage('양식, 제목, 본문을 먼저 준비해야 임시저장할 수 있습니다.');
            return;
        }

        setSubmitting(true);
        setFeedback('');
        setErrorMessage('');

        try {
            const payload = buildSubmissionPayload();
            let response;

            if (compose.atrzTempSqn) {
                response = await approvalAPI.updateTemp(compose.atrzTempSqn, payload, compose.newFiles);
            } else {
                response = await approvalAPI.saveTemp(payload, compose.newFiles);
            }

            await refreshSupportData({ includeTempItems: true });
            setFeedback('임시저장 문서를 저장했습니다.');
            openTempDocument(response.data?.atrzTempSqn || compose.atrzTempSqn);
        } catch (error) {
            setErrorMessage(error.response?.data?.message || error.message || '임시저장에 실패했습니다.');
        } finally {
            setSubmitting(false);
        }
    }

    async function handleSubmit() {
        if (!compose.atrzDocTmplId || !compose.atrzDocTtl.trim() || !documentHtml) {
            setErrorMessage('양식, 제목, 본문은 필수입니다.');
            return;
        }

        const missingLabel = missingRequiredFieldLabel(currentSchema, compose.values);
        if (missingLabel) {
            setErrorMessage(`${missingLabel} 항목을 입력해 주세요.`);
            return;
        }

        if (compose.approvalLines.length === 0) {
            setErrorMessage('결재선을 한 명 이상 선택해야 합니다.');
            return;
        }
        if (isVacationForm && usedVacationDays === 0) {
            setErrorMessage('휴가 시작일과 종료일을 확인해 주세요.');
            return;
        }
        if (isVacationForm && compose.values.halfDay && compose.values.period?.start !== compose.values.period?.end) {
            setErrorMessage('반차는 하루 일정으로만 신청할 수 있습니다.');
            return;
        }

        setSubmitting(true);
        setFeedback('');
        setErrorMessage('');

        try {
            const response = await approvalAPI.create(buildSubmissionPayload(), compose.newFiles);
            await refreshSupportData();
            setFeedback('문서를 상신했습니다.');
            setCompose(EMPTY_COMPOSE);
            setParams({
                section: 'draft',
                status: 'progress',
                page: 1,
                mode: 'detail',
                atrzDocId: response.data?.atrzDocId,
                tempId: '',
            });
        } catch (error) {
            setErrorMessage(error.response?.data?.message || error.message || '문서 상신에 실패했습니다.');
        } finally {
            setSubmitting(false);
        }
    }

    async function handleDeleteTemp() {
        if (!compose.atrzTempSqn) {
            return;
        }

        setSubmitting(true);
        setFeedback('');
        setErrorMessage('');

        try {
            await approvalAPI.deleteTemp(compose.atrzTempSqn);
            await refreshSupportData({ includeTempItems: true });
            setFeedback('임시저장 문서를 삭제했습니다.');
            setCompose(EMPTY_COMPOSE);
            setParams({
                section: 'temp',
                status: 'all',
                page: 1,
                mode: '',
                tempId: '',
                atrzDocId: '',
            });
        } catch (error) {
            setErrorMessage(error.response?.data?.message || error.message || '임시저장 문서를 삭제하지 못했습니다.');
        } finally {
            setSubmitting(false);
        }
    }

    async function handleBookmarkSave() {
        if (!bookmarkName.trim()) {
            setErrorMessage('즐겨찾기 이름을 입력해 주세요.');
            return;
        }
        if (compose.approvalLines.length === 0) {
            setErrorMessage('즐겨찾기로 저장할 결재선이 없습니다.');
            return;
        }

        setErrorMessage('');
        setFeedback('');

        try {
            const payload = compose.approvalLines.map((approvalUserId, index) => ({
                cstmLineBmNm: bookmarkName.trim(),
                atrzLineSeq: index + 1,
                atrzApprId: approvalUserId,
                apprAtrzYn: 'N',
            }));

            await approvalAPI.createCustomLine(payload);
            const response = await approvalAPI.customLines();
            setBookmarks(response.data || []);
            setBookmarkName('');
            setFeedback('결재선 즐겨찾기를 저장했습니다.');
        } catch (error) {
            setErrorMessage(error.response?.data?.message || error.message || '결재선 즐겨찾기를 저장하지 못했습니다.');
        }
    }

    async function handleBookmarkDelete(name) {
        setErrorMessage('');
        setFeedback('');

        try {
            await approvalAPI.deleteCustomLine(name);
            const response = await approvalAPI.customLines();
            setBookmarks(response.data || []);
            setFeedback('결재선 즐겨찾기를 삭제했습니다.');
        } catch (error) {
            setErrorMessage(error.response?.data?.message || error.message || '결재선 즐겨찾기를 삭제하지 못했습니다.');
        }
    }

    function applyBookmark(lines) {
        setCompose((prev) => ({
            ...prev,
            approvalLines: lines.map((item) => lineUserId(item)).filter(Boolean),
        }));
    }

    async function handleDocumentAction(type) {
        if (!detail?.document?.atrzDocId) {
            return;
        }

        setProcessingAction(true);
        setErrorMessage('');
        setFeedback('');

        try {
            let response;

            if (type === 'approve') {
                response = await approvalAPI.approve(detail.document.atrzDocId, {
                    opinion: actionOpinion,
                    otpCode: otpCode ? Number(otpCode) : null,
                    htmlData: stripMeta(detail.document.htmlData),
                });
            } else if (type === 'reject') {
                response = await approvalAPI.reject(detail.document.atrzDocId, {
                    opinion: actionOpinion,
                    otpCode: otpCode ? Number(otpCode) : null,
                });
            } else {
                response = await approvalAPI.retract(detail.document.atrzDocId);
            }

            setDetail(response.data?.detail || response.data);
            setActionOpinion('');
            setOtpCode('');
            await refreshSupportData();
            setFeedback(type === 'approve'
                ? '문서를 승인했습니다.'
                : type === 'reject'
                    ? '문서를 반려했습니다.'
                    : '문서를 회수했습니다.');
        } catch (error) {
            setErrorMessage(error.response?.data?.message || error.message || '문서 처리를 완료하지 못했습니다.');
        } finally {
            setProcessingAction(false);
        }
    }

    function renderSectionCount(key) {
        return numberValue(summary?.[key], 0);
    }

    function renderStatusBadge(code) {
        const meta = STATUS_META[code] || { label: code || '상태 없음', badge: 'badge-gray' };
        return <span className={`badge ${meta.badge}`}>{meta.label}</span>;
    }

    function renderListTable() {
        if (section === 'temp') {
            return (
                <div className="approval-card approval-list-card approval-list-card--temp">
                    <div className="approval-list-toolbar approval-list-toolbar--simple">
                        <div className="approval-card-header approval-card-header--dense">
                            <div>
                                <h3>임시저장 문서</h3>
                                <p>{tempItems.length}건의 문서를 다시 열어 수정할 수 있습니다.</p>
                            </div>
                        </div>
                        <div className="approval-list-toolbar-actions">
                            <button type="button" className="btn btn-secondary" onClick={() => openCreate()}>
                                새 문서 작성
                            </button>
                        </div>
                    </div>
                    {loadingTempItems && !tempItemsLoaded ? (
                        <div className="approval-empty">
                            <strong>임시 저장 문서를 불러오는 중입니다.</strong>
                        </div>
                    ) : tempItems.length === 0 ? (
                        <div className="approval-empty">
                            <strong>임시저장 문서가 없습니다.</strong>
                            <p>작성 중인 문서를 저장하면 이곳에서 다시 이어서 작업할 수 있습니다.</p>
                        </div>
                    ) : (
                        <div className="approval-table-wrap">
                            <table className="data-table approval-table approval-table--temp">
                                <colgroup>
                                    <col className="approval-col-title" />
                                    <col className="approval-col-template" />
                                    <col className="approval-col-date" />
                                </colgroup>
                                <thead>
                                    <tr>
                                        <th>제목</th>
                                        <th>양식</th>
                                        <th>저장일</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {tempItems.map((item) => (
                                        <tr
                                            key={item.atrzTempSqn}
                                            className={tempId === item.atrzTempSqn && mode === 'temp' ? 'active' : ''}
                                            onClick={() => openTempDocument(item.atrzTempSqn)}
                                        >
                                            <td className="approval-cell-title">
                                                <div className="approval-table-title approval-table-title--single">
                                                    <strong title={item.atrzDocTtl || '제목 없음'}>{item.atrzDocTtl || '제목 없음'}</strong>
                                                </div>
                                            </td>
                                            <td className="approval-cell-template" title={item.atrzDocTmplNm || '-'}>
                                                {item.atrzDocTmplNm || '-'}
                                            </td>
                                            <td className="approval-cell-date">{formatDateTime(item.atrzSbmtDt)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            );
        }

        return (
            <div className="approval-card approval-list-card">
                <div className="approval-list-toolbar">
                    <div className="approval-card-header approval-card-header--dense">
                        <div>
                            <h3>{SECTIONS.find((item) => item.key === section)?.label}</h3>
                            <p>{listData.totalRecords}건의 문서가 조회되었습니다.</p>
                        </div>
                    </div>
                    <div className="approval-list-toolbar-actions">
                        <button type="button" className="btn btn-outline" onClick={() => navigate('/approval/contracts')}>
                            전자계약 서비스
                        </button>
                        <button type="button" className="btn btn-primary" onClick={() => openCreate()}>
                            문서 작성하기
                        </button>
                    </div>
                </div>

                {statusTabs.length > 0 && (
                    <div className="tabs approval-status-tabs">
                        {statusTabs.map((tab) => (
                            <button
                                key={tab.key}
                                type="button"
                                className={`tab-btn ${status === tab.key ? 'active' : ''}`}
                                onClick={() => handleSectionStatus(tab.key)}
                            >
                                {tab.label}
                                <span className="approval-tab-count">{numberValue(listData.statusCounts?.[tab.key], 0)}</span>
                            </button>
                        ))}
                    </div>
                )}

                <div className="approval-filter-bar">
                    <div className="approval-filter-grid">
                        <input
                            className="form-input"
                            type="search"
                            value={filters.keyword}
                            onChange={(event) => handleFilterChange('keyword', event.target.value)}
                            placeholder="제목, 기안자, 양식 검색"
                        />
                        <select
                            className="form-input"
                            value={filters.templateId}
                            onChange={(event) => handleFilterChange('templateId', event.target.value)}
                        >
                            <option value="">전체 양식</option>
                            {templates.map((template) => (
                                <option key={template.atrzDocTmplId} value={template.atrzDocTmplId}>
                                    {template.atrzDocTmplNm}
                                </option>
                            ))}
                        </select>
                        <input
                            className="form-input"
                            type="date"
                            value={filters.dateFrom}
                            onChange={(event) => handleFilterChange('dateFrom', event.target.value)}
                        />
                        <input
                            className="form-input"
                            type="date"
                            value={filters.dateTo}
                            onChange={(event) => handleFilterChange('dateTo', event.target.value)}
                        />
                    </div>
                    <div className="approval-filter-actions">
                        <button type="button" className="btn btn-secondary" onClick={resetFilters}>
                            초기화
                        </button>
                        <button type="button" className="btn btn-primary" onClick={applyFilters}>
                            조회
                        </button>
                    </div>
                </div>

                {loadingList ? (
                    <div className="approval-empty">
                        <strong>문서 목록을 불러오는 중입니다.</strong>
                    </div>
                ) : listData.items.length === 0 ? (
                    <div className="approval-empty">
                        <strong>조건에 맞는 문서가 없습니다.</strong>
                        <p>필터를 조정하거나 새 문서를 올려서 결재 흐름을 시작할 수 있습니다.</p>
                    </div>
                ) : (
                    <>
                        <div className="approval-table-wrap">
                            <table className="data-table approval-table approval-table--documents">
                                <colgroup>
                                    <col className="approval-col-title" />
                                    <col className="approval-col-template" />
                                    <col className="approval-col-drafter" />
                                    <col className="approval-col-status" />
                                    <col className="approval-col-date" />
                                </colgroup>
                                <thead>
                                    <tr>
                                        <th>제목</th>
                                        <th>양식</th>
                                        <th>기안자</th>
                                        <th>상태</th>
                                        <th>기안일</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {listData.items.map((item) => (
                                        <tr
                                            key={item.atrzDocId}
                                            className={atrzDocId === item.atrzDocId && mode === 'detail' ? 'active' : ''}
                                            onClick={() => openDetail(item.atrzDocId)}
                                        >
                                            <td className="approval-cell-title">
                                                <div className="approval-table-title approval-table-title--single">
                                                    <strong title={item.atrzDocTtl}>{item.atrzDocTtl}</strong>
                                                </div>
                                            </td>
                                            <td className="approval-cell-template" title={item.atrzDocTmplNm}>
                                                {item.atrzDocTmplNm}
                                            </td>
                                            <td className="approval-cell-drafter" title={item.drafterName || '-'}>
                                                {item.drafterName || '-'}
                                            </td>
                                            <td className="approval-cell-status">{renderStatusBadge(item.crntAtrzStepCd)}</td>
                                            <td className="approval-cell-date">{formatDateTime(item.atrzSbmtDt)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        <div className="approval-pagination">
                            <button
                                type="button"
                                className="btn btn-secondary"
                                disabled={page <= 1}
                                onClick={() => setParams({ page: page - 1 })}
                            >
                                이전
                            </button>
                            {pageNumbers(listData.totalPages, page).map((pageNo) => (
                                <button
                                    key={pageNo}
                                    type="button"
                                    className={`btn ${pageNo === page ? 'btn-primary' : 'btn-secondary'}`}
                                    onClick={() => setParams({ page: pageNo })}
                                >
                                    {pageNo}
                                </button>
                            ))}
                            <button
                                type="button"
                                className="btn btn-secondary"
                                disabled={page >= listData.totalPages}
                                onClick={() => setParams({ page: page + 1 })}
                            >
                                다음
                            </button>
                        </div>
                    </>
                )}
            </div>
        );
    }

    function renderDocumentControl(field) {
        const fieldKey = field.key || field.id;
        const value = compose.values[fieldKey] ?? defaultFieldValue(field);
        const placeholder = field.placeholder || `${field.label}을 입력하세요`;

        if (field.type === 'textarea') {
            return (
                <textarea
                    className="form-input approval-doc-control approval-doc-control--textarea"
                    value={value || ''}
                    onChange={(event) => updateGenericField(field, event.target.value)}
                    placeholder={placeholder}
                />
            );
        }

        if (field.type === 'daterange' || field.type === 'range') {
            return (
                <div className="approval-range-inputs approval-doc-range">
                    <input
                        className="form-input approval-doc-control"
                        type="date"
                        value={value?.start || ''}
                        onChange={(event) => updateRangeField(field, 'start', event.target.value)}
                    />
                    <span>~</span>
                    <input
                        className="form-input approval-doc-control"
                        type="date"
                        value={value?.end || ''}
                        onChange={(event) => updateRangeField(field, 'end', event.target.value)}
                    />
                </div>
            );
        }

        if (field.type === 'radio') {
            return (
                <div className="approval-doc-option-group">
                    {field.options.map((option) => (
                        <label key={option} className={`approval-doc-pill ${value === option ? 'active' : ''}`}>
                            <input
                                type="radio"
                                name={fieldKey}
                                checked={value === option}
                                onChange={() => updateGenericField(field, option)}
                            />
                            <span>{option}</span>
                        </label>
                    ))}
                </div>
            );
        }

        if (field.type === 'select') {
            return (
                <select
                    className="form-input approval-doc-control"
                    value={value || field.defaultValue || ''}
                    onChange={(event) => updateGenericField(field, event.target.value)}
                >
                    <option value="">선택하세요</option>
                    {field.options.map((option) => (
                        <option key={option} value={option}>
                            {option}
                        </option>
                    ))}
                </select>
            );
        }

        if (field.type === 'user-select') {
            return (
                <select
                    className="form-input approval-doc-control"
                    value={value || ''}
                    onChange={(event) => updateGenericField(field, event.target.value)}
                >
                    <option value="">{field.placeholder || '선택 안 함'}</option>
                    {availableUsers.map((candidate) => (
                        <option key={candidate.userId} value={candidate.userNm}>
                            {userLabel(candidate)}
                        </option>
                    ))}
                </select>
            );
        }

        if (field.type === 'checkbox') {
            return (
                <label className={`approval-doc-checkbox ${value ? 'active' : ''}`}>
                    <input
                        type="checkbox"
                        checked={Boolean(value)}
                        onChange={(event) => updateCheckboxField(field, event.target.checked)}
                    />
                    <span>{field.optionLabel || field.label}</span>
                </label>
            );
        }

        return (
            <input
                className="form-input approval-doc-control"
                type={field.type === 'number' ? 'number' : field.type === 'date' ? 'date' : 'text'}
                value={value || ''}
                onChange={(event) => updateGenericField(field, event.target.value)}
                placeholder={placeholder}
            />
        );
    }

    function renderComposeRow(fieldDefs, rowKey) {
        if (fieldDefs.length === 0) {
            return null;
        }

        if (fieldDefs.length === 1) {
            const [field] = fieldDefs;
            return (
                <tr key={rowKey}>
                    <th>{field.label}</th>
                    <td colSpan={3}>{renderDocumentControl(field)}</td>
                </tr>
            );
        }

        return (
            <tr key={rowKey}>
                {fieldDefs.flatMap((field) => ([
                    <th key={`${rowKey}-${field.key || field.id}-label`}>{field.label}</th>,
                    <td key={`${rowKey}-${field.key || field.id}-control`}>{renderDocumentControl(field)}</td>,
                ]))}
            </tr>
        );
    }

    function renderComposeInsights() {
        if (!currentSchema) {
            return null;
        }

        return (
            <>
                {isVacationForm && (
                    <div className="approval-doc-stat-grid">
                        <div className="approval-doc-stat">
                            <strong>잔여 연차</strong>
                            <span>{vacationBalance}일</span>
                        </div>
                        <div className="approval-doc-stat">
                            <strong>사용 예정</strong>
                            <span>{usedVacationDays}일</span>
                        </div>
                        <div className="approval-doc-stat">
                            <strong>승인 반영</strong>
                            <span>연차 사용 내역</span>
                        </div>
                    </div>
                )}
                {currentSchema.automationNote && (
                    <div className="approval-doc-callout">
                        <strong>승인 처리 안내</strong>
                        <p>{currentSchema.automationNote}</p>
                    </div>
                )}
            </>
        );
    }

    function renderSchemaDocument() {
        const schemaFieldMap = buildFieldIndex(currentSchema?.fields);

        return (
            <div className="approval-document-sheet approval-compose-document">
                <div className="approval-doc-header">
                    <div>
                        <span className="approval-doc-kicker">{currentSchema?.categoryLabel || currentTemplate?.atrzCategory || '전자결재'}</span>
                        <h2>{currentSchema?.heading || currentTemplate?.atrzDocTmplNm}</h2>
                        <p>{currentSchema?.description || currentTemplate?.atrzDescription || '문서형 양식 안에서 본문을 직접 작성합니다.'}</p>
                    </div>
                </div>

                <table className="approval-doc-table approval-doc-meta-table">
                    <tbody>
                        <tr>
                            <th>기안부서</th>
                            <td>{user?.deptNm || '-'}</td>
                            <th>직위</th>
                            <td>{user?.jbgdNm || '-'}</td>
                        </tr>
                        <tr>
                            <th>기안자</th>
                            <td>{user?.userNm || user?.userId || '-'}</td>
                            <th>기안일</th>
                            <td>{formatDateOnly()}</td>
                        </tr>
                        <tr>
                            <th>사번</th>
                            <td>{user?.userId || '-'}</td>
                            <th>보존연한</th>
                            <td>
                                <select
                                    className="form-input approval-doc-control approval-doc-control--compact"
                                    value={compose.saveYear}
                                    onChange={(event) => updateComposeField('saveYear', event.target.value)}
                                >
                                    {SAVE_YEAR_OPTIONS.map((year) => (
                                        <option key={year} value={year}>{year}년</option>
                                    ))}
                                </select>
                            </td>
                        </tr>
                    </tbody>
                </table>

                {renderComposeInsights()}

                <table className="approval-doc-table approval-doc-body-table">
                    <tbody>
                        <tr>
                            <th>제목</th>
                            <td colSpan={3}>
                                <input
                                    className="form-input approval-doc-control"
                                    value={compose.atrzDocTtl}
                                    onChange={(event) => updateComposeField('atrzDocTtl', event.target.value)}
                                    placeholder="문서 제목을 입력하세요"
                                />
                            </td>
                        </tr>
                        {currentSchema.layout.map((layoutRow, index) => renderComposeRow(
                            layoutRow.map((fieldKey) => schemaFieldMap.get(fieldKey)).filter(Boolean),
                            `schema-row-${index + 1}`
                        ))}
                    </tbody>
                </table>
            </div>
        );
    }

    function renderFallbackDocument() {
        return (
            <div className="approval-document-sheet approval-compose-document">
                <div className="approval-doc-header">
                    <div>
                        <span className="approval-doc-kicker">{currentTemplate?.atrzCategory || '전자결재'}</span>
                        <h2>{compose.heading || currentTemplate?.atrzDocTmplNm || '전자결재 문서'}</h2>
                        <p>{currentTemplate?.atrzDescription || '선택한 양식에 맞춰 문서를 작성합니다.'}</p>
                    </div>
                </div>

                <table className="approval-doc-table approval-doc-meta-table">
                    <tbody>
                        <tr>
                            <th>기안부서</th>
                            <td>{user?.deptNm || '-'}</td>
                            <th>직위</th>
                            <td>{user?.jbgdNm || '-'}</td>
                        </tr>
                        <tr>
                            <th>기안자</th>
                            <td>{user?.userNm || user?.userId || '-'}</td>
                            <th>기안일</th>
                            <td>{formatDateOnly()}</td>
                        </tr>
                        <tr>
                            <th>사번</th>
                            <td>{user?.userId || '-'}</td>
                            <th>보존연한</th>
                            <td>
                                <select
                                    className="form-input approval-doc-control approval-doc-control--compact"
                                    value={compose.saveYear}
                                    onChange={(event) => updateComposeField('saveYear', event.target.value)}
                                >
                                    {SAVE_YEAR_OPTIONS.map((year) => (
                                        <option key={year} value={year}>{year}년</option>
                                    ))}
                                </select>
                            </td>
                        </tr>
                    </tbody>
                </table>

                <table className="approval-doc-table approval-doc-body-table">
                    <tbody>
                        <tr>
                            <th>제목</th>
                            <td colSpan={3}>
                                <input
                                    className="form-input approval-doc-control"
                                    value={compose.atrzDocTtl}
                                    onChange={(event) => updateComposeField('atrzDocTtl', event.target.value)}
                                    placeholder="문서 제목을 입력하세요"
                                />
                            </td>
                        </tr>
                        {compose.fields.map((field, index) => renderComposeRow([field], `fallback-row-${index + 1}`))}
                    </tbody>
                </table>
            </div>
        );
    }

    function renderComposeForm() {
        const showTemplatePicker = !compose.atrzDocTmplId;

        return (
            <div className="approval-compose-layout">
                <div className="form-shell approval-compose-main">
                    <div className="page-header approval-compose-header">
                        <div>
                            <h2>{mode === 'temp' ? '임시저장 문서 편집' : '문서 작성하기'}</h2>
                            <p>문서형 양식 안에서 본문을 직접 작성하고, 우측에서 결재선과 옵션을 함께 구성합니다.</p>
                        </div>
                        <div className="page-header-actions approval-compose-actions">
                            <button type="button" className="btn btn-ghost" onClick={closeCompose}>
                                취소
                            </button>
                            {mode === 'temp' && compose.atrzTempSqn && (
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={handleDeleteTemp}
                                    disabled={submitting}
                                >
                                    임시저장 삭제
                                </button>
                            )}
                            <button type="button" className="btn btn-secondary" onClick={handleSaveTemp} disabled={submitting}>
                                {submitting ? '저장 중...' : '임시저장'}
                            </button>
                            <button type="button" className="btn btn-primary" onClick={handleSubmit} disabled={submitting}>
                                {submitting ? '상신 중...' : '기안 상신'}
                            </button>
                        </div>
                    </div>

                    <div className="form-section approval-compose-template">
                        <div className="approval-side-header">
                            <div className="form-section-title">양식 선택</div>
                            {currentTemplate && (
                                <span className="approval-small-muted">{currentSchema?.categoryLabel || currentTemplate.atrzCategory}</span>
                            )}
                        </div>
                        <select
                            className="form-input"
                            value={compose.atrzDocTmplId}
                            onChange={(event) => handleTemplateSelect(event.target.value)}
                        >
                            <option value="">양식을 선택하세요</option>
                            {templates.map((template) => (
                                <option key={template.atrzDocTmplId} value={template.atrzDocTmplId}>
                                    {template.atrzDocTmplNm}
                                </option>
                            ))}
                        </select>
                    </div>

                    {showTemplatePicker ? (
                        <div className="approval-card approval-template-picker">
                            <div className="approval-card-header">
                                <div>
                                    <h3>문서 양식 선택</h3>
                                    <p>작성할 양식을 먼저 고르면 바로 문서형 작성 화면으로 이동합니다.</p>
                                </div>
                            </div>
                            <div className="approval-template-grid">
                                {templates.map((template) => (
                                    <button
                                        key={template.atrzDocTmplId}
                                        type="button"
                                        className="approval-template-card"
                                        onClick={() => handleTemplateSelect(template.atrzDocTmplId)}
                                    >
                                        <strong>{template.atrzDocTmplNm}</strong>
                                        <span>{template.atrzDescription || template.atrzCategory}</span>
                                    </button>
                                ))}
                            </div>
                        </div>
                    ) : currentSchema ? renderSchemaDocument() : renderFallbackDocument()}
                </div>

                <aside className="approval-side-rail approval-compose-rail">
                    <div className="form-section approval-panel-group">
                        <div className="approval-panel-heading">
                            <div>
                                <div className="form-section-title">문서 옵션</div>
                                <p className="approval-panel-description">공개 여부와 기안 메모를 한곳에서 정리합니다.</p>
                            </div>
                        </div>
                        <div className="approval-toggle-list">
                            <label className="form-toggle">
                                <input
                                    type="checkbox"
                                    checked={compose.openYn === 'Y'}
                                    onChange={(event) => updateComposeField('openYn', event.target.checked ? 'Y' : 'N')}
                                />
                                공개 문서로 저장
                            </label>
                            <label className="form-toggle">
                                <input
                                    type="checkbox"
                                    checked={compose.urgentYn === 'Y'}
                                    onChange={(event) => updateComposeField('urgentYn', event.target.checked ? 'Y' : 'N')}
                                />
                                긴급 문서로 표시
                            </label>
                        </div>
                        <div className="approval-panel-block">
                            <div className="approval-panel-subtitle">기안 의견</div>
                            <textarea
                                className="form-input"
                                value={compose.note}
                                onChange={(event) => updateComposeField('note', event.target.value)}
                                placeholder="결재자에게 따로 전달할 배경이나 요청 사항을 입력하세요."
                            />
                        </div>
                    </div>

                    <div className="form-section approval-panel-group">
                        <div className="approval-panel-heading">
                            <div>
                                <div className="form-section-title">결재선</div>
                                <p className="approval-panel-description">선택, 순서 조정, 즐겨찾기를 한 흐름으로 정리합니다.</p>
                            </div>
                            <span className="approval-small-muted">{compose.approvalLines.length}명 선택</span>
                        </div>
                        <div className="approval-panel-block">
                            <div className="approval-panel-subtitle">선택된 결재선</div>
                            <div className="approval-line-list">
                                {compose.approvalLines.length === 0 ? (
                                    <div className="approval-empty compact">결재선을 선택해 주세요.</div>
                                ) : compose.approvalLines.map((approvalUserId, index) => {
                                    const candidate = pickUser(availableUsers, approvalUserId);
                                    return (
                                        <div key={approvalUserId} className="approval-line-item">
                                            <div>
                                                <strong>{index + 1}. {candidate?.userNm || approvalUserId}</strong>
                                                <span>{candidate ? `${candidate.deptNm} · ${candidate.jbgdNm}` : approvalUserId}</span>
                                            </div>
                                            <div className="approval-line-actions">
                                                <button type="button" className="btn btn-ghost" onClick={() => moveApprovalUser(index, 'up')}>위</button>
                                                <button type="button" className="btn btn-ghost" onClick={() => moveApprovalUser(index, 'down')}>아래</button>
                                                <button type="button" className="btn btn-ghost" onClick={() => toggleApprovalUser(approvalUserId)}>제거</button>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                        <div className="approval-panel-block">
                            <div className="approval-panel-subtitle">결재자 선택</div>
                            <div className="approval-choice-list">
                                {loadingComposeSupport && !composeSupportLoaded ? (
                                    <div className="approval-empty compact">결재선 목록을 불러오는 중입니다.</div>
                                ) : availableUsers.length === 0 ? (
                                    <div className="approval-empty compact">선택 가능한 사용자가 없습니다.</div>
                                ) : availableUsers.map((candidate) => (
                                    <label key={candidate.userId} className="approval-choice-item">
                                        <input
                                            type="checkbox"
                                            checked={compose.approvalLines.includes(candidate.userId)}
                                            onChange={() => toggleApprovalUser(candidate.userId)}
                                        />
                                        <span>{userLabel(candidate)}</span>
                                    </label>
                                ))}
                            </div>
                        </div>
                        <div className="approval-panel-block">
                            <div className="approval-side-header">
                                <div className="approval-panel-subtitle">결재선 즐겨찾기</div>
                            </div>
                            <div className="approval-bookmark-form">
                                <input
                                    className="form-input"
                                    value={bookmarkName}
                                    onChange={(event) => setBookmarkName(event.target.value)}
                                    placeholder="즐겨찾기 이름"
                                />
                                <button type="button" className="btn btn-secondary" onClick={handleBookmarkSave}>
                                    저장
                                </button>
                            </div>
                            <div className="approval-bookmark-list">
                                {loadingComposeSupport && !composeSupportLoaded ? (
                                    <div className="approval-empty compact">결재선 즐겨찾기를 불러오는 중입니다.</div>
                                ) : groupedBookmarks.length === 0 ? (
                                    <div className="approval-empty compact">저장된 결재선 즐겨찾기가 없습니다.</div>
                                ) : groupedBookmarks.map((bookmark) => (
                                    <div key={bookmark.name} className="approval-bookmark-item">
                                        <button type="button" className="approval-bookmark-apply" onClick={() => applyBookmark(bookmark.lines)}>
                                            <strong>{bookmark.name}</strong>
                                            <span>{bookmark.lines.map((item) => pickUser(users, lineUserId(item))?.userNm || lineUserId(item)).join(' → ')}</span>
                                        </button>
                                        <button
                                            type="button"
                                            className="btn btn-ghost"
                                            onClick={() => handleBookmarkDelete(bookmark.name)}
                                        >
                                            삭제
                                        </button>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>

                    <div className="form-section approval-panel-group">
                        <div className="approval-panel-heading">
                            <div>
                                <div className="form-section-title">참조 및 첨부</div>
                                <p className="approval-panel-description">참조자 지정과 파일 정리를 한 패널에서 처리합니다.</p>
                            </div>
                        </div>
                        <div className="approval-panel-block">
                            <div className="approval-side-header">
                                <div className="approval-panel-subtitle">참조자</div>
                                <span className="approval-small-muted">{compose.receiverIds.length}명 선택</span>
                            </div>
                            <div className="approval-choice-list">
                                {loadingComposeSupport && !composeSupportLoaded ? (
                                    <div className="approval-empty compact">참조자 목록을 불러오는 중입니다.</div>
                                ) : availableUsers.length === 0 ? (
                                    <div className="approval-empty compact">선택 가능한 사용자가 없습니다.</div>
                                ) : availableUsers.map((candidate) => (
                                    <label key={candidate.userId} className="approval-choice-item">
                                        <input
                                            type="checkbox"
                                            checked={compose.receiverIds.includes(candidate.userId)}
                                            onChange={() => toggleReceiverUser(candidate.userId)}
                                        />
                                        <span>{userLabel(candidate)}</span>
                                    </label>
                                ))}
                            </div>
                        </div>
                        <div className="approval-panel-block">
                            <div className="approval-panel-subtitle">첨부파일</div>
                            <div className="form-file">
                                <label className="form-file-control" htmlFor="approval-files">
                                    <span className="form-file-button">파일 선택</span>
                                    <span className="form-file-name">
                                        {compose.newFiles.length > 0
                                            ? compose.newFiles.map((file) => file.name).join(', ')
                                            : '새 첨부파일을 선택하세요'}
                                    </span>
                                </label>
                                <input
                                    id="approval-files"
                                    className="form-file-input"
                                    type="file"
                                    multiple
                                    onChange={handleFilesSelected}
                                />
                            </div>
                            {compose.existingFiles.length > 0 && compose.newFiles.length === 0 && (
                                <div className="approval-file-list">
                                    {compose.existingFiles.map((file) => (
                                        <a
                                            key={`${file.fileId}-${file.fileSeq}`}
                                            className="approval-file-link"
                                            href={file.filePath}
                                            target="_blank"
                                            rel="noreferrer"
                                        >
                                            {fileLabel(file)}
                                        </a>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                </aside>
            </div>
        );
    }

    function renderDetailPanel() {
        if (mode !== 'detail') {
            return null;
        }

        if (loadingDetail) {
            return (
                <div className="approval-card approval-detail-card">
                    <div className="approval-empty">
                        <strong>문서 상세를 불러오는 중입니다.</strong>
                    </div>
                </div>
            );
        }

        if (!detail?.document) {
            return (
                <div className="approval-card approval-detail-card">
                    <div className="approval-empty">
                        <strong>상세 문서를 표시할 수 없습니다.</strong>
                    </div>
                </div>
            );
        }

        return (
            <div className="approval-card approval-detail-card">
                <div className="approval-card-header approval-detail-heading">
                    <div className="approval-detail-header-main">
                        <span className="approval-detail-kicker">{detail.template?.atrzDocTmplNm || detail.document.atrzDocTmplNm || '전자결재 문서'}</span>
                        <h3>{detail.document.atrzDocTtl}</h3>
                        <p>{detail.template?.atrzDescription || detail.template?.atrzDocTmplNm || detail.document.atrzDocTmplNm}</p>
                    </div>
                    <div className="approval-action-row approval-detail-actions">
                        {detail.canRetract && (
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => handleDocumentAction('retract')}
                                disabled={processingAction}
                            >
                                회수
                            </button>
                        )}
                        {detail.canReject && (
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => handleDocumentAction('reject')}
                                disabled={processingAction}
                            >
                                반려
                            </button>
                        )}
                        {detail.canApprove && (
                            <button
                                type="button"
                                className="btn btn-primary"
                                onClick={() => handleDocumentAction('approve')}
                                disabled={processingAction}
                            >
                                승인
                            </button>
                        )}
                        <button type="button" className="btn btn-ghost approval-detail-close" onClick={closeDetailPanel}>
                            닫기
                        </button>
                    </div>
                </div>

                <div className="approval-detail-layout">
                    <div className="approval-detail-main">
                        <div className="approval-meta-grid">
                            <div className="approval-meta-item">
                                <span>기안자</span>
                                <strong>{detail.document.drafterName || detail.document.atrzUserId}</strong>
                            </div>
                            <div className="approval-meta-item">
                                <span>기안일</span>
                                <strong>{formatDateTime(detail.document.atrzSbmtDt)}</strong>
                            </div>
                            <div className="approval-meta-item">
                                <span>상태</span>
                                <strong>
                                    <span className={`badge ${detailStatusMeta.badge}`}>{detailStatusMeta.label}</span>
                                </strong>
                            </div>
                            <div className="approval-meta-item">
                                <span>현재 순번</span>
                                <strong>{detail.currentSeq ? `${detail.currentSeq}차` : '-'}</strong>
                            </div>
                        </div>

                        {(detail.canApprove || detail.canReject) && (
                            <div className="form-section">
                                <div className="form-section-title">결재 의견</div>
                                <textarea
                                    className="form-input"
                                    value={actionOpinion}
                                    onChange={(event) => setActionOpinion(event.target.value)}
                                    placeholder="승인 또는 반려 의견을 입력하세요."
                                />
                                <label>
                                    <div className="form-section-title">OTP 코드</div>
                                    <input
                                        className="form-input"
                                        type="number"
                                        value={otpCode}
                                        onChange={(event) => setOtpCode(event.target.value)}
                                        placeholder="OTP를 사용하는 경우에만 입력"
                                    />
                                </label>
                            </div>
                        )}

                        <div className="approval-document-sheet" dangerouslySetInnerHTML={{ __html: stripMeta(detail.document.htmlData) }} />

                        <div className="approval-card nested">
                            <div className="approval-card-header">
                                <div>
                                    <h3>결재 의견 이력</h3>
                                </div>
                            </div>
                            {detailOpinions.length === 0 ? (
                                <div className="approval-empty compact">등록된 결재 의견이 없습니다.</div>
                            ) : (
                                <div className="approval-history-table-wrap">
                                    <table className="data-table approval-history-table">
                                        <thead>
                                            <tr>
                                                <th>순번</th>
                                                <th>결재자</th>
                                                <th>상태</th>
                                                <th>처리일</th>
                                                <th>의견</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {detailOpinions.map((line, index) => {
                                                const lineStatusMeta = LINE_STATUS[line.atrzApprStts] || {
                                                    label: line.atrzApprStts || '대기',
                                                    badge: 'badge-gray',
                                                };

                                                return (
                                                    <tr key={line.atrzLineSqn}>
                                                        <td>{index + 1}</td>
                                                        <td>
                                                            <div className="approval-history-user">
                                                                <strong>{line.atrzApprUserNm || line.atrzApprUserId}</strong>
                                                                <span>{[line.deptNm, line.jbgdNm].filter(Boolean).join(' · ') || '소속 정보 없음'}</span>
                                                            </div>
                                                        </td>
                                                        <td><span className={`badge ${lineStatusMeta.badge}`}>{lineStatusMeta.label}</span></td>
                                                        <td>{formatDateTime(line.prcsDt)}</td>
                                                        <td>{line.atrzOpnn}</td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </div>

                    <aside className="approval-side-rail">
                        <div className="form-section">
                            <div className="form-section-title">결재선</div>
                            <div className="approval-line-list">
                                {detailApprovalLines.map((line, index) => {
                                    const statusMeta = LINE_STATUS[line.atrzApprStts] || { label: line.atrzApprStts || '대기', badge: 'badge-gray' };
                                    const isCurrent = detail.lineSqn === line.atrzLineSqn;
                                    return (
                                        <div key={line.atrzLineSqn} className={`approval-line-item ${isCurrent ? 'current' : ''}`}>
                                            <div>
                                                <strong>{index + 1}. {line.atrzApprUserNm || line.atrzApprUserId}</strong>
                                                <span>{[line.deptNm, line.jbgdNm].filter(Boolean).join(' · ')}</span>
                                            </div>
                                            <span className={`badge ${statusMeta.badge}`}>{statusMeta.label}</span>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>

                        <div className="form-section">
                            <div className="form-section-title">참조자</div>
                            {(detail.receivers || []).length === 0 ? (
                                <div className="approval-empty compact">등록된 참조자가 없습니다.</div>
                            ) : (
                                <div className="approval-chip-list">
                                    {detail.receivers.map((receiver) => (
                                        <div key={receiver.atrzRcvrId} className="approval-chip">
                                            <strong>{receiver.userNm || receiver.atrzRcvrId}</strong>
                                            <span>{[receiver.deptNm, receiver.jbgdNm].filter(Boolean).join(' · ')}</span>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>

                        <div className="form-section">
                            <div className="form-section-title">첨부파일</div>
                            {(detail.attachments || []).length === 0 ? (
                                <div className="approval-empty compact">첨부파일이 없습니다.</div>
                            ) : (
                                <div className="approval-file-list">
                                    {detail.attachments.map((file) => (
                                        <a
                                            key={`${file.fileId}-${file.fileSeq}`}
                                            className="approval-file-link"
                                            href={file.filePath}
                                            target="_blank"
                                            rel="noreferrer"
                                        >
                                            {fileLabel(file)}
                                        </a>
                                    ))}
                                </div>
                            )}
                        </div>
                    </aside>
                </div>
            </div>
        );
    }

    return (
        <div className="approval-shell">
            <aside className="approval-nav-card">
                <button type="button" className="btn btn-primary approval-create-btn" onClick={() => openCreate()}>
                    문서 작성하기
                </button>
                <div className="approval-nav-list">
                    {SECTIONS.map((item) => (
                        <button
                            key={item.key}
                            type="button"
                            className={`approval-nav-item ${section === item.key ? 'active' : ''}`}
                            onClick={() => openSection(item.key)}
                        >
                            <span>{item.label}</span>
                            <strong>{renderSectionCount(item.countKey)}</strong>
                        </button>
                    ))}
                </div>
            </aside>

            <section className="approval-main">
                <div className="page-header">
                    <div>
                        <h2>전자결재</h2>
                        <p>
                            {mode === 'create' || mode === 'temp'
                                ? '문서를 작성하고 결재선을 구성해 실제 결재 흐름을 시작할 수 있습니다.'
                                : `${SECTIONS.find((item) => item.key === section)?.label || '전자결재'} 영역입니다. 현재 ${sectionCount}건이 집계되어 있습니다.`}
                        </p>
                    </div>
                </div>

                {feedback && (
                    <div className="alert alert-success" role="status">
                        {feedback}
                    </div>
                )}
                {errorMessage && (
                    <div className="alert alert-error" role="alert">
                        {errorMessage}
                    </div>
                )}

                {loadingBootstrap ? (
                    <div className="approval-card">
                        <div className="approval-empty">
                            <strong>전자결재 화면을 준비하는 중입니다.</strong>
                        </div>
                    </div>
                ) : mode === 'create' || mode === 'temp' ? (
                    renderComposeForm()
                ) : (
                    <div className={`approval-list-detail ${mode === 'detail' ? 'is-detail-open' : 'is-list-focus'}`}>
                        {renderListTable()}
                        {mode === 'detail' && renderDetailPanel()}
                    </div>
                )}
            </section>
        </div>
    );
}
