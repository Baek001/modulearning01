import { HttpsError, onCall } from "firebase-functions/v2/https";
import { COLLECTIONS, CallableAuth, JsonMap, asObject, assertSignedIn, db, ensureBaselineData, normalizeAttachment, nowIso, numberValue, sanitizeDocIdPart, toIso, trimmed } from "./shared.js";
import { getDirectoryUser, listDirectoryUsersData, loadProfile } from "./profile.js";
import { ensureWorkSeedData, listCommunitiesData, listFavoriteUsersFor } from "./work.js";

const COLLAB_BOOTSTRAP = "system/collabBootstrap";
const BOARDS = "boards";
const USER_SETTINGS = "dashboardSettings";
const USER_SAVED_POSTS = "savedPosts";
const USER_TODOS = "dashboardTodos";
const USER_RECOMMENDATIONS = "dashboardRecommendations";
const USER_ALARMS = "alarmLogs";

function makeId(prefix: string): string {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function isAdminRole(userRole: string): boolean {
  return userRole.includes("ADMIN");
}

function normalizeStringArray(value: unknown): string[] {
  return Array.isArray(value) ? value.map((item) => trimmed(item)).filter(Boolean) : [];
}

function normalizePerson(item: unknown, fallback: Partial<Record<string, unknown>> = {}) {
  const source = asObject(item);
  return {
    userId: trimmed(source.userId) || trimmed(fallback.userId),
    userNm: trimmed(source.userNm) || trimmed(fallback.userNm),
    deptNm: trimmed(source.deptNm) || trimmed(fallback.deptNm),
    jbgdNm: trimmed(source.jbgdNm) || trimmed(fallback.jbgdNm),
  };
}

function normalizeComment(item: unknown, index = 0) {
  const source = asObject(item);
  return {
    cmntSqn: trimmed(source.cmntSqn) || `CMNT_${index + 1}`,
    pstId: trimmed(source.pstId),
    contents: trimmed(source.contents),
    crtUserId: trimmed(source.crtUserId),
    userNm: trimmed(source.userNm),
    deptNm: trimmed(source.deptNm),
    jbgdNm: trimmed(source.jbgdNm),
    frstCrtDt: toIso(source.frstCrtDt) || nowIso(),
    upCmntSqn: trimmed(source.upCmntSqn),
    delYn: trimmed(source.delYn) || "N",
    attachments: Array.isArray(source.attachments) ? source.attachments.map((file, fileIndex) => normalizeAttachment(file, fileIndex)) : [],
  };
}

function normalizeBoardRecord(item: unknown) {
  const source = asObject(item);
  const pollSource = asObject(source.poll);
  const scheduleSource = asObject(source.schedule);
  const todoSource = asObject(source.todo);

  return {
    pstId: trimmed(source.pstId),
    bbsCtgrCd: trimmed(source.bbsCtgrCd) || "F104",
    communityId: trimmed(source.communityId),
    communityNm: trimmed(source.communityNm),
    pstTtl: trimmed(source.pstTtl),
    contents: trimmed(source.contents),
    pstTypeCd: trimmed(source.pstTypeCd) || "story",
    visibilityCd: trimmed(source.visibilityCd) || "community",
    importanceCd: trimmed(source.importanceCd) || "normal",
    linkUrl: trimmed(source.linkUrl),
    fixedYn: trimmed(source.fixedYn) || "N",
    reservedPublishDt: toIso(source.reservedPublishDt),
    publishedDt: toIso(source.publishedDt) || toIso(source.frstCrtDt) || nowIso(),
    frstCrtDt: toIso(source.frstCrtDt) || nowIso(),
    lastMdfcnDt: toIso(source.lastMdfcnDt) || toIso(source.frstCrtDt) || nowIso(),
    crtUserId: trimmed(source.crtUserId),
    userNm: trimmed(source.userNm),
    deptNm: trimmed(source.deptNm),
    jbgdNm: trimmed(source.jbgdNm),
    mentions: Array.isArray(source.mentions) ? source.mentions.map((entry) => normalizePerson(entry)).filter((entry) => entry.userId) : [],
    attachments: Array.isArray(source.attachments) ? source.attachments.map((file, index) => normalizeAttachment(file, index)) : [],
    comments: Array.isArray(source.comments) ? source.comments.map((comment, index) => normalizeComment(comment, index)) : [],
    likes: Array.isArray(source.likes) ? source.likes.map((entry) => ({
      ...normalizePerson(entry),
      crtDt: toIso(asObject(entry).crtDt) || nowIso(),
    })).filter((entry) => entry.userId) : [],
    readers: Array.isArray(source.readers) ? source.readers.map((entry) => ({
      ...normalizePerson(entry),
      readDt: toIso(asObject(entry).readDt) || nowIso(),
    })).filter((entry) => entry.userId) : [],
    shares: Array.isArray(source.shares) ? source.shares.map((entry) => ({
      userIds: normalizeStringArray(asObject(entry).userIds),
      communityIds: normalizeStringArray(asObject(entry).communityIds),
      crtUserId: trimmed(asObject(entry).crtUserId),
      createdAt: toIso(asObject(entry).createdAt) || nowIso(),
    })) : [],
    reports: Array.isArray(source.reports) ? source.reports.map((entry) => ({
      userId: trimmed(asObject(entry).userId),
      userNm: trimmed(asObject(entry).userNm),
      reasonText: trimmed(asObject(entry).reasonText),
      createdAt: toIso(asObject(entry).createdAt) || nowIso(),
    })) : [],
    poll: source.poll ? {
      multipleYn: trimmed(pollSource.multipleYn) || "N",
      anonymousYn: trimmed(pollSource.anonymousYn) || "N",
      resultOpenYn: trimmed(pollSource.resultOpenYn) || "Y",
      participantOpenYn: trimmed(pollSource.participantOpenYn) || "Y",
      deadlineDt: toIso(pollSource.deadlineDt),
      options: Array.isArray(pollSource.options) ? pollSource.options.map((option, index) => ({
        optionId: trimmed(asObject(option).optionId) || `OPT_${index + 1}`,
        optionText: trimmed(asObject(option).optionText) || `옵션 ${index + 1}`,
        voteUserIds: normalizeStringArray(asObject(option).voteUserIds),
      })) : [],
    } : null,
    schedule: source.schedule ? {
      startDt: toIso(scheduleSource.startDt),
      endDt: toIso(scheduleSource.endDt),
      repeatRule: trimmed(scheduleSource.repeatRule),
      placeText: trimmed(scheduleSource.placeText),
      placeUrl: trimmed(scheduleSource.placeUrl),
      reminderMinutes: numberValue(scheduleSource.reminderMinutes, 30),
      videoMeetingYn: trimmed(scheduleSource.videoMeetingYn) || "N",
      meetingRoomId: trimmed(scheduleSource.meetingRoomId),
      attendees: Array.isArray(scheduleSource.attendees) ? scheduleSource.attendees.map((attendee) => ({
        ...normalizePerson(attendee),
        attendanceSttsCd: trimmed(asObject(attendee).attendanceSttsCd) || "invited",
      })).filter((attendee) => attendee.userId) : [],
    } : null,
    todo: source.todo ? {
      dueDt: toIso(todoSource.dueDt),
      assignees: Array.isArray(todoSource.assignees) ? todoSource.assignees.map((assignee) => ({
        ...normalizePerson(assignee),
        statusCd: trimmed(asObject(assignee).statusCd) || "requested",
      })).filter((assignee) => assignee.userId) : [],
    } : null,
    viewCnt: numberValue(source.viewCnt, 0),
  };
}

async function ensureCollabSeedData(): Promise<void> {
  await ensureBaselineData();
  await ensureWorkSeedData();

  const bootstrapRef = db.doc(COLLAB_BOOTSTRAP);
  const snapshot = await bootstrapRef.get();
  if (snapshot.exists) {
    return;
  }

  const [users, communities] = await Promise.all([listDirectoryUsersData(), listCommunitiesData()]);
  const admin = users.find((item) => item.userId === "admin") || users[0];
  const memberOne = users.find((item) => item.userId === "user01") || users[0];
  const memberTwo = users.find((item) => item.userId === "user02") || users[1] || users[0];
  const primaryCommunity = communities[0];
  const secondaryCommunity = communities[1] || communities[0];
  const seededAt = nowIso();

  const boards = [
    {
      pstId: "PST_NOTICE_FIREBASE",
      bbsCtgrCd: "F101",
      communityId: "",
      communityNm: "",
      pstTtl: "Firebase 전환 공지",
      contents: "<p>기존 업무 화면을 Firebase 기반으로 단계 전환하고 있습니다.</p><p>중요 기능은 순차적으로 동일한 응답 구조를 유지한 채 이전합니다.</p>",
      pstTypeCd: "story",
      visibilityCd: "community",
      importanceCd: "notice",
      linkUrl: "",
      fixedYn: "Y",
      reservedPublishDt: "",
      publishedDt: seededAt,
      frstCrtDt: seededAt,
      lastMdfcnDt: seededAt,
      crtUserId: admin?.userId || "admin",
      userNm: admin?.userNm || "관리자",
      deptNm: admin?.deptNm || "",
      jbgdNm: admin?.jbgdNm || "",
      mentions: [],
      attachments: [],
      comments: [
        {
          cmntSqn: "CMNT_NOTICE_1",
          pstId: "PST_NOTICE_FIREBASE",
          contents: "테스트 환경에서도 동일하게 확인하고 있습니다.",
          crtUserId: memberOne?.userId || "user01",
          userNm: memberOne?.userNm || "테스트 사용자",
          deptNm: memberOne?.deptNm || "",
          jbgdNm: memberOne?.jbgdNm || "",
          frstCrtDt: seededAt,
          upCmntSqn: "",
          delYn: "N",
          attachments: [],
        },
      ],
      likes: [],
      readers: [],
      shares: [],
      reports: [],
      poll: null,
      schedule: null,
      todo: null,
      viewCnt: 3,
    },
    {
      pstId: "PST_STORY_TEAM",
      bbsCtgrCd: "F104",
      communityId: primaryCommunity?.communityId || "",
      communityNm: primaryCommunity?.communityNm || "",
      pstTtl: "이번 주 개발 공유",
      contents: "<p>이번 주에는 대시보드, 게시판, 알림 영역을 우선 이전합니다.</p><p>실시간 메신저는 다음 단계에서 이어집니다.</p>",
      pstTypeCd: "story",
      visibilityCd: "community",
      importanceCd: "important",
      linkUrl: "",
      fixedYn: "N",
      reservedPublishDt: "",
      publishedDt: seededAt,
      frstCrtDt: seededAt,
      lastMdfcnDt: seededAt,
      crtUserId: memberOne?.userId || "user01",
      userNm: memberOne?.userNm || "테스트 사용자",
      deptNm: memberOne?.deptNm || "",
      jbgdNm: memberOne?.jbgdNm || "",
      mentions: [normalizePerson(memberTwo)],
      attachments: [],
      comments: [],
      likes: [],
      readers: [],
      shares: [],
      reports: [],
      poll: null,
      schedule: null,
      todo: null,
      viewCnt: 1,
    },
    {
      pstId: "PST_POLL_FRONT",
      bbsCtgrCd: "F102",
      communityId: secondaryCommunity?.communityId || primaryCommunity?.communityId || "",
      communityNm: secondaryCommunity?.communityNm || primaryCommunity?.communityNm || "",
      pstTtl: "UI 리팩터링 우선순위 설문",
      contents: "<p>다음 작업에서 어떤 메뉴를 먼저 다듬을지 의견을 받아 봅니다.</p>",
      pstTypeCd: "poll",
      visibilityCd: "community",
      importanceCd: "normal",
      linkUrl: "",
      fixedYn: "N",
      reservedPublishDt: "",
      publishedDt: seededAt,
      frstCrtDt: seededAt,
      lastMdfcnDt: seededAt,
      crtUserId: memberTwo?.userId || "user02",
      userNm: memberTwo?.userNm || "테스트 사용자2",
      deptNm: memberTwo?.deptNm || "",
      jbgdNm: memberTwo?.jbgdNm || "",
      mentions: [],
      attachments: [],
      comments: [],
      likes: [],
      readers: [],
      shares: [],
      reports: [],
      poll: {
        multipleYn: "N",
        anonymousYn: "N",
        resultOpenYn: "Y",
        participantOpenYn: "Y",
        deadlineDt: new Date(Date.now() + 1000 * 60 * 60 * 48).toISOString(),
        options: [
          { optionId: "OPT_BOARD", optionText: "게시판", voteUserIds: [] },
          { optionId: "OPT_EMAIL", optionText: "메일", voteUserIds: [] },
          { optionId: "OPT_CONTRACT", optionText: "전자계약", voteUserIds: [] },
        ],
      },
      schedule: null,
      todo: null,
      viewCnt: 0,
    },
    {
      pstId: "PST_SCHEDULE_SYNC",
      bbsCtgrCd: "F104",
      communityId: primaryCommunity?.communityId || "",
      communityNm: primaryCommunity?.communityNm || "",
      pstTtl: "배포 동기화 일정",
      contents: "<p>전환 직전 최종 점검 일정을 공유합니다.</p>",
      pstTypeCd: "schedule",
      visibilityCd: "community",
      importanceCd: "important",
      linkUrl: "",
      fixedYn: "N",
      reservedPublishDt: "",
      publishedDt: seededAt,
      frstCrtDt: seededAt,
      lastMdfcnDt: seededAt,
      crtUserId: admin?.userId || "admin",
      userNm: admin?.userNm || "관리자",
      deptNm: admin?.deptNm || "",
      jbgdNm: admin?.jbgdNm || "",
      mentions: [],
      attachments: [],
      comments: [],
      likes: [],
      readers: [],
      shares: [],
      reports: [],
      poll: null,
      schedule: {
        startDt: new Date(Date.now() + 1000 * 60 * 60 * 24).toISOString(),
        endDt: new Date(Date.now() + 1000 * 60 * 60 * 26).toISOString(),
        repeatRule: "",
        placeText: "온라인 회의",
        placeUrl: "",
        reminderMinutes: 30,
        videoMeetingYn: "N",
        meetingRoomId: "",
        attendees: [normalizePerson(admin), normalizePerson(memberOne), normalizePerson(memberTwo)].map((person) => ({
          ...person,
          attendanceSttsCd: "invited",
        })),
      },
      todo: null,
      viewCnt: 0,
    },
    {
      pstId: "PST_TODO_MIGRATION",
      bbsCtgrCd: "F106",
      communityId: primaryCommunity?.communityId || "",
      communityNm: primaryCommunity?.communityNm || "",
      pstTtl: "마이그레이션 검수 체크",
      contents: "<p>남은 기능 전환 전에 체크할 검수 목록입니다.</p>",
      pstTypeCd: "todo",
      visibilityCd: "community",
      importanceCd: "urgent",
      linkUrl: "",
      fixedYn: "N",
      reservedPublishDt: "",
      publishedDt: seededAt,
      frstCrtDt: seededAt,
      lastMdfcnDt: seededAt,
      crtUserId: admin?.userId || "admin",
      userNm: admin?.userNm || "관리자",
      deptNm: admin?.deptNm || "",
      jbgdNm: admin?.jbgdNm || "",
      mentions: [],
      attachments: [],
      comments: [],
      likes: [],
      readers: [],
      shares: [],
      reports: [],
      poll: null,
      schedule: null,
      todo: {
        dueDt: new Date(Date.now() + 1000 * 60 * 60 * 72).toISOString(),
        assignees: [
          { ...normalizePerson(memberOne), statusCd: "requested" },
          { ...normalizePerson(memberTwo), statusCd: "in_progress" },
        ],
      },
      viewCnt: 0,
    },
  ];

  const batch = db.batch();
  boards.forEach((board) => {
    batch.set(db.collection(BOARDS).doc(board.pstId), board);
  });
  batch.set(bootstrapRef, { seedVersion: 1, seededAt });
  await batch.commit();
}

async function ensureUserDashboardState(userId: string): Promise<void> {
  await ensureCollabSeedData();
  const prefRef = db.collection(COLLECTIONS.sessionUsers).doc(userId).collection(USER_SETTINGS).doc("preferences");
  const prefSnapshot = await prefRef.get();
  if (!prefSnapshot.exists) {
    await prefRef.set({
      defaultScope: "all",
      defaultSort: "recent",
      defaultView: "summary",
      defaultCategory: "all",
      lastDeptId: "",
      lastSearchQ: "",
      categories: ["F104", "F102"],
      updatedAt: nowIso(),
    });
  }

  const alarmSnapshot = await db.collection(COLLECTIONS.sessionUsers).doc(userId).collection(USER_ALARMS).limit(1).get();
  if (alarmSnapshot.empty) {
    const profile = await getDirectoryUser(userId);
    const seededAt = nowIso();
    const alarms = [
      {
        alarmId: makeId("alarm"),
        alarmCategory: "전자결재",
        alarmMessage: `${profile?.userNm || userId}님, 테스트 결재 문서가 도착했습니다.`,
        relatedUrl: "/approval",
        readYn: "N",
        createdDt: seededAt,
      },
      {
        alarmId: makeId("alarm"),
        alarmCategory: "게시판",
        alarmMessage: "새 공지와 일정 공유 글이 등록되었습니다.",
        relatedUrl: "/board",
        readYn: "N",
        createdDt: new Date(Date.now() - 1000 * 60 * 20).toISOString(),
      },
      {
        alarmId: makeId("alarm"),
        alarmCategory: "프로젝트",
        alarmMessage: "담당 업무 상태를 확인해 주세요.",
        relatedUrl: "/project",
        readYn: "Y",
        createdDt: new Date(Date.now() - 1000 * 60 * 60 * 2).toISOString(),
      },
    ];
    const batch = db.batch();
    alarms.forEach((alarm) => {
      batch.set(db.collection(COLLECTIONS.sessionUsers).doc(userId).collection(USER_ALARMS).doc(alarm.alarmId), alarm);
    });
    await batch.commit();
  }
}

async function listBoardRecords() {
  await ensureCollabSeedData();
  const snapshot = await db.collection(BOARDS).get();
  return snapshot.docs.map((doc) => normalizeBoardRecord(doc.data()));
}

async function getBoardRecordOrThrow(pstId: string) {
  await ensureCollabSeedData();
  const snapshot = await db.collection(BOARDS).doc(pstId).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "게시글을 찾을 수 없습니다.");
  }
  return normalizeBoardRecord(snapshot.data());
}

