import { Client } from '@stomp/stompjs';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { alarmAPI, messengerAPI } from '../services/api';

function websocketUrl() {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    return `${protocol}://${window.location.host}/starworks-groupware-websocket`;
}

function formatRelativeDate(value) {
    if (!value) {
        return '-';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '-';
    }

    const now = new Date();
    const diffMinutes = Math.floor((now.getTime() - date.getTime()) / 60000);
    if (diffMinutes < 1) {
        return '방금 전';
    }
    if (diffMinutes < 60) {
        return `${diffMinutes}분 전`;
    }

    const sameDay = date.toDateString() === now.toDateString();
    return sameDay
        ? date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
        : date.toLocaleString('ko-KR', {
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
        });
}

function stripAlarmMessage(value) {
    if (!value) {
        return '새 알림이 도착했습니다.';
    }

    return String(value)
        .replace(/<br\s*\/?>/gi, ' ')
        .replace(/<[^>]+>/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();
}

function normalizeAlarm(item, index = 0) {
    return {
        alarmId: item?.alarmId ?? `temp-${index}`,
        alarmCategory: item?.alarmCategory || '알림',
        alarmMessage: stripAlarmMessage(item?.alarmMessage || item?.template),
        readYn: item?.readYn || 'N',
        createdDt: item?.createdDt || new Date().toISOString(),
        relatedUrl: item?.relatedUrl || '#',
    };
}

function resolveAlarmRoute(relatedUrl) {
    if (!relatedUrl || relatedUrl === '#') {
        return '';
    }

    let pathname = relatedUrl;
    try {
        pathname = new URL(relatedUrl, window.location.origin).pathname;
    } catch {
        pathname = relatedUrl;
    }

    if (pathname.startsWith('/approval')) return '/approval';
    if (pathname.startsWith('/board')) return '/board';
    if (pathname.startsWith('/mail')) return '/email';
    if (pathname.startsWith('/project') || pathname.startsWith('/projects')) return '/project';
    if (pathname.startsWith('/calendar')) return '/calendar';
    if (pathname.startsWith('/meeting')) return '/meeting';
    if (pathname.startsWith('/attendance')) return '/attendance';
    if (pathname.startsWith('/mypage')) return '/mypage';
    return '';
}

function BellIcon() {
    return (
        <svg className="topbar-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M9 18H15M10 21H14M18 16V11C18 7.686 15.314 5 12 5C8.686 5 6 7.686 6 11V16L4 18H20L18 16Z" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
    );
}

function ChatIcon() {
    return (
        <svg className="topbar-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M7 10H17M7 14H13M6 19L3 21V6C3 4.895 3.895 4 5 4H19C20.105 4 21 4.895 21 6V16C21 17.105 20.105 18 19 18H6V19Z" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
    );
}

function GearIcon() {
    return (
        <svg className="topbar-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M12 15.5C13.933 15.5 15.5 13.933 15.5 12C15.5 10.067 13.933 8.5 12 8.5C10.067 8.5 8.5 10.067 8.5 12C8.5 13.933 10.067 15.5 12 15.5Z" stroke="currentColor" strokeWidth="1.8" />
            <path d="M19.4 15A1 1 0 0 0 19.6 16.1L19.7 16.2A1.2 1.2 0 1 1 18 17.9L17.9 17.8A1 1 0 0 0 16.8 17.6A1 1 0 0 0 16.2 18.5V18.8A1.2 1.2 0 1 1 13.8 18.8V18.6A1 1 0 0 0 13.2 17.7A1 1 0 0 0 12.1 17.9L12 18A1.2 1.2 0 1 1 10.3 16.3L10.4 16.2A1 1 0 0 0 10.6 15.1A1 1 0 0 0 9.7 14.5H9.4A1.2 1.2 0 1 1 9.4 12.1H9.6A1 1 0 0 0 10.5 11.5A1 1 0 0 0 10.3 10.4L10.2 10.3A1.2 1.2 0 1 1 11.9 8.6L12 8.7A1 1 0 0 0 13.1 8.9A1 1 0 0 0 13.7 8V7.8A1.2 1.2 0 1 1 16.1 7.8V8A1 1 0 0 0 16.7 8.9A1 1 0 0 0 17.8 8.7L17.9 8.6A1.2 1.2 0 1 1 19.6 10.3L19.5 10.4A1 1 0 0 0 19.3 11.5A1 1 0 0 0 20.2 12.1H20.4A1.2 1.2 0 1 1 20.4 14.5H20.1A1 1 0 0 0 19.4 15Z" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
    );
}

function ChevronIcon() {
    return (
        <svg className="topbar-caret" viewBox="0 0 20 20" fill="none" aria-hidden="true">
            <path d="M5 7.5L10 12.5L15 7.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
    );
}

export default function Topbar() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const clientRef = useRef(null);
    const alarmSubscriptionRef = useRef(null);
    const messageSubscriptionRef = useRef(null);
    const messageGlobalSubscriptionRef = useRef(null);
    const containerRef = useRef(null);
    const [openMenu, setOpenMenu] = useState('');
    const [alarms, setAlarms] = useState([]);
    const [alarmLoading, setAlarmLoading] = useState(false);
    const [alarmError, setAlarmError] = useState('');
    const [markingAllRead, setMarkingAllRead] = useState(false);
    const [messagePanel, setMessagePanel] = useState({ rooms: [], unreadRoomCount: 0, unreadMessageCount: 0 });
    const [messageLoading, setMessageLoading] = useState(false);
    const [messageError, setMessageError] = useState('');

    const initials = user?.userNm ? user.userNm.charAt(0) : '?';
    const unreadAlarmCount = useMemo(() => alarms.filter((item) => item.readYn !== 'Y').length, [alarms]);
    const unreadMessageCount = messagePanel?.unreadMessageCount || 0;

    const refreshAlarms = useCallback(async (silent = false) => {
        if (!silent) {
            setAlarmLoading(true);
        }
        setAlarmError('');
        try {
            const response = await alarmAPI.top10();
            setAlarms((response.data || []).map(normalizeAlarm));
        } catch (error) {
            setAlarmError(error.response?.data?.message || '알림을 불러오지 못했습니다.');
        } finally {
            if (!silent) {
                setAlarmLoading(false);
            }
        }
    }, []);

    const refreshMessengerPanel = useCallback(async (silent = false) => {
        if (!silent) {
            setMessageLoading(true);
        }
        setMessageError('');
        try {
            const response = await messengerAPI.panel();
            setMessagePanel(response.data || { rooms: [], unreadRoomCount: 0, unreadMessageCount: 0 });
        } catch (error) {
            setMessageError(error.response?.data?.message || '메신저 목록을 불러오지 못했습니다.');
        } finally {
            if (!silent) {
                setMessageLoading(false);
            }
        }
    }, []);

    const connectSocket = useCallback(() => {
        if (!user?.userId || clientRef.current) {
            return;
        }

        const client = new Client({
            brokerURL: websocketUrl(),
            reconnectDelay: 5000,
            debug: () => { },
            onConnect: () => {
                alarmSubscriptionRef.current?.unsubscribe();
                messageSubscriptionRef.current?.unsubscribe();
                messageGlobalSubscriptionRef.current?.unsubscribe();

                alarmSubscriptionRef.current = client.subscribe(`/topic/notify/${user.userId}`, () => {
                    refreshAlarms(true);
                });
                messageSubscriptionRef.current = client.subscribe(`/topic/chat/update/${user.userId}`, () => {
                    refreshMessengerPanel(true);
                });
                messageGlobalSubscriptionRef.current = client.subscribe('/topic/chat/update', () => {
                    refreshMessengerPanel(true);
                });
            },
            onStompError: () => {
                setAlarmError((current) => current || '실시간 연결 중 오류가 발생했습니다.');
                setMessageError((current) => current || '메신저 실시간 연결 중 오류가 발생했습니다.');
            },
        });

        client.activate();
        clientRef.current = client;
    }, [refreshAlarms, refreshMessengerPanel, user?.userId]);

    useEffect(() => {
        if (!user?.userId) {
            setAlarms([]);
            setMessagePanel({ rooms: [], unreadRoomCount: 0, unreadMessageCount: 0 });
            return undefined;
        }

        refreshAlarms();
        refreshMessengerPanel();
        connectSocket();

        return () => {
            alarmSubscriptionRef.current?.unsubscribe();
            messageSubscriptionRef.current?.unsubscribe();
            messageGlobalSubscriptionRef.current?.unsubscribe();
            clientRef.current?.deactivate();
            clientRef.current = null;
        };
    }, [connectSocket, refreshAlarms, refreshMessengerPanel, user?.userId]);

    useEffect(() => {
        const handlePointerDown = (event) => {
            if (!containerRef.current?.contains(event.target)) {
                setOpenMenu('');
            }
        };

        const handleEscape = (event) => {
            if (event.key === 'Escape') {
                setOpenMenu('');
            }
        };

        document.addEventListener('mousedown', handlePointerDown);
        document.addEventListener('keydown', handleEscape);
        return () => {
            document.removeEventListener('mousedown', handlePointerDown);
            document.removeEventListener('keydown', handleEscape);
        };
    }, []);

    async function handleLogout() {
        await logout();
        navigate('/login');
    }

    function handleOpenAlarms() {
        const next = openMenu === 'alarms' ? '' : 'alarms';
        setOpenMenu(next);
        if (next === 'alarms') {
            refreshAlarms();
        }
    }

    function handleOpenMessenger() {
        const next = openMenu === 'messages' ? '' : 'messages';
        setOpenMenu(next);
        if (next === 'messages') {
            refreshMessengerPanel();
        }
    }

    function handleOpenUserMenu() {
        setOpenMenu((current) => (current === 'user' ? '' : 'user'));
    }

    function handleSettings() {
        setOpenMenu('');
        navigate('/mypage');
    }

    async function handleMarkAllRead() {
        if (markingAllRead || unreadAlarmCount === 0) {
            return;
        }
        setMarkingAllRead(true);
        setAlarmError('');
        try {
            await alarmAPI.markAllRead();
            setAlarms((current) => current.map((item) => ({ ...item, readYn: 'Y' })));
        } catch (error) {
            setAlarmError(error.response?.data?.message || '알림 읽음 처리에 실패했습니다.');
        } finally {
            setMarkingAllRead(false);
        }
    }

    function handleAlarmClick(alarm) {
        const route = resolveAlarmRoute(alarm.relatedUrl);
        if (!route) {
            return;
        }
        setOpenMenu('');
        navigate(route);
    }

    async function handleOpenSelfRoom() {
        try {
            const response = await messengerAPI.selfRoom();
            const roomId = response.data?.msgrId;
            setOpenMenu('');
            navigate(roomId ? `/messenger?room=${roomId}` : '/messenger');
        } catch (error) {
            setMessageError(error.response?.data?.message || '나와의 채팅을 열지 못했습니다.');
        }
    }

    function handleOpenRoom(roomId) {
        setOpenMenu('');
        navigate(`/messenger?room=${roomId}`);
    }

    return (
        <header className="topbar">
            <div className="topbar-search">
                <span className="search-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <circle cx="11" cy="11" r="8"></circle>
                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    </svg>
                </span>
                <input type="text" placeholder="메뉴, 문서, 직원명 검색" />
            </div>
            <div className="topbar-actions" ref={containerRef}>
                <div className="topbar-menu">
                    <button
                        type="button"
                        className={`topbar-btn ${openMenu === 'alarms' ? 'active' : ''}`}
                        title="알림"
                        aria-label="알림"
                        aria-expanded={openMenu === 'alarms'}
                        onClick={handleOpenAlarms}
                    >
                        <BellIcon />
                        {unreadAlarmCount > 0 && <span className="topbar-badge">{unreadAlarmCount > 9 ? '9+' : unreadAlarmCount}</span>}
                    </button>
                    {openMenu === 'alarms' && (
                        <div className="topbar-panel" role="dialog" aria-label="최근 알림">
                            <div className="topbar-panel-header">
                                <div>
                                    <strong>최근 알림</strong>
                                    <div className="topbar-panel-subtitle">최근 10건을 표시합니다.</div>
                                </div>
                                <button type="button" className="btn btn-sm btn-outline" onClick={handleMarkAllRead} disabled={markingAllRead || unreadAlarmCount === 0}>
                                    {markingAllRead ? '처리 중...' : '모두 읽음'}
                                </button>
                            </div>
                            <div className="topbar-panel-body">
                                {alarmError && <div className="topbar-panel-status error">{alarmError}</div>}
                                {alarmLoading ? (
                                    <div className="topbar-panel-status">알림을 불러오는 중입니다.</div>
                                ) : alarms.length === 0 ? (
                                    <div className="topbar-panel-empty">
                                        <strong>새 알림이 없습니다.</strong>
                                        <p>결재, 게시판, 일정, 프로젝트 알림이 여기에 표시됩니다.</p>
                                    </div>
                                ) : (
                                    <div className="topbar-panel-list">
                                        {alarms.map((alarm, index) => {
                                            const route = resolveAlarmRoute(alarm.relatedUrl);
                                            return (
                                                <button
                                                    key={`${alarm.alarmId}-${index}`}
                                                    type="button"
                                                    className={`topbar-panel-item ${alarm.readYn !== 'Y' ? 'unread' : ''} ${route ? '' : 'disabled'}`}
                                                    onClick={() => handleAlarmClick(alarm)}
                                                    disabled={!route}
                                                >
                                                    <div className="topbar-panel-item-main">
                                                        <div className="topbar-panel-item-head">
                                                            <span className="topbar-panel-category">{alarm.alarmCategory}</span>
                                                            <span className="topbar-panel-time">{formatRelativeDate(alarm.createdDt)}</span>
                                                        </div>
                                                        <div className="topbar-panel-message">{alarm.alarmMessage}</div>
                                                    </div>
                                                    {alarm.readYn !== 'Y' && <span className="topbar-panel-dot" aria-hidden="true"></span>}
                                                </button>
                                            );
                                        })}
                                    </div>
                                )}
                            </div>
                        </div>
                    )}
                </div>

                <div className="topbar-menu">
                    <button
                        type="button"
                        className={`topbar-btn ${openMenu === 'messages' ? 'active' : ''}`}
                        title="메신저"
                        aria-label="메신저"
                        aria-expanded={openMenu === 'messages'}
                        onClick={handleOpenMessenger}
                    >
                        <ChatIcon />
                        {unreadMessageCount > 0 && <span className="topbar-badge">{unreadMessageCount > 99 ? '99+' : unreadMessageCount}</span>}
                    </button>
                    {openMenu === 'messages' && (
                        <div className="topbar-panel topbar-messenger-panel" role="dialog" aria-label="메신저">
                            <div className="topbar-panel-header">
                                <div>
                                    <strong>메신저</strong>
                                    <div className="topbar-panel-subtitle">최근 대화와 읽지 않은 방을 빠르게 확인합니다.</div>
                                </div>
                                <div className="topbar-inline-actions">
                                    <button type="button" className="btn btn-sm btn-outline" onClick={handleOpenSelfRoom}>나와의 채팅</button>
                                    <button type="button" className="btn btn-sm btn-primary" onClick={() => { setOpenMenu(''); navigate('/messenger'); }}>메신저 열기</button>
                                </div>
                            </div>
                            <div className="topbar-panel-body">
                                {messageError && <div className="topbar-panel-status error">{messageError}</div>}
                                {messageLoading ? (
                                    <div className="topbar-panel-status">대화방 목록을 불러오는 중입니다.</div>
                                ) : messagePanel.rooms?.length ? (
                                    <div className="topbar-panel-list">
                                        {messagePanel.rooms.map((room) => (
                                            <button key={room.msgrId} type="button" className="topbar-panel-item" onClick={() => handleOpenRoom(room.msgrId)}>
                                                <div className="topbar-panel-item-main">
                                                    <div className="topbar-panel-item-head">
                                                        <span className="topbar-panel-category">{room.roomTypeCd === 'private' ? '1:1' : room.roomTypeCd === 'self' ? 'me' : room.roomTypeCd === 'community' ? '커뮤니티' : '그룹'}</span>
                                                        <span className="topbar-panel-time">{formatRelativeDate(room.lastMsgDt || room.crtDt)}</span>
                                                    </div>
                                                    <div className="topbar-panel-message topbar-message-room-title">{room.msgrNm || '이름 없는 대화방'}</div>
                                                    <div className="topbar-panel-message muted">{room.lastMsgCont || '아직 메시지가 없습니다.'}</div>
                                                </div>
                                                {room.unreadCount > 0 && <span className="topbar-panel-dot-count">{room.unreadCount}</span>}
                                            </button>
                                        ))}
                                    </div>
                                ) : (
                                    <div className="topbar-panel-empty">
                                        <strong>표시할 대화가 없습니다.</strong>
                                        <p>메신저에서 1:1 또는 그룹 대화를 시작해 보세요.</p>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}
                </div>

                <button type="button" className="topbar-btn" title="설정" aria-label="설정" onClick={handleSettings}>
                    <GearIcon />
                </button>

                <div className="topbar-menu">
                    <button
                        type="button"
                        className={`topbar-user ${openMenu === 'user' ? 'active' : ''}`}
                        title="내 계정"
                        aria-label="내 계정"
                        aria-expanded={openMenu === 'user'}
                        onClick={handleOpenUserMenu}
                    >
                        <div className="topbar-user-avatar">{initials}</div>
                        <div className="topbar-user-info">
                            <span className="topbar-user-name">{user?.userNm || 'Guest'}</span>
                            <span className="topbar-user-role">{[user?.deptNm, user?.jbgdNm].filter(Boolean).join(' / ')}</span>
                        </div>
                        <ChevronIcon />
                    </button>
                    {openMenu === 'user' && (
                        <div className="topbar-dropdown" role="menu" aria-label="계정 메뉴">
                            <button type="button" className="topbar-dropdown-item" onClick={handleSettings}>마이페이지</button>
                            <button type="button" className="topbar-dropdown-item danger" onClick={handleLogout}>로그아웃</button>
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
}
