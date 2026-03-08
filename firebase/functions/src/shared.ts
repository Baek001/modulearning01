import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";

initializeApp();

export const db = getFirestore();

export type JsonMap = Record<string, unknown>;
export type CallableAuth = {
  uid?: string;
  token?: Record<string, unknown>;
} | null | undefined;

export type UserProfile = {
  userId: string;
  userNm: string;
  userEmail: string;
  userTelno: string;
  extTel: string;
  deptId: string;
  deptNm: string;
  jbgdNm: string;
  userRole: string;
  hireYmd: string;
  workSttsCd: string;
  rsgntnYn: string;
  rsgntnYmd: string;
  profileImageUrl: string;
  profileImagePath: string;
  firebaseUid?: string;
};

export type Department = {
  deptId: string;
  deptNm: string;
  upDeptId: string | null;
  sortNum: number;
  useYn: string;
};

export type CommonCode = {
  codeId: string;
  codeGrpId: string;
  codeNm: string;
  sortOrder: number;
  useYn: string;
};

export type ApprovalTemplate = {
  atrzDocTmplId: string;
  atrzDocCd: string;
  atrzDocTmplNm: string;
  htmlContents: string;
  atrzSaveYear: string;
  atrzCategory: string;
  atrzDescription: string;
};

export type ApprovalAttachment = {
  fileId: string;
  fileSeq: number;
  orgnFileNm: string;
  saveFileNm: string;
  filePath: string;
  fileSize: number;
  contentType?: string;
};

export type ApprovalLine = {
  atrzLineSqn: number;
  atrzApprUserId: string;
  atrzApprUserNm: string;
  deptNm: string;
  jbgdNm: string;
  atrzApprStts: string;
  atrzOpnn: string;
  prcsDt: string;
};

export type ApprovalReceiver = {
  atrzRcvrId: string;
  userNm: string;
  deptNm: string;
  jbgdNm: string;
};

export type ApprovalDocument = {
  atrzDocId: string;
  atrzDocTmplId: string;
  atrzDocCd: string;
  atrzDocTmplNm: string;
  atrzDocTtl: string;
  htmlData: string;
  openYn: string;
  atrzUserId: string;
  drafterName: string;
  atrzSbmtDt: string;
  crntAtrzStepCd: string;
  approvalLines: ApprovalLine[];
  receivers: ApprovalReceiver[];
  attachments: ApprovalAttachment[];
  currentSeq: number;
  lineSqn: number;
  updatedAt: string;
};

export type ApprovalTemp = {
  atrzTempSqn: string;
  atrzDocTmplId: string;
  atrzDocTtl: string;
  htmlData: string;
  openYn: string;
  atrzUserId: string;
  drafterName: string;
  atrzSbmtDt: string;
  atrzFileId: string;
  attachments: ApprovalAttachment[];
  updatedAt: string;
};

export type MessengerPayload = {
  roomId: string;
  messageText: string;
  msgType: string;
  attachments: Array<Record<string, unknown>>;
};

export type RoomMember = {
  id: string;
  unreadCount?: number;
  lastReadAt?: unknown;
  notifyEnabled?: boolean;
};

export const PAGE_SIZE = 10;

export const COLLECTIONS = {
  bootstrap: "system/bootstrap",
  sessionUsers: "users",
  directoryUsers: "orgUsers",
  departments: "departments",
  commonCodes: "commonCodes",
  approvalTemplates: "approvalTemplates",
  approvalDocuments: "approvalDocuments",
  approvalTemps: "approvalTemps",
  approvalCustomLines: "approvalCustomLines",
  messengerRooms: "messengerRooms",
} as const;

export const DEFAULT_DEPARTMENTS: Department[] = [
  { deptId: "DP001000", deptNm: "본사", upDeptId: null, sortNum: 1, useYn: "Y" },
  { deptId: "DP001001", deptNm: "운영", upDeptId: "DP001000", sortNum: 1, useYn: "Y" },
  { deptId: "DP001002", deptNm: "개발", upDeptId: "DP001000", sortNum: 2, useYn: "Y" },
];

