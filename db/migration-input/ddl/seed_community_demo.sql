-- Community demo seed for workspace, join approvals, favorites, and org sync.

UPDATE COMMUNITY c
SET COMMUNITY_NM = d.DEPT_NM,
    COMMUNITY_DESC = d.DEPT_NM || ' 조직 커뮤니티',
    INTRO_TEXT = d.DEPT_NM || ' 조직 커뮤니티',
    VISIBILITY_CD = 'org',
    JOIN_POLICY_CD = 'auto',
    CLOSED_YN = 'N',
    CLOSED_DT = NULL,
    DEL_YN = 'N',
    LAST_CHG_DT = CURRENT_TIMESTAMP
FROM DEPARTMENT d
WHERE c.COMMUNITY_TYPE_CD = 'org'
  AND c.DEPT_ID = d.DEPT_ID
  AND COALESCE(d.USE_YN, 'N') = 'Y';

INSERT INTO COMMUNITY (
    COMMUNITY_NM,
    COMMUNITY_DESC,
    COMMUNITY_TYPE_CD,
    OWNER_USER_ID,
    DEL_YN,
    CRT_DT,
    LAST_CHG_DT,
    VISIBILITY_CD,
    JOIN_POLICY_CD,
    DEPT_ID,
    INTRO_TEXT,
    POST_TEMPLATE_HTML,
    CLOSED_YN,
    CLOSED_DT
)
SELECT
    d.DEPT_NM,
    d.DEPT_NM || ' 조직 커뮤니티',
    'org',
    NULL,
    'N',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'org',
    'auto',
    d.DEPT_ID,
    d.DEPT_NM || ' 조직 커뮤니티',
    '<p><strong>' || d.DEPT_NM || '</strong> 공지와 업무 공유를 위한 기본 양식입니다.</p><p>목적, 진행 상황, 필요한 협업 내용을 적어 주세요.</p>',
    'N',
    NULL
FROM DEPARTMENT d
WHERE COALESCE(d.USE_YN, 'N') = 'Y'
  AND NOT EXISTS (
      SELECT 1
      FROM COMMUNITY c
      WHERE c.COMMUNITY_TYPE_CD = 'org'
        AND c.DEPT_ID = d.DEPT_ID
  );

UPDATE COMMUNITY
SET CLOSED_YN = 'Y',
    CLOSED_DT = COALESCE(CLOSED_DT, CURRENT_TIMESTAMP),
    DEL_YN = 'Y',
    LAST_CHG_DT = CURRENT_TIMESTAMP
WHERE COMMUNITY_TYPE_CD = 'org'
  AND DEPT_ID IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM DEPARTMENT d
      WHERE d.DEPT_ID = COMMUNITY.DEPT_ID
        AND COALESCE(d.USE_YN, 'N') = 'Y'
  );

INSERT INTO COMMUNITY_MEMBER (
    COMMUNITY_ID,
    USER_ID,
    ROLE_CD,
    STATUS_CD,
    CRT_DT,
    LAST_CHG_DT
)
SELECT
    c.COMMUNITY_ID,
    u.USER_ID,
    'member',
    'active',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM COMMUNITY c
JOIN USERS u
  ON u.DEPT_ID = c.DEPT_ID
WHERE c.COMMUNITY_TYPE_CD = 'org'
  AND COALESCE(c.DEL_YN, 'N') = 'N'
  AND COALESCE(c.CLOSED_YN, 'N') = 'N'
  AND COALESCE(u.RSGNTN_YN, 'N') = 'N'
ON CONFLICT (COMMUNITY_ID, USER_ID)
DO UPDATE SET
    ROLE_CD = 'member',
    STATUS_CD = 'active',
    LAST_CHG_DT = CURRENT_TIMESTAMP;

UPDATE COMMUNITY_MEMBER cm
SET STATUS_CD = 'removed',
    LAST_CHG_DT = CURRENT_TIMESTAMP
