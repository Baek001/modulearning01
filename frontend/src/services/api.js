import axios from 'axios';

export const STORAGE_KEYS = {
    user: 'starworks.user',
};

const api = axios.create({
    timeout: 15000,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
});

function clearStoredAuth() {
    localStorage.removeItem(STORAGE_KEYS.user);
}

function isApiRequest(url = '') {
    return ['/common/', '/rest/', '/chat/', '/mail/'].some((prefix) => url.includes(prefix));
}

function isAuthRequest(url = '') {
    return url.includes('/common/auth');
}

function isLoginRedirectResponse(response) {
    const requestUrl = response?.request?.responseURL || '';
    const contentType = String(response?.headers?.['content-type'] || '');

    return isApiRequest(response?.config?.url || '')
        && (
            requestUrl.includes('/login')
            || contentType.includes('text/html')
        );
}

function toFormData(data) {
    const formData = new FormData();

    Object.entries(data ?? {}).forEach(([key, value]) => {
        if (value === undefined || value === null || value === '') {
            return;
        }
        formData.append(key, value);
    });

    return formData;
}

function toMultipartPayload(payload, files = []) {
    const formData = new FormData();
    formData.append('payload', JSON.stringify(payload ?? {}));

    (files || []).forEach((file) => {
        if (file) {
            formData.append('files', file);
        }
    });

    return formData;
}

function toNamedMultipartPayload(payload, fileMap = {}) {
    const formData = new FormData();
    formData.append('payload', JSON.stringify(payload ?? {}));

    Object.entries(fileMap ?? {}).forEach(([key, value]) => {
        if (value) {
            formData.append(key, value);
        }
    });

    return formData;
}

api.interceptors.response.use(
    (response) => {
        if (typeof window !== 'undefined' && isLoginRedirectResponse(response)) {
            clearStoredAuth();
            if (window.location.pathname !== '/login') {
                window.location.assign('/login');
            }

            return Promise.reject(new Error('AUTH_REDIRECT'));
        }

        return response;
    },
    (error) => {
        if (error.response?.status === 401 && !isAuthRequest(error.config?.url)) {
            clearStoredAuth();
            if (window.location.pathname !== '/login') {
                window.location.assign('/login');
            }
        }

        return Promise.reject(error);
    }
);

export const authAPI = {
    login: (username, password) => api.post('/common/auth', { username, password }),
    logout: () => api.post('/common/auth/revoke'),
    session: () => api.get('/rest/mypage'),
};

export const usersAPI = {
    list: () => api.get('/rest/comm-user'),
    detail: (userId) => api.get(`/rest/comm-user/${userId}`),
    me: () => api.get('/rest/comm-user/me'),
    create: (data) => api.post('/rest/comm-user', data),
    modify: (userId, data) => api.put(`/rest/comm-user/${userId}`, data),
    retire: (userId) => api.patch(`/rest/comm-user/${userId}/retire`),
    search: (term) => api.get('/rest/comm-user/search', { params: { term } }),
};

export const departmentAPI = {
    list: () => api.get('/rest/comm-depart'),
};

export const commonCodeAPI = {
    list: (codeGrpId) => api.get('/rest/comm-code', { params: { codeGrpId } }),
};

export const dashboardAPI = {
    summary: () => api.get('/rest/dashboard'),
    bootstrap: () => api.get('/rest/dashboard/bootstrap'),
    feed: (params = {}) => api.get('/rest/dashboard/feed', { params }),
    widgets: () => api.get('/rest/dashboard/widgets'),
    preferences: () => api.get('/rest/dashboard/preferences'),
    savePreferences: (data) => api.put('/rest/dashboard/preferences', data),
    categories: () => api.get('/rest/dashboard/categories'),
    saveCategories: (categories) => api.put('/rest/dashboard/categories', { categories }),
    markRead: (pstId) => api.post(`/rest/dashboard/board-read/${pstId}`),
    savePost: (pstId) => api.post(`/rest/dashboard/saved-posts/${pstId}`),
    unsavePost: (pstId) => api.delete(`/rest/dashboard/saved-posts/${pstId}`),
    favoriteUsers: () => api.get('/rest/dashboard/favorite-users'),
    addFavoriteUser: (targetUserId) => api.post(`/rest/dashboard/favorite-users/${targetUserId}`),
    removeFavoriteUser: (targetUserId) => api.delete(`/rest/dashboard/favorite-users/${targetUserId}`),
    todos: () => api.get('/rest/dashboard/todos'),
    createTodo: (data) => api.post('/rest/dashboard/todos', data),
    updateTodo: (todoId, data) => api.patch(`/rest/dashboard/todos/${todoId}`, data),
    deleteTodo: (todoId) => api.delete(`/rest/dashboard/todos/${todoId}`),
    recommendations: (box = 'inbox') => api.get('/rest/dashboard/category-recommendations', { params: { box } }),
    createRecommendations: (targetUserId, categoryCodes, message) => api.post('/rest/dashboard/category-recommendations', { targetUserId, categoryCodes, message }),
    updateRecommendation: (recommendId, data) => api.patch(`/rest/dashboard/category-recommendations/${recommendId}`, data),
    profile: (userId) => api.get(`/rest/dashboard/profile/${userId}`),
};