export const DEFAULT_DIRECTORY_USERS: UserProfile[] = [
  {
    userId: "admin",
    userNm: "관리자",
    userEmail: "admin@starworks.local",
    userTelno: "010-0000-0000",
    extTel: "1001",
    deptId: "DP001001",
    deptNm: "운영",
    jbgdNm: "부장",
    userRole: "ROLE_ADMIN",
    hireYmd: "2026-03-06",
    workSttsCd: "C103",
    rsgntnYn: "N",
    rsgntnYmd: "",
    profileImageUrl: "",
    profileImagePath: "",
  },
  {
    userId: "user01",
    userNm: "테스트 사용자",
    userEmail: "user01@starworks.local",
    userTelno: "010-0000-0001",
    extTel: "1002",
    deptId: "DP001002",
    deptNm: "개발",
    jbgdNm: "사원",
    userRole: "ROLE_USER",
    hireYmd: "2026-03-06",
    workSttsCd: "C103",
    rsgntnYn: "N",
    rsgntnYmd: "",
    profileImageUrl: "",
    profileImagePath: "",
  },
  {
    userId: "user02",
    userNm: "테스트 사용자 2",
    userEmail: "user02@starworks.local",
    userTelno: "010-0000-0002",
    extTel: "1003",
    deptId: "DP001002",
    deptNm: "개발",
    jbgdNm: "사원",
    userRole: "ROLE_USER",
    hireYmd: "2026-03-06",
    workSttsCd: "C103",
    rsgntnYn: "N",
    rsgntnYmd: "",
    profileImageUrl: "",
    profileImagePath: "",
  },
];

export const DEFAULT_COMMON_CODES: CommonCode[] = [
  { codeId: "B101", codeGrpId: "B1", codeNm: "책임자", sortOrder: 1, useYn: "Y" },
  { codeId: "B102", codeGrpId: "B1", codeNm: "팀원", sortOrder: 2, useYn: "Y" },
  { codeId: "B103", codeGrpId: "B1", codeNm: "열람자", sortOrder: 3, useYn: "Y" },
  { codeId: "B201", codeGrpId: "B2", codeNm: "일반", sortOrder: 1, useYn: "Y" },
  { codeId: "B202", codeGrpId: "B2", codeNm: "신규 구축", sortOrder: 2, useYn: "Y" },
  { codeId: "B203", codeGrpId: "B2", codeNm: "유지보수", sortOrder: 3, useYn: "Y" },
  { codeId: "B301", codeGrpId: "B3", codeNm: "승인 대기", sortOrder: 1, useYn: "Y" },
  { codeId: "B302", codeGrpId: "B3", codeNm: "진행", sortOrder: 2, useYn: "Y" },
  { codeId: "B303", codeGrpId: "B3", codeNm: "보류", sortOrder: 3, useYn: "Y" },
  { codeId: "B304", codeGrpId: "B3", codeNm: "완료", sortOrder: 4, useYn: "Y" },
  { codeId: "B305", codeGrpId: "B3", codeNm: "취소", sortOrder: 5, useYn: "Y" },
  { codeId: "B401", codeGrpId: "B4", codeNm: "미시작", sortOrder: 1, useYn: "Y" },
  { codeId: "B402", codeGrpId: "B4", codeNm: "진행중", sortOrder: 2, useYn: "Y" },
  { codeId: "B403", codeGrpId: "B4", codeNm: "보류", sortOrder: 3, useYn: "Y" },
  { codeId: "B404", codeGrpId: "B4", codeNm: "완료", sortOrder: 4, useYn: "Y" },
  { codeId: "C103", codeGrpId: "WORK_STTS", codeNm: "재직", sortOrder: 1, useYn: "Y" },
  { codeId: "C104", codeGrpId: "WORK_STTS", codeNm: "휴직", sortOrder: 2, useYn: "Y" },
  { codeId: "A202", codeGrpId: "APPR_DOC_STTS", codeNm: "결재 대기", sortOrder: 1, useYn: "Y" },
  { codeId: "A203", codeGrpId: "APPR_DOC_STTS", codeNm: "결재 진행", sortOrder: 2, useYn: "Y" },
  { codeId: "A204", codeGrpId: "APPR_DOC_STTS", codeNm: "반려", sortOrder: 3, useYn: "Y" },
  { codeId: "A205", codeGrpId: "APPR_DOC_STTS", codeNm: "기안 회수", sortOrder: 4, useYn: "Y" },
  { codeId: "A206", codeGrpId: "APPR_DOC_STTS", codeNm: "결재 완료", sortOrder: 5, useYn: "Y" },
  { codeId: "A301", codeGrpId: "APPR_LINE_STTS", codeNm: "미열람", sortOrder: 1, useYn: "Y" },
  { codeId: "A302", codeGrpId: "APPR_LINE_STTS", codeNm: "대기", sortOrder: 2, useYn: "Y" },
  { codeId: "A303", codeGrpId: "APPR_LINE_STTS", codeNm: "처리 완료", sortOrder: 3, useYn: "Y" },
  { codeId: "A304", codeGrpId: "APPR_LINE_STTS", codeNm: "반려", sortOrder: 4, useYn: "Y" },
];

