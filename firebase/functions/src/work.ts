import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
  CallableAuth,
  COLLECTIONS,
  JsonMap,
  asObject,
  assertSignedIn,
  db,
  ensureBaselineData,
  nowIso,
  numberValue,
  toIso,
  trimmed,
} from "./shared.js";
import { getDirectoryUser, listDirectoryUsersData, loadProfile } from "./profile.js";

type ProjectMember = {
  bizUserId: string;
  bizUserNm: string;
  bizUserDeptNm: string;
  bizUserJobNm: string;
  bizAuthCd: string;
  bizAuthNm: string;
};

type ProjectItem = {
  bizId: string;
  bizNm: string;
  bizTypeCd: string;
  bizSttsCd: string;
  bizGoal: string;
  bizDetail: string;
  bizScope: string;
  bizBdgt: number | null;
  bizPrgrs: number;
  strtBizDt: string;
  endBizDt: string;
  bizPicId: string;
  bizPicNm: string;
  members: ProjectMember[];
  createdAt: string;
  updatedAt: string;
};

type ProjectTask = {
  taskId: string;
  bizId: string;
  taskNm: string;
  bizUserId: string;
  bizUserNm: string;
  taskSttsCd: string;
  strtTaskDt: string;
  endTaskDt: string;
  taskDetail: string;
  taskPrgrs: number;
  createdAt: string;
  updatedAt: string;
};

type AttendanceRecord = {
  userId: string;
  userNm: string;
  deptId: string;
  deptNm: string;
  workYmd: string;
  workBgngDt: string;
  workEndDt: string;
  workHr: number;
  workSttsCd: string;
  lateYn: string;
  createdAt: string;
  updatedAt: string;
};

type MeetingRoom = {
  roomId: string;
  roomName: string;
  location: string;
  capacity: number;
  useYn: string;
  createdAt: string;
  updatedAt: string;
};

type MeetingReservation = {
  reservationId: string;
  roomId: string;
  roomName: string;
  title: string;
  userId: string;
  userNm: string;
  meetingDate: string;
  startTime: number;
  endTime: number;
  createdAt: string;
  updatedAt: string;
};

type CalendarEvent = {
  eventKey: string;
  sourceCd: string;
  sourceGroupCd: string;
  title: string;
  description: string;
  startDt: string;
  endDt: string;
  alldayYn: string;
  colorCd: string;
  ownerUserId: string;
  ownerUserNm: string;
  deptId: string;
  deptNm: string;
  communityId: string;
  communityNm: string;
  projectId: string;
  projectNm: string;
  placeText: string;
  placeUrl: string;
  repeatRule: string;
  statusLabel: string;
  detailHref: string;
  canEdit: boolean;
  canDelete: boolean;
  createdAt: string;
  updatedAt: string;
};

type FavoriteUser = {
  targetUserId: string;
  userNm: string;
  deptNm: string;
  jbgdNm: string;
  createdAt: string;
};

type CommunityItem = {
  communityId: string;
  communityNm: string;
  communityDesc: string;
  communityTypeCd: string;
  visibilityCd: string;
  joinPolicyCd: string;
  introText: string;
  postTemplateHtml: string;
  favoriteUserIds: string[];
  memberships: Array<{
    userId: string;
    roleCd: string;
    memberStatusCd: string;
  }>;
  createdAt: string;
  updatedAt: string;
};

const WORK_BOOTSTRAP = "system/workBootstrap";
const PROJECTS = "projects";
const PROJECT_TASKS = "projectTasks";
const ATTENDANCE = "attendanceRecords";
const MEETING_ROOMS = "meetingRooms";
const MEETING_RESERVATIONS = "meetingReservations";
const CALENDAR_EVENTS = "calendarEvents";
const COMMUNITIES = "communities";

const PROJECT_ROLE_LABELS: Record<string, string> = {
  B101: "PM",
  B102: "팀원",
  B103: "열람자",
};

const CALENDAR_COLORS: Record<string, string> = {
  user_schedule: "#ec4899",
  dept_schedule: "#16a34a",
  community_schedule: "#3b82f6",
  subscription_schedule: "#84cc16",
  user_todo: "#38bdf8",
  team_schedule: "#f97316",
};

function dateKey(value = new Date()): string {
  const current = value instanceof Date ? value : new Date(value);
  const year = current.getFullYear();
  const month = String(current.getMonth() + 1).padStart(2, "0");
  const day = String(current.getDate()).padStart(2, "0");
  return `${year}${month}${day}`;
}

function toDateInput(value: string): string {
  const normalized = trimmed(value);
  if (!normalized) {
    return "";
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(normalized)) {
    return normalized;
  }
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  return date.toISOString().slice(0, 10);
}

function startOfMonth(anchorDate: string): string {
  const base = new Date(anchorDate || nowIso());
  return new Date(base.getFullYear(), base.getMonth(), 1).toISOString().slice(0, 10);
}

function endOfMonth(anchorDate: string): string {
  const base = new Date(anchorDate || nowIso());
  return new Date(base.getFullYear(), base.getMonth() + 1, 0).toISOString().slice(0, 10);
}

function normalizeProjectMember(input: unknown, defaults: Partial<ProjectMember> = {}): ProjectMember {
  const source = asObject(input);
  return {
    bizUserId: trimmed(source.bizUserId) || defaults.bizUserId || "",
    bizUserNm: trimmed(source.bizUserNm) || defaults.bizUserNm || "",
    bizUserDeptNm: trimmed(source.bizUserDeptNm) || defaults.bizUserDeptNm || "",
    bizUserJobNm: trimmed(source.bizUserJobNm) || defaults.bizUserJobNm || "",
    bizAuthCd: trimmed(source.bizAuthCd) || defaults.bizAuthCd || "B102",
    bizAuthNm: trimmed(source.bizAuthNm) || defaults.bizAuthNm || PROJECT_ROLE_LABELS[trimmed(source.bizAuthCd)] || "팀원",
  };
}

function normalizeProject(source: JsonMap): ProjectItem {
  return {
    bizId: trimmed(source.bizId),
    bizNm: trimmed(source.bizNm),
    bizTypeCd: trimmed(source.bizTypeCd) || "B201",
    bizSttsCd: trimmed(source.bizSttsCd) || "B301",
    bizGoal: trimmed(source.bizGoal),
    bizDetail: trimmed(source.bizDetail),
    bizScope: trimmed(source.bizScope),
    bizBdgt: source.bizBdgt === null || source.bizBdgt === undefined || source.bizBdgt === "" ? null : numberValue(source.bizBdgt, 0),
    bizPrgrs: numberValue(source.bizPrgrs, 0),
    strtBizDt: toIso(source.strtBizDt),
    endBizDt: toIso(source.endBizDt),
    bizPicId: trimmed(source.bizPicId),
    bizPicNm: trimmed(source.bizPicNm),
    members: Array.isArray(source.members) ? source.members.map((item) => normalizeProjectMember(item)) : [],
    createdAt: toIso(source.createdAt),
    updatedAt: toIso(source.updatedAt),
  };
}

function normalizeTask(source: JsonMap): ProjectTask {
  return {
    taskId: trimmed(source.taskId),
    bizId: trimmed(source.bizId),
    taskNm: trimmed(source.taskNm),
    bizUserId: trimmed(source.bizUserId),
    bizUserNm: trimmed(source.bizUserNm),
    taskSttsCd: trimmed(source.taskSttsCd) || "B401",
    strtTaskDt: toIso(source.strtTaskDt),
    endTaskDt: toIso(source.endTaskDt),
    taskDetail: trimmed(source.taskDetail),
    taskPrgrs: numberValue(source.taskPrgrs, 0),
    createdAt: toIso(source.createdAt),
    updatedAt: toIso(source.updatedAt),
  };
}

function normalizeAttendance(source: JsonMap): AttendanceRecord {
  return {
    userId: trimmed(source.userId),
    userNm: trimmed(source.userNm),
    deptId: trimmed(source.deptId),
    deptNm: trimmed(source.deptNm),
    workYmd: trimmed(source.workYmd),
    workBgngDt: toIso(source.workBgngDt),
    workEndDt: toIso(source.workEndDt),
    workHr: numberValue(source.workHr, 0),
    workSttsCd: trimmed(source.workSttsCd) || "C103",
    lateYn: trimmed(source.lateYn) || "N",
    createdAt: toIso(source.createdAt),
    updatedAt: toIso(source.updatedAt),
  };
}

