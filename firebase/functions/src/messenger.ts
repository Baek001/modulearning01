import { Buffer } from "node:buffer";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
  COLLECTIONS,
  CallableAuth,
  asObject,
  assertSignedIn,
  db,
  numberValue,
  nowIso,
  toIso,
  trimmed,
} from "./shared.js";
import { getDirectoryUser, listDirectoryUsersData, loadProfile } from "./profile.js";

type Attachment = Record<string, unknown>;
type SenderProfile = {
  userId: string;
  userNm: string;
  deptNm: string;
  jbgdNm: string;
};
type RoomMember = {
  userId: string;
  displayName: string;
  deptNm: string;
  jbgdNm: string;
  roleCd: string;
  memberStatusCd: string;
  joinedAt: string;
  updatedAt: string;
  lastReadAt: string;
  unreadCount: number;
  notifyEnabled: boolean;
};

function now() {
  return nowIso();
}

function roomTypeLabel(roomTypeCd: string) {
  switch (roomTypeCd) {
    case "private":
      return "1:1";
    case "self":
      return "me";
    case "community":
      return "community";
    default:
      return "group";
  }
}

function directRoomId(leftUserId: string, rightUserId: string) {
  return `direct_${[leftUserId, rightUserId].sort().join("_")}`;
}

function roomRef(roomId: string) {
  return db.collection(COLLECTIONS.messengerRooms).doc(roomId);
}

function roomMembersRef(roomId: string) {
  return roomRef(roomId).collection("members");
}

function roomMessagesRef(roomId: string) {
  return roomRef(roomId).collection("messages");
}

function userRoomRef(userId: string, roomId: string) {
  return db.collection(COLLECTIONS.sessionUsers).doc(userId).collection("rooms").doc(roomId);
}

function sanitizeAttachments(value: unknown): Attachment[] {
  return Array.isArray(value)
    ? value.filter((item) => item && typeof item === "object") as Attachment[]
    : [];
}

function sanitizeMessengerPayload(data: unknown) {
  const source = asObject(data);
  return {
    roomId: trimmed(source.roomId),
    messageText: trimmed(source.messageText) || trimmed(source.contents),
    msgType: trimmed(source.msgType) || trimmed(source.msgTypeCd),
    attachments: sanitizeAttachments(source.attachments),
    forwardPreview: trimmed(source.forwardPreview),
  };
}

function normalizeMember(data: unknown, fallbackUserId = ""): RoomMember {
  const source = asObject(data);
  return {
    userId: trimmed(source.userId) || fallbackUserId,
    displayName: trimmed(source.displayName) || trimmed(source.userNm) || fallbackUserId,
    deptNm: trimmed(source.deptNm),
    jbgdNm: trimmed(source.jbgdNm),
    roleCd: trimmed(source.roleCd) || "member",
    memberStatusCd: trimmed(source.memberStatusCd) || "active",
    joinedAt: toIso(source.joinedAt) || now(),
    updatedAt: toIso(source.updatedAt) || now(),
    lastReadAt: toIso(source.lastReadAt),
    unreadCount: numberValue(source.unreadCount, 0),
    notifyEnabled: source.notifyEnabled !== false,
  };
}

function normalizePanelRoom(roomId: string, data: Record<string, unknown> = {}) {
  const roomTypeCd = trimmed(data.roomType) || trimmed(data.roomTypeCd) || "group";
  return {
    msgrId: roomId,
    msgrNm: trimmed(data.roomNm) || trimmed(data.msgrNm) || "이름 없는 대화방",
    roomTypeCd,
    msgrTypeCd: roomTypeCd,
    unreadCount: numberValue(data.unreadCount, 0),
    unreadCnt: numberValue(data.unreadCount, 0),
    notifyEnabled: data.notifyEnabled !== false,
    lastMsgCont: trimmed(data.lastMessage) || trimmed(data.lastMsgCont),
    lastMsgDt: toIso(data.lastMessageAt) || toIso(data.lastMsgDt) || toIso(data.updatedAt),
    crtDt: toIso(data.createdAt) || toIso(data.crtDt) || toIso(data.updatedAt),
  };
}

function normalizeMessageResult(messageId: string, data: Record<string, unknown>) {
  const contents = trimmed(data.messageText) || (trimmed(data.deletedYn) === "Y" ? "삭제된 메시지입니다." : "");
  return {
    msgContId: messageId,
    userId: trimmed(data.senderId),
    userNm: trimmed(data.senderName) || trimmed(data.userNm),
    deptNm: trimmed(data.deptNm),
    jbgdNm: trimmed(data.jbgdNm),
    contents,
    msgTypeCd: trimmed(data.msgType) || "text",
    attachments: sanitizeAttachments(data.attachments),
    sendDt: toIso(data.createdAt) || toIso(data.sendDt),
    unreadCount: numberValue(data.unreadCount, 0),
    forwardPreview: trimmed(data.forwardPreview),
  };
}