FROM COMMUNITY c
WHERE cm.COMMUNITY_ID = c.COMMUNITY_ID
  AND c.COMMUNITY_TYPE_CD = 'org'
  AND (
      NOT EXISTS (
          SELECT 1
          FROM USERS u
          WHERE u.USER_ID = cm.USER_ID
            AND COALESCE(u.RSGNTN_YN, 'N') = 'N'
            AND u.DEPT_ID IS NOT DISTINCT FROM c.DEPT_ID
      )
  )
  AND cm.STATUS_CD <> 'removed';

INSERT INTO COMMUNITY (
    COMMUNITY_NM,
    COMMUNITY_DESC,
    COMMUNITY_TYPE_CD,
    OWNER_USER_ID,
    DEL_YN,
    CRT_DT,
    LAST_CHG_DT,
    VISIBILITY_CD,
    JOIN_POLICY_CD,
    INTRO_TEXT,
    POST_TEMPLATE_HTML,
    CLOSED_YN
)
SELECT
    U&'\BAA8\B450\C758\0020\B7EC\B2DD\0020\B77C\C6B4\C9C0',
    '전사 자유 소통용 공개 커뮤니티',
    'general',
    'admin',
    'N',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'public',
    'instant',
    '업무 공유와 가벼운 의견 교환을 위한 메인 커뮤니티입니다.',
    '<p><strong>공유할 내용</strong></p><ul><li>배경</li><li>핵심 포인트</li><li>도움이 필요한 항목</li></ul>',
    'N'
WHERE NOT EXISTS (SELECT 1 FROM COMMUNITY WHERE COMMUNITY_NM = U&'\BAA8\B450\C758\0020\B7EC\B2DD\0020\B77C\C6B4\C9C0' AND COMMUNITY_TYPE_CD = 'general');

INSERT INTO COMMUNITY (
    COMMUNITY_NM,
    COMMUNITY_DESC,
    COMMUNITY_TYPE_CD,
    OWNER_USER_ID,
    DEL_YN,
    CRT_DT,
    LAST_CHG_DT,
    VISIBILITY_CD,
    JOIN_POLICY_CD,
    INTRO_TEXT,
    POST_TEMPLATE_HTML,
    CLOSED_YN
)
SELECT
    '디자인 요청',
    '디자인 협업 요청과 승인 대기를 위한 공개 커뮤니티',
    'general',
    'user01',
    'N',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'public',
    'approval',
    '시안 요청, 리뷰, 일정 조율을 다루는 협업 커뮤니티입니다.',
    '<p><strong>요청 배경</strong></p><p></p><p><strong>필요 산출물</strong></p><p></p><p><strong>희망 일정</strong></p><p></p>',
    'N'
WHERE NOT EXISTS (SELECT 1 FROM COMMUNITY WHERE COMMUNITY_NM = '디자인 요청' AND COMMUNITY_TYPE_CD = 'general');

INSERT INTO COMMUNITY (
    COMMUNITY_NM,
    COMMUNITY_DESC,
    COMMUNITY_TYPE_CD,
    OWNER_USER_ID,
    DEL_YN,
    CRT_DT,
    LAST_CHG_DT,
    VISIBILITY_CD,
    JOIN_POLICY_CD,
    INTRO_TEXT,
    POST_TEMPLATE_HTML,
    CLOSED_YN
)
SELECT
    '보안 운영 공지',
    '운영자 글 중심으로 노출되는 공지 커뮤니티',
    'notice',
    'admin',
    'N',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'private',
    'invite_only',
    '보안 이슈와 운영 공지를 다루는 제한 커뮤니티입니다.',
    '<p><strong>공지 요약</strong></p><p></p><p><strong>영향 범위</strong></p><p></p><p><strong>대응 일정</strong></p><p></p>',
    'N'
WHERE NOT EXISTS (SELECT 1 FROM COMMUNITY WHERE COMMUNITY_NM = '보안 운영 공지' AND COMMUNITY_TYPE_CD = 'notice');

