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
  trimmed,
} from "./shared.js";
import { getDirectoryUser, listDirectoryUsersData, loadProfile } from "./profile.js";
import { ensureWorkSeedData, listCommunitiesData } from "./work.js";

const COMMUNITIES = "communities";
const COMMUNITY_SETTINGS = "communitySettings";

type CommunityMembership = {
  userId: string;
  roleCd: string;
  memberStatusCd: string;
  joinedAt: string;
  updatedAt: string;
  invitedBy?: string;
};

type CommunityRecord = {
  communityId: string;
  communityNm: string;
  communityDesc: string;
  communityTypeCd: string;
  visibilityCd: string;
  joinPolicyCd: string;
  introText: string;
  postTemplateHtml: string;
  iconFilePath: string;
  coverFilePath: string;
  ownerUserId: string;
  ownerUserNm: string;
  favoriteUserIds: string[];
  memberships: CommunityMembership[];
  closedYn: string;
  removedYn: string;
  createdAt: string;
  updatedAt: string;
};

function makeId(prefix: string) {
  return `${prefix}_${Date.now()}_${db.collection(COMMUNITIES).doc().id.slice(0, 8)}`;
}

function isAdminRole(userRole: string) {
  return String(userRole || "").includes("ADMIN");
}

function normalizeStringArray(value: unknown): string[] {
  return Array.isArray(value)
    ? value.map((item) => trimmed(item)).filter(Boolean)
    : [];
}

function normalizeMembership(item: unknown, defaults: Partial<CommunityMembership> = {}): CommunityMembership {
  const source = asObject(item);
  return {
    userId: trimmed(source.userId) || defaults.userId || "",
    roleCd: trimmed(source.roleCd) || defaults.roleCd || "member",
    memberStatusCd: trimmed(source.memberStatusCd) || defaults.memberStatusCd || "active",
    joinedAt: trimmed(source.joinedAt) || defaults.joinedAt || nowIso(),
    updatedAt: trimmed(source.updatedAt) || defaults.updatedAt || nowIso(),
    invitedBy: trimmed(source.invitedBy) || defaults.invitedBy || "",
  };
}

function normalizeCommunityRecord(item: unknown): CommunityRecord {
  const source = asObject(item);
  const memberships = Array.isArray(source.memberships)
    ? source.memberships.map((entry) => normalizeMembership(entry)).filter((entry) => entry.userId)
    : [];
  const ownerMembership = memberships.find((entry) => entry.roleCd === "owner") || memberships[0] || null;

  return {
    communityId: trimmed(source.communityId),
    communityNm: trimmed(source.communityNm) || "이름 없는 커뮤니티",
    communityDesc: trimmed(source.communityDesc),
    communityTypeCd: trimmed(source.communityTypeCd) || "general",
    visibilityCd: trimmed(source.visibilityCd) || "public",
    joinPolicyCd: trimmed(source.joinPolicyCd) || "instant",
    introText: trimmed(source.introText),
    postTemplateHtml: trimmed(source.postTemplateHtml) || "<p>새 게시글을 작성해 보세요.</p>",
    iconFilePath: trimmed(source.iconFilePath),
    coverFilePath: trimmed(source.coverFilePath),
    ownerUserId: trimmed(source.ownerUserId) || ownerMembership?.userId || "",
    ownerUserNm: trimmed(source.ownerUserNm),
    favoriteUserIds: normalizeStringArray(source.favoriteUserIds),
    memberships,
    closedYn: trimmed(source.closedYn) || "N",
    removedYn: trimmed(source.removedYn) || "N",
    createdAt: trimmed(source.createdAt) || nowIso(),
    updatedAt: trimmed(source.updatedAt) || nowIso(),
  };
}

async function ensureCommunitySeedData() {
  await ensureBaselineData();
  await ensureWorkSeedData();
  await listCommunitiesData();
}

async function getCommunityOrThrow(communityId: string): Promise<CommunityRecord> {
  await ensureCommunitySeedData();
  const snapshot = await db.collection(COMMUNITIES).doc(String(communityId)).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "커뮤니티를 찾을 수 없습니다.");
  }
  return normalizeCommunityRecord(snapshot.data());
}