async function getCommunityMap() {
  const items = await listCommunitiesData();
  return new Map(items.map((item) => [item.communityId, item]));
}

async function getDepartmentList() {
  await ensureBaselineData();
  const snapshot = await db.collection(COLLECTIONS.departments).get();
  return snapshot.docs
    .map((doc) => asObject(doc.data()))
    .map((item) => ({
      deptId: trimmed(item.deptId),
      deptNm: trimmed(item.deptNm),
    }))
    .filter((item) => item.deptId)
    .sort((left, right) => left.deptNm.localeCompare(right.deptNm, "ko"));
}

async function getSavedPostIds(userId: string) {
  const snapshot = await db.collection(COLLECTIONS.sessionUsers).doc(userId).collection(USER_SAVED_POSTS).get();
  return new Set(snapshot.docs.map((doc) => doc.id));
}

async function getDashboardPreferences(userId: string) {
  await ensureUserDashboardState(userId);
  const snapshot = await db.collection(COLLECTIONS.sessionUsers).doc(userId).collection(USER_SETTINGS).doc("preferences").get();
  const source = asObject(snapshot.data());
  return {
    defaultScope: trimmed(source.defaultScope) || "all",
    defaultSort: trimmed(source.defaultSort) || "recent",
    defaultView: trimmed(source.defaultView) || "summary",
    defaultCategory: trimmed(source.defaultCategory) || "all",
    lastDeptId: trimmed(source.lastDeptId),
    lastSearchQ: trimmed(source.lastSearchQ),
    categories: normalizeStringArray(source.categories),
    updatedAt: toIso(source.updatedAt) || nowIso(),
  };
}

