BEGIN;

DELETE FROM CONTRACT_AUDIT_LOG
WHERE CONTRACT_ID IN (
    SELECT CONTRACT_ID FROM CONTRACT_DOCUMENT WHERE CONTRACT_REF IN ('CTRDOCDEMO001', 'CTRDOCDEMO002', 'CTRDOCDEMO003')
);

DELETE FROM CONTRACT_FIELD_VALUE
WHERE CONTRACT_ID IN (
    SELECT CONTRACT_ID FROM CONTRACT_DOCUMENT WHERE CONTRACT_REF IN ('CTRDOCDEMO001', 'CTRDOCDEMO002', 'CTRDOCDEMO003')
);

DELETE FROM CONTRACT_INVITATION
WHERE CONTRACT_ID IN (
    SELECT CONTRACT_ID FROM CONTRACT_DOCUMENT WHERE CONTRACT_REF IN ('CTRDOCDEMO001', 'CTRDOCDEMO002', 'CTRDOCDEMO003')
);

DELETE FROM CONTRACT_SIGNER
WHERE CONTRACT_ID IN (
    SELECT CONTRACT_ID FROM CONTRACT_DOCUMENT WHERE CONTRACT_REF IN ('CTRDOCDEMO001', 'CTRDOCDEMO002', 'CTRDOCDEMO003')
);

DELETE FROM CONTRACT_DOCUMENT WHERE CONTRACT_REF IN ('CTRDOCDEMO001', 'CTRDOCDEMO002', 'CTRDOCDEMO003');
DELETE FROM CONTRACT_BATCH WHERE BATCH_REF = 'CTRBATCH001';
DELETE FROM CONTRACT_TEMPLATE_VERSION
WHERE TEMPLATE_ID IN (
    SELECT TEMPLATE_ID FROM CONTRACT_TEMPLATE WHERE TEMPLATE_CODE IN ('EMPLOYMENT_OFFER', 'NDA_SIMPLE', 'BULK_CONSENT')
);
DELETE FROM CONTRACT_TEMPLATE WHERE TEMPLATE_CODE IN ('EMPLOYMENT_OFFER', 'NDA_SIMPLE', 'BULK_CONSENT');
DELETE FROM CONTRACT_TEMPLATE_REQUEST WHERE REQUEST_REF IN ('CTRREQDEMO001', 'CTRREQDEMO002');

UPDATE CONTRACT_COMPANY_SETTING
SET COMPANY_NM = COALESCE(COMPANY_NM, U&'\BAA8\B450\C758\0020\B7EC\B2DD'),
    SENDER_NM = COALESCE(SENDER_NM, U&'\AD00\B9AC\C790'),
    SENDER_EMAIL = COALESCE(SENDER_EMAIL, 'admin@modulearning.local'),
    SENDER_TELNO = COALESCE(SENDER_TELNO, '010-0000-0000'),
    PROVIDER_NM = COALESCE(PROVIDER_NM, 'internal'),
    GUIDE_ACK_YN = 'Y',
    LAST_CHG_DT = CURRENT_TIMESTAMP
WHERE SETTING_ID = (SELECT MIN(SETTING_ID) FROM CONTRACT_COMPANY_SETTING);