function normalizeMeetingRoom(source: JsonMap): MeetingRoom {
  return {
    roomId: trimmed(source.roomId),
    roomName: trimmed(source.roomName),
    location: trimmed(source.location),
    capacity: numberValue(source.capacity, 6),
    useYn: trimmed(source.useYn) || "Y",
    createdAt: toIso(source.createdAt),
    updatedAt: toIso(source.updatedAt),
  };
}

function normalizeReservation(source: JsonMap): MeetingReservation {
  return {
    reservationId: trimmed(source.reservationId),
    roomId: trimmed(source.roomId),
    roomName: trimmed(source.roomName),
    title: trimmed(source.title),
    userId: trimmed(source.userId),
    userNm: trimmed(source.userNm),
    meetingDate: toDateInput(trimmed(source.meetingDate) || toIso(source.createdAt)),
    startTime: numberValue(source.startTime, 9),
    endTime: numberValue(source.endTime, 10),
    createdAt: toIso(source.createdAt),
    updatedAt: toIso(source.updatedAt),
  };
}

function normalizeCalendarEvent(source: JsonMap, defaults: Partial<CalendarEvent> = {}): CalendarEvent {
  const sourceCd = trimmed(source.sourceCd) || defaults.sourceCd || "user_schedule";
  return {
    eventKey: trimmed(source.eventKey) || defaults.eventKey || "",
    sourceCd,
    sourceGroupCd: trimmed(source.sourceGroupCd) || defaults.sourceGroupCd || "my",
    title: trimmed(source.title) || defaults.title || "",
    description: trimmed(source.description) || defaults.description || "",
    startDt: toIso(source.startDt) || defaults.startDt || "",
    endDt: toIso(source.endDt) || defaults.endDt || "",
    alldayYn: trimmed(source.alldayYn) || defaults.alldayYn || "N",
    colorCd: trimmed(source.colorCd) || defaults.colorCd || CALENDAR_COLORS[sourceCd] || "#64748b",
    ownerUserId: trimmed(source.ownerUserId) || defaults.ownerUserId || "",
    ownerUserNm: trimmed(source.ownerUserNm) || defaults.ownerUserNm || "",
    deptId: trimmed(source.deptId) || defaults.deptId || "",
    deptNm: trimmed(source.deptNm) || defaults.deptNm || "",
    communityId: trimmed(source.communityId) || defaults.communityId || "",
    communityNm: trimmed(source.communityNm) || defaults.communityNm || "",
    projectId: trimmed(source.projectId) || defaults.projectId || "",
    projectNm: trimmed(source.projectNm) || defaults.projectNm || "",
    placeText: trimmed(source.placeText) || defaults.placeText || "",
    placeUrl: trimmed(source.placeUrl) || defaults.placeUrl || "",
    repeatRule: trimmed(source.repeatRule) || defaults.repeatRule || "",
    statusLabel: trimmed(source.statusLabel) || defaults.statusLabel || "",
    detailHref: trimmed(source.detailHref) || defaults.detailHref || "",
    canEdit: typeof source.canEdit === "boolean" ? source.canEdit : defaults.canEdit || false,
    canDelete: typeof source.canDelete === "boolean" ? source.canDelete : defaults.canDelete || false,
    createdAt: toIso(source.createdAt) || defaults.createdAt || "",
    updatedAt: toIso(source.updatedAt) || defaults.updatedAt || "",
  };
}

function normalizeFavorite(source: JsonMap): FavoriteUser {
  return {
    targetUserId: trimmed(source.targetUserId),
    userNm: trimmed(source.userNm),
    deptNm: trimmed(source.deptNm),
    jbgdNm: trimmed(source.jbgdNm),
    createdAt: toIso(source.createdAt),
  };
}

function normalizeCommunity(source: JsonMap): CommunityItem {
  return {
    communityId: trimmed(source.communityId),
    communityNm: trimmed(source.communityNm),
    communityDesc: trimmed(source.communityDesc),
    communityTypeCd: trimmed(source.communityTypeCd) || "general",
    visibilityCd: trimmed(source.visibilityCd) || "public",
    joinPolicyCd: trimmed(source.joinPolicyCd) || "instant",
    introText: trimmed(source.introText),
    postTemplateHtml: trimmed(source.postTemplateHtml),
    favoriteUserIds: Array.isArray(source.favoriteUserIds) ? source.favoriteUserIds.map((item) => trimmed(item)).filter(Boolean) : [],
    memberships: Array.isArray(source.memberships)
      ? source.memberships
        .map((item) => {
          const member = asObject(item);
          return {
            userId: trimmed(member.userId),
            roleCd: trimmed(member.roleCd) || "member",
            memberStatusCd: trimmed(member.memberStatusCd) || "active",
          };
        })
        .filter((item) => item.userId)
      : [],
    createdAt: toIso(source.createdAt),
    updatedAt: toIso(source.updatedAt),
  };
}

