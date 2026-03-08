import axios from 'axios';
import { apiBaseUrl } from './runtime';

export const STORAGE_KEYS = {
    user: 'starworks.user',
};

function createDisabledBridge() {
    return new Proxy({}, {
        get() {
            return () => {
                throw new Error('Legacy alternate runtime has been removed from this project.');
            };
        },
    });
}

const legacyApprovalBridge = createDisabledBridge();
const legacyAlarmBridge = createDisabledBridge();
const legacyAttendanceBridge = createDisabledBridge();
const legacyBoardBridge = createDisabledBridge();
const legacyCalendarBridge = createDisabledBridge();
const legacyCommonBridge = createDisabledBridge();
const legacyCommunityBridge = createDisabledBridge();
const legacyContractBridge = createDisabledBridge();
const legacyDashboardBridge = createDisabledBridge();
const legacyEmailBridge = createDisabledBridge();
const legacyMeetingBridge = createDisabledBridge();
const legacyMessengerBridge = createDisabledBridge();
const legacyProjectBridge = createDisabledBridge();

const api = axios.create({
    baseURL: apiBaseUrl || undefined,
    timeout: 15000,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
});

function legacyBridgeEnabled() {
    return false;
}

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
    list: () => (legacyBridgeEnabled() ? legacyCommonBridge.listUsers() : api.get('/rest/comm-user')),
    detail: (userId) => (legacyBridgeEnabled() ? legacyCommonBridge.getUserDetail(userId) : api.get(`/rest/comm-user/${userId}`)),
    me: () => (legacyBridgeEnabled() ? legacyCommonBridge.getMyProfile() : api.get('/rest/comm-user/me')),
    create: (data) => (legacyBridgeEnabled() ? legacyCommonBridge.createUser(data) : api.post('/rest/comm-user', data)),
    modify: (userId, data) => (legacyBridgeEnabled() ? legacyCommonBridge.updateUser(userId, data) : api.put(`/rest/comm-user/${userId}`, data)),
    retire: (userId) => (legacyBridgeEnabled() ? legacyCommonBridge.retireUser(userId) : api.patch(`/rest/comm-user/${userId}/retire`)),
    search: (term) => (legacyBridgeEnabled() ? legacyCommonBridge.searchUsers(term) : api.get('/rest/comm-user/search', { params: { term } })),
};

export const departmentAPI = {
    list: () => (legacyBridgeEnabled() ? legacyCommonBridge.listDepartments() : api.get('/rest/comm-depart')),
};

export const commonCodeAPI = {
    list: (codeGrpId) => (legacyBridgeEnabled() ? legacyCommonBridge.listCommonCodes(codeGrpId) : api.get('/rest/comm-code', { params: { codeGrpId } })),
};

export const dashboardAPI = {
    summary: () => (legacyBridgeEnabled() ? legacyDashboardBridge.summary() : api.get('/rest/dashboard')),
    bootstrap: () => (legacyBridgeEnabled() ? legacyDashboardBridge.bootstrap() : api.get('/rest/dashboard/bootstrap')),
    feed: (params = {}) => (legacyBridgeEnabled() ? legacyDashboardBridge.feed(params) : api.get('/rest/dashboard/feed', { params })),
    widgets: () => (legacyBridgeEnabled() ? legacyDashboardBridge.widgets() : api.get('/rest/dashboard/widgets')),
    preferences: () => (legacyBridgeEnabled() ? legacyDashboardBridge.preferences() : api.get('/rest/dashboard/preferences')),
    savePreferences: (data) => (legacyBridgeEnabled() ? legacyDashboardBridge.savePreferences(data) : api.put('/rest/dashboard/preferences', data)),
    categories: () => (legacyBridgeEnabled() ? legacyDashboardBridge.categories() : api.get('/rest/dashboard/categories')),
    saveCategories: (categories) => (legacyBridgeEnabled() ? legacyDashboardBridge.saveCategories(categories) : api.put('/rest/dashboard/categories', { categories })),
    markRead: (pstId) => (legacyBridgeEnabled() ? legacyDashboardBridge.markRead(pstId) : api.post(`/rest/dashboard/board-read/${pstId}`)),
    savePost: (pstId) => (legacyBridgeEnabled() ? legacyDashboardBridge.savePost(pstId) : api.post(`/rest/dashboard/saved-posts/${pstId}`)),
    unsavePost: (pstId) => (legacyBridgeEnabled() ? legacyDashboardBridge.unsavePost(pstId) : api.delete(`/rest/dashboard/saved-posts/${pstId}`)),
    favoriteUsers: () => (legacyBridgeEnabled() ? legacyDashboardBridge.favoriteUsers() : api.get('/rest/dashboard/favorite-users')),
    addFavoriteUser: (targetUserId) => (legacyBridgeEnabled() ? legacyDashboardBridge.addFavoriteUser(targetUserId) : api.post(`/rest/dashboard/favorite-users/${targetUserId}`)),
    removeFavoriteUser: (targetUserId) => (legacyBridgeEnabled() ? legacyDashboardBridge.removeFavoriteUser(targetUserId) : api.delete(`/rest/dashboard/favorite-users/${targetUserId}`)),
    todos: () => (legacyBridgeEnabled() ? legacyDashboardBridge.todos() : api.get('/rest/dashboard/todos')),
    createTodo: (data) => (legacyBridgeEnabled() ? legacyDashboardBridge.createTodo(data) : api.post('/rest/dashboard/todos', data)),
    updateTodo: (todoId, data) => (legacyBridgeEnabled() ? legacyDashboardBridge.updateTodo(todoId, data) : api.patch(`/rest/dashboard/todos/${todoId}`, data)),
    deleteTodo: (todoId) => (legacyBridgeEnabled() ? legacyDashboardBridge.deleteTodo(todoId) : api.delete(`/rest/dashboard/todos/${todoId}`)),
    recommendations: (box = 'inbox') => (legacyBridgeEnabled() ? legacyDashboardBridge.recommendations(box) : api.get('/rest/dashboard/category-recommendations', { params: { box } })),
    createRecommendations: (targetUserId, categoryCodes, message) => (legacyBridgeEnabled() ? legacyDashboardBridge.createRecommendations(targetUserId, categoryCodes, message) : api.post('/rest/dashboard/category-recommendations', { targetUserId, categoryCodes, message })),
    updateRecommendation: (recommendId, data) => (legacyBridgeEnabled() ? legacyDashboardBridge.updateRecommendation(recommendId, data) : api.patch(`/rest/dashboard/category-recommendations/${recommendId}`, data)),
    profile: (userId) => (legacyBridgeEnabled() ? legacyDashboardBridge.profile(userId) : api.get(`/rest/dashboard/profile/${userId}`)),
};