function csvEscape(value: unknown) {
  const text = String(value ?? "");
  if (/[\",\\n]/.test(text)) {
    return `"${text.replace(/"/g, "\"\"")}"`;
  }
  return text;
}

function messagePreview(messageText: string, attachments: Attachment[]) {
  if (messageText) {
    return messageText;
  }
  if (attachments.length > 0) {
    return `파일 ${attachments.length}개`;
  }
  return "";
}

async function resolveDirectoryMap() {
  const users = await listDirectoryUsersData();
  return new Map(users.map((user) => [user.userId, user]));
}

async function getRoomOrThrow(roomId: string) {
  const snapshot = await roomRef(roomId).get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "대화방을 찾을 수 없습니다.");
  }
  return {
    roomId,
    roomData: snapshot.data() as Record<string, unknown>,
    ref: snapshot.ref,
  };
}

async function listMembers(roomId: string): Promise<RoomMember[]> {
  const snapshot = await roomMembersRef(roomId).get();
  return snapshot.docs.map((doc) => normalizeMember(doc.data(), doc.id));
}

async function getActiveMember(roomId: string, userId: string) {
  const snapshot = await roomMembersRef(roomId).doc(userId).get();
  if (!snapshot.exists) {
    return null;
  }
  const member = normalizeMember(snapshot.data(), userId);
  return member.memberStatusCd === "active" ? member : null;
}

async function requireActiveMember(roomId: string, userId: string) {
  const member = await getActiveMember(roomId, userId);
  if (!member) {
    throw new HttpsError("permission-denied", "대화방에 참여 중인 사용자만 사용할 수 있습니다.");
  }
  return member;
}

async function requireManager(roomId: string, userId: string) {
  const member = await requireActiveMember(roomId, userId);
  if (!["owner", "operator"].includes(member.roleCd)) {
    throw new HttpsError("permission-denied", "대화방을 관리할 권한이 없습니다.");
  }
  return member;
}

async function buildMemberRecord(userId: string, roleCd: string, options: Partial<RoomMember> = {}): Promise<RoomMember> {
  const directoryUser = await getDirectoryUser(userId);
  return {
    userId,
    displayName: directoryUser?.userNm || userId,
    deptNm: directoryUser?.deptNm || "",
    jbgdNm: directoryUser?.jbgdNm || "",
    roleCd,
    memberStatusCd: options.memberStatusCd || "active",
    joinedAt: options.joinedAt || now(),
    updatedAt: options.updatedAt || now(),
    lastReadAt: options.lastReadAt || "",
    unreadCount: options.unreadCount || 0,
    notifyEnabled: options.notifyEnabled !== false,
  };
}

async function syncUserRoomSummaries(roomId: string) {
  const [{ roomData }, members, directoryMap] = await Promise.all([
    getRoomOrThrow(roomId),
    listMembers(roomId),
    resolveDirectoryMap(),
  ]);

  const roomTypeCd = trimmed(roomData.roomType) || "group";
  const activeMembers = members.filter((member) => member.memberStatusCd === "active");
  const baseRoomName = trimmed(roomData.roomNm) || `${roomTypeLabel(roomTypeCd)} 대화방`;
  const lastMessage = trimmed(roomData.lastMessage);
  const lastMessageAt = toIso(roomData.lastMessageAt) || now();
  const createdAt = toIso(roomData.createdAt) || lastMessageAt;
  const batch = db.batch();

  activeMembers.forEach((member) => {
    let resolvedName = baseRoomName;
    if (roomTypeCd === "private") {
      const other = activeMembers.find((item) => item.userId !== member.userId);
      resolvedName = other?.displayName || directoryMap.get(other?.userId || "")?.userNm || baseRoomName;
    } else if (roomTypeCd === "self") {
      resolvedName = `${member.displayName || member.userId} (me)`;
    }

    batch.set(userRoomRef(member.userId, roomId), {
      roomId,
      roomNm: resolvedName,
      roomType: roomTypeCd,
      unreadCount: member.unreadCount,
      notifyEnabled: member.notifyEnabled,
      lastMessage,
      lastMessageAt,
      createdAt,
      updatedAt: now(),
    }, { merge: true });
  });

  members
    .filter((member) => member.memberStatusCd !== "active")
    .forEach((member) => batch.delete(userRoomRef(member.userId, roomId)));

  await batch.commit();
}