async function ensureWorkSeedData(): Promise<void> {
  await ensureBaselineData();
  const bootstrapRef = db.doc(WORK_BOOTSTRAP);
  const snapshot = await bootstrapRef.get();
  if (snapshot.exists) {
    return;
  }

  const users = await listDirectoryUsersData();
  const admin = users.find((item) => item.userId === "admin") || users[0];
  const memberOne = users.find((item) => item.userId === "user01") || users[0];
  const memberTwo = users.find((item) => item.userId === "user02") || users[1] || users[0];
  const seededAt = nowIso();
  const batch = db.batch();

  const projectA: ProjectItem = {
    bizId: "BIZ_FIREBASE",
    bizNm: "StarWorks Firebase 전환",
    bizTypeCd: "B202",
    bizSttsCd: "B302",
    bizGoal: "핵심 협업 기능을 Firebase 기준으로 전환합니다.",
    bizDetail: "인증, 전자결재, 프로젝트, 일정, 메신저를 우선 대상으로 전환합니다.",
    bizScope: "프런트 API 어댑터, Firebase Functions, Firestore/Storage 연결",
    bizBdgt: 0,
    bizPrgrs: 48,
    strtBizDt: "2026-03-01T09:00:00.000Z",
    endBizDt: "2026-04-30T18:00:00.000Z",
    bizPicId: admin?.userId || "admin",
    bizPicNm: admin?.userNm || "관리자",
    members: [
      {
        bizUserId: admin?.userId || "admin",
        bizUserNm: admin?.userNm || "관리자",
        bizUserDeptNm: admin?.deptNm || "",
        bizUserJobNm: admin?.jbgdNm || "",
        bizAuthCd: "B101",
        bizAuthNm: PROJECT_ROLE_LABELS.B101,
      },
      {
        bizUserId: memberOne?.userId || "user01",
        bizUserNm: memberOne?.userNm || "user01",
        bizUserDeptNm: memberOne?.deptNm || "",
        bizUserJobNm: memberOne?.jbgdNm || "",
        bizAuthCd: "B102",
        bizAuthNm: PROJECT_ROLE_LABELS.B102,
      },
      {
        bizUserId: memberTwo?.userId || "user02",
        bizUserNm: memberTwo?.userNm || "user02",
        bizUserDeptNm: memberTwo?.deptNm || "",
        bizUserJobNm: memberTwo?.jbgdNm || "",
        bizAuthCd: "B103",
        bizAuthNm: PROJECT_ROLE_LABELS.B103,
      },
    ],
    createdAt: seededAt,
    updatedAt: seededAt,
  };

  const projectB: ProjectItem = {
    bizId: "BIZ_GROUPWARE",
    bizNm: "그룹웨어 운영 안정화",
    bizTypeCd: "B203",
    bizSttsCd: "B301",
    bizGoal: "운영 중인 핵심 모듈의 체감 속도와 안정성을 확보합니다.",
    bizDetail: "느린 목록과 빈 화면 문제를 줄이고 운영 데이터를 정리합니다.",
    bizScope: "전자결재, 게시판, 캘린더, 메일",
    bizBdgt: 0,
    bizPrgrs: 18,
    strtBizDt: "2026-03-05T09:00:00.000Z",
    endBizDt: "2026-05-20T18:00:00.000Z",
    bizPicId: memberOne?.userId || "user01",
    bizPicNm: memberOne?.userNm || "user01",
    members: [
      {
        bizUserId: memberOne?.userId || "user01",
        bizUserNm: memberOne?.userNm || "user01",
        bizUserDeptNm: memberOne?.deptNm || "",
        bizUserJobNm: memberOne?.jbgdNm || "",
        bizAuthCd: "B101",
        bizAuthNm: PROJECT_ROLE_LABELS.B101,
      },
      {
        bizUserId: admin?.userId || "admin",
        bizUserNm: admin?.userNm || "관리자",
        bizUserDeptNm: admin?.deptNm || "",
        bizUserJobNm: admin?.jbgdNm || "",
        bizAuthCd: "B102",
        bizAuthNm: PROJECT_ROLE_LABELS.B102,
      },
    ],
    createdAt: seededAt,
    updatedAt: seededAt,
  };

  [projectA, projectB].forEach((project) => {
    batch.set(db.collection(PROJECTS).doc(project.bizId), project);
  });

  const projectTasks: ProjectTask[] = [
    {
      taskId: "TASK_FIREBASE_API",
      bizId: projectA.bizId,
      taskNm: "API 어댑터 Firebase 전환",
      bizUserId: admin?.userId || "admin",
      bizUserNm: admin?.userNm || "관리자",
      taskSttsCd: "B402",
      strtTaskDt: "2026-03-06T09:00:00.000Z",
      endTaskDt: "2026-03-12T18:00:00.000Z",
      taskDetail: "legacy REST 분기를 Firebase 브리지로 대체합니다.",
      taskPrgrs: 60,
      createdAt: seededAt,
      updatedAt: seededAt,
    },
    {
      taskId: "TASK_FIREBASE_CAL",
      bizId: projectA.bizId,
      taskNm: "캘린더 메타데이터 정리",
      bizUserId: memberOne?.userId || "user01",
      bizUserNm: memberOne?.userNm || "user01",
      taskSttsCd: "B401",
      strtTaskDt: "2026-03-10T09:00:00.000Z",
      endTaskDt: "2026-03-17T18:00:00.000Z",
      taskDetail: "구독 사용자, 커뮤니티, 프로젝트 일정 소스를 정리합니다.",
      taskPrgrs: 0,
      createdAt: seededAt,
      updatedAt: seededAt,
    },
    {
      taskId: "TASK_OPER_BOARD",
      bizId: projectB.bizId,
      taskNm: "운영 공지 정리",
      bizUserId: memberTwo?.userId || "user02",
      bizUserNm: memberTwo?.userNm || "user02",
      taskSttsCd: "B403",
      strtTaskDt: "2026-03-08T09:00:00.000Z",
      endTaskDt: "2026-03-21T18:00:00.000Z",
      taskDetail: "이관 이후 공지/문서 정합성을 검수합니다.",
      taskPrgrs: 15,
      createdAt: seededAt,
      updatedAt: seededAt,
    },
  ];

  projectTasks.forEach((task) => {
    batch.set(db.collection(PROJECT_TASKS).doc(task.taskId), task);
  });

  const meetingRooms: MeetingRoom[] = [
    { roomId: "ROOM_A", roomName: "3층 대회의실", location: "본관 3층", capacity: 12, useYn: "Y", createdAt: seededAt, updatedAt: seededAt },
    { roomId: "ROOM_B", roomName: "5층 세미나실", location: "본관 5층", capacity: 8, useYn: "Y", createdAt: seededAt, updatedAt: seededAt },
  ];

  meetingRooms.forEach((room) => {
    batch.set(db.collection(MEETING_ROOMS).doc(room.roomId), room);
  });

  const meetingReservations: MeetingReservation[] = [
    {
      reservationId: "RSV_001",
      roomId: "ROOM_A",
      roomName: "3층 대회의실",
      title: "주간 운영 회의",
      userId: admin?.userId || "admin",
      userNm: admin?.userNm || "관리자",
      meetingDate: "2026-03-09",
      startTime: 10,
      endTime: 11,
      createdAt: seededAt,
      updatedAt: seededAt,
    },
    {
      reservationId: "RSV_002",
      roomId: "ROOM_B",
      roomName: "5층 세미나실",
      title: "프로젝트 킥오프",
      userId: memberOne?.userId || "user01",
      userNm: memberOne?.userNm || "user01",
      meetingDate: "2026-03-09",
      startTime: 14,
      endTime: 16,
      createdAt: seededAt,
      updatedAt: seededAt,
    },
  ];

  meetingReservations.forEach((reservation) => {
    batch.set(db.collection(MEETING_RESERVATIONS).doc(reservation.reservationId), reservation);
  });

  const communities: CommunityItem[] = [
    {
      communityId: "COMM_DEV",
      communityNm: "개발 라운지",
      communityDesc: "개발 조직 공지와 협업 자료를 공유합니다.",
      communityTypeCd: "general",
      visibilityCd: "public",
      joinPolicyCd: "instant",
      introText: "업무 팁, 공지, 기술 공유",
      postTemplateHtml: "<p>공유할 내용을 작성하세요.</p>",
      favoriteUserIds: [memberOne?.userId || "user01"],
      memberships: [
        { userId: admin?.userId || "admin", roleCd: "owner", memberStatusCd: "active" },
        { userId: memberOne?.userId || "user01", roleCd: "member", memberStatusCd: "active" },
      ],
      createdAt: seededAt,
      updatedAt: seededAt,
    },
    {
      communityId: "COMM_PM",
      communityNm: "프로젝트 운영",
      communityDesc: "프로젝트 운영 공지와 일정 공유",
      communityTypeCd: "notice",
      visibilityCd: "private",
      joinPolicyCd: "approval",
      introText: "운영 공지 전용 채널",
      postTemplateHtml: "<p>운영 공지를 작성하세요.</p>",
      favoriteUserIds: [admin?.userId || "admin"],
      memberships: [
        { userId: admin?.userId || "admin", roleCd: "owner", memberStatusCd: "active" },
        { userId: memberTwo?.userId || "user02", roleCd: "operator", memberStatusCd: "active" },
      ],
      createdAt: seededAt,
      updatedAt: seededAt,
    },
  ];

  communities.forEach((community) => {
    batch.set(db.collection(COMMUNITIES).doc(community.communityId), community);
  });

  const calendarEvents: CalendarEvent[] = [
    {
      eventKey: "user_EVT_001",
      sourceCd: "user_schedule",
      sourceGroupCd: "my",
      title: "Firebase 마이그레이션 점검",
      description: "이번 주 전환 범위와 리스크를 점검합니다.",
      startDt: "2026-03-10T01:00:00.000Z",
      endDt: "2026-03-10T02:00:00.000Z",
      alldayYn: "N",
      colorCd: CALENDAR_COLORS.user_schedule,
      ownerUserId: admin?.userId || "admin",
      ownerUserNm: admin?.userNm || "관리자",
      deptId: admin?.deptId || "",
      deptNm: admin?.deptNm || "",
      communityId: "",
      communityNm: "",
      projectId: "",
      projectNm: "",
      placeText: "온라인",
      placeUrl: "",
      repeatRule: "",
      statusLabel: "",
      detailHref: "",
      canEdit: true,
      canDelete: true,
      createdAt: seededAt,
      updatedAt: seededAt,
    },
    {
      eventKey: "dept_EVT_001",
      sourceCd: "dept_schedule",
      sourceGroupCd: "org",
      title: "개발 조직 주간 공유",
      description: "주간 이슈와 배포 일정을 공유합니다.",
      startDt: "2026-03-11T05:00:00.000Z",
      endDt: "2026-03-11T06:00:00.000Z",
      alldayYn: "N",
      colorCd: CALENDAR_COLORS.dept_schedule,
      ownerUserId: admin?.userId || "admin",
      ownerUserNm: admin?.userNm || "관리자",
      deptId: admin?.deptId || "",
      deptNm: admin?.deptNm || "",
      communityId: "",
      communityNm: "",
      projectId: "",
      projectNm: "",
      placeText: "본관 3층",
      placeUrl: "",
      repeatRule: "",
      statusLabel: "",
      detailHref: "",
      canEdit: true,
      canDelete: true,
      createdAt: seededAt,
      updatedAt: seededAt,
    },
    {
      eventKey: "community_EVT_001",
      sourceCd: "community_schedule",
      sourceGroupCd: "community",
      title: "개발 라운지 월간 밋업",
      description: "커뮤니티 월간 공유 모임입니다.",
      startDt: "2026-03-14T10:00:00.000Z",
      endDt: "2026-03-14T11:30:00.000Z",
      alldayYn: "N",
      colorCd: CALENDAR_COLORS.community_schedule,
      ownerUserId: memberOne?.userId || "user01",
      ownerUserNm: memberOne?.userNm || "user01",
      deptId: memberOne?.deptId || "",
      deptNm: memberOne?.deptNm || "",
      communityId: "COMM_DEV",
      communityNm: "개발 라운지",
      projectId: "",
      projectNm: "",
      placeText: "오프라인 세미나실",
      placeUrl: "",
      repeatRule: "",
      statusLabel: "",
      detailHref: "",
      canEdit: false,
      canDelete: false,
      createdAt: seededAt,
      updatedAt: seededAt,
    },
  ];

  calendarEvents.forEach((event) => {
    batch.set(db.collection(CALENDAR_EVENTS).doc(event.eventKey), event);
  });

  const attendanceRecords: AttendanceRecord[] = users.slice(0, 3).flatMap((user, index) => {
    const begin = new Date(Date.UTC(2026, 2, 7 + index, 0, 15 + index * 5));
    const end = new Date(begin.getTime() + (8 * 60 + 20) * 60000);
    return [
      {
        userId: user.userId,
        userNm: user.userNm,
        deptId: user.deptId,
        deptNm: user.deptNm,
        workYmd: `2026030${7 + index}`,
        workBgngDt: begin.toISOString(),
        workEndDt: end.toISOString(),
        workHr: 500,
        workSttsCd: "C103",
        lateYn: index === 2 ? "Y" : "N",
        createdAt: seededAt,
        updatedAt: seededAt,
      },
    ];
  });

  attendanceRecords.forEach((record) => {
    batch.set(db.collection(ATTENDANCE).doc(`${record.userId}_${record.workYmd}`), record);
  });

  batch.set(bootstrapRef, { seedVersion: 1, seededAt });
  await batch.commit();
}

