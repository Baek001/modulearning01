import { callFirebaseFunction } from './functions';
import { isFirebasePlatformEnabled } from './platform';
import { uploadStorageFile } from './storage';

function wrapData(data) {
    return Promise.resolve({ data });
}

function currentStoredUser() {
    if (typeof window === 'undefined') {
        return {};
    }

    try {
        return JSON.parse(localStorage.getItem('starworks.user') || '{}');
    } catch {
        return {};
    }
}

function ownerId() {
    const currentUser = currentStoredUser();
    return currentUser.userId || currentUser.firebaseUid || 'anonymous';
}

async function uploadProfileImage(file) {
    if (!file) {
        return null;
    }

    return uploadStorageFile({
        domain: 'profiles',
        ownerId: ownerId(),
        pathPrefix: 'profiles',
        file,
        metadata: {
            ownerId: ownerId(),
            area: 'profile',
        },
    });
}

async function uploadApprovalFiles(files = []) {
    const safeFiles = (files || []).filter(Boolean);

    return Promise.all(
        safeFiles.map(async (file, index) => {
            const uploaded = await uploadStorageFile({
                domain: 'approval',
                ownerId: ownerId(),
                pathPrefix: `approval/${ownerId()}`,
                file,
                metadata: {
                    ownerId: ownerId(),
                    area: 'approval',
                },
            });

            return {
                fileId: crypto.randomUUID(),
                fileSeq: index + 1,
                orgnFileNm: uploaded.name,
                saveFileNm: uploaded.path,
                filePath: uploaded.downloadURL,
                fileSize: uploaded.size,
                contentType: uploaded.contentType,
            };
        })
    );
}

export const firebaseBridge = {
    enabled: () => isFirebasePlatformEnabled(),
    wrapData,
};

export const firebaseCommonBridge = {
    listUsers: async () => wrapData(await callFirebaseFunction('listUsers')),
    getUserDetail: async (userId) => wrapData(await callFirebaseFunction('getUserDetail', { userId })),
    getMyProfile: async () => wrapData(await callFirebaseFunction('getMyProfile')),
    searchUsers: async (term) => wrapData(await callFirebaseFunction('searchUsers', { term })),
    createUser: async (user) => wrapData(await callFirebaseFunction('upsertUserProfile', { user })),
    updateUser: async (userId, user) => wrapData(await callFirebaseFunction('upsertUserProfile', { userId, user })),
    retireUser: async (userId) => wrapData(await callFirebaseFunction('retireUser', { userId })),
    listDepartments: async () => wrapData(await callFirebaseFunction('listDepartments')),
    listCommonCodes: async (codeGrpId) => wrapData(await callFirebaseFunction('listCommonCodes', { codeGrpId })),
    updateProfile: async (data) => {
        const profileImage = await uploadProfileImage(data?.profileImage);

        return wrapData(await callFirebaseFunction('upsertProfile', {
            ...data,
            profileImage: undefined,
            profileImagePath: profileImage?.path || '',
            profileImageUrl: profileImage?.downloadURL || '',
        }));
    },
};

export const firebaseApprovalBridge = {
    templates: async () => wrapData(await callFirebaseFunction('approvalGetTemplates')),
    summary: async () => wrapData(await callFirebaseFunction('approvalGetSummary')),
    list: async (params = {}) => wrapData(await callFirebaseFunction('approvalGetList', params)),
    detail: async (atrzDocId) => wrapData(await callFirebaseFunction('approvalGetDetail', { atrzDocId })),
    create: async (payload, files = []) => wrapData(await callFirebaseFunction('approvalCreateDocument', {
        payload,
        attachments: await uploadApprovalFiles(files),
    })),
    approve: async (atrzDocId, data) => wrapData(await callFirebaseFunction('approvalApproveDocument', { atrzDocId, ...data })),
    reject: async (atrzDocId, data) => wrapData(await callFirebaseFunction('approvalRejectDocument', { atrzDocId, ...data })),
    retract: async (atrzDocId) => wrapData(await callFirebaseFunction('approvalRetractDocument', { atrzDocId })),
    customLines: async () => wrapData(await callFirebaseFunction('approvalGetCustomLines')),
    createCustomLine: async (lines) => wrapData(await callFirebaseFunction('approvalSaveCustomLine', { lines })),
    deleteCustomLine: async (name) => wrapData(await callFirebaseFunction('approvalDeleteCustomLine', { name })),
    vacationBalance: async () => wrapData(await callFirebaseFunction('approvalGetVacationBalance')),
    tempList: async () => wrapData(await callFirebaseFunction('approvalGetTempList')),
    tempDetail: async (atrzTempSqn) => wrapData(await callFirebaseFunction('approvalGetTempDetail', { atrzTempSqn })),
    saveTemp: async (payload, files = []) => wrapData(await callFirebaseFunction('approvalSaveTemp', {
        payload,
        attachments: await uploadApprovalFiles(files),
    })),
    updateTemp: async (atrzTempSqn, payload, files = []) => wrapData(await callFirebaseFunction('approvalUpdateTemp', {
        atrzTempSqn,
        payload,
        attachments: await uploadApprovalFiles(files),
    })),
    deleteTemp: async (atrzTempSqn) => wrapData(await callFirebaseFunction('approvalDeleteTemp', { atrzTempSqn })),
};