async function saveCommunity(record: CommunityRecord) {
  await db.collection(COMMUNITIES).doc(record.communityId).set({
    ...record,
    updatedAt: nowIso(),
  }, { merge: true });
}

async function getCommunityOrder(userId: string): Promise<string[]> {
  const snapshot = await db.collection(COLLECTIONS.sessionUsers).doc(userId).collection(COMMUNITY_SETTINGS).doc("order").get();
  return normalizeStringArray(snapshot.data()?.communityIds);
}

async function setCommunityOrder(userId: string, communityIds: string[]) {
  await db.collection(COLLECTIONS.sessionUsers).doc(userId).collection(COMMUNITY_SETTINGS).doc("order").set({
    communityIds,
    updatedAt: nowIso(),
  }, { merge: true });
}

function membershipFor(record: CommunityRecord, userId: string) {
  return record.memberships.find((item) => item.userId === userId) || null;
}

function isManageable(record: CommunityRecord, userId: string, userRole = "") {
  if (isAdminRole(userRole)) {
    return true;
  }
  const membership = membershipFor(record, userId);
  return Boolean(membership && membership.memberStatusCd === "active" && ["owner", "operator"].includes(membership.roleCd));
}

function isJoinable(record: CommunityRecord, membership: CommunityMembership | null) {
  if (record.closedYn === "Y" || record.removedYn === "Y") {
    return false;
  }
  if (membership?.memberStatusCd === "active") {
    return false;
  }
  if (record.communityTypeCd === "org") {
    return false;
  }
  if (record.visibilityCd === "private" && record.joinPolicyCd === "invite_only") {
    return false;
  }
  return true;
}

async function buildMemberRecord(member: CommunityMembership, currentUserId = "") {
  const directoryUser = await getDirectoryUser(member.userId);
  return {
    userId: member.userId,
    userNm: directoryUser?.userNm || member.userId,
    deptNm: directoryUser?.deptNm || "",
    jbgdNm: directoryUser?.jbgdNm || "",
    roleCd: member.roleCd,
    memberStatusCd: member.memberStatusCd,
    statusCd: member.memberStatusCd,
    me: member.userId === currentUserId,
    joinedAt: member.joinedAt,
    updatedAt: member.updatedAt,
  };
}

function sortCommunities(items: Array<Record<string, unknown>>, orderedIds: string[]) {
  const orderMap = new Map(orderedIds.map((communityId, index) => [String(communityId), index]));
  return [...items].sort((left, right) => {
    const leftOrder = orderMap.has(String(left.communityId)) ? Number(orderMap.get(String(left.communityId))) : Number.MAX_SAFE_INTEGER;
    const rightOrder = orderMap.has(String(right.communityId)) ? Number(orderMap.get(String(right.communityId))) : Number.MAX_SAFE_INTEGER;
    if (leftOrder !== rightOrder) {
      return leftOrder - rightOrder;
    }
    return String(left.communityNm || "").localeCompare(String(right.communityNm || ""), "ko");
  });
}

async function summarizeCommunity(record: CommunityRecord, currentUserId: string, userRole = "") {
  const membership = membershipFor(record, currentUserId);
  const ownerProfile = record.ownerUserNm || (await getDirectoryUser(record.ownerUserId))?.userNm || record.ownerUserId;

  return {
    communityId: record.communityId,
    communityNm: record.communityNm,
    communityDesc: record.communityDesc,
    communityTypeCd: record.communityTypeCd,
    visibilityCd: record.visibilityCd,
    joinPolicyCd: record.joinPolicyCd,
    introText: record.introText,
    postTemplateHtml: record.postTemplateHtml,
    iconFilePath: record.iconFilePath,
    coverFilePath: record.coverFilePath,
    ownerUserId: record.ownerUserId,
    ownerUserNm: ownerProfile,
    favoriteYn: record.favoriteUserIds.includes(currentUserId) ? "Y" : "N",
    joined: membership?.memberStatusCd === "active",
    joinedYn: membership?.memberStatusCd === "active" ? "Y" : "N",
    joinable: isJoinable(record, membership),
    manageable: isManageable(record, currentUserId, userRole),
    memberRoleCd: membership?.roleCd || "",
    roleCd: membership?.roleCd || "",
    memberStatusCd: membership?.memberStatusCd || "discover",
    memberCount: record.memberships.filter((item) => item.memberStatusCd === "active").length,
    closedYn: record.closedYn,
    removedYn: record.removedYn,
    createdAt: record.createdAt,
    updatedAt: record.updatedAt,
  };
}

