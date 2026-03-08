// Mock data for 모두의 러닝 groupware UI
export const mockUser = {
  userId: 'admin',
  userNm: '김관리',
  deptId: 'DEPT001',
  deptNm: '경영지원팀',
  jbgdCd: 'JG001',
  jbgdNm: '부장',
  userEmail: 'admin@modulearning.co.kr',
  userRole: 'ROLE_ADMIN',
  workSttsCd: 'WORK',
  hireYmd: '2020-03-15',
};

export const mockDepartments = [
  { deptId: 'DEPT000', deptNm: '모두의 러닝', upDeptId: null, children: [] },
  { deptId: 'DEPT001', deptNm: '경영지원팀', upDeptId: 'DEPT000' },
  { deptId: 'DEPT002', deptNm: '인사팀', upDeptId: 'DEPT000' },
  { deptId: 'DEPT003', deptNm: '개발팀', upDeptId: 'DEPT000' },
  { deptId: 'DEPT004', deptNm: '영업팀', upDeptId: 'DEPT000' },
  { deptId: 'DEPT005', deptNm: '마케팅팀', upDeptId: 'DEPT000' },
  { deptId: 'DEPT006', deptNm: '디자인팀', upDeptId: 'DEPT000' },
  { deptId: 'DEPT007', deptNm: '재무팀', upDeptId: 'DEPT000' },
];

export const mockUsers = [
  { userId: 'user001', userNm: '이영희', deptNm: '개발팀', jbgdNm: '과장', workSttsCd: 'WORK', userEmail: 'lee@modulearning.co.kr', extTel: '1001' },
  { userId: 'user002', userNm: '박민수', deptNm: '개발팀', jbgdNm: '대리', workSttsCd: 'WORK', userEmail: 'park@modulearning.co.kr', extTel: '1002' },
  { userId: 'user003', userNm: '최지현', deptNm: '디자인팀', jbgdNm: '사원', workSttsCd: 'MEETING', userEmail: 'choi@modulearning.co.kr', extTel: '1003' },
  { userId: 'user004', userNm: '정수진', deptNm: '영업팀', jbgdNm: '차장', workSttsCd: 'BIZTRIP', userEmail: 'jung@modulearning.co.kr', extTel: '1004' },
  { userId: 'user005', userNm: '한동훈', deptNm: '마케팅팀', jbgdNm: '대리', workSttsCd: 'WORK', userEmail: 'han@modulearning.co.kr', extTel: '1005' },
  { userId: 'user006', userNm: '강민지', deptNm: '인사팀', jbgdNm: '과장', workSttsCd: 'VACATION', userEmail: 'kang@modulearning.co.kr', extTel: '1006' },
  { userId: 'user007', userNm: '윤서준', deptNm: '재무팀', jbgdNm: '사원', workSttsCd: 'WORK', userEmail: 'yoon@modulearning.co.kr', extTel: '1007' },
  { userId: 'user008', userNm: '송하늘', deptNm: '경영지원팀', jbgdNm: '대리', workSttsCd: 'WORK', userEmail: 'song@modulearning.co.kr', extTel: '1008' },
  { userId: 'admin', userNm: '김관리', deptNm: '경영지원팀', jbgdNm: '부장', workSttsCd: 'WORK', userEmail: 'admin@modulearning.co.kr', extTel: '1000' },
];

export const mockBoards = [
  { pstId: 'PST001', pstTtl: '2026년 상반기 경영계획 안내', crtUserId: 'admin', crtUserNm: '김관리', frstCrtDt: '2026-03-05', viewCnt: 128, bbsCtgrCd: 'NOTICE', fixedYn: 'Y' },
  { pstId: 'PST002', pstTtl: '신규 프로젝트 킥오프 미팅 일정', crtUserId: 'user001', crtUserNm: '이영희', frstCrtDt: '2026-03-04', viewCnt: 67, bbsCtgrCd: 'NOTICE', fixedYn: 'N' },
  { pstId: 'PST003', pstTtl: '사내 동호회 회원 모집', crtUserId: 'user005', crtUserNm: '한동훈', frstCrtDt: '2026-03-03', viewCnt: 45, bbsCtgrCd: 'COMMUNITY', fixedYn: 'N' },
  { pstId: 'PST004', pstTtl: '3월 팀 빌딩 행사 안내', crtUserId: 'user006', crtUserNm: '강민지', frstCrtDt: '2026-03-02', viewCnt: 89, bbsCtgrCd: 'NOTICE', fixedYn: 'N' },
  { pstId: 'PST005', pstTtl: '개발팀 코드리뷰 가이드라인', crtUserId: 'user002', crtUserNm: '박민수', frstCrtDt: '2026-03-01', viewCnt: 34, bbsCtgrCd: 'COMMUNITY', fixedYn: 'N' },
  { pstId: 'PST006', pstTtl: '사내 카페테리아 메뉴 변경 안내', crtUserId: 'user008', crtUserNm: '송하늘', frstCrtDt: '2026-02-28', viewCnt: 156, bbsCtgrCd: 'NOTICE', fixedYn: 'N' },
];

