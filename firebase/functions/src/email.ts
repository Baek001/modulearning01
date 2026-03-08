import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
  CallableAuth,
  JsonMap,
  asObject,
  assertSignedIn,
  db,
  ensureBaselineData,
  nowIso,
  trimmed,
} from "./shared.js";
import { listDirectoryUsersData, loadProfile } from "./profile.js";

const EMAIL_BOOTSTRAP = "system/emailBootstrap";
const EMAILS = "emails";

type EmailRecord = {
  emailContId: string;
  ownerUserId: string;
  mailboxTypeCd: string;
  prevMailboxTypeCd: string;
  senderUserId: string;
  senderName: string;
  senderEmail: string;
  subject: string;
  content: string;
  sendDate: string;
  readYn: string;
  importanceYn: string;
  deletedYn: string;
};

function makeId() {
  return `MAIL_${Date.now()}_${db.collection(EMAILS).doc().id.slice(0, 8)}`;
}

function normalizeEmail(item: unknown): EmailRecord {
  const source = asObject(item);
  return {
    emailContId: trimmed(source.emailContId),
    ownerUserId: trimmed(source.ownerUserId),
    mailboxTypeCd: trimmed(source.mailboxTypeCd) || "G101",
    prevMailboxTypeCd: trimmed(source.prevMailboxTypeCd) || "G101",
    senderUserId: trimmed(source.senderUserId),
    senderName: trimmed(source.senderName),
    senderEmail: trimmed(source.senderEmail),
    subject: trimmed(source.subject),
    content: trimmed(source.content),
    sendDate: trimmed(source.sendDate) || nowIso(),
    readYn: trimmed(source.readYn) || "N",
    importanceYn: trimmed(source.importanceYn) || "N",
    deletedYn: trimmed(source.deletedYn) || "N",
  };
}

async function ensureEmailSeedData() {
  await ensureBaselineData();
  const bootstrapRef = db.doc(EMAIL_BOOTSTRAP);
  const snapshot = await bootstrapRef.get();
  if (snapshot.exists) {
    return;
  }

  const users = await listDirectoryUsersData();
  const admin = users.find((item) => item.userId === "admin") || users[0];
  const memberOne = users.find((item) => item.userId === "user01") || users[0];
  const memberTwo = users.find((item) => item.userId === "user02") || users[1] || users[0];
  const seededAt = nowIso();
  const emails: EmailRecord[] = [
    {
      emailContId: makeId(),
      ownerUserId: admin?.userId || "admin",
      mailboxTypeCd: "G101",
      prevMailboxTypeCd: "G101",
      senderUserId: memberOne?.userId || "user01",
      senderName: memberOne?.userNm || "사용자",
      senderEmail: memberOne?.userEmail || "user01@starworks.local",
      subject: "Firebase 전환 현황 공유",
      content: "대시보드와 게시판 전환이 완료됐습니다. 남은 메신저/계약도 이번 단계에서 정리합니다.",
      sendDate: seededAt,
      readYn: "N",
      importanceYn: "Y",
      deletedYn: "N",
    },
    {
      emailContId: makeId(),
      ownerUserId: admin?.userId || "admin",
      mailboxTypeCd: "G102",
      prevMailboxTypeCd: "G102",
      senderUserId: admin?.userId || "admin",
      senderName: admin?.userNm || "관리자",
      senderEmail: admin?.userEmail || "admin@starworks.local",
      subject: "계약 템플릿 요청 안내",
      content: "전자계약 템플릿 요청이 접수되면 워크스페이스에서 바로 승인할 수 있습니다.",
      sendDate: new Date(Date.now() - 1000 * 60 * 60).toISOString(),
      readYn: "Y",
      importanceYn: "N",
      deletedYn: "N",
    },
    {
      emailContId: makeId(),
      ownerUserId: memberOne?.userId || "user01",
      mailboxTypeCd: "G101",
      prevMailboxTypeCd: "G101",
      senderUserId: admin?.userId || "admin",
      senderName: admin?.userNm || "관리자",
      senderEmail: admin?.userEmail || "admin@starworks.local",
      subject: "프로젝트 회의 공지",
      content: "이번 주 프로젝트 점검 회의는 금요일 오후 2시에 진행합니다.",
      sendDate: new Date(Date.now() - 1000 * 60 * 30).toISOString(),
      readYn: "N",
      importanceYn: "N",
      deletedYn: "N",
    },
    {
      emailContId: makeId(),
      ownerUserId: memberTwo?.userId || "user02",
      mailboxTypeCd: "G105",
      prevMailboxTypeCd: "G101",
      senderUserId: admin?.userId || "admin",
      senderName: admin?.userNm || "관리자",
      senderEmail: admin?.userEmail || "admin@starworks.local",
      subject: "삭제된 메일 예시",
      content: "휴지통 복원 흐름을 확인하기 위한 샘플 메일입니다.",
      sendDate: new Date(Date.now() - 1000 * 60 * 60 * 8).toISOString(),
      readYn: "Y",
      importanceYn: "N",
      deletedYn: "Y",
    },
  ];

  const batch = db.batch();
  emails.forEach((email) => {
    batch.set(db.collection(EMAILS).doc(email.emailContId), email);
  });
  batch.set(bootstrapRef, { seedVersion: 1, seededAt });
  await batch.commit();
}

