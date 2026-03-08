import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { boardAPI, communityAPI, dashboardAPI, meetingAPI, usersAPI } from '../../services/api';

const CATEGORY_OPTIONS = [
    ['F101', '공지'],
    ['F104', '사내소식'],
    ['F102', '동호회'],
    ['F103', '경조사'],
    ['F105', '건의사항'],
    ['F106', '기타'],
];
const TYPE_OPTIONS = [['story', '이야기'], ['poll', '설문'], ['schedule', '일정'], ['todo', '할일']];
const SCOPE_OPTIONS = [
    ['all', '전체'],
    ['saved', '관심글'],
    ['mine', '내 글'],
    ['shared', '공유받은 글'],
    ['scheduled', '예약 글'],
    ['pinned', '상단공지'],
    ['poll', '설문'],
    ['schedule', '일정'],
    ['todo', '할일'],
];
const SEARCH_OPTIONS = [['all', '제목 + 내용'], ['title', '제목'], ['content', '내용'], ['writer', '작성자'], ['comment', '댓글']];
const VISIBILITY_OPTIONS = [['community', '커뮤니티'], ['private', '비공개']];
const SORT_OPTIONS = [['recent', '최신순'], ['popular', '조회순'], ['liked', '좋아요순'], ['deadline', '마감임박']];
const TODO_STATUS_OPTIONS = [['requested', '요청'], ['in_progress', '진행'], ['done', '완료']];
const REPEAT_OPTIONS = [['', '반복 안 함'], ['daily', '매일'], ['weekly', '매주'], ['monthly', '매월']];
const REMINDER_OPTIONS = [[10, '10분 전'], [15, '15분 전'], [30, '30분 전'], [60, '1시간 전'], [1440, '1일 전']];
const FILTER_IMPORTANCE_OPTIONS = [
    ['normal', '기본'],
    ['important', '중요'],
    ['urgent', '긴급'],
    ['notice', '일반공지'],
    ['congratulation', '경사'],
    ['condolence', '조사'],
    ['event', '이벤트'],
];
const IMPORTANCE_OPTIONS_BY_TYPE = {
    story: [['normal', '이야기'], ['important', '중요'], ['urgent', '긴급'], ['notice', '일반공지']],
    poll: [['normal', '설문'], ['important', '중요설문'], ['urgent', '긴급설문']],
    schedule: [['normal', '일정'], ['important', '중요일정'], ['urgent', '긴급일정'], ['congratulation', '경사'], ['condolence', '조사'], ['event', '이벤트']],
    todo: [['normal', '할일'], ['important', '중요할일'], ['urgent', '긴급할일']],
};
const ATTENDANCE_STATUS_LABELS = {
    invited: '초대됨',
    accepted: '참석',
    declined: '불참',
    tentative: '보류',
};

function emptyDraft() {
    return {
        bbsCtgrCd: 'F104',
        communityId: '',
        pstTtl: '',
        contents: '',
        pstTypeCd: 'story',
        visibilityCd: 'community',
        importanceCd: 'normal',
        linkUrl: '',
        fixedYn: 'N',
        reservedPublishDt: '',
        mentionUserIds: [],
        pollOptions: ['', ''],
        poll: { multipleYn: 'N', anonymousYn: 'N', resultOpenYn: 'Y', participantOpenYn: 'Y', deadlineDt: '' },
        schedule: {
            startDt: '',
            endDt: '',
            repeatRule: '',
            placeText: '',
            placeUrl: '',
            reminderMinutes: 30,
            videoMeetingYn: 'N',
            meetingRoomId: '',
            attendeeUserIds: [],
        },
        todo: { dueDt: '', assigneeUserIds: [] },
    };
}

const DEFAULT_FILTERS = { scope: 'all', q: '', searchType: 'all', type: 'all', communityId: 'all', importance: 'all', sort: 'recent', page: 1 };

const labelOf = (options, value, fallback = '-') => options.find(([key]) => key === value)?.[1] || fallback;
const toggle = (values, value) => values.includes(value) ? values.filter((item) => item !== value) : [...values, value];
const joinMeta = (...values) => values.filter(Boolean).join(' · ');
const getImportanceOptions = (type) => IMPORTANCE_OPTIONS_BY_TYPE[type] || IMPORTANCE_OPTIONS_BY_TYPE.story;
const getFilterImportanceOptions = (type) => type && type !== 'all' ? getImportanceOptions(type) : FILTER_IMPORTANCE_OPTIONS;
const sanitizeImportance = (type, importance) => {
    const options = getImportanceOptions(type);
    return options.some(([value]) => value === importance) ? importance : options[0][0];
};
const labelOfImportance = (type, importance) => labelOf(getImportanceOptions(type), importance, labelOf(FILTER_IMPORTANCE_OPTIONS, importance, '-'));
const toLocalInput = (value) => {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};
const formatDateTime = (value) => {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '-';
    return date.toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
};
const formatRelative = (value) => {
    if (!value) return '-';
    const diffMinutes = Math.round((Date.now() - new Date(value).getTime()) / 60000);
    const absMinutes = Math.max(Math.abs(diffMinutes), 1);
    if (absMinutes < 60) return `${absMinutes}분 ${diffMinutes >= 0 ? '전' : '후'}`;
    if (absMinutes < 1440) return `${Math.round(absMinutes / 60)}시간 ${diffMinutes >= 0 ? '전' : '후'}`;
    return `${Math.round(absMinutes / 1440)}일 ${diffMinutes >= 0 ? '전' : '후'}`;
};

const overlayTimestamp = (mode, item) => mode === 'likes' ? item?.crtDt : item?.readDt;

