import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
  ApprovalAttachment,
  ApprovalDocument,
  ApprovalLine,
  ApprovalReceiver,
  ApprovalTemp,
  ApprovalTemplate,
  COLLECTIONS,
  CallableAuth,
  DEFAULT_APPROVAL_TEMPLATES,
  JsonMap,
  PAGE_SIZE,
  asObject,
  assertSignedIn,
  db,
  ensureBaselineData,
  normalizeAttachment,
  normalizeDocument,
  normalizeReceiver,
  normalizeTemplate,
  normalizeTemp,
  nowIso,
  numberValue,
  sanitizeDocIdPart,
  trimmed,
} from "./shared.js";
import { getDirectoryUser, loadProfile } from "./profile.js";

async function listApprovalTemplatesData(): Promise<ApprovalTemplate[]> {
  await ensureBaselineData();
  const snapshot = await db.collection(COLLECTIONS.approvalTemplates).get();
  return snapshot.docs
    .map((doc) => normalizeTemplate(doc.data() as JsonMap))
    .sort((left, right) => left.atrzDocTmplNm.localeCompare(right.atrzDocTmplNm, "ko"));
}

async function getApprovalTemplateById(atrzDocTmplId: string): Promise<ApprovalTemplate | null> {
  if (!atrzDocTmplId) {
    return null;
  }
  const snapshot = await db.collection(COLLECTIONS.approvalTemplates).doc(atrzDocTmplId).get();
  return snapshot.exists ? normalizeTemplate(snapshot.data() as JsonMap) : null;
}

async function listApprovalDocumentsData(): Promise<ApprovalDocument[]> {
  await ensureBaselineData();
  const snapshot = await db.collection(COLLECTIONS.approvalDocuments).get();
  return snapshot.docs
    .map((doc) => normalizeDocument(doc.data() as JsonMap))
    .sort((left, right) => right.atrzSbmtDt.localeCompare(left.atrzSbmtDt));
}

async function listApprovalTempsData(): Promise<ApprovalTemp[]> {
  await ensureBaselineData();
  const snapshot = await db.collection(COLLECTIONS.approvalTemps).get();
  return snapshot.docs
    .map((doc) => normalizeTemp(doc.data() as JsonMap))
    .sort((left, right) => right.updatedAt.localeCompare(left.updatedAt));
}

function extractStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.map((item) => trimmed(item)).filter(Boolean);
}

function extractApprovalUserIds(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((entry) => {
      const item = asObject(entry);
      return trimmed(item.atrzApprUserId) || trimmed(item.atrzApprId) || trimmed(item.userId);
    })
    .filter(Boolean);
}

function sanitizeAttachmentList(value: unknown): ApprovalAttachment[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.map((item, index) => normalizeAttachment(item, index)).filter((item) => item.filePath || item.orgnFileNm);
}

async function getUsersByIds(userIds: string[]) {
  const uniqueIds = Array.from(new Set(userIds.filter(Boolean)));
  const users = await Promise.all(uniqueIds.map((userId) => getDirectoryUser(userId)));
  const mapped = new Map<string, Awaited<ReturnType<typeof getDirectoryUser>>>();
  uniqueIds.forEach((userId, index) => {
    mapped.set(userId, users[index]);
  });
  return mapped;
}

async function buildApprovalLines(userIds: string[]): Promise<ApprovalLine[]> {
  const userMap = await getUsersByIds(userIds);
  return userIds
    .filter(Boolean)
    .map((userId, index) => {
      const user = userMap.get(userId);
      return {
        atrzLineSqn: index + 1,
        atrzApprUserId: userId,
        atrzApprUserNm: user?.userNm || userId,
        deptNm: user?.deptNm || "",
        jbgdNm: user?.jbgdNm || "",
        atrzApprStts: index === 0 ? "A302" : "A301",
        atrzOpnn: "",
        prcsDt: "",
      };
    });
}

async function buildReceivers(userIds: string[]): Promise<ApprovalReceiver[]> {
  const userMap = await getUsersByIds(userIds);
  return userIds
    .filter(Boolean)
    .map((userId) => {
      const user = userMap.get(userId);
      return {
        atrzRcvrId: userId,
        userNm: user?.userNm || userId,
        deptNm: user?.deptNm || "",
        jbgdNm: user?.jbgdNm || "",
      };
    });
}

