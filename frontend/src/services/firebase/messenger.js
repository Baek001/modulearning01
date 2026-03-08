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
    return {
        msgrId: roomId,
        msgrNm: data.roomNm || data.msgrNm || '',
        msgrTypeCd: data.roomType || data.msgrTypeCd || 'group',
        unreadCnt: Number(data.unreadCount || data.unreadCnt || 0),
        notifyEnabled: data.notifyEnabled !== false,
        lastMsgCont: data.lastMessage || data.lastMsgCont || '',
        lastMsgAt: toIsoString(data.lastMessageAt || data.lastMsgAt),
    };
}

function normalizeMessageSnapshot(messageId, data = {}) {
    return {
        msgContId: messageId,
        senderId: data.senderId || '',
        senderNm: data.senderName || data.senderNm || '',
        msgCont: data.messageText || data.msgCont || '',
        msgTypeCd: data.msgType || data.msgTypeCd || 'text',
        files: Array.isArray(data.attachments) ? data.attachments : [],
        createdDt: toIsoString(data.createdAt || data.createdDt),
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