export const alarmAPI = {
    list: () => api.get('/rest/alarm-log-list'),
    top10: () => api.get('/rest/alarm-log-top10'),
    detail: (alarmId) => api.get(`/rest/alarm-log/${alarmId}`),
    markAllRead: () => api.put('/rest/alarm-log-list'),
};

export const approvalAPI = {
    templates: () => api.get('/rest/approval-template'),
    templateDetail: (atrzDocTmplId) => api.get(`/rest/approval-template/${atrzDocTmplId}`),
    summary: () => api.get('/rest/approval-documents/summary'),
    list: (params = {}) => api.get('/rest/approval-documents', { params }),
    detail: (atrzDocId) => api.get(`/rest/approval-documents/${atrzDocId}`),
    create: (payload, files = []) => api.post('/rest/approval-documents', toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    }),
    approve: (atrzDocId, data) => api.post(`/rest/approval-documents/${atrzDocId}/approve`, data),
    reject: (atrzDocId, data) => api.post(`/rest/approval-documents/${atrzDocId}/reject`, data),
    retract: (atrzDocId) => api.post(`/rest/approval-documents/${atrzDocId}/retract`),
    customLines: () => api.get('/rest/approval-customline'),
    createCustomLine: (data) => api.post('/rest/approval-customline', data),
    deleteCustomLine: (name) => api.delete(`/rest/approval-customline/${encodeURIComponent(name)}`),
    vacationBalance: () => api.get('/rest/approval-vacation/E101'),
    tempList: () => api.get('/rest/approval-temp'),
    tempDetail: (atrzTempSqn) => api.get(`/rest/approval-temp/${atrzTempSqn}`),
    saveTemp: (payload, files = []) => api.post('/rest/approval-temp', toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    }),
    updateTemp: (atrzTempSqn, payload, files = []) => api.put(`/rest/approval-temp/${atrzTempSqn}`, toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    }),
    deleteTemp: (atrzTempSqn) => api.delete(`/rest/approval-temp/${atrzTempSqn}`),
};