export const DEFAULT_APPROVAL_TEMPLATES: ApprovalTemplate[] = [
  {
    atrzDocTmplId: "ATRZTEMP001",
    atrzDocCd: "VAC_REQ",
    atrzDocTmplNm: "휴가 신청서",
    atrzCategory: "hr",
    atrzSaveYear: "5",
    atrzDescription: "연차, 반차, 병가 신청용 문서입니다.",
    htmlContents: "<section><h2>휴가 신청서</h2><table><tr><th>휴가 종류</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>사유</th><td></td></tr><tr><th>업무 인수자</th><td></td></tr></table></section>",
  },
  {
    atrzDocTmplId: "ATRZTEMP002",
    atrzDocCd: "TRIP_REQ",
    atrzDocTmplNm: "출장/외근 신청서",
    atrzCategory: "trip",
    atrzSaveYear: "5",
    atrzDescription: "출장 및 외근 승인 요청 문서입니다.",
    htmlContents: "<section><h2>출장/외근 신청서</h2><table><tr><th>방문지</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>목적</th><td></td></tr><tr><th>예상 비용</th><td></td></tr></table></section>",
  },
  {
    atrzDocTmplId: "ATRZTEMP003",
    atrzDocCd: "EXP_APPROVAL",
    atrzDocTmplNm: "지출 결의서",
    atrzCategory: "finance",
    atrzSaveYear: "5",
    atrzDescription: "비용 집행 및 정산 승인 문서입니다.",
    htmlContents: "<section><h2>지출 결의서</h2><table><tr><th>지출 일자</th><td></td></tr><tr><th>금액</th><td></td></tr><tr><th>비용 계정</th><td></td></tr><tr><th>세부 내역</th><td></td></tr></table></section>",
  },
  {
    atrzDocTmplId: "ATRZTEMP004",
    atrzDocCd: "PO_REQUEST",
    atrzDocTmplNm: "구매 품의서",
    atrzCategory: "finance",
    atrzSaveYear: "5",
    atrzDescription: "구매 요청 및 예산 집행 문서입니다.",
    htmlContents: "<section><h2>구매 품의서</h2><table><tr><th>품목</th><td></td></tr><tr><th>수량</th><td></td></tr><tr><th>예산</th><td></td></tr><tr><th>요청 사유</th><td></td></tr></table></section>",
  },
  {
    atrzDocTmplId: "ATRZTEMP005",
    atrzDocCd: "PRO_PROPOSAL",
    atrzDocTmplNm: "프로젝트 기안서",
    atrzCategory: "project",
    atrzSaveYear: "5",
    atrzDescription: "프로젝트 목적, 범위, 일정 계획을 담는 문서입니다.",
    htmlContents: "<section><h2>프로젝트 기안서</h2><table><tr><th>프로젝트명</th><td></td></tr><tr><th>일정</th><td></td></tr><tr><th>목표</th><td></td></tr><tr><th>범위</th><td></td></tr></table></section>",
  },
  {
    atrzDocTmplId: "ATRZTEMP006",
    atrzDocCd: "MKT_REQUEST",
    atrzDocTmplNm: "마케팅 실행 요청서",
    atrzCategory: "marketing",
    atrzSaveYear: "5",
    atrzDescription: "캠페인 대상과 기간, 기대 KPI를 정리하는 문서입니다.",
    htmlContents: "<section><h2>마케팅 실행 요청서</h2><table><tr><th>캠페인명</th><td></td></tr><tr><th>대상</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>기대 KPI</th><td></td></tr></table></section>",
  },
  {
    atrzDocTmplId: "ATRZTEMP007",
    atrzDocCd: "IT_WORK_REQ",
    atrzDocTmplNm: "개발/IT 작업 요청서",
    atrzCategory: "it",
    atrzSaveYear: "5",
    atrzDescription: "개발 및 IT 작업 요청용 문서입니다.",
    htmlContents: "<section><h2>개발/IT 작업 요청서</h2><table><tr><th>시스템</th><td></td></tr><tr><th>우선순위</th><td></td></tr><tr><th>요청 내용</th><td></td></tr><tr><th>희망 완료일</th><td></td></tr></table></section>",
  },
  {
    atrzDocTmplId: "ATRZTEMP008",
    atrzDocCd: "LOG_TRANSFER",
    atrzDocTmplNm: "물류 이동 요청서",
    atrzCategory: "logistics",
    atrzSaveYear: "5",
    atrzDescription: "물류 이동, 재고 전송 요청 문서입니다.",
    htmlContents: "<section><h2>물류 이동 요청서</h2><table><tr><th>출발지</th><td></td></tr><tr><th>도착지</th><td></td></tr><tr><th>품목/수량</th><td></td></tr><tr><th>요청 사유</th><td></td></tr></table></section>",
  },
];

