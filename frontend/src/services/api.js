import axios from 'axios';
import {
    firebaseApprovalBridge,
    firebaseAlarmBridge,
    firebaseAttendanceBridge,
    firebaseBoardBridge,
    firebaseCalendarBridge,
    firebaseCommonBridge,
    firebaseCommunityBridge,
    firebaseContractBridge,
    firebaseDashboardBridge,
    firebaseEmailBridge,
    firebaseMeetingBridge,
    firebaseMessengerBridge,
    firebaseProjectBridge,
} from './firebase/bridge';
import { isFirebasePlatformEnabled } from './firebase/platform';

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

function firebaseBackendEnabled() {
    return isFirebasePlatformEnabled();
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
    list: () => (firebaseBackendEnabled() ? firebaseCommonBridge.listUsers() : api.get('/rest/comm-user')),
    detail: (userId) => (firebaseBackendEnabled() ? firebaseCommonBridge.getUserDetail(userId) : api.get(`/rest/comm-user/${userId}`)),
    me: () => (firebaseBackendEnabled() ? firebaseCommonBridge.getMyProfile() : api.get('/rest/comm-user/me')),
    create: (data) => (firebaseBackendEnabled() ? firebaseCommonBridge.createUser(data) : api.post('/rest/comm-user', data)),
    modify: (userId, data) => (firebaseBackendEnabled() ? firebaseCommonBridge.updateUser(userId, data) : api.put(`/rest/comm-user/${userId}`, data)),
    retire: (userId) => (firebaseBackendEnabled() ? firebaseCommonBridge.retireUser(userId) : api.patch(`/rest/comm-user/${userId}/retire`)),
    search: (term) => (firebaseBackendEnabled() ? firebaseCommonBridge.searchUsers(term) : api.get('/rest/comm-user/search', { params: { term } })),
};

export const departmentAPI = {
    list: () => (firebaseBackendEnabled() ? firebaseCommonBridge.listDepartments() : api.get('/rest/comm-depart')),
};

export const commonCodeAPI = {
    list: (codeGrpId) => (firebaseBackendEnabled() ? firebaseCommonBridge.listCommonCodes(codeGrpId) : api.get('/rest/comm-code', { params: { codeGrpId } })),
};

export const dashboardAPI = {
    summary: () => (firebaseBackendEnabled() ? firebaseDashboardBridge.summary() : api.get('/rest/dashboard')),
    bootstrap: () => (firebaseBackendEnabled() ? firebaseDashboardBridge.bootstrap() : api.get('/rest/dashboard/bootstrap')),
    feed: (params = {}) => (firebaseBackendEnabled() ? firebaseDashboardBridge.feed(params) : api.get('/rest/dashboard/feed', { params })),
    widgets: () => (firebaseBackendEnabled() ? firebaseDashboardBridge.widgets() : api.get('/rest/dashboard/widgets')),
    preferences: () => (firebaseBackendEnabled() ? firebaseDashboardBridge.preferences() : api.get('/rest/dashboard/preferences')),
    savePreferences: (data) => (firebaseBackendEnabled() ? firebaseDashboardBridge.savePreferences(data) : api.put('/rest/dashboard/preferences', data)),
    categories: () => (firebaseBackendEnabled() ? firebaseDashboardBridge.categories() : api.get('/rest/dashboard/categories')),
    saveCategories: (categories) => (firebaseBackendEnabled() ? firebaseDashboardBridge.saveCategories(categories) : api.put('/rest/dashboard/categories', { categories })),
    markRead: (pstId) => (firebaseBackendEnabled() ? firebaseDashboardBridge.markRead(pstId) : api.post(`/rest/dashboard/board-read/${pstId}`)),
    savePost: (pstId) => (firebaseBackendEnabled() ? firebaseDashboardBridge.savePost(pstId) : api.post(`/rest/dashboard/saved-posts/${pstId}`)),
    unsavePost: (pstId) => (firebaseBackendEnabled() ? firebaseDashboardBridge.unsavePost(pstId) : api.delete(`/rest/dashboard/saved-posts/${pstId}`)),
    favoriteUsers: () => (firebaseBackendEnabled() ? firebaseDashboardBridge.favoriteUsers() : api.get('/rest/dashboard/favorite-users')),
    addFavoriteUser: (targetUserId) => (firebaseBackendEnabled() ? firebaseDashboardBridge.addFavoriteUser(targetUserId) : api.post(`/rest/dashboard/favorite-users/${targetUserId}`)),
    removeFavoriteUser: (targetUserId) => (firebaseBackendEnabled() ? firebaseDashboardBridge.removeFavoriteUser(targetUserId) : api.delete(`/rest/dashboard/favorite-users/${targetUserId}`)),
    todos: () => (firebaseBackendEnabled() ? firebaseDashboardBridge.todos() : api.get('/rest/dashboard/todos')),
    createTodo: (data) => (firebaseBackendEnabled() ? firebaseDashboardBridge.createTodo(data) : api.post('/rest/dashboard/todos', data)),
    updateTodo: (todoId, data) => (firebaseBackendEnabled() ? firebaseDashboardBridge.updateTodo(todoId, data) : api.patch(`/rest/dashboard/todos/${todoId}`, data)),
    deleteTodo: (todoId) => (firebaseBackendEnabled() ? firebaseDashboardBridge.deleteTodo(todoId) : api.delete(`/rest/dashboard/todos/${todoId}`)),
    recommendations: (box = 'inbox') => (firebaseBackendEnabled() ? firebaseDashboardBridge.recommendations(box) : api.get('/rest/dashboard/category-recommendations', { params: { box } })),
    createRecommendations: (targetUserId, categoryCodes, message) => (firebaseBackendEnabled() ? firebaseDashboardBridge.createRecommendations(targetUserId, categoryCodes, message) : api.post('/rest/dashboard/category-recommendations', { targetUserId, categoryCodes, message })),
    updateRecommendation: (recommendId, data) => (firebaseBackendEnabled() ? firebaseDashboardBridge.updateRecommendation(recommendId, data) : api.patch(`/rest/dashboard/category-recommendations/${recommendId}`, data)),
    profile: (userId) => (firebaseBackendEnabled() ? firebaseDashboardBridge.profile(userId) : api.get(`/rest/dashboard/profile/${userId}`)),
};

