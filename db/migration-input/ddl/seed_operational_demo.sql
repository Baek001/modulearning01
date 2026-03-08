-- Operational demo seed for approval, meeting, and attendance screens.
BEGIN;

INSERT INTO USERS (
  USER_ID, USER_PSWD, USER_NM, USER_EMAIL, USER_TELNO, EXT_TEL,
  RSGNTN_YN, RSGNTN_YMD, DEPT_ID, JBGD_CD, USER_ROLE, HIRE_YMD, WORK_STTS_CD
)
SELECT
  'user02', '{noop}user2234', 'Test User 2', 'user02@modulearning.local', '010-0000-0002', '1003',
  'N', NULL, 'DP001002', 'J001', 'ROLE_USER', CURRENT_TIMESTAMP, 'C103'
WHERE NOT EXISTS (
  SELECT 1
  FROM USERS
  WHERE USER_ID = 'user02'
);

DELETE FROM AUTHORIZATION_DOCUMENT_RECEIVER
WHERE ATRZ_DOC_ID IN ('ATRZDEMO001', 'ATRZDEMO002', 'ATRZDEMO003', 'ATRZDEMO004', 'ATRZDEMO005', 'ATRZDEMO006', 'ATRZDEMO007');

DELETE FROM AUTHORIZATION_LINE
WHERE ATRZ_DOC_ID IN ('ATRZDEMO001', 'ATRZDEMO002', 'ATRZDEMO003', 'ATRZDEMO004', 'ATRZDEMO005', 'ATRZDEMO006', 'ATRZDEMO007');

DELETE FROM VACATION
WHERE VACT_SQN = 'DEMO-VACT-001'
   OR ATRZ_DOC_ID = 'ATRZDEMO002';

DELETE FROM AUTHORIZATION_DOCUMENT
WHERE ATRZ_DOC_ID IN ('ATRZDEMO001', 'ATRZDEMO002', 'ATRZDEMO003', 'ATRZDEMO004', 'ATRZDEMO005', 'ATRZDEMO006', 'ATRZDEMO007');

DELETE FROM AUTHORIZATION_TEMP
WHERE ATRZ_TEMP_SQN = 'DEMO-TEMP-001';

DELETE FROM CUSTOM_LINE_BOOKMARK
WHERE USER_ID = 'admin'
  AND CSTM_LINE_BM_NM = '기본 승인선';

DELETE FROM MEETING_RESERVATION
WHERE RESERVATION_ID IN (9001, 9002, 9003, 9004);

DELETE FROM MEETING_ROOM
WHERE ROOM_ID IN ('ROOM01', 'ROOM02', 'ROOM03');

DELETE FROM ATTENDANCE_DEPART_STATUS
WHERE USER_ID IN ('admin', 'user01', 'user02')
  AND WORK_YMD::date = CURRENT_DATE;

DELETE FROM USER_MONTHLY_ATTENDANCE
WHERE USER_ID IN ('admin', 'user01', 'user02')
  AND WORK_MONTH = TO_CHAR(CURRENT_DATE, 'YYYY-MM');

DELETE FROM USER_WEEKLY_ATTENDANCE
WHERE USER_ID IN ('admin', 'user01', 'user02');

UPDATE USERS
SET DEPT_ID = CASE USER_ID
        WHEN 'admin' THEN 'DP001000'
        ELSE DEPT_ID
    END,
    WORK_STTS_CD = CASE USER_ID
        WHEN 'admin' THEN 'C101'
        WHEN 'user01' THEN 'C104'
        WHEN 'user02' THEN 'C105'
        ELSE WORK_STTS_CD
    END
WHERE USER_ID IN ('admin', 'user01', 'user02');