let bootstrapPromise: Promise<void> | null = null;

export function asObject(value: unknown): JsonMap {
  return value && typeof value === "object" ? (value as JsonMap) : {};
}

export function trimmed(value: unknown): string {
  return typeof value === "string" ? value.trim() : "";
}

export function toIso(value: unknown): string {
  if (!value) {
    return "";
  }
  if (typeof value === "string") {
    return value;
  }
  if (value instanceof Date) {
    return value.toISOString();
  }
  if (typeof value === "object" && typeof (value as { toDate?: () => Date }).toDate === "function") {
    return (value as { toDate: () => Date }).toDate().toISOString();
  }
  return "";
}

export function nowIso(): string {
  return new Date().toISOString();
}

export function numberValue(value: unknown, fallback = 0): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function sanitizeDocIdPart(value: string): string {
  return value.replace(/[^a-zA-Z0-9_-]/g, "_");
}

export function resolveUserIdFromToken(uid: string, token: Record<string, unknown> = {}, fallback = ""): string {
  const fromFallback = trimmed(fallback);
  const fromTokenId = trimmed(token.user_id);
  const email = trimmed(token.email);

  if (fromFallback) return fromFallback;
  if (fromTokenId) return fromTokenId;
  if (email.includes("@")) return email.split("@")[0] || uid;
  return uid;
}

export function normalizeDepartment(source: JsonMap, fallback: Partial<Department> = {}): Department {
  return {
    deptId: trimmed(source.deptId) || fallback.deptId || "",
    deptNm: trimmed(source.deptNm) || fallback.deptNm || "",
    upDeptId: trimmed(source.upDeptId) || fallback.upDeptId || null,
    sortNum: numberValue(source.sortNum, fallback.sortNum || 0),
    useYn: trimmed(source.useYn) || fallback.useYn || "Y",
  };
}