function isApprovalParticipant(document: ApprovalDocument, userId: string): boolean {
  return document.atrzUserId === userId
    || document.receivers.some((receiver) => receiver.atrzRcvrId === userId)
    || document.approvalLines.some((line) => line.atrzApprUserId === userId)
    || document.openYn === "Y";
}

function findCurrentLine(document: ApprovalDocument): ApprovalLine | null {
  return document.approvalLines.find((line) => line.atrzApprStts === "A302") || null;
}

function buildDetailResponse(document: ApprovalDocument, template: ApprovalTemplate | null, userId: string) {
  const currentLine = findCurrentLine(document);
  const userLine = document.approvalLines.find((line) => line.atrzApprUserId === userId) || null;
  const canApprove = Boolean(userLine && userLine.atrzApprStts === "A302");
  return {
    document,
    template,
    receivers: document.receivers,
    attachments: document.attachments,
    canApprove,
    canReject: canApprove,
    canRetract: document.atrzUserId === userId && ["A202", "A203"].includes(document.crntAtrzStepCd),
    currentSeq: currentLine?.atrzLineSqn || document.currentSeq || 0,
    lineSqn: currentLine?.atrzLineSqn || document.lineSqn || 0,
  };
}

function filterByDateRange(value: string, dateFrom: string, dateTo: string): boolean {
  const compare = value.slice(0, 10);
  if (dateFrom && compare < dateFrom) return false;
  if (dateTo && compare > dateTo) return false;
  return true;
}

function matchesKeyword(fields: string[], keyword: string): boolean {
  const normalized = keyword.trim().toLowerCase();
  if (!normalized) {
    return true;
  }
  return fields.some((value) => value.toLowerCase().includes(normalized));
}

function filterDocuments(items: ApprovalDocument[], userId: string, params: JsonMap): ApprovalDocument[] {
  const section = trimmed(params.section) || "draft";
  const status = trimmed(params.status);
  const keyword = trimmed(params.keyword);
  const templateId = trimmed(params.templateId);
  const dateFrom = trimmed(params.dateFrom);
  const dateTo = trimmed(params.dateTo);

  return items.filter((item) => {
    const userLine = item.approvalLines.find((line) => line.atrzApprUserId === userId) || null;
    const receiverMatch = item.receivers.some((receiver) => receiver.atrzRcvrId === userId);
    let matchesSection = false;

    if (section === "draft") {
      matchesSection = item.atrzUserId === userId;
      if (matchesSection) {
        if (status === "approved") matchesSection = item.crntAtrzStepCd === "A206";
        else if (status === "rejected") matchesSection = item.crntAtrzStepCd === "A204";
        else if (status === "retracted") matchesSection = item.crntAtrzStepCd === "A205";
        else matchesSection = ["A202", "A203"].includes(item.crntAtrzStepCd);
      }
    } else if (section === "inbox") {
      matchesSection = Boolean(userLine && userLine.atrzApprStts === "A302");
    } else if (section === "upcoming") {
      matchesSection = Boolean(userLine && userLine.atrzApprStts === "A301");
    } else if (section === "reference") {
      matchesSection = receiverMatch;
    } else if (section === "archive") {
      matchesSection = isApprovalParticipant(item, userId) && ["A204", "A205", "A206"].includes(item.crntAtrzStepCd);
      if (matchesSection && status && status !== "all") {
        matchesSection = (
          (status === "approved" && item.crntAtrzStepCd === "A206")
          || (status === "rejected" && item.crntAtrzStepCd === "A204")
          || (status === "retracted" && item.crntAtrzStepCd === "A205")
        );
      }
    }

    if (!matchesSection) return false;
    if (templateId && item.atrzDocTmplId !== templateId) return false;
    if (!filterByDateRange(item.atrzSbmtDt, dateFrom, dateTo)) return false;
    return matchesKeyword([item.atrzDocTtl, item.atrzDocTmplNm, item.drafterName, item.atrzUserId], keyword);
  });
}

function buildStatusCounts(section: string, items: ApprovalDocument[], userId: string) {
  if (section === "draft") {
    const drafts = items.filter((item) => item.atrzUserId === userId);
    return {
      progress: drafts.filter((item) => ["A202", "A203"].includes(item.crntAtrzStepCd)).length,
      approved: drafts.filter((item) => item.crntAtrzStepCd === "A206").length,
      rejected: drafts.filter((item) => item.crntAtrzStepCd === "A204").length,
      retracted: drafts.filter((item) => item.crntAtrzStepCd === "A205").length,
    };
  }

  if (section === "archive") {
    const archive = items.filter((item) => isApprovalParticipant(item, userId) && ["A204", "A205", "A206"].includes(item.crntAtrzStepCd));
    return {
      all: archive.length,
      approved: archive.filter((item) => item.crntAtrzStepCd === "A206").length,
      rejected: archive.filter((item) => item.crntAtrzStepCd === "A204").length,
      retracted: archive.filter((item) => item.crntAtrzStepCd === "A205").length,
    };
  }

  return {};
}