async function setDashboardPreferences(userId: string, payload: Record<string, unknown>) {
  const current = await getDashboardPreferences(userId);
  const next = {
    ...current,
    ...payload,
    categories: Array.isArray(payload.categories) ? normalizeStringArray(payload.categories) : current.categories,
    updatedAt: nowIso(),
  };
  await db.collection(COLLECTIONS.sessionUsers).doc(userId).collection(USER_SETTINGS).doc("preferences").set(next, { merge: true });
  return next;
}

async function listDashboardTodos(userId: string) {
  const snapshot = await db.collection(COLLECTIONS.sessionUsers).doc(userId).collection(USER_TODOS).get();
  return snapshot.docs
    .map((doc) => {
      const item = asObject(doc.data());
      return {
        todoId: doc.id,
        targetUserId: trimmed(item.targetUserId),
        targetUserName: trimmed(item.targetUserName),
        todoTtl: trimmed(item.todoTtl),
        todoCn: trimmed(item.todoCn),
        dueDt: toIso(item.dueDt),
        createdAt: toIso(item.createdAt) || nowIso(),
      };
    })
    .sort((left, right) => (left.dueDt || left.createdAt).localeCompare(right.dueDt || right.createdAt));
}

async function listRecommendations(userId: string, box = "inbox") {
  const snapshot = await db.collection(COLLECTIONS.sessionUsers).doc(userId).collection(USER_RECOMMENDATIONS).get();
  const items = snapshot.docs.map((doc) => {
    const item = asObject(doc.data());
    return {
      recommendId: doc.id,
      targetUserId: trimmed(item.targetUserId),
      fromUserId: trimmed(item.fromUserId),
      fromUserName: trimmed(item.fromUserName),
      fromDeptName: trimmed(item.fromDeptName),
      categoryCode: trimmed(item.categoryCode),
      categoryLabel: trimmed(item.categoryLabel),
      message: trimmed(item.message),
      acceptedYn: trimmed(item.acceptedYn) || "N",
      readYn: trimmed(item.readYn) || "N",
      createdAt: toIso(item.createdAt) || nowIso(),
    };
  });

  return items
    .filter((item) => box === "outbox" ? item.fromUserId === userId : item.targetUserId === userId)
    .sort((left, right) => right.createdAt.localeCompare(left.createdAt));
}

async function createAlarm(userId: string, payload: Record<string, unknown>) {
  const alarmId = makeId("alarm");
  const alarm = {
    alarmId,
    alarmCategory: trimmed(payload.alarmCategory) || "알림",
    alarmMessage: trimmed(payload.alarmMessage) || "새 알림이 도착했습니다.",
    relatedUrl: trimmed(payload.relatedUrl) || "#",
    readYn: trimmed(payload.readYn) || "N",
    createdDt: toIso(payload.createdDt) || nowIso(),
  };
  await db.collection(COLLECTIONS.sessionUsers).doc(userId).collection(USER_ALARMS).doc(alarmId).set(alarm);
  return alarm;
}

function matchesKeyword(target: string, keyword: string) {
  const normalized = keyword.trim().toLowerCase();
  if (!normalized) {
    return true;
  }
  return target.toLowerCase().includes(normalized);
}

function summarizeBoardForList(board: ReturnType<typeof normalizeBoardRecord>, savedSet: Set<string>) {
  const comments = board.comments.filter((comment) => comment.delYn !== "Y");
  return {
    pstId: board.pstId,
    bbsCtgrCd: board.bbsCtgrCd,
    communityId: board.communityId,
    communityNm: board.communityNm,
    pstTtl: board.pstTtl,
    contents: board.contents,
    pstTypeCd: board.pstTypeCd,
    visibilityCd: board.visibilityCd,
    importanceCd: board.importanceCd,
    fixedYn: board.fixedYn,
    linkUrl: board.linkUrl,
    publishedDt: board.publishedDt,
    frstCrtDt: board.frstCrtDt,
    userNm: board.userNm,
    crtUserId: board.crtUserId,
    deptNm: board.deptNm,
    commentCount: comments.length,
    likeCount: board.likes.length,
    readerCount: board.readers.length,
    viewCnt: board.viewCnt,
    saved: savedSet.has(board.pstId),
  };
}

function buildBoardDetail(board: ReturnType<typeof normalizeBoardRecord>, currentUserId: string, userRole: string) {
  const isAdmin = isAdminRole(userRole);
  const comments = board.comments
    .filter((comment) => comment.delYn !== "Y")
    .sort((left, right) => left.frstCrtDt.localeCompare(right.frstCrtDt));
  const liked = board.likes.some((entry) => entry.userId === currentUserId);
  const poll = board.poll
    ? {
      ...board.poll,
      closed: board.poll.deadlineDt ? new Date(board.poll.deadlineDt).getTime() < Date.now() : false,
      options: board.poll.options.map((option) => ({
        optionId: option.optionId,
        optionText: option.optionText,
        voteCount: option.voteUserIds.length,
        selected: option.voteUserIds.includes(currentUserId),
      })),
    }
    : null;
  const todo = board.todo
    ? {
      ...board.todo,
      totalCount: board.todo.assignees.length,
      doneCount: board.todo.assignees.filter((assignee) => assignee.statusCd === "done").length,
    }
    : null;

  return {
    ...board,
    saved: false,
    liked,
    likeCount: board.likes.length,
    readerCount: board.readers.length,
    commentCount: comments.length,
    shareable: true,
    pinnable: isAdmin || board.crtUserId === currentUserId,
    manageable: isAdmin || board.crtUserId === currentUserId,
    comments,
    poll,
    todo,
  };
}

async function buildBoardDetailForUser(pstId: string, currentUserId: string, userRole: string) {
  const [board, savedSet] = await Promise.all([getBoardRecordOrThrow(pstId), getSavedPostIds(currentUserId)]);
  const detail = buildBoardDetail(board, currentUserId, userRole);
  return {
    ...detail,
    saved: savedSet.has(detail.pstId),
  };
}

function boardSearchTarget(board: ReturnType<typeof normalizeBoardRecord>, searchType: string) {
  const base = {
    title: board.pstTtl,
    content: board.contents,
    writer: [board.userNm, board.crtUserId, board.deptNm].join(" "),
    comment: board.comments.map((comment) => comment.contents).join(" "),
  };

  switch (searchType) {
    case "title":
      return base.title;
    case "content":
      return base.content;
    case "writer":
      return base.writer;
    case "comment":
      return base.comment;
    default:
      return Object.values(base).join(" ");
  }
}