async function listProjects(): Promise<ProjectItem[]> {
  await ensureWorkSeedData();
  const snapshot = await db.collection(PROJECTS).get();
  return snapshot.docs
    .map((doc) => normalizeProject(doc.data() as JsonMap))
    .sort((left, right) => right.updatedAt.localeCompare(left.updatedAt));
}

async function getProjectOrThrow(bizId: string): Promise<ProjectItem> {
  await ensureWorkSeedData();
  const snapshot = await db.collection(PROJECTS).doc(bizId).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "Project not found.");
  }
  return normalizeProject(snapshot.data() as JsonMap);
}

async function listProjectTasks(bizId: string): Promise<ProjectTask[]> {
  await ensureWorkSeedData();
  const snapshot = await db.collection(PROJECT_TASKS).where("bizId", "==", bizId).get();
  return snapshot.docs
    .map((doc) => normalizeTask(doc.data() as JsonMap))
    .sort((left, right) => left.endTaskDt.localeCompare(right.endTaskDt));
}

async function saveProject(project: ProjectItem): Promise<void> {
  await db.collection(PROJECTS).doc(project.bizId).set(project, { merge: true });
}

function currentProjectAuth(project: ProjectItem, userId: string): string {
  return project.members.find((member) => member.bizUserId === userId)?.bizAuthCd || "";
}

function listProjectMembers(project: ProjectItem): ProjectMember[] {
  return project.members.slice().sort((left, right) => {
    const leftPriority = left.bizAuthCd === "B101" ? 0 : left.bizAuthCd === "B102" ? 1 : 2;
    const rightPriority = right.bizAuthCd === "B101" ? 0 : right.bizAuthCd === "B102" ? 1 : 2;
    if (leftPriority !== rightPriority) {
      return leftPriority - rightPriority;
    }
    return left.bizUserNm.localeCompare(right.bizUserNm, "ko");
  });
}

async function buildProjectMembers(
  ownerUserId: string,
  memberEntries: unknown[],
  fallbackProject?: ProjectItem
): Promise<ProjectMember[]> {
  const requested = Array.isArray(memberEntries) ? memberEntries.map((item) => asObject(item)) : [];
  const unique = new Map<string, string>();
  requested.forEach((item) => {
    const memberUserId = trimmed(item.bizUserId);
    if (memberUserId) {
      unique.set(memberUserId, trimmed(item.bizAuthCd) || "B102");
    }
  });
  unique.set(ownerUserId, "B101");

  const members = await Promise.all(
    Array.from(unique.entries()).map(async ([bizUserId, bizAuthCd]) => {
      const directoryUser = await getDirectoryUser(bizUserId);
      const existing = fallbackProject?.members.find((member) => member.bizUserId === bizUserId);
      return {
        bizUserId,
        bizUserNm: directoryUser?.userNm || existing?.bizUserNm || bizUserId,
        bizUserDeptNm: directoryUser?.deptNm || existing?.bizUserDeptNm || "",
        bizUserJobNm: directoryUser?.jbgdNm || existing?.bizUserJobNm || "",
        bizAuthCd,
        bizAuthNm: PROJECT_ROLE_LABELS[bizAuthCd] || existing?.bizAuthNm || "팀원",
      };
    })
  );

  return members.sort((left, right) => {
    const leftPriority = left.bizAuthCd === "B101" ? 0 : left.bizAuthCd === "B102" ? 1 : 2;
    const rightPriority = right.bizAuthCd === "B101" ? 0 : right.bizAuthCd === "B102" ? 1 : 2;
    if (leftPriority !== rightPriority) {
      return leftPriority - rightPriority;
    }
    return left.bizUserNm.localeCompare(right.bizUserNm, "ko");
  });
}

function buildProjectPayload(source: JsonMap, currentUserId: string, currentUserName: string, project?: ProjectItem): ProjectItem {
  return {
    bizId: trimmed(source.bizId) || project?.bizId || `BIZ_${db.collection(PROJECTS).doc().id}`,
    bizNm: trimmed(source.bizNm),
    bizTypeCd: trimmed(source.bizTypeCd) || project?.bizTypeCd || "B201",
    bizSttsCd: trimmed(source.bizSttsCd) || project?.bizSttsCd || "B301",
    bizGoal: trimmed(source.bizGoal),
    bizDetail: trimmed(source.bizDetail),
    bizScope: trimmed(source.bizScope),
    bizBdgt: source.bizBdgt === null || source.bizBdgt === undefined || source.bizBdgt === "" ? null : numberValue(source.bizBdgt, 0),
    bizPrgrs: source.bizPrgrs === null || source.bizPrgrs === undefined || source.bizPrgrs === "" ? (project?.bizPrgrs || 0) : numberValue(source.bizPrgrs, 0),
    strtBizDt: trimmed(source.strtBizDt) || project?.strtBizDt || "",
    endBizDt: trimmed(source.endBizDt) || project?.endBizDt || "",
    bizPicId: project?.bizPicId || currentUserId,
    bizPicNm: project?.bizPicNm || currentUserName,
    members: [],
    createdAt: project?.createdAt || nowIso(),
    updatedAt: nowIso(),
  };
}

async function listAttendanceRecords(userId?: string): Promise<AttendanceRecord[]> {
  await ensureWorkSeedData();
  const query = userId ? db.collection(ATTENDANCE).where("userId", "==", userId) : db.collection(ATTENDANCE);
  const snapshot = await query.get();
  return snapshot.docs
    .map((doc) => normalizeAttendance(doc.data() as JsonMap))
    .sort((left, right) => right.workYmd.localeCompare(left.workYmd));
}

