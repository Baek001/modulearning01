-- Sample seed for local development (no production data migration)
BEGIN;

INSERT INTO COMMON_CODE_GROUP (CODE_GRP_ID, CODE_GRP_NM, USE_YN)
VALUES
('B1', '프로젝트 권한', 'Y'),
('B2', '프로젝트 유형', 'Y'),
('B3', '프로젝트 상태', 'Y'),
('B4', '업무 상태', 'Y'),
('WORK_STTS', '근무 상태', 'Y'),
('APPR_DOC_STTS', '결재 문서 상태', 'Y'),
('APPR_LINE_STTS', '결재선 상태', 'Y')
ON CONFLICT DO NOTHING;

INSERT INTO COMMON_CODE (CODE_ID, CODE_GRP_ID, CODE_NM, USE_YN)
VALUES
('B101', 'B1', '책임자', 'Y'),
('B102', 'B1', '팀원', 'Y'),
('B103', 'B1', '열람자', 'Y'),
('B104', 'B1', '제외', 'Y'),
('B201', 'B2', '일반', 'Y'),
('B202', 'B2', '신규 구축', 'Y'),
('B203', 'B2', '유지보수', 'Y'),
('B301', 'B3', '승인 대기', 'Y'),
('B302', 'B3', '진행', 'Y'),
('B303', 'B3', '보류', 'Y'),
('B304', 'B3', '완료', 'Y'),
('B305', 'B3', '취소', 'Y'),
('B401', 'B4', '미시작', 'Y'),
('B402', 'B4', '진행중', 'Y'),
('B403', 'B4', '보류', 'Y'),
('B404', 'B4', '완료', 'Y'),
('B405', 'B4', '삭제', 'Y'),
('C101', 'WORK_STTS', '근무중', 'Y'),
('C103', 'WORK_STTS', '퇴근', 'Y'),
('C104', 'WORK_STTS', '휴가', 'Y'),
('C105', 'WORK_STTS', '출장', 'Y'),
('A202', 'APPR_DOC_STTS', '결재 대기', 'Y'),
('A203', 'APPR_DOC_STTS', '결재 진행', 'Y'),
('A204', 'APPR_DOC_STTS', '반려', 'Y'),
('A205', 'APPR_DOC_STTS', '기안 회수', 'Y'),
('A206', 'APPR_DOC_STTS', '결재 완료', 'Y'),
('D001', 'APPR_DOC_STTS', '대기', 'Y'),
('D002', 'APPR_DOC_STTS', '승인', 'Y'),
('A301', 'APPR_LINE_STTS', '미열람', 'Y'),
('A302', 'APPR_LINE_STTS', '대기', 'Y'),
('A303', 'APPR_LINE_STTS', '처리 완료', 'Y'),
('A304', 'APPR_LINE_STTS', '반려', 'Y'),
('L001', 'APPR_LINE_STTS', '대기', 'Y'),
('L002', 'APPR_LINE_STTS', '승인', 'Y')
ON CONFLICT DO NOTHING;

INSERT INTO JOB_GRADE (JBGD_CD, JBGD_NM, APPR_ATRZ_YN, CRT_DT, USE_YN)
VALUES
('J001', '사원', 'N', CURRENT_TIMESTAMP, 'Y'),
('J002', '대리', 'N', CURRENT_TIMESTAMP, 'Y'),
('J003', '과장', 'Y', CURRENT_TIMESTAMP, 'Y'),
('J004', '부장', 'Y', CURRENT_TIMESTAMP, 'Y')
ON CONFLICT DO NOTHING;

INSERT INTO DEPARTMENT (DEPT_ID, DEPT_NM, USE_YN, UP_DEPT_ID, SORT_NUM)
VALUES
('DP001000', '본사', 'Y', NULL, 1),
('DP001001', '운영', 'Y', 'DP001000', 1),
('DP001002', '개발', 'Y', 'DP001000', 2)
ON CONFLICT DO NOTHING;

INSERT INTO USERS (
  USER_ID, USER_PSWD, USER_NM, USER_EMAIL, USER_TELNO, EXT_TEL,
  RSGNTN_YN, RSGNTN_YMD, DEPT_ID, JBGD_CD, USER_ROLE, HIRE_YMD, WORK_STTS_CD
) VALUES
('admin', '{noop}admin1234', '관리자', 'admin@modulearning.local', '010-0000-0000', '1001',
 'N', NULL, 'DP001000', 'J004', 'ROLE_ADMIN', CURRENT_TIMESTAMP, 'C103'),
('user01', '{noop}user1234', '테스트 사용자', 'user01@modulearning.local', '010-0000-0001', '1002',
 'N', NULL, 'DP001002', 'J001', 'ROLE_USER', CURRENT_TIMESTAMP, 'C103'),
