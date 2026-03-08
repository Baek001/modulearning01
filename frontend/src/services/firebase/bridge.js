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

async function uploadBoardFiles(files = [], area = 'boards') {
    const safeFiles = (files || []).filter(Boolean);

    return Promise.all(
        safeFiles.map(async (file, index) => {
            const uploaded = await uploadStorageFile({
                domain: area,
                ownerId: ownerId(),
                pathPrefix: `${area}/${ownerId()}`,
                file,
                metadata: {
                    ownerId: ownerId(),
                    area,
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
    summary: async () => wrapData(await callFirebaseFunction('dashboardGetSummary')),
    bootstrap: async () => wrapData(await callFirebaseFunction('dashboardGetBootstrap')),
    feed: async (params = {}) => wrapData(await callFirebaseFunction('dashboardGetFeed', params)),
    widgets: async () => wrapData(await callFirebaseFunction('dashboardGetWidgets')),
    preferences: async () => wrapData(await callFirebaseFunction('dashboardGetPreferences')),
    savePreferences: async (data) => wrapData(await callFirebaseFunction('dashboardSavePreferences', data)),
    categories: async () => wrapData(await callFirebaseFunction('dashboardGetCategories')),
    saveCategories: async (categories = []) => wrapData(await callFirebaseFunction('dashboardSaveCategories', { categories })),
    markRead: async (pstId) => wrapData(await callFirebaseFunction('dashboardMarkBoardRead', { pstId })),
    savePost: async (pstId) => wrapData(await callFirebaseFunction('dashboardSavePost', { pstId })),
    unsavePost: async (pstId) => wrapData(await callFirebaseFunction('dashboardUnsavePost', { pstId })),
    favoriteUsers: async () => wrapData(await callFirebaseFunction('dashboardFavoriteUsers')),
    addFavoriteUser: async (targetUserId) => wrapData(await callFirebaseFunction('dashboardAddFavoriteUser', { targetUserId })),
    removeFavoriteUser: async (targetUserId) => wrapData(await callFirebaseFunction('dashboardRemoveFavoriteUser', { targetUserId })),
    todos: async () => wrapData(await callFirebaseFunction('dashboardGetTodos')),
    createTodo: async (data) => wrapData(await callFirebaseFunction('dashboardCreateTodo', data)),
    updateTodo: async (todoId, data) => wrapData(await callFirebaseFunction('dashboardUpdateTodo', { todoId, ...data })),
    deleteTodo: async (todoId) => wrapData(await callFirebaseFunction('dashboardDeleteTodo', { todoId })),
    recommendations: async (box = 'inbox') => wrapData(await callFirebaseFunction('dashboardGetRecommendations', { box })),
    createRecommendations: async (targetUserId, categoryCodes, message) => wrapData(await callFirebaseFunction('dashboardCreateRecommendations', { targetUserId, categoryCodes, message })),
    updateRecommendation: async (recommendId, data = {}) => wrapData(await callFirebaseFunction('dashboardUpdateRecommendation', { recommendId, ...data })),
    profile: async (userId) => wrapData(await callFirebaseFunction('dashboardGetProfile', { userId })),
};

export const firebaseAlarmBridge = {
    list: async () => wrapData(await callFirebaseFunction('alarmGetList')),
    top10: async () => wrapData(await callFirebaseFunction('alarmGetTop10')),
    detail: async (alarmId) => wrapData(await callFirebaseFunction('alarmGetDetail', { alarmId })),
    markAllRead: async () => wrapData(await callFirebaseFunction('alarmMarkAllRead')),
};

export const firebaseBoardBridge = {
    notices: async () => wrapData(await callFirebaseFunction('boardGetNotices')),
    community: async (bbsCtgrCd) => wrapData(await callFirebaseFunction('boardGetCommunity', { bbsCtgrCd })),
    categoryCounts: async () => wrapData(await callFirebaseFunction('boardGetCategoryCounts')),
    detail: async (pstId) => wrapData(await callFirebaseFunction('boardGetDetail', { pstId })),
    create: async (payload) => wrapData(await callFirebaseFunction('boardCreate', payload)),
    update: async (pstId, payload) => wrapData(await callFirebaseFunction('boardUpdate', { pstId, payload })),
    remove: async (pstId) => wrapData(await callFirebaseFunction('boardRemove', { pstId })),
    incrementView: async (pstId) => wrapData(await callFirebaseFunction('boardIncrementView', { pstId })),
    comments: async (pstId) => wrapData(await callFirebaseFunction('boardGetComments', { pstId })),
    createComment: async (pstId, payload) => wrapData(await callFirebaseFunction('boardCreateComment', { pstId, ...payload })),
    updateComment: async (pstId, cmntSqn, payload) => wrapData(await callFirebaseFunction('boardUpdateComment', { pstId, cmntSqn, ...payload })),
    deleteComment: async (pstId, cmntSqn) => wrapData(await callFirebaseFunction('boardDeleteComment', { pstId, cmntSqn })),
    workspace: async (params = {}) => wrapData(await callFirebaseFunction('boardGetWorkspace', params)),
    workspaceDetail: async (pstId) => wrapData(await callFirebaseFunction('boardGetWorkspaceDetail', { pstId })),
    createWorkspace: async (payload, files = []) => wrapData(await callFirebaseFunction('boardCreateWorkspace', {
        payload,
        attachments: await uploadBoardFiles(files, 'boards'),
    })),
    updateWorkspace: async (pstId, payload, files = []) => wrapData(await callFirebaseFunction('boardUpdateWorkspace', {
        pstId,
        payload,
        attachments: await uploadBoardFiles(files, 'boards'),
    })),
    toggleLikePost: async (pstId) => wrapData(await callFirebaseFunction('boardToggleLike', { pstId })),
    likeUsers: async (pstId) => wrapData(await callFirebaseFunction('boardGetLikeUsers', { pstId })),
    readers: async (pstId) => wrapData(await callFirebaseFunction('boardGetReaders', { pstId })),
    share: async (pstId, data) => wrapData(await callFirebaseFunction('boardSharePost', { pstId, ...data })),
    report: async (pstId, data) => wrapData(await callFirebaseFunction('boardReportPost', { pstId, ...data })),
    pin: async (pstId, fixedYn) => wrapData(await callFirebaseFunction('boardPinPost', { pstId, fixedYn })),
    votePoll: async (pstId, optionIds = []) => wrapData(await callFirebaseFunction('boardVotePoll', { pstId, optionIds })),
    updateTodoAssignee: async (pstId, assigneeUserId, statusCd) => wrapData(await callFirebaseFunction('boardUpdateTodoAssignee', { pstId, assigneeUserId, statusCd })),
    scheduleAvailability: async (data) => wrapData(await callFirebaseFunction('boardCheckScheduleAvailability', data)),
    createCommentMultipart: async (pstId, payload, files = [], upCmntSqn = '') => wrapData(await callFirebaseFunction('boardCreateComment', {
        pstId,
        ...payload,
        upCmntSqn,
        attachments: await uploadBoardFiles(files, 'board-comments'),
    })),
    updateCommentMultipart: async (pstId, cmntSqn, payload, files = []) => wrapData(await callFirebaseFunction('boardUpdateComment', {
        pstId,
        cmntSqn,
        ...payload,
        attachments: await uploadBoardFiles(files, 'board-comments'),
    })),
};

function blobFromBase64(contentBase64, mimeType = 'application/octet-stream') {
    const binary = atob(String(contentBase64 || ''));
    const bytes = new Uint8Array(binary.length);
    for (let index = 0; index < binary.length; index += 1) {
        bytes[index] = binary.charCodeAt(index);
    }
    return new Blob([bytes], { type: mimeType });
}

function toStorageFilePayload(uploaded) {
    if (!uploaded) {
        return null;
    }
    return {
        name: uploaded.name,
        path: uploaded.path,
        downloadURL: uploaded.downloadURL,
        size: uploaded.size,
        contentType: uploaded.contentType,
    };
}

async function uploadCommunityAssets(files = {}) {
    const entries = await Promise.all(
        Object.entries({
            iconFile: files.iconFile,
            coverFile: files.coverFile,
        }).map(async ([key, file]) => {
            if (!file) {
                return null;
            }

            const uploaded = await uploadStorageFile({
                domain: 'communities',
                ownerId: ownerId(),
                pathPrefix: `communities/${ownerId()}`,
                file,
                metadata: {
                    ownerId: ownerId(),
                    area: key,
                },
            });

            return [key, uploaded];
        })
    );

    const nextPayload = {};
    entries.filter(Boolean).forEach(([key, uploaded]) => {
        if (key === 'iconFile') {
            nextPayload.iconFilePath = uploaded.downloadURL;
            nextPayload.iconFileUrl = uploaded.downloadURL;
        }
        if (key === 'coverFile') {
            nextPayload.coverFilePath = uploaded.downloadURL;
            nextPayload.coverFileUrl = uploaded.downloadURL;
        }
    });
    return nextPayload;
}

async function uploadMessengerFiles(files = [], roomId = '') {
    const safeFiles = (files || []).filter(Boolean);

    return Promise.all(
        safeFiles.map(async (file, index) => {
            const uploaded = await uploadStorageFile({
                domain: 'messenger',
                ownerId: ownerId(),
                pathPrefix: `messenger/${roomId || ownerId()}`,
                file,
                metadata: {
                    ownerId: ownerId(),
                    area: 'messenger',
                    roomId: roomId || '',
                },
            });

            return {
                fileId: crypto.randomUUID(),
                fileSeq: index + 1,
                orgnFileNm: uploaded.name,
                saveFileNm: uploaded.path,
                filePath: uploaded.downloadURL,
                downloadURL: uploaded.downloadURL,
                fileSize: uploaded.size,
                contentType: uploaded.contentType,
            };
        })
    );
}

async function uploadContractFiles(files = {}) {
    const entries = await Promise.all(
        Object.entries(files || {}).map(async ([key, file]) => {
            if (!file) {
                return null;
            }

            const uploaded = await uploadStorageFile({
                domain: 'contracts',
                ownerId: ownerId(),
                pathPrefix: `contracts/${ownerId()}/${key}`,
                file,
                metadata: {
                    ownerId: ownerId(),
                    area: key,
                },
            });

            return [key, toStorageFilePayload(uploaded)];
        })
    );

    return Object.fromEntries(entries.filter(Boolean));
}

export const firebaseMessengerBridge = {
    currentUser: async () => wrapData(await callFirebaseFunction('messengerGetCurrentUser')),
    users: async () => wrapData(await callFirebaseFunction('messengerGetUsers')),
    panel: async () => wrapData(await callFirebaseFunction('messengerGetPanel')),
    rooms: async (params = {}) => wrapData(await callFirebaseFunction('messengerGetRooms', params)),
    roomDetail: async (roomId) => wrapData(await callFirebaseFunction('messengerGetRoomDetail', { roomId })),
    messages: async (roomId) => wrapData(await callFirebaseFunction('messengerGetMessages', { roomId })),
    searchMessages: async (roomId, q) => wrapData(await callFirebaseFunction('messengerSearchMessages', { roomId, q })),
    selfRoom: async () => wrapData(await callFirebaseFunction('createSelfRoom')),
    findOrCreate: async (userId) => wrapData(await callFirebaseFunction('messengerFindOrCreateRoom', { userId })),
    createRoom: async (payload) => wrapData(await callFirebaseFunction('messengerCreateRoom', payload)),
    send: async (roomId, messageText, options = {}) => wrapData(await callFirebaseFunction('sendMessengerMessage', {
        roomId,
        messageText,
        ...options,
    })),
    invite: async (roomId, userIds) => wrapData(await callFirebaseFunction('messengerInviteUsers', { roomId, userIds })),
    kick: async (roomId, userId) => wrapData(await callFirebaseFunction('messengerKickUser', { roomId, userId })),
    markAsRead: async (roomId) => wrapData(await callFirebaseFunction('markRoomRead', { roomId })),
    renameRoom: async (roomId, msgrNm) => wrapData(await callFirebaseFunction('messengerRenameRoom', { roomId, msgrNm })),
    participants: async (roomId) => wrapData(await callFirebaseFunction('messengerGetParticipants', { roomId })),
    notify: async (roomId, notifyEnabled) => wrapData(await callFirebaseFunction('messengerToggleNotify', { roomId, notifyEnabled })),
    pin: async (roomId, msgContId) => wrapData(await callFirebaseFunction('messengerPinMessage', { roomId, msgContId })),
    clearPin: async (roomId) => wrapData(await callFirebaseFunction('messengerClearPinMessage', { roomId })),
    leave: async (roomId) => wrapData(await callFirebaseFunction('messengerLeaveRoom', { roomId })),
    deleteMessage: async (msgContId, roomId) => wrapData(await callFirebaseFunction('messengerDeleteMessage', { msgContId, roomId })),
    forwardMessage: async (msgContId, targetRoomId) => wrapData(await callFirebaseFunction('messengerForwardMessage', { msgContId, targetRoomId })),
    uploadFiles: async (roomId, contents, files = []) => wrapData(await callFirebaseFunction('sendMessengerMessage', {
        roomId,
        messageText: contents,
        attachments: await uploadMessengerFiles(files, roomId),
    })),
    exportMessages: async (roomId) => {
        const response = await callFirebaseFunction('messengerExportMessages', { roomId });
        return wrapData(blobFromBase64(response.contentBase64, response.mimeType));
    },
};

export const firebaseCommunityBridge = {
    list: async (params = {}) => wrapData(await callFirebaseFunction('communityGetList', typeof params === 'string' ? (params ? { q: params } : {}) : params)),
    search: async (params = {}) => wrapData(await callFirebaseFunction('communitySearch', typeof params === 'string' ? (params ? { q: params } : {}) : params)),
    detail: async (communityId) => wrapData(await callFirebaseFunction('communityGetDetail', { communityId })),
    create: async (payload, files = {}) => wrapData(await callFirebaseFunction('communityCreate', {
        ...payload,
        ...(await uploadCommunityAssets(files)),
    })),
    update: async (communityId, payload, files = {}) => wrapData(await callFirebaseFunction('communityUpdate', {
        communityId,
        payload: {
            ...payload,
            ...(await uploadCommunityAssets(files)),
        },
    })),
    remove: async (communityId) => wrapData(await callFirebaseFunction('communityRemove', { communityId })),
    close: async (communityId) => wrapData(await callFirebaseFunction('communityClose', { communityId })),
    join: async (communityId) => wrapData(await callFirebaseFunction('communityJoin', { communityId })),
    leave: async (communityId) => wrapData(await callFirebaseFunction('communityLeave', { communityId })),
    members: async (communityId, status = '') => wrapData(await callFirebaseFunction('communityGetMembers', { communityId, status })),
    requests: async (communityId) => wrapData(await callFirebaseFunction('communityGetRequests', { communityId })),
    addMembers: async (communityId, userIds = []) => wrapData(await callFirebaseFunction('communityAddMembers', { communityId, userIds })),
    removeMember: async (communityId, userId) => wrapData(await callFirebaseFunction('communityRemoveMember', { communityId, userId })),
    updateRole: async (communityId, userId, roleCd) => wrapData(await callFirebaseFunction('communityUpdateRole', { communityId, userId, roleCd })),
    approveRequest: async (communityId, userId) => wrapData(await callFirebaseFunction('communityApproveRequest', { communityId, userId })),
    rejectRequest: async (communityId, userId) => wrapData(await callFirebaseFunction('communityRejectRequest', { communityId, userId })),
    favorite: async (communityId, favoriteYn) => wrapData(await callFirebaseFunction('communityFavorite', { communityId, favoriteYn })),
    saveOrder: async (communityIds = []) => wrapData(await callFirebaseFunction('communitySaveOrder', { communityIds })),
    syncOrg: async () => wrapData(await callFirebaseFunction('communitySyncOrg')),
};

export const firebaseEmailBridge = {
    counts: async () => wrapData(await callFirebaseFunction('emailGetCounts')),
    list: async (mailboxTypeCd, page = 1, searchWord = '') => wrapData(await callFirebaseFunction('emailGetList', { mailboxTypeCd, page, searchWord })),
    toggleImportance: async (emailContId) => wrapData(await callFirebaseFunction('emailToggleImportance', { emailContId })),
    deleteSelected: async (emailContIds = [], mailboxTypeCd) => wrapData(await callFirebaseFunction('emailDeleteSelected', { emailContIds, mailboxTypeCd })),
    deleteAll: async (mailboxTypeCd) => wrapData(await callFirebaseFunction('emailDeleteAll', { mailboxTypeCd })),
    restoreSelected: async (emailContIds = []) => wrapData(await callFirebaseFunction('emailRestoreSelected', { emailContIds })),
};

export const firebaseContractBridge = {
    dashboard: async () => wrapData(await callFirebaseFunction('contractGetDashboard')),
    list: async (params = {}) => wrapData(await callFirebaseFunction('contractGetList', params)),
    detail: async (contractId) => wrapData(await callFirebaseFunction('contractGetDetail', { contractId })),
    create: async (payload) => wrapData(await callFirebaseFunction('contractCreate', payload)),
    send: async (contractId) => wrapData(await callFirebaseFunction('contractSend', { contractId })),
    cancel: async (contractId, data = {}) => wrapData(await callFirebaseFunction('contractCancel', { contractId, ...data })),
    remind: async (contractId) => wrapData(await callFirebaseFunction('contractRemind', { contractId })),
    links: async (contractId) => wrapData(await callFirebaseFunction('contractGetLinks', { contractId })),
    createBatch: async (payload) => wrapData(await callFirebaseFunction('contractCreateBatch', payload)),
    batchDetail: async (batchId) => wrapData(await callFirebaseFunction('contractGetBatchDetail', { batchId })),
    templates: async () => wrapData(await callFirebaseFunction('contractGetTemplates')),
    templateDetail: async (templateId) => wrapData(await callFirebaseFunction('contractGetTemplateDetail', { templateId })),
    createTemplate: async (payload, files = {}) => wrapData(await callFirebaseFunction('contractCreateTemplate', {
        payload,
        ...(await uploadContractFiles(files)),
    })),
    updateTemplate: async (templateId, payload, files = {}) => wrapData(await callFirebaseFunction('contractUpdateTemplate', {
        templateId,
        payload,
        ...(await uploadContractFiles(files)),
    })),
    publishTemplate: async (templateId, templateVersionId) => wrapData(await callFirebaseFunction('contractPublishTemplate', { templateId, templateVersionId })),
    templateRequests: async () => wrapData(await callFirebaseFunction('contractGetTemplateRequests')),
    createTemplateRequest: async (payload, files = {}) => wrapData(await callFirebaseFunction('contractCreateTemplateRequest', {
        payload,
        ...(await uploadContractFiles(files)),
    })),
    approveTemplateRequest: async (requestId, data = {}) => wrapData(await callFirebaseFunction('contractApproveTemplateRequest', { requestId, ...data })),
    rejectTemplateRequest: async (requestId, data = {}) => wrapData(await callFirebaseFunction('contractRejectTemplateRequest', { requestId, ...data })),
    companySettings: async () => wrapData(await callFirebaseFunction('contractGetCompanySettings')),
    updateCompanySettings: async (payload, files = {}) => wrapData(await callFirebaseFunction('contractUpdateCompanySettings', {
        payload,
        ...(await uploadContractFiles(files)),
    })),
    publicDetail: async (token) => wrapData(await callFirebaseFunction('contractPublicDetail', { token })),
    publicClaim: async (token, payload) => wrapData(await callFirebaseFunction('contractPublicClaim', { token, ...payload })),
    publicSubmit: async (token, payload) => wrapData(await callFirebaseFunction('contractPublicSubmit', { token, ...payload })),
    publicDownload: async (token) => {
        const response = await callFirebaseFunction('contractPublicDownload', { token });
        return wrapData(blobFromBase64(response.contentBase64, response.mimeType));
    },
};