function computeWorkMinutes(record: AttendanceRecord): number {
  if (!record.workBgngDt || !record.workEndDt) {
    return 0;
  }
  const start = new Date(record.workBgngDt).getTime();
  const end = new Date(record.workEndDt).getTime();
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) {
    return 0;
  }
  return Math.round((end - start) / 60000);
}

function summarizeAttendance(records: AttendanceRecord[]) {
  const workDays = records.length;
  const totalWorkHr = records.reduce((sum, item) => sum + Number(item.workHr || 0), 0);
  const lateCount = records.filter((item) => item.lateYn === "Y").length;
  const totalOvertimeHr = records.reduce((sum, item) => sum + Math.max(Number(item.workHr || 0) - 480, 0), 0);
  const absentDays = Math.max(0, 20 - workDays);
  return { workDays, totalWorkHr, lateCount, totalOvertimeHr, absentDays };
}

function withinMonth(workYmd: string, anchor = new Date()): boolean {
  const target = `${anchor.getFullYear()}${String(anchor.getMonth() + 1).padStart(2, "0")}`;
  return workYmd.startsWith(target);
}

function withinWeek(workYmd: string, anchor = new Date()): boolean {
  const year = Number(workYmd.slice(0, 4));
  const month = Number(workYmd.slice(4, 6)) - 1;
  const day = Number(workYmd.slice(6, 8));
  const date = new Date(year, month, day);
  const start = new Date(anchor);
  start.setHours(0, 0, 0, 0);
  start.setDate(start.getDate() - start.getDay());
  const end = new Date(start);
  end.setDate(start.getDate() + 7);
  return date >= start && date < end;
}

async function ensureAttendanceUserRecord(profile: Awaited<ReturnType<typeof loadProfile>>, workYmd: string): Promise<AttendanceRecord> {
  await ensureWorkSeedData();
  const recordRef = db.collection(ATTENDANCE).doc(`${profile.userId}_${workYmd}`);
  const snapshot = await recordRef.get();
  if (snapshot.exists) {
    return normalizeAttendance(snapshot.data() as JsonMap);
  }
  const nextRecord: AttendanceRecord = {
    userId: profile.userId,
    userNm: profile.userNm,
    deptId: profile.deptId,
    deptNm: profile.deptNm,
    workYmd,
    workBgngDt: "",
    workEndDt: "",
    workHr: 0,
    workSttsCd: "C103",
    lateYn: "N",
    createdAt: nowIso(),
    updatedAt: nowIso(),
  };
  await recordRef.set(nextRecord);
  return nextRecord;
}

async function listMeetingRoomsData(): Promise<MeetingRoom[]> {
  await ensureWorkSeedData();
  const snapshot = await db.collection(MEETING_ROOMS).get();
  return snapshot.docs
    .map((doc) => normalizeMeetingRoom(doc.data() as JsonMap))
    .sort((left, right) => left.roomName.localeCompare(right.roomName, "ko"));
}

async function listReservations(date?: string): Promise<MeetingReservation[]> {
  await ensureWorkSeedData();
  const query = date ? db.collection(MEETING_RESERVATIONS).where("meetingDate", "==", date) : db.collection(MEETING_RESERVATIONS);
  const snapshot = await query.get();
  return snapshot.docs
    .map((doc) => normalizeReservation(doc.data() as JsonMap))
    .sort((left, right) => {
      if (left.meetingDate !== right.meetingDate) {
        return left.meetingDate.localeCompare(right.meetingDate);
      }
      if (left.startTime !== right.startTime) {
        return left.startTime - right.startTime;
      }
      return left.roomName.localeCompare(right.roomName, "ko");
    });
}

async function listCalendarEventsRaw(): Promise<CalendarEvent[]> {
  await ensureWorkSeedData();
  const snapshot = await db.collection(CALENDAR_EVENTS).get();
  return snapshot.docs.map((doc) => normalizeCalendarEvent(doc.data() as JsonMap));
}

async function listFavoriteUsersFor(userId: string): Promise<FavoriteUser[]> {
  await ensureWorkSeedData();
  const snapshot = await db.collection(COLLECTIONS.sessionUsers).doc(userId).collection("favoriteUsers").get();
  return snapshot.docs
    .map((doc) => normalizeFavorite(doc.data() as JsonMap))
    .sort((left, right) => left.userNm.localeCompare(right.userNm, "ko"));
}

async function listCommunitiesData(): Promise<CommunityItem[]> {
  await ensureWorkSeedData();
  const snapshot = await db.collection(COMMUNITIES).get();
  return snapshot.docs.map((doc) => normalizeCommunity(doc.data() as JsonMap));
}

function eventOverlaps(start: string, end: string, filterStart: string, filterEnd: string): boolean {
  const startValue = start ? new Date(start).getTime() : 0;
  const endValue = end ? new Date(end).getTime() : startValue;
  if (!Number.isFinite(startValue) || !Number.isFinite(endValue)) {
    return false;
  }
  if (!filterStart && !filterEnd) {
    return true;
  }
  const rangeStart = filterStart ? new Date(filterStart).getTime() : Number.MIN_SAFE_INTEGER;
  const rangeEnd = filterEnd ? new Date(`${filterEnd}T23:59:59`).getTime() : Number.MAX_SAFE_INTEGER;
  return startValue <= rangeEnd && endValue >= rangeStart;
}

function matchesEventKeyword(event: CalendarEvent, keyword: string): boolean {
  const normalized = keyword.toLowerCase().trim();
  if (!normalized) {
    return true;
  }
  return [
    event.title,
    event.description,
    event.placeText,
    event.communityNm,
    event.projectNm,
    event.ownerUserNm,
    event.deptNm,
  ].some((field) => field.toLowerCase().includes(normalized));
}

function asSourceGroups(value: string): Set<string> {
  return new Set(
    trimmed(value)
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean)
  );
}

function filterCommunitiesForUser(items: CommunityItem[], userId: string, params: JsonMap) {
  const keyword = trimmed(params.q).toLowerCase();
  const view = trimmed(params.view) || "joined";
  const manageable = String(params.manageable || "") === "true" || params.manageable === true;

  return items
    .map((community) => {
      const membership = community.memberships.find((item) => item.userId === userId) || null;
      const favoriteYn = community.favoriteUserIds.includes(userId) ? "Y" : "N";
      return {
        communityId: community.communityId,
        communityNm: community.communityNm,
        communityDesc: community.communityDesc,
        communityTypeCd: community.communityTypeCd,
        visibilityCd: community.visibilityCd,
        joinPolicyCd: community.joinPolicyCd,
        introText: community.introText,
        postTemplateHtml: community.postTemplateHtml,
        memberStatusCd: membership?.memberStatusCd || "discover",
        roleCd: membership?.roleCd || "",
        favoriteYn,
        manageable: membership ? ["owner", "operator"].includes(membership.roleCd) : false,
        memberCount: community.memberships.filter((item) => item.memberStatusCd === "active").length,
      };
    })
    .filter((item) => {
      if (keyword && ![item.communityNm, item.communityDesc].some((value) => value.toLowerCase().includes(keyword))) {
        return false;
      }
      if (view === "joined") {
        return item.memberStatusCd === "active";
      }
      if (view === "favorites") {
        return item.favoriteYn === "Y";
      }
      if (view === "discover") {
        return item.memberStatusCd !== "active";
      }
      if (manageable || view === "manageable") {
        return item.manageable;
      }
      return true;
    })
    .sort((left, right) => left.communityNm.localeCompare(right.communityNm, "ko"));
}

function mapFavoriteUser(targetUserId: string, directoryUser: Awaited<ReturnType<typeof getDirectoryUser>>): FavoriteUser {
  return {
    targetUserId,
    userNm: directoryUser?.userNm || targetUserId,
    deptNm: directoryUser?.deptNm || "",
    jbgdNm: directoryUser?.jbgdNm || "",
    createdAt: nowIso(),
  };
}