async function persistRoomMessage(roomId: string, sender: SenderProfile, payload: ReturnType<typeof sanitizeMessengerPayload>) {
  const { roomData, ref } = await getRoomOrThrow(roomId);
  const members = await listMembers(roomId);
  const activeMembers = members.filter((member) => member.memberStatusCd === "active");
  const createdAt = now();
  const attachments = payload.attachments;
  const preview = messagePreview(payload.messageText, attachments);
  const msgTypeCd = payload.msgType || (attachments.length > 0 && !payload.messageText ? "file" : "text");
  const messageRef = roomMessagesRef(roomId).doc();
  const batch = db.batch();

  batch.set(messageRef, {
    senderId: sender.userId,
    senderName: sender.userNm,
    deptNm: sender.deptNm,
    jbgdNm: sender.jbgdNm,
    messageText: payload.messageText,
    msgType: msgTypeCd,
    attachments,
    forwardPreview: payload.forwardPreview,
    createdAt,
    updatedAt: createdAt,
    deletedYn: "N",
  });

  activeMembers.forEach((member) => {
    const unreadCount = member.userId === sender.userId ? 0 : member.unreadCount + 1;
    const lastReadAt = member.userId === sender.userId ? createdAt : member.lastReadAt;
    batch.set(roomMembersRef(roomId).doc(member.userId), {
      unreadCount,
      lastReadAt,
      updatedAt: createdAt,
    }, { merge: true });
  });

  batch.set(ref, {
    lastMessage: preview,
    lastMessageAt: createdAt,
    updatedAt: createdAt,
    roomType: trimmed(roomData.roomType) || "group",
    roomNm: trimmed(roomData.roomNm),
    ownerId: trimmed(roomData.ownerId),
    memberIds: activeMembers.map((member) => member.userId),
    participantCount: activeMembers.length,
  }, { merge: true });

  await batch.commit();
  await syncUserRoomSummaries(roomId);

  return {
    roomId,
    messageId: messageRef.id,
    msgContId: messageRef.id,
    lastMessage: preview,
    sendDt: createdAt,
  };
}

async function appendSystemMessage(roomId: string, contents: string, actorUserId = "") {
  return persistRoomMessage(roomId, {
    userId: actorUserId || "system",
    userNm: "시스템",
    deptNm: "",
    jbgdNm: "",
  }, {
    roomId,
    messageText: contents,
    msgType: "system",
    attachments: [],
    forwardPreview: "",
  });
}

async function createSelfRoomForUser(userId: string, token: Record<string, unknown> = {}) {
  const profile = await loadProfile(userId, token);
  const roomId = `self_${profile.userId}`;
  const createdAt = now();

  await roomRef(roomId).set({
    roomNm: `${profile.userNm || profile.userId} (me)`,
    roomType: "self",
    ownerId: profile.userId,
    participantCount: 1,
    memberIds: [profile.userId],
    createdAt,
    updatedAt: createdAt,
    lastMessage: "",
    lastMessageAt: createdAt,
    pinnedMsgContId: "",
    pinnedMsgPreview: "",
  }, { merge: true });

  await roomMembersRef(roomId).doc(profile.userId).set({
    userId: profile.userId,
    displayName: profile.userNm,
    deptNm: profile.deptNm,
    jbgdNm: profile.jbgdNm,
    roleCd: "owner",
    memberStatusCd: "active",
    joinedAt: createdAt,
    updatedAt: createdAt,
    lastReadAt: createdAt,
    unreadCount: 0,
    notifyEnabled: true,
  }, { merge: true });

  await syncUserRoomSummaries(roomId);

  return {
    msgrId: roomId,
    msgrNm: `${profile.userNm || profile.userId} (me)`,
    roomTypeCd: "self",
    msgrTypeCd: "self",
  };
}

async function findOrCreatePrivateRoom(currentUserId: string, targetUserId: string, token: Record<string, unknown> = {}) {
  const [currentUser, targetUser] = await Promise.all([
    loadProfile(currentUserId, token),
    getDirectoryUser(targetUserId),
  ]);

  if (!targetUser?.userId) {
    throw new HttpsError("not-found", "대화 상대를 찾을 수 없습니다.");
  }

  const roomId = directRoomId(currentUser.userId, targetUser.userId);
  const createdAt = now();
  const snapshot = await roomRef(roomId).get();

  if (!snapshot.exists) {
    await roomRef(roomId).set({
      roomNm: `${currentUser.userNm}, ${targetUser.userNm}`,
      roomType: "private",
      ownerId: currentUser.userId,
      participantCount: 2,
      memberIds: [currentUser.userId, targetUser.userId],
      createdAt,
      updatedAt: createdAt,
      lastMessage: "",
      lastMessageAt: createdAt,
      pinnedMsgContId: "",
      pinnedMsgPreview: "",
    });

    await Promise.all([
      roomMembersRef(roomId).doc(currentUser.userId).set({
        userId: currentUser.userId,
        displayName: currentUser.userNm,
        deptNm: currentUser.deptNm,
        jbgdNm: currentUser.jbgdNm,
        roleCd: "owner",
        memberStatusCd: "active",
        joinedAt: createdAt,
        updatedAt: createdAt,
        lastReadAt: createdAt,
        unreadCount: 0,
        notifyEnabled: true,
      }),
      roomMembersRef(roomId).doc(targetUser.userId).set({
        userId: targetUser.userId,
        displayName: targetUser.userNm,
        deptNm: targetUser.deptNm,
        jbgdNm: targetUser.jbgdNm,
        roleCd: "member",
        memberStatusCd: "active",
        joinedAt: createdAt,
        updatedAt: createdAt,
        lastReadAt: "",
        unreadCount: 0,
        notifyEnabled: true,
      }),
    ]);
  }

  await syncUserRoomSummaries(roomId);

  return {
    msgrId: roomId,
    msgrNm: targetUser.userNm,
    roomTypeCd: "private",
    msgrTypeCd: "private",
  };
}

