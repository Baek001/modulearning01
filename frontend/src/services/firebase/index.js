export { firebaseAuthService } from './auth';
export {
    firebaseApprovalBridge,
    firebaseAttendanceBridge,
    firebaseBridge,
    firebaseCalendarBridge,
    firebaseCommonBridge,
    firebaseCommunityBridge,
    firebaseDashboardBridge,
    firebaseMeetingBridge,
    firebaseProjectBridge,
} from './bridge';
export { getFirebaseApp, getFirebaseAuth, getFirebaseDb, getFirebaseFunctions, getFirebaseStorage } from './client';
export { callFirebaseFunction, firebaseFunctionsAPI } from './functions';
export { firebaseMessengerAPI, subscribeRoomMessages, subscribeUserRooms } from './messenger';
export {
    createFirebaseConfig,
    firebaseFunctionRegion,
    firebaseLoginDomain,
    firebaseUserCollection,
    isFirebasePlatformEnabled,
    resolveFirebaseLoginEmail,
    firebaseEmulatorsEnabled,
} from './platform';
export { buildStoragePath, getStorageDownloadUrl, uploadStorageFile } from './storage';