async function buildCalendarItems(currentUserId: string, currentUserName: string, params: JsonMap) {
  const [storedEvents, favoriteUsers, communities, projects] = await Promise.all([
    listCalendarEventsRaw(),
    listFavoriteUsersFor(currentUserId),
    listCommunitiesData(),
    listProjects(),
  ]);

  const sourceGroups = asSourceGroups(trimmed(params.sourceGroups) || "my,org,community,subscription,todo,team");
  const ownerUserId = trimmed(params.ownerUserId);
  const communityId = trimmed(params.communityId);
  const projectId = trimmed(params.projectId);
  const keyword = trimmed(params.q);
  const anchorDate = trimmed(params.anchorDate) || nowIso().slice(0, 10);
  const filterStart = trimmed(params.startDate) || startOfMonth(anchorDate);
  const filterEnd = trimmed(params.endDate) || endOfMonth(anchorDate);

  const userEvents = storedEvents
    .filter((event) => event.sourceCd === "user_schedule" && event.ownerUserId === currentUserId)
    .map((event) => normalizeCalendarEvent(event, { canEdit: true, canDelete: true, sourceGroupCd: "my" }));

  const deptEvents = storedEvents
    .filter((event) => event.sourceCd === "dept_schedule")
    .map((event) => normalizeCalendarEvent(event, { canEdit: event.ownerUserId === currentUserId, canDelete: event.ownerUserId === currentUserId, sourceGroupCd: "org" }));

  const communityEvents = storedEvents
    .filter((event) => event.sourceCd === "community_schedule")
    .filter((event) => communities.some((community) => community.communityId === event.communityId && community.memberships.some((member) => member.userId === currentUserId && member.memberStatusCd === "active")))
    .map((event) => normalizeCalendarEvent(event, { canEdit: false, canDelete: false, sourceGroupCd: "community" }));

  const subscriptionEvents = favoriteUsers.flatMap((favorite) =>
    storedEvents
      .filter((event) => event.sourceCd === "user_schedule" && event.ownerUserId === favorite.targetUserId)
      .map((event) =>
        normalizeCalendarEvent(
          {
            ...event,
            eventKey: `subscription_${favorite.targetUserId}_${event.eventKey}`,
            sourceCd: "subscription_schedule",
            sourceGroupCd: "subscription",
            colorCd: CALENDAR_COLORS.subscription_schedule,
          },
          { canEdit: false, canDelete: false }
        )
      )
  );

  const teamEvents = projects.map((project) =>
    normalizeCalendarEvent({
      eventKey: `project_${project.bizId}`,
      sourceCd: "team_schedule",
      sourceGroupCd: "team",
      title: project.bizNm,
      description: project.bizGoal,
      startDt: project.strtBizDt,
      endDt: project.endBizDt || project.strtBizDt,
      alldayYn: "Y",
      colorCd: CALENDAR_COLORS.team_schedule,
      ownerUserId: project.bizPicId,
      ownerUserNm: project.bizPicNm,
      projectId: project.bizId,
      projectNm: project.bizNm,
      statusLabel: project.bizSttsCd,
      canEdit: false,
      canDelete: false,
      createdAt: project.createdAt,
      updatedAt: project.updatedAt,
    })
  );

  const todoEvents: CalendarEvent[] = [
    normalizeCalendarEvent({
      eventKey: `todo_${currentUserId}`,
      sourceCd: "user_todo",
      sourceGroupCd: "todo",
      title: `${currentUserName} 할 일 정리`,
      description: "이번 주 우선순위 할 일을 정리합니다.",
      startDt: `${anchorDate}T09:00:00.000Z`,
      endDt: `${anchorDate}T10:00:00.000Z`,
      alldayYn: "N",
      colorCd: CALENDAR_COLORS.user_todo,
      ownerUserId: currentUserId,
      ownerUserNm: currentUserName,
      canEdit: false,
      canDelete: false,
      createdAt: nowIso(),
      updatedAt: nowIso(),
    }),
  ];

  return [...userEvents, ...deptEvents, ...communityEvents, ...subscriptionEvents, ...teamEvents, ...todoEvents]
    .filter((event) => sourceGroups.has(event.sourceGroupCd))
    .filter((event) => !ownerUserId || event.ownerUserId === ownerUserId)
    .filter((event) => !communityId || event.communityId === communityId)
    .filter((event) => !projectId || event.projectId === projectId)
    .filter((event) => eventOverlaps(event.startDt, event.endDt || event.startDt, filterStart, filterEnd))
    .filter((event) => matchesEventKeyword(event, keyword))
    .sort((left, right) => left.startDt.localeCompare(right.startDt));
}

export const projectGetList = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const items = await listProjects();
  return items.map((project) => ({
    bizId: project.bizId,
    bizNm: project.bizNm,
    bizTypeCd: project.bizTypeCd,
    bizSttsCd: project.bizSttsCd,
    bizGoal: project.bizGoal,
    bizDetail: project.bizDetail,
    bizScope: project.bizScope,
    bizBdgt: project.bizBdgt,
    bizPrgrs: project.bizPrgrs,
    strtBizDt: project.strtBizDt,
    endBizDt: project.endBizDt,
    bizPicId: project.bizPicId,
    bizPicNm: project.bizPicNm,
  }));
});

export const projectGetDetail = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const bizId = trimmed(asObject(request.data).bizId);
  const project = await getProjectOrThrow(bizId);
  return {
    project,
    members: listProjectMembers(project),
    currentUserAuthCd: currentProjectAuth(project, currentUser.userId),
  };
});

export const projectCreate = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  if (!trimmed(payload.bizNm) || !trimmed(payload.bizGoal)) {
    throw new HttpsError("invalid-argument", "Project name and goal are required.");
  }
  const project = buildProjectPayload(payload, currentUser.userId, currentUser.userNm);
  project.members = await buildProjectMembers(currentUser.userId, Array.isArray(payload.members) ? payload.members : []);
  await saveProject(project);
  return { bizId: project.bizId };
});

export const projectUpdate = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const bizId = trimmed(data.bizId);
  const existing = await getProjectOrThrow(bizId);
  if (!["B101", "B102"].includes(currentProjectAuth(existing, currentUser.userId))) {
    throw new HttpsError("permission-denied", "No permission to update project.");
  }
  const payload = asObject(data.payload);
  const project = buildProjectPayload({ ...existing, ...payload }, existing.bizPicId || currentUser.userId, existing.bizPicNm || currentUser.userNm, existing);
  project.members = await buildProjectMembers(existing.bizPicId || currentUser.userId, Array.isArray(payload.members) ? payload.members : existing.members, existing);
  await saveProject(project);
  return { bizId: project.bizId };
});

export const projectSetStatus = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const bizId = trimmed(data.bizId);
  const bizSttsCd = trimmed(data.bizSttsCd);
  const project = await getProjectOrThrow(bizId);
  if (currentProjectAuth(project, currentUser.userId) !== "B101") {
    throw new HttpsError("permission-denied", "Only PM can change project status.");
  }
  project.bizSttsCd = bizSttsCd || project.bizSttsCd;
  project.updatedAt = nowIso();
  await saveProject(project);
  return { bizId: project.bizId, bizSttsCd: project.bizSttsCd };
});

export const projectGetTasks = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const bizId = trimmed(asObject(request.data).bizId);
  const project = await getProjectOrThrow(bizId);
  return {
    tasks: await listProjectTasks(bizId),
    currentUserAuthCd: currentProjectAuth(project, currentUser.userId),
  };
});

export const projectCreateTask = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const bizId = trimmed(data.bizId);
  const project = await getProjectOrThrow(bizId);
  if (!["B101", "B102"].includes(currentProjectAuth(project, currentUser.userId))) {
    throw new HttpsError("permission-denied", "No permission to create task.");
  }
  const payload = asObject(data.payload);
  const assigneeId = trimmed(payload.bizUserId);
  const assignee = await getDirectoryUser(assigneeId);
  const task: ProjectTask = {
    taskId: `TASK_${db.collection(PROJECT_TASKS).doc().id}`,
    bizId,
    taskNm: trimmed(payload.taskNm),
    bizUserId: assigneeId,
    bizUserNm: assignee?.userNm || assigneeId,
    taskSttsCd: trimmed(payload.taskSttsCd) || "B401",
    strtTaskDt: trimmed(payload.strtTaskDt),
    endTaskDt: trimmed(payload.endTaskDt),
    taskDetail: trimmed(payload.taskDetail),
    taskPrgrs: numberValue(payload.taskPrgrs, 0),
    createdAt: nowIso(),
    updatedAt: nowIso(),
  };
  await db.collection(PROJECT_TASKS).doc(task.taskId).set(task);
  return task;
});