async function createRoomRecord(ownerUserId: string, roomNm: string, roomTypeCd: string, memberIds: string[]) {
  const nextRoomId = db.collection(COLLECTIONS.messengerRooms).doc().id;
  const createdAt = now();
  const uniqueMemberIds = Array.from(new Set(memberIds.filter(Boolean)));
  const directoryMap = await resolveDirectoryMap();

  await roomRef(nextRoomId).set({
    roomNm: roomNm || `${roomTypeLabel(roomTypeCd)} 대화방`,
    roomType: roomTypeCd,
    ownerId: ownerUserId,
    participantCount: uniqueMemberIds.length,
    memberIds: uniqueMemberIds,
    createdAt,
    updatedAt: createdAt,
    lastMessage: "",
    lastMessageAt: createdAt,
    pinnedMsgContId: "",
    pinnedMsgPreview: "",
  });

  await Promise.all(uniqueMemberIds.map(async (memberId) => {
    const user = directoryMap.get(memberId);
    await roomMembersRef(nextRoomId).doc(memberId).set({
      userId: memberId,
      displayName: user?.userNm || memberId,
      deptNm: user?.deptNm || "",
      jbgdNm: user?.jbgdNm || "",
      roleCd: memberId === ownerUserId ? "owner" : "member",
      memberStatusCd: "active",
      joinedAt: createdAt,
      updatedAt: createdAt,
      lastReadAt: memberId === ownerUserId ? createdAt : "",
      unreadCount: 0,
      notifyEnabled: true,
    });
  }));

  await syncUserRoomSummaries(nextRoomId);
  return nextRoomId;
}

async function getPinnedMessage(roomId: string, pinnedMsgContId: string) {
  if (!pinnedMsgContId) {
    return null;
  }
  const snapshot = await roomMessagesRef(roomId).doc(pinnedMsgContId).get();
  if (!snapshot.exists) {
    return null;
  }
  return normalizeMessageResult(snapshot.id, snapshot.data() as Record<string, unknown>);
}

async function listMessagesData(roomId: string) {
  const snapshot = await roomMessagesRef(roomId).orderBy("createdAt", "asc").get();
  return snapshot.docs.map((doc) => ({
    id: doc.id,
    data: doc.data() as Record<string, unknown>,
  }));
}

async function findMessageForUser(userId: string, msgContId: string) {
  const roomSnapshot = await db.collection(COLLECTIONS.sessionUsers).doc(userId).collection("rooms").get();
  for (const roomDoc of roomSnapshot.docs) {
    const messageSnapshot = await roomMessagesRef(roomDoc.id).doc(msgContId).get();
    if (messageSnapshot.exists) {
      return {
        roomId: roomDoc.id,
        messageId: messageSnapshot.id,
        messageData: messageSnapshot.data() as Record<string, unknown>,
      };
    }
  }
  return null;
}

export const createSelfRoom = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  return createSelfRoomForUser(uid, request.auth?.token || {});
});

export const messengerFindOrCreateRoom = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const targetUserId = trimmed(asObject(request.data).userId);
  if (!targetUserId) {
    throw new HttpsError("invalid-argument", "userId is required.");
  }
  return findOrCreatePrivateRoom(uid, targetUserId, request.auth?.token || {});
});

export const messengerGetCurrentUser = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  return loadProfile(uid, request.auth?.token || {});
});

export const messengerGetUsers = onCall(async (request) => {
  assertSignedIn(request.auth as CallableAuth);
  const users = await listDirectoryUsersData();
  return users
    .filter((user) => user.rsgntnYn !== "Y")
    .map((user) => ({
      userId: user.userId,
      userNm: user.userNm,
      deptNm: user.deptNm,
      jbgdNm: user.jbgdNm,
      profileImageUrl: user.profileImageUrl,
    }));
});