function boardDeadlineValue(board: ReturnType<typeof normalizeBoardRecord>) {
  if (board.pstTypeCd === "poll") {
    return board.poll?.deadlineDt || "9999-12-31T23:59:59.999Z";
  }
  if (board.pstTypeCd === "schedule") {
    return board.schedule?.startDt || "9999-12-31T23:59:59.999Z";
  }
  if (board.pstTypeCd === "todo") {
    return board.todo?.dueDt || "9999-12-31T23:59:59.999Z";
  }
  return "9999-12-31T23:59:59.999Z";
}

async function getBoardWorkspace(userId: string, userRole: string, params: JsonMap) {
  const [boards, savedSet] = await Promise.all([listBoardRecords(), getSavedPostIds(userId)]);
  const scope = trimmed(params.scope) || "all";
  const searchType = trimmed(params.searchType) || "all";
  const keyword = trimmed(params.q);
  const type = trimmed(params.type);
  const communityId = trimmed(params.communityId);
  const importance = trimmed(params.importance);
  const sort = trimmed(params.sort) || "recent";
  const page = Math.max(1, numberValue(params.page, 1));

  const filtered = boards
    .filter((board) => !type || type === "all" || board.pstTypeCd === type)
    .filter((board) => !communityId || communityId === "all" || String(board.communityId) === String(communityId))
    .filter((board) => !importance || importance === "all" || board.importanceCd === importance)
    .filter((board) => matchesKeyword(boardSearchTarget(board, searchType), keyword))
    .filter((board) => {
      if (scope === "saved") return savedSet.has(board.pstId);
      if (scope === "mine") return board.crtUserId === userId;
      if (scope === "shared") return board.shares.some((entry) => entry.userIds.includes(userId));
      if (scope === "scheduled" || scope === "schedule") return board.pstTypeCd === "schedule";
      if (scope === "pinned") return board.fixedYn === "Y";
      if (scope === "poll") return board.pstTypeCd === "poll";
      if (scope === "todo") return board.pstTypeCd === "todo";
      return true;
    })
    .sort((left, right) => {
      if (sort === "popular") {
        return right.viewCnt - left.viewCnt || right.publishedDt.localeCompare(left.publishedDt);
      }
      if (sort === "liked") {
        return right.likes.length - left.likes.length || right.publishedDt.localeCompare(left.publishedDt);
      }
      if (sort === "deadline") {
        return boardDeadlineValue(left).localeCompare(boardDeadlineValue(right));
      }
      return right.publishedDt.localeCompare(left.publishedDt);
    });

  const pageSize = 10;
  const startIndex = (page - 1) * pageSize;
  const items = filtered.slice(startIndex, startIndex + pageSize).map((board) => summarizeBoardForList(board, savedSet));

  return {
    items,
    summary: {
      totalCount: filtered.length,
    },
    pinnedItems: filtered.filter((board) => board.fixedYn === "Y").slice(0, 5).map((board) => summarizeBoardForList(board, savedSet)),
    closingPolls: filtered.filter((board) => board.pstTypeCd === "poll").slice(0, 5).map((board) => summarizeBoardForList(board, savedSet)),
    todoItems: filtered.filter((board) => board.pstTypeCd === "todo" && board.todo?.assignees.some((assignee) => assignee.userId === userId)).slice(0, 5).map((board) => summarizeBoardForList(board, savedSet)),
    page,
    totalPages: Math.max(1, Math.ceil(filtered.length / pageSize)),
  };
}

async function markBoardAsReadForUser(userId: string, pstId: string) {
  const board = await getBoardRecordOrThrow(pstId);
  if (board.readers.some((entry) => entry.userId === userId)) {
    return board;
  }

  const profile = await getDirectoryUser(userId);
  const next = {
    ...board,
    readers: [
      ...board.readers,
      {
        userId,
        userNm: profile?.userNm || userId,
        deptNm: profile?.deptNm || "",
        jbgdNm: profile?.jbgdNm || "",
        readDt: nowIso(),
      },
    ],
  };
  await db.collection(BOARDS).doc(board.pstId).set(next, { merge: true });
  return next;
}

async function incrementBoardView(pstId: string) {
  const board = await getBoardRecordOrThrow(pstId);
  const next = {
    ...board,
    viewCnt: board.viewCnt + 1,
    lastMdfcnDt: nowIso(),
  };
  await db.collection(BOARDS).doc(board.pstId).set(next, { merge: true });
  return next;
}

async function saveBoardRecord(board: Record<string, unknown>) {
  await db.collection(BOARDS).doc(trimmed(board.pstId)).set(board, { merge: true });
}

async function buildMentionProfiles(userIds: string[]) {
  const uniqueUserIds = [...new Set(userIds.filter(Boolean))];
  const users = await Promise.all(uniqueUserIds.map(async (userId) => normalizePerson(await getDirectoryUser(userId) || { userId, userNm: userId })));
  return users.filter((user) => user.userId);
}

async function buildTodoAssignees(items: unknown[]) {
  const assignees = Array.isArray(items) ? items : [];
  return Promise.all(assignees.map(async (assignee) => {
    const source = asObject(assignee);
    const userId = trimmed(source.userId);
    const profile = await getDirectoryUser(userId);
    return {
      ...normalizePerson(profile || source || { userId }),
      statusCd: trimmed(source.statusCd) || "requested",
    };
  }));
}

async function buildScheduleAttendees(items: unknown[]) {
  const attendees = Array.isArray(items) ? items : [];
  return Promise.all(attendees.map(async (attendee) => {
    const source = asObject(attendee);
    const userId = trimmed(source.userId);
    const profile = await getDirectoryUser(userId);
    return {
      ...normalizePerson(profile || source || { userId }),
      attendanceSttsCd: trimmed(source.attendanceSttsCd) || "invited",
    };
  }));
}

async function createOrUpdateBoard(currentUserId: string, userRole: string, payload: JsonMap, attachments: unknown[], existingBoard?: ReturnType<typeof normalizeBoardRecord>) {
  const currentUser = await getDirectoryUser(currentUserId);
  if (!currentUser) {
    throw new HttpsError("not-found", "사용자 정보를 찾을 수 없습니다.");
  }

  const communityMap = await getCommunityMap();
  const communityId = trimmed(payload.communityId ? String(payload.communityId) : "");
  const community = communityMap.get(communityId);
  const type = trimmed(payload.pstTypeCd) || existingBoard?.pstTypeCd || "story";
  const createdAt = existingBoard?.frstCrtDt || nowIso();
  const mentionProfiles = await buildMentionProfiles(normalizeStringArray(payload.mentionUserIds));
  const nextAttachments = [
    ...(existingBoard?.attachments || []),
    ...((Array.isArray(attachments) ? attachments : []).map((file, index) => normalizeAttachment(file, (existingBoard?.attachments?.length || 0) + index))),
  ];
  const nextBoard: Record<string, unknown> = {
    pstId: existingBoard?.pstId || makeId("pst"),
    bbsCtgrCd: trimmed(payload.bbsCtgrCd) || existingBoard?.bbsCtgrCd || "F104",
    communityId,
    communityNm: community?.communityNm || existingBoard?.communityNm || "",
    pstTtl: trimmed(payload.pstTtl) || existingBoard?.pstTtl || "",
    contents: trimmed(payload.contents) || existingBoard?.contents || "",
    pstTypeCd: type,
    visibilityCd: trimmed(payload.visibilityCd) || existingBoard?.visibilityCd || "community",
    importanceCd: trimmed(payload.importanceCd) || existingBoard?.importanceCd || "normal",
    linkUrl: trimmed(payload.linkUrl) || existingBoard?.linkUrl || "",
    fixedYn: trimmed(payload.fixedYn) || existingBoard?.fixedYn || "N",
    reservedPublishDt: toIso(payload.reservedPublishDt) || existingBoard?.reservedPublishDt || "",
    publishedDt: toIso(payload.reservedPublishDt) || existingBoard?.publishedDt || nowIso(),
    frstCrtDt: createdAt,
    lastMdfcnDt: nowIso(),
    crtUserId: existingBoard?.crtUserId || currentUser.userId,
    userNm: existingBoard?.userNm || currentUser.userNm,
    deptNm: existingBoard?.deptNm || currentUser.deptNm,
    jbgdNm: existingBoard?.jbgdNm || currentUser.jbgdNm,
    mentions: mentionProfiles,
    attachments: nextAttachments,
    comments: existingBoard?.comments || [],
    likes: existingBoard?.likes || [],
    readers: existingBoard?.readers || [],
    shares: existingBoard?.shares || [],
    reports: existingBoard?.reports || [],
    poll: null,
    schedule: null,
    todo: null,
    viewCnt: existingBoard?.viewCnt || 0,
  };

  if (type === "poll") {
    const poll = asObject(payload.poll);
    const nextOptions = Array.isArray(payload.pollOptions) ? payload.pollOptions : [];
    nextBoard.poll = {
      multipleYn: trimmed(poll.multipleYn) || "N",
      anonymousYn: trimmed(poll.anonymousYn) || "N",
      resultOpenYn: trimmed(poll.resultOpenYn) || "Y",
      participantOpenYn: trimmed(poll.participantOpenYn) || "Y",
      deadlineDt: toIso(poll.deadlineDt),
      options: nextOptions.map((option, index) => {
        const optionSource = asObject(option);
        const existingOption = existingBoard?.poll?.options?.find((item) => item.optionId === trimmed(optionSource.optionId) || item.optionText === trimmed(optionSource.optionText));
        return {
          optionId: trimmed(optionSource.optionId) || existingOption?.optionId || makeId(`opt_${index + 1}`),
          optionText: trimmed(optionSource.optionText) || existingOption?.optionText || `옵션 ${index + 1}`,
          voteUserIds: existingOption?.voteUserIds || [],
        };
      }).filter((option) => option.optionText),
    };
  }

  if (type === "schedule") {
    const schedule = asObject(payload.schedule);
    nextBoard.schedule = {
      startDt: toIso(schedule.startDt),
      endDt: toIso(schedule.endDt),
      repeatRule: trimmed(schedule.repeatRule),
      placeText: trimmed(schedule.placeText),
      placeUrl: trimmed(schedule.placeUrl),
      reminderMinutes: numberValue(schedule.reminderMinutes, 30),
      videoMeetingYn: trimmed(schedule.videoMeetingYn) || "N",
      meetingRoomId: trimmed(schedule.meetingRoomId),
      attendees: await buildScheduleAttendees(Array.isArray(schedule.attendees) ? schedule.attendees : []),
    };
  }

  if (type === "todo") {
    const todo = asObject(payload.todo);
    nextBoard.todo = {
      dueDt: toIso(todo.dueDt),
      assignees: await buildTodoAssignees(Array.isArray(todo.assignees) ? todo.assignees : []),
    };
  }

  if (!existingBoard && nextBoard.fixedYn === "Y" && !isAdminRole(userRole)) {
    nextBoard.fixedYn = "N";
  }

  await saveBoardRecord(nextBoard);
  return normalizeBoardRecord(nextBoard);
}