export const alarmAPI = {
    list: () => (legacyBridgeEnabled() ? legacyAlarmBridge.list() : api.get('/rest/alarm-log-list')),
    top10: () => (legacyBridgeEnabled() ? legacyAlarmBridge.top10() : api.get('/rest/alarm-log-top10')),
    detail: (alarmId) => (legacyBridgeEnabled() ? legacyAlarmBridge.detail(alarmId) : api.get(`/rest/alarm-log/${alarmId}`)),
    markAllRead: () => (legacyBridgeEnabled() ? legacyAlarmBridge.markAllRead() : api.put('/rest/alarm-log-list')),
};

export const approvalAPI = {
    templates: () => (legacyBridgeEnabled() ? legacyApprovalBridge.templates() : api.get('/rest/approval-template')),
    templateDetail: (atrzDocTmplId) => api.get(`/rest/approval-template/${atrzDocTmplId}`),
    summary: () => (legacyBridgeEnabled() ? legacyApprovalBridge.summary() : api.get('/rest/approval-documents/summary')),
    list: (params = {}) => (legacyBridgeEnabled() ? legacyApprovalBridge.list(params) : api.get('/rest/approval-documents', { params })),
    detail: (atrzDocId) => (legacyBridgeEnabled() ? legacyApprovalBridge.detail(atrzDocId) : api.get(`/rest/approval-documents/${atrzDocId}`)),
    create: (payload, files = []) => (
        legacyBridgeEnabled()
            ? legacyApprovalBridge.create(payload, files)
            : api.post('/rest/approval-documents', toMultipartPayload(payload, files), {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })
    ),
    approve: (atrzDocId, data) => (legacyBridgeEnabled() ? legacyApprovalBridge.approve(atrzDocId, data) : api.post(`/rest/approval-documents/${atrzDocId}/approve`, data)),
    reject: (atrzDocId, data) => (legacyBridgeEnabled() ? legacyApprovalBridge.reject(atrzDocId, data) : api.post(`/rest/approval-documents/${atrzDocId}/reject`, data)),
    retract: (atrzDocId) => (legacyBridgeEnabled() ? legacyApprovalBridge.retract(atrzDocId) : api.post(`/rest/approval-documents/${atrzDocId}/retract`)),
    customLines: () => (legacyBridgeEnabled() ? legacyApprovalBridge.customLines() : api.get('/rest/approval-customline')),
    createCustomLine: (data) => (legacyBridgeEnabled() ? legacyApprovalBridge.createCustomLine(data) : api.post('/rest/approval-customline', data)),
    deleteCustomLine: (name) => (legacyBridgeEnabled() ? legacyApprovalBridge.deleteCustomLine(name) : api.delete(`/rest/approval-customline/${encodeURIComponent(name)}`)),
    vacationBalance: () => (legacyBridgeEnabled() ? legacyApprovalBridge.vacationBalance() : api.get('/rest/approval-vacation/E101')),
    tempList: () => (legacyBridgeEnabled() ? legacyApprovalBridge.tempList() : api.get('/rest/approval-temp')),
    tempDetail: (atrzTempSqn) => (legacyBridgeEnabled() ? legacyApprovalBridge.tempDetail(atrzTempSqn) : api.get(`/rest/approval-temp/${atrzTempSqn}`)),
    saveTemp: (payload, files = []) => (
        legacyBridgeEnabled()
            ? legacyApprovalBridge.saveTemp(payload, files)
            : api.post('/rest/approval-temp', toMultipartPayload(payload, files), {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })
    ),
    updateTemp: (atrzTempSqn, payload, files = []) => (
        legacyBridgeEnabled()
            ? legacyApprovalBridge.updateTemp(atrzTempSqn, payload, files)
            : api.put(`/rest/approval-temp/${atrzTempSqn}`, toMultipartPayload(payload, files), {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })
    ),
    deleteTemp: (atrzTempSqn) => (legacyBridgeEnabled() ? legacyApprovalBridge.deleteTemp(atrzTempSqn) : api.delete(`/rest/approval-temp/${atrzTempSqn}`)),
};