export const alarmAPI = {
    list: () => (firebaseBackendEnabled() ? firebaseAlarmBridge.list() : api.get('/rest/alarm-log-list')),
    top10: () => (firebaseBackendEnabled() ? firebaseAlarmBridge.top10() : api.get('/rest/alarm-log-top10')),
    detail: (alarmId) => (firebaseBackendEnabled() ? firebaseAlarmBridge.detail(alarmId) : api.get(`/rest/alarm-log/${alarmId}`)),
    markAllRead: () => (firebaseBackendEnabled() ? firebaseAlarmBridge.markAllRead() : api.put('/rest/alarm-log-list')),
};

export const approvalAPI = {
    templates: () => (firebaseBackendEnabled() ? firebaseApprovalBridge.templates() : api.get('/rest/approval-template')),
    templateDetail: (atrzDocTmplId) => api.get(`/rest/approval-template/${atrzDocTmplId}`),
    summary: () => (firebaseBackendEnabled() ? firebaseApprovalBridge.summary() : api.get('/rest/approval-documents/summary')),
    list: (params = {}) => (firebaseBackendEnabled() ? firebaseApprovalBridge.list(params) : api.get('/rest/approval-documents', { params })),
    detail: (atrzDocId) => (firebaseBackendEnabled() ? firebaseApprovalBridge.detail(atrzDocId) : api.get(`/rest/approval-documents/${atrzDocId}`)),
    create: (payload, files = []) => (
        firebaseBackendEnabled()
            ? firebaseApprovalBridge.create(payload, files)
            : api.post('/rest/approval-documents', toMultipartPayload(payload, files), {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })
    ),
    approve: (atrzDocId, data) => (firebaseBackendEnabled() ? firebaseApprovalBridge.approve(atrzDocId, data) : api.post(`/rest/approval-documents/${atrzDocId}/approve`, data)),
    reject: (atrzDocId, data) => (firebaseBackendEnabled() ? firebaseApprovalBridge.reject(atrzDocId, data) : api.post(`/rest/approval-documents/${atrzDocId}/reject`, data)),
    retract: (atrzDocId) => (firebaseBackendEnabled() ? firebaseApprovalBridge.retract(atrzDocId) : api.post(`/rest/approval-documents/${atrzDocId}/retract`)),
    customLines: () => (firebaseBackendEnabled() ? firebaseApprovalBridge.customLines() : api.get('/rest/approval-customline')),
    createCustomLine: (data) => (firebaseBackendEnabled() ? firebaseApprovalBridge.createCustomLine(data) : api.post('/rest/approval-customline', data)),
    deleteCustomLine: (name) => (firebaseBackendEnabled() ? firebaseApprovalBridge.deleteCustomLine(name) : api.delete(`/rest/approval-customline/${encodeURIComponent(name)}`)),
    vacationBalance: () => (firebaseBackendEnabled() ? firebaseApprovalBridge.vacationBalance() : api.get('/rest/approval-vacation/E101')),
    tempList: () => (firebaseBackendEnabled() ? firebaseApprovalBridge.tempList() : api.get('/rest/approval-temp')),
    tempDetail: (atrzTempSqn) => (firebaseBackendEnabled() ? firebaseApprovalBridge.tempDetail(atrzTempSqn) : api.get(`/rest/approval-temp/${atrzTempSqn}`)),
    saveTemp: (payload, files = []) => (
        firebaseBackendEnabled()
            ? firebaseApprovalBridge.saveTemp(payload, files)
            : api.post('/rest/approval-temp', toMultipartPayload(payload, files), {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })
    ),
    updateTemp: (atrzTempSqn, payload, files = []) => (
        firebaseBackendEnabled()
            ? firebaseApprovalBridge.updateTemp(atrzTempSqn, payload, files)
            : api.put(`/rest/approval-temp/${atrzTempSqn}`, toMultipartPayload(payload, files), {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            })
    ),
    deleteTemp: (atrzTempSqn) => (firebaseBackendEnabled() ? firebaseApprovalBridge.deleteTemp(atrzTempSqn) : api.delete(`/rest/approval-temp/${atrzTempSqn}`)),
};