function paginate<T>(items: T[], page: number) {
  const totalRecords = items.length;
  const totalPages = Math.max(1, Math.ceil(totalRecords / PAGE_SIZE));
  const currentPage = Math.min(Math.max(1, page), totalPages);
  const start = (currentPage - 1) * PAGE_SIZE;
  return {
    items: items.slice(start, start + PAGE_SIZE),
    page: currentPage,
    totalPages,
    totalRecords,
  };
}

function buildSummaryCounts(documents: ApprovalDocument[], temps: ApprovalTemp[], userId: string) {
  const draftDocs = documents.filter((item) => item.atrzUserId === userId);
  const inboxDocs = documents.filter((item) => item.approvalLines.some((line) => line.atrzApprUserId === userId && line.atrzApprStts === "A302"));
  const upcomingDocs = documents.filter((item) => item.approvalLines.some((line) => line.atrzApprUserId === userId && line.atrzApprStts === "A301"));
  const referenceDocs = documents.filter((item) => item.receivers.some((receiver) => receiver.atrzRcvrId === userId));
  const archiveDocs = documents.filter((item) => isApprovalParticipant(item, userId) && ["A204", "A205", "A206"].includes(item.crntAtrzStepCd));
  const tempDocs = temps.filter((item) => item.atrzUserId === userId);

  return {
    draftProgress: draftDocs.filter((item) => ["A202", "A203"].includes(item.crntAtrzStepCd)).length,
    inboxPending: inboxDocs.length,
    upcoming: upcomingDocs.length,
    reference: referenceDocs.length,
    tempSaved: tempDocs.length,
    archive: archiveDocs.length,
    draftApproved: draftDocs.filter((item) => item.crntAtrzStepCd === "A206").length,
    draftRejected: draftDocs.filter((item) => item.crntAtrzStepCd === "A204").length,
    draftRetracted: draftDocs.filter((item) => item.crntAtrzStepCd === "A205").length,
  };
}

function makeApprovalDocumentId(): string {
  return `ATRZDOC_${db.collection(COLLECTIONS.approvalDocuments).doc().id}`;
}

function makeApprovalTempId(): string {
  return `ATRZTEMP_${db.collection(COLLECTIONS.approvalTemps).doc().id}`;
}

async function getApprovalDocumentOrThrow(atrzDocId: string): Promise<ApprovalDocument> {
  const snapshot = await db.collection(COLLECTIONS.approvalDocuments).doc(atrzDocId).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "문서를 찾을 수 없습니다.");
  }
  return normalizeDocument(snapshot.data() as JsonMap);
}

async function createApprovalDocument(
  currentUser: Awaited<ReturnType<typeof loadProfile>>,
  payloadValue: unknown,
  attachmentsValue: unknown
) {
  const payload = asObject(payloadValue);
  const atrzDocTmplId = trimmed(payload.atrzDocTmplId);
  const atrzDocTtl = trimmed(payload.atrzDocTtl);
  const htmlData = trimmed(payload.htmlData);

  if (!atrzDocTmplId || !atrzDocTtl || !htmlData) {
    throw new HttpsError("invalid-argument", "양식, 제목, 본문은 필수입니다.");
  }

  const template = await getApprovalTemplateById(atrzDocTmplId);
  if (!template) {
    throw new HttpsError("not-found", "결재 양식을 찾을 수 없습니다.");
  }

  const approvalUserIds = extractApprovalUserIds(payload.approvalLines);
  if (approvalUserIds.length === 0) {
    throw new HttpsError("invalid-argument", "결재선을 한 명 이상 선택해야 합니다.");
  }

  const receiverIds = extractStringArray(payload.receiverIds);
  const [approvalLines, receivers] = await Promise.all([
    buildApprovalLines(approvalUserIds),
    buildReceivers(receiverIds),
  ]);

  let attachments = sanitizeAttachmentList(attachmentsValue);
  const tempId = trimmed(payload.atrzTempSqn);
  if (tempId) {
    const tempSnapshot = await db.collection(COLLECTIONS.approvalTemps).doc(tempId).get();
    if (tempSnapshot.exists && attachments.length === 0) {
      attachments = normalizeTemp(tempSnapshot.data() as JsonMap).attachments;
    }
  }

  const document: ApprovalDocument = {
    atrzDocId: makeApprovalDocumentId(),
    atrzDocTmplId,
    atrzDocCd: template.atrzDocCd,
    atrzDocTmplNm: template.atrzDocTmplNm,
    atrzDocTtl,
    htmlData,
    openYn: trimmed(payload.openYn) || "N",
    atrzUserId: currentUser.userId,
    drafterName: currentUser.userNm || currentUser.userId,
    atrzSbmtDt: nowIso(),
    crntAtrzStepCd: "A202",
    approvalLines,
    receivers,
    attachments,
    currentSeq: 1,
    lineSqn: 1,
    updatedAt: nowIso(),
  };

  await db.collection(COLLECTIONS.approvalDocuments).doc(document.atrzDocId).set(document);
  if (tempId) {
    await db.collection(COLLECTIONS.approvalTemps).doc(tempId).delete().catch(() => undefined);
  }
  return { atrzDocId: document.atrzDocId };
}