export const mockApprovals = [
  { atrzDocId: 'ATRZ001', atrzDocTtl: '2026년 1분기 마케팅 예산 승인', atrzUserId: 'user005', atrzUserNm: '한동훈', atrzSbmtDt: '2026-03-05', crntAtrzStepCd: 'PENDING', atrzCategory: 'finance' },
  { atrzDocId: 'ATRZ002', atrzDocTtl: '신규 인력 채용 요청서', atrzUserId: 'user006', atrzUserNm: '강민지', atrzSbmtDt: '2026-03-04', crntAtrzStepCd: 'APPROVED', atrzCategory: 'hr' },
  { atrzDocId: 'ATRZ003', atrzDocTtl: '서버 장비 구매 품의서', atrzUserId: 'user001', atrzUserNm: '이영희', atrzSbmtDt: '2026-03-03', crntAtrzStepCd: 'PENDING', atrzCategory: 'it' },
  { atrzDocId: 'ATRZ004', atrzDocTtl: '출장 신청서 - 부산 영업소 방문', atrzUserId: 'user004', atrzUserNm: '정수진', atrzSbmtDt: '2026-03-02', crntAtrzStepCd: 'APPROVED', atrzCategory: 'trip' },
  { atrzDocId: 'ATRZ005', atrzDocTtl: '연차 사용 신청서', atrzUserId: 'user003', atrzUserNm: '최지현', atrzSbmtDt: '2026-03-01', crntAtrzStepCd: 'REJECTED', atrzCategory: 'hr' },
];

export const mockAttendance = {
  today: { workBgngDt: '2026-03-06 08:52:00', workEndDt: null, workHr: '6h 08m', lateYn: 'N', earlyYn: 'N', overtimeYn: 'N' },
  weekly: { workDays: 4, lateDays: 0, earlyDays: 0, totalWorkHr: '33h 20m', totalOvertimeHr: '2h 15m' },
  monthly: { workDays: 18, lateDays: 1, earlyDays: 0, absentDays: 0, totalWorkHr: '145h 30m', totalOvertimeHr: '12h 45m' },
};

export const mockSchedules = [
  { schdId: 'SCH001', schdTtl: '팀 주간회의', schdStrtDt: '2026-03-06 10:00', schdEndDt: '2026-03-06 11:00', type: 'personal' },
  { schdId: 'SCH002', schdTtl: '프로젝트 리뷰', schdStrtDt: '2026-03-06 14:00', schdEndDt: '2026-03-06 15:30', type: 'dept' },
  { schdId: 'SCH003', schdTtl: '거래처 미팅', schdStrtDt: '2026-03-07 09:00', schdEndDt: '2026-03-07 10:00', type: 'meeting' },
  { schdId: 'SCH004', schdTtl: '신입사원 환영회', schdStrtDt: '2026-03-07 18:00', schdEndDt: '2026-03-07 20:00', type: 'personal' },
  { schdId: 'SCH005', schdTtl: '1분기 경영보고', schdStrtDt: '2026-03-10 09:00', schdEndDt: '2026-03-10 12:00', type: 'dept' },
  { schdId: 'SCH006', schdTtl: '디자인 워크샵', schdStrtDt: '2026-03-12 13:00', schdEndDt: '2026-03-12 17:00', type: 'personal' },
];

export const mockEmails = [
  { emailContId: 'EM001', subject: '2026년 상반기 사업계획 검토 요청', userId: 'user004', senderNm: '정수진', sendDate: '2026-03-06 09:15', readYn: 'N', content: '안녕하세요, 2026년 상반기 사업계획서를 첨부드립니다. 검토 부탁드립니다.' },
  { emailContId: 'EM002', subject: 'Re: 서버 이전 일정 확인', userId: 'user001', senderNm: '이영희', sendDate: '2026-03-05 16:30', readYn: 'Y', content: '서버 이전 일정은 3월 15일로 확정되었습니다.' },
  { emailContId: 'EM003', subject: '3월 교육 프로그램 안내', userId: 'user006', senderNm: '강민지', sendDate: '2026-03-05 11:20', readYn: 'N', content: '3월 사내 교육 프로그램 일정을 안내드립니다.' },
  { emailContId: 'EM004', subject: '클라이언트 미팅 후기', userId: 'user005', senderNm: '한동훈', sendDate: '2026-03-04 17:45', readYn: 'Y', content: '오늘 진행한 클라이언트 미팅 결과를 공유합니다.' },
  { emailContId: 'EM005', subject: '디자인 시안 피드백', userId: 'user003', senderNm: '최지현', sendDate: '2026-03-04 14:10', readYn: 'Y', content: '첨부된 디자인 시안에 대한 피드백 부탁드립니다.' },
];