WITH template_ids AS (
    SELECT
        MAX(CASE WHEN ATRZ_DOC_CD = 'PRO_PROPOSAL' THEN ATRZ_DOC_TMPL_ID END) AS pro_proposal,
        MAX(CASE WHEN ATRZ_DOC_CD = 'VAC_REQ' THEN ATRZ_DOC_TMPL_ID END) AS vac_req,
        MAX(CASE WHEN ATRZ_DOC_CD = 'EXP_APPROVAL' THEN ATRZ_DOC_TMPL_ID END) AS exp_approval,
        MAX(CASE WHEN ATRZ_DOC_CD = 'IT_WORK_REQ' THEN ATRZ_DOC_TMPL_ID END) AS it_work_req,
        MAX(CASE WHEN ATRZ_DOC_CD = 'PO_REQUEST' THEN ATRZ_DOC_TMPL_ID END) AS po_request,
        MAX(CASE WHEN ATRZ_DOC_CD = 'MKT_REQUEST' THEN ATRZ_DOC_TMPL_ID END) AS mkt_request,
        MAX(CASE WHEN ATRZ_DOC_CD = 'TRIP_REQ' THEN ATRZ_DOC_TMPL_ID END) AS trip_req
    FROM AUTHORIZATION_DOCUMENT_TEMPLATE
),
demo_documents AS (
    SELECT
        'ATRZDEMO001' AS atrz_doc_id,
        pro_proposal AS atrz_doc_tmpl_id,
        '신규 그룹웨어 구축 기안' AS atrz_doc_ttl,
        CURRENT_TIMESTAMP - INTERVAL '4 days' AS atrz_sbmt_dt,
        'admin' AS atrz_user_id,
        'A203' AS crnt_atrz_step_cd,
        TO_CHAR(CURRENT_DATE - INTERVAL '4 days', 'YYYY-MM-DD') AS submitted_label,
        $$<section class="approval-doc-layout"><h2>신규 그룹웨어 구축 기안</h2><p>운영 이관 일정과 데이터 정합성 점검 범위를 확정합니다.</p></section>$$ AS html_data,
        'Y' AS open_yn
    FROM template_ids
    UNION ALL
    SELECT
        'ATRZDEMO002',
        vac_req,
        '상반기 연차 사용 신청',
        CURRENT_TIMESTAMP - INTERVAL '12 days',
        'admin',
        'A206',
        TO_CHAR(CURRENT_DATE - INTERVAL '12 days', 'YYYY-MM-DD'),
        $$<section class="approval-doc-layout"><h2>상반기 연차 사용 신청</h2><p>프로젝트 마감 이후 재충전을 위한 연차 사용 계획입니다.</p></section>$$,
        'Y'
    FROM template_ids
    UNION ALL
    SELECT
        'ATRZDEMO003',
        exp_approval,
        '운영 서버 라이선스 비용 품의',
        CURRENT_TIMESTAMP - INTERVAL '9 days',
        'admin',
        'A204',
        TO_CHAR(CURRENT_DATE - INTERVAL '9 days', 'YYYY-MM-DD'),
        $$<section class="approval-doc-layout"><h2>운영 서버 라이선스 비용 품의</h2><p>증빙 보완이 필요한 지출 건으로 반려된 예시 문서입니다.</p></section>$$,
        'N'
    FROM template_ids
    UNION ALL
    SELECT
        'ATRZDEMO004',
        it_work_req,
        '사내 SSO 개선 작업 요청 초안 회수',
        CURRENT_TIMESTAMP - INTERVAL '6 days',
        'admin',
        'A205',
        TO_CHAR(CURRENT_DATE - INTERVAL '6 days', 'YYYY-MM-DD'),
        $$<section class="approval-doc-layout"><h2>사내 SSO 개선 작업 요청 초안 회수</h2><p>범위 재정의를 위해 기안자가 회수한 문서입니다.</p></section>$$,
        'N'
    FROM template_ids
    UNION ALL
    SELECT
        'ATRZDEMO005',
        po_request,
        '백오피스 장비 교체 구매 요청',
        CURRENT_TIMESTAMP - INTERVAL '2 days',
        'user01',
        'A202',
        TO_CHAR(CURRENT_DATE - INTERVAL '2 days', 'YYYY-MM-DD'),
        $$<section class="approval-doc-layout"><h2>백오피스 장비 교체 구매 요청</h2><p>관리자 승인을 기다리는 결재 대기 문서입니다.</p></section>$$,
        'Y'
    FROM template_ids
    UNION ALL
    SELECT
        'ATRZDEMO006',
        mkt_request,
        '봄 캠페인 실행 요청',
        CURRENT_TIMESTAMP - INTERVAL '1 day',
        'user02',
        'A203',
        TO_CHAR(CURRENT_DATE - INTERVAL '1 day', 'YYYY-MM-DD'),
        $$<section class="approval-doc-layout"><h2>봄 캠페인 실행 요청</h2><p>선행 승인 이후 관리자 차례가 예정된 결재 문서입니다.</p></section>$$,
        'Y'
    FROM template_ids
    UNION ALL
    SELECT
        'ATRZDEMO007',
        trip_req,
        '고객사 현장 방문 보고',
        CURRENT_TIMESTAMP - INTERVAL '3 days',
        'user01',
        'A206',
        TO_CHAR(CURRENT_DATE - INTERVAL '3 days', 'YYYY-MM-DD'),
        $$<section class="approval-doc-layout"><h2>고객사 현장 방문 보고</h2><p>관리자를 참조자로 포함한 완료 문서입니다.</p></section>$$,
        'Y'
    FROM template_ids
)
INSERT INTO AUTHORIZATION_DOCUMENT (
    ATRZ_DOC_ID,
    ATRZ_DOC_TMPL_ID,
    ATRZ_DOC_TTL,
    ATRZ_SBMT_DT,
    ATRZ_USER_ID,
    CRNT_ATRZ_STEP_CD,
    "date",
    DEL_YN,
    HTML_DATA,
    OPEN_YN
)
SELECT
    atrz_doc_id,
    atrz_doc_tmpl_id,
    atrz_doc_ttl,
    atrz_sbmt_dt,
    atrz_user_id,
    crnt_atrz_step_cd,
    submitted_label,
    'N',
    html_data,
    open_yn