async function filterCommunities(currentUserId: string, userRole: string, params: JsonMap) {
  await ensureCommunitySeedData();
  const keyword = trimmed(params.q).toLowerCase();
  const view = trimmed(params.view) || "joined";
  const manageableOnly = params.manageable === true || trimmed(params.manageable) === "true";
  const communities = (await listCommunitiesData())
    .map((item) => normalizeCommunityRecord(item))
    .filter((item) => item.removedYn !== "Y");

  const summarized = await Promise.all(communities.map((community) => summarizeCommunity(community, currentUserId, userRole)));
  const filtered = summarized.filter((community) => {
    if (keyword && ![community.communityNm, community.communityDesc, community.introText].some((value) => String(value || "").toLowerCase().includes(keyword))) {
      return false;
    }
    if (manageableOnly || view === "manageable") {
      return community.manageable;
    }
    if (view === "joined") {
      return community.memberStatusCd === "active";
    }
    if (view === "favorites") {
      return community.favoriteYn === "Y";
    }
    if (view === "discover") {
      return community.memberStatusCd !== "active";
    }
    return true;
  });

  return sortCommunities(filtered, await getCommunityOrder(currentUserId));
}

function dedupeMemberships(items: CommunityMembership[]) {
  const byUser = new Map<string, CommunityMembership>();
  items.forEach((item) => {
    if (!item.userId) {
      return;
    }
    byUser.set(item.userId, item);
  });
  return Array.from(byUser.values());
}

function ensureOwnerMemberships(record: CommunityRecord, ownerId: string) {
  const next = record.memberships.filter((item) => item.userId !== ownerId);
  next.unshift({
    userId: ownerId,
    roleCd: "owner",
    memberStatusCd: "active",
    joinedAt: nowIso(),
    updatedAt: nowIso(),
  });
  record.memberships = dedupeMemberships(next);
  return record;
}

export const communityGetList = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  return filterCommunities(currentUser.userId, currentUser.userRole, asObject(request.data));
});

export const communitySearch = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  return filterCommunities(currentUser.userId, currentUser.userRole, {
    ...asObject(request.data),
    view: trimmed(asObject(request.data).view) || "discover",
  });
});

export const communityGetDetail = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const communityId = trimmed(asObject(request.data).communityId);
  if (!communityId) {
    throw new HttpsError("invalid-argument", "communityId is required.");
  }
  const record = await getCommunityOrThrow(communityId);
  return summarizeCommunity(record, currentUser.userId, currentUser.userRole);
});

export const communityGetMembers = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const communityId = trimmed(data.communityId);
  const status = trimmed(data.status);
  const record = await getCommunityOrThrow(communityId);
  const members = await Promise.all(
    record.memberships
      .filter((member) => !status || member.memberStatusCd === status)
      .map((member) => buildMemberRecord(member, currentUser.userId))
  );
  return members.sort((left, right) => {
    const leftRank = left.roleCd === "owner" ? 0 : left.roleCd === "operator" ? 1 : 2;
    const rightRank = right.roleCd === "owner" ? 0 : right.roleCd === "operator" ? 1 : 2;
    return leftRank - rightRank || left.userNm.localeCompare(right.userNm, "ko");
  });
});

export const communityGetRequests = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const communityId = trimmed(asObject(request.data).communityId);
  const record = await getCommunityOrThrow(communityId);
  if (!isManageable(record, currentUser.userId, currentUser.userRole)) {
    throw new HttpsError("permission-denied", "요청 목록을 확인할 권한이 없습니다.");
  }
  const requests = await Promise.all(
    record.memberships
      .filter((member) => member.memberStatusCd === "pending")
      .map((member) => buildMemberRecord(member, currentUser.userId))
  );
  return requests;
});