export default function BoardListPage() {
    const { user } = useAuth();
    const [searchParams, setSearchParams] = useSearchParams();
    const isAdmin = String(user?.userRole || '').includes('ADMIN');
    const [workspace, setWorkspace] = useState({ items: [], summary: {}, pinnedItems: [], closingPolls: [], todoItems: [], page: 1, totalPages: 1 });
    const [communities, setCommunities] = useState([]);
    const [users, setUsers] = useState([]);
    const [rooms, setRooms] = useState([]);
    const [filters, setFilters] = useState(() => ({
        ...DEFAULT_FILTERS,
        communityId: searchParams.get('communityId') || 'all',
        scope: searchParams.get('scope') || 'all',
    }));
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [detailPost, setDetailPost] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [composeOpen, setComposeOpen] = useState(false);
    const [editingPstId, setEditingPstId] = useState('');
    const [draft, setDraft] = useState(emptyDraft());
    const [draftFiles, setDraftFiles] = useState([]);
    const [availabilityItems, setAvailabilityItems] = useState([]);
    const [availabilityLoading, setAvailabilityLoading] = useState(false);
    const [commentText, setCommentText] = useState('');
    const [commentFiles, setCommentFiles] = useState([]);
    const [replyTargetId, setReplyTargetId] = useState('');
    const [shareOpen, setShareOpen] = useState(false);
    const [shareDraft, setShareDraft] = useState({ userIds: [], communityIds: [] });
    const [overlayMode, setOverlayMode] = useState('');
    const [reportReason, setReportReason] = useState('');
    const [selectedPollOptionIds, setSelectedPollOptionIds] = useState([]);
    const [likes, setLikes] = useState([]);
    const [readers, setReaders] = useState([]);
    const [composeIntentHandled, setComposeIntentHandled] = useState(false);
    const searchParamString = searchParams.toString();

    const filteredUsers = useMemo(() => users.filter((item) => item.userId !== user?.userId), [users, user?.userId]);
    const composeCategories = useMemo(() => (isAdmin ? CATEGORY_OPTIONS : CATEGORY_OPTIONS.filter(([value]) => value !== 'F101')), [isAdmin]);
    const composeImportanceOptions = useMemo(() => getImportanceOptions(draft.pstTypeCd), [draft.pstTypeCd]);
    const filterImportanceOptions = useMemo(() => getFilterImportanceOptions(filters.type), [filters.type]);
    const selectedCommunity = useMemo(
        () => communities.find((community) => String(community.communityId) === String(draft.communityId)),
        [communities, draft.communityId],
    );
    const canPinInCompose = isAdmin || Boolean(selectedCommunity?.manageable);

    useEffect(() => { void bootstrap(); }, []);
    // The workspace should refresh only when filters change.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    useEffect(() => { void loadWorkspace(); }, [filters]);
    useEffect(() => {
        const nextParams = new URLSearchParams(searchParams);
        if (filters.communityId && filters.communityId !== 'all') {
            nextParams.set('communityId', filters.communityId);
        } else {
            nextParams.delete('communityId');
        }
        if (filters.scope && filters.scope !== 'all') {
            nextParams.set('scope', filters.scope);
        } else {
            nextParams.delete('scope');
        }
        if (nextParams.toString() !== searchParamString) {
            setSearchParams(nextParams, { replace: true });
        }
    }, [filters.communityId, filters.scope, searchParams, searchParamString, setSearchParams]);
    useEffect(() => {
        if (composeIntentHandled || communities.length === 0 || searchParams.get('compose') !== '1') {
            return;
        }
        const nextCommunityId = searchParams.get('communityId') || '';
        const template = communities.find((community) => String(community.communityId) === String(nextCommunityId))?.postTemplateHtml || '';
        setEditingPstId('');
        setDraft({
            ...emptyDraft(),
            communityId: nextCommunityId || (filters.communityId !== 'all' ? filters.communityId : ''),
            contents: template,
        });
        setDraftFiles([]);
        setAvailabilityItems([]);
        setComposeOpen(true);
        const nextParams = new URLSearchParams(searchParams);
        nextParams.delete('compose');
        if (nextParams.toString() !== searchParamString) {
            setSearchParams(nextParams, { replace: true });
        }
        setComposeIntentHandled(true);
    }, [communities, composeIntentHandled, filters.communityId, searchParams, searchParamString, setSearchParams]);
    useEffect(() => {
        const postId = searchParams.get('postId');
        if (!postId || detailLoading || detailPost?.pstId === postId) {
            return;
        }
        void openDetail(postId);
    }, [detailLoading, detailPost?.pstId, searchParamString]);

    async function bootstrap() {
        setLoading(true);
        setError('');
        try {
            const [communityResponse, usersResponse, roomResponse] = await Promise.all([communityAPI.list({ view: 'joined' }), usersAPI.list(), meetingAPI.rooms()]);
            setCommunities(communityResponse.data || []);
            setUsers(usersResponse.data || []);
            setRooms(roomResponse.data || []);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '게시판 정보를 불러오지 못했습니다.');
        } finally {
            setLoading(false);
        }
    }

    async function loadWorkspace() {
        setLoading(true);
        setError('');
        try {
            setWorkspace((await boardAPI.workspace(filters)).data || { items: [], summary: {}, pinnedItems: [], closingPolls: [], todoItems: [], page: 1, totalPages: 1 });
        } catch (requestError) {
            setError(requestError.response?.data?.message || '게시물을 불러오지 못했습니다.');
        } finally {
            setLoading(false);
        }
    }

    async function openDetail(pstId) {
        if (!pstId) return;
        setDetailLoading(true);
        setError('');
        try {
            await Promise.allSettled([dashboardAPI.markRead(pstId), boardAPI.incrementView(pstId)]);
            const response = await boardAPI.workspaceDetail(pstId);
            setDetailPost(response.data);
            setSelectedPollOptionIds((response.data?.poll?.options || []).filter((item) => item.selected).map((item) => item.optionId));
            setCommentText('');
            setCommentFiles([]);
            setReplyTargetId('');
        } catch (requestError) {
            setError(requestError.response?.data?.message || '게시물 상세를 불러오지 못했습니다.');
        } finally {
            setDetailLoading(false);
        }
    }

    function closeCompose() {
        setComposeOpen(false);
        setEditingPstId('');
        setDraft(emptyDraft());
        setDraftFiles([]);
        setAvailabilityItems([]);
    }

    function closeDetail() {
        setDetailPost(null);
        setShareOpen(false);
        setOverlayMode('');
        setReportReason('');
        setLikes([]);
        setReaders([]);
        if (searchParams.get('postId')) {
            const nextParams = new URLSearchParams(searchParams);
            nextParams.delete('postId');
            setSearchParams(nextParams, { replace: true });
        }
    }

    function updateDraftCommunity(nextCommunityId) {
        setDraft((current) => {
            const previousTemplate = communities.find((community) => String(community.communityId) === String(current.communityId))?.postTemplateHtml || '';
            const nextTemplate = communities.find((community) => String(community.communityId) === String(nextCommunityId))?.postTemplateHtml || '';
            const shouldReplaceContents = !current.contents.trim() || (previousTemplate && current.contents === previousTemplate);
            return {
                ...current,
                communityId: nextCommunityId,
                contents: shouldReplaceContents ? nextTemplate : current.contents,
            };
        });
    }

    function updateDraftType(nextType) {
        setDraft((current) => ({
            ...current,
            pstTypeCd: nextType,
            importanceCd: sanitizeImportance(nextType, current.importanceCd),
        }));
    }

    function updateFilterType(nextType) {
        setFilters((current) => ({
            ...current,
            type: nextType,
            importance: nextType === 'all'
                ? current.importance
                : getImportanceOptions(nextType).some(([value]) => value === current.importance) ? current.importance : 'all',
            page: 1,
        }));
    }

    function openCreateModal(preferredCommunityId = '') {
        const nextCommunityId = preferredCommunityId || (filters.communityId !== 'all' ? filters.communityId : '');
        const template = communities.find((community) => String(community.communityId) === String(nextCommunityId))?.postTemplateHtml || '';
        setEditingPstId('');
        setDraft({
            ...emptyDraft(),
            communityId: nextCommunityId,
            contents: template,
        });
        setDraftFiles([]);
        setAvailabilityItems([]);
        setComposeOpen(true);
    }

    function openEditModal() {
        if (!detailPost) return;
        const nextPollOptions = (detailPost.poll?.options || []).map((item) => item.optionText);
        while (nextPollOptions.length < 2) nextPollOptions.push('');
        const nextType = detailPost.pstTypeCd || 'story';
        setEditingPstId(detailPost.pstId);
        setDraft({
            bbsCtgrCd: detailPost.bbsCtgrCd || 'F104',
            communityId: detailPost.communityId ? String(detailPost.communityId) : '',
            pstTtl: detailPost.pstTtl || '',
            contents: detailPost.contents || '',
            pstTypeCd: nextType,
            visibilityCd: detailPost.visibilityCd || 'community',
            importanceCd: sanitizeImportance(nextType, detailPost.importanceCd || 'normal'),
            linkUrl: detailPost.linkUrl || '',
            fixedYn: detailPost.fixedYn || 'N',
            reservedPublishDt: toLocalInput(detailPost.reservedPublishDt),
            mentionUserIds: (detailPost.mentions || []).map((item) => item.userId),
            pollOptions: nextPollOptions,
            poll: {
                multipleYn: detailPost.poll?.multipleYn || 'N',
                anonymousYn: detailPost.poll?.anonymousYn || 'N',
                resultOpenYn: detailPost.poll?.resultOpenYn || 'Y',
                participantOpenYn: detailPost.poll?.participantOpenYn || 'Y',
                deadlineDt: toLocalInput(detailPost.poll?.deadlineDt),
            },
            schedule: {
                startDt: toLocalInput(detailPost.schedule?.startDt),
                endDt: toLocalInput(detailPost.schedule?.endDt),
                repeatRule: detailPost.schedule?.repeatRule || '',
                placeText: detailPost.schedule?.placeText || '',
                placeUrl: detailPost.schedule?.placeUrl || '',
                reminderMinutes: detailPost.schedule?.reminderMinutes || 30,
                videoMeetingYn: detailPost.schedule?.videoMeetingYn || 'N',
                meetingRoomId: detailPost.schedule?.meetingRoomId || '',
                attendeeUserIds: (detailPost.schedule?.attendees || []).map((item) => item.userId),
            },
            todo: {
                dueDt: toLocalInput(detailPost.todo?.dueDt),
                assigneeUserIds: (detailPost.todo?.assignees || []).map((item) => item.userId),
            },
        });
        setDraftFiles([]);
        setAvailabilityItems([]);
        setComposeOpen(true);
    }

    function buildPayload() {
        const payload = {
            bbsCtgrCd: draft.bbsCtgrCd,
            communityId: draft.communityId ? Number(draft.communityId) : null,
            pstTtl: draft.pstTtl.trim(),
            contents: draft.contents.trim(),
            pstTypeCd: draft.pstTypeCd,
            visibilityCd: draft.visibilityCd,
            importanceCd: draft.importanceCd,
            linkUrl: draft.linkUrl || null,
            fixedYn: draft.fixedYn,
            reservedPublishDt: draft.reservedPublishDt ? new Date(draft.reservedPublishDt).toISOString() : null,
            mentionUserIds: draft.mentionUserIds,
        };

        if (draft.pstTypeCd === 'poll') {
            payload.poll = {
                ...draft.poll,
                deadlineDt: draft.poll.deadlineDt ? new Date(draft.poll.deadlineDt).toISOString() : null,
            };
            payload.pollOptions = draft.pollOptions
                .map((item) => item.trim())
                .filter(Boolean)
                .map((optionText) => ({ optionText }));
        }

        if (draft.pstTypeCd === 'schedule') {
            payload.schedule = {
                ...draft.schedule,
                startDt: draft.schedule.startDt ? new Date(draft.schedule.startDt).toISOString() : null,
                endDt: draft.schedule.endDt ? new Date(draft.schedule.endDt).toISOString() : null,
                meetingRoomId: draft.schedule.videoMeetingYn === 'Y' ? draft.schedule.meetingRoomId || null : null,
                attendees: draft.schedule.attendeeUserIds.map((userId) => ({ userId })),
            };
        }

        if (draft.pstTypeCd === 'todo') {
            payload.todo = {
                dueDt: draft.todo.dueDt ? new Date(draft.todo.dueDt).toISOString() : null,
                assignees: draft.todo.assigneeUserIds.map((userId) => ({ userId })),
            };
        }

        return payload;
    }

    async function submitCompose(event) {
        event.preventDefault();
        if (!draft.pstTtl.trim() || !draft.contents.trim()) return setError('제목과 내용을 입력해 주세요.');
        if (draft.pstTypeCd === 'poll' && draft.pollOptions.filter((item) => item.trim()).length < 2) return setError('설문 옵션은 최소 두 개 이상이어야 합니다.');
        if (draft.pstTypeCd === 'schedule' && (!draft.schedule.startDt || !draft.schedule.endDt)) return setError('일정의 시작일시와 종료일시를 입력해 주세요.');
        if (draft.pstTypeCd === 'schedule' && draft.schedule.videoMeetingYn === 'Y' && !draft.schedule.meetingRoomId) return setError('화상회의를 개설하려면 회의실을 선택해 주세요.');
        if (draft.pstTypeCd === 'todo' && draft.todo.assigneeUserIds.length === 0) return setError('할일 담당자를 한 명 이상 선택해 주세요.');

        setSaving(true);
        setError('');
        try {
            const payload = buildPayload();
            if (editingPstId) {
                await boardAPI.updateWorkspace(editingPstId, payload, draftFiles);
                await openDetail(editingPstId);
            } else {
                const response = await boardAPI.createWorkspace(payload, draftFiles);
                await openDetail(response.data?.board?.pstId);
            }
            closeCompose();
            await loadWorkspace();
        } catch (requestError) {
            setError(requestError.response?.data?.message || '게시물 저장에 실패했습니다.');
        } finally {
            setSaving(false);
        }
    }

    async function readAvailability() {
        if (!draft.schedule.startDt || !draft.schedule.endDt || draft.schedule.attendeeUserIds.length === 0) {
            setAvailabilityItems([]);
            return;
        }
        setAvailabilityLoading(true);
        setError('');
        try {
            const response = await boardAPI.scheduleAvailability({
                startDt: new Date(draft.schedule.startDt).toISOString(),
                endDt: new Date(draft.schedule.endDt).toISOString(),
                userIds: draft.schedule.attendeeUserIds,
            });
            setAvailabilityItems(response.data?.items || []);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '참석자 일정을 확인하지 못했습니다.');
        } finally {
            setAvailabilityLoading(false);
        }
    }

    async function toggleSave(post) {
        try {
            post.saved ? await dashboardAPI.unsavePost(post.pstId) : await dashboardAPI.savePost(post.pstId);
            detailPost?.pstId === post.pstId ? await openDetail(post.pstId) : await loadWorkspace();
        } catch (requestError) {
            setError(requestError.response?.data?.message || '관심글 상태를 변경하지 못했습니다.');
        }
    }

    async function toggleLike(post) {
        try {
            await boardAPI.toggleLikePost(post.pstId);
            detailPost?.pstId === post.pstId ? await openDetail(post.pstId) : await loadWorkspace();
        } catch (requestError) {
            setError(requestError.response?.data?.message || '좋아요 상태를 변경하지 못했습니다.');
        }
    }

    async function deletePost() {
        if (!detailPost || !window.confirm('게시물을 삭제할까요?')) return;
        try {
            await boardAPI.remove(detailPost.pstId);
            closeDetail();
            await loadWorkspace();
        } catch (requestError) {
            setError(requestError.response?.data?.message || '게시물을 삭제하지 못했습니다.');
        }
    }

    async function submitComment(event) {
        event.preventDefault();
        if (!detailPost || (!commentText.trim() && commentFiles.length === 0)) return;
        try {
            await boardAPI.createCommentMultipart(detailPost.pstId, { pstId: detailPost.pstId, contents: commentText.trim() }, commentFiles, replyTargetId);
            setCommentText('');
            setCommentFiles([]);
            setReplyTargetId('');
            await openDetail(detailPost.pstId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '댓글을 등록하지 못했습니다.');
        }
    }

    async function editComment(comment) {
        const nextValue = window.prompt('댓글을 수정해 주세요.', comment.contents || '');
        if (!nextValue || nextValue === comment.contents) return;
        try {
            await boardAPI.updateComment(detailPost.pstId, comment.cmntSqn, { contents: nextValue });
            await openDetail(detailPost.pstId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '댓글을 수정하지 못했습니다.');
        }
    }

    async function deleteComment(comment) {
        if (!window.confirm('댓글을 삭제할까요?')) return;
        try {
            await boardAPI.deleteComment(detailPost.pstId, comment.cmntSqn);
            await openDetail(detailPost.pstId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '댓글을 삭제하지 못했습니다.');
        }
    }

    async function submitShare() {
        if (!detailPost) return;
        if (shareDraft.userIds.length === 0 && shareDraft.communityIds.length === 0) return setError('공유 대상을 한 명 이상 선택해 주세요.');
        try {
            await boardAPI.share(detailPost.pstId, {
                userIds: shareDraft.userIds,
                communityIds: shareDraft.communityIds.map((communityId) => Number(communityId)),
            });
            setShareOpen(false);
            setShareDraft({ userIds: [], communityIds: [] });
            await openDetail(detailPost.pstId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '게시물을 공유하지 못했습니다.');
        }
    }

    async function submitReport() {
        if (!detailPost || !reportReason.trim()) return setError('신고 사유를 입력해 주세요.');
        try {
            await boardAPI.report(detailPost.pstId, { reasonText: reportReason.trim() });
            setOverlayMode('');
            setReportReason('');
        } catch (requestError) {
            setError(requestError.response?.data?.message || '게시물을 신고하지 못했습니다.');
        }
    }

    async function togglePin() {
        if (!detailPost) return;
        try {
            await boardAPI.pin(detailPost.pstId, detailPost.fixedYn === 'Y' ? 'N' : 'Y');
            await openDetail(detailPost.pstId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '상단공지 상태를 변경하지 못했습니다.');
        }
    }

    async function submitPollVote() {
        if (!detailPost || selectedPollOptionIds.length === 0) return setError('설문 항목을 선택해 주세요.');
        try {
            await boardAPI.votePoll(detailPost.pstId, selectedPollOptionIds);
            await openDetail(detailPost.pstId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '설문 투표에 실패했습니다.');
        }
    }

    async function updateTodoStatus(postId, assigneeUserId, statusCd) {
        try {
            await boardAPI.updateTodoAssignee(postId, assigneeUserId, statusCd);
            await openDetail(postId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '할일 상태를 변경하지 못했습니다.');
        }
    }

    async function openPeopleOverlay(mode) {
        if (!detailPost) return;
        try {
            const response = mode === 'likes' ? await boardAPI.likeUsers(detailPost.pstId) : await boardAPI.readers(detailPost.pstId);
            if (mode === 'likes') {
                setLikes(response.data || []);
            } else {
                setReaders(response.data || []);
            }
            setOverlayMode(mode);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '사용자 목록을 불러오지 못했습니다.');
        }
    }

    const widgetCards = [
        ['상단공지', '중요한 게시물을 가장 먼저 확인하세요.', workspace.pinnedItems || []],
        ['마감 임박 설문', '마감 전에 의견을 남겨 주세요.', workspace.closingPolls || []],
        ['내 할일', '내게 배정된 할일만 모아 봅니다.', workspace.todoItems || []],
    ];

    function renderMainLayout() {
        return (
            <>
                <div className="page-header">
                    <div>
                        <h2>게시판</h2>
                        <p>이야기, 설문, 일정, 할일을 한곳에서 관리하세요.</p>
                    </div>
                    <div className="page-header-actions">
                        <button type="button" className="btn btn-primary" onClick={openCreateModal}>글쓰기</button>
                    </div>
                </div>

                {error && (
                    <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
                        <div className="card-body" style={{ color: 'var(--danger)' }}>{error}</div>
                    </div>
                )}

                <div className="board-shell">
                    <aside className="board-sidebar-left">
                        <div className="board-sidebar-card">
                            <h3>피드 개요</h3>
                            <p style={{ margin: 0, fontSize: '11px', color: 'var(--gray-500)' }}>결재/공지 위주로 정리된 요약 패널입니다.</p>
                        </div>

                        <div className="board-sidebar-card">
                            <h3>피드 분류</h3>
                            <div className="board-filter-stack">
                                {SCOPE_OPTIONS.map(([value, label]) => (
                                    <button
                                        key={value}
                                        type="button"
                                        className={`board-toolbar-button ${filters.scope === value ? 'active' : ''}`}
                                        style={{ textAlign: 'left', margin: 0 }}
                                        onClick={() => setFilters((current) => ({ ...current, scope: value, page: 1 }))}
                                    >
                                        {label}
                                    </button>
                                ))}
                            </div>
                        </div>

                        <div className="board-sidebar-card">
                            <h3>검색 필터</h3>
                            <div className="board-filter-stack">
                                <input
                                    className="form-input"
                                    type="search"
                                    placeholder="검색어 입력"
                                    value={filters.q}
                                    onChange={(event) => setFilters((current) => ({ ...current, q: event.target.value, page: 1 }))}
                                />
                                <select className="form-input" value={filters.searchType} onChange={(event) => setFilters((current) => ({ ...current, searchType: event.target.value, page: 1 }))}>
                                    {SEARCH_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                                </select>
                                <select className="form-input" value={filters.type} onChange={(event) => updateFilterType(event.target.value)}>
                                    <option value="all">유형: 전체보기</option>
                                    {TYPE_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                                </select>
                                <select className="form-input" value={filters.communityId} onChange={(event) => setFilters((current) => ({ ...current, communityId: event.target.value, page: 1 }))}>
                                    <option value="all">커뮤니티: 전체</option>
                                    {communities.map((community) => <option key={community.communityId} value={community.communityId}>{community.communityNm}</option>)}
                                </select>
                                <select className="form-input" value={filters.importance} onChange={(event) => setFilters((current) => ({ ...current, importance: event.target.value, page: 1 }))}>
                                    <option value="all">상태: 전체</option>
                                    {filterImportanceOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                                </select>
                                <select className="form-input" value={filters.sort} onChange={(event) => setFilters((current) => ({ ...current, sort: event.target.value, page: 1 }))}>
                                    {SORT_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                                </select>
                            </div>
                        </div>
                    </aside>

                    <main className="board-main">
                        <section className="board-main-toolbar">
                            <div className="board-toolbar-actions">
                                {SORT_OPTIONS.map(([value, label]) => (
                                    <button
                                        key={value}
                                        type="button"
                                        className={`board-toolbar-button ${filters.sort === value ? 'active' : ''}`}
                                        onClick={() => setFilters((current) => ({ ...current, sort: value, page: 1 }))}
                                    >
                                        {label}
                                    </button>
                                ))}
                            </div>
                            <div className="board-card-stats">
                                <span>Total: {workspace.summary?.totalCount || workspace.items.length || 0}건</span>
                            </div>
                        </section>

                        <section className="board-list-section">
                            {loading ? (
                                <div className="feed-empty">게시물을 불러오는 중입니다.</div>
                            ) : workspace.items.length === 0 ? (
                                <div className="feed-empty">조건에 맞는 게시물이 없습니다.</div>
                            ) : (
                                <div className="board-card-list">
                                    {workspace.items.map((post) => (
                                        <button key={post.pstId} type="button" className={`board-card ${post.fixedYn === 'Y' ? 'pinned' : ''}`} onClick={() => openDetail(post.pstId)}>
                                            <div className="board-card-row1">
                                                {post.fixedYn === 'Y' && <span className="board-badge red">상단공지</span>}
                                                <span className="board-badge blue">{labelOf(CATEGORY_OPTIONS, post.bbsCtgrCd, post.bbsCtgrCd)}</span>
                                                <h4 className="board-card-title">{post.pstTtl}</h4>
                                                {post.visibilityCd === 'private' && <span className="board-badge gray">비공개</span>}
                                                <span className="board-badge gray" style={{ fontWeight: 400 }}>{labelOfImportance(post.pstTypeCd, post.importanceCd)}</span>
                                            </div>
                                            <div className="board-card-row2">
                                                <div className="board-card-meta">
                                                    <span className="board-card-author">{post.userNm || post.crtUserId} <small>{joinMeta(post.communityNm, post.deptNm)}</small></span>
                                                    <span>{formatRelative(post.publishedDt || post.frstCrtDt)}</span>
                                                </div>
                                                <div className="board-card-stats">
                                                    {post.saved && <span style={{ color: 'var(--primary-600)' }}>★ 관심</span>}
                                                    <span>💬 {post.commentCount || 0}</span>
                                                    <span>❤️ {post.likeCount || 0}</span>
                                                    <span>👁️ {post.readerCount || post.viewCnt || 0}</span>
                                                </div>
                                            </div>
                                        </button>
                                    ))}
                                </div>
                            )}
                        </section>

                        <div className="feed-pagination">
                            <button type="button" className="btn btn-outline" disabled={(workspace.page || 1) <= 1} onClick={() => setFilters((current) => ({ ...current, page: Math.max(1, current.page - 1) }))}>이전</button>
                            <span>{workspace.page || 1} / {workspace.totalPages || 1}</span>
                            <button type="button" className="btn btn-outline" disabled={(workspace.page || 1) >= (workspace.totalPages || 1)} onClick={() => setFilters((current) => ({ ...current, page: Math.min(workspace.totalPages || 1, current.page + 1) }))}>다음</button>
                        </div>
                    </main>

                    <aside className="board-sidebar-right">
                        {widgetCards.map(([title, , items]) => (
                            <div key={title} className="board-widget-card">
                                <div className="board-widget-header">
                                    <h3>{title}</h3>
                                </div>
                                <div className="board-widget-list">
                                    {items.length === 0 ? <div style={{ padding: '12px', fontSize: '11px', color: 'var(--gray-400)', textAlign: 'center' }}>정보가 없습니다.</div> : items.map((item) => (
                                        <button key={item.pstId} type="button" className="board-widget-item" onClick={() => openDetail(item.pstId)}>
                                            <h4 className="board-widget-title">{item.pstTtl}</h4>
                                            <div className="board-widget-meta">
                                                <span>{joinMeta(item.communityNm, labelOf(TYPE_OPTIONS, item.pstTypeCd, '이야기'))}</span>
                                                <small>{formatDateTime(item.publishedDt || item.frstCrtDt)}</small>
                                            </div>
                                        </button>
                                    ))}
                                </div>
                            </div>
                        ))}
                    </aside>
                </div>
            </>
        );
    }

    function renderComposeModal() {
        if (!composeOpen) return null;
        return (
            <div className="modal-overlay" onClick={closeCompose}>
                <div className="modal profile-sheet" onClick={(event) => event.stopPropagation()} style={{ maxWidth: '720px' }}>
                    <div className="modal-header">
                        <h3>{editingPstId ? '게시물 수정' : '새 게시물'}</h3>
                        <div className="board-toolbar-actions">
                            <button type="button" className="board-toolbar-button" onClick={closeCompose}>닫기</button>
                            <button type="button" className="btn btn-primary" style={{ padding: '6px 14px', height: 'auto' }} onClick={submitCompose} disabled={saving}>{saving ? '저장 중...' : editingPstId ? '변경사항 저장' : '게시하기'}</button>
                        </div>
                    </div>
                    <div className="modal-body" style={{ padding: 0 }}>
                        <form onSubmit={submitCompose}>
                            <div className="board-modal-section">
                                <div className="board-section-title">기본 정보</div>
                                <div className="board-box-row">
                                    <input className="form-input" style={{ fontSize: '15px', fontWeight: 'bold' }} type="text" placeholder="제목을 입력하세요" value={draft.pstTtl} onChange={(event) => setDraft((current) => ({ ...current, pstTtl: event.target.value }))} />
                                </div>
                                <div className="feed-chip-list" style={{ marginTop: 0 }}>
                                    {TYPE_OPTIONS.map(([value, label]) => (
                                        <button key={value} type="button" className={`board-toolbar-button ${draft.pstTypeCd === value ? 'active' : ''}`} onClick={() => updateDraftType(value)}>
                                            {label}
                                        </button>
                                    ))}
                                </div>
                                <div className="profile-side-grid">
                                    <select className="form-input" value={draft.bbsCtgrCd} onChange={(event) => setDraft((current) => ({ ...current, bbsCtgrCd: event.target.value }))}>
                                        {composeCategories.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                                    </select>
                                    <select className="form-input" value={draft.communityId} onChange={(event) => updateDraftCommunity(event.target.value)}>
                                        <option value="">전체 공개</option>
                                        {communities.map((community) => <option key={community.communityId} value={community.communityId}>{community.communityNm}</option>)}
                                    </select>
                                    <select className="form-input" value={draft.visibilityCd} onChange={(event) => setDraft((current) => ({ ...current, visibilityCd: event.target.value }))}>
                                        {VISIBILITY_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                                    </select>
                                    <select className="form-input" value={draft.importanceCd} onChange={(event) => setDraft((current) => ({ ...current, importanceCd: event.target.value }))}>
                                        {composeImportanceOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                                    </select>
                                </div>
                            </div>

                            <div className="board-modal-section">
                                <div className="board-section-title">본문 내용</div>
                                <textarea className="form-input" style={{ minHeight: '240px', resize: 'vertical' }} placeholder="내용을 입력하세요" value={draft.contents} onChange={(event) => setDraft((current) => ({ ...current, contents: event.target.value }))} />
                                <input className="form-input" type="file" multiple onChange={(event) => setDraftFiles(Array.from(event.target.files || []))} />
                            </div>

                            {(draft.pstTypeCd === 'poll' || draft.pstTypeCd === 'schedule' || draft.pstTypeCd === 'todo') && (
                                <div className="board-modal-section">
                                    <div className="board-section-title">{labelOf(TYPE_OPTIONS, draft.pstTypeCd, '옵션')} 설정</div>
                                    {draft.pstTypeCd === 'poll' && (
                                        <div className="feed-composer-form">
                                            {draft.pollOptions.map((option, index) => (
                                                <input
                                                    key={`poll-${index}`}
                                                    className="form-input"
                                                    type="text"
                                                    placeholder={`옵션 ${index + 1}`}
                                                    value={option}
                                                    onChange={(event) => setDraft((current) => {
                                                        const next = [...current.pollOptions];
                                                        next[index] = event.target.value;
                                                        return { ...current, pollOptions: next };
                                                    })}
                                                />
                                            ))}
                                            <div className="feed-action-row compact">
                                                <button type="button" className="board-action-btn" onClick={() => setDraft((current) => ({ ...current, pollOptions: [...current.pollOptions, ''] }))}>+ 옵션 추가</button>
                                            </div>
                                            <div className="profile-side-grid" style={{ marginTop: '8px' }}>
                                                <input title="마감일" className="form-input" type="datetime-local" value={draft.poll.deadlineDt} onChange={(event) => setDraft((current) => ({ ...current, poll: { ...current.poll, deadlineDt: event.target.value } }))} />
                                            </div>
                                            <div className="feed-action-row compact" style={{ marginTop: '8px' }}>
                                                <label className="feed-checkbox"><input type="checkbox" checked={draft.poll.multipleYn === 'Y'} onChange={(event) => setDraft((current) => ({ ...current, poll: { ...current.poll, multipleYn: event.target.checked ? 'Y' : 'N' } }))} />중복 허용</label>
                                                <label className="feed-checkbox"><input type="checkbox" checked={draft.poll.anonymousYn === 'Y'} onChange={(event) => setDraft((current) => ({ ...current, poll: { ...current.poll, anonymousYn: event.target.checked ? 'Y' : 'N' } }))} />익명 투표</label>
                                                <label className="feed-checkbox"><input type="checkbox" checked={draft.poll.resultOpenYn === 'Y'} onChange={(event) => setDraft((current) => ({ ...current, poll: { ...current.poll, resultOpenYn: event.target.checked ? 'Y' : 'N', participantOpenYn: event.target.checked ? current.poll.participantOpenYn : 'N' } }))} />결과 공개</label>
                                                <label className="feed-checkbox"><input type="checkbox" checked={draft.poll.participantOpenYn === 'Y'} disabled={draft.poll.resultOpenYn !== 'Y'} onChange={(event) => setDraft((current) => ({ ...current, poll: { ...current.poll, participantOpenYn: event.target.checked ? 'Y' : 'N' } }))} />참여자 공개</label>
                                            </div>
                                        </div>
                                    )}
                                    {draft.pstTypeCd === 'schedule' && (
                                        <div className="feed-composer-form">
                                            <div className="profile-side-grid">
                                                <input className="form-input" type="datetime-local" title="시작일시" value={draft.schedule.startDt} onChange={(event) => setDraft((current) => ({ ...current, schedule: { ...current.schedule, startDt: event.target.value } }))} />
                                                <input className="form-input" type="datetime-local" title="종료일시" value={draft.schedule.endDt} onChange={(event) => setDraft((current) => ({ ...current, schedule: { ...current.schedule, endDt: event.target.value } }))} />
                                            </div>
                                            <div className="profile-side-grid">
                                                <select className="form-input" value={draft.schedule.repeatRule} onChange={(event) => setDraft((current) => ({ ...current, schedule: { ...current.schedule, repeatRule: event.target.value } }))}>
                                                    {REPEAT_OPTIONS.map(([value, label]) => <option key={value || 'none'} value={value}>{label}</option>)}
                                                </select>
                                                <select className="form-input" value={draft.schedule.reminderMinutes} onChange={(event) => setDraft((current) => ({ ...current, schedule: { ...current.schedule, reminderMinutes: Number(event.target.value) } }))}>
                                                    {REMINDER_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                                                </select>
                                            </div>
                                            <div className="profile-side-grid">
                                                <input className="form-input" type="text" placeholder="장소 입력" value={draft.schedule.placeText} onChange={(event) => setDraft((current) => ({ ...current, schedule: { ...current.schedule, placeText: event.target.value } }))} />
                                                <input className="form-input" type="url" placeholder="지도 링크 URL" value={draft.schedule.placeUrl} onChange={(event) => setDraft((current) => ({ ...current, schedule: { ...current.schedule, placeUrl: event.target.value } }))} />
                                            </div>
                                            <label className="feed-checkbox"><input type="checkbox" checked={draft.schedule.videoMeetingYn === 'Y'} onChange={(event) => setDraft((current) => ({ ...current, schedule: { ...current.schedule, videoMeetingYn: event.target.checked ? 'Y' : 'N', meetingRoomId: event.target.checked ? current.schedule.meetingRoomId : '' } }))} />화상회의 생성</label>
                                            <select className="form-input" value={draft.schedule.meetingRoomId} disabled={draft.schedule.videoMeetingYn !== 'Y'} onChange={(event) => setDraft((current) => ({ ...current, schedule: { ...current.schedule, meetingRoomId: event.target.value } }))}>
                                                <option value="">회의실 선택</option>
                                                {rooms.map((room) => <option key={room.roomId} value={room.roomId}>{room.roomName || room.roomNm || room.roomId}</option>)}
                                            </select>
                                            <div className="feed-chip-list" style={{ marginTop: '8px' }}>
                                                {filteredUsers.map((person) => (
                                                    <button key={person.userId} type="button" className={`board-toolbar-button ${draft.schedule.attendeeUserIds.includes(person.userId) ? 'active' : ''}`} onClick={() => setDraft((current) => ({ ...current, schedule: { ...current.schedule, attendeeUserIds: toggle(current.schedule.attendeeUserIds, person.userId) } }))}>
                                                        {person.userNm || person.userId}
                                                    </button>
                                                ))}
                                            </div>
                                            <button type="button" className="btn btn-outline" style={{ marginTop: '8px' }} onClick={readAvailability} disabled={availabilityLoading}>{availabilityLoading ? '확인 중...' : '선택된 참석자 일정 확인'}</button>
                                            <div className="board-filter-stack" style={{ marginTop: '8px' }}>
                                                {availabilityItems.map((item) => (
                                                    <div key={item.userId} className="board-card-row2">
                                                        <span className="board-card-author">{item.userNm || item.userId} <small>{item.deptNm || '-'}</small></span>
                                                        <span className={`board-badge ${item.busy ? 'red' : 'green'}`}>{item.busy ? '일정 있음' : '참석 가능'}</span>
                                                    </div>
                                                ))}
                                            </div>
                                        </div>
                                    )}
                                    {draft.pstTypeCd === 'todo' && (
                                        <div className="feed-composer-form">
                                            <input className="form-input" type="datetime-local" title="마감일시" value={draft.todo.dueDt} onChange={(event) => setDraft((current) => ({ ...current, todo: { ...current.todo, dueDt: event.target.value } }))} />
                                            <div className="feed-chip-list">
                                                {filteredUsers.map((person) => (
                                                    <button key={person.userId} type="button" className={`board-toolbar-button ${draft.todo.assigneeUserIds.includes(person.userId) ? 'active' : ''}`} onClick={() => setDraft((current) => ({ ...current, todo: { ...current.todo, assigneeUserIds: toggle(current.todo.assigneeUserIds, person.userId) } }))}>
                                                        {person.userNm || person.userId}
                                                    </button>
                                                ))}
                                            </div>
                                        </div>
                                    )}
                                </div>
                            )}

                            <div className="board-modal-section">
                                <div className="board-section-title">추가 설정</div>
                                <div className="profile-side-grid">
                                    <input className="form-input" type="url" placeholder="참고 웹 링크 (선택)" value={draft.linkUrl} onChange={(event) => setDraft((current) => ({ ...current, linkUrl: event.target.value }))} />
                                    <input className="form-input" type="datetime-local" title="예약 게시 일시" value={draft.reservedPublishDt} onChange={(event) => setDraft((current) => ({ ...current, reservedPublishDt: event.target.value }))} />
                                </div>
                                {canPinInCompose ? (
                                    <label className="feed-checkbox" style={{ marginTop: '8px' }}>
                                        <input type="checkbox" checked={draft.fixedYn === 'Y'} onChange={(event) => setDraft((current) => ({ ...current, fixedYn: event.target.checked ? 'Y' : 'N' }))} />
                                        상단공지로 고정
                                    </label>
                                ) : (
                                    <small style={{ color: 'var(--gray-500)', marginTop: '8px', display: 'block' }}>* 상단공지는 커뮤니티 관리자만 설정할 수 있습니다.</small>
                                )}
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        );
    }

    function renderDetailModal() {
        if (!detailPost) return null;
        return (
            <div className="modal-overlay" onClick={closeDetail}>
                <div className="modal" style={{ maxWidth: '820px' }} onClick={(event) => event.stopPropagation()}>
                    <div className="modal-body" style={{ padding: '30px' }}>
                        {detailLoading ? <div className="feed-empty">상세 정보를 불러오는 중입니다.</div> : (
                            <div className="board-detail-container">
                                <div className="board-detail-header">
                                    <div className="board-detail-title-row">
                                        <span className="board-badge blue">{labelOf(CATEGORY_OPTIONS, detailPost.bbsCtgrCd, detailPost.bbsCtgrCd)}</span>
                                        <h2 className="board-detail-title">{detailPost.pstTtl}</h2>
                                        {detailPost.fixedYn === 'Y' && <span className="board-badge red">상단공지</span>}
                                        <span className="board-badge gray">{labelOfImportance(detailPost.pstTypeCd, detailPost.importanceCd)}</span>
                                    </div>
                                    <div className="board-detail-meta-row">
                                        <div className="board-detail-author-info">
                                            <strong>{detailPost.userNm || detailPost.crtUserId}</strong>
                                            <span>{detailPost.communityNm || '전체 공개'}</span>
                                            <span>{formatDateTime(detailPost.publishedDt || detailPost.frstCrtDt)}</span>
                                        </div>
                                        <div className="board-detail-actions">
                                            <button type="button" className={`board-action-btn ${detailPost.saved ? 'active' : ''}`} onClick={() => toggleSave(detailPost)}>{detailPost.saved ? '★ 관심글' : '☆ 관심글'}</button>
                                            <button type="button" className={`board-action-btn ${detailPost.liked ? 'active' : ''}`} onClick={() => toggleLike(detailPost)}>{detailPost.liked ? '❤️ 좋아요' : '🤍 좋아요'}</button>
                                            <button type="button" className="board-action-btn" onClick={() => openPeopleOverlay('likes')}>{detailPost.likeCount || 0}</button>
                                            <button type="button" className="board-action-btn" onClick={() => openPeopleOverlay('readers')}>읽은 사람 {detailPost.readerCount || 0}</button>
                                        </div>
                                    </div>
                                    <div className="board-detail-actions" style={{ marginTop: '12px', justifyContent: 'flex-end' }}>
                                        {detailPost.shareable && <button type="button" className="board-action-btn" onClick={() => { setShareDraft({ userIds: [], communityIds: [] }); setShareOpen(true); }}>공유</button>}
                                        <button type="button" className="board-action-btn" onClick={() => setOverlayMode('report')}>신고</button>
                                        {detailPost.pinnable && <button type="button" className="board-action-btn" onClick={togglePin}>{detailPost.fixedYn === 'Y' ? '상단공지 해제' : '상단공지 고정'}</button>}
                                        {detailPost.manageable && (
                                            <>
                                                <button type="button" className="board-action-btn" onClick={openEditModal}>수정</button>
                                                <button type="button" className="board-action-btn" style={{ color: 'var(--danger)', borderColor: 'var(--danger-200)' }} onClick={deletePost}>삭제</button>
                                            </>
                                        )}
                                    </div>
                                </div>

                                <div className="board-detail-content" dangerouslySetInnerHTML={{ __html: detailPost.contents || '' }} />

                                {detailPost.linkUrl && (
                                    <div className="board-box">
                                        <div className="board-box-title">참고 링크</div>
                                        <div className="board-box-row">
                                            <span className="board-box-value"><a href={detailPost.linkUrl} target="_blank" rel="noreferrer" style={{ color: 'var(--primary-600)', textDecoration: 'underline' }}>{detailPost.linkUrl}</a></span>
                                        </div>
                                    </div>
                                )}

                                {(detailPost.attachments || []).length > 0 && (
                                    <div className="board-box">
                                        <div className="board-box-title">첨부파일 ({(detailPost.attachments || []).length}개)</div>
                                        <div className="board-filter-stack">
                                            {detailPost.attachments.map((file) => (
                                                <div key={`${file.fileId}-${file.fileSeq}`} className="board-box-row" style={{ alignItems: 'center' }}>
                                                    <span className="board-box-label" style={{ width: '40px' }}><small>{Math.round((file.fileSize || 0) / 1024)} KB</small></span>
                                                    <a className="board-box-value" href={file.filePath} target="_blank" rel="noreferrer" style={{ fontWeight: 500 }}>{file.orgnFileNm || file.saveFileNm}</a>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {detailPost.pstTypeCd === 'poll' && detailPost.poll && (
                                    <div className="board-box">
                                        <div className="board-box-title" style={{ display: 'flex', justifyContent: 'space-between' }}>
                                            <span>설문 투표</span>
                                            <span style={{ fontWeight: 400, color: 'var(--gray-500)' }}>{detailPost.poll.deadlineDt ? `마감: ${formatDateTime(detailPost.poll.deadlineDt)}` : '마감 없음'}</span>
                                        </div>
                                        <div className="board-filter-stack">
                                            {(detailPost.poll.options || []).map((option) => (
                                                <label key={option.optionId} className="board-box-row" style={{ cursor: 'pointer', alignItems: 'center', background: '#fff', padding: '8px 12px', border: '1px solid var(--gray-200)', borderRadius: '6px' }}>
                                                    <span className="board-box-label" style={{ width: 'auto', marginRight: '8px' }}>
                                                        <input type={detailPost.poll.multipleYn === 'Y' ? 'checkbox' : 'radio'} checked={selectedPollOptionIds.includes(option.optionId)} disabled={detailPost.poll.closed} onChange={() => setSelectedPollOptionIds((current) => detailPost.poll.multipleYn === 'Y' ? (current.includes(option.optionId) ? current.filter((value) => value !== option.optionId) : [...current, option.optionId]) : [option.optionId])} />
                                                    </span>
                                                    <span className="board-box-value"><strong>{option.optionText}</strong></span>
                                                    <span className="board-badge gray">{option.voteCount || 0}표</span>
                                                </label>
                                            ))}
                                        </div>
                                        <div style={{ marginTop: '12px', textAlign: 'right' }}>
                                            <button type="button" className="btn btn-primary" onClick={submitPollVote} disabled={detailPost.poll.closed}>{detailPost.poll.closed ? '마감됨' : '투표 제출하기'}</button>
                                        </div>
                                    </div>
                                )}

                                {detailPost.pstTypeCd === 'schedule' && detailPost.schedule && (
                                    <div className="board-box">
                                        <div className="board-box-title">일정 정보</div>
                                        <div className="board-box-row"><span className="board-box-label">일시</span><span className="board-box-value">{`${formatDateTime(detailPost.schedule.startDt)} ~ ${formatDateTime(detailPost.schedule.endDt)}`}</span></div>
                                        {detailPost.schedule.repeatRule && <div className="board-box-row"><span className="board-box-label">반복/알림</span><span className="board-box-value">{labelOf(REPEAT_OPTIONS, detailPost.schedule.repeatRule, detailPost.schedule.repeatRule)} / {detailPost.schedule.reminderMinutes || 30}분 전</span></div>}
                                        {detailPost.schedule.placeText && <div className="board-box-row"><span className="board-box-label">장소</span><span className="board-box-value">{detailPost.schedule.placeText} {detailPost.schedule.placeUrl && <a href={detailPost.schedule.placeUrl} target="_blank" rel="noreferrer" style={{ marginLeft: '10px', color: 'var(--primary-600)' }}>[지도보기]</a>}</span></div>}
                                        <div className="board-box-row"><span className="board-box-label">구분</span><span className="board-box-value">{detailPost.schedule.videoMeetingYn === 'Y' ? '화상회의 연동' : '일반 일정'}</span></div>
                                        <div className="board-box-row" style={{ marginTop: '12px', paddingTop: '12px', borderTop: '1px dashed var(--gray-200)' }}>
                                            <span className="board-box-label">참석자</span>
                                            <div className="board-box-value" style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                                                {(detailPost.schedule.attendees || []).map((attendee) => (
                                                    <span key={attendee.userId} className="board-badge gray" title={attendee.deptNm || '-'}>
                                                        {attendee.userNm || attendee.userId} <small style={{ marginLeft: '4px', opacity: 0.7 }}>({ATTENDANCE_STATUS_LABELS[attendee.attendanceSttsCd] || attendee.attendanceSttsCd || '초대됨'})</small>
                                                    </span>
                                                ))}
                                            </div>
                                        </div>
                                    </div>
                                )}

                                {detailPost.pstTypeCd === 'todo' && detailPost.todo && (
                                    <div className="board-box">
                                        <div className="board-box-title" style={{ display: 'flex', justifyContent: 'space-between' }}>
                                            <span>할일 배정</span>
                                            <span style={{ fontWeight: 400, color: 'var(--gray-500)' }}>달성률 <strong>{detailPost.todo.doneCount || 0}</strong>/{detailPost.todo.totalCount || 0}</span>
                                        </div>
                                        <div className="board-box-row"><span className="board-box-label">마감일</span><span className="board-box-value">{formatDateTime(detailPost.todo.dueDt)}</span></div>
                                        <div className="board-filter-stack" style={{ marginTop: '12px' }}>
                                            {(detailPost.todo.assignees || []).map((assignee) => (
                                                <div key={assignee.userId} className="board-box-row" style={{ alignItems: 'center', background: '#fff', padding: '8px 12px', border: '1px solid var(--gray-200)', borderRadius: '6px' }}>
                                                    <span className="board-box-value"><strong>{assignee.userNm || assignee.userId}</strong> <span style={{ color: 'var(--gray-500)', fontSize: '12px' }}>{joinMeta(assignee.deptNm, assignee.jbgdNm)}</span></span>
                                                    {isAdmin || assignee.userId === user?.userId ? (
                                                        <select className="form-input" style={{ width: '100px', height: '28px', fontSize: '12px', padding: '0 8px' }} value={assignee.statusCd || 'requested'} onChange={(event) => updateTodoStatus(detailPost.pstId, assignee.userId, event.target.value)}>{TODO_STATUS_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
                                                    ) : (
                                                        <span className="board-badge gray">{labelOf(TODO_STATUS_OPTIONS, assignee.statusCd, '요청')}</span>
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                <div className="board-comment-section">
                                    <div className="board-detail-title-row" style={{ marginBottom: '16px' }}>
                                        <h3 style={{ fontSize: '16px', margin: 0, fontWeight: 700 }}>댓글 {(detailPost.comments || []).length}</h3>
                                    </div>
                                    <form className="board-comment-form" onSubmit={submitComment}>
                                        {replyTargetId && <div className="board-comment-header"><span className="board-badge blue">답글 대상: {replyTargetId}</span><button type="button" className="board-action-btn" onClick={() => setReplyTargetId('')}>취소</button></div>}
                                        <textarea className="board-comment-input" placeholder="댓글을 입력하세요..." value={commentText} onChange={(event) => setCommentText(event.target.value)} />
                                        <div className="board-comment-controls">
                                            <input className="form-input" style={{ width: 'auto', border: 'none', background: 'transparent', padding: 0 }} type="file" multiple onChange={(event) => setCommentFiles(Array.from(event.target.files || []))} />
                                            <button type="submit" className="btn btn-primary">등록</button>
                                        </div>
                                    </form>

                                    <div className="board-filter-stack">
                                        {(detailPost.comments || []).length === 0 ? <div style={{ textAlign: 'center', padding: '30px', color: 'var(--gray-400)', fontSize: '13px' }}>작성된 댓글이 없습니다.</div> : detailPost.comments.map((comment) => (
                                            <div key={comment.cmntSqn} className="board-comment-item" style={{ marginLeft: comment.upCmntSqn ? '30px' : 0 }}>
                                                <div className="board-comment-header">
                                                    <strong>{comment.userNm || comment.crtUserId}</strong>
                                                    <small>{formatDateTime(comment.frstCrtDt)}</small>
                                                </div>
                                                <p className="board-comment-body">{comment.contents}</p>
                                                {(comment.attachments || []).length > 0 && (
                                                    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '8px' }}>
                                                        {comment.attachments.map((file) => (
                                                            <a key={`${file.fileId}-${file.fileSeq}`} href={file.filePath} target="_blank" rel="noreferrer" className="board-badge gray" style={{ fontWeight: 400, textDecoration: 'none' }}>📎 {file.orgnFileNm || file.saveFileNm}</a>
                                                        ))}
                                                    </div>
                                                )}
                                                <div className="board-comment-actions">
                                                    {!comment.upCmntSqn && <button type="button" className="board-action-btn" onClick={() => setReplyTargetId(comment.cmntSqn)}>답글 달기</button>}
                                                    {(comment.crtUserId === user?.userId || detailPost.manageable) && (
                                                        <>
                                                            <button type="button" className="board-action-btn" onClick={() => editComment(comment)}>수정</button>
                                                            <button type="button" className="board-action-btn" style={{ color: 'var(--danger)' }} onClick={() => deleteComment(comment)}>삭제</button>
                                                        </>
                                                    )}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        );
    }

    function renderShareModal() {
        if (!shareOpen || !detailPost) return null;
        return (
            <div className="modal-overlay" onClick={() => setShareOpen(false)}>
                <div className="modal" style={{ maxWidth: '720px' }} onClick={(event) => event.stopPropagation()}>
                    <div className="modal-header">
                        <h3>게시물 공유</h3>
                        <button type="button" className="btn btn-outline" onClick={() => setShareOpen(false)}>닫기</button>
                    </div>
                    <div className="modal-body">
                        <div className="profile-layout">
                            <div className="feed-widget-card compact-card">
                                <div className="feed-widget-header"><div><h3>사용자</h3></div></div>
                                <div className="feed-chip-list">
                                    {filteredUsers.map((person) => (
                                        <button key={person.userId} type="button" className={`feed-chip ${shareDraft.userIds.includes(person.userId) ? 'active' : ''}`} onClick={() => setShareDraft((current) => ({ ...current, userIds: toggle(current.userIds, person.userId) }))}>
                                            {person.userNm || person.userId}
                                        </button>
                                    ))}
                                </div>
                            </div>
                            <div className="feed-widget-card compact-card">
                                <div className="feed-widget-header"><div><h3>커뮤니티</h3></div></div>
                                <div className="feed-chip-list">
                                    {communities.map((community) => (
                                        <button key={community.communityId} type="button" className={`feed-chip ${shareDraft.communityIds.includes(String(community.communityId)) ? 'active' : ''}`} onClick={() => setShareDraft((current) => ({ ...current, communityIds: toggle(current.communityIds, String(community.communityId)) }))}>
                                            {community.communityNm}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        </div>
                        <div className="feed-composer-actions" style={{ marginTop: 'var(--spacing-lg)' }}>
                            <button type="button" className="btn btn-outline" onClick={() => setShareOpen(false)}>취소</button>
                            <button type="button" className="btn btn-primary" onClick={submitShare}>공유</button>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    function renderReportModal() {
        if (overlayMode !== 'report' || !detailPost) return null;
        return (
            <div className="modal-overlay" onClick={() => setOverlayMode('')}>
                <div className="modal" style={{ maxWidth: '560px' }} onClick={(event) => event.stopPropagation()}>
                    <div className="modal-header">
                        <h3>게시물 신고</h3>
                        <button type="button" className="btn btn-outline" onClick={() => setOverlayMode('')}>닫기</button>
                    </div>
                    <div className="modal-body">
                        <textarea className="form-input" rows="6" placeholder="신고 사유를 입력하세요" value={reportReason} onChange={(event) => setReportReason(event.target.value)} />
                        <div className="feed-composer-actions" style={{ marginTop: 'var(--spacing-md)' }}>
                            <button type="button" className="btn btn-outline" onClick={() => setOverlayMode('')}>취소</button>
                            <button type="button" className="btn btn-primary" onClick={submitReport}>신고</button>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    function renderPeopleModal() {
        if (overlayMode !== 'likes' && overlayMode !== 'readers') return null;
        const items = overlayMode === 'likes' ? likes : readers;
        return (
            <div className="modal-overlay" onClick={() => setOverlayMode('')}>
                <div className="modal" style={{ maxWidth: '560px' }} onClick={(event) => event.stopPropagation()}>
                    <div className="modal-header">
                        <h3>{overlayMode === 'likes' ? '좋아요한 사람' : '읽은 사람'}</h3>
                        <button type="button" className="btn btn-outline" onClick={() => setOverlayMode('')}>닫기</button>
                    </div>
                    <div className="modal-body">
                        <div className="feed-widget-list">
                            {items.length === 0 ? <div className="feed-widget-empty">사용자가 없습니다.</div> : items.map((item, index) => (
                                <div key={`${item.userId || 'user'}-${index}`} className="feed-inline-item">
                                    <div><strong>{item.userNm || item.userId}</strong><small>{joinMeta(item.deptNm, formatDateTime(overlayTimestamp(overlayMode, item)))}</small></div>
                                    <span className="badge badge-gray">{overlayMode === 'likes' ? '좋아요' : '읽음'}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="animate-slide-up">
            {renderMainLayout()}
            {renderComposeModal()}
            {renderDetailModal()}
            {renderShareModal()}
            {renderReportModal()}
            {renderPeopleModal()}
        </div>
    );
}