async function saveApprovalTemp(
  currentUser: Awaited<ReturnType<typeof loadProfile>>,
  payloadValue: unknown,
  attachmentsValue: unknown,
  atrzTempSqn = ""
) {
  const payload = asObject(payloadValue);
  const atrzDocTmplId = trimmed(payload.atrzDocTmplId);
  const atrzDocTtl = trimmed(payload.atrzDocTtl);
  const htmlData = trimmed(payload.htmlData);

  if (!atrzDocTmplId || !atrzDocTtl || !htmlData) {
    throw new HttpsError("invalid-argument", "양식, 제목, 본문은 필수입니다.");
  }

  const nextId = atrzTempSqn || makeApprovalTempId();
  const tempRef = db.collection(COLLECTIONS.approvalTemps).doc(nextId);
  const existingSnapshot = await tempRef.get();
  const existing = existingSnapshot.exists ? normalizeTemp(existingSnapshot.data() as JsonMap) : null;

  if (existing && existing.atrzUserId !== currentUser.userId) {
    throw new HttpsError("permission-denied", "임시저장 문서를 수정할 권한이 없습니다.");
  }

  const newAttachments = sanitizeAttachmentList(attachmentsValue);
  const temp: ApprovalTemp = {
    atrzTempSqn: nextId,
    atrzDocTmplId,
    atrzDocTtl,
    htmlData,
    openYn: trimmed(payload.openYn) || existing?.openYn || "N",
    atrzUserId: currentUser.userId,
    drafterName: currentUser.userNm || currentUser.userId,
    atrzSbmtDt: existing?.atrzSbmtDt || nowIso(),
    atrzFileId: newAttachments[0]?.fileId || existing?.atrzFileId || "",
    attachments: [...(existing?.attachments || []), ...newAttachments],
    updatedAt: nowIso(),
  };

  await tempRef.set(temp, { merge: true });
  return { atrzTempSqn: nextId };
}

function flattenCustomLine(name: string, ownerUserId: string, lines: unknown[]) {
  return lines
    .map((line, index) => {
      const item = asObject(line);
      return {
        cstmLineBmNm: name,
        atrzLineSeq: numberValue(item.atrzLineSeq, index + 1),
        atrzApprId: trimmed(item.atrzApprId) || trimmed(item.atrzApprUserId) || trimmed(item.userId),
        apprAtrzYn: trimmed(item.apprAtrzYn) || "N",
        ownerUserId,
      };
    })
    .filter((line) => trimmed(line.atrzApprId));
}

