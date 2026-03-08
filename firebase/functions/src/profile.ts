import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
  COLLECTIONS,
  CallableAuth,
  JsonMap,
  UserProfile,
  asObject,
  assertSignedIn,
  db,
  ensureBaselineData,
  normalizeCommonCode,
  normalizeDepartment,
  normalizeUser,
  nowIso,
  resolveUserIdFromToken,
  trimmed,
} from "./shared.js";

export async function listDirectoryUsersData(): Promise<UserProfile[]> {
  await ensureBaselineData();
  const snapshot = await db.collection(COLLECTIONS.directoryUsers).get();
  return snapshot.docs
    .map((doc) => normalizeUser(doc.data() as JsonMap))
    .sort((left, right) => left.userNm.localeCompare(right.userNm, "ko"));
}

export async function getDirectoryUser(userId: string): Promise<UserProfile | null> {
  if (!userId) {
    return null;
  }
  await ensureBaselineData();
  const snapshot = await db.collection(COLLECTIONS.directoryUsers).doc(userId).get();
  return snapshot.exists ? normalizeUser(snapshot.data() as JsonMap) : null;
}

export async function getDepartmentName(deptId: string): Promise<string> {
  if (!deptId) {
    return "";
  }
  await ensureBaselineData();
  const snapshot = await db.collection(COLLECTIONS.departments).doc(deptId).get();
  return snapshot.exists ? normalizeDepartment(snapshot.data() as JsonMap).deptNm : "";
}

export async function loadProfile(uid: string, token: Record<string, unknown> = {}): Promise<UserProfile> {
  await ensureBaselineData();
  const sessionSnapshot = await db.collection(COLLECTIONS.sessionUsers).doc(uid).get();
  const sessionProfile = sessionSnapshot.exists ? normalizeUser(sessionSnapshot.data() as JsonMap) : null;
  const userId = resolveUserIdFromToken(uid, token, sessionProfile?.userId);
  const directoryUser = await getDirectoryUser(userId);

  const profile = normalizeUser({
    ...(directoryUser || {}),
    ...(sessionProfile || {}),
    userId,
    userEmail: sessionProfile?.userEmail || directoryUser?.userEmail || trimmed(token.email),
    userNm: sessionProfile?.userNm || directoryUser?.userNm || userId,
    firebaseUid: uid,
  });

  if (!profile.deptNm && profile.deptId) {
    profile.deptNm = await getDepartmentName(profile.deptId);
  }

  return profile;
}

export async function persistUserProfile(uid: string, profile: UserProfile): Promise<void> {
  const payload = {
    ...profile,
    updatedAt: nowIso(),
  };
  const batch = db.batch();
  batch.set(db.collection(COLLECTIONS.sessionUsers).doc(uid), payload, { merge: true });
  batch.set(db.collection(COLLECTIONS.directoryUsers).doc(profile.userId), payload, { merge: true });
  await batch.commit();
}

function sanitizeProfilePayload(data: unknown): Partial<UserProfile> {
  const source = asObject(data);
  return {
    userId: trimmed(source.userId),
    userNm: trimmed(source.userNm),
    userEmail: trimmed(source.userEmail),
    userTelno: trimmed(source.userTelno),
    extTel: trimmed(source.extTel),
    deptId: trimmed(source.deptId),
    deptNm: trimmed(source.deptNm),
    jbgdNm: trimmed(source.jbgdNm),
    userRole: trimmed(source.userRole),
    hireYmd: trimmed(source.hireYmd),
    workSttsCd: trimmed(source.workSttsCd),
    rsgntnYn: trimmed(source.rsgntnYn),
    rsgntnYmd: trimmed(source.rsgntnYmd),
    profileImageUrl: trimmed(source.profileImageUrl),
    profileImagePath: trimmed(source.profileImagePath),
  };
}

