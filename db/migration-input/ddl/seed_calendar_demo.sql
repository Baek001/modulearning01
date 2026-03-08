BEGIN;

DELETE FROM FULLCALENDAR_TEAM
WHERE EVENT_ID IN ('FCTDEMO001', 'FCTDEMO002', 'FCTDEMO003');

DELETE FROM FULLCALENDAR_DEPT
WHERE EVENT_ID IN ('FCDEPT001', 'FCDEPT002');

DELETE FROM DEPARTMENT_SCHEDULE
WHERE DEPT_SCHD_ID IN ('DPTSCH001', 'DPTSCH002');

DELETE FROM USER_SCHEDULE
WHERE USER_SCHD_ID IN ('CALUSR001', 'CALUSR002', 'CALUSR003', 'CALUSR004', 'CALUSR005', 'CALUSR006');

DELETE FROM USER_FAVORITE_USER
WHERE (USER_ID = 'admin' AND TARGET_USER_ID IN ('user01', 'user02'))
   OR (USER_ID = 'user01' AND TARGET_USER_ID = 'admin')
   OR (USER_ID = 'user02' AND TARGET_USER_ID = 'admin');

DELETE FROM USER_TODO
WHERE TODO_ID IN (9101, 9102, 9103, 9104, 9105);

DELETE FROM PROJECT_MEMBER
WHERE BIZ_ID IN ('BIZCAL001', 'BIZCAL002');

DELETE FROM PROJECT
WHERE BIZ_ID IN ('BIZCAL001', 'BIZCAL002');

INSERT INTO USER_SCHEDULE (
    USER_SCHD_ID,
    USER_ID,
    SCHD_TTL,
    SCHD_STRT_DT,
    SCHD_END_DT,
    ALLDAY,
    USER_SCHD_EXPLN,
    DEL_YN
) VALUES
('CALUSR001', 'admin', '분기 운영 리뷰 준비', CURRENT_DATE + INTERVAL '1 day 09 hours', CURRENT_DATE + INTERVAL '1 day 10 hours 30 minutes', 'N', '운영 지표와 장애 대응 항목을 정리합니다.', 'N'),
('CALUSR002', 'admin', '거버넌스 정비일', CURRENT_DATE + INTERVAL '5 days', CURRENT_DATE + INTERVAL '5 days 23 hours 59 minutes', 'Y', '규정과 운영 기준 문서를 정리하는 날입니다.', 'N'),
('CALUSR003', 'user01', '디자인 QA 검토', CURRENT_DATE + INTERVAL '1 day 14 hours', CURRENT_DATE + INTERVAL '1 day 15 hours', 'N', '게시판과 커뮤니티 화면 QA를 진행합니다.', 'N'),
('CALUSR004', 'user01', '스프린트 회고 준비', CURRENT_DATE + INTERVAL '4 days 10 hours', CURRENT_DATE + INTERVAL '4 days 11 hours', 'N', '개발 스프린트 회고 아젠다를 정리합니다.', 'N'),
('CALUSR005', 'user02', '운영 문서 점검', CURRENT_DATE + INTERVAL '2 days 13 hours', CURRENT_DATE + INTERVAL '2 days 14 hours', 'N', '운영 가이드 초안 누락 항목을 확인합니다.', 'N'),
('CALUSR006', 'user02', '사용자 인터뷰 정리', CURRENT_DATE + INTERVAL '6 days 09 hours', CURRENT_DATE + INTERVAL '6 days 11 hours', 'N', '최근 피드백과 VOC를 문서로 정리합니다.', 'N');

INSERT INTO DEPARTMENT_SCHEDULE (
    DEPT_SCHD_ID,
    DEPT_ID,
    DEPT_SCHD_CRT_USER_ID,
    SCHD_TTL,
    SCHD_STRT_DT,
    SCHD_END_DT,
    DEPT_SCHD_EXPLN,
    ALLDAY,
    DEL_YN
) VALUES
('DPTSCH001', 'DP001000', 'admin', '전사 타운홀 사전 점검', CURRENT_DATE + INTERVAL '2 days 10 hours', CURRENT_DATE + INTERVAL '2 days 11 hours', '본사 전체 공유 전에 발표 자료와 진행 순서를 점검합니다.', 'N', 'N'),
('DPTSCH002', 'DP001002', 'user01', '개발 일정 조율 회의', CURRENT_DATE + INTERVAL '3 days 15 hours', CURRENT_DATE + INTERVAL '3 days 16 hours', '개발 부서 캘린더와 게시판 일정 충돌 여부를 점검합니다.', 'N', 'N');

INSERT INTO FULLCALENDAR_DEPT (
    EVENT_ID,
    DEPT_ID,
    USER_ID,
    USER_NM,
    TITLE,
    START_DT,
    END_DT,
    ALLDAY,
    DESCRIPTION,
    EVENT_TYPE,
    TYPE
) VALUES
('FCDEPT001', 'DP001000', 'admin', '관리자', '본사 운영 브리핑', CURRENT_DATE + INTERVAL '1 day 08 hours', CURRENT_DATE + INTERVAL '1 day 09 hours', 'N', '본사 운영 이슈와 일정 리스크를 공유합니다.', 'org_feed', 'briefing'),
('FCDEPT002', 'DP001002', 'user01', '테스트 사용자', '개발 배포 브리핑', CURRENT_DATE + INTERVAL '4 days 09 hours', CURRENT_DATE + INTERVAL '4 days 10 hours', 'N', '개발 부서 배포 일정을 공유합니다.', 'org_feed', 'deployment');