export function normalizeUser(source: JsonMap, fallback: Partial<UserProfile> = {}): UserProfile {
  return {
    userId: trimmed(source.userId) || fallback.userId || "",
    userNm: trimmed(source.userNm) || fallback.userNm || "",
    userEmail: trimmed(source.userEmail) || fallback.userEmail || "",
    userTelno: trimmed(source.userTelno) || fallback.userTelno || "",
    extTel: trimmed(source.extTel) || fallback.extTel || "",
    deptId: trimmed(source.deptId) || fallback.deptId || "",
    deptNm: trimmed(source.deptNm) || fallback.deptNm || "",
    jbgdNm: trimmed(source.jbgdNm) || fallback.jbgdNm || "",
    userRole: trimmed(source.userRole) || fallback.userRole || "ROLE_USER",
    hireYmd: trimmed(source.hireYmd) || fallback.hireYmd || "",
    workSttsCd: trimmed(source.workSttsCd) || fallback.workSttsCd || "C103",
    rsgntnYn: trimmed(source.rsgntnYn) || fallback.rsgntnYn || "N",
    rsgntnYmd: trimmed(source.rsgntnYmd) || fallback.rsgntnYmd || "",
    profileImageUrl: trimmed(source.profileImageUrl) || fallback.profileImageUrl || "",
    profileImagePath: trimmed(source.profileImagePath) || fallback.profileImagePath || "",
    firebaseUid: trimmed(source.firebaseUid) || fallback.firebaseUid || "",
  };
}

export function normalizeCommonCode(source: JsonMap, fallback: Partial<CommonCode> = {}): CommonCode {
  return {
    codeId: trimmed(source.codeId) || fallback.codeId || "",
    codeGrpId: trimmed(source.codeGrpId) || fallback.codeGrpId || "",
    codeNm: trimmed(source.codeNm) || fallback.codeNm || "",
    sortOrder: numberValue(source.sortOrder, fallback.sortOrder || 0),
    useYn: trimmed(source.useYn) || fallback.useYn || "Y",
  };
}

export function normalizeAttachment(item: unknown, index = 0): ApprovalAttachment {
  const source = asObject(item);
  return {
    fileId: trimmed(source.fileId) || `FILE_${index + 1}`,
    fileSeq: numberValue(source.fileSeq, index + 1),
    orgnFileNm: trimmed(source.orgnFileNm) || `attachment-${index + 1}`,
    saveFileNm: trimmed(source.saveFileNm) || trimmed(source.orgnFileNm) || `attachment-${index + 1}`,
    filePath: trimmed(source.filePath),
    fileSize: numberValue(source.fileSize, 0),
    contentType: trimmed(source.contentType),
  };
}

export function normalizeApprovalLine(item: unknown, index = 0): ApprovalLine {
  const source = asObject(item);
  return {
    atrzLineSqn: numberValue(source.atrzLineSqn, index + 1),
    atrzApprUserId: trimmed(source.atrzApprUserId) || trimmed(source.atrzApprId) || trimmed(source.userId),
    atrzApprUserNm: trimmed(source.atrzApprUserNm) || trimmed(source.userNm),
    deptNm: trimmed(source.deptNm),
    jbgdNm: trimmed(source.jbgdNm),
    atrzApprStts: trimmed(source.atrzApprStts) || "A301",
    atrzOpnn: trimmed(source.atrzOpnn),
    prcsDt: toIso(source.prcsDt),
  };
}

export function normalizeReceiver(item: unknown): ApprovalReceiver {
  const source = asObject(item);
  return {
    atrzRcvrId: trimmed(source.atrzRcvrId) || trimmed(source.userId),
    userNm: trimmed(source.userNm),
    deptNm: trimmed(source.deptNm),
    jbgdNm: trimmed(source.jbgdNm),
  };
}

export function normalizeTemplate(source: JsonMap, fallback: Partial<ApprovalTemplate> = {}): ApprovalTemplate {
  return {
    atrzDocTmplId: trimmed(source.atrzDocTmplId) || fallback.atrzDocTmplId || "",
    atrzDocCd: trimmed(source.atrzDocCd) || fallback.atrzDocCd || "",
    atrzDocTmplNm: trimmed(source.atrzDocTmplNm) || fallback.atrzDocTmplNm || "",
    htmlContents: trimmed(source.htmlContents) || fallback.htmlContents || "",
    atrzSaveYear: trimmed(source.atrzSaveYear) || fallback.atrzSaveYear || "5",
    atrzCategory: trimmed(source.atrzCategory) || fallback.atrzCategory || "",
    atrzDescription: trimmed(source.atrzDescription) || fallback.atrzDescription || "",
  };
}