async function updateApprovalDecision(atrzDocId: string, userId: string, action: "approve" | "reject", dataValue: unknown) {
  const payload = asObject(dataValue);
  const opinion = trimmed(payload.opinion);
  const htmlData = trimmed(payload.htmlData);
  const document = await getApprovalDocumentOrThrow(atrzDocId);
  const lineIndex = document.approvalLines.findIndex((line) => line.atrzApprUserId === userId && line.atrzApprStts === "A302");
  if (lineIndex < 0) {
    throw new HttpsError("permission-denied", "현재 결재 차수가 아닙니다.");
  }

  const lines = document.approvalLines.map((line) => ({ ...line }));
  const currentLine = { ...lines[lineIndex], atrzOpnn: opinion, prcsDt: nowIso() };

  if (action === "approve") {
    currentLine.atrzApprStts = "A303";
    lines[lineIndex] = currentLine;
    const nextIndex = lines.findIndex((line, index) => index > lineIndex && line.atrzApprStts === "A301");
    if (nextIndex >= 0) {
      lines[nextIndex] = { ...lines[nextIndex], atrzApprStts: "A302" };
    }

    const updated: ApprovalDocument = {
      ...document,
      htmlData: htmlData || document.htmlData,
      approvalLines: lines,
      crntAtrzStepCd: nextIndex >= 0 ? "A203" : "A206",
      currentSeq: nextIndex >= 0 ? lines[nextIndex].atrzLineSqn : currentLine.atrzLineSqn,
      lineSqn: nextIndex >= 0 ? lines[nextIndex].atrzLineSqn : currentLine.atrzLineSqn,
      updatedAt: nowIso(),
    };

    await db.collection(COLLECTIONS.approvalDocuments).doc(atrzDocId).set(updated, { merge: true });
    return buildDetailResponse(updated, await getApprovalTemplateById(updated.atrzDocTmplId), userId);
  }

  currentLine.atrzApprStts = "A304";
  lines[lineIndex] = currentLine;

  const rejected: ApprovalDocument = {
    ...document,
    approvalLines: lines,
    crntAtrzStepCd: "A204",
    currentSeq: currentLine.atrzLineSqn,
    lineSqn: currentLine.atrzLineSqn,
    updatedAt: nowIso(),
  };
  await db.collection(COLLECTIONS.approvalDocuments).doc(atrzDocId).set(rejected, { merge: true });
  return buildDetailResponse(rejected, await getApprovalTemplateById(rejected.atrzDocTmplId), userId);
}

export const approvalGetTemplates = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  return listApprovalTemplatesData();
});

export const approvalGetSummary = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const [documents, temps] = await Promise.all([listApprovalDocumentsData(), listApprovalTempsData()]);
  return { counts: buildSummaryCounts(documents, temps, currentUser.userId) };
});

export const approvalGetList = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const params = asObject(request.data);
  const section = trimmed(params.section) || "draft";
  const status = trimmed(params.status) || (section === "archive" ? "all" : "progress");
  const documents = await listApprovalDocumentsData();
  const filtered = filterDocuments(documents, currentUser.userId, params);
  const paged = paginate(filtered, numberValue(params.page, 1));

  return {
    section,
    status,
    items: paged.items.map((item) => ({
      atrzDocId: item.atrzDocId,
      atrzDocTtl: item.atrzDocTtl,
      atrzDocTmplNm: item.atrzDocTmplNm,
      drafterName: item.drafterName,
      crntAtrzStepCd: item.crntAtrzStepCd,
      atrzSbmtDt: item.atrzSbmtDt,
    })),
    page: paged.page,
    totalPages: paged.totalPages,
    totalRecords: paged.totalRecords,
    statusCounts: buildStatusCounts(section, documents, currentUser.userId),
  };
});

export const approvalGetDetail = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const atrzDocId = trimmed(asObject(request.data).atrzDocId);
  const document = await getApprovalDocumentOrThrow(atrzDocId);
  if (!isApprovalParticipant(document, currentUser.userId)) {
    throw new HttpsError("permission-denied", "문서를 볼 권한이 없습니다.");
  }
  return buildDetailResponse(document, await getApprovalTemplateById(document.atrzDocTmplId), currentUser.userId);
});

export const approvalCreateDocument = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  return createApprovalDocument(currentUser, data.payload, data.attachments);
});

export const approvalApproveDocument = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  return updateApprovalDecision(trimmed(data.atrzDocId), currentUser.userId, "approve", data);
});

export const approvalRejectDocument = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  return updateApprovalDecision(trimmed(data.atrzDocId), currentUser.userId, "reject", data);
});

export const approvalRetractDocument = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const atrzDocId = trimmed(asObject(request.data).atrzDocId);
  const document = await getApprovalDocumentOrThrow(atrzDocId);
  if (document.atrzUserId !== currentUser.userId) {
    throw new HttpsError("permission-denied", "기안자만 회수할 수 있습니다.");
  }
  const retracted: ApprovalDocument = {
    ...document,
    crntAtrzStepCd: "A205",
    updatedAt: nowIso(),
  };
  await db.collection(COLLECTIONS.approvalDocuments).doc(atrzDocId).set(retracted, { merge: true });
  return buildDetailResponse(retracted, await getApprovalTemplateById(retracted.atrzDocTmplId), currentUser.userId);
});