FROM demo_documents
WHERE atrz_doc_tmpl_id IS NOT NULL;

INSERT INTO AUTHORIZATION_LINE (
    ATRZ_LINE_SQN,
    ATRZ_DOC_ID,
    ATRZ_LINE_SEQ,
    ATRZ_APPR_USER_ID,
    ATRZ_APPR_STTS,
    PRCS_DT,
    SIGN_FILE_ID,
    ATRZ_OPNN,
    ATRZ_ACT,
    APPR_ATRZ_YN
)
VALUES
    (8101, 'ATRZDEMO001', 1, 'user01', 'A303', CURRENT_TIMESTAMP - INTERVAL '3 days 20 hours', NULL, '요구사항 범위를 반영했습니다.', 'A401', 'Y'),
    (8102, 'ATRZDEMO001', 2, 'user02', 'A302', NULL, NULL, NULL, NULL, 'Y'),
    (8201, 'ATRZDEMO002', 1, 'user01', 'A303', CURRENT_TIMESTAMP - INTERVAL '11 days 20 hours', NULL, '업무 인수 일정을 확인했습니다.', 'A401', 'Y'),
    (8202, 'ATRZDEMO002', 2, 'user02', 'A303', CURRENT_TIMESTAMP - INTERVAL '11 days 12 hours', NULL, '일정 조정 반영 완료', 'A401', 'Y'),
    (8301, 'ATRZDEMO003', 1, 'user01', 'A303', CURRENT_TIMESTAMP - INTERVAL '8 days 21 hours', NULL, '증빙 보완 후 다시 올려 주세요.', 'A402', 'Y'),
    (8401, 'ATRZDEMO004', 1, 'user01', NULL, NULL, NULL, NULL, NULL, 'Y'),
    (8402, 'ATRZDEMO004', 2, 'user02', NULL, NULL, NULL, NULL, NULL, 'Y'),
    (8501, 'ATRZDEMO005', 1, 'admin', 'A302', NULL, NULL, NULL, NULL, 'Y'),
    (8502, 'ATRZDEMO005', 2, 'user02', NULL, NULL, NULL, NULL, NULL, 'Y'),
    (8601, 'ATRZDEMO006', 1, 'user01', 'A302', NULL, NULL, NULL, NULL, 'Y'),
    (8602, 'ATRZDEMO006', 2, 'admin', NULL, NULL, NULL, NULL, NULL, 'Y'),
    (8701, 'ATRZDEMO007', 1, 'user02', 'A303', CURRENT_TIMESTAMP - INTERVAL '2 days 18 hours', NULL, '현장 이슈 없이 종료되었습니다.', 'A401', 'Y');