INSERT INTO PROJECT (
    BIZ_ID,
    BIZ_NM,
    BIZ_DETAIL,
    BIZ_GOAL,
    BIZ_SCOPE,
    BIZ_STTS_CD,
    BIZ_TYPE_CD,
    STRT_BIZ_DT,
    END_BIZ_DT
) VALUES
('BIZCAL001', '그룹웨어 리프레시', '게시판, 커뮤니티, 캘린더 마감 정리를 진행합니다.', '사용자 매뉴얼 기준 기능 정합성 확보', '전사 협업 모듈', 'B302', 'B201', CURRENT_DATE - INTERVAL '14 days', CURRENT_DATE + INTERVAL '40 days'),
('BIZCAL002', '캠페인 자동화', '운영 요청과 승인 흐름을 자동화합니다.', '반복 업무 감소와 승인 가시성 향상', '마케팅 운영', 'B302', 'B201', CURRENT_DATE - INTERVAL '7 days', CURRENT_DATE + INTERVAL '55 days');

INSERT INTO PROJECT_MEMBER (
    BIZ_AUTH_CD,
    BIZ_ID,
    BIZ_USER_ID
) VALUES
('B101', 'BIZCAL001', 'admin'),
('B102', 'BIZCAL001', 'user01'),
('B102', 'BIZCAL001', 'user02'),
('B101', 'BIZCAL002', 'admin'),
('B102', 'BIZCAL002', 'user02');

INSERT INTO FULLCALENDAR_TEAM (
    EVENT_ID,
    BIZ_ID,
    USER_ID,
    USER_NM,
    TITLE,
    START_DT,
    END_DT,
    ALLDAY,
    DESCRIPTION,
    EVENT_TYPE,
    TYPE
) VALUES
('FCTDEMO001', 'BIZCAL001', 'admin', '관리자', '리프레시 스프린트 체크인', CURRENT_DATE + INTERVAL '2 days 14 hours', CURRENT_DATE + INTERVAL '2 days 15 hours', 'N', '리프레시 프로젝트 진행 현황을 점검합니다.', 'team', 'sprint'),
('FCTDEMO002', 'BIZCAL001', 'user01', '테스트 사용자', '캘린더 UI 점검', CURRENT_DATE + INTERVAL '3 days 11 hours', CURRENT_DATE + INTERVAL '3 days 12 hours', 'N', '캘린더 월간/주간/목록/조회 화면을 검토합니다.', 'team', 'review'),
('FCTDEMO003', 'BIZCAL002', 'user02', 'Test User 2', '자동화 요구사항 정리', CURRENT_DATE + INTERVAL '5 days 16 hours', CURRENT_DATE + INTERVAL '5 days 17 hours', 'N', '캠페인 자동화 대상 업무를 정리합니다.', 'team', 'planning');

INSERT INTO USER_FAVORITE_USER (
    USER_ID,
    TARGET_USER_ID
) VALUES
('admin', 'user01'),
('admin', 'user02'),
('user01', 'admin'),
('user02', 'admin')
ON CONFLICT (USER_ID, TARGET_USER_ID) DO NOTHING;

INSERT INTO USER_TODO (
    TODO_ID,
    USER_ID,
    TARGET_USER_ID,
    TODO_TTL,
    TODO_CN,
    DONE_YN,
    DUE_DT
) VALUES
    (9101, 'admin', 'admin', '캘린더 데모 점검', '월간과 주간 화면에서 일정이 정상 노출되는지 확인합니다.', 'N', CURRENT_DATE + INTERVAL '0 day 18 hours'),
    (9102, 'admin', 'admin', '구독 사용자 정리', '즐겨찾기 사용자 목록과 일정 노출 여부를 검토합니다.', 'Y', CURRENT_DATE + INTERVAL '3 days 17 hours'),
    (9103, 'user01', 'user01', '조직 일정 문구 검토', '개발 일정 카드와 상세 문구를 확인합니다.', 'N', CURRENT_DATE + INTERVAL '1 day 17 hours'),
    (9104, 'user01', 'user01', '커뮤니티 일정 링크 확인', '커뮤니티 일정에서 원본 게시글 딥링크를 확인합니다.', 'Y', CURRENT_DATE + INTERVAL '4 days 18 hours'),
    (9105, 'user02', 'user02', '조회 화면 필터 확인', '작성자, 기간, 커뮤니티 필터를 검토합니다.', 'N', CURRENT_DATE + INTERVAL '2 days 18 hours')
ON CONFLICT (TODO_ID) DO UPDATE
SET USER_ID = EXCLUDED.USER_ID,
    TARGET_USER_ID = EXCLUDED.TARGET_USER_ID,
    TODO_TTL = EXCLUDED.TODO_TTL,
    TODO_CN = EXCLUDED.TODO_CN,
    DONE_YN = EXCLUDED.DONE_YN,
    DUE_DT = EXCLUDED.DUE_DT,
    LAST_CHG_DT = CURRENT_TIMESTAMP;

SELECT setval(
    pg_get_serial_sequence('user_todo', 'todo_id'),
    GREATEST(COALESCE((SELECT MAX(TODO_ID) FROM USER_TODO), 1), 9105),
    true
);

COMMIT;