export const firebaseProjectBridge = {
    list: async () => wrapData(await callFirebaseFunction('projectGetList')),
    detail: async (bizId) => wrapData(await callFirebaseFunction('projectGetDetail', { bizId })),
    create: async (payload) => wrapData(await callFirebaseFunction('projectCreate', payload)),
    update: async (bizId, payload) => wrapData(await callFirebaseFunction('projectUpdate', { bizId, payload })),
    setStatus: async (bizId, bizSttsCd) => wrapData(await callFirebaseFunction('projectSetStatus', { bizId, bizSttsCd })),
    tasks: async (bizId) => wrapData(await callFirebaseFunction('projectGetTasks', { bizId })),
    createTask: async (bizId, payload) => wrapData(await callFirebaseFunction('projectCreateTask', { bizId, payload })),
    updateTask: async (taskId, payload) => wrapData(await callFirebaseFunction('projectUpdateTask', { taskId, payload })),
    setTaskStatus: async (taskId, taskSttsCd) => wrapData(await callFirebaseFunction('projectSetTaskStatus', { taskId, taskSttsCd })),
    deleteTask: async (taskId) => wrapData(await callFirebaseFunction('projectDeleteTask', { taskId })),
};

export const firebaseAttendanceBridge = {
    today: async (userId, workYmd) => wrapData(await callFirebaseFunction('attendanceToday', { userId, workYmd })),
    history: async (userId) => wrapData(await callFirebaseFunction('attendanceHistory', { userId })),
    clockIn: async () => wrapData(await callFirebaseFunction('attendanceClockIn')),
    clockOut: async (workYmd) => wrapData(await callFirebaseFunction('attendanceClockOut', { workYmd })),
    week: async () => wrapData(await callFirebaseFunction('attendanceWeek')),
    month: async () => wrapData(await callFirebaseFunction('attendanceMonth')),
    monthList: async () => wrapData(await callFirebaseFunction('attendanceMonthList')),
    depart: async () => wrapData(await callFirebaseFunction('attendanceDepart')),
};

export const firebaseMeetingBridge = {
    rooms: async () => wrapData(await callFirebaseFunction('meetingGetRooms')),
    createRoom: async (payload) => wrapData(await callFirebaseFunction('meetingCreateRoom', payload)),
    reservations: async (params = {}) => wrapData(await callFirebaseFunction('meetingGetReservations', params)),
    detail: async (reservationId) => wrapData(await callFirebaseFunction('meetingGetDetail', { reservationId })),
    createReservation: async (payload) => wrapData(await callFirebaseFunction('meetingCreateReservation', payload)),
    updateReservation: async (payload) => wrapData(await callFirebaseFunction('meetingUpdateReservation', payload)),
    deleteReservation: async (reservationId) => wrapData(await callFirebaseFunction('meetingDeleteReservation', { reservationId })),
};

export const firebaseCalendarBridge = {
    events: async (params = {}) => wrapData(await callFirebaseFunction('calendarGetEvents', params)),
    createUser: async (payload) => wrapData(await callFirebaseFunction('calendarCreateUser', payload)),
    updateUser: async (eventKey, payload) => wrapData(await callFirebaseFunction('calendarUpdateUser', { eventKey, payload })),
    deleteUser: async (eventKey) => wrapData(await callFirebaseFunction('calendarDeleteUser', { eventKey })),
    createDept: async (payload) => wrapData(await callFirebaseFunction('calendarCreateDept', payload)),
    updateDept: async (eventKey, payload) => wrapData(await callFirebaseFunction('calendarUpdateDept', { eventKey, payload })),
    deleteDept: async (eventKey) => wrapData(await callFirebaseFunction('calendarDeleteDept', { eventKey })),
    teamProjects: async () => wrapData(await callFirebaseFunction('calendarTeamProjects')),
};

export const firebaseDashboardBridge = {
    favoriteUsers: async () => wrapData(await callFirebaseFunction('dashboardFavoriteUsers')),
    addFavoriteUser: async (targetUserId) => wrapData(await callFirebaseFunction('dashboardAddFavoriteUser', { targetUserId })),
    removeFavoriteUser: async (targetUserId) => wrapData(await callFirebaseFunction('dashboardRemoveFavoriteUser', { targetUserId })),
};

export const firebaseCommunityBridge = {
    list: async (params = {}) => wrapData(await callFirebaseFunction('communityGetList', typeof params === 'string' ? (params ? { q: params } : {}) : params)),
};