INSERT INTO AUTHORIZATION_DOCUMENT_RECEIVER (
    ATRZ_DOC_ID,
    ATRZ_RCVR_ID,
    OPEN_DT,
    OPEN_YN
)
VALUES
    ('ATRZDEMO001', 'user01', CURRENT_TIMESTAMP - INTERVAL '3 days 19 hours', 'Y'),
    ('ATRZDEMO002', 'user02', CURRENT_TIMESTAMP - INTERVAL '11 days 10 hours', 'Y'),
    ('ATRZDEMO005', 'user02', NULL, 'N'),
    ('ATRZDEMO007', 'admin', CURRENT_TIMESTAMP - INTERVAL '2 days 16 hours', 'Y');

INSERT INTO AUTHORIZATION_TEMP (
    ATRZ_TEMP_SQN,
    ATRZ_USER_ID,
    ATRZ_DOC_TMPL_ID,
    ATRZ_DOC_TTL,
    ATRZ_SBMT_DT,
    DEL_YN,
    HTML_DATA,
    OPEN_YN,
    ATRZ_FILE_ID
)
SELECT
    'DEMO-TEMP-001',
    'admin',
    ATRZ_DOC_TMPL_ID,
    '모니터 추가 구매안 초안',
    CURRENT_TIMESTAMP - INTERVAL '20 hours',
    'N',
    $$<section class="approval-doc-layout"><h2>모니터 추가 구매안 초안</h2><p>예산 확정 전 저장된 임시 문서입니다.</p></section>$$,
    'Y',
    NULL
FROM AUTHORIZATION_DOCUMENT_TEMPLATE
WHERE ATRZ_DOC_CD = 'PO_REQUEST'
LIMIT 1;

INSERT INTO CUSTOM_LINE_BOOKMARK (
    CSTM_LINE_BM_SQN,
    USER_ID,
    CSTM_LINE_BM_NM,
    ATRZ_LINE_SEQ,
    ATRZ_APPR_ID,
    APPR_ATRZ_YN
)
VALUES
    ('DEMO-BM-001', 'admin', '기본 승인선', 1, 'user01', 'Y'),
    ('DEMO-BM-002', 'admin', '기본 승인선', 2, 'user02', 'Y');

INSERT INTO VACATION (
    VACT_SQN,
    ATRZ_DOC_ID,
    VACT_USER_ID,
    VACT_CD,
    VACT_BGNG_DT,
    VACT_END_DT,
    USE_VACT_CNT,
    ALLDAY,
    VACT_EXPLN
)
VALUES
    (
        'DEMO-VACT-001',
        'ATRZDEMO002',
        'admin',
        'E101',
        CURRENT_DATE - INTERVAL '32 days',
        CURRENT_DATE - INTERVAL '30 days',
        3,
        'Y',
        '상반기 연차 사용'
    );

INSERT INTO MEETING_ROOM (
    ROOM_ID,
    ROOM_NAME,
    LOCATION,
    CAPACITY,
    USE_YN,
    DEL_YN
)
VALUES
    ('ROOM01', '3층 대회의실', '본관 3층', 12, 'Y', 'N'),
    ('ROOM02', '제품 리뷰룸', '본관 2층', 8, 'Y', 'N'),
    ('ROOM03', '고객 미팅룸', '별관 1층', 6, 'Y', 'N');

INSERT INTO MEETING_RESERVATION (
    RESERVATION_ID,
    ROOM_ID,
    USER_ID,
    START_TIME,
    END_TIME,
    TITLE,
    MEETING_DATE
)
VALUES
    (9001, 'ROOM01', 'admin', 10, 12, '주간 운영 회의', CURRENT_DATE),
    (9002, 'ROOM02', 'user01', 14, 15, '제품 기획 리뷰', CURRENT_DATE),
    (9003, 'ROOM03', 'user02', 16, 17, '고객사 데모 리허설', CURRENT_DATE),
    (9004, 'ROOM01', 'user01', 9, 10, '다음날 아침 스탠드업', CURRENT_DATE + 1);

SELECT setval(
    'SEQ_MEETING_RESERVATION',
    GREATEST((SELECT COALESCE(MAX(RESERVATION_ID), 0) FROM MEETING_RESERVATION), 9004),
    true
);