export const communityCreate = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = asObject(request.data);
  const communityNm = trimmed(payload.communityNm);
  const communityDesc = trimmed(payload.communityDesc);
  if (!communityNm || !communityDesc) {
    throw new HttpsError("invalid-argument", "communityNm and communityDesc are required.");
  }

  const communityId = makeId("COMM");
  const operatorUserIds = normalizeStringArray(payload.operatorUserIds);
  const memberUserIds = normalizeStringArray(payload.memberUserIds);
  const createdAt = nowIso();
  const memberships = dedupeMemberships([
    {
      userId: currentUser.userId,
      roleCd: "owner",
      memberStatusCd: "active",
      joinedAt: createdAt,
      updatedAt: createdAt,
    },
    ...operatorUserIds.map((userId) => ({
      userId,
      roleCd: "operator",
      memberStatusCd: "active",
      joinedAt: createdAt,
      updatedAt: createdAt,
      invitedBy: currentUser.userId,
    })),
    ...memberUserIds.map((userId) => ({
      userId,
      roleCd: "member",
      memberStatusCd: "active",
      joinedAt: createdAt,
      updatedAt: createdAt,
      invitedBy: currentUser.userId,
    })),
  ]);

  const record: CommunityRecord = {
    communityId,
    communityNm,
    communityDesc,
    communityTypeCd: trimmed(payload.communityTypeCd) || "general",
    visibilityCd: trimmed(payload.visibilityCd) || "public",
    joinPolicyCd: trimmed(payload.joinPolicyCd) || "instant",
    introText: trimmed(payload.introText) || communityDesc,
    postTemplateHtml: trimmed(payload.postTemplateHtml) || "<p>새 게시글을 작성해 보세요.</p>",
    iconFilePath: trimmed(payload.iconFilePath) || trimmed(payload.iconFileUrl),
    coverFilePath: trimmed(payload.coverFilePath) || trimmed(payload.coverFileUrl),
    ownerUserId: currentUser.userId,
    ownerUserNm: currentUser.userNm,
    favoriteUserIds: [],
    memberships,
    closedYn: "N",
    removedYn: "N",
    createdAt,
    updatedAt: createdAt,
  };

  await saveCommunity(record);
  return { communityId };
});

export const communityUpdate = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const communityId = trimmed(data.communityId);
  const payload = asObject(data.payload || data);
  const record = await getCommunityOrThrow(communityId);
  if (!isManageable(record, currentUser.userId, currentUser.userRole)) {
    throw new HttpsError("permission-denied", "커뮤니티를 수정할 권한이 없습니다.");
  }

  const operatorUserIds = normalizeStringArray(payload.operatorUserIds);
  const updatedAt = nowIso();
  const nextMemberships = record.memberships.map((member) => {
    if (member.roleCd === "owner") {
      return member;
    }
    if (member.memberStatusCd !== "active") {
      return member;
    }
    return {
      ...member,
      roleCd: operatorUserIds.includes(member.userId) ? "operator" : "member",
      updatedAt,
    };
  });

  const nextRecord = ensureOwnerMemberships({
    ...record,
    communityNm: trimmed(payload.communityNm) || record.communityNm,
    communityDesc: trimmed(payload.communityDesc) || record.communityDesc,
    communityTypeCd: trimmed(payload.communityTypeCd) || record.communityTypeCd,
    visibilityCd: trimmed(payload.visibilityCd) || record.visibilityCd,
    joinPolicyCd: trimmed(payload.joinPolicyCd) || record.joinPolicyCd,
    introText: trimmed(payload.introText) || record.introText,
    postTemplateHtml: trimmed(payload.postTemplateHtml) || record.postTemplateHtml,
    iconFilePath: trimmed(payload.iconFilePath) || trimmed(payload.iconFileUrl) || record.iconFilePath,
    coverFilePath: trimmed(payload.coverFilePath) || trimmed(payload.coverFileUrl) || record.coverFilePath,
    ownerUserNm: record.ownerUserNm || currentUser.userNm,
    memberships: nextMemberships,
    updatedAt,
  }, record.ownerUserId);

  await saveCommunity(nextRecord);
  return { communityId };
});

export const communityRemove = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const communityId = trimmed(asObject(request.data).communityId);
  const record = await getCommunityOrThrow(communityId);
  if (!isManageable(record, currentUser.userId, currentUser.userRole)) {
    throw new HttpsError("permission-denied", "커뮤니티를 삭제할 권한이 없습니다.");
  }
  await db.collection(COMMUNITIES).doc(communityId).delete();
  return { deleted: true };
});