export const messengerGetPanel = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const snapshot = await db.collection(COLLECTIONS.sessionUsers).doc(currentUser.userId).collection("rooms").get();
  const rooms = snapshot.docs
    .map((doc) => normalizePanelRoom(doc.id, doc.data() as Record<string, unknown>))
    .sort((left, right) => String(right.lastMsgDt || right.crtDt || "").localeCompare(String(left.lastMsgDt || left.crtDt || "")));

  return {
    rooms,
    unreadRoomCount: rooms.filter((room) => room.unreadCount > 0).length,
    unreadMessageCount: rooms.reduce((sum, room) => sum + Number(room.unreadCount || 0), 0),
  };
});

export const messengerGetRooms = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const scope = trimmed(data.scope) || "all";
  const keyword = trimmed(data.keyword).toLowerCase();
  const type = trimmed(data.type) || "all";
  const snapshot = await db.collection(COLLECTIONS.sessionUsers).doc(currentUser.userId).collection("rooms").get();

  return snapshot.docs
    .map((doc) => normalizePanelRoom(doc.id, doc.data() as Record<string, unknown>))
    .filter((room) => (scope === "unread" ? room.unreadCount > 0 : true))
    .filter((room) => (type !== "all" ? room.roomTypeCd === type : true))
    .filter((room) => (!keyword ? true : [room.msgrNm, room.lastMsgCont].some((value) => String(value || "").toLowerCase().includes(keyword))))
    .sort((left, right) => String(right.lastMsgDt || right.crtDt || "").localeCompare(String(left.lastMsgDt || left.crtDt || "")));
});

export const messengerGetRoomDetail = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const roomId = trimmed(asObject(request.data).roomId);
  const { roomData } = await getRoomOrThrow(roomId);
  const members = await listMembers(roomId);
  const currentMember = members.find((member) => member.userId === currentUser.userId && member.memberStatusCd === "active");
  if (!currentMember) {
    throw new HttpsError("permission-denied", "대화방 정보를 볼 권한이 없습니다.");
  }

  const participants = members
    .filter((member) => member.memberStatusCd === "active")
    .sort((left, right) => {
      const leftRank = left.roleCd === "owner" ? 0 : left.roleCd === "operator" ? 1 : 2;
      const rightRank = right.roleCd === "owner" ? 0 : right.roleCd === "operator" ? 1 : 2;
      return leftRank - rightRank || left.displayName.localeCompare(right.displayName, "ko");
    })
    .map((member) => ({
      userId: member.userId,
      userNm: member.displayName,
      deptNm: member.deptNm,
      jbgdNm: member.jbgdNm,
      roleCd: member.roleCd,
      me: member.userId === currentUser.userId,
    }));

  return {
    room: {
      msgrId: roomId,
      msgrNm: trimmed(roomData.roomNm) || "이름 없는 대화방",
      roomTypeCd: trimmed(roomData.roomType) || "group",
      pinnedMsgContId: trimmed(roomData.pinnedMsgContId),
      pinnedMsgPreview: trimmed(roomData.pinnedMsgPreview),
      lastMsgCont: trimmed(roomData.lastMessage),
      lastMsgDt: toIso(roomData.lastMessageAt),
      crtDt: toIso(roomData.createdAt),
    },
    participants,
    currentUserRole: currentMember.roleCd,
    notifyEnabled: currentMember.notifyEnabled,
    pinnedMessage: await getPinnedMessage(roomId, trimmed(roomData.pinnedMsgContId)),
  };
});

export const messengerGetMessages = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const roomId = trimmed(asObject(request.data).roomId);
  await requireActiveMember(roomId, currentUser.userId);
  const messages = await listMessagesData(roomId);
  return messages.map((item) => normalizeMessageResult(item.id, item.data));
});

export const messengerSearchMessages = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const roomId = trimmed(data.roomId);
  const query = trimmed(data.q).toLowerCase();
  await requireActiveMember(roomId, currentUser.userId);
  const messages = await listMessagesData(roomId);
  return messages
    .map((item) => normalizeMessageResult(item.id, item.data))
    .filter((message) => {
      if (!query) {
        return true;
      }
      const attachmentNames = sanitizeAttachments(message.attachments).map((attachment) => trimmed(asObject(attachment).orgnFileNm || asObject(attachment).name));
      return [message.contents, message.userNm, ...attachmentNames]
        .some((value) => String(value || "").toLowerCase().includes(query));
    });
});