export function normalizeDocument(source: JsonMap): ApprovalDocument {
  return {
    atrzDocId: trimmed(source.atrzDocId),
    atrzDocTmplId: trimmed(source.atrzDocTmplId),
    atrzDocCd: trimmed(source.atrzDocCd),
    atrzDocTmplNm: trimmed(source.atrzDocTmplNm),
    atrzDocTtl: trimmed(source.atrzDocTtl),
    htmlData: trimmed(source.htmlData),
    openYn: trimmed(source.openYn) || "N",
    atrzUserId: trimmed(source.atrzUserId),
    drafterName: trimmed(source.drafterName),
    atrzSbmtDt: toIso(source.atrzSbmtDt),
    crntAtrzStepCd: trimmed(source.crntAtrzStepCd) || "A202",
    approvalLines: Array.isArray(source.approvalLines) ? source.approvalLines.map((line, index) => normalizeApprovalLine(line, index)) : [],
    receivers: Array.isArray(source.receivers) ? source.receivers.map((receiver) => normalizeReceiver(receiver)) : [],
    attachments: Array.isArray(source.attachments) ? source.attachments.map((file, index) => normalizeAttachment(file, index)) : [],
    currentSeq: numberValue(source.currentSeq, 1),
    lineSqn: numberValue(source.lineSqn, 1),
    updatedAt: toIso(source.updatedAt),
  };
}

export function normalizeTemp(source: JsonMap): ApprovalTemp {
  return {
    atrzTempSqn: trimmed(source.atrzTempSqn),
    atrzDocTmplId: trimmed(source.atrzDocTmplId),
    atrzDocTtl: trimmed(source.atrzDocTtl),
    htmlData: trimmed(source.htmlData),
    openYn: trimmed(source.openYn) || "N",
    atrzUserId: trimmed(source.atrzUserId),
    drafterName: trimmed(source.drafterName),
    atrzSbmtDt: toIso(source.atrzSbmtDt),
    atrzFileId: trimmed(source.atrzFileId),
    attachments: Array.isArray(source.attachments) ? source.attachments.map((file, index) => normalizeAttachment(file, index)) : [],
    updatedAt: toIso(source.updatedAt),
  };
}

export function assertSignedIn(auth: CallableAuth): string {
  if (!auth?.uid) {
    throw new HttpsError("unauthenticated", "로그인이 필요합니다.");
  }
  return auth.uid;
}

export async function ensureBaselineData(): Promise<void> {
  if (bootstrapPromise) {
    return bootstrapPromise;
  }

  bootstrapPromise = (async () => {
    const bootstrapRef = db.doc(COLLECTIONS.bootstrap);
    const snapshot = await bootstrapRef.get();
    if (snapshot.exists) {
      return;
    }

    const batch = db.batch();

    DEFAULT_DEPARTMENTS.forEach((department) => {
      batch.set(db.collection(COLLECTIONS.departments).doc(department.deptId), { ...department, seededAt: nowIso() });
    });
    DEFAULT_DIRECTORY_USERS.forEach((user) => {
      batch.set(db.collection(COLLECTIONS.directoryUsers).doc(user.userId), { ...user, seededAt: nowIso() });
    });
    DEFAULT_COMMON_CODES.forEach((code) => {
      batch.set(db.collection(COLLECTIONS.commonCodes).doc(`${code.codeGrpId}_${code.codeId}`), { ...code, seededAt: nowIso() });
    });
    DEFAULT_APPROVAL_TEMPLATES.forEach((template) => {
      batch.set(db.collection(COLLECTIONS.approvalTemplates).doc(template.atrzDocTmplId), { ...template, seededAt: nowIso() });
    });
    batch.set(bootstrapRef, { seedVersion: 1, seededAt: nowIso() });

    await batch.commit();
  })();

  return bootstrapPromise;
}