export const contractAPI = {
    dashboard: () => (legacyBridgeEnabled() ? legacyContractBridge.dashboard() : api.get('/rest/contracts/dashboard')),
    list: (params = {}) => (legacyBridgeEnabled() ? legacyContractBridge.list(params) : api.get('/rest/contracts', { params })),
    detail: (contractId) => (legacyBridgeEnabled() ? legacyContractBridge.detail(contractId) : api.get(`/rest/contracts/${contractId}`)),
    create: (payload) => (legacyBridgeEnabled() ? legacyContractBridge.create(payload) : api.post('/rest/contracts', payload)),
    send: (contractId) => (legacyBridgeEnabled() ? legacyContractBridge.send(contractId) : api.post(`/rest/contracts/${contractId}/send`)),
    cancel: (contractId, data = {}) => (legacyBridgeEnabled() ? legacyContractBridge.cancel(contractId, data) : api.post(`/rest/contracts/${contractId}/cancel`, data)),
    remind: (contractId) => (legacyBridgeEnabled() ? legacyContractBridge.remind(contractId) : api.post(`/rest/contracts/${contractId}/remind`)),
    links: (contractId) => (legacyBridgeEnabled() ? legacyContractBridge.links(contractId) : api.get(`/rest/contracts/${contractId}/links`)),
    createBatch: (payload) => (legacyBridgeEnabled() ? legacyContractBridge.createBatch(payload) : api.post('/rest/contracts/batches', payload)),
    batchDetail: (batchId) => (legacyBridgeEnabled() ? legacyContractBridge.batchDetail(batchId) : api.get(`/rest/contracts/batches/${batchId}`)),
    templates: () => (legacyBridgeEnabled() ? legacyContractBridge.templates() : api.get('/rest/contracts/templates')),
    templateDetail: (templateId) => (legacyBridgeEnabled() ? legacyContractBridge.templateDetail(templateId) : api.get(`/rest/contracts/templates/${templateId}`)),
    createTemplate: (payload, files = {}) => {
        if (legacyBridgeEnabled()) {
            return legacyContractBridge.createTemplate(payload, files);
        }
        if (files.sourceFile || files.backgroundFile) {
            return api.post('/rest/contracts/templates', toNamedMultipartPayload(payload, files), {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
        }
        return api.post('/rest/contracts/templates', payload);
    },
    updateTemplate: (templateId, payload, files = {}) => {
        if (legacyBridgeEnabled()) {
            return legacyContractBridge.updateTemplate(templateId, payload, files);
        }
        if (files.sourceFile || files.backgroundFile) {
            return api.put(`/rest/contracts/templates/${templateId}`, toNamedMultipartPayload(payload, files), {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
        }
        return api.put(`/rest/contracts/templates/${templateId}`, payload);
    },
    publishTemplate: (templateId, templateVersionId) => (legacyBridgeEnabled() ? legacyContractBridge.publishTemplate(templateId, templateVersionId) : api.post(`/rest/contracts/templates/${templateId}/publish`, templateVersionId ? { templateVersionId } : {})),
    templateRequests: () => (legacyBridgeEnabled() ? legacyContractBridge.templateRequests() : api.get('/rest/contracts/template-requests')),
    createTemplateRequest: (payload, files = {}) => {
        if (legacyBridgeEnabled()) {
            return legacyContractBridge.createTemplateRequest(payload, files);
        }
        if (files.sourceFile || files.markedFile || files.sealFile) {
            return api.post('/rest/contracts/template-requests', toNamedMultipartPayload(payload, files), {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
        }
        return api.post('/rest/contracts/template-requests', payload);
    },
    approveTemplateRequest: (requestId, data = {}) => (legacyBridgeEnabled() ? legacyContractBridge.approveTemplateRequest(requestId, data) : api.post(`/rest/contracts/template-requests/${requestId}/approve`, data)),
    rejectTemplateRequest: (requestId, data = {}) => (legacyBridgeEnabled() ? legacyContractBridge.rejectTemplateRequest(requestId, data) : api.post(`/rest/contracts/template-requests/${requestId}/reject`, data)),
    companySettings: () => (legacyBridgeEnabled() ? legacyContractBridge.companySettings() : api.get('/rest/contracts/company-settings')),
    updateCompanySettings: (payload, files = {}) => {
        if (legacyBridgeEnabled()) {
            return legacyContractBridge.updateCompanySettings(payload, files);
        }
        if (files.sealFile) {
            return api.put('/rest/contracts/company-settings', toNamedMultipartPayload(payload, files), {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
        }
        return api.put('/rest/contracts/company-settings', payload);
    },
    publicDetail: (token) => (legacyBridgeEnabled() ? legacyContractBridge.publicDetail(token) : api.get(`/rest/contracts/public/${token}`)),
    publicClaim: (token, payload) => (legacyBridgeEnabled() ? legacyContractBridge.publicClaim(token, payload) : api.post(`/rest/contracts/public/${token}/claim`, payload)),
    publicSubmit: (token, payload) => (legacyBridgeEnabled() ? legacyContractBridge.publicSubmit(token, payload) : api.post(`/rest/contracts/public/${token}/submit`, payload)),
    publicDownload: (token) => (legacyBridgeEnabled() ? legacyContractBridge.publicDownload(token) : api.get(`/rest/contracts/public/${token}/download`, { responseType: 'blob' })),
};

export const boardAPI = {
    notices: () => (legacyBridgeEnabled() ? legacyBoardBridge.notices() : api.get('/rest/board-notice')),
    community: (bbsCtgrCd) => (legacyBridgeEnabled() ? legacyBoardBridge.community(bbsCtgrCd) : api.get('/rest/board-community', { params: bbsCtgrCd ? { bbsCtgrCd } : {} })),
    categoryCounts: () => (legacyBridgeEnabled() ? legacyBoardBridge.categoryCounts() : api.get('/rest/board-category-counts')),
    detail: (pstId) => (legacyBridgeEnabled() ? legacyBoardBridge.detail(pstId) : api.get(`/rest/board/${pstId}`)),
    create: (data) => (legacyBridgeEnabled() ? legacyBoardBridge.create(data) : api.post('/rest/board', data)),
    update: (pstId, data) => (legacyBridgeEnabled() ? legacyBoardBridge.update(pstId, data) : api.put(`/rest/board/${pstId}`, data)),
    remove: (pstId) => (legacyBridgeEnabled() ? legacyBoardBridge.remove(pstId) : api.delete(`/rest/board/${pstId}`)),
    incrementView: (pstId) => (legacyBridgeEnabled() ? legacyBoardBridge.incrementView(pstId) : api.put(`/rest/board-vct/${pstId}`)),
    comments: (pstId) => (legacyBridgeEnabled() ? legacyBoardBridge.comments(pstId) : api.get(`/rest/board-comment/${pstId}`)),
    createComment: (pstId, data) => (legacyBridgeEnabled() ? legacyBoardBridge.createComment(pstId, data) : api.post(`/rest/board-comment/${pstId}`, data)),
    updateComment: (pstId, cmntSqn, data) => (legacyBridgeEnabled() ? legacyBoardBridge.updateComment(pstId, cmntSqn, data) : api.put(`/rest/board-comment/${pstId}`, data, { params: { cmntSqn } })),
    deleteComment: (pstId, cmntSqn) => (legacyBridgeEnabled() ? legacyBoardBridge.deleteComment(pstId, cmntSqn) : api.delete(`/rest/board-comment/${pstId}`, { params: { cmntSqn } })),
    workspace: (params = {}) => (legacyBridgeEnabled() ? legacyBoardBridge.workspace(params) : api.get('/rest/boards', { params })),
    workspaceDetail: (pstId) => (legacyBridgeEnabled() ? legacyBoardBridge.workspaceDetail(pstId) : api.get(`/rest/boards/${pstId}`)),
    createWorkspace: (payload, files = []) => (legacyBridgeEnabled() ? legacyBoardBridge.createWorkspace(payload, files) : api.post('/rest/boards', toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    })),
    updateWorkspace: (pstId, payload, files = []) => (legacyBridgeEnabled() ? legacyBoardBridge.updateWorkspace(pstId, payload, files) : api.put(`/rest/boards/${pstId}`, toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    })),
    toggleLikePost: (pstId) => (legacyBridgeEnabled() ? legacyBoardBridge.toggleLikePost(pstId) : api.post(`/rest/boards/${pstId}/likes`)),
    likeUsers: (pstId) => (legacyBridgeEnabled() ? legacyBoardBridge.likeUsers(pstId) : api.get(`/rest/boards/${pstId}/likes`)),
    readers: (pstId) => (legacyBridgeEnabled() ? legacyBoardBridge.readers(pstId) : api.get(`/rest/boards/${pstId}/readers`)),
    share: (pstId, data) => (legacyBridgeEnabled() ? legacyBoardBridge.share(pstId, data) : api.post(`/rest/boards/${pstId}/share`, data)),
    report: (pstId, data) => (legacyBridgeEnabled() ? legacyBoardBridge.report(pstId, data) : api.post(`/rest/boards/${pstId}/report`, data)),
    pin: (pstId, fixedYn) => (legacyBridgeEnabled() ? legacyBoardBridge.pin(pstId, fixedYn) : api.post(`/rest/boards/${pstId}/pin`, { fixedYn })),
    votePoll: (pstId, optionIds) => (legacyBridgeEnabled() ? legacyBoardBridge.votePoll(pstId, optionIds) : api.post(`/rest/boards/${pstId}/poll/vote`, { optionIds })),
    updateTodoAssignee: (pstId, assigneeUserId, statusCd) => (legacyBridgeEnabled() ? legacyBoardBridge.updateTodoAssignee(pstId, assigneeUserId, statusCd) : api.patch(`/rest/boards/${pstId}/todo-assignees/${assigneeUserId}`, { statusCd })),
    scheduleAvailability: (data) => (legacyBridgeEnabled() ? legacyBoardBridge.scheduleAvailability(data) : api.post('/rest/boards/schedule/availability', data)),
    createCommentMultipart: (pstId, payload, files = [], upCmntSqn = '') => (legacyBridgeEnabled() ? legacyBoardBridge.createCommentMultipart(pstId, payload, files, upCmntSqn) : api.post(`/rest/board-comment/${pstId}`, toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
        params: upCmntSqn ? { upCmntSqn } : {},
    })),
    updateCommentMultipart: (pstId, cmntSqn, payload, files = []) => (legacyBridgeEnabled() ? legacyBoardBridge.updateCommentMultipart(pstId, cmntSqn, payload, files) : api.put(`/rest/board-comment/${pstId}`, toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
        params: { cmntSqn },
    })),
};

export const attendanceAPI = {
    today: (userId, workYmd = new Date().toISOString().slice(0, 10).replace(/-/g, '')) => (legacyBridgeEnabled() ? legacyAttendanceBridge.today(userId, workYmd) : api.get(`/rest/attendance/${userId}/${workYmd}`)),
    history: (userId) => (legacyBridgeEnabled() ? legacyAttendanceBridge.history(userId) : api.get(`/rest/attendance/${userId}`)),
    clockIn: () => (legacyBridgeEnabled() ? legacyAttendanceBridge.clockIn() : api.post('/rest/attendance')),
    clockOut: (workYmd) => (legacyBridgeEnabled() ? legacyAttendanceBridge.clockOut(workYmd) : api.put('/rest/attendance', { workYmd })),
    week: () => (legacyBridgeEnabled() ? legacyAttendanceBridge.week() : api.get('/rest/attendance-stats/week')),
    month: () => (legacyBridgeEnabled() ? legacyAttendanceBridge.month() : api.get('/rest/attendance-stats/month')),
    monthList: () => (legacyBridgeEnabled() ? legacyAttendanceBridge.monthList() : api.get('/rest/attendance-stats/month-list')),
    depart: () => (legacyBridgeEnabled() ? legacyAttendanceBridge.depart() : api.get('/rest/attendance-stats/depart')),
};

export const calendarAPI = {
    events: (params = {}) => (legacyBridgeEnabled() ? legacyCalendarBridge.events(params) : api.get('/rest/calendar/events', { params })),
    user: () => api.get('/rest/calendar-user'),
    userDetail: (userSchdId) => api.get(`/rest/calendar-user/${userSchdId}`),
    createUser: (data) => (legacyBridgeEnabled() ? legacyCalendarBridge.createUser(data) : api.post('/rest/calendar-user', data)),
    updateUser: (userSchdId, data) => (legacyBridgeEnabled() ? legacyCalendarBridge.updateUser(userSchdId, data) : api.put(`/rest/calendar-user/${userSchdId}`, data)),
    deleteUser: (userSchdId) => (legacyBridgeEnabled() ? legacyCalendarBridge.deleteUser(userSchdId) : api.delete(`/rest/calendar-user/${userSchdId}`)),
    dept: () => api.get('/rest/calendar-depart'),
    deptDetail: (deptSchdId) => api.get(`/rest/calendar-depart/${deptSchdId}`),
    createDept: (data) => (legacyBridgeEnabled() ? legacyCalendarBridge.createDept(data) : api.post('/rest/calendar-depart', data)),
    updateDept: (deptSchdId, data) => (legacyBridgeEnabled() ? legacyCalendarBridge.updateDept(deptSchdId, data) : api.put(`/rest/calendar-depart/${deptSchdId}`, data)),
    deleteDept: (deptSchdId) => (legacyBridgeEnabled() ? legacyCalendarBridge.deleteDept(deptSchdId) : api.delete(`/rest/calendar-depart/${deptSchdId}`)),
    teamProjects: () => (legacyBridgeEnabled() ? legacyCalendarBridge.teamProjects() : api.get('/rest/fullcalendar-team/project-list')),
};

export const emailAPI = {
    counts: () => (legacyBridgeEnabled() ? legacyEmailBridge.counts() : api.get('/mail/counts')),
    list: (mailboxTypeCd, page = 1, searchWord = '') => (legacyBridgeEnabled() ? legacyEmailBridge.list(mailboxTypeCd, page, searchWord) : api.get(`/mail/listData/${mailboxTypeCd}`, {
        params: {
            page,
            ...(searchWord ? { searchWord } : {}),
        },
    })),
    toggleImportance: (emailContId) => (legacyBridgeEnabled() ? legacyEmailBridge.toggleImportance(emailContId) : api.post(`/mail/toggle-importance/${emailContId}`)),
    deleteSelected: (emailContIds, mailboxTypeCd) => (legacyBridgeEnabled() ? legacyEmailBridge.deleteSelected(emailContIds, mailboxTypeCd) : api.post('/mail/deleteSelected', { emailContIds, mailboxTypeCd })),
    deleteAll: (mailboxTypeCd) => (legacyBridgeEnabled() ? legacyEmailBridge.deleteAll(mailboxTypeCd) : api.post('/mail/deleteAll', { mailboxTypeCd })),
    restoreSelected: (emailContIds) => (legacyBridgeEnabled() ? legacyEmailBridge.restoreSelected(emailContIds) : api.post('/mail/restoreSelected', { emailContIds })),
};

export const messengerAPI = {
    currentUser: () => (legacyBridgeEnabled() ? legacyMessengerBridge.currentUser() : api.get('/chat/current-user')),
    users: () => (legacyBridgeEnabled() ? legacyMessengerBridge.users() : api.get('/chat/users')),
    panel: () => (legacyBridgeEnabled() ? legacyMessengerBridge.panel() : api.get('/chat/panel')),
    rooms: (params = {}) => (legacyBridgeEnabled() ? legacyMessengerBridge.rooms(params) : api.get('/chat/rooms', { params })),
    roomDetail: (msgrId) => (legacyBridgeEnabled() ? legacyMessengerBridge.roomDetail(msgrId) : api.get(`/chat/room/${msgrId}`)),
    messages: (msgrId) => (legacyBridgeEnabled() ? legacyMessengerBridge.messages(msgrId) : api.get(`/chat/room/${msgrId}/messages`)),
    searchMessages: (msgrId, q) => (legacyBridgeEnabled() ? legacyMessengerBridge.searchMessages(msgrId, q) : api.get(`/chat/room/${msgrId}/search`, { params: { q } })),
    findOrCreate: (userId) => (legacyBridgeEnabled() ? legacyMessengerBridge.findOrCreate(userId) : api.get('/chat/room/findOrCreate', { params: { userId } })),
    selfRoom: () => (legacyBridgeEnabled() ? legacyMessengerBridge.selfRoom() : api.post('/chat/room/self')),
    createRoom: (data) => (legacyBridgeEnabled() ? legacyMessengerBridge.createRoom(data) : api.post('/chat/room/create', data)),
    send: (msgrId, contents, options = {}) => (legacyBridgeEnabled() ? legacyMessengerBridge.send(msgrId, contents, options) : Promise.resolve(null)),
    invite: (msgrId, userIds) => (legacyBridgeEnabled() ? legacyMessengerBridge.invite(msgrId, userIds) : api.post(`/chat/room/${msgrId}/invite`, { userIds })),
    kick: (msgrId, userId) => (legacyBridgeEnabled() ? legacyMessengerBridge.kick(msgrId, userId) : api.post(`/chat/room/${msgrId}/kick`, { userId })),
    markAsRead: (msgrId) => (legacyBridgeEnabled() ? legacyMessengerBridge.markAsRead(msgrId) : api.post(`/chat/room/markAsRead/${msgrId}`)),
    renameRoom: (msgrId, msgrNm) => (legacyBridgeEnabled() ? legacyMessengerBridge.renameRoom(msgrId, msgrNm) : api.post(`/chat/room/${msgrId}/name`, { msgrNm })),
    participants: (msgrId) => (legacyBridgeEnabled() ? legacyMessengerBridge.participants(msgrId) : api.get(`/chat/room/${msgrId}/participants`)),
    notify: (msgrId, notifyEnabled) => (legacyBridgeEnabled() ? legacyMessengerBridge.notify(msgrId, notifyEnabled) : api.patch(`/chat/room/${msgrId}/notify`, { notifyEnabled })),
    pin: (msgrId, msgContId) => (legacyBridgeEnabled() ? legacyMessengerBridge.pin(msgrId, msgContId) : api.patch(`/chat/room/${msgrId}/pin`, { msgContId })),
    clearPin: (msgrId) => (legacyBridgeEnabled() ? legacyMessengerBridge.clearPin(msgrId) : api.patch(`/chat/room/${msgrId}/pin`, {})),
    leave: (msgrId) => (legacyBridgeEnabled() ? legacyMessengerBridge.leave(msgrId) : api.post(`/chat/room/${msgrId}/leave`)),
    deleteMessage: (msgContId, msgrId) => (legacyBridgeEnabled() ? legacyMessengerBridge.deleteMessage(msgContId, msgrId) : api.delete(`/chat/message/${msgContId}`, { params: { msgrId } })),
    forwardMessage: (msgContId, targetRoomId) => (legacyBridgeEnabled() ? legacyMessengerBridge.forwardMessage(msgContId, targetRoomId) : api.post(`/chat/message/${msgContId}/forward`, { targetRoomId })),
    uploadFiles: (msgrId, contents, files = []) => {
        if (legacyBridgeEnabled()) {
            return legacyMessengerBridge.uploadFiles(msgrId, contents, files);
        }
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
    exportMessages: (msgrId) => (legacyBridgeEnabled() ? legacyMessengerBridge.exportMessages(msgrId) : api.get(`/chat/room/${msgrId}/export.xlsx`, { responseType: 'blob' })),
};

export const communityAPI = {
    list: (params = {}) => (legacyBridgeEnabled() ? legacyCommunityBridge.list(params) : api.get('/rest/communities', { params: typeof params === 'string' ? (params ? { q: params } : {}) : params })),
    search: (params = {}) => (legacyBridgeEnabled() ? legacyCommunityBridge.search(params) : api.get('/rest/communities/search', { params: typeof params === 'string' ? (params ? { q: params } : {}) : params })),
    detail: (communityId) => (legacyBridgeEnabled() ? legacyCommunityBridge.detail(communityId) : api.get(`/rest/communities/${communityId}`)),
    create: (payload, files = {}) => {
        if (legacyBridgeEnabled()) {
            return legacyCommunityBridge.create(payload, files);
        }
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
        if (legacyBridgeEnabled()) {
            return legacyCommunityBridge.update(communityId, payload, files);
        }
        if (files.iconFile || files.coverFile) {
            return api.patch(`/rest/communities/${communityId}`, toNamedMultipartPayload(payload, files), {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            });
        }
        return api.patch(`/rest/communities/${communityId}`, payload);
    },
    remove: (communityId) => (legacyBridgeEnabled() ? legacyCommunityBridge.remove(communityId) : api.delete(`/rest/communities/${communityId}`)),
    close: (communityId) => (legacyBridgeEnabled() ? legacyCommunityBridge.close(communityId) : api.post(`/rest/communities/${communityId}/close`)),
    join: (communityId) => (legacyBridgeEnabled() ? legacyCommunityBridge.join(communityId) : api.post(`/rest/communities/${communityId}/join`)),
    leave: (communityId) => (legacyBridgeEnabled() ? legacyCommunityBridge.leave(communityId) : api.post(`/rest/communities/${communityId}/leave`)),
    members: (communityId, status = '') => (legacyBridgeEnabled() ? legacyCommunityBridge.members(communityId, status) : api.get(`/rest/communities/${communityId}/members`, { params: status ? { status } : {} })),
    requests: (communityId) => (legacyBridgeEnabled() ? legacyCommunityBridge.requests(communityId) : api.get(`/rest/communities/${communityId}/requests`)),
    addMembers: (communityId, userIds) => (legacyBridgeEnabled() ? legacyCommunityBridge.addMembers(communityId, userIds) : api.post(`/rest/communities/${communityId}/members`, { userIds })),
    removeMember: (communityId, userId) => (legacyBridgeEnabled() ? legacyCommunityBridge.removeMember(communityId, userId) : api.delete(`/rest/communities/${communityId}/members/${userId}`)),
    updateRole: (communityId, userId, roleCd) => (legacyBridgeEnabled() ? legacyCommunityBridge.updateRole(communityId, userId, roleCd) : api.patch(`/rest/communities/${communityId}/members/${userId}/role`, { roleCd })),
    approveRequest: (communityId, userId) => (legacyBridgeEnabled() ? legacyCommunityBridge.approveRequest(communityId, userId) : api.post(`/rest/communities/${communityId}/requests/${userId}/approve`)),
    rejectRequest: (communityId, userId) => (legacyBridgeEnabled() ? legacyCommunityBridge.rejectRequest(communityId, userId) : api.post(`/rest/communities/${communityId}/requests/${userId}/reject`)),
    favorite: (communityId, favoriteYn) => (legacyBridgeEnabled() ? legacyCommunityBridge.favorite(communityId, favoriteYn) : api.put(`/rest/communities/${communityId}/favorite`, { favoriteYn })),
    saveOrder: (communityIds) => (legacyBridgeEnabled() ? legacyCommunityBridge.saveOrder(communityIds) : api.put('/rest/communities/order', { communityIds })),
    syncOrg: () => (legacyBridgeEnabled() ? legacyCommunityBridge.syncOrg() : api.post('/rest/communities/sync-org')),
};

export const projectAPI = {
    list: () => (legacyBridgeEnabled() ? legacyProjectBridge.list() : api.get('/rest/project')),
    detail: (bizId) => (legacyBridgeEnabled() ? legacyProjectBridge.detail(bizId) : api.get(`/rest/project/${bizId}`)),
    create: (data) => (legacyBridgeEnabled() ? legacyProjectBridge.create(data) : api.post('/rest/project', data)),
    update: (bizId, data) => (legacyBridgeEnabled() ? legacyProjectBridge.update(bizId, data) : api.put(`/rest/project/${bizId}`, data)),
    setStatus: (bizId, bizSttsCd) => (legacyBridgeEnabled() ? legacyProjectBridge.setStatus(bizId, bizSttsCd) : api.patch(`/rest/project/${bizId}/status`, { bizSttsCd })),
    members: (bizId) => api.get(`/rest/project/${bizId}/members`),
    tasks: (bizId) => (legacyBridgeEnabled() ? legacyProjectBridge.tasks(bizId) : api.get(`/rest/project/${bizId}/tasks`)),
    createTask: (bizId, data) => (legacyBridgeEnabled() ? legacyProjectBridge.createTask(bizId, data) : api.post(`/rest/project/${bizId}/tasks`, data)),
    updateTask: (taskId, data) => (legacyBridgeEnabled() ? legacyProjectBridge.updateTask(taskId, data) : api.put(`/rest/project/tasks/${taskId}`, data)),
    setTaskStatus: (taskId, taskSttsCd) => (legacyBridgeEnabled() ? legacyProjectBridge.setTaskStatus(taskId, taskSttsCd) : api.patch(`/rest/project/tasks/${taskId}/status`, { taskSttsCd })),
    deleteTask: (taskId) => (legacyBridgeEnabled() ? legacyProjectBridge.deleteTask(taskId) : api.delete(`/rest/project/tasks/${taskId}`)),
};

export const meetingAPI = {
    rooms: () => (legacyBridgeEnabled() ? legacyMeetingBridge.rooms() : api.get('/rest/meeting/room')),
    createRoom: (data) => (legacyBridgeEnabled() ? legacyMeetingBridge.createRoom(data) : api.post('/rest/meeting/room', data)),
    reservations: (params) => (legacyBridgeEnabled() ? legacyMeetingBridge.reservations(params) : api.get('/rest/meeting/reservations', { params })),
    detail: (reservationId) => (legacyBridgeEnabled() ? legacyMeetingBridge.detail(reservationId) : api.get(`/rest/meeting/reservations/${reservationId}`)),
    createReservation: (data) => (legacyBridgeEnabled() ? legacyMeetingBridge.createReservation(data) : api.post('/rest/meeting', data)),
    updateReservation: (data) => (legacyBridgeEnabled() ? legacyMeetingBridge.updateReservation(data) : api.put('/rest/meeting', data)),
    deleteReservation: (reservationId) => (legacyBridgeEnabled() ? legacyMeetingBridge.deleteReservation(reservationId) : api.delete(`/rest/meeting/${reservationId}`)),
};

export const myPageAPI = {
    profile: () => (legacyBridgeEnabled() ? legacyCommonBridge.getMyProfile() : api.get('/rest/mypage')),
    updateProfile: (data) => (
        legacyBridgeEnabled()
            ? legacyCommonBridge.updateProfile(data)
            : api.put('/rest/mypage/profile', toFormData(data), {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })
    ),
    changePassword: (data) => api.put('/rest/mypage/password', data),
};

export { toFormData, toMultipartPayload, toNamedMultipartPayload };
export default api;