async function listEmailsForUser(userId: string): Promise<EmailRecord[]> {
  await ensureEmailSeedData();
  const snapshot = await db.collection(EMAILS).where("ownerUserId", "==", userId).get();
  return snapshot.docs
    .map((doc) => normalizeEmail(doc.data()))
    .sort((left, right) => right.sendDate.localeCompare(left.sendDate));
}

function mailboxMatches(email: EmailRecord, mailboxTypeCd: string) {
  if (mailboxTypeCd === "G104") {
    return email.importanceYn === "Y" && email.mailboxTypeCd !== "G105";
  }
  return email.mailboxTypeCd === mailboxTypeCd;
}

async function updateMany(emailContIds: string[], updater: (record: EmailRecord) => EmailRecord) {
  const batch = db.batch();
  for (const emailContId of emailContIds) {
    const snapshot = await db.collection(EMAILS).doc(emailContId).get();
    if (!snapshot.exists) {
      continue;
    }
    const next = updater(normalizeEmail(snapshot.data()));
    batch.set(snapshot.ref, next, { merge: true });
  }
  await batch.commit();
}

export const emailGetCounts = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const emails = await listEmailsForUser(currentUser.userId);
  return {
    inboxCount: emails.filter((item) => item.mailboxTypeCd === "G101").length,
    sentCount: emails.filter((item) => item.mailboxTypeCd === "G102").length,
    draftsCount: emails.filter((item) => item.mailboxTypeCd === "G103").length,
    importantCount: emails.filter((item) => item.importanceYn === "Y" && item.mailboxTypeCd !== "G105").length,
    trashCount: emails.filter((item) => item.mailboxTypeCd === "G105").length,
  };
});

export const emailGetList = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const mailboxTypeCd = trimmed(data.mailboxTypeCd) || "G101";
  const page = Number(data.page || 1) || 1;
  const searchWord = trimmed(data.searchWord).toLowerCase();
  const allEmails = await listEmailsForUser(currentUser.userId);
  const filtered = allEmails
    .filter((item) => mailboxMatches(item, mailboxTypeCd))
    .filter((item) => {
      if (!searchWord) {
        return true;
      }
      return [item.subject, item.content, item.senderName, item.senderEmail]
        .some((value) => String(value || "").toLowerCase().includes(searchWord));
    });

  const startIndex = (Math.max(page, 1) - 1) * 20;
  return {
    emailList: filtered.slice(startIndex, startIndex + 20),
    paginationInfo: {
      currentPage: page,
      totalRecord: filtered.length,
      totalPage: Math.max(1, Math.ceil(filtered.length / 20)),
    },
  };
});

export const emailToggleImportance = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const emailContId = trimmed(asObject(request.data).emailContId);
  if (!emailContId) {
    throw new HttpsError("invalid-argument", "emailContId is required.");
  }
  const snapshot = await db.collection(EMAILS).doc(emailContId).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "메일을 찾을 수 없습니다.");
  }
  const email = normalizeEmail(snapshot.data());
  if (email.ownerUserId !== currentUser.userId) {
    throw new HttpsError("permission-denied", "메일을 수정할 권한이 없습니다.");
  }
  const importanceYn = email.importanceYn === "Y" ? "N" : "Y";
  await snapshot.ref.set({ importanceYn, updatedAt: nowIso() }, { merge: true });
  return { emailContId, importanceYn };
});

export const emailDeleteSelected = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const emailContIds = normalizeEmailIds(data.emailContIds);
  const mailboxTypeCd = trimmed(data.mailboxTypeCd) || "G101";
  await updateMany(emailContIds, (email) => {
    if (email.ownerUserId !== currentUser.userId) {
      return email;
    }
    return {
      ...email,
      prevMailboxTypeCd: email.mailboxTypeCd || mailboxTypeCd,
      mailboxTypeCd: "G105",
      deletedYn: "Y",
    };
  });
  return { success: true };
});

export const emailDeleteAll = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const mailboxTypeCd = trimmed(asObject(request.data).mailboxTypeCd) || "G101";
  const emails = await listEmailsForUser(currentUser.userId);
  const targetIds = emails.filter((item) => mailboxMatches(item, mailboxTypeCd)).map((item) => item.emailContId);
  await updateMany(targetIds, (email) => ({
    ...email,
    prevMailboxTypeCd: email.mailboxTypeCd || mailboxTypeCd,
    mailboxTypeCd: "G105",
    deletedYn: "Y",
  }));
  return { success: true, count: targetIds.length };
});

export const emailRestoreSelected = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const emailContIds = normalizeEmailIds(asObject(request.data).emailContIds);
  await updateMany(emailContIds, (email) => {
    if (email.ownerUserId !== currentUser.userId) {
      return email;
    }
    return {
      ...email,
      mailboxTypeCd: email.prevMailboxTypeCd || "G101",
      deletedYn: "N",
    };
  });
  return { success: true };
});

function normalizeEmailIds(value: unknown): string[] {
  return Array.isArray(value)
    ? value.map((item) => trimmed(item)).filter(Boolean)
    : [];
}