function toFeedItem(board: ReturnType<typeof normalizeBoardRecord>, currentUserId: string, savedSet: Set<string>) {
  const commentCount = board.comments.filter((comment) => comment.delYn !== "Y").length;
  const isRead = board.readers.some((reader) => reader.userId === currentUserId);
  return {
    feedId: board.pstId,
    itemType: "board",
    badge: board.fixedYn === "Y" ? "공지" : board.pstTypeCd === "poll" ? "설문" : board.pstTypeCd === "schedule" ? "일정" : board.pstTypeCd === "todo" ? "할일" : "게시글",
    categoryLabel: board.communityNm || board.bbsCtgrCd,
    actorUserId: board.crtUserId,
    actorName: board.userNm,
    actorDeptName: board.deptNm,
    actorJobGradeName: board.jbgdNm,
    createdAt: board.publishedDt || board.frstCrtDt,
    title: board.pstTtl,
    bodyPreview: board.contents.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim(),
    read: isRead,
    commentCount,
    viewCount: board.viewCnt,
    saved: savedSet.has(board.pstId),
    categoryCode: board.bbsCtgrCd,
    fixedYn: board.fixedYn,
  };
}

async function buildDashboardBootstrap(userId: string) {
  const [preferences, boards, favoriteUsers, todos, recommendations, savedSet] = await Promise.all([
    getDashboardPreferences(userId),
    listBoardRecords(),
    listFavoriteUsersFor(userId),
    listDashboardTodos(userId),
    listRecommendations(userId, "inbox"),
    getSavedPostIds(userId),
  ]);

  const notices = boards
    .filter((board) => board.fixedYn === "Y" || board.bbsCtgrCd === "F101")
    .sort((left, right) => right.publishedDt.localeCompare(left.publishedDt))
    .slice(0, 5)
    .map((board) => ({
      itemType: "board",
      title: board.pstTtl,
      subtitle: board.userNm,
      createdAt: board.publishedDt || board.frstCrtDt,
      route: `/board?postId=${board.pstId}`,
    }));

  const sharedSchedules = boards
    .filter((board) => board.pstTypeCd === "schedule")
    .sort((left, right) => boardDeadlineValue(left).localeCompare(boardDeadlineValue(right)))
    .slice(0, 4)
    .map((board) => ({
      title: board.pstTtl,
      subtitle: board.schedule?.startDt ? board.schedule.startDt.slice(0, 16).replace("T", " ") : "",
      createdAt: board.schedule?.startDt || board.publishedDt,
      route: "/calendar",
    }));

  const mySchedules = boards
    .filter((board) => board.pstTypeCd === "schedule" && (board.crtUserId === userId || board.schedule?.attendees.some((attendee) => attendee.userId === userId)))
    .sort((left, right) => boardDeadlineValue(left).localeCompare(boardDeadlineValue(right)))
    .slice(0, 4)
    .map((board) => ({
      title: board.pstTtl,
      subtitle: board.communityNm || board.userNm,
      createdAt: board.schedule?.startDt || board.publishedDt,
      route: "/calendar",
    }));

  const quickLinks = [
    { title: "전자결재", route: "/approval" },
    { title: "게시판", route: "/board" },
    { title: "메신저", route: "/messenger" },
    { title: "프로젝트", route: "/project" },
    { title: "메일", route: "/email" },
  ];

  return {
    preferences,
    categories: preferences.categories,
    widgets: {
      notices,
      sharedSchedules,
      mySchedules,
      quickLinks,
      favoriteUsers,
      todoItems: boards
        .filter((board) => board.pstTypeCd === "todo" && board.todo?.assignees.some((assignee) => assignee.userId === userId))
        .slice(0, 4)
        .map((board) => summarizeBoardForList(board, savedSet)),
    },
    todos,
    recommendations: {
      items: recommendations,
    },
  };
}

async function buildDashboardFeed(userId: string, params: JsonMap) {
  const [boards, preferences, savedSet, todos, recommendations, departments] = await Promise.all([
    listBoardRecords(),
    getDashboardPreferences(userId),
    getSavedPostIds(userId),
    listDashboardTodos(userId),
    listRecommendations(userId, "inbox"),
    getDepartmentList(),
  ]);
  const scope = trimmed(params.scope) || preferences.defaultScope || "all";
  const category = trimmed(params.category) || preferences.defaultCategory || "all";
  const deptId = trimmed(params.deptId) || preferences.lastDeptId || "";
  const keyword = trimmed(params.q) || preferences.lastSearchQ || "";
  const sort = trimmed(params.sort) || preferences.defaultSort || "recent";
  const page = Math.max(1, numberValue(params.page, 1));

  const boardItems = boards
    .filter((board) => category === "all" || board.bbsCtgrCd === category)
    .filter((board) => !deptId || board.deptNm === departments.find((dept) => dept.deptId === deptId)?.deptNm)
    .filter((board) => matchesKeyword(`${board.pstTtl} ${board.contents} ${board.userNm} ${board.deptNm}`, keyword))
    .map((board) => toFeedItem(board, userId, savedSet));

  const activityItems = [
    ...todos.map((todo) => ({
      feedId: `todo_${todo.todoId}`,
      itemType: "activity",
      badge: "할 일",
      categoryLabel: "할 일",
      actorUserId: userId,
      actorName: todo.targetUserName || "개인 할 일",
      actorDeptName: "",
      actorJobGradeName: "",
      createdAt: todo.createdAt,
      title: todo.todoTtl,
      bodyPreview: todo.todoCn || "개인 메모",
      route: "/",
      read: true,
      commentCount: 0,
      viewCount: 0,
      saved: false,
      categoryCode: "activity",
      fixedYn: "N",
    })),
    ...recommendations.map((recommendation) => ({
      feedId: `recommend_${recommendation.recommendId}`,
      itemType: "activity",
      badge: "추천",
      categoryLabel: "추천",
      actorUserId: recommendation.fromUserId,
      actorName: recommendation.fromUserName,
      actorDeptName: recommendation.fromDeptName,
      actorJobGradeName: "",
      createdAt: recommendation.createdAt,
      title: `${recommendation.categoryLabel} 추천이 도착했습니다.`,
      bodyPreview: recommendation.message || "추천 메시지를 확인해 보세요.",
      route: "/",
      read: recommendation.readYn === "Y",
      commentCount: 0,
      viewCount: 0,
      saved: false,
      categoryCode: "activity",
      fixedYn: "N",
    })),
  ].sort((left, right) => right.createdAt.localeCompare(left.createdAt));

  let filteredItems = boardItems;
  if (scope === "unread") {
    filteredItems = boardItems.filter((item) => !item.read);
  } else if (scope === "saved") {
    filteredItems = boardItems.filter((item) => item.saved);
  } else if (scope === "my-posts") {
    filteredItems = boardItems.filter((item) => item.actorUserId === userId);
  } else if (scope === "commented") {
    filteredItems = boardItems.filter((item) => {
      const board = boards.find((entry) => entry.pstId === item.feedId);
      return Boolean(board?.comments.some((comment) => comment.crtUserId === userId));
    });
  } else if (scope === "activity") {
    filteredItems = activityItems;
  }

  filteredItems = [...filteredItems].sort((left, right) => {
    if (sort === "reaction") {
      return numberValue(right.commentCount, 0) + numberValue(right.viewCount, 0) - (numberValue(left.commentCount, 0) + numberValue(left.viewCount, 0))
        || right.createdAt.localeCompare(left.createdAt);
    }
    return right.createdAt.localeCompare(left.createdAt);
  });

  const pageSize = 10;
  const startIndex = (page - 1) * pageSize;
  const items = filteredItems.slice(startIndex, startIndex + pageSize);

  return {
    counts: {
      all: boardItems.length,
      unread: boardItems.filter((item) => !item.read).length,
      saved: boardItems.filter((item) => item.saved).length,
      "my-posts": boardItems.filter((item) => item.actorUserId === userId).length,
      commented: boardItems.filter((item) => {
        const board = boards.find((entry) => entry.pstId === item.feedId);
        return Boolean(board?.comments.some((comment) => comment.crtUserId === userId));
      }).length,
      activity: activityItems.length,
      ...boards.reduce<Record<string, number>>((accumulator, board) => {
        accumulator[board.bbsCtgrCd] = (accumulator[board.bbsCtgrCd] || 0) + 1;
        return accumulator;
      }, {}),
    },
    items,
    departments,
    page,
    totalPages: Math.max(1, Math.ceil(filteredItems.length / pageSize)),
  };
}