export const mockChatRooms = [
  { msgrId: 'MSG001', msgrNm: '개발팀 채팅방', lastMsg: '배포 완료했습니다!', lastTime: '10:30', unread: 3 },
  { msgrId: 'MSG002', msgrNm: '프로젝트 A팀', lastMsg: '회의록 공유드립니다', lastTime: '09:15', unread: 0 },
  { msgrId: 'MSG003', msgrNm: '전체 공지방', lastMsg: '금일 서버 점검 안내', lastTime: '어제', unread: 1 },
  { msgrId: 'MSG004', msgrNm: '이영희', lastMsg: '네, 확인했습니다', lastTime: '어제', unread: 0 },
];

export const mockChatMessages = [
  { msgContId: 'MC001', userId: 'user002', userNm: '박민수', contents: '오늘 배포 준비 다 됐나요?', sendDt: '10:15' },
  { msgContId: 'MC002', userId: 'user001', userNm: '이영희', contents: '네, 테스트 완료했습니다. 배포 진행해도 될 것 같습니다.', sendDt: '10:20' },
  { msgContId: 'MC003', userId: 'admin', userNm: '김관리', contents: '좋습니다. 배포 진행해주세요.', sendDt: '10:25' },
  { msgContId: 'MC004', userId: 'user002', userNm: '박민수', contents: '배포 완료했습니다!', sendDt: '10:30' },
];

export const mockProjects = [
  { bizId: 'BIZ001', bizNm: '모두의 러닝 2.0 리뉴얼', bizSttsCd: 'PROGRESS', bizPrgrs: '65', strtBizDt: '2026-01-15', endBizDt: '2026-06-30', bizPicNm: '이영희' },
  { bizId: 'BIZ002', bizNm: '모바일 앱 개발', bizSttsCd: 'PROGRESS', bizPrgrs: '30', strtBizDt: '2026-02-01', endBizDt: '2026-08-31', bizPicNm: '박민수' },
  { bizId: 'BIZ003', bizNm: '고객 포털 구축', bizSttsCd: 'PLANNING', bizPrgrs: '10', strtBizDt: '2026-03-01', endBizDt: '2026-09-30', bizPicNm: '정수진' },
  { bizId: 'BIZ004', bizNm: 'AI 챗봇 도입', bizSttsCd: 'COMPLETE', bizPrgrs: '100', strtBizDt: '2025-10-01', endBizDt: '2026-02-28', bizPicNm: '한동훈' },
];

export const mockTasks = [
  { taskId: 'T001', taskNm: 'API 엔드포인트 설계', taskSttsCd: 'COMPLETE', taskPrgrs: '100', bizId: 'BIZ001', assignee: '이영희' },
  { taskId: 'T002', taskNm: 'DB 스키마 마이그레이션', taskSttsCd: 'PROGRESS', taskPrgrs: '70', bizId: 'BIZ001', assignee: '박민수' },
  { taskId: 'T003', taskNm: 'UI 컴포넌트 개발', taskSttsCd: 'PROGRESS', taskPrgrs: '45', bizId: 'BIZ001', assignee: '최지현' },
  { taskId: 'T004', taskNm: '인증 모듈 구현', taskSttsCd: 'TODO', taskPrgrs: '0', bizId: 'BIZ001', assignee: '이영희' },
  { taskId: 'T005', taskNm: '단위 테스트 작성', taskSttsCd: 'TODO', taskPrgrs: '0', bizId: 'BIZ001', assignee: '박민수' },
  { taskId: 'T006', taskNm: '성능 최적화', taskSttsCd: 'TODO', taskPrgrs: '0', bizId: 'BIZ001', assignee: '이영희' },
];

export const mockMeetingRooms = [
  { roomId: 'ROOM001', roomName: '대회의실 A', capacity: '20', location: '3층', useYn: 'Y' },
  { roomId: 'ROOM002', roomName: '소회의실 B', capacity: '6', location: '3층', useYn: 'Y' },
  { roomId: 'ROOM003', roomName: '미팅룸 C', capacity: '4', location: '4층', useYn: 'Y' },
  { roomId: 'ROOM004', roomName: '화상 회의실', capacity: '10', location: '5층', useYn: 'Y' },
];

export const mockMeetingReservations = [
  { reservationId: 'RES001', roomId: 'ROOM001', title: '1분기 경영보고', userId: 'admin', userNm: '김관리', meetingDate: '2026-03-06', startTime: '10:00', endTime: '12:00' },
  { reservationId: 'RES002', roomId: 'ROOM002', title: '프로젝트 스프린트 회의', userId: 'user001', userNm: '이영희', meetingDate: '2026-03-06', startTime: '14:00', endTime: '15:00' },
  { reservationId: 'RES003', roomId: 'ROOM003', title: '디자인팀 브레인스토밍', userId: 'user003', userNm: '최지현', meetingDate: '2026-03-07', startTime: '09:00', endTime: '10:30' },
];

export const mockDashboard = {
  totalEmployees: 42,
  todayAttendance: 38,
  pendingApprovals: 7,
  newEmails: 12,
  attendanceRate: 90.5,
  approvalRate: 85,
};