('user02', '{noop}user2234', 'Test User 2', 'user02@modulearning.local', '010-0000-0002', '1003',
 'N', NULL, 'DP001002', 'J001', 'ROLE_USER', CURRENT_TIMESTAMP, 'C103')
ON CONFLICT DO NOTHING;

-- 알림 템플릿 기본 시드
INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'APPROVAL_01', '전자결재', '결재 요청이 도착했습니다.', 'approval', '/approval'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'APPROVAL_01');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'APPROVAL_02', '전자결재', '결재 문서가 승인되었습니다.', 'approval', '/approval'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'APPROVAL_02');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'APPROVAL_03', '전자결재', '결재 문서가 반려되었습니다.', 'approval', '/approval'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'APPROVAL_03');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'BOARD_01', '게시판', '게시글에 댓글이 등록되었습니다.', 'board', '/board'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'BOARD_01');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'BOARD_02', '게시판', '댓글에 답글이 등록되었습니다.', 'board', '/board'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'BOARD_02');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'BOARD_URGENT', '게시판', '긴급 게시글이 등록되었습니다.', 'board', '/board'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'BOARD_URGENT');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'BOARD_REPORT', '게시판', '게시글 신고가 접수되었습니다.', 'board', '/board'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'BOARD_REPORT');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'BOARD_MENTION', '게시판', '게시글에서 회원님이 언급되었습니다.', 'board', '/board'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'BOARD_MENTION');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'MAIL_01', '메일', '새 메일이 도착했습니다.', 'mail', '/email'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'MAIL_01');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'TASK_02', '업무', '업무에 댓글이 등록되었습니다.', 'task', '/task'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'TASK_02');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'COMMUNITY_JOIN_REQUEST', '커뮤니티', '가입 신청이 도착했습니다.', 'community', '/community'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'COMMUNITY_JOIN_REQUEST');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'COMMUNITY_JOIN_APPROVED', '커뮤니티', '가입 신청이 승인되었습니다.', 'community', '/community'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'COMMUNITY_JOIN_APPROVED');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'COMMUNITY_JOIN_REJECTED', '커뮤니티', '가입 신청이 반려되었습니다.', 'community', '/community'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'COMMUNITY_JOIN_REJECTED');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'COMMUNITY_INVITED', '커뮤니티', '커뮤니티에 초대되었습니다.', 'community', '/community'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'COMMUNITY_INVITED');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'COMMUNITY_REMOVED', '커뮤니티', '커뮤니티 멤버 상태가 변경되었습니다.', 'community', '/community'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'COMMUNITY_REMOVED');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'CONTRACT_SENT', '전자계약', '전자계약 요청이 도착했습니다.', 'contract', '/approval/contracts'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'CONTRACT_SENT');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'CONTRACT_COMPLETED', '전자계약', '전자계약이 완료되었습니다.', 'contract', '/approval/contracts'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'CONTRACT_COMPLETED');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'CONTRACT_REQUEST', '전자계약', '계약서 양식 신청이 접수되었습니다.', 'contract', '/approval/contracts'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'CONTRACT_REQUEST');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'CONTRACT_TEMPLATE_APPROVED', '전자계약', '계약서 양식 신청이 승인되었습니다.', 'contract', '/approval/contracts'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'CONTRACT_TEMPLATE_APPROVED');

INSERT INTO ALARM_TEMPLATE (ALARM_CODE, ALARM_TITLE, ALARM_MESSAGE, ALARM_CATEGORY, RELATED_URL)
SELECT 'CONTRACT_REMINDER', '전자계약', '전자계약 서명을 다시 요청했습니다.', 'contract', '/approval/contracts'
WHERE NOT EXISTS (SELECT 1 FROM ALARM_TEMPLATE WHERE ALARM_CODE = 'CONTRACT_REMINDER');

INSERT INTO CONTRACT_COMPANY_SETTING (
  COMPANY_NM, SENDER_NM, SENDER_EMAIL, SENDER_TELNO, PROVIDER_NM, ADMIN_READY_YN,
  GUIDE_ACK_YN, CHECKLIST_JSON, NOTE_TEXT, CRT_USER_ID, LAST_CHG_USER_ID
)
SELECT
  '모두의 러닝', '관리자', 'admin@modulearning.local', '010-0000-0000', 'internal', 'N',
  'N',
  '{"items":[{"key":"provider","label":"전자계약 서비스 운영 계정 준비","done":false},{"key":"seal","label":"회사 직인 파일 등록","done":false},{"key":"templates","label":"계약서 양식 승인 및 게시","done":false}]}',
  '전자계약 기본 설정', 'admin', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM CONTRACT_COMPANY_SETTING);