async function buildDashboardProfile(currentUserId: string, targetUserId: string) {
  const [user, favorites, boards] = await Promise.all([
    getDirectoryUser(targetUserId),
    listFavoriteUsersFor(currentUserId),
    listBoardRecords(),
  ]);

  if (!user) {
    throw new HttpsError("not-found", "사용자를 찾을 수 없습니다.");
  }

  const recentBoards = boards
    .filter((board) => board.crtUserId === targetUserId)
    .sort((left, right) => right.publishedDt.localeCompare(left.publishedDt))
    .slice(0, 5)
    .map((board) => ({
      pstId: board.pstId,
      pstTtl: board.pstTtl,
      bbsCtgrCd: board.bbsCtgrCd,
      frstCrtDt: board.frstCrtDt,
    }));

  return {
    user,
    favorite: favorites.some((entry) => entry.targetUserId === targetUserId),
    recentBoards,
    recentActivities: recentBoards.map((board) => ({
      feedId: board.pstId,
      title: board.pstTtl,
      badge: "게시글",
      bodyPreview: "최근 작성 게시글",
      createdAt: board.frstCrtDt,
      route: `/board?postId=${board.pstId}`,
    })),
    histories: user.hireYmd ? [{
      historyId: `history_${sanitizeDocIdPart(user.userId)}`,
      afterDeptNm: user.deptNm,
      crtDt: `${user.hireYmd}T09:00:00.000Z`,
    }] : [],
  };
}

export const dashboardGetBootstrap = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  return buildDashboardBootstrap(uid);
});

export const dashboardGetFeed = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  return buildDashboardFeed(uid, asObject(request.data));
});

export const dashboardGetSummary = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const feed = await buildDashboardFeed(uid, {});
  return {
    counts: feed.counts,
  };
});

export const dashboardGetWidgets = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const bootstrap = await buildDashboardBootstrap(uid);
  return bootstrap.widgets;
});

export const dashboardGetPreferences = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  return getDashboardPreferences(uid);
});

export const dashboardSavePreferences = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const payload = asObject(request.data);
  return setDashboardPreferences(uid, {
    defaultScope: trimmed(payload.defaultScope) || "all",
    defaultSort: trimmed(payload.defaultSort) || "recent",
    defaultView: trimmed(payload.defaultView) || "summary",
    defaultCategory: trimmed(payload.defaultCategory) || "all",
    lastDeptId: trimmed(payload.lastDeptId),
    lastSearchQ: trimmed(payload.lastSearchQ),
  });
});

export const dashboardGetCategories = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const preferences = await getDashboardPreferences(uid);
  return {
    categories: preferences.categories,
  };
});

export const dashboardSaveCategories = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const payload = asObject(request.data);
  const categories = normalizeStringArray(payload.categories);
  const preferences = await setDashboardPreferences(uid, { categories });
  return {
    categories: preferences.categories,
  };
});

export const dashboardMarkBoardRead = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const pstId = trimmed(asObject(request.data).pstId);
  if (!pstId) {
    throw new HttpsError("invalid-argument", "pstId is required.");
  }
  await markBoardAsReadForUser(uid, pstId);
  return { pstId, readYn: "Y" };
});

export const dashboardSavePost = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const pstId = trimmed(asObject(request.data).pstId);
  if (!pstId) {
    throw new HttpsError("invalid-argument", "pstId is required.");
  }
  await db.collection(COLLECTIONS.sessionUsers).doc(uid).collection(USER_SAVED_POSTS).doc(pstId).set({
    pstId,
    savedAt: nowIso(),
  });
  return { pstId, saved: true };
});

export const dashboardUnsavePost = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const pstId = trimmed(asObject(request.data).pstId);
  if (!pstId) {
    throw new HttpsError("invalid-argument", "pstId is required.");
  }
  await db.collection(COLLECTIONS.sessionUsers).doc(uid).collection(USER_SAVED_POSTS).doc(pstId).delete().catch(() => undefined);
  return { pstId, saved: false };
});

export const dashboardGetTodos = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  return listDashboardTodos(uid);
});

export const dashboardCreateTodo = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const payload = asObject(request.data);
  const todoId = makeId("todo");
  const targetUserId = trimmed(payload.targetUserId);
  const targetUser = targetUserId ? await getDirectoryUser(targetUserId) : null;
  const todo = {
    targetUserId,
    targetUserName: targetUser?.userNm || "",
    todoTtl: trimmed(payload.todoTtl),
    todoCn: trimmed(payload.todoCn),
    dueDt: toIso(payload.dueDt),
    createdAt: nowIso(),
  };
  if (!todo.todoTtl) {
    throw new HttpsError("invalid-argument", "todoTtl is required.");
  }
  await db.collection(COLLECTIONS.sessionUsers).doc(uid).collection(USER_TODOS).doc(todoId).set(todo);
  return { todoId, ...todo };
});

export const dashboardUpdateTodo = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const payload = asObject(request.data);
  const todoId = trimmed(payload.todoId);
  if (!todoId) {
    throw new HttpsError("invalid-argument", "todoId is required.");
  }
  const next = {
    todoTtl: trimmed(payload.todoTtl),
    todoCn: trimmed(payload.todoCn),
    dueDt: toIso(payload.dueDt),
    updatedAt: nowIso(),
  };
  await db.collection(COLLECTIONS.sessionUsers).doc(uid).collection(USER_TODOS).doc(todoId).set(next, { merge: true });
  return { todoId, ...next };
});

export const dashboardDeleteTodo = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const todoId = trimmed(asObject(request.data).todoId);
  if (!todoId) {
    throw new HttpsError("invalid-argument", "todoId is required.");
  }
  await db.collection(COLLECTIONS.sessionUsers).doc(uid).collection(USER_TODOS).doc(todoId).delete().catch(() => undefined);
  return { todoId, deleted: true };
});

export const dashboardGetRecommendations = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const box = trimmed(asObject(request.data).box) || "inbox";
  return {
    items: await listRecommendations(uid, box),
  };
});

export const dashboardCreateRecommendations = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const payload = asObject(request.data);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const targetUserId = trimmed(payload.targetUserId);
  const categoryCodes = normalizeStringArray(payload.categoryCodes);
  const message = trimmed(payload.message);
  if (!targetUserId || categoryCodes.length === 0) {
    throw new HttpsError("invalid-argument", "targetUserId and categoryCodes are required.");
  }

  const categoryLabels: Record<string, string> = {
    F102: "동호회",
    F103: "경조사",
    F104: "사내소식",
    F105: "건의사항",
    F106: "기타",
  };
  const batch = db.batch();
  categoryCodes.forEach((categoryCode) => {
    const recommendId = makeId("recommend");
    batch.set(db.collection(COLLECTIONS.sessionUsers).doc(targetUserId).collection(USER_RECOMMENDATIONS).doc(recommendId), {
      targetUserId,
      fromUserId: currentUser.userId,
      fromUserName: currentUser.userNm,
      fromDeptName: currentUser.deptNm,
      categoryCode,
      categoryLabel: categoryLabels[categoryCode] || categoryCode,
      message,
      acceptedYn: "N",
      readYn: "N",
      createdAt: nowIso(),
    });
  });
  await batch.commit();
  await createAlarm(targetUserId, {
    alarmCategory: "추천",
    alarmMessage: `${currentUser.userNm}님이 새로운 커뮤니티 분류를 추천했습니다.`,
    relatedUrl: "/",
  });
  return { success: true };
});