async function listUsersMerged(): Promise<UserProfile[]> {
  const [directoryUsers, sessionUsersSnapshot] = await Promise.all([
    listDirectoryUsersData(),
    db.collection(COLLECTIONS.sessionUsers).get(),
  ]);
  const byId = new Map<string, UserProfile>();

  [...directoryUsers, ...sessionUsersSnapshot.docs.map((doc) => normalizeUser(doc.data() as JsonMap))].forEach((user) => {
    if (!user.userId) {
      return;
    }
    byId.set(user.userId, normalizeUser(user, byId.get(user.userId) || {}));
  });

  return Array.from(byId.values()).sort((left, right) => left.userNm.localeCompare(right.userNm, "ko"));
}

function matchesKeyword(values: string[], keyword: string): boolean {
  const normalized = keyword.trim().toLowerCase();
  if (!normalized) {
    return true;
  }
  return values.some((value) => value.toLowerCase().includes(normalized));
}

export const sessionProfile = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  return loadProfile(uid, request.auth?.token || {});
});

export const upsertProfile = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const current = await loadProfile(uid, request.auth?.token || {});
  const payload = sanitizeProfilePayload(request.data);
  const profile = normalizeUser({
    ...current,
    ...payload,
    userId: current.userId,
    firebaseUid: uid,
  });

  if (!profile.deptNm && profile.deptId) {
    profile.deptNm = await getDepartmentName(profile.deptId);
  }

  await persistUserProfile(uid, profile);
  return profile;
});

export const listUsers = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  return listUsersMerged();
});

export const getUserDetail = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const userId = trimmed(asObject(request.data).userId);
  const users = await listUsersMerged();
  const match = users.find((item) => item.userId === userId);
  if (!match) {
    throw new HttpsError("not-found", "사용자를 찾을 수 없습니다.");
  }
  return match;
});

export const getMyProfile = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  return loadProfile(uid, request.auth?.token || {});
});

export const searchUsers = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const term = trimmed(asObject(request.data).term);
  const users = await listUsersMerged();
  return users.filter((user) => matchesKeyword([user.userId, user.userNm, user.deptNm, user.userEmail], term));
});

export const upsertUserProfile = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const data = asObject(request.data);
  const userPayload = sanitizeProfilePayload(data.user || data);
  const userId = trimmed(data.userId) || trimmed(userPayload.userId);

  if (!userId) {
    throw new HttpsError("invalid-argument", "userId is required.");
  }

  const existing = await getDirectoryUser(userId);
  const nextUser = normalizeUser({
    ...(existing || {}),
    ...userPayload,
    userId,
  });

  if (!nextUser.deptNm && nextUser.deptId) {
    nextUser.deptNm = await getDepartmentName(nextUser.deptId);
  }

  await db.collection(COLLECTIONS.directoryUsers).doc(userId).set({
    ...nextUser,
    updatedAt: nowIso(),
  }, { merge: true });

  return nextUser;
});

export const retireUser = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const userId = trimmed(asObject(request.data).userId);
  const existing = await getDirectoryUser(userId);
  if (!existing) {
    throw new HttpsError("not-found", "사용자를 찾을 수 없습니다.");
  }

  const retired = {
    ...existing,
    rsgntnYn: "Y",
    rsgntnYmd: nowIso().slice(0, 10),
    workSttsCd: "C104",
    updatedAt: nowIso(),
  };

  await db.collection(COLLECTIONS.directoryUsers).doc(userId).set(retired, { merge: true });
  return retired;
});

export const listDepartments = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  await ensureBaselineData();
  const snapshot = await db.collection(COLLECTIONS.departments).get();
  return snapshot.docs
    .map((doc) => normalizeDepartment(doc.data() as JsonMap))
    .sort((left, right) => left.sortNum - right.sortNum || left.deptNm.localeCompare(right.deptNm, "ko"));
});

export const listCommonCodes = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  await ensureBaselineData();
  const codeGrpId = trimmed(asObject(request.data).codeGrpId);
  const snapshot = await db.collection(COLLECTIONS.commonCodes).get();
  const items = snapshot.docs
    .map((doc) => normalizeCommonCode(doc.data() as JsonMap))
    .filter((item) => !codeGrpId || item.codeGrpId === codeGrpId)
    .sort((left, right) => left.sortOrder - right.sortOrder || left.codeNm.localeCompare(right.codeNm, "ko"));
  return items;
});