export const communityClose = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const communityId = trimmed(asObject(request.data).communityId);
  const record = await getCommunityOrThrow(communityId);
  if (!isManageable(record, currentUser.userId, currentUser.userRole)) {
    throw new HttpsError("permission-denied", "커뮤니티를 닫을 권한이 없습니다.");
  }
  record.closedYn = "Y";
  record.updatedAt = nowIso();
  await saveCommunity(record);
  return { communityId, closedYn: "Y" };
});

export const communityJoin = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const communityId = trimmed(asObject(request.data).communityId);
  const record = await getCommunityOrThrow(communityId);
  const membership = membershipFor(record, currentUser.userId);
  if (!isJoinable(record, membership)) {
    throw new HttpsError("failed-precondition", "가입할 수 없는 커뮤니티입니다.");
  }
  const status = record.joinPolicyCd === "approval" ? "pending" : "active";
  const nextMemberships = record.memberships.filter((item) => item.userId !== currentUser.userId);
  nextMemberships.push({
    userId: currentUser.userId,
    roleCd: membership?.roleCd || "member",
    memberStatusCd: status,
    joinedAt: membership?.joinedAt || nowIso(),
    updatedAt: nowIso(),
  });
  record.memberships = dedupeMemberships(nextMemberships);
  record.updatedAt = nowIso();
  await saveCommunity(record);
  return { communityId, memberStatusCd: status };
});

export const communityLeave = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const communityId = trimmed(asObject(request.data).communityId);
  const record = await getCommunityOrThrow(communityId);
  const membership = membershipFor(record, currentUser.userId);
  if (!membership || membership.memberStatusCd !== "active") {
    return { communityId, memberStatusCd: membership?.memberStatusCd || "left" };
  }
  if (membership.roleCd === "owner") {
    throw new HttpsError("failed-precondition", "소유자는 커뮤니티를 바로 나갈 수 없습니다.");
  }
  record.memberships = record.memberships.map((item) => item.userId === currentUser.userId ? {
    ...item,
    memberStatusCd: "left",
    updatedAt: nowIso(),
  } : item);
  record.updatedAt = nowIso();
  await saveCommunity(record);
  return { communityId, memberStatusCd: "left" };
});

export const communityAddMembers = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const communityId = trimmed(data.communityId);
  const userIds = normalizeStringArray(data.userIds);
  const record = await getCommunityOrThrow(communityId);
  if (!isManageable(record, currentUser.userId, currentUser.userRole)) {
    throw new HttpsError("permission-denied", "멤버를 추가할 권한이 없습니다.");
  }
  const updatedAt = nowIso();
  const remaining = record.memberships.filter((item) => !userIds.includes(item.userId));
  const additions = userIds.map((userId) => ({
    userId,
    roleCd: "member",
    memberStatusCd: "active",
    joinedAt: updatedAt,
    updatedAt,
    invitedBy: currentUser.userId,
  }));
  record.memberships = dedupeMemberships([...remaining, ...additions]);
  record.updatedAt = updatedAt;
  await saveCommunity(record);
  return { communityId, count: userIds.length };
});

export const communityRemoveMember = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const communityId = trimmed(data.communityId);
  const userId = trimmed(data.userId);
  const record = await getCommunityOrThrow(communityId);
  if (!isManageable(record, currentUser.userId, currentUser.userRole)) {
    throw new HttpsError("permission-denied", "멤버를 제거할 권한이 없습니다.");
  }
  record.memberships = record.memberships.map((member) => member.userId === userId ? {
    ...member,
    memberStatusCd: "removed",
    updatedAt: nowIso(),
  } : member);
  record.updatedAt = nowIso();
  await saveCommunity(record);
  return { communityId, userId, removed: true };
});

export const communityUpdateRole = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const communityId = trimmed(data.communityId);
  const userId = trimmed(data.userId);
  const roleCd = trimmed(data.roleCd) || "member";
  const record = await getCommunityOrThrow(communityId);
  if (!isManageable(record, currentUser.userId, currentUser.userRole)) {
    throw new HttpsError("permission-denied", "권한을 변경할 수 없습니다.");
  }
  record.memberships = record.memberships.map((member) => member.userId === userId ? {
    ...member,
    roleCd: member.roleCd === "owner" ? "owner" : roleCd,
    updatedAt: nowIso(),
  } : member);
  record.updatedAt = nowIso();
  await saveCommunity(record);
  return { communityId, userId, roleCd };
});