export const dashboardUpdateRecommendation = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const payload = asObject(request.data);
  const recommendId = trimmed(payload.recommendId);
  if (!recommendId) {
    throw new HttpsError("invalid-argument", "recommendId is required.");
  }

  const recommendationRef = db.collection(COLLECTIONS.sessionUsers).doc(uid).collection(USER_RECOMMENDATIONS).doc(recommendId);
  const snapshot = await recommendationRef.get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "추천 정보를 찾을 수 없습니다.");
  }
  const recommendation = asObject(snapshot.data());
  const acceptedYn = trimmed(payload.acceptedYn) || trimmed(recommendation.acceptedYn) || "N";
  const readYn = trimmed(payload.readYn) || "Y";

  await recommendationRef.set({
    acceptedYn,
    readYn,
    updatedAt: nowIso(),
  }, { merge: true });

  if (acceptedYn === "Y") {
    const preferences = await getDashboardPreferences(uid);
    if (!preferences.categories.includes(trimmed(recommendation.categoryCode))) {
      await setDashboardPreferences(uid, {
        categories: [...preferences.categories, trimmed(recommendation.categoryCode)],
      });
    }
  }

  return {
    recommendId,
    acceptedYn,
    readYn,
  };
});

export const dashboardGetProfile = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const userId = trimmed(asObject(request.data).userId);
  if (!userId) {
    throw new HttpsError("invalid-argument", "userId is required.");
  }
  return buildDashboardProfile(uid, userId);
});

export const alarmGetList = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  await ensureUserDashboardState(uid);
  const snapshot = await db.collection(COLLECTIONS.sessionUsers).doc(uid).collection(USER_ALARMS).get();
  return snapshot.docs
    .map((doc) => asObject(doc.data()))
    .map((item) => ({
      alarmId: trimmed(item.alarmId),
      alarmCategory: trimmed(item.alarmCategory),
      alarmMessage: trimmed(item.alarmMessage),
      relatedUrl: trimmed(item.relatedUrl),
      readYn: trimmed(item.readYn) || "N",
      createdDt: toIso(item.createdDt) || nowIso(),
    }))
    .sort((left, right) => right.createdDt.localeCompare(left.createdDt));
});

export const alarmGetTop10 = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  await ensureUserDashboardState(uid);
  const snapshot = await db.collection(COLLECTIONS.sessionUsers).doc(uid).collection(USER_ALARMS).get();
  return snapshot.docs
    .map((doc) => asObject(doc.data()))
    .map((item) => ({
      alarmId: trimmed(item.alarmId),
      alarmCategory: trimmed(item.alarmCategory),
      alarmMessage: trimmed(item.alarmMessage),
      relatedUrl: trimmed(item.relatedUrl),
      readYn: trimmed(item.readYn) || "N",
      createdDt: toIso(item.createdDt) || nowIso(),
    }))
    .sort((left, right) => right.createdDt.localeCompare(left.createdDt))
    .slice(0, 10);
});

export const alarmGetDetail = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const alarmId = trimmed(asObject(request.data).alarmId);
  if (!alarmId) {
    throw new HttpsError("invalid-argument", "alarmId is required.");
  }
  await ensureUserDashboardState(uid);
  const snapshot = await db.collection(COLLECTIONS.sessionUsers).doc(uid).collection(USER_ALARMS).doc(alarmId).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "알림을 찾을 수 없습니다.");
  }
  const item = asObject(snapshot.data());
  return {
    alarmId,
    alarmCategory: trimmed(item.alarmCategory),
    alarmMessage: trimmed(item.alarmMessage),
    relatedUrl: trimmed(item.relatedUrl),
    readYn: trimmed(item.readYn) || "N",
    createdDt: toIso(item.createdDt) || nowIso(),
  };
});

export const alarmMarkAllRead = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  await ensureUserDashboardState(uid);
  const snapshot = await db.collection(COLLECTIONS.sessionUsers).doc(uid).collection(USER_ALARMS).get();
  const batch = db.batch();
  snapshot.docs.forEach((doc) => {
    batch.set(doc.ref, { readYn: "Y", updatedAt: nowIso() }, { merge: true });
  });
  await batch.commit();
  return { success: true };
});

export const boardGetNotices = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const boards = await listBoardRecords();
  return boards
    .filter((board) => board.fixedYn === "Y" || board.bbsCtgrCd === "F101")
    .sort((left, right) => right.publishedDt.localeCompare(left.publishedDt))
    .slice(0, 10)
    .map((board) => summarizeBoardForList(board, new Set<string>()));
});

export const boardGetCommunity = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const bbsCtgrCd = trimmed(asObject(request.data).bbsCtgrCd);
  const boards = await listBoardRecords();
  return boards
    .filter((board) => !bbsCtgrCd || board.bbsCtgrCd === bbsCtgrCd)
    .map((board) => summarizeBoardForList(board, new Set<string>()));
});

export const boardGetCategoryCounts = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const boards = await listBoardRecords();
  return boards.reduce<Record<string, number>>((accumulator, board) => {
    accumulator[board.bbsCtgrCd] = (accumulator[board.bbsCtgrCd] || 0) + 1;
    return accumulator;
  }, {});
});

export const boardGetDetail = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const pstId = trimmed(asObject(request.data).pstId);
  if (!pstId) {
    throw new HttpsError("invalid-argument", "pstId is required.");
  }
  return buildBoardDetailForUser(pstId, currentUser.userId, currentUser.userRole);
});

export const boardCreate = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  return createOrUpdateBoard(currentUser.userId, currentUser.userRole, payload, []);
});

export const boardUpdate = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const pstId = trimmed(payload.pstId);
  if (!pstId) {
    throw new HttpsError("invalid-argument", "pstId is required.");
  }
  const existing = await getBoardRecordOrThrow(pstId);
  if (!isAdminRole(currentUser.userRole) && existing.crtUserId !== currentUser.userId) {
    throw new HttpsError("permission-denied", "게시글 수정 권한이 없습니다.");
  }
  return createOrUpdateBoard(currentUser.userId, currentUser.userRole, asObject(payload.payload || payload), [], existing);
});

export const boardRemove = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const pstId = trimmed(asObject(request.data).pstId);
  if (!pstId) {
    throw new HttpsError("invalid-argument", "pstId is required.");
  }
  const existing = await getBoardRecordOrThrow(pstId);
  if (!isAdminRole(currentUser.userRole) && existing.crtUserId !== currentUser.userId) {
    throw new HttpsError("permission-denied", "게시글 삭제 권한이 없습니다.");
  }
  await db.collection(BOARDS).doc(pstId).delete();
  return { pstId, deleted: true };
});

export const boardIncrementView = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const pstId = trimmed(asObject(request.data).pstId);
  if (!pstId) {
    throw new HttpsError("invalid-argument", "pstId is required.");
  }
  const board = await incrementBoardView(pstId);
  return { pstId: board.pstId, viewCnt: board.viewCnt };
});

export const boardGetComments = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const pstId = trimmed(asObject(request.data).pstId);
  const board = await getBoardRecordOrThrow(pstId);
  return board.comments.filter((comment) => comment.delYn !== "Y");
});

export const boardCreateComment = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const pstId = trimmed(payload.pstId);
  const board = await getBoardRecordOrThrow(pstId);
  const commentId = makeId("comment");
  const nextComment = {
    cmntSqn: commentId,
    pstId,
    contents: trimmed(payload.contents),
    crtUserId: currentUser.userId,
    userNm: currentUser.userNm,
    deptNm: currentUser.deptNm,
    jbgdNm: currentUser.jbgdNm,
    frstCrtDt: nowIso(),
    upCmntSqn: trimmed(payload.upCmntSqn),
    delYn: "N",
    attachments: Array.isArray(payload.attachments) ? payload.attachments.map((file, index) => normalizeAttachment(file, index)) : [],
  };
  if (!nextComment.contents && nextComment.attachments.length === 0) {
    throw new HttpsError("invalid-argument", "comment contents or attachments are required.");
  }
  const nextBoard = {
    ...board,
    comments: [...board.comments, nextComment],
    lastMdfcnDt: nowIso(),
  };
  await saveBoardRecord(nextBoard);
  if (board.crtUserId && board.crtUserId !== currentUser.userId) {
    await createAlarm(board.crtUserId, {
      alarmCategory: "게시판",
      alarmMessage: `${currentUser.userNm}님이 \"${board.pstTtl}\" 글에 댓글을 남겼습니다.`,
      relatedUrl: "/board",
    });
  }
  return nextComment;
});

export const boardUpdateComment = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const pstId = trimmed(payload.pstId);
  const cmntSqn = trimmed(payload.cmntSqn);
  const board = await getBoardRecordOrThrow(pstId);
  const comment = board.comments.find((entry) => entry.cmntSqn === cmntSqn);
  if (!comment) {
    throw new HttpsError("not-found", "댓글을 찾을 수 없습니다.");
  }
  if (!isAdminRole(currentUser.userRole) && comment.crtUserId !== currentUser.userId && board.crtUserId !== currentUser.userId) {
    throw new HttpsError("permission-denied", "댓글 수정 권한이 없습니다.");
  }
  const nextBoard = {
    ...board,
    comments: board.comments.map((entry) => entry.cmntSqn === cmntSqn ? { ...entry, contents: trimmed(payload.contents) || entry.contents, attachments: Array.isArray(payload.attachments) && payload.attachments.length > 0 ? payload.attachments.map((file, index) => normalizeAttachment(file, index)) : entry.attachments } : entry),
    lastMdfcnDt: nowIso(),
  };
  await saveBoardRecord(nextBoard);
  return nextBoard.comments.find((entry) => entry.cmntSqn === cmntSqn);
});