export const contractAPI = {
    dashboard: () => api.get('/rest/contracts/dashboard'),
    list: (params = {}) => api.get('/rest/contracts', { params }),
    detail: (contractId) => api.get(`/rest/contracts/${contractId}`),
    create: (payload) => api.post('/rest/contracts', payload),
    send: (contractId) => api.post(`/rest/contracts/${contractId}/send`),
    cancel: (contractId, data = {}) => api.post(`/rest/contracts/${contractId}/cancel`, data),
    remind: (contractId) => api.post(`/rest/contracts/${contractId}/remind`),
    links: (contractId) => api.get(`/rest/contracts/${contractId}/links`),
    createBatch: (payload) => api.post('/rest/contracts/batches', payload),
    batchDetail: (batchId) => api.get(`/rest/contracts/batches/${batchId}`),
    templates: () => api.get('/rest/contracts/templates'),
    templateDetail: (templateId) => api.get(`/rest/contracts/templates/${templateId}`),
    createTemplate: (payload, files = {}) => {
        if (files.sourceFile || files.backgroundFile) {
            return api.post('/rest/contracts/templates', toNamedMultipartPayload(payload, files), {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
        }
        return api.post('/rest/contracts/templates', payload);
    },
    updateTemplate: (templateId, payload, files = {}) => {
        if (files.sourceFile || files.backgroundFile) {
            return api.put(`/rest/contracts/templates/${templateId}`, toNamedMultipartPayload(payload, files), {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
        }
        return api.put(`/rest/contracts/templates/${templateId}`, payload);
    },
    publishTemplate: (templateId, templateVersionId) => api.post(`/rest/contracts/templates/${templateId}/publish`, templateVersionId ? { templateVersionId } : {}),
    templateRequests: () => api.get('/rest/contracts/template-requests'),
    createTemplateRequest: (payload, files = {}) => {
        if (files.sourceFile || files.markedFile || files.sealFile) {
            return api.post('/rest/contracts/template-requests', toNamedMultipartPayload(payload, files), {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
        }
        return api.post('/rest/contracts/template-requests', payload);
    },
    approveTemplateRequest: (requestId, data = {}) => api.post(`/rest/contracts/template-requests/${requestId}/approve`, data),
    rejectTemplateRequest: (requestId, data = {}) => api.post(`/rest/contracts/template-requests/${requestId}/reject`, data),
    companySettings: () => api.get('/rest/contracts/company-settings'),
    updateCompanySettings: (payload, files = {}) => {
        if (files.sealFile) {
            return api.put('/rest/contracts/company-settings', toNamedMultipartPayload(payload, files), {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
        }
        return api.put('/rest/contracts/company-settings', payload);
    },
    publicDetail: (token) => api.get(`/rest/contracts/public/${token}`),
    publicClaim: (token, payload) => api.post(`/rest/contracts/public/${token}/claim`, payload),
    publicSubmit: (token, payload) => api.post(`/rest/contracts/public/${token}/submit`, payload),
    publicDownload: (token) => api.get(`/rest/contracts/public/${token}/download`, { responseType: 'blob' }),
};

export const boardAPI = {
    notices: () => api.get('/rest/board-notice'),
    community: (bbsCtgrCd) => api.get('/rest/board-community', { params: bbsCtgrCd ? { bbsCtgrCd } : {} }),
    categoryCounts: () => api.get('/rest/board-category-counts'),
    detail: (pstId) => api.get(`/rest/board/${pstId}`),
    create: (data) => api.post('/rest/board', data),
    update: (pstId, data) => api.put(`/rest/board/${pstId}`, data),
    remove: (pstId) => api.delete(`/rest/board/${pstId}`),
    incrementView: (pstId) => api.put(`/rest/board-vct/${pstId}`),
    comments: (pstId) => api.get(`/rest/board-comment/${pstId}`),
    createComment: (pstId, data) => api.post(`/rest/board-comment/${pstId}`, data),
    updateComment: (pstId, cmntSqn, data) => api.put(`/rest/board-comment/${pstId}`, data, { params: { cmntSqn } }),
    deleteComment: (pstId, cmntSqn) => api.delete(`/rest/board-comment/${pstId}`, { params: { cmntSqn } }),
    workspace: (params = {}) => api.get('/rest/boards', { params }),
    workspaceDetail: (pstId) => api.get(`/rest/boards/${pstId}`),
    createWorkspace: (payload, files = []) => api.post('/rest/boards', toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    }),
    updateWorkspace: (pstId, payload, files = []) => api.put(`/rest/boards/${pstId}`, toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    }),
    toggleLikePost: (pstId) => api.post(`/rest/boards/${pstId}/likes`),
    likeUsers: (pstId) => api.get(`/rest/boards/${pstId}/likes`),
    readers: (pstId) => api.get(`/rest/boards/${pstId}/readers`),
    share: (pstId, data) => api.post(`/rest/boards/${pstId}/share`, data),
    report: (pstId, data) => api.post(`/rest/boards/${pstId}/report`, data),
    pin: (pstId, fixedYn) => api.post(`/rest/boards/${pstId}/pin`, { fixedYn }),
    votePoll: (pstId, optionIds) => api.post(`/rest/boards/${pstId}/poll/vote`, { optionIds }),
    updateTodoAssignee: (pstId, assigneeUserId, statusCd) => api.patch(`/rest/boards/${pstId}/todo-assignees/${assigneeUserId}`, { statusCd }),
    scheduleAvailability: (data) => api.post('/rest/boards/schedule/availability', data),
    createCommentMultipart: (pstId, payload, files = [], upCmntSqn = '') => api.post(`/rest/board-comment/${pstId}`, toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
        params: upCmntSqn ? { upCmntSqn } : {},
    }),
    updateCommentMultipart: (pstId, cmntSqn, payload, files = []) => api.put(`/rest/board-comment/${pstId}`, toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
        params: { cmntSqn },
    }),
};

export const attendanceAPI = {
    today: (userId, workYmd = new Date().toISOString().slice(0, 10).replace(/-/g, '')) => api.get(`/rest/attendance/${userId}/${workYmd}`),
    history: (userId) => api.get(`/rest/attendance/${userId}`),
    clockIn: () => api.post('/rest/attendance'),
    clockOut: (workYmd) => api.put('/rest/attendance', { workYmd }),
    week: () => api.get('/rest/attendance-stats/week'),
    month: () => api.get('/rest/attendance-stats/month'),
    monthList: () => api.get('/rest/attendance-stats/month-list'),
    depart: () => api.get('/rest/attendance-stats/depart'),
};

export const calendarAPI = {
    events: (params = {}) => api.get('/rest/calendar/events', { params }),
    user: () => api.get('/rest/calendar-user'),
    userDetail: (userSchdId) => api.get(`/rest/calendar-user/${userSchdId}`),
    createUser: (data) => api.post('/rest/calendar-user', data),
    updateUser: (userSchdId, data) => api.put(`/rest/calendar-user/${userSchdId}`, data),
    deleteUser: (userSchdId) => api.delete(`/rest/calendar-user/${userSchdId}`),
    dept: () => api.get('/rest/calendar-depart'),
    deptDetail: (deptSchdId) => api.get(`/rest/calendar-depart/${deptSchdId}`),
    createDept: (data) => api.post('/rest/calendar-depart', data),
    updateDept: (deptSchdId, data) => api.put(`/rest/calendar-depart/${deptSchdId}`, data),
    deleteDept: (deptSchdId) => api.delete(`/rest/calendar-depart/${deptSchdId}`),
    teamProjects: () => api.get('/rest/fullcalendar-team/project-list'),
};

export const emailAPI = {
    counts: () => api.get('/mail/counts'),
    list: (mailboxTypeCd, page = 1, searchWord = '') => api.get(`/mail/listData/${mailboxTypeCd}`, {
        params: {
            page,
            ...(searchWord ? { searchWord } : {}),
        },
    }),
    toggleImportance: (emailContId) => api.post(`/mail/toggle-importance/${emailContId}`),
    deleteSelected: (emailContIds, mailboxTypeCd) => api.post('/mail/deleteSelected', { emailContIds, mailboxTypeCd }),
    deleteAll: (mailboxTypeCd) => api.post('/mail/deleteAll', { mailboxTypeCd }),
    restoreSelected: (emailContIds) => api.post('/mail/restoreSelected', { emailContIds }),
};

export const messengerAPI = {
    currentUser: () => api.get('/chat/current-user'),
    users: () => api.get('/chat/users'),
    panel: () => api.get('/chat/panel'),
    rooms: (params = {}) => api.get('/chat/rooms', { params }),
    roomDetail: (msgrId) => api.get(`/chat/room/${msgrId}`),
    messages: (msgrId) => api.get(`/chat/room/${msgrId}/messages`),
    searchMessages: (msgrId, q) => api.get(`/chat/room/${msgrId}/search`, { params: { q } }),
    findOrCreate: (userId) => api.get('/chat/room/findOrCreate', { params: { userId } }),
    selfRoom: () => api.post('/chat/room/self'),
    createRoom: (data) => api.post('/chat/room/create', data),
    invite: (msgrId, userIds) => api.post(`/chat/room/${msgrId}/invite`, { userIds }),
    kick: (msgrId, userId) => api.post(`/chat/room/${msgrId}/kick`, { userId }),
    markAsRead: (msgrId) => api.post(`/chat/room/markAsRead/${msgrId}`),
    renameRoom: (msgrId, msgrNm) => api.post(`/chat/room/${msgrId}/name`, { msgrNm }),
    participants: (msgrId) => api.get(`/chat/room/${msgrId}/participants`),
    notify: (msgrId, notifyEnabled) => api.patch(`/chat/room/${msgrId}/notify`, { notifyEnabled }),
    pin: (msgrId, msgContId) => api.patch(`/chat/room/${msgrId}/pin`, { msgContId }),
    clearPin: (msgrId) => api.patch(`/chat/room/${msgrId}/pin`, {}),
    leave: (msgrId) => api.post(`/chat/room/${msgrId}/leave`),
    deleteMessage: (msgContId, msgrId) => api.delete(`/chat/message/${msgContId}`, { params: { msgrId } }),
    forwardMessage: (msgContId, targetRoomId) => api.post(`/chat/message/${msgContId}/forward`, { targetRoomId }),
    uploadFiles: (msgrId, contents, files = []) => {
        const formData = new FormData();
        if (contents) {
            formData.append('contents', contents);
        }
        files.forEach((file) => {
            if (file) {
                formData.append('files', file);
            }
        });
        return api.post(`/chat/room/${msgrId}/files`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
    },
    exportMessages: (msgrId) => api.get(`/chat/room/${msgrId}/export.xlsx`, { responseType: 'blob' }),
};

export const communityAPI = {
    list: (params = {}) => api.get('/rest/communities', { params: typeof params === 'string' ? (params ? { q: params } : {}) : params }),
    search: (params = {}) => api.get('/rest/communities/search', { params: typeof params === 'string' ? (params ? { q: params } : {}) : params }),
    detail: (communityId) => api.get(`/rest/communities/${communityId}`),
    create: (payload, files = {}) => {
        if (files.iconFile || files.coverFile) {
            return api.post('/rest/communities', toNamedMultipartPayload(payload, files), {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            });
        }
        return api.post('/rest/communities', payload);
    },
    update: (communityId, payload, files = {}) => {
        if (files.iconFile || files.coverFile) {
            return api.patch(`/rest/communities/${communityId}`, toNamedMultipartPayload(payload, files), {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            });
        }
        return api.patch(`/rest/communities/${communityId}`, payload);
    },
    remove: (communityId) => api.delete(`/rest/communities/${communityId}`),
    close: (communityId) => api.post(`/rest/communities/${communityId}/close`),
    join: (communityId) => api.post(`/rest/communities/${communityId}/join`),
    leave: (communityId) => api.post(`/rest/communities/${communityId}/leave`),
    members: (communityId, status = '') => api.get(`/rest/communities/${communityId}/members`, { params: status ? { status } : {} }),
    requests: (communityId) => api.get(`/rest/communities/${communityId}/requests`),
    addMembers: (communityId, userIds) => api.post(`/rest/communities/${communityId}/members`, { userIds }),
    removeMember: (communityId, userId) => api.delete(`/rest/communities/${communityId}/members/${userId}`),
    updateRole: (communityId, userId, roleCd) => api.patch(`/rest/communities/${communityId}/members/${userId}/role`, { roleCd }),
    approveRequest: (communityId, userId) => api.post(`/rest/communities/${communityId}/requests/${userId}/approve`),
    rejectRequest: (communityId, userId) => api.post(`/rest/communities/${communityId}/requests/${userId}/reject`),
    favorite: (communityId, favoriteYn) => api.put(`/rest/communities/${communityId}/favorite`, { favoriteYn }),
    saveOrder: (communityIds) => api.put('/rest/communities/order', { communityIds }),
    syncOrg: () => api.post('/rest/communities/sync-org'),
};

export const projectAPI = {
    list: () => api.get('/rest/project'),
    detail: (bizId) => api.get(`/rest/project/${bizId}`),
    create: (data) => api.post('/rest/project', data),
    update: (bizId, data) => api.put(`/rest/project/${bizId}`, data),
    setStatus: (bizId, bizSttsCd) => api.patch(`/rest/project/${bizId}/status`, { bizSttsCd }),
    members: (bizId) => api.get(`/rest/project/${bizId}/members`),
    tasks: (bizId) => api.get(`/rest/project/${bizId}/tasks`),
    createTask: (bizId, data) => api.post(`/rest/project/${bizId}/tasks`, data),
    updateTask: (taskId, data) => api.put(`/rest/project/tasks/${taskId}`, data),
    setTaskStatus: (taskId, taskSttsCd) => api.patch(`/rest/project/tasks/${taskId}/status`, { taskSttsCd }),
    deleteTask: (taskId) => api.delete(`/rest/project/tasks/${taskId}`),
};

export const meetingAPI = {
    rooms: () => api.get('/rest/meeting/room'),
    createRoom: (data) => api.post('/rest/meeting/room', data),
    reservations: (params) => api.get('/rest/meeting/reservations', { params }),
    detail: (reservationId) => api.get(`/rest/meeting/reservations/${reservationId}`),
    createReservation: (data) => api.post('/rest/meeting', data),
    updateReservation: (data) => api.put('/rest/meeting', data),
    deleteReservation: (reservationId) => api.delete(`/rest/meeting/${reservationId}`),
};

export const myPageAPI = {
    profile: () => api.get('/rest/mypage'),
    updateProfile: (data) => api.put('/rest/mypage/profile', toFormData(data), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    }),
    changePassword: (data) => api.put('/rest/mypage/password', data),
};

export { toFormData, toMultipartPayload, toNamedMultipartPayload };
export default api;