INSERT INTO CONTRACT_TEMPLATE_REQUEST (
    REQUEST_REF, REQUEST_TITLE, REQUEST_STATUS_CD, REQUEST_NOTE, REQUESTER_USER_ID,
    REVIEW_USER_ID, REVIEW_NOTE, CRT_DT, REVIEW_DT, LAST_CHG_DT
)
VALUES
('CTRREQDEMO001', '근로계약서 양식 신청', 'approved', '근로계약 기본 양식 등록 요청', 'admin', 'admin', '승인 후 디자이너 초안으로 전환되었습니다.', CURRENT_TIMESTAMP - INTERVAL '9 days', CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days'),
('CTRREQDEMO002', '개인정보 처리 동의서 양식 신청', 'reviewing', '대량 전송용 동의서 검토 요청', 'user01', NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '2 days', NULL, CURRENT_TIMESTAMP - INTERVAL '2 days');

INSERT INTO CONTRACT_TEMPLATE (
    TEMPLATE_CODE, TEMPLATE_NM, TEMPLATE_DESC, TEMPLATE_CATEGORY_CD, ACTIVE_YN, REQUEST_ID, CRT_USER_ID, CRT_DT, LAST_CHG_DT
)
SELECT
    code, name, description, category, 'Y',
    (
        SELECT r.REQUEST_ID
        FROM CONTRACT_TEMPLATE_REQUEST r
        WHERE r.REQUEST_REF = template_data.request_ref
    ),
    creator, created_dt, created_dt
FROM (
    VALUES
        ('EMPLOYMENT_OFFER', '근로계약서', '채용 및 입사 안내용 근로계약서', 'hr', 'CTRREQDEMO001', 'admin', CURRENT_TIMESTAMP - INTERVAL '8 days'),
        ('NDA_SIMPLE', '비밀유지계약서', '외부 파트너와 체결하는 비밀유지 계약', 'legal', NULL, 'admin', CURRENT_TIMESTAMP - INTERVAL '6 days'),
        ('BULK_CONSENT', '개인정보 수집 동의서', '대량 전송용 동의 양식', 'ops', NULL, 'admin', CURRENT_TIMESTAMP - INTERVAL '4 days')
) AS template_data(code, name, description, category, request_ref, creator, created_dt);

WITH template_ids AS (
    SELECT TEMPLATE_ID, TEMPLATE_CODE
    FROM CONTRACT_TEMPLATE
    WHERE TEMPLATE_CODE IN ('EMPLOYMENT_OFFER', 'NDA_SIMPLE', 'BULK_CONSENT')
)
INSERT INTO CONTRACT_TEMPLATE_VERSION (
    TEMPLATE_ID, VERSION_NO, VERSION_LABEL, VERSION_STATUS_CD, VERSION_NOTE,
    SCHEMA_JSON, LAYOUT_JSON, CRT_USER_ID, CRT_DT, PUBLISHED_DT, LAST_CHG_DT
)
SELECT
    t.TEMPLATE_ID,
    1,
    'v1.0',
    'published',
    '데모 게시 버전',
    CASE t.TEMPLATE_CODE
        WHEN 'EMPLOYMENT_OFFER' THEN $employment_schema$
{"pages":[{"pageId":"page-1","label":"1페이지"}],"fields":[{"fieldId":"employeeName","fieldKey":"employeeName","label":"계약 대상자 이름","fieldTypeCd":"text","pageId":"page-1","x":72,"y":170,"width":280,"height":44,"requiredYn":"Y","assignmentCd":"creator"},{"fieldId":"position","fieldKey":"position","label":"직무","fieldTypeCd":"text","pageId":"page-1","x":372,"y":170,"width":180,"height":44,"requiredYn":"Y","assignmentCd":"creator"},{"fieldId":"startDate","fieldKey":"startDate","label":"입사일","fieldTypeCd":"date","pageId":"page-1","x":72,"y":234,"width":180,"height":44,"requiredYn":"Y","assignmentCd":"creator"},{"fieldId":"salary","fieldKey":"salary","label":"연봉","fieldTypeCd":"number","pageId":"page-1","x":272,"y":234,"width":180,"height":44,"requiredYn":"Y","assignmentCd":"creator"},{"fieldId":"companyName","fieldKey":"companyName","label":"회사명","fieldTypeCd":"auto","pageId":"page-1","x":72,"y":312,"width":220,"height":44,"autoSourceCd":"companyName","requiredYn":"N","assignmentCd":"system"},{"fieldId":"companySeal","fieldKey":"companySeal","label":"회사 직인","fieldTypeCd":"stamp","pageId":"page-1","x":320,"y":312,"width":150,"height":88,"requiredYn":"N","assignmentCd":"system"},{"fieldId":"employeeSign","fieldKey":"employeeSign","label":"계약 대상자 서명","fieldTypeCd":"signature","pageId":"page-1","x":72,"y":470,"width":260,"height":110,"requiredYn":"Y","assignmentCd":"signer","signerOrder":1},{"fieldId":"agreement","fieldKey":"agreement","label":"계약 내용을 확인했습니다","fieldTypeCd":"confirm","pageId":"page-1","x":72,"y":604,"width":420,"height":40,"requiredYn":"Y","assignmentCd":"signer","signerOrder":1}]}
$employment_schema$
        WHEN 'NDA_SIMPLE' THEN $nda_schema$
{"pages":[{"pageId":"page-1","label":"1페이지"}],"fields":[{"fieldId":"partnerName","fieldKey":"partnerName","label":"상대 회사명","fieldTypeCd":"text","pageId":"page-1","x":72,"y":164,"width":280,"height":44,"requiredYn":"Y","assignmentCd":"creator"},{"fieldId":"projectName","fieldKey":"projectName","label":"프로젝트명","fieldTypeCd":"text","pageId":"page-1","x":370,"y":164,"width":260,"height":44,"requiredYn":"Y","assignmentCd":"creator"},{"fieldId":"effectiveDate","fieldKey":"effectiveDate","label":"효력 발생일","fieldTypeCd":"date","pageId":"page-1","x":72,"y":224,"width":200,"height":44,"requiredYn":"Y","assignmentCd":"creator"},{"fieldId":"disclosurePeriod","fieldKey":"disclosurePeriod","label":"비밀유지 기간","fieldTypeCd":"select","pageId":"page-1","x":288,"y":224,"width":180,"height":44,"requiredYn":"Y","assignmentCd":"creator","options":[{"value":"12m","label":"12개월"},{"value":"24m","label":"24개월"},{"value":"36m","label":"36개월"}]},{"fieldId":"companySeal","fieldKey":"companySeal","label":"회사 직인","fieldTypeCd":"stamp","pageId":"page-1","x":72,"y":300,"width":160,"height":88,"assignmentCd":"system"},{"fieldId":"partnerSignerName","fieldKey":"partnerSignerName","label":"상대 담당자 이름","fieldTypeCd":"text","pageId":"page-1","x":72,"y":438,"width":240,"height":44,"requiredYn":"Y","assignmentCd":"signer","signerOrder":2},{"fieldId":"partnerInitial","fieldKey":"partnerInitial","label":"이니셜","fieldTypeCd":"initial","pageId":"page-1","x":330,"y":438,"width":110,"height":72,"requiredYn":"Y","assignmentCd":"signer","signerOrder":2},{"fieldId":"partnerSign","fieldKey":"partnerSign","label":"상대 서명","fieldTypeCd":"signature","pageId":"page-1","x":72,"y":528,"width":260,"height":110,"requiredYn":"Y","assignmentCd":"signer","signerOrder":2},{"fieldId":"internalSign","fieldKey":"internalSign","label":"사내 승인 서명","fieldTypeCd":"signature","pageId":"page-1","x":360,"y":528,"width":240,"height":110,"requiredYn":"Y","assignmentCd":"signer","signerOrder":1}]}
$nda_schema$
        ELSE $bulk_schema$
{"pages":[{"pageId":"page-1","label":"1페이지"}],"fields":[{"fieldId":"campaignName","fieldKey":"campaignName","label":"캠페인명","fieldTypeCd":"text","pageId":"page-1","x":72,"y":160,"width":280,"height":44,"requiredYn":"Y","assignmentCd":"creator"},{"fieldId":"consentScope","fieldKey":"consentScope","label":"수집 항목","fieldTypeCd":"textarea","pageId":"page-1","x":72,"y":224,"width":420,"height":110,"requiredYn":"Y","assignmentCd":"creator"},{"fieldId":"recipientName","fieldKey":"recipientName","label":"이름","fieldTypeCd":"text","pageId":"page-1","x":72,"y":384,"width":200,"height":44,"requiredYn":"Y","assignmentCd":"signer","signerOrder":1},{"fieldId":"recipientEmail","fieldKey":"recipientEmail","label":"이메일","fieldTypeCd":"text","pageId":"page-1","x":288,"y":384,"width":260,"height":44,"requiredYn":"Y","assignmentCd":"signer","signerOrder":1},{"fieldId":"agreeMarketing","fieldKey":"agreeMarketing","label":"마케팅 수신 동의","fieldTypeCd":"checkbox","pageId":"page-1","x":72,"y":450,"width":280,"height":40,"requiredYn":"N","assignmentCd":"signer","signerOrder":1},{"fieldId":"finalConfirm","fieldKey":"finalConfirm","label":"동의 내용을 확인했습니다","fieldTypeCd":"confirm","pageId":"page-1","x":72,"y":500,"width":360,"height":40,"requiredYn":"Y","assignmentCd":"signer","signerOrder":1},{"fieldId":"recipientSignature","fieldKey":"recipientSignature","label":"서명","fieldTypeCd":"signature","pageId":"page-1","x":72,"y":566,"width":260,"height":110,"requiredYn":"Y","assignmentCd":"signer","signerOrder":1}]}
$bulk_schema$
    END,
    '{"canvas":{"width":820,"height":1160,"backgroundColor":"#fffdf7","padding":48}}',
    'admin',
    CURRENT_TIMESTAMP - INTERVAL '7 days',
    CURRENT_TIMESTAMP - INTERVAL '6 days',
    CURRENT_TIMESTAMP - INTERVAL '6 days'
FROM template_ids t;

UPDATE CONTRACT_TEMPLATE t
SET PUBLISHED_VERSION_ID = v.TEMPLATE_VERSION_ID,
    LAST_CHG_DT = CURRENT_TIMESTAMP
FROM CONTRACT_TEMPLATE_VERSION v
WHERE t.TEMPLATE_ID = v.TEMPLATE_ID
  AND v.VERSION_NO = 1
  AND t.TEMPLATE_CODE IN ('EMPLOYMENT_OFFER', 'NDA_SIMPLE', 'BULK_CONSENT');

INSERT INTO CONTRACT_BATCH (
    BATCH_REF, TEMPLATE_ID, TEMPLATE_VERSION_ID, BATCH_TITLE, CREATOR_USER_ID,
    TOTAL_COUNT, SUCCESS_COUNT, FAILED_COUNT, RESULT_JSON, CRT_DT, LAST_CHG_DT
)
SELECT
    'CTRBATCH001',
    t.TEMPLATE_ID,
    v.TEMPLATE_VERSION_ID,
    '개인정보 수집 동의서 3건 대량 전송',
    'admin',
    3,
    3,
    0,
    '{"createdContracts":["CTRDOCDEMO003","CTRDOCBULK002","CTRDOCBULK003"]}',
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM CONTRACT_TEMPLATE t
JOIN CONTRACT_TEMPLATE_VERSION v
  ON v.TEMPLATE_ID = t.TEMPLATE_ID
 AND v.VERSION_NO = 1
WHERE t.TEMPLATE_CODE = 'BULK_CONSENT';

WITH employment AS (
    SELECT t.TEMPLATE_ID, v.TEMPLATE_VERSION_ID, v.SCHEMA_JSON, v.LAYOUT_JSON
    FROM CONTRACT_TEMPLATE t
    JOIN CONTRACT_TEMPLATE_VERSION v
      ON v.TEMPLATE_ID = t.TEMPLATE_ID
     AND v.VERSION_NO = 1
    WHERE t.TEMPLATE_CODE = 'EMPLOYMENT_OFFER'
), nda AS (
    SELECT t.TEMPLATE_ID, v.TEMPLATE_VERSION_ID, v.SCHEMA_JSON, v.LAYOUT_JSON
    FROM CONTRACT_TEMPLATE t
    JOIN CONTRACT_TEMPLATE_VERSION v
      ON v.TEMPLATE_ID = t.TEMPLATE_ID
     AND v.VERSION_NO = 1
    WHERE t.TEMPLATE_CODE = 'NDA_SIMPLE'
), bulk AS (
    SELECT t.TEMPLATE_ID, v.TEMPLATE_VERSION_ID, v.SCHEMA_JSON, v.LAYOUT_JSON
    FROM CONTRACT_TEMPLATE t
    JOIN CONTRACT_TEMPLATE_VERSION v
      ON v.TEMPLATE_ID = t.TEMPLATE_ID
     AND v.VERSION_NO = 1
    WHERE t.TEMPLATE_CODE = 'BULK_CONSENT'
), batch_ref AS (
    SELECT BATCH_ID FROM CONTRACT_BATCH WHERE BATCH_REF = 'CTRBATCH001'
)
INSERT INTO CONTRACT_DOCUMENT (
    CONTRACT_REF, TEMPLATE_ID, TEMPLATE_VERSION_ID, BATCH_ID, CONTRACT_TITLE, CONTRACT_MESSAGE,
    SEND_TYPE_CD, SIGNING_FLOW_CD, STATUS_CD, CREATOR_USER_ID, SENDER_NM, SENDER_EMAIL, SENDER_TELNO,
    COMPANY_SNAPSHOT_JSON, TEMPLATE_SCHEMA_JSON, TEMPLATE_LAYOUT_JSON, FINAL_HTML_DATA, EXPIRES_DT,
    SENT_DT, COMPLETED_DT, CANCELLED_DT, CANCELLED_REASON, TOTAL_SIGNER_COUNT, CURRENT_SIGN_ORDER, CRT_DT, LAST_CHG_DT
)
SELECT
    'CTRDOCDEMO001',
    e.TEMPLATE_ID,
    e.TEMPLATE_VERSION_ID,
    CAST(NULL AS BIGINT),
    '2026 상반기 개발직 채용 근로계약',
    '비대면 전송으로 진행하는 근로계약입니다.',
    'remote',
    'parallel',
    'completed',
    'admin',
    '관리자',
    'admin@modulearning.local',
    '010-0000-0000',
    U&'{"companyName":"\BAA8\B450\C758\0020\B7EC\B2DD","senderName":"\AD00\B9AC\C790"}',
    e.SCHEMA_JSON,
    e.LAYOUT_JSON,
    '<article><h1>2026 상반기 개발직 채용 근로계약</h1><p>모든 서명이 완료되었습니다.</p></article>',
    CURRENT_TIMESTAMP + INTERVAL '10 days',
    CURRENT_TIMESTAMP - INTERVAL '5 days',
    CURRENT_TIMESTAMP - INTERVAL '3 days',
    CAST(NULL AS TIMESTAMP),
    NULL,
    1,
    1,
    CURRENT_TIMESTAMP - INTERVAL '6 days',
    CURRENT_TIMESTAMP - INTERVAL '3 days'
FROM employment e
UNION ALL
SELECT
    'CTRDOCDEMO002',
    n.TEMPLATE_ID,
    n.TEMPLATE_VERSION_ID,
    CAST(NULL AS BIGINT),
    '에이전시 비밀유지계약',
    '링크 계약으로 진행하며 순차 서명이 필요한 문서입니다.',
    'link',
    'sequential',
    'in_progress',
    'admin',
    '관리자',
    'admin@modulearning.local',
    '010-0000-0000',
    U&'{"companyName":"\BAA8\B450\C758\0020\B7EC\B2DD","senderName":"\AD00\B9AC\C790"}',
    n.SCHEMA_JSON,
    n.LAYOUT_JSON,
    NULL,
    CURRENT_TIMESTAMP + INTERVAL '6 days',
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    NULL,
    CAST(NULL AS TIMESTAMP),
    NULL,
    2,
    2,
    CURRENT_TIMESTAMP - INTERVAL '2 days',
    CURRENT_TIMESTAMP - INTERVAL '12 hours'
FROM nda n
UNION ALL
SELECT
    'CTRDOCDEMO003',
    b.TEMPLATE_ID,
    b.TEMPLATE_VERSION_ID,
    (SELECT BATCH_ID FROM batch_ref),
    '개인정보 수집 동의서 - 3월 캠페인',
    '대량 전송 예시 문서입니다.',
    'bulk',
    'parallel',
    'sent',
    'admin',
    '관리자',
    'admin@modulearning.local',
    '010-0000-0000',
    U&'{"companyName":"\BAA8\B450\C758\0020\B7EC\B2DD","senderName":"\AD00\B9AC\C790"}',
    b.SCHEMA_JSON,
    b.LAYOUT_JSON,
    NULL,
    CURRENT_TIMESTAMP + INTERVAL '5 days',
    CURRENT_TIMESTAMP - INTERVAL '20 hours',
    NULL,
    CAST(NULL AS TIMESTAMP),
    NULL,
    1,
    1,
    CURRENT_TIMESTAMP - INTERVAL '20 hours',
    CURRENT_TIMESTAMP - INTERVAL '20 hours'
FROM bulk b;

WITH docs AS (
    SELECT CONTRACT_ID, CONTRACT_REF
    FROM CONTRACT_DOCUMENT
    WHERE CONTRACT_REF IN ('CTRDOCDEMO001', 'CTRDOCDEMO002', 'CTRDOCDEMO003')
)
INSERT INTO CONTRACT_SIGNER (
    CONTRACT_ID, SIGNER_ORDER, SIGNER_ROLE_CD, SIGNER_TYPE_CD, INTERNAL_USER_ID,
    SIGNER_NM, SIGNER_EMAIL, SIGNER_TELNO, STATUS_CD, OPENED_DT, SIGNED_DT,
    DECLINED_REASON, SIGNATURE_DATA, INITIAL_DATA, CLAIMED_NM, CLAIMED_EMAIL, CLAIMED_TELNO, LAST_CHG_DT
)
SELECT d.CONTRACT_ID, 1, 'signer', 'external', NULL, '김지원', 'candidate@demo.local', '010-1111-0001', 'signed',
       CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '3 days',
       NULL, 'data:image/png;base64,complete-signature', NULL, '김지원', 'candidate@demo.local', '010-1111-0001', CURRENT_TIMESTAMP - INTERVAL '3 days'
FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO001'
UNION ALL
SELECT d.CONTRACT_ID, 1, 'signer', 'internal', 'admin', '관리자', 'admin@modulearning.local', '010-0000-0000', 'signed',
       CURRENT_TIMESTAMP - INTERVAL '20 hours', CURRENT_TIMESTAMP - INTERVAL '18 hours',
       NULL, 'data:image/png;base64,admin-signature', 'data:image/png;base64,admin-initial', '관리자', 'admin@modulearning.local', '010-0000-0000', CURRENT_TIMESTAMP - INTERVAL '18 hours'
FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO002'
UNION ALL
SELECT d.CONTRACT_ID, 2, 'signer', 'external', NULL, '이수민', 'partner@demo.local', '010-3333-0002', 'opened',
       CURRENT_TIMESTAMP - INTERVAL '4 hours', NULL,
       NULL, NULL, NULL, '이수민', 'partner@demo.local', '010-3333-0002', CURRENT_TIMESTAMP - INTERVAL '4 hours'
FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO002'
UNION ALL
SELECT d.CONTRACT_ID, 1, 'signer', 'external', NULL, '박서윤', 'bulk1@demo.local', '010-2222-0003', 'pending',
       NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '20 hours'
FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO003';

INSERT INTO CONTRACT_INVITATION (
    CONTRACT_ID, SIGNER_ID, TOKEN_HASH, TOKEN_HINT, CHANNEL_CD, DELIVERY_STATUS_CD, DELIVERY_NOTE,
    ACTIVE_YN, CRT_DT, SENT_DT, OPENED_DT, COMPLETED_DT, EXPIRES_DT, LAST_CHG_DT
)
SELECT
    s.CONTRACT_ID,
    s.SIGNER_ID,
    'demohash-' || s.SIGNER_ID,
    RIGHT('0000' || s.SIGNER_ID::text, 4),
    CASE d.SEND_TYPE_CD WHEN 'link' THEN 'link' WHEN 'bulk' THEN 'bulk' ELSE 'remote' END,
    CASE WHEN s.STATUS_CD = 'pending' THEN 'sent' ELSE 'opened' END,
    CASE d.SEND_TYPE_CD WHEN 'remote' THEN '비대면 전송' WHEN 'link' THEN '링크 생성' ELSE '대량 전송' END,
    'Y',
    d.CRT_DT,
    d.SENT_DT,
    s.OPENED_DT,
    s.SIGNED_DT,
    d.EXPIRES_DT,
    COALESCE(s.SIGNED_DT, s.OPENED_DT, d.SENT_DT, d.CRT_DT)
FROM CONTRACT_SIGNER s
JOIN CONTRACT_DOCUMENT d
  ON d.CONTRACT_ID = s.CONTRACT_ID
WHERE d.CONTRACT_REF IN ('CTRDOCDEMO001', 'CTRDOCDEMO002', 'CTRDOCDEMO003');

WITH docs AS (
    SELECT CONTRACT_ID, CONTRACT_REF FROM CONTRACT_DOCUMENT WHERE CONTRACT_REF IN ('CTRDOCDEMO001', 'CTRDOCDEMO002', 'CTRDOCDEMO003')
), signers AS (
    SELECT SIGNER_ID, CONTRACT_ID, SIGNER_ORDER FROM CONTRACT_SIGNER
)
INSERT INTO CONTRACT_FIELD_VALUE (
    CONTRACT_ID, FIELD_KEY, REPEAT_GROUP_KEY, ROW_INDEX, FIELD_VALUE, SIGNER_ID, LAST_CHG_DT
)
SELECT d.CONTRACT_ID, 'employeeName', NULL, 0, '김지원', CAST(NULL AS BIGINT), CURRENT_TIMESTAMP - INTERVAL '6 days' FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO001'
UNION ALL
SELECT d.CONTRACT_ID, 'position', NULL, 0, '백엔드 엔지니어', CAST(NULL AS BIGINT), CURRENT_TIMESTAMP - INTERVAL '6 days' FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO001'
UNION ALL
SELECT d.CONTRACT_ID, 'startDate', NULL, 0, CURRENT_DATE::text, CAST(NULL AS BIGINT), CURRENT_TIMESTAMP - INTERVAL '6 days' FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO001'
UNION ALL
SELECT d.CONTRACT_ID, 'salary', NULL, 0, '48000000', CAST(NULL AS BIGINT), CURRENT_TIMESTAMP - INTERVAL '6 days' FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO001'
UNION ALL
SELECT d.CONTRACT_ID, 'companyName', NULL, 0, U&'\BAA8\B450\C758\0020\B7EC\B2DD', CAST(NULL AS BIGINT), CURRENT_TIMESTAMP - INTERVAL '6 days' FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO001'
UNION ALL
SELECT d.CONTRACT_ID, 'agreement', NULL, 0, 'Y', (SELECT s.SIGNER_ID FROM signers s WHERE s.CONTRACT_ID = d.CONTRACT_ID AND s.SIGNER_ORDER = 1), CURRENT_TIMESTAMP - INTERVAL '3 days' FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO001'
UNION ALL
SELECT d.CONTRACT_ID, 'projectName', NULL, 0, '신규 파트너 검토', CAST(NULL AS BIGINT), CURRENT_TIMESTAMP - INTERVAL '2 days' FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO002'
UNION ALL
SELECT d.CONTRACT_ID, 'partnerName', NULL, 0, '오션에이전시', CAST(NULL AS BIGINT), CURRENT_TIMESTAMP - INTERVAL '2 days' FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO002'
UNION ALL
SELECT d.CONTRACT_ID, 'effectiveDate', NULL, 0, CURRENT_DATE::text, CAST(NULL AS BIGINT), CURRENT_TIMESTAMP - INTERVAL '2 days' FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO002'
UNION ALL
SELECT d.CONTRACT_ID, 'disclosurePeriod', NULL, 0, '24m', CAST(NULL AS BIGINT), CURRENT_TIMESTAMP - INTERVAL '2 days' FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO002'
UNION ALL
SELECT d.CONTRACT_ID, 'campaignName', NULL, 0, '3월 신규 서비스 안내', CAST(NULL AS BIGINT), CURRENT_TIMESTAMP - INTERVAL '20 hours' FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO003'
UNION ALL
SELECT d.CONTRACT_ID, 'consentScope', NULL, 0, '이름, 이메일, 연락처, 서비스 사용 로그', CAST(NULL AS BIGINT), CURRENT_TIMESTAMP - INTERVAL '20 hours' FROM docs d WHERE d.CONTRACT_REF = 'CTRDOCDEMO003';

INSERT INTO CONTRACT_AUDIT_LOG (
    CONTRACT_ID, SIGNER_ID, ACTOR_USER_ID, ACTOR_NM, ACTOR_TYPE_CD, ACTION_CD, ACTION_MESSAGE, PAYLOAD_JSON, CRT_DT
)
SELECT d.CONTRACT_ID, CAST(NULL AS BIGINT), 'admin', '관리자', 'internal', 'created', '계약이 생성되었습니다.', '{"sendType":"remote"}', d.CRT_DT
FROM CONTRACT_DOCUMENT d
WHERE d.CONTRACT_REF = 'CTRDOCDEMO001'
UNION ALL
SELECT d.CONTRACT_ID, CAST(NULL AS BIGINT), 'admin', '관리자', 'internal', 'sent', '비대면 계약 링크를 발급했습니다.', '{"channel":"remote"}', d.SENT_DT
FROM CONTRACT_DOCUMENT d
WHERE d.CONTRACT_REF = 'CTRDOCDEMO001'
UNION ALL
SELECT d.CONTRACT_ID, s.SIGNER_ID, NULL, '김지원', 'signer', 'signed', '계약 대상자가 서명했습니다.', '{"result":"completed"}', s.SIGNED_DT
FROM CONTRACT_DOCUMENT d
JOIN CONTRACT_SIGNER s ON s.CONTRACT_ID = d.CONTRACT_ID AND s.SIGNER_ORDER = 1
WHERE d.CONTRACT_REF = 'CTRDOCDEMO001'
UNION ALL
SELECT d.CONTRACT_ID, CAST(NULL AS BIGINT), 'admin', '관리자', 'internal', 'created', '계약이 생성되었습니다.', '{"sendType":"link"}', d.CRT_DT
FROM CONTRACT_DOCUMENT d
WHERE d.CONTRACT_REF = 'CTRDOCDEMO002'
UNION ALL
SELECT d.CONTRACT_ID, CAST(NULL AS BIGINT), 'admin', '관리자', 'internal', 'sent', '링크 계약을 시작했습니다.', '{"channel":"link","flow":"sequential"}', d.SENT_DT
FROM CONTRACT_DOCUMENT d
WHERE d.CONTRACT_REF = 'CTRDOCDEMO002'
UNION ALL
SELECT d.CONTRACT_ID, s.SIGNER_ID, 'admin', '관리자', 'internal', 'signed', '사내 서명 단계가 완료되었습니다.', '{"order":1}', s.SIGNED_DT
FROM CONTRACT_DOCUMENT d
JOIN CONTRACT_SIGNER s ON s.CONTRACT_ID = d.CONTRACT_ID AND s.SIGNER_ORDER = 1
WHERE d.CONTRACT_REF = 'CTRDOCDEMO002'
UNION ALL
SELECT d.CONTRACT_ID, s.SIGNER_ID, NULL, '이수민', 'signer', 'opened', '외부 서명자가 링크를 열람했습니다.', '{"order":2}', s.OPENED_DT
FROM CONTRACT_DOCUMENT d
JOIN CONTRACT_SIGNER s ON s.CONTRACT_ID = d.CONTRACT_ID AND s.SIGNER_ORDER = 2
WHERE d.CONTRACT_REF = 'CTRDOCDEMO002'
UNION ALL
SELECT d.CONTRACT_ID, CAST(NULL AS BIGINT), 'admin', '관리자', 'internal', 'batch_created', '대량 전송 계약이 생성되었습니다.', '{"batchRef":"CTRBATCH001"}', d.CRT_DT
FROM CONTRACT_DOCUMENT d
WHERE d.CONTRACT_REF = 'CTRDOCDEMO003';

COMMIT;