export const projectUpdateTask = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const data = asObject(request.data);
  const taskId = trimmed(data.taskId);
  const snapshot = await db.collection(PROJECT_TASKS).doc(taskId).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "Task not found.");
  }
  const task = normalizeTask(snapshot.data() as JsonMap);
  const payload = asObject(data.payload);
  const assigneeId = trimmed(payload.bizUserId) || task.bizUserId;
  const assignee = await getDirectoryUser(assigneeId);
  const nextTask: ProjectTask = {
    ...task,
    taskNm: trimmed(payload.taskNm) || task.taskNm,
    bizUserId: assigneeId,
    bizUserNm: assignee?.userNm || task.bizUserNm,
    taskSttsCd: trimmed(payload.taskSttsCd) || task.taskSttsCd,
    strtTaskDt: trimmed(payload.strtTaskDt) || task.strtTaskDt,
    endTaskDt: trimmed(payload.endTaskDt) || task.endTaskDt,
    taskDetail: trimmed(payload.taskDetail) || task.taskDetail,
    taskPrgrs: payload.taskPrgrs === undefined ? task.taskPrgrs : numberValue(payload.taskPrgrs, task.taskPrgrs),
    updatedAt: nowIso(),
  };
  await db.collection(PROJECT_TASKS).doc(task.taskId).set(nextTask, { merge: true });
  return nextTask;
});

export const projectSetTaskStatus = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const data = asObject(request.data);
  const taskId = trimmed(data.taskId);
  const snapshot = await db.collection(PROJECT_TASKS).doc(taskId).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "Task not found.");
  }
  const task = normalizeTask(snapshot.data() as JsonMap);
  task.taskSttsCd = trimmed(data.taskSttsCd) || task.taskSttsCd;
  task.updatedAt = nowIso();
  await db.collection(PROJECT_TASKS).doc(task.taskId).set(task, { merge: true });
  return task;
});

export const projectDeleteTask = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const taskId = trimmed(asObject(request.data).taskId);
  await db.collection(PROJECT_TASKS).doc(taskId).delete().catch(() => undefined);
  return { deleted: true };
});

export const attendanceToday = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const data = asObject(request.data);
  const userId = trimmed(data.userId);
  const workYmd = trimmed(data.workYmd);
  const snapshot = await db.collection(ATTENDANCE).doc(`${userId}_${workYmd}`).get();
  return snapshot.exists ? normalizeAttendance(snapshot.data() as JsonMap) : null;
});

export const attendanceHistory = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const userId = trimmed(asObject(request.data).userId);
  return {
    listTAA: await listAttendanceRecords(userId),
  };
});

export const attendanceWeek = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const records = await listAttendanceRecords(currentUser.userId);
  return {
    uwaDTO: summarizeAttendance(records.filter((item) => withinWeek(item.workYmd))),
  };
});

export const attendanceMonth = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const records = await listAttendanceRecords(currentUser.userId);
  return {
    umaDTO: summarizeAttendance(records.filter((item) => withinMonth(item.workYmd))),
  };
});

export const attendanceMonthList = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const records = await listAttendanceRecords(currentUser.userId);
  return {
    list: records.filter((item) => withinMonth(item.workYmd)),
  };
});

export const attendanceDepart = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const records = await listAttendanceRecords();
  const users = await listDirectoryUsersData();
  const today = dateKey();
  const todayRecords = new Map(records.filter((item) => item.workYmd === today).map((item) => [`${item.userId}_${item.workYmd}`, item]));

  const departmentUsers = users.filter((item) => item.deptId === currentUser.deptId);
  const items = departmentUsers.map((member) => {
    const record = todayRecords.get(`${member.userId}_${today}`);
    return {
      userId: member.userId,
      userNm: member.userNm,
      workBgngDt: record?.workBgngDt || "",
      workEndDt: record?.workEndDt || "",
      workHr: record?.workHr || 0,
      workSttsCd: record?.workSttsCd || member.workSttsCd || "C103",
      workYmd: record?.workYmd || today,
      lateYn: record?.lateYn || "N",
    };
  });
  return { adsDTOList: items };
});

export const attendanceClockIn = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const workYmd = dateKey();
  const current = await ensureAttendanceUserRecord(currentUser, workYmd);
  if (current.workBgngDt) {
    return current;
  }
  const startAt = nowIso();
  const nextRecord: AttendanceRecord = {
    ...current,
    userNm: currentUser.userNm,
    deptId: currentUser.deptId,
    deptNm: currentUser.deptNm,
    workBgngDt: startAt,
    workSttsCd: "C101",
    lateYn: new Date(startAt).getHours() >= 9 ? "Y" : "N",
    updatedAt: nowIso(),
  };
  await db.collection(ATTENDANCE).doc(`${currentUser.userId}_${workYmd}`).set(nextRecord, { merge: true });
  return nextRecord;
});

export const attendanceClockOut = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const workYmd = trimmed(asObject(request.data).workYmd) || dateKey();
  const current = await ensureAttendanceUserRecord(currentUser, workYmd);
  const endAt = nowIso();
  const nextRecord: AttendanceRecord = {
    ...current,
    userNm: currentUser.userNm,
    deptId: currentUser.deptId,
    deptNm: currentUser.deptNm,
    workBgngDt: current.workBgngDt || endAt,
    workEndDt: endAt,
    workSttsCd: "C103",
    updatedAt: nowIso(),
    workHr: computeWorkMinutes({
      ...current,
      workBgngDt: current.workBgngDt || endAt,
      workEndDt: endAt,
      updatedAt: nowIso(),
    }),
  };
  await db.collection(ATTENDANCE).doc(`${currentUser.userId}_${workYmd}`).set(nextRecord, { merge: true });
  return nextRecord;
});

export const meetingGetRooms = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  return listMeetingRoomsData();
});

export const meetingCreateRoom = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const payload = asObject(request.data);
  const room: MeetingRoom = {
    roomId: `ROOM_${db.collection(MEETING_ROOMS).doc().id}`,
    roomName: trimmed(payload.roomName),
    location: trimmed(payload.location),
    capacity: numberValue(payload.capacity, 6),
    useYn: trimmed(payload.useYn) || "Y",
    createdAt: nowIso(),
    updatedAt: nowIso(),
  };
  await db.collection(MEETING_ROOMS).doc(room.roomId).set(room);
  return room;
});

export const meetingGetReservations = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const date = trimmed(asObject(request.data).date);
  return listReservations(date);
});

export const meetingGetDetail = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const reservationId = trimmed(asObject(request.data).reservationId);
  const snapshot = await db.collection(MEETING_RESERVATIONS).doc(reservationId).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "Reservation not found.");
  }
  return normalizeReservation(snapshot.data() as JsonMap);
});

function hasReservationConflict(reservations: MeetingReservation[], next: MeetingReservation, excludeReservationId = ""): boolean {
  return reservations.some((reservation) =>
    reservation.reservationId !== excludeReservationId
    && reservation.roomId === next.roomId
    && reservation.meetingDate === next.meetingDate
    && next.startTime < reservation.endTime
    && next.endTime > reservation.startTime
  );
}

export const meetingCreateReservation = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const roomId = trimmed(payload.roomId);
  const roomSnapshot = await db.collection(MEETING_ROOMS).doc(roomId).get();
  if (!roomSnapshot.exists) {
    throw new HttpsError("not-found", "Meeting room not found.");
  }
  const room = normalizeMeetingRoom(roomSnapshot.data() as JsonMap);
  const reservation: MeetingReservation = {
    reservationId: `RSV_${db.collection(MEETING_RESERVATIONS).doc().id}`,
    roomId: room.roomId,
    roomName: room.roomName,
    title: trimmed(payload.title) || "제목 없음",
    userId: currentUser.userId,
    userNm: currentUser.userNm,
    meetingDate: toDateInput(trimmed(payload.meetingDate)),
    startTime: numberValue(payload.startTime, 9),
    endTime: numberValue(payload.endTime, 10),
    createdAt: nowIso(),
    updatedAt: nowIso(),
  };
  if (reservation.startTime >= reservation.endTime) {
    throw new HttpsError("invalid-argument", "End time must be after start time.");
  }
  const reservations = await listReservations(reservation.meetingDate);
  if (hasReservationConflict(reservations, reservation)) {
    throw new HttpsError("already-exists", "Meeting room is already booked for this time.");
  }
  await db.collection(MEETING_RESERVATIONS).doc(reservation.reservationId).set(reservation);
  return reservation;
});

