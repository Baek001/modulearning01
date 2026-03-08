import { httpsCallable } from 'firebase/functions';
import { getFirebaseFunctions } from './client';

export async function callFirebaseFunction(name, data = {}) {
    const callable = httpsCallable(getFirebaseFunctions(), name);
    const response = await callable(data);
    return response.data;
}

export const firebaseFunctionsAPI = {
    sessionProfile: () => callFirebaseFunction('sessionProfile'),
    upsertProfile: (payload) => callFirebaseFunction('upsertProfile', payload),
    createSelfRoom: () => callFirebaseFunction('createSelfRoom'),
    sendMessengerMessage: (payload) => callFirebaseFunction('sendMessengerMessage', payload),
    markRoomRead: (roomId) => callFirebaseFunction('markRoomRead', { roomId }),
};