export const contractAPI = {
    dashboard: () => (firebaseBackendEnabled() ? firebaseContractBridge.dashboard() : api.get('/rest/contracts/dashboard')),
    list: (params = {}) => (firebaseBackendEnabled() ? firebaseContractBridge.list(params) : api.get('/rest/contracts', { params })),
    detail: (contractId) => (firebaseBackendEnabled() ? firebaseContractBridge.detail(contractId) : api.get(`/rest/contracts/${contractId}`)),
    create: (payload) => (firebaseBackendEnabled() ? firebaseContractBridge.create(payload) : api.post('/rest/contracts', payload)),
    send: (contractId) => (firebaseBackendEnabled() ? firebaseContractBridge.send(contractId) : api.post(`/rest/contracts/${contractId}/send`)),
    cancel: (contractId, data = {}) => (firebaseBackendEnabled() ? firebaseContractBridge.cancel(contractId, data) : api.post(`/rest/contracts/${contractId}/cancel`, data)),
    remind: (contractId) => (firebaseBackendEnabled() ? firebaseContractBridge.remind(contractId) : api.post(`/rest/contracts/${contractId}/remind`)),
    links: (contractId) => (firebaseBackendEnabled() ? firebaseContractBridge.links(contractId) : api.get(`/rest/contracts/${contractId}/links`)),
    createBatch: (payload) => (firebaseBackendEnabled() ? firebaseContractBridge.createBatch(payload) : api.post('/rest/contracts/batches', payload)),
    batchDetail: (batchId) => (firebaseBackendEnabled() ? firebaseContractBridge.batchDetail(batchId) : api.get(`/rest/contracts/batches/${batchId}`)),
    templates: () => (firebaseBackendEnabled() ? firebaseContractBridge.templates() : api.get('/rest/contracts/templates')),
    templateDetail: (templateId) => (firebaseBackendEnabled() ? firebaseContractBridge.templateDetail(templateId) : api.get(`/rest/contracts/templates/${templateId}`)),
    createTemplate: (payload, files = {}) => {
        if (firebaseBackendEnabled()) {
            return firebaseContractBridge.createTemplate(payload, files);
        }
        if (files.sourceFile || files.backgroundFile) {
            return api.post('/rest/contracts/templates', toNamedMultipartPayload(payload, files), {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
        }
        return api.post('/rest/contracts/templates', payload);
    },
    updateTemplate: (templateId, payload, files = {}) => {
        if (firebaseBackendEnabled()) {
            return firebaseContractBridge.updateTemplate(templateId, payload, files);
        }
        if (files.sourceFile || files.backgroundFile) {
            return api.put(`/rest/contracts/templates/${templateId}`, toNamedMultipartPayload(payload, files), {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
        }
        return api.put(`/rest/contracts/templates/${templateId}`, payload);
    },
    publishTemplate: (templateId, templateVersionId) => (firebaseBackendEnabled() ? firebaseContractBridge.publishTemplate(templateId, templateVersionId) : api.post(`/rest/contracts/templates/${templateId}/publish`, templateVersionId ? { templateVersionId } : {})),
    templateRequests: () => (firebaseBackendEnabled() ? firebaseContractBridge.templateRequests() : api.get('/rest/contracts/template-requests')),
    createTemplateRequest: (payload, files = {}) => {
        if (firebaseBackendEnabled()) {
            return firebaseContractBridge.createTemplateRequest(payload, files);
        }
        if (files.sourceFile || files.markedFile || files.sealFile) {
            return api.post('/rest/contracts/template-requests', toNamedMultipartPayload(payload, files), {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
        }
        return api.post('/rest/contracts/template-requests', payload);
    },
    approveTemplateRequest: (requestId, data = {}) => (firebaseBackendEnabled() ? firebaseContractBridge.approveTemplateRequest(requestId, data) : api.post(`/rest/contracts/template-requests/${requestId}/approve`, data)),
    rejectTemplateRequest: (requestId, data = {}) => (firebaseBackendEnabled() ? firebaseContractBridge.rejectTemplateRequest(requestId, data) : api.post(`/rest/contracts/template-requests/${requestId}/reject`, data)),
    companySettings: () => (firebaseBackendEnabled() ? firebaseContractBridge.companySettings() : api.get('/rest/contracts/company-settings')),
    updateCompanySettings: (payload, files = {}) => {
        if (firebaseBackendEnabled()) {
            return firebaseContractBridge.updateCompanySettings(payload, files);
        }
        if (files.sealFile) {
            return api.put('/rest/contracts/company-settings', toNamedMultipartPayload(payload, files), {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
        }
        return api.put('/rest/contracts/company-settings', payload);
    },
    publicDetail: (token) => (firebaseBackendEnabled() ? firebaseContractBridge.publicDetail(token) : api.get(`/rest/contracts/public/${token}`)),
    publicClaim: (token, payload) => (firebaseBackendEnabled() ? firebaseContractBridge.publicClaim(token, payload) : api.post(`/rest/contracts/public/${token}/claim`, payload)),
    publicSubmit: (token, payload) => (firebaseBackendEnabled() ? firebaseContractBridge.publicSubmit(token, payload) : api.post(`/rest/contracts/public/${token}/submit`, payload)),
    publicDownload: (token) => (firebaseBackendEnabled() ? firebaseContractBridge.publicDownload(token) : api.get(`/rest/contracts/public/${token}/download`, { responseType: 'blob' })),
};

export const boardAPI = {
    notices: () => (firebaseBackendEnabled() ? firebaseBoardBridge.notices() : api.get('/rest/board-notice')),
    community: (bbsCtgrCd) => (firebaseBackendEnabled() ? firebaseBoardBridge.community(bbsCtgrCd) : api.get('/rest/board-community', { params: bbsCtgrCd ? { bbsCtgrCd } : {} })),
    categoryCounts: () => (firebaseBackendEnabled() ? firebaseBoardBridge.categoryCounts() : api.get('/rest/board-category-counts')),
    detail: (pstId) => (firebaseBackendEnabled() ? firebaseBoardBridge.detail(pstId) : api.get(`/rest/board/${pstId}`)),
    create: (data) => (firebaseBackendEnabled() ? firebaseBoardBridge.create(data) : api.post('/rest/board', data)),
    update: (pstId, data) => (firebaseBackendEnabled() ? firebaseBoardBridge.update(pstId, data) : api.put(`/rest/board/${pstId}`, data)),
    remove: (pstId) => (firebaseBackendEnabled() ? firebaseBoardBridge.remove(pstId) : api.delete(`/rest/board/${pstId}`)),
    incrementView: (pstId) => (firebaseBackendEnabled() ? firebaseBoardBridge.incrementView(pstId) : api.put(`/rest/board-vct/${pstId}`)),
    comments: (pstId) => (firebaseBackendEnabled() ? firebaseBoardBridge.comments(pstId) : api.get(`/rest/board-comment/${pstId}`)),
    createComment: (pstId, data) => (firebaseBackendEnabled() ? firebaseBoardBridge.createComment(pstId, data) : api.post(`/rest/board-comment/${pstId}`, data)),
    updateComment: (pstId, cmntSqn, data) => (firebaseBackendEnabled() ? firebaseBoardBridge.updateComment(pstId, cmntSqn, data) : api.put(`/rest/board-comment/${pstId}`, data, { params: { cmntSqn } })),
    deleteComment: (pstId, cmntSqn) => (firebaseBackendEnabled() ? firebaseBoardBridge.deleteComment(pstId, cmntSqn) : api.delete(`/rest/board-comment/${pstId}`, { params: { cmntSqn } })),
    workspace: (params = {}) => (firebaseBackendEnabled() ? firebaseBoardBridge.workspace(params) : api.get('/rest/boards', { params })),
    workspaceDetail: (pstId) => (firebaseBackendEnabled() ? firebaseBoardBridge.workspaceDetail(pstId) : api.get(`/rest/boards/${pstId}`)),
    createWorkspace: (payload, files = []) => (firebaseBackendEnabled() ? firebaseBoardBridge.createWorkspace(payload, files) : api.post('/rest/boards', toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    })),
    updateWorkspace: (pstId, payload, files = []) => (firebaseBackendEnabled() ? firebaseBoardBridge.updateWorkspace(pstId, payload, files) : api.put(`/rest/boards/${pstId}`, toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    })),
    toggleLikePost: (pstId) => (firebaseBackendEnabled() ? firebaseBoardBridge.toggleLikePost(pstId) : api.post(`/rest/boards/${pstId}/likes`)),
    likeUsers: (pstId) => (firebaseBackendEnabled() ? firebaseBoardBridge.likeUsers(pstId) : api.get(`/rest/boards/${pstId}/likes`)),
    readers: (pstId) => (firebaseBackendEnabled() ? firebaseBoardBridge.readers(pstId) : api.get(`/rest/boards/${pstId}/readers`)),
    share: (pstId, data) => (firebaseBackendEnabled() ? firebaseBoardBridge.share(pstId, data) : api.post(`/rest/boards/${pstId}/share`, data)),
    report: (pstId, data) => (firebaseBackendEnabled() ? firebaseBoardBridge.report(pstId, data) : api.post(`/rest/boards/${pstId}/report`, data)),
    pin: (pstId, fixedYn) => (firebaseBackendEnabled() ? firebaseBoardBridge.pin(pstId, fixedYn) : api.post(`/rest/boards/${pstId}/pin`, { fixedYn })),
    votePoll: (pstId, optionIds) => (firebaseBackendEnabled() ? firebaseBoardBridge.votePoll(pstId, optionIds) : api.post(`/rest/boards/${pstId}/poll/vote`, { optionIds })),
    updateTodoAssignee: (pstId, assigneeUserId, statusCd) => (firebaseBackendEnabled() ? firebaseBoardBridge.updateTodoAssignee(pstId, assigneeUserId, statusCd) : api.patch(`/rest/boards/${pstId}/todo-assignees/${assigneeUserId}`, { statusCd })),
    scheduleAvailability: (data) => (firebaseBackendEnabled() ? firebaseBoardBridge.scheduleAvailability(data) : api.post('/rest/boards/schedule/availability', data)),
    createCommentMultipart: (pstId, payload, files = [], upCmntSqn = '') => (firebaseBackendEnabled() ? firebaseBoardBridge.createCommentMultipart(pstId, payload, files, upCmntSqn) : api.post(`/rest/board-comment/${pstId}`, toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
        params: upCmntSqn ? { upCmntSqn } : {},
    })),
    updateCommentMultipart: (pstId, cmntSqn, payload, files = []) => (firebaseBackendEnabled() ? firebaseBoardBridge.updateCommentMultipart(pstId, cmntSqn, payload, files) : api.put(`/rest/board-comment/${pstId}`, toMultipartPayload(payload, files), {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
        params: { cmntSqn },
    })),
};

export const attendanceAPI = {
    today: (userId, workYmd = new Date().toISOString().slice(0, 10).replace(/-/g, '')) => (firebaseBackendEnabled() ? firebaseAttendanceBridge.today(userId, workYmd) : api.get(`/rest/attendance/${userId}/${workYmd}`)),
    history: (userId) => (firebaseBackendEnabled() ? firebaseAttendanceBridge.history(userId) : api.get(`/rest/attendance/${userId}`)),
    clockIn: () => (firebaseBackendEnabled() ? firebaseAttendanceBridge.clockIn() : api.post('/rest/attendance')),
    clockOut: (workYmd) => (firebaseBackendEnabled() ? firebaseAttendanceBridge.clockOut(workYmd) : api.put('/rest/attendance', { workYmd })),
    week: () => (firebaseBackendEnabled() ? firebaseAttendanceBridge.week() : api.get('/rest/attendance-stats/week')),
    month: () => (firebaseBackendEnabled() ? firebaseAttendanceBridge.month() : api.get('/rest/attendance-stats/month')),
    monthList: () => (firebaseBackendEnabled() ? firebaseAttendanceBridge.monthList() : api.get('/rest/attendance-stats/month-list')),
    depart: () => (firebaseBackendEnabled() ? firebaseAttendanceBridge.depart() : api.get('/rest/attendance-stats/depart')),
};

export const calendarAPI = {
    events: (params = {}) => (firebaseBackendEnabled() ? firebaseCalendarBridge.events(params) : api.get('/rest/calendar/events', { params })),
    user: () => api.get('/rest/calendar-user'),
    userDetail: (userSchdId) => api.get(`/rest/calendar-user/${userSchdId}`),
    createUser: (data) => (firebaseBackendEnabled() ? firebaseCalendarBridge.createUser(data) : api.post('/rest/calendar-user', data)),
    updateUser: (userSchdId, data) => (firebaseBackendEnabled() ? firebaseCalendarBridge.updateUser(userSchdId, data) : api.put(`/rest/calendar-user/${userSchdId}`, data)),
    deleteUser: (userSchdId) => (firebaseBackendEnabled() ? firebaseCalendarBridge.deleteUser(userSchdId) : api.delete(`/rest/calendar-user/${userSchdId}`)),
    dept: () => api.get('/rest/calendar-depart'),
    deptDetail: (deptSchdId) => api.get(`/rest/calendar-depart/${deptSchdId}`),
    createDept: (data) => (firebaseBackendEnabled() ? firebaseCalendarBridge.createDept(data) : api.post('/rest/calendar-depart', data)),
    updateDept: (deptSchdId, data) => (firebaseBackendEnabled() ? firebaseCalendarBridge.updateDept(deptSchdId, data) : api.put(`/rest/calendar-depart/${deptSchdId}`, data)),
    deleteDept: (deptSchdId) => (firebaseBackendEnabled() ? firebaseCalendarBridge.deleteDept(deptSchdId) : api.delete(`/rest/calendar-depart/${deptSchdId}`)),
    teamProjects: () => (firebaseBackendEnabled() ? firebaseCalendarBridge.teamProjects() : api.get('/rest/fullcalendar-team/project-list')),
};

export const emailAPI = {
    counts: () => (firebaseBackendEnabled() ? firebaseEmailBridge.counts() : api.get('/mail/counts')),
    list: (mailboxTypeCd, page = 1, searchWord = '') => (firebaseBackendEnabled() ? firebaseEmailBridge.list(mailboxTypeCd, page, searchWord) : api.get(`/mail/listData/${mailboxTypeCd}`, {
        params: {
            page,
            ...(searchWord ? { searchWord } : {}),
        },
    })),
    toggleImportance: (emailContId) => (firebaseBackendEnabled() ? firebaseEmailBridge.toggleImportance(emailContId) : api.post(`/mail/toggle-importance/${emailContId}`)),
    deleteSelected: (emailContIds, mailboxTypeCd) => (firebaseBackendEnabled() ? firebaseEmailBridge.deleteSelected(emailContIds, mailboxTypeCd) : api.post('/mail/deleteSelected', { emailContIds, mailboxTypeCd })),
    deleteAll: (mailboxTypeCd) => (firebaseBackendEnabled() ? firebaseEmailBridge.deleteAll(mailboxTypeCd) : api.post('/mail/deleteAll', { mailboxTypeCd })),
    restoreSelected: (emailContIds) => (firebaseBackendEnabled() ? firebaseEmailBridge.restoreSelected(emailContIds) : api.post('/mail/restoreSelected', { emailContIds })),
};

export const messengerAPI = {
    currentUser: () => (firebaseBackendEnabled() ? firebaseMessengerBridge.currentUser() : api.get('/chat/current-user')),
    users: () => (firebaseBackendEnabled() ? firebaseMessengerBridge.users() : api.get('/chat/users')),
    panel: () => (firebaseBackendEnabled() ? firebaseMessengerBridge.panel() : api.get('/chat/panel')),
    rooms: (params = {}) => (firebaseBackendEnabled() ? firebaseMessengerBridge.rooms(params) : api.get('/chat/rooms', { params })),
    roomDetail: (msgrId) => (firebaseBackendEnabled() ? firebaseMessengerBridge.roomDetail(msgrId) : api.get(`/chat/room/${msgrId}`)),
    messages: (msgrId) => (firebaseBackendEnabled() ? firebaseMessengerBridge.messages(msgrId) : api.get(`/chat/room/${msgrId}/messages`)),
    searchMessages: (msgrId, q) => (firebaseBackendEnabled() ? firebaseMessengerBridge.searchMessages(msgrId, q) : api.get(`/chat/room/${msgrId}/search`, { params: { q } })),
    findOrCreate: (userId) => (firebaseBackendEnabled() ? firebaseMessengerBridge.findOrCreate(userId) : api.get('/chat/room/findOrCreate', { params: { userId } })),
    selfRoom: () => (firebaseBackendEnabled() ? firebaseMessengerBridge.selfRoom() : api.post('/chat/room/self')),
    createRoom: (data) => (firebaseBackendEnabled() ? firebaseMessengerBridge.createRoom(data) : api.post('/chat/room/create', data)),
    send: (msgrId, contents, options = {}) => (firebaseBackendEnabled() ? firebaseMessengerBridge.send(msgrId, contents, options) : Promise.resolve(null)),
    invite: (msgrId, userIds) => (firebaseBackendEnabled() ? firebaseMessengerBridge.invite(msgrId, userIds) : api.post(`/chat/room/${msgrId}/invite`, { userIds })),
    kick: (msgrId, userId) => (firebaseBackendEnabled() ? firebaseMessengerBridge.kick(msgrId, userId) : api.post(`/chat/room/${msgrId}/kick`, { userId })),
    markAsRead: (msgrId) => (firebaseBackendEnabled() ? firebaseMessengerBridge.markAsRead(msgrId) : api.post(`/chat/room/markAsRead/${msgrId}`)),
    renameRoom: (msgrId, msgrNm) => (firebaseBackendEnabled() ? firebaseMessengerBridge.renameRoom(msgrId, msgrNm) : api.post(`/chat/room/${msgrId}/name`, { msgrNm })),
    participants: (msgrId) => (firebaseBackendEnabled() ? firebaseMessengerBridge.participants(msgrId) : api.get(`/chat/room/${msgrId}/participants`)),
    notify: (msgrId, notifyEnabled) => (firebaseBackendEnabled() ? firebaseMessengerBridge.notify(msgrId, notifyEnabled) : api.patch(`/chat/room/${msgrId}/notify`, { notifyEnabled })),
    pin: (msgrId, msgContId) => (firebaseBackendEnabled() ? firebaseMessengerBridge.pin(msgrId, msgContId) : api.patch(`/chat/room/${msgrId}/pin`, { msgContId })),
    clearPin: (msgrId) => (firebaseBackendEnabled() ? firebaseMessengerBridge.clearPin(msgrId) : api.patch(`/chat/room/${msgrId}/pin`, {})),
    leave: (msgrId) => (firebaseBackendEnabled() ? firebaseMessengerBridge.leave(msgrId) : api.post(`/chat/room/${msgrId}/leave`)),
    deleteMessage: (msgContId, msgrId) => (firebaseBackendEnabled() ? firebaseMessengerBridge.deleteMessage(msgContId, msgrId) : api.delete(`/chat/message/${msgContId}`, { params: { msgrId } })),
    forwardMessage: (msgContId, targetRoomId) => (firebaseBackendEnabled() ? firebaseMessengerBridge.forwardMessage(msgContId, targetRoomId) : api.post(`/chat/message/${msgContId}/forward`, { targetRoomId })),
    uploadFiles: (msgrId, contents, files = []) => {
        if (firebaseBackendEnabled()) {
            return firebaseMessengerBridge.uploadFiles(msgrId, contents, files);
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
    exportMessages: (msgrId) => (firebaseBackendEnabled() ? firebaseMessengerBridge.exportMessages(msgrId) : api.get(`/chat/room/${msgrId}/export.xlsx`, { responseType: 'blob' })),
};

export const communityAPI = {
    list: (params = {}) => (firebaseBackendEnabled() ? firebaseCommunityBridge.list(params) : api.get('/rest/communities', { params: typeof params === 'string' ? (params ? { q: params } : {}) : params })),
    search: (params = {}) => (firebaseBackendEnabled() ? firebaseCommunityBridge.search(params) : api.get('/rest/communities/search', { params: typeof params === 'string' ? (params ? { q: params } : {}) : params })),
    detail: (communityId) => (firebaseBackendEnabled() ? firebaseCommunityBridge.detail(communityId) : api.get(`/rest/communities/${communityId}`)),
    create: (payload, files = {}) => {
        if (firebaseBackendEnabled()) {
            return firebaseCommunityBridge.create(payload, files);
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
        if (firebaseBackendEnabled()) {
            return firebaseCommunityBridge.update(communityId, payload, files);
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
    remove: (communityId) => (firebaseBackendEnabled() ? firebaseCommunityBridge.remove(communityId) : api.delete(`/rest/communities/${communityId}`)),
    close: (communityId) => (firebaseBackendEnabled() ? firebaseCommunityBridge.close(communityId) : api.post(`/rest/communities/${communityId}/close`)),
    join: (communityId) => (firebaseBackendEnabled() ? firebaseCommunityBridge.join(communityId) : api.post(`/rest/communities/${communityId}/join`)),
    leave: (communityId) => (firebaseBackendEnabled() ? firebaseCommunityBridge.leave(communityId) : api.post(`/rest/communities/${communityId}/leave`)),
    members: (communityId, status = '') => (firebaseBackendEnabled() ? firebaseCommunityBridge.members(communityId, status) : api.get(`/rest/communities/${communityId}/members`, { params: status ? { status } : {} })),
    requests: (communityId) => (firebaseBackendEnabled() ? firebaseCommunityBridge.requests(communityId) : api.get(`/rest/communities/${communityId}/requests`)),
    addMembers: (communityId, userIds) => (firebaseBackendEnabled() ? firebaseCommunityBridge.addMembers(communityId, userIds) : api.post(`/rest/communities/${communityId}/members`, { userIds })),
    removeMember: (communityId, userId) => (firebaseBackendEnabled() ? firebaseCommunityBridge.removeMember(communityId, userId) : api.delete(`/rest/communities/${communityId}/members/${userId}`)),
    updateRole: (communityId, userId, roleCd) => (firebaseBackendEnabled() ? firebaseCommunityBridge.updateRole(communityId, userId, roleCd) : api.patch(`/rest/communities/${communityId}/members/${userId}/role`, { roleCd })),
    approveRequest: (communityId, userId) => (firebaseBackendEnabled() ? firebaseCommunityBridge.approveRequest(communityId, userId) : api.post(`/rest/communities/${communityId}/requests/${userId}/approve`)),
    rejectRequest: (communityId, userId) => (firebaseBackendEnabled() ? firebaseCommunityBridge.rejectRequest(communityId, userId) : api.post(`/rest/communities/${communityId}/requests/${userId}/reject`)),
    favorite: (communityId, favoriteYn) => (firebaseBackendEnabled() ? firebaseCommunityBridge.favorite(communityId, favoriteYn) : api.put(`/rest/communities/${communityId}/favorite`, { favoriteYn })),
    saveOrder: (communityIds) => (firebaseBackendEnabled() ? firebaseCommunityBridge.saveOrder(communityIds) : api.put('/rest/communities/order', { communityIds })),
    syncOrg: () => (firebaseBackendEnabled() ? firebaseCommunityBridge.syncOrg() : api.post('/rest/communities/sync-org')),
};

export const projectAPI = {
    list: () => (firebaseBackendEnabled() ? firebaseProjectBridge.list() : api.get('/rest/project')),
    detail: (bizId) => (firebaseBackendEnabled() ? firebaseProjectBridge.detail(bizId) : api.get(`/rest/project/${bizId}`)),
    create: (data) => (firebaseBackendEnabled() ? firebaseProjectBridge.create(data) : api.post('/rest/project', data)),
    update: (bizId, data) => (firebaseBackendEnabled() ? firebaseProjectBridge.update(bizId, data) : api.put(`/rest/project/${bizId}`, data)),
    setStatus: (bizId, bizSttsCd) => (firebaseBackendEnabled() ? firebaseProjectBridge.setStatus(bizId, bizSttsCd) : api.patch(`/rest/project/${bizId}/status`, { bizSttsCd })),
    members: (bizId) => api.get(`/rest/project/${bizId}/members`),
    tasks: (bizId) => (firebaseBackendEnabled() ? firebaseProjectBridge.tasks(bizId) : api.get(`/rest/project/${bizId}/tasks`)),
    createTask: (bizId, data) => (firebaseBackendEnabled() ? firebaseProjectBridge.createTask(bizId, data) : api.post(`/rest/project/${bizId}/tasks`, data)),
    updateTask: (taskId, data) => (firebaseBackendEnabled() ? firebaseProjectBridge.updateTask(taskId, data) : api.put(`/rest/project/tasks/${taskId}`, data)),
    setTaskStatus: (taskId, taskSttsCd) => (firebaseBackendEnabled() ? firebaseProjectBridge.setTaskStatus(taskId, taskSttsCd) : api.patch(`/rest/project/tasks/${taskId}/status`, { taskSttsCd })),
    deleteTask: (taskId) => (firebaseBackendEnabled() ? firebaseProjectBridge.deleteTask(taskId) : api.delete(`/rest/project/tasks/${taskId}`)),
};

export const meetingAPI = {
    rooms: () => (firebaseBackendEnabled() ? firebaseMeetingBridge.rooms() : api.get('/rest/meeting/room')),
    createRoom: (data) => (firebaseBackendEnabled() ? firebaseMeetingBridge.createRoom(data) : api.post('/rest/meeting/room', data)),
    reservations: (params) => (firebaseBackendEnabled() ? firebaseMeetingBridge.reservations(params) : api.get('/rest/meeting/reservations', { params })),
    detail: (reservationId) => (firebaseBackendEnabled() ? firebaseMeetingBridge.detail(reservationId) : api.get(`/rest/meeting/reservations/${reservationId}`)),
    createReservation: (data) => (firebaseBackendEnabled() ? firebaseMeetingBridge.createReservation(data) : api.post('/rest/meeting', data)),
    updateReservation: (data) => (firebaseBackendEnabled() ? firebaseMeetingBridge.updateReservation(data) : api.put('/rest/meeting', data)),
    deleteReservation: (reservationId) => (firebaseBackendEnabled() ? firebaseMeetingBridge.deleteReservation(reservationId) : api.delete(`/rest/meeting/${reservationId}`)),
};

export const myPageAPI = {
    profile: () => (firebaseBackendEnabled() ? firebaseCommonBridge.getMyProfile() : api.get('/rest/mypage')),
    updateProfile: (data) => (
        firebaseBackendEnabled()
            ? firebaseCommonBridge.updateProfile(data)
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
