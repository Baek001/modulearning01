import { FieldValue } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
  COLLECTIONS,
  CallableAuth,
  RoomMember,
  asObject,
  assertSignedIn,
  db,
  trimmed,
} from "./shared.js";
import { loadProfile } from "./profile.js";

function sanitizeMessengerPayload(data: unknown) {
  const source = asObject(data);
  return {
    roomId: trimmed(source.roomId),
    messageText: trimmed(source.messageText),
    msgType: trimmed(source.msgType) || "text",
    attachments: Array.isArray(source.attachments)
      ? source.attachments.filter((item) => item && typeof item === "object") as Array<Record<string, unknown>>
      : [],
  };
}

async function upsertUserRoom(userId: string, roomId: string, data: Record<string, unknown>) {
  await db.doc(`${COLLECTIONS.sessionUsers}/${userId}/rooms/${roomId}`).set(data, { merge: true });
}

export const createSelfRoom = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const profile = await loadProfile(uid, request.auth?.token || {});
  const roomId = `self_${uid}`;
  const roomRef = db.doc(`${COLLECTIONS.messengerRooms}/${roomId}`);
  const memberRef = roomRef.collection("members").doc(uid);

  await db.runTransaction(async (transaction) => {
    const roomSnapshot = await transaction.get(roomRef);

    if (!roomSnapshot.exists) {
      transaction.set(roomRef, {
        roomNm: `${profile.userNm || profile.userId} (me)`,
        roomType: "self",
        ownerId: uid,
        participantCount: 1,
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
        memberIds: [uid],
        lastMessage: "",
        lastMessageAt: null,
      });
    } else {
      transaction.set(roomRef, {
        updatedAt: FieldValue.serverTimestamp(),
        participantCount: 1,
        memberIds: [uid],
      }, { merge: true });
    }

    transaction.set(memberRef, {
      userId: uid,
      displayName: profile.userNm,
      role: "owner",
      joinedAt: FieldValue.serverTimestamp(),
      lastReadAt: FieldValue.serverTimestamp(),
      unreadCount: 0,
      notifyEnabled: true,
    }, { merge: true });
  });

  await upsertUserRoom(uid, roomId, {
    roomId,
    roomNm: `${profile.userNm || profile.userId} (me)`,
    roomType: "self",
    unreadCount: 0,
    notifyEnabled: true,
    updatedAt: FieldValue.serverTimestamp(),
  });

  return {
    msgrId: roomId,
    msgrNm: `${profile.userNm || profile.userId} (me)`,
    msgrTypeCd: "self",
  };
});

export const sendMessengerMessage = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const payload = sanitizeMessengerPayload(request.data);

  if (!payload.roomId) {
    throw new HttpsError("invalid-argument", "roomId is required.");
  }
  if (!payload.messageText && payload.attachments.length === 0) {
    throw new HttpsError("invalid-argument", "메시지 내용 또는 첨부파일이 필요합니다.");
  }

  const profile = await loadProfile(uid, request.auth?.token || {});
  const roomRef = db.doc(`${COLLECTIONS.messengerRooms}/${payload.roomId}`);
  const roomSnapshot = await roomRef.get();

  if (!roomSnapshot.exists) {
    throw new HttpsError("not-found", "대화방을 찾을 수 없습니다.");
  }

  const membersSnapshot = await roomRef.collection("members").get();
  const members: RoomMember[] = membersSnapshot.docs.map((entry) => ({
    id: entry.id,
    ...(entry.data() as Record<string, unknown>),
  }));

  if (!members.some((member) => member.id === uid)) {
    throw new HttpsError("permission-denied", "대화방 참여자가 아닙니다.");
  }

  const messageRef = roomRef.collection("messages").doc();
  const roomData = roomSnapshot.data() || {};
  const roomName = trimmed(roomData.roomNm) || trimmed(roomData.msgrNm);
  const roomType = trimmed(roomData.roomType) || "group";
  const lastMessage = payload.messageText || `파일 ${payload.attachments.length}개`;
  const batch = db.batch();

  batch.set(messageRef, {
    senderId: uid,
    senderName: profile.userNm,
    messageText: payload.messageText,
    msgType: payload.msgType,
    attachments: payload.attachments,
    createdAt: FieldValue.serverTimestamp(),
    deletedYn: "N",
  });

  batch.set(roomRef, {
    lastMessage,
    lastMessageAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });

  members.forEach((member) => {
    const memberId = String(member.id || "");
    const nextUnreadCount = memberId === uid ? 0 : Number(member.unreadCount || 0) + 1;

    batch.set(roomRef.collection("members").doc(memberId), {
      unreadCount: nextUnreadCount,
      lastReadAt: memberId === uid ? FieldValue.serverTimestamp() : member.lastReadAt || null,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    batch.set(db.doc(`${COLLECTIONS.sessionUsers}/${memberId}/rooms/${payload.roomId}`), {
      roomId: payload.roomId,
      roomNm: roomName,
      roomType,
      lastMessage,
      lastMessageAt: FieldValue.serverTimestamp(),
      unreadCount: nextUnreadCount,
      notifyEnabled: member.notifyEnabled !== false,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });

  await batch.commit();

  return {
    roomId: payload.roomId,
    messageId: messageRef.id,
    lastMessage,
  };
});

export const markRoomRead = onCall(async (request) => {
  const uid = assertSignedIn(request.auth as CallableAuth);
  const roomId = trimmed(asObject(request.data).roomId);
  if (!roomId) {
    throw new HttpsError("invalid-argument", "roomId is required.");
  }

  const roomRef = db.doc(`${COLLECTIONS.messengerRooms}/${roomId}`);
  const memberRef = roomRef.collection("members").doc(uid);
  const memberSnapshot = await memberRef.get();
  if (!memberSnapshot.exists) {
    throw new HttpsError("permission-denied", "대화방 참여자가 아닙니다.");
  }

  const batch = db.batch();
  batch.set(memberRef, {
    unreadCount: 0,
    lastReadAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });
  batch.set(db.doc(`${COLLECTIONS.sessionUsers}/${uid}/rooms/${roomId}`), {
    unreadCount: 0,
    lastReadAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });
  await batch.commit();

  return {
    roomId,
    unreadCount: 0,
  };
});