export const communityApproveRequest = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const communityId = trimmed(data.communityId);
  const userId = trimmed(data.userId);
  const record = await getCommunityOrThrow(communityId);
  if (!isManageable(record, currentUser.userId, currentUser.userRole)) {
    throw new HttpsError("permission-denied", "가입 요청을 승인할 권한이 없습니다.");
  }
  record.memberships = record.memberships.map((member) => member.userId === userId ? {
    ...member,
    memberStatusCd: "active",
    updatedAt: nowIso(),
  } : member);
  record.updatedAt = nowIso();
  await saveCommunity(record);
  return { communityId, userId, statusCd: "active" };
});

export const communityRejectRequest = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const communityId = trimmed(data.communityId);
  const userId = trimmed(data.userId);
  const record = await getCommunityOrThrow(communityId);
  if (!isManageable(record, currentUser.userId, currentUser.userRole)) {
    throw new HttpsError("permission-denied", "가입 요청을 반려할 권한이 없습니다.");
  }
  record.memberships = record.memberships.map((member) => member.userId === userId ? {
    ...member,
    memberStatusCd: "rejected",
    updatedAt: nowIso(),
  } : member);
  record.updatedAt = nowIso();
  await saveCommunity(record);
  return { communityId, userId, statusCd: "rejected" };
});

export const communityFavorite = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const communityId = trimmed(data.communityId);
  const favoriteYn = trimmed(data.favoriteYn) || "N";
  const record = await getCommunityOrThrow(communityId);
  const nextFavorites = new Set(record.favoriteUserIds);
  if (favoriteYn === "Y") {
    nextFavorites.add(currentUser.userId);
  } else {
    nextFavorites.delete(currentUser.userId);
  }
  record.favoriteUserIds = Array.from(nextFavorites);
  record.updatedAt = nowIso();
  await saveCommunity(record);
  return { communityId, favoriteYn };
});

export const communitySaveOrder = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const communityIds = normalizeStringArray(asObject(request.data).communityIds);
  await setCommunityOrder(currentUser.userId, communityIds);
  return { success: true, communityIds };
});

export const communitySyncOrg = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  if (!isAdminRole(currentUser.userRole)) {
    throw new HttpsError("permission-denied", "조직 커뮤니티를 동기화할 권한이 없습니다.");
  }

  await ensureCommunitySeedData();
  const [departmentsSnapshot, users] = await Promise.all([
    db.collection(COLLECTIONS.departments).get(),
    listDirectoryUsersData(),
  ]);

  const batch = db.batch();
  departmentsSnapshot.docs.forEach((doc) => {
    const department = asObject(doc.data());
    const deptId = trimmed(department.deptId);
    const deptNm = trimmed(department.deptNm);
    if (!deptId || !deptNm) {
      return;
    }
    const communityId = `ORG_${deptId}`;
    const members = users
      .filter((user) => user.deptId === deptId && user.rsgntnYn !== "Y")
      .map((user) => ({
        userId: user.userId,
        roleCd: user.userId === currentUser.userId ? "owner" : "member",
        memberStatusCd: "active",
        joinedAt: nowIso(),
        updatedAt: nowIso(),
      }));
    batch.set(db.collection(COMMUNITIES).doc(communityId), {
      communityId,
      communityNm: `${deptNm} 커뮤니티`,
      communityDesc: `${deptNm} 조직 공지와 자료를 공유합니다.`,
      communityTypeCd: "org",
      visibilityCd: "org",
      joinPolicyCd: "instant",
      introText: `${deptNm} 조직 전용 공간`,
      postTemplateHtml: "<p>조직 공지와 자료를 공유해 보세요.</p>",
      iconFilePath: "",
      coverFilePath: "",
      ownerUserId: currentUser.userId,
      ownerUserNm: currentUser.userNm,
      favoriteUserIds: [],
      memberships: members,
      closedYn: "N",
      removedYn: "N",
      createdAt: nowIso(),
      updatedAt: nowIso(),
    }, { merge: true });
  });

  await batch.commit();
  return { success: true };
});