INSERT INTO TIME_AND_ATTENDANCE (
    WORK_YMD,
    USER_ID,
    WORK_BGNG_DT,
    WORK_END_DT,
    WORK_HR,
    LATE_YN,
    EARLY_YN,
    OVERTIME_YN,
    OVERTIME_HR
)
VALUES
    (CURRENT_DATE - 4, 'admin', CURRENT_DATE - 4 + TIME '08:55', CURRENT_DATE - 4 + TIME '18:20', '505', 'N', 'N', 'Y', '20'),
    (CURRENT_DATE - 3, 'admin', CURRENT_DATE - 3 + TIME '09:12', CURRENT_DATE - 3 + TIME '18:05', '473', 'Y', 'N', 'Y', '5'),
    (CURRENT_DATE - 2, 'admin', CURRENT_DATE - 2 + TIME '08:58', CURRENT_DATE - 2 + TIME '17:40', '462', 'N', 'Y', 'N', '0'),
    (CURRENT_DATE - 1, 'admin', CURRENT_DATE - 1 + TIME '09:01', CURRENT_DATE - 1 + TIME '18:12', '491', 'Y', 'N', 'Y', '12'),
    (CURRENT_DATE, 'admin', CURRENT_DATE + TIME '08:56', NULL, '0', 'N', 'N', 'N', '0'),
    (CURRENT_DATE + 1, 'admin', CURRENT_DATE + 1 + TIME '08:56', NULL, '0', 'N', 'N', 'N', '0'),
    (CURRENT_DATE - 4, 'user01', CURRENT_DATE - 4 + TIME '09:00', CURRENT_DATE - 4 + TIME '18:00', '480', 'N', 'N', 'N', '0'),
    (CURRENT_DATE - 3, 'user01', CURRENT_DATE - 3 + TIME '08:47', CURRENT_DATE - 3 + TIME '18:03', '496', 'N', 'N', 'Y', '3'),
    (CURRENT_DATE - 2, 'user01', CURRENT_DATE - 2 + TIME '08:59', CURRENT_DATE - 2 + TIME '18:11', '492', 'N', 'N', 'Y', '11'),
    (CURRENT_DATE - 1, 'user01', CURRENT_DATE - 1 + TIME '09:05', CURRENT_DATE - 1 + TIME '17:58', '473', 'Y', 'Y', 'N', '0'),
    (CURRENT_DATE, 'user01', CURRENT_DATE + TIME '18:00', CURRENT_DATE + TIME '18:00', '0', 'N', 'N', 'N', '0'),
    (CURRENT_DATE + 1, 'user01', CURRENT_DATE + 1 + TIME '18:00', CURRENT_DATE + 1 + TIME '18:00', '0', 'N', 'N', 'N', '0'),
    (CURRENT_DATE - 4, 'user02', CURRENT_DATE - 4 + TIME '08:50', CURRENT_DATE - 4 + TIME '18:25', '515', 'N', 'N', 'Y', '25'),
    (CURRENT_DATE - 3, 'user02', CURRENT_DATE - 3 + TIME '09:08', CURRENT_DATE - 3 + TIME '18:10', '482', 'Y', 'N', 'Y', '10'),
    (CURRENT_DATE - 2, 'user02', CURRENT_DATE - 2 + TIME '08:57', CURRENT_DATE - 2 + TIME '17:55', '478', 'N', 'Y', 'N', '0'),
    (CURRENT_DATE - 1, 'user02', CURRENT_DATE - 1 + TIME '09:10', CURRENT_DATE - 1 + TIME '18:06', '476', 'Y', 'N', 'Y', '6'),
    (CURRENT_DATE, 'user02', CURRENT_DATE + TIME '18:00', CURRENT_DATE + TIME '18:00', '0', 'N', 'N', 'N', '0'),
    (CURRENT_DATE + 1, 'user02', CURRENT_DATE + 1 + TIME '18:00', CURRENT_DATE + 1 + TIME '18:00', '0', 'N', 'N', 'N', '0')
