import { Client } from '@stomp/stompjs';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { communityAPI, messengerAPI } from '../../services/api';

const EMOJI_LIST = ['😀', '😂', '😍', '😎', '😭', '👍', '🙏', '🔥', '🎉', '✅', '💬', '📌'];

function websocketUrl() {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    return `${protocol}://${window.location.host}/starworks-groupware-websocket`;
}

function asArray(value) {
    return Array.isArray(value) ? value : [];
}

function formatDateTime(value) {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '-';
    return date.toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

function formatDayKey(value) {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' });
}

function roomTypeLabel(type) {
    switch (type) {
        case 'private': return '1:1';
        case 'self': return 'me';
        case 'community': return '커뮤니티';
        default: return '그룹';
    }
}

function roleLabel(roleCd) {
    return roleCd === 'owner' ? '방장' : '참여자';
}

function fileDownloadUrl(saveFileNm) {
    return `/rest/file/download/${encodeURIComponent(saveFileNm)}`;
}

function recentEmojiList() {
    try {
        return asArray(JSON.parse(localStorage.getItem('starworks.messenger.emoji') || '[]'));
    } catch {
        return [];
    }
}

export default function MessengerPage() {
    const { user: authUser } = useAuth();
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const popupMode = searchParams.get('popup') === '1';
    const requestedRoomId = searchParams.get('room') || '';

    const clientRef = useRef(null);
    const roomSubscriptionRef = useRef(null);
    const updateSubscriptionRef = useRef(null);
    const roomsReloadTimerRef = useRef(null);
    const activeRoomIdRef = useRef('');
    const messageRefs = useRef({});
    const fileInputRef = useRef(null);

    const [currentUser, setCurrentUser] = useState(null);
    const [users, setUsers] = useState([]);
    const [rooms, setRooms] = useState([]);
    const [activeRoomId, setActiveRoomId] = useState('');
    const [roomDetail, setRoomDetail] = useState(null);
    const [messages, setMessages] = useState([]);
    const [communities, setCommunities] = useState([]);
    const [communityMembers, setCommunityMembers] = useState([]);
    const [selectedCommunityId, setSelectedCommunityId] = useState(null);
    const [showCommunityManager, setShowCommunityManager] = useState(false);
    const [communityKeyword, setCommunityKeyword] = useState('');
    const [communityForm, setCommunityForm] = useState({ communityNm: '', communityDesc: '', communityTypeCd: 'group' });
    const [communityMemberIds, setCommunityMemberIds] = useState([]);
    const [roomScope, setRoomScope] = useState('all');
    const [roomType, setRoomType] = useState('all');
    const [roomKeyword, setRoomKeyword] = useState('');
    const [directUserId, setDirectUserId] = useState('');
    const [groupRoomNm, setGroupRoomNm] = useState('');
    const [groupUserIds, setGroupUserIds] = useState([]);
    const [messageInput, setMessageInput] = useState('');
    const [selectedFiles, setSelectedFiles] = useState([]);
    const [showEmojiPicker, setShowEmojiPicker] = useState(false);
    const [showSearchPanel, setShowSearchPanel] = useState(false);
    const [searchKeyword, setSearchKeyword] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [inviteUserIds, setInviteUserIds] = useState([]);
    const [messageMenuId, setMessageMenuId] = useState('');
    const [forwardingMessageId, setForwardingMessageId] = useState('');
    const [highlightedMessageId, setHighlightedMessageId] = useState('');
    const [connected, setConnected] = useState(false);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [recentEmojis, setRecentEmojis] = useState(recentEmojiList());
    const [showDetailPanel, setShowDetailPanel] = useState(false);
    const [showRoomList, setShowRoomList] = useState(false);
    const [loadingUsers, setLoadingUsers] = useState(false);
    const [loadingCommunities, setLoadingCommunities] = useState(false);
    const [usersLoaded, setUsersLoaded] = useState(false);
    const [communitiesLoaded, setCommunitiesLoaded] = useState(false);

    const activeRoom = useMemo(() => rooms.find((room) => room.msgrId === activeRoomId) || null, [rooms, activeRoomId]);
    const directRoomUsers = useMemo(() => asArray(users).filter((user) => user.userId !== currentUser?.userId), [users, currentUser?.userId]);
    const inviteCandidates = useMemo(() => {
        const existing = new Set(asArray(roomDetail?.participants).map((participant) => participant.userId));
        return directRoomUsers.filter((candidate) => !existing.has(candidate.userId));
    }, [directRoomUsers, roomDetail?.participants]);
    const selectedCommunity = useMemo(() => communities.find((community) => community.communityId === selectedCommunityId) || null, [communities, selectedCommunityId]);
    const totalUnreadCount = useMemo(() => rooms.reduce((sum, room) => sum + Number(room.unreadCount || 0), 0), [rooms]);
    const activeParticipantSummary = useMemo(() => {
        const participants = asArray(roomDetail?.participants);
        if (participants.length === 0) return '선택된 대화방 정보가 없습니다.';
        const names = participants.map((participant) => participant.userNm).filter(Boolean);
        if (names.length === 0) return '선택된 대화방 정보가 없습니다.';
        const preview = names.slice(0, 4).join(', ');
        return names.length > 4 ? `${preview} 외 ${names.length - 4}명` : preview;
    }, [roomDetail?.participants]);

    useEffect(() => {
        bootstrap();
        return () => {
            roomSubscriptionRef.current?.unsubscribe();
            updateSubscriptionRef.current?.unsubscribe();
            clientRef.current?.deactivate();
            if (roomsReloadTimerRef.current) {
                window.clearTimeout(roomsReloadTimerRef.current);
            }
        };
    }, []);

    useEffect(() => {
        if (!currentUser?.userId || clientRef.current) return;
        connectSocket();
    }, [currentUser?.userId]);

    useEffect(() => {
        activeRoomIdRef.current = activeRoomId;
    }, [activeRoomId]);

    useEffect(() => {
        loadRooms();
    }, [roomScope, roomType]);

    useEffect(() => {
        if (!connected || !activeRoomId) return;
        subscribeRoom(activeRoomId);
    }, [connected, activeRoomId]);

    useEffect(() => {
        if (!showCommunityManager || !selectedCommunityId) {
            setCommunityMembers([]);
            return;
        }
        loadCommunityMembers(selectedCommunityId);
    }, [selectedCommunityId, showCommunityManager]);

    useEffect(() => {
        if (!highlightedMessageId) return;
        const timer = window.setTimeout(() => setHighlightedMessageId(''), 2400);
        return () => window.clearTimeout(timer);
    }, [highlightedMessageId]);

    useEffect(() => {
        if (!showDetailPanel && !showRoomList && !messageMenuId) return undefined;
        function handleEscape(event) {
            if (event.key !== 'Escape') return;
            setShowDetailPanel(false);
            setShowRoomList(false);
            setMessageMenuId('');
        }
        window.addEventListener('keydown', handleEscape);
        return () => window.removeEventListener('keydown', handleEscape);
    }, [showDetailPanel, showRoomList, messageMenuId]);

    function scheduleRoomsReload(delay = 120) {
        if (roomsReloadTimerRef.current) {
            window.clearTimeout(roomsReloadTimerRef.current);
        }

        roomsReloadTimerRef.current = window.setTimeout(() => {
            roomsReloadTimerRef.current = null;
            loadRooms(true);
        }, delay);
    }

    async function ensureUsersLoaded(force = false) {
        if ((usersLoaded && !force) || loadingUsers) {
            return;
        }

        setLoadingUsers(true);
        try {
            const response = await messengerAPI.users();
            const userItems = asArray(response.data);
            setUsers(userItems);
            setDirectUserId((current) => current || userItems[0]?.userId || '');
            setUsersLoaded(true);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '대화 상대 목록을 불러오지 못했습니다.');
        } finally {
            setLoadingUsers(false);
        }
    }

    async function ensureCommunitiesLoaded(force = false, keyword = '') {
        if ((communitiesLoaded && !force && !keyword) || loadingCommunities) {
            return;
        }

        setLoadingCommunities(true);
        try {
            const response = await communityAPI.list(keyword ? { q: keyword } : { view: 'joined' });
            const next = asArray(response.data);
            setCommunities(next);
            setCommunitiesLoaded(true);
            if (!selectedCommunityId && next[0]) {
                setSelectedCommunityId(next[0].communityId);
                setCommunityForm({
                    communityNm: next[0].communityNm || '',
                    communityDesc: next[0].communityDesc || '',
                    communityTypeCd: next[0].communityTypeCd || 'group',
                });
            }
        } catch (requestError) {
            setError(requestError.response?.data?.message || '커뮤니티 목록을 불러오지 못했습니다.');
        } finally {
            setLoadingCommunities(false);
        }
    }

    useEffect(() => {
        if (!showCommunityManager) {
            return;
        }

        ensureUsersLoaded();
        ensureCommunitiesLoaded();
    }, [showCommunityManager]);

    async function bootstrap() {
        setLoading(true);
        setError('');
        try {
            const currentUserResponse = await messengerAPI.currentUser();
            const me = currentUserResponse.data?.userId ? { ...authUser, ...currentUserResponse.data } : authUser;
            if (!me?.userId) throw new Error('현재 로그인 사용자 정보를 확인하지 못했습니다.');

            setCurrentUser(me);
            const roomResponse = await messengerAPI.rooms({ scope: roomScope, type: roomType });
            const roomItems = asArray(roomResponse.data);
            setRooms(roomItems);

            const initialRoomId = roomItems.some((room) => room.msgrId === requestedRoomId) ? requestedRoomId : roomItems[0]?.msgrId || '';
            if (initialRoomId) await openRoom(initialRoomId);
        } catch (requestError) {
            if (requestError.message !== 'AUTH_REDIRECT') {
                setError(requestError.response?.data?.message || requestError.message || '메신저 데이터를 불러오지 못했습니다.');
            }
        } finally {
            setLoading(false);
            window.setTimeout(() => {
                ensureUsersLoaded();
            }, 0);
        }
    }

    function connectSocket() {
        const client = new Client({
            brokerURL: websocketUrl(),
            reconnectDelay: 5000,
            debug: () => {},
            onConnect: () => {
                setConnected(true);
                updateSubscriptionRef.current?.unsubscribe();
                updateSubscriptionRef.current = client.subscribe(`/topic/chat/update/${currentUser.userId}`, (frame) => {
                    const payload = JSON.parse(frame.body || '{}');
                    scheduleRoomsReload();
                    if (payload?.msgrId && payload.msgrId === activeRoomIdRef.current) {
                        refreshRoom(payload.msgrId, true, { markRead: false });
                    }
                });
                if (activeRoomId) subscribeRoom(activeRoomId);
            },
            onStompError: () => setError('메신저 실시간 연결 중 오류가 발생했습니다.'),
            onWebSocketClose: () => setConnected(false),
        });
        client.activate();
        clientRef.current = client;
    }

    function subscribeRoom(roomId) {
        if (!clientRef.current?.connected) return;
        roomSubscriptionRef.current?.unsubscribe();
        roomSubscriptionRef.current = clientRef.current.subscribe(`/topic/room/${roomId}`, async (frame) => {
            const payload = JSON.parse(frame.body);
            if (['DELETE', 'PIN', 'JOIN', 'LEAVE', 'ROOM_UPDATE'].includes(payload.type)) {
                await refreshRoom(roomId, true, { markRead: false });
                scheduleRoomsReload();
                return;
            }
            setMessages((current) => [...current, payload]);
            scheduleRoomsReload();
            if (payload.userId && payload.userId !== currentUser?.userId) {
                await messengerAPI.markAsRead(roomId).catch(() => {});
                scheduleRoomsReload(40);
            }
        });
    }

    async function loadRooms(silent = false) {
        try {
            const response = await messengerAPI.rooms({ scope: roomScope, keyword: roomKeyword || undefined, type: roomType });
            setRooms(asArray(response.data));
        } catch (requestError) {
            if (!silent) setError(requestError.response?.data?.message || '대화방 목록을 불러오지 못했습니다.');
        }
    }

    async function refreshRoom(roomId, silent = false, options = {}) {
        const shouldMarkRead = options.markRead !== false;
        try {
            const [detailResponse, messageResponse] = await Promise.all([
                messengerAPI.roomDetail(roomId),
                messengerAPI.messages(roomId),
                shouldMarkRead ? messengerAPI.markAsRead(roomId).catch(() => null) : Promise.resolve(null),
            ]);
            setRoomDetail(detailResponse.data || null);
            setMessages(asArray(messageResponse.data));
            if (shouldMarkRead) {
                scheduleRoomsReload(40);
            }
            if (!silent) setError('');
        } catch (requestError) {
            if (!silent) setError(requestError.response?.data?.message || '대화방 정보를 불러오지 못했습니다.');
        }
    }

    async function openRoom(roomId) {
        if (!roomId) return;
        setActiveRoomId(roomId);
        setShowSearchPanel(false);
        setMessageMenuId('');
        setShowRoomList(false);
        setShowDetailPanel(false);
        await refreshRoom(roomId);
        const nextParams = new URLSearchParams(searchParams);
        nextParams.set('room', roomId);
        if (popupMode) nextParams.set('popup', '1');
        setSearchParams(nextParams);
    }

    async function handleOpenDirectRoom() {
        if (!directUserId) return;
        setSaving(true);
        try {
            const response = await messengerAPI.findOrCreate(directUserId);
            scheduleRoomsReload(0);
            await openRoom(response.data?.msgrId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '1:1 대화를 열지 못했습니다.');
        } finally {
            setSaving(false);
        }
    }

    async function handleOpenSelfRoom() {
        setSaving(true);
        try {
            const response = await messengerAPI.selfRoom();
            scheduleRoomsReload(0);
            await openRoom(response.data?.msgrId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '나와의 채팅을 열지 못했습니다.');
        } finally {
            setSaving(false);
        }
    }

    function toggleGroupUser(userId) {
        setGroupUserIds((current) => current.includes(userId) ? current.filter((item) => item !== userId) : [...current, userId]);
    }

    async function handleCreateGroupRoom() {
        if (groupUserIds.length === 0) {
            setError('그룹 채팅 참여자를 한 명 이상 선택하세요.');
            return;
        }
        setSaving(true);
        try {
            const response = await messengerAPI.createRoom({ roomNm: groupRoomNm, userIds: groupUserIds, isGroup: true, roomTypeCd: 'group' });
            setGroupRoomNm('');
            setGroupUserIds([]);
            scheduleRoomsReload(0);
            await openRoom(response.data?.msgrId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '그룹 채팅방 생성에 실패했습니다.');
        } finally {
            setSaving(false);
        }
    }

    async function handleRenameRoom() {
        if (!activeRoom?.msgrId) return;
        const nextName = window.prompt('채팅방 이름을 입력하세요.', activeRoom.msgrNm || '');
        if (!nextName || !nextName.trim()) return;
        try {
            await messengerAPI.renameRoom(activeRoom.msgrId, nextName.trim());
            await refreshRoom(activeRoom.msgrId, true, { markRead: false });
            scheduleRoomsReload();
        } catch (requestError) {
            setError(requestError.response?.data?.message || '채팅방 이름 변경에 실패했습니다.');
        }
    }

    async function handleLeaveRoom() {
        if (!activeRoom?.msgrId || !window.confirm('정말 이 대화방을 나가시겠습니까?')) return;
        try {
            await messengerAPI.leave(activeRoom.msgrId);
            setMessages([]);
            setRoomDetail(null);
            setActiveRoomId('');
            setShowDetailPanel(false);
            await loadRooms(true);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '채팅방 나가기에 실패했습니다.');
        }
    }

    async function handleRoomSearch() {
        if (!activeRoomId || !searchKeyword.trim()) {
            setSearchResults([]);
            return;
        }
        try {
            const response = await messengerAPI.searchMessages(activeRoomId, searchKeyword.trim());
            setSearchResults(asArray(response.data));
        } catch (requestError) {
            setError(requestError.response?.data?.message || '대화 검색에 실패했습니다.');
        }
    }

    function handleJumpToMessage(msgContId) {
        setShowSearchPanel(false);
        setHighlightedMessageId(msgContId);
        requestAnimationFrame(() => messageRefs.current[msgContId]?.scrollIntoView({ behavior: 'smooth', block: 'center' }));
    }

    function handleEmojiSelect(emoji) {
        setMessageInput((current) => `${current}${emoji}`);
        setShowEmojiPicker(false);
        const next = [emoji, ...recentEmojis.filter((item) => item !== emoji)].slice(0, 10);
        localStorage.setItem('starworks.messenger.emoji', JSON.stringify(next));
        setRecentEmojis(next);
    }

    async function handleSendMessage(event) {
        event.preventDefault();
        if (!messageInput.trim() || !activeRoomId || !clientRef.current?.connected) return;
        clientRef.current.publish({
            destination: `/app/chat.sendMessage/${activeRoomId}`,
            body: JSON.stringify({ contents: messageInput.trim(), msgTypeCd: 'text' }),
        });
        setMessageInput('');
    }

    function handleMessageKeyDown(event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            handleSendMessage(event);
        }
    }

    async function handleUploadFiles() {
        if (!activeRoomId || selectedFiles.length === 0) return;
        setSaving(true);
        try {
            await messengerAPI.uploadFiles(activeRoomId, messageInput.trim(), selectedFiles);
            setSelectedFiles([]);
            setMessageInput('');
            if (fileInputRef.current) fileInputRef.current.value = '';
        } catch (requestError) {
            setError(requestError.response?.data?.message || '파일 업로드에 실패했습니다.');
        } finally {
            setSaving(false);
        }
    }

    function handleDropFiles(event) {
        event.preventDefault();
        const nextFiles = Array.from(event.dataTransfer.files || []);
        if (nextFiles.length > 0) setSelectedFiles(nextFiles);
    }

    async function handleDeleteMessage(message) {
        if (!message?.msgContId || !activeRoomId || !window.confirm('이 메시지를 삭제하시겠습니까?')) return;
        try {
            await messengerAPI.deleteMessage(message.msgContId, activeRoomId);
            setMessageMenuId('');
        } catch (requestError) {
            setError(requestError.response?.data?.message || '메시지 삭제에 실패했습니다.');
        }
    }

    async function handleForwardMessage(targetRoomId) {
        if (!forwardingMessageId || !targetRoomId) return;
        try {
            await messengerAPI.forwardMessage(forwardingMessageId, targetRoomId);
            setForwardingMessageId('');
            setSuccessMessage('메시지를 전달했습니다.');
        } catch (requestError) {
            setError(requestError.response?.data?.message || '메시지 전달에 실패했습니다.');
        }
    }

    async function handleTogglePin(message) {
        if (!activeRoomId) return;
        try {
            if (roomDetail?.room?.pinnedMsgContId === message.msgContId) {
                await messengerAPI.clearPin(activeRoomId);
            } else {
                await messengerAPI.pin(activeRoomId, message.msgContId);
            }
            setMessageMenuId('');
        } catch (requestError) {
            setError(requestError.response?.data?.message || '상단 공지 설정에 실패했습니다.');
        }
    }

    async function handleToggleNotify() {
        if (!activeRoomId) return;
        try {
            await messengerAPI.notify(activeRoomId, !roomDetail?.notifyEnabled);
            await refreshRoom(activeRoomId, true, { markRead: false });
            scheduleRoomsReload();
        } catch (requestError) {
            setError(requestError.response?.data?.message || '알림 설정 변경에 실패했습니다.');
        }
    }

    async function handleInviteUsers() {
        if (!activeRoomId || inviteUserIds.length === 0) return;
        try {
            await messengerAPI.invite(activeRoomId, inviteUserIds);
            setInviteUserIds([]);
            await refreshRoom(activeRoomId, true, { markRead: false });
            scheduleRoomsReload();
        } catch (requestError) {
            setError(requestError.response?.data?.message || '참여자 초대에 실패했습니다.');
        }
    }

    async function handleKickUser(userId) {
        if (!activeRoomId || !window.confirm('이 사용자를 대화방에서 제외하시겠습니까?')) return;
        try {
            await messengerAPI.kick(activeRoomId, userId);
            await refreshRoom(activeRoomId, true, { markRead: false });
            scheduleRoomsReload();
        } catch (requestError) {
            setError(requestError.response?.data?.message || '참여자 강퇴에 실패했습니다.');
        }
    }

    async function handleExportMessages() {
        if (!activeRoomId) return;
        try {
            const response = await messengerAPI.exportMessages(activeRoomId);
            const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `chat-${activeRoomId}.xlsx`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '대화 내보내기에 실패했습니다.');
        }
    }

    function handleOpenPopup() {
        if (!activeRoomId) return;
        window.open(`/messenger?room=${activeRoomId}&popup=1`, `messenger-${activeRoomId}`, 'width=1280,height=860');
    }

    async function loadCommunities(keyword = '') {
        await ensureCommunitiesLoaded(true, keyword);
    }

    async function loadCommunityMembers(communityId) {
        try {
            const [detailResponse, membersResponse] = await Promise.all([communityAPI.detail(communityId), communityAPI.members(communityId)]);
            const detail = detailResponse.data;
            setCommunityForm({ communityNm: detail?.communityNm || '', communityDesc: detail?.communityDesc || '', communityTypeCd: detail?.communityTypeCd || 'group' });
            setCommunityMembers(asArray(membersResponse.data));
        } catch (requestError) {
            setError(requestError.response?.data?.message || '커뮤니티 정보를 불러오지 못했습니다.');
        }
    }

    async function handleSaveCommunity() {
        try {
            if (selectedCommunity) {
                await communityAPI.update(selectedCommunity.communityId, communityForm);
            } else {
                const response = await communityAPI.create({ ...communityForm, memberUserIds: communityMemberIds });
                setSelectedCommunityId(response.data?.communityId || null);
            }
            setCommunityMemberIds([]);
            await loadCommunities(communityKeyword);
            if (selectedCommunityId) await loadCommunityMembers(selectedCommunityId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '커뮤니티 저장에 실패했습니다.');
        }
    }

    async function handleDeleteCommunity() {
        if (!selectedCommunity || !window.confirm('이 커뮤니티를 삭제하시겠습니까?')) return;
        try {
            await communityAPI.remove(selectedCommunity.communityId);
            setSelectedCommunityId(null);
            setCommunityForm({ communityNm: '', communityDesc: '', communityTypeCd: 'group' });
            setCommunityMembers([]);
            await loadCommunities(communityKeyword);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '커뮤니티 삭제에 실패했습니다.');
        }
    }

    async function handleAddCommunityMembers() {
        if (!selectedCommunity || communityMemberIds.length === 0) return;
        try {
            await communityAPI.addMembers(selectedCommunity.communityId, communityMemberIds);
            setCommunityMemberIds([]);
            await loadCommunityMembers(selectedCommunity.communityId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '커뮤니티 멤버 추가에 실패했습니다.');
        }
    }

    async function handleRemoveCommunityMember(userId) {
        if (!selectedCommunity) return;
        try {
            await communityAPI.removeMember(selectedCommunity.communityId, userId);
            await loadCommunityMembers(selectedCommunity.communityId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '커뮤니티 멤버 제거에 실패했습니다.');
        }
    }

    async function handleCreateRoomFromCommunity() {
        if (!selectedCommunity) return;
        try {
            const response = await messengerAPI.createRoom({ roomNm: selectedCommunity.communityNm, userIds: communityMembers.map((member) => member.userId), isGroup: true, roomTypeCd: 'community' });
            scheduleRoomsReload(0);
            await openRoom(response.data?.msgrId);
            setShowCommunityManager(false);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '커뮤니티 채팅방 생성에 실패했습니다.');
        }
    }

    function startNewCommunity() {
        setSelectedCommunityId(null);
        setCommunityMembers([]);
        setCommunityForm({ communityNm: '', communityDesc: '', communityTypeCd: 'group' });
    }

    return (
        <div className={`messenger-shell ${popupMode ? 'popup' : ''}`}>
            <div className="page-header messenger-page-header">
                <div>
                    <h2>{popupMode ? '새 창 메신저' : '메신저'}</h2>
                    <p>대화 목록과 현재 채팅에 집중하는 밀도 높은 메신저 화면으로 정리했습니다.</p>
                </div>
                <div className="page-header-actions messenger-page-actions">
                    <span className={`badge ${connected ? 'badge-green' : 'badge-orange'}`}>{connected ? '실시간 연결됨' : '연결 중'}</span>
                    <button type="button" className="btn btn-outline" onClick={() => navigate('/community')}>커뮤니티</button>
                    <button type="button" className="btn btn-outline" onClick={() => setShowCommunityManager(true)}>커뮤니티 관리</button>
                    {!popupMode && <button type="button" className="btn btn-secondary" onClick={handleOpenPopup} disabled={!activeRoom}>새 창</button>}
                </div>
            </div>

            {error && <div className="alert alert-error">{error}</div>}
            {successMessage && <div className="alert alert-success">{successMessage}</div>}

            <div className="messenger-layout">
                <button type="button" className={`messenger-drawer-backdrop ${showRoomList ? 'visible' : ''}`} aria-label="대화방 목록 닫기" onClick={() => setShowRoomList(false)} />
                <button type="button" className={`messenger-detail-backdrop ${showDetailPanel ? 'visible' : ''}`} aria-label="대화 정보 닫기" onClick={() => setShowDetailPanel(false)} />

                <aside className={`messenger-sidebar ${showRoomList ? 'open' : ''}`}>
                    <div className="messenger-sidebar-frame">
                        <div className="messenger-sidebar-top">
                            <div>
                                <span className="messenger-sidebar-kicker">CHAT</span>
                                <h3>대화방</h3>
                            </div>
                            <div className="messenger-sidebar-top-actions">
                                {totalUnreadCount > 0 && <span className="messenger-unread-chip">미읽음 {totalUnreadCount}</span>}
                                <button type="button" className="btn btn-sm btn-outline messenger-mobile-only" onClick={() => setShowRoomList(false)}>닫기</button>
                            </div>
                        </div>

                        <div className="messenger-sidebar-controls">
                            <div className="messenger-search-inline messenger-room-search">
                                <input className="form-input" placeholder="대화방 검색" value={roomKeyword} onChange={(event) => setRoomKeyword(event.target.value)} />
                                <button type="button" className="btn btn-outline" onClick={() => loadRooms()}>검색</button>
                            </div>
                            <div className="messenger-filter-row">
                                <button type="button" className={`filter-chip ${roomScope === 'all' ? 'active' : ''}`} onClick={() => setRoomScope('all')}>전체</button>
                                <button type="button" className={`filter-chip ${roomScope === 'unread' ? 'active' : ''}`} onClick={() => setRoomScope('unread')}>안 읽음</button>
                            </div>
                            <div className="messenger-filter-row secondary">
                                {['all', 'private', 'group', 'self', 'community'].map((typeCode) => (
                                    <button key={typeCode} type="button" className={`filter-chip ${roomType === typeCode ? 'active' : ''}`} onClick={() => setRoomType(typeCode)}>
                                        {typeCode === 'all' ? '전체' : roomTypeLabel(typeCode)}
                                    </button>
                                ))}
                            </div>
                        </div>

                        <details className="messenger-compose-panel" open={rooms.length === 0}>
                            <summary className="messenger-compose-summary">
                                <div>
                                    <strong>새 대화 시작</strong>
                                    <span>1:1, 그룹, 커뮤니티</span>
                                </div>
                                <span className="messenger-compose-summary-icon">+</span>
                            </summary>
                            <div className="messenger-compose-body">
                                <div className="messenger-compose-actions">
                                    <button type="button" className="btn btn-sm btn-outline" onClick={handleOpenSelfRoom}>me 채팅</button>
                                    <button type="button" className="btn btn-sm btn-outline" onClick={() => setShowCommunityManager(true)}>커뮤니티 관리</button>
                                </div>
                                <div className="messenger-panel-block">
                                    <label className="form-label">1:1 대화</label>
                                    <select className="form-input" value={directUserId} onChange={(event) => setDirectUserId(event.target.value)}>
                                        <option value="">대화 상대 선택</option>
                                        {directRoomUsers.map((item) => (
                                            <option key={item.userId} value={item.userId}>{item.userNm} ({item.userId})</option>
                                        ))}
                                    </select>
                                    <button type="button" className="btn btn-primary" onClick={handleOpenDirectRoom} disabled={!directUserId || saving}>대화 열기</button>
                                </div>
                                <div className="messenger-panel-block">
                                    <label className="form-label">그룹 채팅</label>
                                    <input className="form-input" placeholder="그룹방 이름" value={groupRoomNm} onChange={(event) => setGroupRoomNm(event.target.value)} />
                                    <div className="messenger-check-list compact max-grid">
                                        {directRoomUsers.map((item) => (
                                            <label key={item.userId} className="messenger-check-item">
                                                <input type="checkbox" checked={groupUserIds.includes(item.userId)} onChange={() => toggleGroupUser(item.userId)} />
                                                <span>{item.userNm}</span>
                                                <small>{item.deptNm || '-'}</small>
                                            </label>
                                        ))}
                                    </div>
                                    <button type="button" className="btn btn-secondary" onClick={handleCreateGroupRoom} disabled={saving}>그룹방 생성</button>
                                </div>
                            </div>
                        </details>

                        <div className="messenger-room-list-shell">
                            <div className="messenger-panel-head messenger-room-list-head">
                                <strong>대화방</strong>
                                <span className="messenger-count-chip">{rooms.length}</span>
                            </div>
                            <div className="messenger-room-list">
                                {loading ? (
                                    <div className="approval-empty compact"><strong>대화방을 불러오는 중입니다.</strong></div>
                                ) : rooms.length === 0 ? (
                                    <div className="approval-empty compact"><strong>대화방이 없습니다.</strong><span>새 대화 시작 패널에서 바로 방을 만들 수 있습니다.</span></div>
                                ) : rooms.map((room) => (
                                    <button key={room.msgrId} type="button" className={`messenger-room-item ${activeRoomId === room.msgrId ? 'active' : ''}`} onClick={() => openRoom(room.msgrId)}>
                                        <div className="messenger-room-badge">{room.msgrNm?.slice(0, 1) || '?'}</div>
                                        <div className="messenger-room-body">
                                            <div className="messenger-room-head">
                                                <strong>{room.msgrNm || '이름 없는 대화방'}</strong>
                                                <span>{formatDateTime(room.lastMsgDt || room.crtDt)}</span>
                                            </div>
                                            <div className="messenger-room-meta">
                                                <span className="room-type-pill">{roomTypeLabel(room.roomTypeCd)}</span>
                                                <span className="messenger-room-snippet">{room.lastMsgCont || '아직 메시지가 없습니다.'}</span>
                                            </div>
                                        </div>
                                        {room.unreadCount > 0 && <span className="messenger-room-unread">{room.unreadCount}</span>}
                                    </button>
                                ))}
                            </div>
                        </div>
                    </div>
                </aside>

                <section className="messenger-main" onDragOver={(event) => event.preventDefault()} onDrop={handleDropFiles}>
                    <div className="messenger-main-header">
                        <div className="messenger-main-leading">
                            <button type="button" className="btn btn-sm btn-outline messenger-mobile-only" onClick={() => setShowRoomList(true)}>목록</button>
                            <div className={`messenger-main-avatar ${activeRoom ? '' : 'placeholder'}`}>{activeRoom?.msgrNm?.slice(0, 1) || '?'}</div>
                            <div>
                                <div className="messenger-main-title-row">
                                    <h3>{activeRoom?.msgrNm || '대화방을 선택하세요'}</h3>
                                    {activeRoom && <span className="room-type-pill strong">{roomTypeLabel(activeRoom.roomTypeCd)}</span>}
                                </div>
                                <p>{activeParticipantSummary}</p>
                            </div>
                        </div>
                        {activeRoom && (
                            <div className="messenger-main-actions">
                                <button type="button" className="btn btn-outline" onClick={() => setShowSearchPanel((current) => !current)}>{showSearchPanel ? '검색 닫기' : '대화 검색'}</button>
                                {!popupMode && <button type="button" className="btn btn-outline" onClick={handleOpenPopup}>새 창</button>}
                                <button type="button" className="btn btn-secondary" onClick={() => { setShowRoomList(false); setShowDetailPanel(true); }}>대화 정보</button>
                            </div>
                        )}
                    </div>

                    {roomDetail?.pinnedMessage && (
                        <div className="messenger-pin-banner">
                            <div className="messenger-main-title-row">
                                <strong>상단 공지</strong>
                                <span className="room-type-pill">Pin</span>
                            </div>
                            <p>{roomDetail.pinnedMessage.contents}</p>
                            <button type="button" className="btn btn-sm btn-outline" onClick={() => handleJumpToMessage(roomDetail.pinnedMessage.msgContId)}>이동</button>
                        </div>
                    )}

                    {showSearchPanel && activeRoom && (
                        <div className="messenger-search-panel">
                            <div className="messenger-panel-head">
                                <strong>대화 검색</strong>
                                <span className="messenger-count-chip">{searchResults.length}</span>
                            </div>
                            <div className="messenger-search-inline">
                                <input className="form-input" placeholder="메시지, 작성자 검색" value={searchKeyword} onChange={(event) => setSearchKeyword(event.target.value)} />
                                <button type="button" className="btn btn-outline" onClick={handleRoomSearch}>검색</button>
                            </div>
                            <div className="messenger-search-results">
                                {searchResults.length === 0 ? (
                                    <div className="approval-empty compact"><strong>검색 결과가 없습니다.</strong></div>
                                ) : searchResults.map((item) => (
                                    <button key={item.msgContId} type="button" className="messenger-search-item" onClick={() => handleJumpToMessage(item.msgContId)}>
                                        <strong>{item.userNm || item.userId}</strong>
                                        <span>{formatDateTime(item.sendDt)}</span>
                                        <p>{item.contents || (item.msgTypeCd === 'file' ? '파일 공유' : '')}</p>
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    <div className="messenger-message-stream">
                        {!activeRoom ? (
                            <div className="approval-empty"><strong>대화방을 선택하세요.</strong><span>왼쪽 목록에서 방을 선택하면 실시간 대화를 시작할 수 있습니다.</span></div>
                        ) : messages.length === 0 ? (
                            <div className="approval-empty"><strong>아직 메시지가 없습니다.</strong><span>하단 입력창 또는 파일 업로드로 대화를 시작하세요.</span></div>
                        ) : messages.map((message, index) => {
                            const currentDayKey = formatDayKey(message.sendDt);
                            const previousDayKey = index > 0 ? formatDayKey(messages[index - 1].sendDt) : '';
                            const isMine = message.userId === currentUser?.userId;
                            const isSystem = message.msgTypeCd === 'system';
                            const canDelete = isMine || roomDetail?.currentUserRole === 'owner';
                            return (
                                <div key={message.msgContId || `${message.userId}-${message.sendDt}-${index}`}>
                                    {currentDayKey !== previousDayKey && <div className="messenger-day-divider"><span>{currentDayKey}</span></div>}
                                    <div
                                        ref={(node) => { if (message.msgContId) messageRefs.current[message.msgContId] = node; }}
                                        className={`messenger-message-row ${isMine ? 'mine' : 'theirs'} ${highlightedMessageId === message.msgContId ? 'highlighted' : ''} ${isSystem ? 'system' : ''}`}
                                    >
                                        {isSystem ? (
                                            <div className="messenger-system-message">{message.contents}</div>
                                        ) : (
                                            <>
                                                {!isMine && <div className="messenger-message-avatar">{message.userNm?.slice(0, 1) || '?'}</div>}
                                                <div className="messenger-message-stack">
                                                    {!isMine && <div className="messenger-message-meta"><strong>{message.userNm}</strong><span>{[message.deptNm, message.jbgdNm].filter(Boolean).join(' / ')}</span></div>}
                                                    <div className={`messenger-bubble ${isMine ? 'mine' : 'theirs'}`}>
                                                        {message.forwardPreview && <div className="messenger-forward-chip">전달됨: {message.forwardPreview}</div>}
                                                        {message.contents && <div className="messenger-bubble-text">{message.contents}</div>}
                                                        {asArray(message.attachments).length > 0 && (
                                                            <div className="messenger-attachment-list">
                                                                {message.attachments.map((attachment) => (
                                                                    <a key={`${message.msgContId}-${attachment.fileSeq}`} href={fileDownloadUrl(attachment.saveFileNm)} className="messenger-attachment-item">
                                                                        <strong>{attachment.orgnFileNm || attachment.saveFileNm}</strong>
                                                                        <span>{attachment.fileSize ? `${Math.round(attachment.fileSize / 1024)} KB` : '파일'}</span>
                                                                    </a>
                                                                ))}
                                                            </div>
                                                        )}
                                                    </div>
                                                    <div className="messenger-message-foot">
                                                        <span>{formatDateTime(message.sendDt)}</span>
                                                        {message.unreadCount > 0 && <span>미읽음 {message.unreadCount}</span>}
                                                        <button type="button" className="feed-action-button" onClick={() => setMessageMenuId((current) => current === message.msgContId ? '' : message.msgContId)}>더보기</button>
                                                    </div>
                                                    {messageMenuId === message.msgContId && (
                                                        <div className="messenger-message-menu">
                                                            <button type="button" onClick={() => { setForwardingMessageId(message.msgContId); setMessageMenuId(''); }}>전달</button>
                                                            <button type="button" onClick={() => handleTogglePin(message)}>{roomDetail?.room?.pinnedMsgContId === message.msgContId ? '공지 해제' : '상단 공지'}</button>
                                                            {canDelete && <button type="button" className="danger" onClick={() => handleDeleteMessage(message)}>삭제</button>}
                                                        </div>
                                                    )}
                                                </div>
                                            </>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>

                    <form className="messenger-composer" onSubmit={handleSendMessage}>
                        <div className="messenger-composer-tools">
                            <div className="messenger-composer-actions">
                                <button type="button" className="btn btn-outline" onClick={() => fileInputRef.current?.click()}>파일 첨부</button>
                                <button type="button" className="btn btn-outline" onClick={() => setShowEmojiPicker((current) => !current)}>이모지</button>
                                {selectedFiles.length > 0 && <button type="button" className="btn btn-secondary" onClick={handleUploadFiles} disabled={saving}>파일 전송</button>}
                            </div>
                            {activeRoom && <span className="messenger-drop-hint">파일을 끌어 놓아 바로 첨부할 수 있습니다.</span>}
                        </div>
                        {showEmojiPicker && (
                            <div className="messenger-emoji-panel">
                                {recentEmojis.length > 0 && <div className="messenger-emoji-group"><strong>최근 사용</strong><div className="messenger-emoji-grid">{recentEmojis.map((emoji) => <button key={`recent-${emoji}`} type="button" onClick={() => handleEmojiSelect(emoji)}>{emoji}</button>)}</div></div>}
                                <div className="messenger-emoji-group"><strong>기본 이모지</strong><div className="messenger-emoji-grid">{EMOJI_LIST.map((emoji) => <button key={emoji} type="button" onClick={() => handleEmojiSelect(emoji)}>{emoji}</button>)}</div></div>
                            </div>
                        )}
                        {selectedFiles.length > 0 && <div className="messenger-selected-files">{selectedFiles.map((file) => <span key={`${file.name}-${file.size}`}>{file.name}</span>)}<button type="button" className="btn btn-sm btn-outline" onClick={() => { setSelectedFiles([]); if (fileInputRef.current) fileInputRef.current.value = ''; }}>비우기</button></div>}
                        <div className="messenger-composer-row">
                            <textarea className="form-input messenger-textarea" placeholder={activeRoom ? '메시지를 입력하세요. Enter 전송 / Shift+Enter 줄바꿈' : '먼저 대화방을 선택하세요.'} value={messageInput} onChange={(event) => setMessageInput(event.target.value)} onKeyDown={handleMessageKeyDown} disabled={!activeRoom || !connected} />
                            <button type="submit" className="btn btn-primary" disabled={!activeRoom || !connected || !messageInput.trim()}>전송</button>
                        </div>
                        <input ref={fileInputRef} type="file" multiple hidden onChange={(event) => setSelectedFiles(Array.from(event.target.files || []))} />
                    </form>
                </section>

                <aside className={`messenger-detail-panel ${showDetailPanel ? 'open' : ''}`}>
                    <div className="messenger-detail-drawer">
                        <div className="messenger-detail-header">
                            <div>
                                <span className="messenger-sidebar-kicker">DETAIL</span>
                                <h3>{activeRoom?.msgrNm || '대화 정보'}</h3>
                                {activeRoom && <p>{roomTypeLabel(activeRoom.roomTypeCd)} · {roomDetail?.participants?.length || 0}명</p>}
                            </div>
                            <button type="button" className="btn btn-sm btn-outline" onClick={() => setShowDetailPanel(false)}>닫기</button>
                        </div>
                        <div className="messenger-detail-content">
                            {!activeRoom ? (
                                <div className="approval-empty compact"><strong>대화방을 선택하세요.</strong></div>
                            ) : (
                                <div className="messenger-detail-sections">
                                    <div className="messenger-detail-section">
                                        <label className="form-label">방 옵션</label>
                                        <div className="messenger-detail-actions">
                                            <button type="button" className="btn btn-outline" onClick={handleToggleNotify}>{roomDetail?.notifyEnabled ? '알림 끄기' : '알림 켜기'}</button>
                                            <button type="button" className="btn btn-outline" onClick={handleRenameRoom}>이름 변경</button>
                                            <button type="button" className="btn btn-outline" onClick={handleExportMessages}>내보내기</button>
                                            {!popupMode && <button type="button" className="btn btn-outline" onClick={handleOpenPopup}>새 창</button>}
                                            <button type="button" className="btn btn-secondary" onClick={handleLeaveRoom}>나가기</button>
                                        </div>
                                    </div>
                                    <div className="messenger-detail-section">
                                        <label className="form-label">참여자</label>
                                        <div className="messenger-participant-list">
                                            {asArray(roomDetail?.participants).map((participant) => (
                                                <div key={participant.userId} className="messenger-participant-item">
                                                    <div><strong>{participant.userNm}</strong><span>{[participant.deptNm, participant.jbgdNm].filter(Boolean).join(' / ')}</span><small>{roleLabel(participant.roleCd)}{participant.me ? ' · 나' : ''}</small></div>
                                                    {roomDetail?.currentUserRole === 'owner' && !participant.me && <button type="button" className="btn btn-sm btn-outline" onClick={() => handleKickUser(participant.userId)}>강퇴</button>}
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                    <div className="messenger-detail-section">
                                        <label className="form-label">사용자 초대</label>
                                        <div className="messenger-check-list compact max-grid">
                                            {inviteCandidates.map((candidate) => (
                                                <label key={candidate.userId} className="messenger-check-item">
                                                    <input type="checkbox" checked={inviteUserIds.includes(candidate.userId)} onChange={() => setInviteUserIds((current) => current.includes(candidate.userId) ? current.filter((item) => item !== candidate.userId) : [...current, candidate.userId])} />
                                                    <span>{candidate.userNm}</span>
                                                    <small>{candidate.deptNm || '-'}</small>
                                                </label>
                                            ))}
                                        </div>
                                        <button type="button" className="btn btn-secondary" onClick={handleInviteUsers} disabled={inviteUserIds.length === 0}>초대 적용</button>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </aside>
            </div>

            {forwardingMessageId && (
                <div className="modal-backdrop" onClick={() => setForwardingMessageId('')}>
                    <div className="modal-card messenger-modal" onClick={(event) => event.stopPropagation()}>
                        <div className="modal-header"><div><h3>메시지 전달</h3><p>대상 대화방을 선택해 전달합니다.</p></div><button type="button" className="btn btn-sm btn-outline" onClick={() => setForwardingMessageId('')}>닫기</button></div>
                        <div className="modal-body messenger-forward-list">
                            {rooms.filter((room) => room.msgrId !== activeRoomId).map((room) => (
                                <button key={room.msgrId} type="button" className="messenger-search-item" onClick={() => handleForwardMessage(room.msgrId)}>
                                    <strong>{room.msgrNm}</strong><span>{roomTypeLabel(room.roomTypeCd)}</span><p>{room.lastMsgCont || '최근 메시지 없음'}</p>
                                </button>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {showCommunityManager && (
                <div className="modal-backdrop" onClick={() => setShowCommunityManager(false)}>
                    <div className="modal-card messenger-community-modal" onClick={(event) => event.stopPropagation()}>
                        <div className="modal-header">
                            <div><h3>커뮤니티 관리</h3><p>커뮤니티를 만들고 멤버를 관리한 뒤 채팅방으로 바로 전환할 수 있습니다.</p></div>
                            <div className="modal-header-actions"><button type="button" className="btn btn-sm btn-outline" onClick={startNewCommunity}>새 커뮤니티</button><button type="button" className="btn btn-sm btn-outline" onClick={() => setShowCommunityManager(false)}>닫기</button></div>
                        </div>
                        <div className="modal-body messenger-community-layout">
                            <div className="messenger-community-list-panel">
                                <div className="messenger-search-inline"><input className="form-input" placeholder="커뮤니티 검색" value={communityKeyword} onChange={(event) => setCommunityKeyword(event.target.value)} /><button type="button" className="btn btn-outline" onClick={() => loadCommunities(communityKeyword)}>검색</button></div>
                                <div className="messenger-room-list compact">
                                    {communities.map((community) => (
                                        <button key={community.communityId} type="button" className={`messenger-room-item ${selectedCommunityId === community.communityId ? 'active' : ''}`} onClick={() => setSelectedCommunityId(community.communityId)}>
                                            <div className="messenger-room-badge">{community.communityNm?.slice(0, 1) || 'C'}</div>
                                            <div className="messenger-room-body">
                                                <div className="messenger-room-head"><strong>{community.communityNm}</strong><span>{community.memberCount || 0}명</span></div>
                                                <div className="messenger-room-meta"><span className="room-type-pill">{community.communityTypeCd || 'group'}</span><span className="messenger-room-snippet">{community.communityDesc || '설명 없음'}</span></div>
                                            </div>
                                        </button>
                                    ))}
                                </div>
                            </div>
                            <div className="messenger-community-detail-panel">
                                <div className="form-grid">
                                    <div className="form-span-2"><label className="form-label">커뮤니티 이름</label><input className="form-input" value={communityForm.communityNm} onChange={(event) => setCommunityForm((current) => ({ ...current, communityNm: event.target.value }))} /></div>
                                    <div className="form-span-2"><label className="form-label">설명</label><textarea className="form-input" value={communityForm.communityDesc} onChange={(event) => setCommunityForm((current) => ({ ...current, communityDesc: event.target.value }))} /></div>
                                </div>
                                <div className="messenger-detail-actions"><button type="button" className="btn btn-primary" onClick={handleSaveCommunity}>저장</button>{selectedCommunity && <button type="button" className="btn btn-secondary" onClick={handleCreateRoomFromCommunity}>채팅방 만들기</button>}{selectedCommunity && <button type="button" className="btn btn-outline" onClick={handleDeleteCommunity}>삭제</button>}</div>
                                <div className="messenger-detail-section">
                                    <label className="form-label">멤버 추가</label>
                                    <div className="messenger-check-list compact max-grid">
                                        {directRoomUsers.filter((candidate) => !communityMembers.some((member) => member.userId === candidate.userId)).map((candidate) => (
                                            <label key={candidate.userId} className="messenger-check-item"><input type="checkbox" checked={communityMemberIds.includes(candidate.userId)} onChange={() => setCommunityMemberIds((current) => current.includes(candidate.userId) ? current.filter((item) => item !== candidate.userId) : [...current, candidate.userId])} /><span>{candidate.userNm}</span><small>{candidate.deptNm || '-'}</small></label>
                                        ))}
                                    </div>
                                    {selectedCommunity && <button type="button" className="btn btn-secondary" onClick={handleAddCommunityMembers} disabled={communityMemberIds.length === 0}>멤버 추가</button>}
                                </div>
                                <div className="messenger-detail-section">
                                    <label className="form-label">현재 멤버</label>
                                    <div className="messenger-participant-list">
                                        {communityMembers.map((member) => (
                                            <div key={member.userId} className="messenger-participant-item"><div><strong>{member.userNm}</strong><span>{[member.deptNm, member.jbgdNm].filter(Boolean).join(' / ')}</span><small>{roleLabel(member.roleCd)}</small></div>{member.roleCd !== 'owner' && selectedCommunity && <button type="button" className="btn btn-sm btn-outline" onClick={() => handleRemoveCommunityMember(member.userId)}>제거</button>}</div>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