export const meetingUpdateReservation = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const data = asObject(request.data);
  const reservationId = trimmed(data.reservationId);
  const snapshot = await db.collection(MEETING_RESERVATIONS).doc(reservationId).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "Reservation not found.");
  }
  const current = normalizeReservation(snapshot.data() as JsonMap);
  const payload = asObject(data.payload);
  const nextReservation: MeetingReservation = {
    ...current,
    title: trimmed(payload.title) || current.title,
    meetingDate: toDateInput(trimmed(payload.meetingDate) || current.meetingDate),
    startTime: payload.startTime === undefined ? current.startTime : numberValue(payload.startTime, current.startTime),
    endTime: payload.endTime === undefined ? current.endTime : numberValue(payload.endTime, current.endTime),
    updatedAt: nowIso(),
  };
  const reservations = await listReservations(nextReservation.meetingDate);
  if (hasReservationConflict(reservations, nextReservation, reservationId)) {
    throw new HttpsError("already-exists", "Meeting room is already booked for this time.");
  }
  await db.collection(MEETING_RESERVATIONS).doc(reservationId).set(nextReservation, { merge: true });
  return nextReservation;
});

export const meetingDeleteReservation = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const reservationId = trimmed(asObject(request.data).reservationId);
  await db.collection(MEETING_RESERVATIONS).doc(reservationId).delete().catch(() => undefined);
  return { deleted: true };
});

export const calendarGetEvents = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  return {
    items: await buildCalendarItems(currentUser.userId, currentUser.userNm, asObject(request.data)),
  };
});

export const calendarCreateUser = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const event: CalendarEvent = normalizeCalendarEvent({
    eventKey: `user_${db.collection(CALENDAR_EVENTS).doc().id}`,
    sourceCd: "user_schedule",
    sourceGroupCd: "my",
    title: trimmed(payload.schdTtl),
    description: trimmed(payload.userSchdExpln),
    startDt: trimmed(payload.schdStrtDt),
    endDt: trimmed(payload.schdEndDt),
    alldayYn: trimmed(payload.allday) || "N",
    colorCd: CALENDAR_COLORS.user_schedule,
    ownerUserId: currentUser.userId,
    ownerUserNm: currentUser.userNm,
    deptId: currentUser.deptId,
    deptNm: currentUser.deptNm,
    canEdit: true,
    canDelete: true,
    createdAt: nowIso(),
    updatedAt: nowIso(),
  });
  await db.collection(CALENDAR_EVENTS).doc(event.eventKey).set(event);
  return event;
});

export const calendarUpdateUser = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const eventKey = trimmed(data.eventKey);
  const snapshot = await db.collection(CALENDAR_EVENTS).doc(eventKey).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "Calendar event not found.");
  }
  const current = normalizeCalendarEvent(snapshot.data() as JsonMap);
  if (current.ownerUserId !== currentUser.userId) {
    throw new HttpsError("permission-denied", "No permission to update event.");
  }
  const payload = asObject(data.payload);
  const nextEvent = normalizeCalendarEvent({
    ...current,
    title: trimmed(payload.schdTtl) || current.title,
    description: trimmed(payload.userSchdExpln) || current.description,
    startDt: trimmed(payload.schdStrtDt) || current.startDt,
    endDt: trimmed(payload.schdEndDt) || current.endDt,
    alldayYn: trimmed(payload.allday) || current.alldayYn,
    updatedAt: nowIso(),
  }, current);
  await db.collection(CALENDAR_EVENTS).doc(eventKey).set(nextEvent, { merge: true });
  return nextEvent;
});

export const calendarDeleteUser = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const eventKey = trimmed(asObject(request.data).eventKey);
  const snapshot = await db.collection(CALENDAR_EVENTS).doc(eventKey).get();
  if (!snapshot.exists) {
    return { deleted: true };
  }
  const current = normalizeCalendarEvent(snapshot.data() as JsonMap);
  if (current.ownerUserId !== currentUser.userId) {
    throw new HttpsError("permission-denied", "No permission to delete event.");
  }
  await db.collection(CALENDAR_EVENTS).doc(eventKey).delete();
  return { deleted: true };
});

export const calendarCreateDept = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const event: CalendarEvent = normalizeCalendarEvent({
    eventKey: `dept_${db.collection(CALENDAR_EVENTS).doc().id}`,
    sourceCd: "dept_schedule",
    sourceGroupCd: "org",
    title: trimmed(payload.schdTtl),
    description: trimmed(payload.deptSchdExpln),
    startDt: trimmed(payload.schdStrtDt),
    endDt: trimmed(payload.schdEndDt),
    alldayYn: trimmed(payload.allday) || "N",
    colorCd: CALENDAR_COLORS.dept_schedule,
    ownerUserId: currentUser.userId,
    ownerUserNm: currentUser.userNm,
    deptId: currentUser.deptId,
    deptNm: currentUser.deptNm,
    canEdit: true,
    canDelete: true,
    createdAt: nowIso(),
    updatedAt: nowIso(),
  });
  await db.collection(CALENDAR_EVENTS).doc(event.eventKey).set(event);
  return event;
});

export const calendarUpdateDept = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const eventKey = trimmed(data.eventKey);
  const snapshot = await db.collection(CALENDAR_EVENTS).doc(eventKey).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "Calendar event not found.");
  }
  const current = normalizeCalendarEvent(snapshot.data() as JsonMap);
  if (current.deptId !== currentUser.deptId) {
    throw new HttpsError("permission-denied", "No permission to update department event.");
  }
  const payload = asObject(data.payload);
  const nextEvent = normalizeCalendarEvent({
    ...current,
    title: trimmed(payload.schdTtl) || current.title,
    description: trimmed(payload.deptSchdExpln) || current.description,
    startDt: trimmed(payload.schdStrtDt) || current.startDt,
    endDt: trimmed(payload.schdEndDt) || current.endDt,
    alldayYn: trimmed(payload.allday) || current.alldayYn,
    updatedAt: nowIso(),
  }, current);
  await db.collection(CALENDAR_EVENTS).doc(eventKey).set(nextEvent, { merge: true });
  return nextEvent;
});

export const calendarDeleteDept = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const eventKey = trimmed(asObject(request.data).eventKey);
  const snapshot = await db.collection(CALENDAR_EVENTS).doc(eventKey).get();
  if (!snapshot.exists) {
    return { deleted: true };
  }
  const current = normalizeCalendarEvent(snapshot.data() as JsonMap);
  if (current.deptId !== currentUser.deptId) {
    throw new HttpsError("permission-denied", "No permission to delete department event.");
  }
  await db.collection(CALENDAR_EVENTS).doc(eventKey).delete();
  return { deleted: true };
});

export const calendarTeamProjects = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  return listProjects();
});

export const dashboardFavoriteUsers = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  return listFavoriteUsersFor(currentUser.userId);
});

export const dashboardAddFavoriteUser = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const targetUserId = trimmed(asObject(request.data).targetUserId);
  if (!targetUserId) {
    throw new HttpsError("invalid-argument", "targetUserId is required.");
  }
  const favorite = mapFavoriteUser(targetUserId, await getDirectoryUser(targetUserId));
  await db.collection(COLLECTIONS.sessionUsers).doc(currentUser.userId).collection("favoriteUsers").doc(targetUserId).set(favorite, { merge: true });
  return favorite;
});

export const dashboardRemoveFavoriteUser = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const targetUserId = trimmed(asObject(request.data).targetUserId);
  await db.collection(COLLECTIONS.sessionUsers).doc(currentUser.userId).collection("favoriteUsers").doc(targetUserId).delete().catch(() => undefined);
  return { deleted: true };
});

export const communityGetList = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const communities = await listCommunitiesData();
  return filterCommunitiesForUser(communities, currentUser.userId, asObject(request.data));
});