export const messengerCreateRoom = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const roomTypeCd = trimmed(data.roomTypeCd) || (data.isGroup ? "group" : "private");
  const roomNm = trimmed(data.roomNm);
  const requestedUserIds = Array.isArray(data.userIds) ? data.userIds.map((item) => trimmed(item)).filter(Boolean) : [];
  const memberIds = Array.from(new Set([currentUser.userId, ...requestedUserIds]));

  if (roomTypeCd === "self") {
    return createSelfRoomForUser(uid, request.auth?.token || {});
  }
  if (roomTypeCd === "private" && memberIds.length === 2) {
    const targetUserId = memberIds.find((userId) => userId !== currentUser.userId) || "";
    return findOrCreatePrivateRoom(uid, targetUserId, request.auth?.token || {});
  }
  if (memberIds.length === 0) {
    throw new HttpsError("invalid-argument", "userIds are required.");
  }

  const roomId = await createRoomRecord(currentUser.userId, roomNm, roomTypeCd || "group", memberIds);
  await appendSystemMessage(roomId, `${currentUser.userNm}님이 대화방을 만들었습니다.`, currentUser.userId);

  return {
    msgrId: roomId,
    msgrNm: roomNm || `${roomTypeLabel(roomTypeCd)} 대화방`,
    roomTypeCd,
    msgrTypeCd: roomTypeCd,
  };
});

export const sendMessengerMessage = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const payload = sanitizeMessengerPayload(request.data);

  if (!payload.roomId) {
    throw new HttpsError("invalid-argument", "roomId is required.");
  }
  if (!payload.messageText && payload.attachments.length === 0) {
    throw new HttpsError("invalid-argument", "messageText or attachments are required.");
  }

  await requireActiveMember(payload.roomId, currentUser.userId);
  return persistRoomMessage(payload.roomId, {
    userId: currentUser.userId,
    userNm: currentUser.userNm,
    deptNm: currentUser.deptNm,
    jbgdNm: currentUser.jbgdNm,
  }, payload);
});

export const markRoomRead = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const roomId = trimmed(asObject(request.data).roomId);
  await requireActiveMember(roomId, currentUser.userId);
  const updatedAt = now();

  await Promise.all([
    roomMembersRef(roomId).doc(currentUser.userId).set({
      unreadCount: 0,
      lastReadAt: updatedAt,
      updatedAt,
    }, { merge: true }),
    userRoomRef(currentUser.userId, roomId).set({
      unreadCount: 0,
      lastReadAt: updatedAt,
      updatedAt,
    }, { merge: true }),
  ]);

  return { roomId, unreadCount: 0 };
});

export const messengerInviteUsers = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const roomId = trimmed(data.roomId || data.msgrId);
  const userIds = Array.isArray(data.userIds) ? data.userIds.map((item) => trimmed(item)).filter(Boolean) : [];
  if (!roomId || userIds.length === 0) {
    throw new HttpsError("invalid-argument", "roomId and userIds are required.");
  }

  const { roomData } = await getRoomOrThrow(roomId);
  const roomTypeCd = trimmed(roomData.roomType) || "group";
  if (["private", "self"].includes(roomTypeCd)) {
    throw new HttpsError("failed-precondition", "이 대화방 유형에는 참여자를 초대할 수 없습니다.");
  }

  await requireManager(roomId, currentUser.userId);
  const members = await listMembers(roomId);
  const activeIds = new Set(members.filter((member) => member.memberStatusCd === "active").map((member) => member.userId));
  const nextIds = userIds.filter((userId) => !activeIds.has(userId));
  const createdAt = now();
  const additions = await Promise.all(nextIds.map((userId) => buildMemberRecord(userId, "member", {
    joinedAt: createdAt,
    updatedAt: createdAt,
  })));

  await Promise.all(additions.map((member) => roomMembersRef(roomId).doc(member.userId).set(member, { merge: true })));
  await roomRef(roomId).set({
    memberIds: Array.from(new Set([...activeIds, ...nextIds])),
    participantCount: activeIds.size + nextIds.length,
    updatedAt: createdAt,
  }, { merge: true });

  if (nextIds.length > 0) {
    await appendSystemMessage(roomId, `${nextIds.length}명의 사용자를 초대했습니다.`, currentUser.userId);
  } else {
    await syncUserRoomSummaries(roomId);
  }

  return { roomId, invitedCount: nextIds.length };
});

export const messengerKickUser = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const roomId = trimmed(data.roomId || data.msgrId);
  const userId = trimmed(data.userId);
  if (!roomId || !userId) {
    throw new HttpsError("invalid-argument", "roomId and userId are required.");
  }

  await requireManager(roomId, currentUser.userId);
  const targetMember = await requireActiveMember(roomId, userId);
  if (targetMember.roleCd === "owner") {
    throw new HttpsError("failed-precondition", "방장은 강퇴할 수 없습니다.");
  }

  await roomMembersRef(roomId).doc(userId).set({
    memberStatusCd: "removed",
    unreadCount: 0,
    updatedAt: now(),
  }, { merge: true });

  const members = await listMembers(roomId);
  const activeIds = members
    .filter((member) => member.userId !== userId && member.memberStatusCd === "active")
    .map((member) => member.userId);

  await roomRef(roomId).set({
    memberIds: activeIds,
    participantCount: activeIds.length,
    updatedAt: now(),
  }, { merge: true });
  await userRoomRef(userId, roomId).delete().catch(() => null);
  await appendSystemMessage(roomId, `${targetMember.displayName || userId}님을 대화방에서 제외했습니다.`, currentUser.userId);

  return { roomId, userId, kicked: true };
});

