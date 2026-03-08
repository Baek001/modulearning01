import { collection, limit, onSnapshot, orderBy, query } from 'firebase/firestore';
import { getFirebaseDb } from './client';
import { firebaseFunctionsAPI } from './functions';

function toIsoString(value) {
    if (!value) {
        return '';
    }

    if (typeof value?.toDate === 'function') {
        return value.toDate().toISOString();
    }

    if (value instanceof Date) {
        return value.toISOString();
    }

    return String(value);
}

function normalizeRoomSnapshot(roomId, data = {}) {
    const roomTypeCd = data.roomType || data.roomTypeCd || data.msgrTypeCd || 'group';
    return {
        msgrId: roomId,
        msgrNm: data.roomNm || data.msgrNm || '',
        roomTypeCd,
        msgrTypeCd: roomTypeCd,
        unreadCount: Number(data.unreadCount || data.unreadCnt || 0),
        unreadCnt: Number(data.unreadCount || data.unreadCnt || 0),
        notifyEnabled: data.notifyEnabled !== false,
        lastMsgCont: data.lastMessage || data.lastMsgCont || '',
        lastMsgDt: toIsoString(data.lastMessageAt || data.lastMsgDt || data.lastMsgAt),
        crtDt: toIsoString(data.createdAt || data.crtDt || data.lastMessageAt || data.lastMsgAt),
    };
}

function normalizeMessageSnapshot(messageId, data = {}) {
    return {
        msgContId: messageId,
        userId: data.senderId || data.userId || '',
        userNm: data.senderName || data.userNm || data.senderNm || '',
        deptNm: data.deptNm || '',
        jbgdNm: data.jbgdNm || '',
        contents: data.messageText || data.contents || data.msgCont || '',
        msgTypeCd: data.msgType || data.msgTypeCd || 'text',
        attachments: Array.isArray(data.attachments) ? data.attachments : [],
        sendDt: toIsoString(data.createdAt || data.sendDt || data.createdDt),
        unreadCount: Number(data.unreadCount || 0),
        forwardPreview: data.forwardPreview || '',
    };
}

export function subscribeUserRooms(userId, onNext, onError) {
    const roomsQuery = query(
        collection(getFirebaseDb(), 'users', userId, 'rooms'),
        orderBy('lastMessageAt', 'desc'),
        limit(50)
    );

    return onSnapshot(
        roomsQuery,
        (snapshot) => onNext(snapshot.docs.map((entry) => normalizeRoomSnapshot(entry.id, entry.data()))),
        onError
    );
}

export function subscribeRoomMessages(roomId, onNext, onError) {
    const messagesQuery = query(
        collection(getFirebaseDb(), 'messengerRooms', roomId, 'messages'),
        orderBy('createdAt', 'asc'),
        limit(200)
    );

    return onSnapshot(
        messagesQuery,
        (snapshot) => onNext(snapshot.docs.map((entry) => normalizeMessageSnapshot(entry.id, entry.data()))),
        onError
    );
}

export const firebaseMessengerAPI = {
    subscribeUserRooms,
    subscribeRoomMessages,
    createSelfRoom: () => firebaseFunctionsAPI.createSelfRoom(),
    sendMessage: (roomId, messageText, options = {}) => firebaseFunctionsAPI.sendMessengerMessage({
        roomId,
        messageText,
        ...options,
    }),
    markRoomRead: (roomId) => firebaseFunctionsAPI.markRoomRead(roomId),
};