export const boardDeleteComment = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const pstId = trimmed(payload.pstId);
  const cmntSqn = trimmed(payload.cmntSqn);
  const board = await getBoardRecordOrThrow(pstId);
  const comment = board.comments.find((entry) => entry.cmntSqn === cmntSqn);
  if (!comment) {
    throw new HttpsError("not-found", "댓글을 찾을 수 없습니다.");
  }
  if (!isAdminRole(currentUser.userRole) && comment.crtUserId !== currentUser.userId && board.crtUserId !== currentUser.userId) {
    throw new HttpsError("permission-denied", "댓글 삭제 권한이 없습니다.");
  }
  const nextBoard = {
    ...board,
    comments: board.comments.filter((entry) => entry.cmntSqn !== cmntSqn),
    lastMdfcnDt: nowIso(),
  };
  await saveBoardRecord(nextBoard);
  return { cmntSqn, deleted: true };
});

export const boardGetWorkspace = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  return getBoardWorkspace(currentUser.userId, currentUser.userRole, asObject(request.data));
});

export const boardGetWorkspaceDetail = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const pstId = trimmed(asObject(request.data).pstId);
  if (!pstId) {
    throw new HttpsError("invalid-argument", "pstId is required.");
  }
  return buildBoardDetailForUser(pstId, currentUser.userId, currentUser.userRole);
});

export const boardCreateWorkspace = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const board = await createOrUpdateBoard(currentUser.userId, currentUser.userRole, asObject(payload.payload), Array.isArray(payload.attachments) ? payload.attachments : []);
  return { board };
});

export const boardUpdateWorkspace = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const pstId = trimmed(payload.pstId);
  if (!pstId) {
    throw new HttpsError("invalid-argument", "pstId is required.");
  }
  const existing = await getBoardRecordOrThrow(pstId);
  if (!isAdminRole(currentUser.userRole) && existing.crtUserId !== currentUser.userId) {
    throw new HttpsError("permission-denied", "게시글 수정 권한이 없습니다.");
  }
  const board = await createOrUpdateBoard(currentUser.userId, currentUser.userRole, asObject(payload.payload), Array.isArray(payload.attachments) ? payload.attachments : [], existing);
  return { board };
});

export const boardToggleLike = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const profile = await loadProfile(uid, request.auth?.token || {});
  const pstId = trimmed(asObject(request.data).pstId);
  const board = await getBoardRecordOrThrow(pstId);
  const exists = board.likes.some((entry) => entry.userId === profile.userId);
  const nextLikes = exists
    ? board.likes.filter((entry) => entry.userId !== profile.userId)
    : [...board.likes, { userId: profile.userId, userNm: profile.userNm, deptNm: profile.deptNm, jbgdNm: profile.jbgdNm, crtDt: nowIso() }];
  await saveBoardRecord({
    ...board,
    likes: nextLikes,
    lastMdfcnDt: nowIso(),
  });
  return { pstId, liked: !exists, likeCount: nextLikes.length };
});

export const boardGetLikeUsers = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const pstId = trimmed(asObject(request.data).pstId);
  const board = await getBoardRecordOrThrow(pstId);
  return board.likes;
});

export const boardGetReaders = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const pstId = trimmed(asObject(request.data).pstId);
  const board = await getBoardRecordOrThrow(pstId);
  return board.readers;
});

export const boardSharePost = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const pstId = trimmed(payload.pstId);
  const board = await getBoardRecordOrThrow(pstId);
  const userIds = normalizeStringArray(payload.userIds);
  const communityIds = normalizeStringArray(payload.communityIds).map((entry) => String(entry));
  const shareEntry = {
    userIds,
    communityIds,
    crtUserId: currentUser.userId,
    createdAt: nowIso(),
  };
  await saveBoardRecord({
    ...board,
    shares: [...board.shares, shareEntry],
    lastMdfcnDt: nowIso(),
  });
  await Promise.all(userIds.filter((userId) => userId !== currentUser.userId).map((userId) => createAlarm(userId, {
    alarmCategory: "게시판",
    alarmMessage: `${currentUser.userNm}님이 \"${board.pstTtl}\" 글을 공유했습니다.`,
    relatedUrl: "/board",
  })));
  return { success: true };
});

export const boardReportPost = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const pstId = trimmed(payload.pstId);
  const board = await getBoardRecordOrThrow(pstId);
  const report = {
    userId: currentUser.userId,
    userNm: currentUser.userNm,
    reasonText: trimmed(payload.reasonText),
    createdAt: nowIso(),
  };
  await saveBoardRecord({
    ...board,
    reports: [...board.reports, report],
    lastMdfcnDt: nowIso(),
  });
  return { success: true };
});

export const boardPinPost = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const pstId = trimmed(payload.pstId);
  const fixedYn = trimmed(payload.fixedYn) || "N";
  const board = await getBoardRecordOrThrow(pstId);
  if (!isAdminRole(currentUser.userRole) && board.crtUserId !== currentUser.userId) {
    throw new HttpsError("permission-denied", "상단 공지 권한이 없습니다.");
  }
  await saveBoardRecord({
    ...board,
    fixedYn,
    lastMdfcnDt: nowIso(),
  });
  return { pstId, fixedYn };
});

export const boardVotePoll = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const pstId = trimmed(payload.pstId);
  const selectedOptionIds = normalizeStringArray(payload.optionIds);
  const board = await getBoardRecordOrThrow(pstId);
  if (!board.poll) {
    throw new HttpsError("failed-precondition", "설문 게시글이 아닙니다.");
  }
  const closed = board.poll.deadlineDt ? new Date(board.poll.deadlineDt).getTime() < Date.now() : false;
  if (closed) {
    throw new HttpsError("failed-precondition", "마감된 설문입니다.");
  }
  const effectiveSelections = board.poll.multipleYn === "Y" ? selectedOptionIds : selectedOptionIds.slice(0, 1);
  const nextBoard = {
    ...board,
    poll: {
      ...board.poll,
      options: board.poll.options.map((option) => {
        const voteUserIds = option.voteUserIds.filter((userId) => userId !== currentUser.userId);
        return {
          ...option,
          voteUserIds: effectiveSelections.includes(option.optionId) ? [...voteUserIds, currentUser.userId] : voteUserIds,
        };
      }),
    },
    lastMdfcnDt: nowIso(),
  };
  await saveBoardRecord(nextBoard);
  return { pstId, success: true };
});

export const boardUpdateTodoAssignee = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const pstId = trimmed(payload.pstId);
  const assigneeUserId = trimmed(payload.assigneeUserId);
  const statusCd = trimmed(payload.statusCd) || "requested";
  const board = await getBoardRecordOrThrow(pstId);
  if (!board.todo) {
    throw new HttpsError("failed-precondition", "할 일 게시글이 아닙니다.");
  }
  if (!isAdminRole(currentUser.userRole) && currentUser.userId !== assigneeUserId) {
    throw new HttpsError("permission-denied", "담당자 상태를 변경할 권한이 없습니다.");
  }
  const nextBoard = {
    ...board,
    todo: {
      ...board.todo,
      assignees: board.todo.assignees.map((assignee) => assignee.userId === assigneeUserId ? { ...assignee, statusCd } : assignee),
    },
    lastMdfcnDt: nowIso(),
  };
  await saveBoardRecord(nextBoard);
  return { pstId, assigneeUserId, statusCd };
});

export const boardCheckScheduleAvailability = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const payload = asObject(request.data);
  const startDt = toIso(payload.startDt);
  const endDt = toIso(payload.endDt);
  const userIds = normalizeStringArray(payload.userIds);
  const boards = await listBoardRecords();
  const items = userIds.map((userId) => {
    const conflicts = boards
      .filter((board) => board.pstTypeCd === "schedule" && board.schedule)
      .filter((board) => board.crtUserId === userId || board.schedule?.attendees.some((attendee) => attendee.userId === userId))
      .filter((board) => {
        const conflictStart = new Date(board.schedule?.startDt || "").getTime();
        const conflictEnd = new Date(board.schedule?.endDt || board.schedule?.startDt || "").getTime();
        const targetStart = new Date(startDt).getTime();
        const targetEnd = new Date(endDt).getTime();
        return Number.isFinite(conflictStart)
          && Number.isFinite(conflictEnd)
          && Number.isFinite(targetStart)
          && Number.isFinite(targetEnd)
          && conflictStart < targetEnd
          && conflictEnd > targetStart;
      })
      .map((board) => board.pstTtl);
    return {
      userId,
      availableYn: conflicts.length === 0 ? "Y" : "N",
      conflicts,
    };
  });
  return { items };
});