export const messengerRenameRoom = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const roomId = trimmed(data.roomId || data.msgrId);
  const msgrNm = trimmed(data.msgrNm || data.roomNm);
  if (!roomId || !msgrNm) {
    throw new HttpsError("invalid-argument", "roomId and msgrNm are required.");
  }

  await requireActiveMember(roomId, currentUser.userId);
  await roomRef(roomId).set({
    roomNm: msgrNm,
    updatedAt: now(),
  }, { merge: true });
  await syncUserRoomSummaries(roomId);

  return { roomId, msgrNm };
});

export const messengerGetParticipants = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const roomId = trimmed(asObject(request.data).roomId);
  await requireActiveMember(roomId, currentUser.userId);
  const members = await listMembers(roomId);
  return members
    .filter((member) => member.memberStatusCd === "active")
    .sort((left, right) => {
      const leftRank = left.roleCd === "owner" ? 0 : left.roleCd === "operator" ? 1 : 2;
      const rightRank = right.roleCd === "owner" ? 0 : right.roleCd === "operator" ? 1 : 2;
      return leftRank - rightRank || left.displayName.localeCompare(right.displayName, "ko");
    })
    .map((member) => ({
      userId: member.userId,
      userNm: member.displayName,
      deptNm: member.deptNm,
      jbgdNm: member.jbgdNm,
      roleCd: member.roleCd,
      me: member.userId === currentUser.userId,
    }));
});

export const messengerToggleNotify = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const roomId = trimmed(data.roomId || data.msgrId);
  const notifyEnabled = data.notifyEnabled !== false;
  await requireActiveMember(roomId, currentUser.userId);

  await Promise.all([
    roomMembersRef(roomId).doc(currentUser.userId).set({
      notifyEnabled,
      updatedAt: now(),
    }, { merge: true }),
    userRoomRef(currentUser.userId, roomId).set({
      notifyEnabled,
      updatedAt: now(),
    }, { merge: true }),
  ]);

  return { roomId, notifyEnabled };
});

export const messengerPinMessage = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const roomId = trimmed(data.roomId || data.msgrId);
  const msgContId = trimmed(data.msgContId);
  if (!roomId || !msgContId) {
    throw new HttpsError("invalid-argument", "roomId and msgContId are required.");
  }

  await requireManager(roomId, currentUser.userId);
  const messageSnapshot = await roomMessagesRef(roomId).doc(msgContId).get();
  if (!messageSnapshot.exists) {
    throw new HttpsError("not-found", "고정할 메시지를 찾을 수 없습니다.");
  }
  const message = normalizeMessageResult(messageSnapshot.id, messageSnapshot.data() as Record<string, unknown>);
  await roomRef(roomId).set({
    pinnedMsgContId: msgContId,
    pinnedMsgPreview: message.contents || message.forwardPreview || messagePreview("", sanitizeAttachments(message.attachments)),
    updatedAt: now(),
  }, { merge: true });

  return { roomId, msgContId };
});

export const messengerClearPinMessage = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const roomId = trimmed(asObject(request.data).roomId || asObject(request.data).msgrId);
  await requireManager(roomId, currentUser.userId);
  await roomRef(roomId).set({
    pinnedMsgContId: "",
    pinnedMsgPreview: "",
    updatedAt: now(),
  }, { merge: true });
  return { roomId, cleared: true };
});

export const messengerLeaveRoom = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const roomId = trimmed(asObject(request.data).roomId || asObject(request.data).msgrId);
  const currentMember = await requireActiveMember(roomId, currentUser.userId);
  const room = await getRoomOrThrow(roomId);
  const members = await listMembers(roomId);
  const remaining = members.filter((member) => member.userId !== currentUser.userId && member.memberStatusCd === "active");
  const nextOwnerId = currentMember.roleCd === "owner" ? remaining[0]?.userId || "" : "";

  await roomMembersRef(roomId).doc(currentUser.userId).set({
    memberStatusCd: "left",
    unreadCount: 0,
    updatedAt: now(),
  }, { merge: true });

  if (nextOwnerId) {
    await roomMembersRef(roomId).doc(nextOwnerId).set({
      roleCd: "owner",
      updatedAt: now(),
    }, { merge: true });
  }

  await roomRef(roomId).set({
    ownerId: nextOwnerId || trimmed(room.roomData.ownerId),
    memberIds: remaining.map((member) => member.userId),
    participantCount: remaining.length,
    updatedAt: now(),
  }, { merge: true });

  await userRoomRef(currentUser.userId, roomId).delete().catch(() => null);
  if (remaining.length > 0) {
    await appendSystemMessage(roomId, `${currentUser.userNm}님이 대화방을 나갔습니다.`, currentUser.userId);
  } else {
    await syncUserRoomSummaries(roomId);
  }

  return { roomId, left: true };
});