export const approvalGetCustomLines = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const snapshot = await db.collection(COLLECTIONS.approvalCustomLines).where("ownerUserId", "==", currentUser.userId).get();
  return snapshot.docs
    .flatMap((doc) => {
      const source = asObject(doc.data());
      const name = trimmed(source.cstmLineBmNm);
      const lines = Array.isArray(source.lines) ? source.lines : [];
      return flattenCustomLine(name, currentUser.userId, lines);
    })
    .sort((left, right) => {
      const leftName = trimmed(left.cstmLineBmNm);
      const rightName = trimmed(right.cstmLineBmNm);
      if (leftName !== rightName) {
        return leftName.localeCompare(rightName, "ko");
      }
      return numberValue(left.atrzLineSeq, 0) - numberValue(right.atrzLineSeq, 0);
    });
});

export const approvalSaveCustomLine = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const lines = Array.isArray(asObject(request.data).lines) ? (asObject(request.data).lines as unknown[]) : [];
  if (lines.length === 0) {
    throw new HttpsError("invalid-argument", "저장할 결재선이 없습니다.");
  }
  const name = trimmed(asObject(lines[0]).cstmLineBmNm);
  if (!name) {
    throw new HttpsError("invalid-argument", "즐겨찾기 이름이 필요합니다.");
  }
  const flattened = flattenCustomLine(name, currentUser.userId, lines);
  await db.collection(COLLECTIONS.approvalCustomLines).doc(`${currentUser.userId}_${sanitizeDocIdPart(name)}`).set({
    cstmLineBmNm: name,
    ownerUserId: currentUser.userId,
    lines: flattened,
    updatedAt: nowIso(),
  }, { merge: true });
  return { cstmLineBmNm: name, count: flattened.length };
});

export const approvalDeleteCustomLine = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const name = trimmed(asObject(request.data).name);
  await db.collection(COLLECTIONS.approvalCustomLines).doc(`${currentUser.userId}_${sanitizeDocIdPart(name)}`).delete().catch(() => undefined);
  return { deleted: true };
});

export const approvalGetVacationBalance = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  return 15;
});

export const approvalGetTempList = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const temps = await listApprovalTempsData();
  return temps
    .filter((item) => item.atrzUserId === currentUser.userId)
    .map((item) => ({
      atrzTempSqn: item.atrzTempSqn,
      atrzDocTtl: item.atrzDocTtl,
      atrzDocTmplNm: DEFAULT_APPROVAL_TEMPLATES.find((template) => template.atrzDocTmplId === item.atrzDocTmplId)?.atrzDocTmplNm || item.atrzDocTmplId,
      atrzSbmtDt: item.updatedAt || item.atrzSbmtDt,
    }));
});

export const approvalGetTempDetail = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const atrzTempSqn = trimmed(asObject(request.data).atrzTempSqn);
  const snapshot = await db.collection(COLLECTIONS.approvalTemps).doc(atrzTempSqn).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "임시저장 문서를 찾을 수 없습니다.");
  }
  const temp = normalizeTemp(snapshot.data() as JsonMap);
  if (temp.atrzUserId !== currentUser.userId) {
    throw new HttpsError("permission-denied", "임시저장 문서를 볼 권한이 없습니다.");
  }
  return { temp, attachments: temp.attachments };
});

export const approvalSaveTemp = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  return saveApprovalTemp(currentUser, data.payload, data.attachments);
});

export const approvalUpdateTemp = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  return saveApprovalTemp(currentUser, data.payload, data.attachments, trimmed(data.atrzTempSqn));
});

export const approvalDeleteTemp = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const atrzTempSqn = trimmed(asObject(request.data).atrzTempSqn);
  const tempRef = db.collection(COLLECTIONS.approvalTemps).doc(atrzTempSqn);
  const snapshot = await tempRef.get();
  if (!snapshot.exists) {
    return { deleted: true };
  }
  const temp = normalizeTemp(snapshot.data() as JsonMap);
  if (temp.atrzUserId !== currentUser.userId) {
    throw new HttpsError("permission-denied", "임시저장 문서를 삭제할 권한이 없습니다.");
  }
  await tempRef.delete();
  return { deleted: true };
});