ON CONFLICT (USER_ID, WORK_YMD)
DO UPDATE SET
    WORK_BGNG_DT = EXCLUDED.WORK_BGNG_DT,
    WORK_END_DT = EXCLUDED.WORK_END_DT,
    WORK_HR = EXCLUDED.WORK_HR,
    LATE_YN = EXCLUDED.LATE_YN,
    EARLY_YN = EXCLUDED.EARLY_YN,
    OVERTIME_YN = EXCLUDED.OVERTIME_YN,
    OVERTIME_HR = EXCLUDED.OVERTIME_HR;

INSERT INTO ATTENDANCE_DEPART_STATUS (
    USER_ID,
    WORK_YMD,
    WORK_BGNG_DT,
    WORK_END_DT,
    WORK_HR,
    LATE_YN,
    EARLY_YN,
    OVERTIME_YN,
    OVERTIME_HR,
    WORK_STTS_CD,
    VACT_YN,
    BZTR_YN
)
VALUES
    ('admin', CURRENT_DATE, CURRENT_DATE + TIME '08:56', NULL, '0', 'N', 'N', 'N', '0', 'C101', 'N', 'N'),
    ('user01', CURRENT_DATE, CURRENT_DATE + TIME '18:00', CURRENT_DATE + TIME '18:00', '0', 'N', 'N', 'N', '0', 'C104', 'Y', 'N'),
    ('user02', CURRENT_DATE, CURRENT_DATE + TIME '18:00', CURRENT_DATE + TIME '18:00', '0', 'N', 'N', 'N', '0', 'C105', 'N', 'Y');

INSERT INTO USER_WEEKLY_ATTENDANCE (
    USER_ID,
    USER_NM,
    WORK_WEEK_START_DATE,
    WORK_DAYS,
    TOTAL_WORK_HR,
    LATE_COUNT,
    EARLY_COUNT,
    OVERTIME_COUNT,
    TOTAL_OVERTIME_HR,
    DEPT_NM,
    JBGD_NM
)
VALUES
    ('admin', '관리자', date_trunc('week', CURRENT_DATE), '5', '1931', 2, 1, 3, '37', '본사', '부장'),
    ('user01', '테스트 사용자', date_trunc('week', CURRENT_DATE), '5', '1941', 1, 1, 2, '14', '개발', '사원'),
    ('user02', 'Test User 2', date_trunc('week', CURRENT_DATE), '5', '1951', 2, 1, 3, '41', '개발', '사원')
ON CONFLICT (USER_ID)
DO UPDATE SET
    USER_NM = EXCLUDED.USER_NM,
    WORK_WEEK_START_DATE = EXCLUDED.WORK_WEEK_START_DATE,
    WORK_DAYS = EXCLUDED.WORK_DAYS,
    TOTAL_WORK_HR = EXCLUDED.TOTAL_WORK_HR,
    LATE_COUNT = EXCLUDED.LATE_COUNT,
    EARLY_COUNT = EXCLUDED.EARLY_COUNT,
    OVERTIME_COUNT = EXCLUDED.OVERTIME_COUNT,
    TOTAL_OVERTIME_HR = EXCLUDED.TOTAL_OVERTIME_HR,
    DEPT_NM = EXCLUDED.DEPT_NM,
    JBGD_NM = EXCLUDED.JBGD_NM;

INSERT INTO USER_MONTHLY_ATTENDANCE (
    USER_ID,
    USER_NM,
    DEPT_ID,
    WORK_MONTH,
    WORK_DAYS,
    TOTAL_WORK_HR,
    LATE_COUNT,
    EARLY_COUNT,
    OVERTIME_COUNT,
    TOTAL_OVERTIME_HR,
    ABSENT_DAYS,
    DEPT_NM,
    JBGD_NM
)
VALUES
    ('admin', '관리자', 'DP001000', TO_CHAR(CURRENT_DATE, 'YYYY-MM'), '20', '8200', 3, 1, 5, '120', '0', '본사', '부장'),
    ('user01', '테스트 사용자', 'DP001002', TO_CHAR(CURRENT_DATE, 'YYYY-MM'), '18', '7440', 2, 1, 2, '45', '1', '개발', '사원'),
    ('user02', 'Test User 2', 'DP001002', TO_CHAR(CURRENT_DATE, 'YYYY-MM'), '19', '7810', 3, 1, 4, '75', '0', '개발', '사원');

COMMIT;