export const messengerDeleteMessage = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const roomId = trimmed(data.roomId || data.msgrId);
  const msgContId = trimmed(data.msgContId);
  if (!roomId || !msgContId) {
    throw new HttpsError("invalid-argument", "roomId and msgContId are required.");
  }

  const [messageSnapshot, currentMember, room] = await Promise.all([
    roomMessagesRef(roomId).doc(msgContId).get(),
    requireActiveMember(roomId, currentUser.userId),
    getRoomOrThrow(roomId),
  ]);
  if (!messageSnapshot.exists) {
    throw new HttpsError("not-found", "삭제할 메시지를 찾을 수 없습니다.");
  }

  const messageData = messageSnapshot.data() as Record<string, unknown>;
  const isMine = trimmed(messageData.senderId) === currentUser.userId;
  if (!isMine && !["owner", "operator"].includes(currentMember.roleCd)) {
    throw new HttpsError("permission-denied", "메시지를 삭제할 권한이 없습니다.");
  }

  await messageSnapshot.ref.set({
    messageText: "삭제된 메시지입니다.",
    attachments: [],
    forwardPreview: "",
    deletedYn: "Y",
    updatedAt: now(),
  }, { merge: true });

  const roomPatch: Record<string, unknown> = { updatedAt: now() };
  if (trimmed(room.roomData.pinnedMsgContId) === msgContId) {
    roomPatch.pinnedMsgContId = "";
    roomPatch.pinnedMsgPreview = "";
  }
  if (toIso(room.roomData.lastMessageAt) === toIso(messageData.createdAt)) {
    roomPatch.lastMessage = "삭제된 메시지입니다.";
  }
  await roomRef(roomId).set(roomPatch, { merge: true });
  await syncUserRoomSummaries(roomId);

  return { roomId, msgContId, deleted: true };
});

export const messengerForwardMessage = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const data = asObject(request.data);
  const msgContId = trimmed(data.msgContId);
  const targetRoomId = trimmed(data.targetRoomId);
  if (!msgContId || !targetRoomId) {
    throw new HttpsError("invalid-argument", "msgContId and targetRoomId are required.");
  }

  await requireActiveMember(targetRoomId, currentUser.userId);
  const source = await findMessageForUser(currentUser.userId, msgContId);
  if (!source) {
    throw new HttpsError("not-found", "전달할 메시지를 찾을 수 없습니다.");
  }

  const preview = trimmed(source.messageData.messageText) || messagePreview("", sanitizeAttachments(source.messageData.attachments));
  return persistRoomMessage(targetRoomId, {
    userId: currentUser.userId,
    userNm: currentUser.userNm,
    deptNm: currentUser.deptNm,
    jbgdNm: currentUser.jbgdNm,
  }, {
    roomId: targetRoomId,
    messageText: trimmed(source.messageData.messageText),
    msgType: trimmed(source.messageData.msgType),
    attachments: sanitizeAttachments(source.messageData.attachments),
    forwardPreview: preview,
  });
});

export const messengerExportMessages = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const currentUser = await loadProfile(uid, request.auth?.token || {});
  const roomId = trimmed(asObject(request.data).roomId || asObject(request.data).msgrId);
  await requireActiveMember(roomId, currentUser.userId);

  const rows = await listMessagesData(roomId);
  const csv = [
    ["보낸시각", "작성자", "유형", "내용", "첨부파일"].join(","),
    ...rows.map(({ data }) => {
      const message = normalizeMessageResult("", data);
      const attachmentNames = sanitizeAttachments(message.attachments)
        .map((attachment) => trimmed(asObject(attachment).orgnFileNm || asObject(attachment).name))
        .filter(Boolean)
        .join(" | ");
      return [
        csvEscape(message.sendDt),
        csvEscape(message.userNm || message.userId),
        csvEscape(message.msgTypeCd),
        csvEscape(message.contents),
        csvEscape(attachmentNames),
      ].join(",");
    }),
  ].join("\n");

  return {
    fileName: `chat-${roomId}.csv`,
    mimeType: "text/csv;charset=utf-8",
    contentBase64: Buffer.from(csv, "utf8").toString("base64"),
  };
});