INSERT INTO COMMUNITY (
    COMMUNITY_NM,
    COMMUNITY_DESC,
    COMMUNITY_TYPE_CD,
    OWNER_USER_ID,
    DEL_YN,
    CRT_DT,
    LAST_CHG_DT,
    VISIBILITY_CD,
    JOIN_POLICY_CD,
    INTRO_TEXT,
    POST_TEMPLATE_HTML,
    CLOSED_YN
)
SELECT
    '프로덕트 실험실',
    '초대 기반 비공개 커뮤니티',
    'general',
    'user01',
    'N',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'private',
    'invite_only',
    '실험 아이디어와 리뷰를 비공개로 정리하는 공간입니다.',
    '<p><strong>실험 목표</strong></p><p></p><p><strong>가설</strong></p><p></p><p><strong>측정 지표</strong></p><p></p>',
    'N'
WHERE NOT EXISTS (SELECT 1 FROM COMMUNITY WHERE COMMUNITY_NM = '프로덕트 실험실' AND COMMUNITY_TYPE_CD = 'general');

WITH lounge AS (
    SELECT COMMUNITY_ID FROM COMMUNITY WHERE COMMUNITY_NM = U&'\BAA8\B450\C758\0020\B7EC\B2DD\0020\B77C\C6B4\C9C0' AND COMMUNITY_TYPE_CD = 'general'
), design_req AS (
    SELECT COMMUNITY_ID FROM COMMUNITY WHERE COMMUNITY_NM = '디자인 요청' AND COMMUNITY_TYPE_CD = 'general'
), security_notice AS (
    SELECT COMMUNITY_ID FROM COMMUNITY WHERE COMMUNITY_NM = '보안 운영 공지' AND COMMUNITY_TYPE_CD = 'notice'
), product_lab AS (
    SELECT COMMUNITY_ID FROM COMMUNITY WHERE COMMUNITY_NM = '프로덕트 실험실' AND COMMUNITY_TYPE_CD = 'general'
)
INSERT INTO COMMUNITY_MEMBER (COMMUNITY_ID, USER_ID, ROLE_CD, STATUS_CD, CRT_DT, LAST_CHG_DT)
SELECT COMMUNITY_ID, USER_ID, ROLE_CD, STATUS_CD, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT (SELECT COMMUNITY_ID FROM lounge) AS COMMUNITY_ID, 'admin' AS USER_ID, 'owner' AS ROLE_CD, 'active' AS STATUS_CD
    UNION ALL SELECT (SELECT COMMUNITY_ID FROM lounge), 'user01', 'member', 'active'
    UNION ALL SELECT (SELECT COMMUNITY_ID FROM lounge), 'user02', 'member', 'active'
    UNION ALL SELECT (SELECT COMMUNITY_ID FROM design_req), 'user01', 'owner', 'active'
    UNION ALL SELECT (SELECT COMMUNITY_ID FROM design_req), 'admin', 'operator', 'active'
    UNION ALL SELECT (SELECT COMMUNITY_ID FROM security_notice), 'admin', 'owner', 'active'
    UNION ALL SELECT (SELECT COMMUNITY_ID FROM security_notice), 'user01', 'operator', 'active'
    UNION ALL SELECT (SELECT COMMUNITY_ID FROM security_notice), 'user02', 'member', 'active'
    UNION ALL SELECT (SELECT COMMUNITY_ID FROM product_lab), 'user01', 'owner', 'active'
    UNION ALL SELECT (SELECT COMMUNITY_ID FROM product_lab), 'admin', 'member', 'active'
) seed
WHERE COMMUNITY_ID IS NOT NULL
ON CONFLICT (COMMUNITY_ID, USER_ID)
DO UPDATE SET
    ROLE_CD = EXCLUDED.ROLE_CD,
    STATUS_CD = EXCLUDED.STATUS_CD,
    LAST_CHG_DT = CURRENT_TIMESTAMP;