-- 결재 양식 기본 시드 (카테고리 코드는 UI data-category 값과 일치해야 함)
UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE
SET
  ATRZ_DOC_TMPL_NM = '휴가 신청서',
  HTML_CONTENTS = $$<section><h2>휴가 신청서</h2><table><tr><th>휴가 종류</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>사유</th><td></td></tr><tr><th>업무 인수자</th><td></td></tr></table></section>$$,
  ATRZ_SECURE_LVL = '2',
  ATRZ_SAVE_YEAR = '5',
  ATRZ_CATEGORY = 'hr',
  ATRZ_DESCRIPTION = '연차/반차/병가 신청 문서',
  DEL_YN = 'N'
WHERE ATRZ_DOC_CD = 'VAC_REQ';

INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (
  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,
  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT
)
SELECT
  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),
  'VAC_REQ',
  '휴가 신청서',
  $$<section><h2>휴가 신청서</h2><table><tr><th>휴가 종류</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>사유</th><td></td></tr><tr><th>업무 인수자</th><td></td></tr></table></section>$$,
  '2', '5', 'hr', '연차/반차/병가 신청 문서', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'VAC_REQ');

UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE
SET
  ATRZ_DOC_TMPL_NM = '출장/외근 신청서',
  HTML_CONTENTS = $$<section><h2>출장/외근 신청서</h2><table><tr><th>방문지</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>목적</th><td></td></tr><tr><th>예상 비용</th><td></td></tr></table></section>$$,
  ATRZ_SECURE_LVL = '2',
  ATRZ_SAVE_YEAR = '5',
  ATRZ_CATEGORY = 'trip',
  ATRZ_DESCRIPTION = '출장 및 외근 승인 요청 문서',
  DEL_YN = 'N'
WHERE ATRZ_DOC_CD = 'TRIP_REQ';

INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (
  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,
  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT
)
SELECT
  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),
  'TRIP_REQ',
  '출장/외근 신청서',
  $$<section><h2>출장/외근 신청서</h2><table><tr><th>방문지</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>목적</th><td></td></tr><tr><th>예상 비용</th><td></td></tr></table></section>$$,
  '2', '5', 'trip', '출장 및 외근 승인 요청 문서', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'TRIP_REQ');

UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE
SET
  ATRZ_DOC_TMPL_NM = '지출 결의서',
  HTML_CONTENTS = $$<section><h2>지출 결의서</h2><table><tr><th>지출 일자</th><td></td></tr><tr><th>금액</th><td></td></tr><tr><th>비용 계정</th><td></td></tr><tr><th>세부 내역</th><td></td></tr></table></section>$$,
  ATRZ_SECURE_LVL = '2',
  ATRZ_SAVE_YEAR = '5',
  ATRZ_CATEGORY = 'finance',
  ATRZ_DESCRIPTION = '비용 집행 및 정산 승인 문서',
  DEL_YN = 'N'
WHERE ATRZ_DOC_CD = 'EXP_APPROVAL';

INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (
  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,
  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT
)
SELECT
  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),
  'EXP_APPROVAL',
  '지출 결의서',
  $$<section><h2>지출 결의서</h2><table><tr><th>지출 일자</th><td></td></tr><tr><th>금액</th><td></td></tr><tr><th>비용 계정</th><td></td></tr><tr><th>세부 내역</th><td></td></tr></table></section>$$,
  '2', '5', 'finance', '비용 집행 및 정산 승인 문서', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'EXP_APPROVAL');

UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE
SET
  ATRZ_DOC_TMPL_NM = '구매 품의서',
  HTML_CONTENTS = $$<section><h2>구매 품의서</h2><table><tr><th>품목</th><td></td></tr><tr><th>수량</th><td></td></tr><tr><th>예산</th><td></td></tr><tr><th>요청 사유</th><td></td></tr></table></section>$$,
  ATRZ_SECURE_LVL = '2',
  ATRZ_SAVE_YEAR = '5',
  ATRZ_CATEGORY = 'finance',
  ATRZ_DESCRIPTION = '구매 요청 및 예산 집행 문서',
  DEL_YN = 'N'
WHERE ATRZ_DOC_CD = 'PO_REQUEST';

INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (
  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,
  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT
)
SELECT
  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),
  'PO_REQUEST',
  '구매 품의서',
  $$<section><h2>구매 품의서</h2><table><tr><th>품목</th><td></td></tr><tr><th>수량</th><td></td></tr><tr><th>예산</th><td></td></tr><tr><th>요청 사유</th><td></td></tr></table></section>$$,
  '2', '5', 'finance', '구매 요청 및 예산 집행 문서', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'PO_REQUEST');

UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE
SET
  ATRZ_DOC_TMPL_NM = '프로젝트 기안서',
  HTML_CONTENTS = $$<section><h2>프로젝트 기안서</h2><table><tr><th>프로젝트명</th><td></td></tr><tr><th>목표</th><td></td></tr><tr><th>범위</th><td></td></tr><tr><th>일정</th><td></td></tr></table></section>$$,
  ATRZ_SECURE_LVL = '2',
  ATRZ_SAVE_YEAR = '5',
  ATRZ_CATEGORY = 'pro',
  ATRZ_DESCRIPTION = '프로젝트 실행 기안 문서',
  DEL_YN = 'N'
WHERE ATRZ_DOC_CD = 'PRO_PROPOSAL';

INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (
  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,
  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT
)
SELECT
  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),
  'PRO_PROPOSAL',
  '프로젝트 기안서',
  $$<section><h2>프로젝트 기안서</h2><table><tr><th>프로젝트명</th><td></td></tr><tr><th>목표</th><td></td></tr><tr><th>범위</th><td></td></tr><tr><th>일정</th><td></td></tr></table></section>$$,
  '2', '5', 'pro', '프로젝트 실행 기안 문서', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'PRO_PROPOSAL');

UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE
SET
  ATRZ_DOC_TMPL_NM = '마케팅 실행 요청서',
  HTML_CONTENTS = $$<section><h2>마케팅 실행 요청서</h2><table><tr><th>캠페인명</th><td></td></tr><tr><th>대상</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>기대 KPI</th><td></td></tr></table></section>$$,
  ATRZ_SECURE_LVL = '2',
  ATRZ_SAVE_YEAR = '5',
  ATRZ_CATEGORY = 'sales',
  ATRZ_DESCRIPTION = '영업/마케팅 실행 승인 문서',
  DEL_YN = 'N'
WHERE ATRZ_DOC_CD = 'MKT_REQUEST';

INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (
  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,
  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT
)
SELECT
  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),
  'MKT_REQUEST',
  '마케팅 실행 요청서',
  $$<section><h2>마케팅 실행 요청서</h2><table><tr><th>캠페인명</th><td></td></tr><tr><th>대상</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>기대 KPI</th><td></td></tr></table></section>$$,
  '2', '5', 'sales', '영업/마케팅 실행 승인 문서', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'MKT_REQUEST');

UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE
SET
  ATRZ_DOC_TMPL_NM = '개발/IT 작업 요청서',
  HTML_CONTENTS = $$<section><h2>개발/IT 작업 요청서</h2><table><tr><th>시스템</th><td></td></tr><tr><th>요청 내용</th><td></td></tr><tr><th>우선순위</th><td></td></tr><tr><th>희망 완료일</th><td></td></tr></table></section>$$,
  ATRZ_SECURE_LVL = '2',
  ATRZ_SAVE_YEAR = '5',
  ATRZ_CATEGORY = 'it',
  ATRZ_DESCRIPTION = '개발 및 IT 지원 요청 문서',
  DEL_YN = 'N'
WHERE ATRZ_DOC_CD = 'IT_WORK_REQ';

INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (
  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,
  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT
)
SELECT
  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),
  'IT_WORK_REQ',
  '개발/IT 작업 요청서',
  $$<section><h2>개발/IT 작업 요청서</h2><table><tr><th>시스템</th><td></td></tr><tr><th>요청 내용</th><td></td></tr><tr><th>우선순위</th><td></td></tr><tr><th>희망 완료일</th><td></td></tr></table></section>$$,
  '2', '5', 'it', '개발 및 IT 지원 요청 문서', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'IT_WORK_REQ');

UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE
SET
  ATRZ_DOC_TMPL_NM = '물류 이동 요청서',
  HTML_CONTENTS = $$<section><h2>물류 이동 요청서</h2><table><tr><th>출발지</th><td></td></tr><tr><th>도착지</th><td></td></tr><tr><th>품목/수량</th><td></td></tr><tr><th>요청 사유</th><td></td></tr></table></section>$$,
  ATRZ_SECURE_LVL = '2',
  ATRZ_SAVE_YEAR = '5',
  ATRZ_CATEGORY = 'logistics',
  ATRZ_DESCRIPTION = '재고 및 물류 이동 요청 문서',
  DEL_YN = 'N'
WHERE ATRZ_DOC_CD = 'LOG_TRANSFER';

INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (
  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,
  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT
)
SELECT
  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),
  'LOG_TRANSFER',
  '물류 이동 요청서',
  $$<section><h2>물류 이동 요청서</h2><table><tr><th>출발지</th><td></td></tr><tr><th>도착지</th><td></td></tr><tr><th>품목/수량</th><td></td></tr><tr><th>요청 사유</th><td></td></tr></table></section>$$,
  '2', '5', 'logistics', '재고 및 물류 이동 요청 문서', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'LOG_TRANSFER');

COMMIT;