WITH design_req AS (
    SELECT COMMUNITY_ID FROM COMMUNITY WHERE COMMUNITY_NM = '디자인 요청' AND COMMUNITY_TYPE_CD = 'general'
)
INSERT INTO COMMUNITY_MEMBER (COMMUNITY_ID, USER_ID, ROLE_CD, STATUS_CD, CRT_DT, LAST_CHG_DT)
SELECT COMMUNITY_ID, 'user02', 'member', 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM design_req
WHERE COMMUNITY_ID IS NOT NULL
ON CONFLICT (COMMUNITY_ID, USER_ID)
DO UPDATE SET
    ROLE_CD = 'member',
    STATUS_CD = 'pending',
    LAST_CHG_DT = CURRENT_TIMESTAMP;

INSERT INTO COMMUNITY_USER_PREF (COMMUNITY_ID, USER_ID, FAVORITE_YN, SORT_ORDR, CRT_DT, LAST_CHG_DT)
SELECT COMMUNITY_ID, USER_ID, FAVORITE_YN, SORT_ORDR, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT c.COMMUNITY_ID, 'admin' AS USER_ID, CASE WHEN c.COMMUNITY_NM IN (U&'\BAA8\B450\C758\0020\B7EC\B2DD\0020\B77C\C6B4\C9C0', '보안 운영 공지') THEN 'Y' ELSE 'N' END AS FAVORITE_YN,
           CASE c.COMMUNITY_NM WHEN '보안 운영 공지' THEN 1 WHEN U&'\BAA8\B450\C758\0020\B7EC\B2DD\0020\B77C\C6B4\C9C0' THEN 2 WHEN '프로덕트 실험실' THEN 3 ELSE 20 END AS SORT_ORDR
    FROM COMMUNITY c
    WHERE c.COMMUNITY_NM IN (U&'\BAA8\B450\C758\0020\B7EC\B2DD\0020\B77C\C6B4\C9C0', '보안 운영 공지', '프로덕트 실험실')
    UNION ALL
    SELECT c.COMMUNITY_ID, 'user01', CASE WHEN c.COMMUNITY_NM IN ('디자인 요청', '프로덕트 실험실') THEN 'Y' ELSE 'N' END,
           CASE c.COMMUNITY_NM WHEN '디자인 요청' THEN 1 WHEN '프로덕트 실험실' THEN 2 WHEN U&'\BAA8\B450\C758\0020\B7EC\B2DD\0020\B77C\C6B4\C9C0' THEN 3 ELSE 20 END
    FROM COMMUNITY c
    WHERE c.COMMUNITY_NM IN (U&'\BAA8\B450\C758\0020\B7EC\B2DD\0020\B77C\C6B4\C9C0', '디자인 요청', '프로덕트 실험실')
    UNION ALL
    SELECT c.COMMUNITY_ID, 'user02', CASE WHEN c.COMMUNITY_NM = U&'\BAA8\B450\C758\0020\B7EC\B2DD\0020\B77C\C6B4\C9C0' THEN 'Y' ELSE 'N' END,
           CASE c.COMMUNITY_NM WHEN U&'\BAA8\B450\C758\0020\B7EC\B2DD\0020\B77C\C6B4\C9C0' THEN 1 WHEN '보안 운영 공지' THEN 2 ELSE 20 END
    FROM COMMUNITY c
    WHERE c.COMMUNITY_NM IN (U&'\BAA8\B450\C758\0020\B7EC\B2DD\0020\B77C\C6B4\C9C0', '보안 운영 공지')
) pref_seed
ON CONFLICT (COMMUNITY_ID, USER_ID)
DO UPDATE SET
    FAVORITE_YN = EXCLUDED.FAVORITE_YN,
    SORT_ORDR = EXCLUDED.SORT_ORDR,
    LAST_CHG_DT = CURRENT_TIMESTAMP;
