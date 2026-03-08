import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { calendarAPI, communityAPI, dashboardAPI, usersAPI } from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';

const VIEW_OPTIONS = [
    ['month', '월간'],
    ['week', '주간'],
    ['list', '목록'],
    ['query', '조회'],
];

const CATEGORY_OPTIONS = [
    ['all', '전체일정'],
    ['my', '나의일정'],
    ['org', '조직일정'],
    ['community', '커뮤니티일정'],
    ['subscription', '구독'],
];

const EMPTY_FORM = {
    source: 'user',
    schdTtl: '',
    schdStrtDt: '',
    schdEndDt: '',
    allday: 'N',
    description: '',
};

const SOURCE_LABELS = {
    user_schedule: '나의 일정',
    dept_schedule: '조직 일정',
    dept_feed: '조직 피드 일정',
    community_schedule: '커뮤니티 일정',
    subscription_schedule: '구독 일정',
    user_todo: '나의 할일',
    team_schedule: '프로젝트 일정',
};

const TYPE_LABELS = {
    user_schedule: '개인 일정',
    dept_schedule: '조직 일정',
    dept_feed: '조직 피드 일정',
    community_schedule: '게시판 일정',
    subscription_schedule: '구독 구성원 일정',
    user_todo: '할일',
    team_schedule: '팀 일정',
};

const pad = (value) => String(value).padStart(2, '0');

const startOfDay = (value) => {
    const date = value instanceof Date ? new Date(value) : new Date(value);
    date.setHours(0, 0, 0, 0);
    return date;
};

const addDays = (value, amount) => {
    const date = value instanceof Date ? new Date(value) : new Date(value);
    date.setDate(date.getDate() + amount);
    return date;
};

const toDateKey = (value) => {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '';
    }
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};

const toLocalInputValue = (value) => {
    if (!value) {
        return '';
    }
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '';
    }
    return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};

const toKoreanDate = (value) => {
    if (!value) {
        return '-';
    }
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '-';
    }
    return new Intl.DateTimeFormat('ko-KR', {
        month: 'long',
        day: 'numeric',
        weekday: 'short',
    }).format(date);
};

const toKoreanDateTime = (value) => {
    if (!value) {
        return '-';
    }
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '-';
    }
    return new Intl.DateTimeFormat('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    }).format(date);
};

const toKoreanTime = (value) => {
    if (!value) {
        return '-';
    }
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '-';
    }
    return new Intl.DateTimeFormat('ko-KR', {
        hour: '2-digit',
        minute: '2-digit',
    }).format(date);
};

const stripHtml = (value) => String(value || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();

const buildDefaultQuery = (value) => {
    const date = value instanceof Date ? value : new Date(value);
    const startDate = new Date(date.getFullYear(), date.getMonth(), 1);
    const endDate = new Date(date.getFullYear(), date.getMonth() + 1, 0);
    return {
        q: '',
        startDate: toDateKey(startDate),
        endDate: toDateKey(endDate),
        communityId: '',
        projectId: '',
        ownerUserId: '',
    };
};

const buildMonthCells = (anchorDate) => {
    const monthStart = new Date(anchorDate.getFullYear(), anchorDate.getMonth(), 1);
    const calendarStart = addDays(monthStart, -monthStart.getDay());
    const todayKey = toDateKey(new Date());

    return Array.from({ length: 42 }, (_, index) => {
        const date = addDays(calendarStart, index);
        return {
            key: toDateKey(date),
            date,
            isCurrentMonth: date.getMonth() === anchorDate.getMonth(),
            isToday: toDateKey(date) === todayKey,
        };
    });
};

const buildWeekDates = (anchorDate) => {
    const startDate = addDays(startOfDay(anchorDate), -startOfDay(anchorDate).getDay());
    return Array.from({ length: 7 }, (_, index) => addDays(startDate, index));
};

const matchesCategory = (category, event) => {
    switch (category) {
        case 'my':
            return event.sourceGroupCd === 'my' || event.sourceGroupCd === 'todo';
        case 'org':
            return event.sourceGroupCd === 'org';
        case 'community':
            return event.sourceGroupCd === 'community';
        case 'subscription':
            return event.sourceGroupCd === 'subscription';
        default:
            return true;
    }
};

const overlapsDay = (event, day) => {
    const start = new Date(event.startDt || event.endDt);
    const end = new Date(event.endDt || event.startDt);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
        return false;
    }
    const rangeStart = startOfDay(day);
    const rangeEnd = addDays(rangeStart, 1);
    return start < rangeEnd && end >= rangeStart;
};

const eventStyle = (event) => {
    const color = event?.colorCd || '#64748b';
    return {
        borderColor: `${color}44`,
        background: `${color}18`,
        color,
    };
};

const sourceLabelOf = (event) => SOURCE_LABELS[event?.sourceCd] || event?.sourceLabel || '일정';
const typeLabelOf = (event) => TYPE_LABELS[event?.sourceCd] || event?.typeLabel || '일정';

const eventRangeText = (event) => {
    if (!event) {
        return '-';
    }
    if (event.alldayYn === 'Y') {
        const startText = toKoreanDate(event.startDt);
        const endText = toKoreanDate(event.endDt || event.startDt);
        return startText === endText ? `${startText} 종일` : `${startText} ~ ${endText}`;
    }
    return `${toKoreanDateTime(event.startDt)} ~ ${toKoreanDateTime(event.endDt || event.startDt)}`;
};

const toRequestDateTime = (value, endOfDay = false) => {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '';
    }
    if (endOfDay) {
        date.setHours(23, 59, 0, 0);
    } else {
        date.setHours(0, 0, 0, 0);
    }
    return toLocalInputValue(date);
};

const fetchCalendarItems = async ({ viewMode, anchorKey, filters }) => {
    const params = {
        view: viewMode,
        anchorDate: anchorKey,
        sourceGroups: 'my,org,community,subscription,todo,team',
    };

    if (viewMode === 'query') {
        if (filters.q) params.q = filters.q;
        if (filters.startDate) params.startDate = filters.startDate;
        if (filters.endDate) params.endDate = filters.endDate;
        if (filters.communityId) params.communityId = Number(filters.communityId);
        if (filters.projectId) params.projectId = filters.projectId;
        if (filters.ownerUserId) params.ownerUserId = filters.ownerUserId;
    }

    const response = await calendarAPI.events(params);
    return response.data?.items || [];
};

export default function CalendarPage() {
    const navigate = useNavigate();
    const { user } = useAuth();

    const [viewMode, setViewMode] = useState('month');
    const [category, setCategory] = useState('all');
    const [anchorDate, setAnchorDate] = useState(() => startOfDay(new Date()));
    const [events, setEvents] = useState([]);
    const [favorites, setFavorites] = useState([]);
    const [communities, setCommunities] = useState([]);
    const [projects, setProjects] = useState([]);
    const [users, setUsers] = useState([]);
    const [queryDraft, setQueryDraft] = useState(() => buildDefaultQuery(new Date()));
    const [queryFilters, setQueryFilters] = useState(() => buildDefaultQuery(new Date()));
    const [selectedEvent, setSelectedEvent] = useState(null);
    const [composeOpen, setComposeOpen] = useState(false);
    const [editingEvent, setEditingEvent] = useState(null);
    const [form, setForm] = useState(EMPTY_FORM);
    const [candidateUserId, setCandidateUserId] = useState('');
    const [loadingMeta, setLoadingMeta] = useState(true);
    const [loadingEvents, setLoadingEvents] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');

    const anchorKey = toDateKey(anchorDate);
    const favoriteIds = useMemo(() => new Set((favorites || []).map((item) => item.targetUserId)), [favorites]);

    const availableUsers = useMemo(
        () => (users || []).filter((item) => item.userId !== user?.userId && !favoriteIds.has(item.userId)),
        [favoriteIds, user?.userId, users],
    );

    const filteredEvents = useMemo(
        () => (events || [])
            .filter((event) => matchesCategory(category, event))
            .sort((left, right) => new Date(left.startDt || 0) - new Date(right.startDt || 0)),
        [category, events],
    );

    const listGroups = useMemo(() => {
        const grouped = new Map();
        filteredEvents.forEach((event) => {
            const key = toDateKey(event.startDt || event.endDt || new Date());
            if (!grouped.has(key)) {
                grouped.set(key, []);
            }
            grouped.get(key).push(event);
        });
        return Array.from(grouped.entries()).sort(([left], [right]) => left.localeCompare(right));
    }, [filteredEvents]);

    const upcomingEvents = useMemo(
        () => filteredEvents.filter((event) => new Date(event.endDt || event.startDt) >= new Date()).slice(0, 5),
        [filteredEvents],
    );

    useEffect(() => {
        void loadMeta();
    }, []);

    useEffect(() => {
        let cancelled = false;

        const run = async () => {
            setLoadingEvents(true);
            try {
                const nextItems = await fetchCalendarItems({ viewMode, anchorKey, filters: queryFilters });
                if (!cancelled) {
                    applyEventItems(nextItems);
                }
            } catch (requestError) {
                if (!cancelled) {
                    setError(requestError.response?.data?.message || '일정 정보를 불러오지 못했습니다.');
                }
            } finally {
                if (!cancelled) {
                    setLoadingEvents(false);
                }
            }
        };

        void run();

        return () => {
            cancelled = true;
        };
    }, [anchorKey, queryFilters, viewMode]);

    function applyEventItems(nextItems) {
        setEvents(nextItems);
        setSelectedEvent((current) => {
            if (!current) {
                return null;
            }
            return nextItems.find((item) => item.eventKey === current.eventKey) || null;
        });
    }

    async function loadMeta() {
        setLoadingMeta(true);
        try {
            const [favoriteRes, communityRes, projectRes, usersRes] = await Promise.all([
                dashboardAPI.favoriteUsers(),
                communityAPI.list({ view: 'joined' }),
                calendarAPI.teamProjects(),
                usersAPI.list(),
            ]);
            setFavorites(favoriteRes.data || []);
            setCommunities(communityRes.data || []);
            setProjects(projectRes.data || []);
            setUsers(usersRes.data || []);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '캘린더 기본 정보를 불러오지 못했습니다.');
        } finally {
            setLoadingMeta(false);
        }
    }

    async function loadEvents(filters = queryFilters) {
        setLoadingEvents(true);
        try {
            const nextItems = await fetchCalendarItems({ viewMode, anchorKey, filters });
            applyEventItems(nextItems);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '일정 정보를 불러오지 못했습니다.');
        } finally {
            setLoadingEvents(false);
        }
    }

    function shiftPeriod(direction) {
        const next = new Date(anchorDate);
        if (viewMode === 'week') {
            next.setDate(next.getDate() + (direction * 7));
        } else {
            next.setMonth(next.getMonth() + direction);
        }
        setAnchorDate(startOfDay(next));
    }

    function openCreate(day = new Date(), source = category === 'org' ? 'dept' : 'user') {
        const start = startOfDay(day);
        start.setHours(9, 0, 0, 0);
        const end = new Date(start);
        end.setHours(10, 0, 0, 0);

        setEditingEvent(null);
        setForm({
            ...EMPTY_FORM,
            source,
            schdStrtDt: toLocalInputValue(start),
            schdEndDt: toLocalInputValue(end),
        });
        setComposeOpen(true);
    }

    function openEdit(event) {
        if (!event?.canEdit) {
            return;
        }
        setEditingEvent(event);
        setForm({
            source: event.sourceCd === 'dept_schedule' ? 'dept' : 'user',
            schdTtl: event.title || '',
            schdStrtDt: toLocalInputValue(event.startDt),
            schdEndDt: toLocalInputValue(event.endDt || event.startDt),
            allday: event.alldayYn || 'N',
            description: stripHtml(event.description),
        });
        setComposeOpen(true);
    }

    function closeCompose() {
        setComposeOpen(false);
        setEditingEvent(null);
        setForm(EMPTY_FORM);
    }

    async function submitForm(submitEvent) {
        submitEvent.preventDefault();
        if (!form.schdTtl.trim()) {
            setError('일정 제목을 입력해 주세요.');
            return;
        }
        if (!form.schdStrtDt || !form.schdEndDt) {
            setError('시작 일시와 종료 일시를 입력해 주세요.');
            return;
        }

        const normalizedStart = form.allday === 'Y' ? toRequestDateTime(form.schdStrtDt) : form.schdStrtDt;
        const normalizedEnd = form.allday === 'Y' ? toRequestDateTime(form.schdEndDt, true) : form.schdEndDt;

        if (normalizedStart > normalizedEnd) {
            setError('종료 일시는 시작 일시보다 늦어야 합니다.');
            return;
        }

        const userPayload = {
            schdTtl: form.schdTtl.trim(),
            schdStrtDt: normalizedStart,
            schdEndDt: normalizedEnd,
            allday: form.allday,
            userSchdExpln: form.description.trim(),
        };

        const deptPayload = {
            schdTtl: form.schdTtl.trim(),
            schdStrtDt: normalizedStart,
            schdEndDt: normalizedEnd,
            allday: form.allday,
            deptSchdExpln: form.description.trim(),
        };

        setSaving(true);
        setError('');

        try {
            if (form.source === 'user') {
                if (editingEvent) {
                    await calendarAPI.updateUser(editingEvent.eventKey, userPayload);
                    setMessage('개인 일정을 수정했습니다.');
                } else {
                    await calendarAPI.createUser(userPayload);
                    setMessage('개인 일정을 등록했습니다.');
                }
            } else if (editingEvent) {
                await calendarAPI.updateDept(editingEvent.eventKey, deptPayload);
                setMessage('조직 일정을 수정했습니다.');
            } else {
                await calendarAPI.createDept(deptPayload);
                setMessage('조직 일정을 등록했습니다.');
            }

            closeCompose();
            await loadEvents(queryFilters);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '일정을 저장하지 못했습니다.');
        } finally {
            setSaving(false);
        }
    }

    async function removeEvent() {
        if (!editingEvent || !window.confirm('이 일정을 삭제하시겠습니까?')) {
            return;
        }
        setSaving(true);
        setError('');
        try {
            if (editingEvent.sourceCd === 'dept_schedule') {
                await calendarAPI.deleteDept(editingEvent.eventKey);
            } else {
                await calendarAPI.deleteUser(editingEvent.eventKey);
            }
            setMessage('일정을 삭제했습니다.');
            closeCompose();
            setSelectedEvent(null);
            await loadEvents(queryFilters);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '일정을 삭제하지 못했습니다.');
        } finally {
            setSaving(false);
        }
    }

    async function toggleFavorite(targetUserId, active) {
        setSaving(true);
        setError('');
        try {
            if (active) {
                await dashboardAPI.removeFavoriteUser(targetUserId);
                setMessage('구독 사용자를 해제했습니다.');
            } else {
                await dashboardAPI.addFavoriteUser(targetUserId);
                setMessage('구독 사용자를 추가했습니다.');
            }
            setCandidateUserId('');
            await Promise.all([loadMeta(), loadEvents(queryFilters)]);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '구독 사용자 설정을 저장하지 못했습니다.');
        } finally {
            setSaving(false);
        }
    }

    function applyQuery(submitEvent) {
        submitEvent.preventDefault();
        setQueryFilters({ ...queryDraft });
        if (queryDraft.startDate) {
            setAnchorDate(startOfDay(new Date(queryDraft.startDate)));
        }
    }

    function resetQuery() {
        const next = buildDefaultQuery(anchorDate);
        setQueryDraft(next);
        setQueryFilters(next);
    }

    function renderChip(event, compact = false) {
        return (
            <button
                key={event.eventKey}
                type="button"
                className={`calendar-event-chip ${compact ? 'compact' : ''}`}
                style={eventStyle(event)}
                onClick={(clickEvent) => {
                    clickEvent.stopPropagation();
                    setSelectedEvent(event);
                }}
            >
                <strong>{event.title}</strong>
                <span>{event.alldayYn === 'Y' ? '종일' : toKoreanTime(event.startDt)}</span>
            </button>
        );
    }

    function renderMonth() {
        return (
            <div className="calendar-month-grid">
                {['일', '월', '화', '수', '목', '금', '토'].map((day) => (
                    <div key={day} className="calendar-month-head">{day}</div>
                ))}
                {buildMonthCells(anchorDate).map((cell) => {
                    const dayEvents = filteredEvents.filter((event) => overlapsDay(event, cell.date));
                    return (
                        <button
                            key={cell.key}
                            type="button"
                            className={`calendar-month-cell ${cell.isCurrentMonth ? '' : 'muted'} ${cell.isToday ? 'today' : ''}`}
                            onClick={() => openCreate(cell.date)}
                        >
                            <div className="calendar-month-date-row">
                                <span className="calendar-month-date">{cell.date.getDate()}</span>
                                {cell.isToday ? <span className="badge badge-blue">오늘</span> : null}
                            </div>
                            <div className="calendar-month-event-list">
                                {dayEvents.slice(0, 4).map((event) => renderChip(event, true))}
                                {dayEvents.length > 4 ? <span className="calendar-more-label">+{dayEvents.length - 4}건 더 있음</span> : null}
                            </div>
                        </button>
                    );
                })}
            </div>
        );
    }

    function renderWeek() {
        return (
            <div className="calendar-week-grid">
                {buildWeekDates(anchorDate).map((day) => {
                    const dayEvents = filteredEvents.filter((event) => overlapsDay(event, day));
                    return (
                        <div key={toDateKey(day)} className="calendar-week-column">
                            <button type="button" className="calendar-week-head" onClick={() => openCreate(day)}>
                                <strong>{toKoreanDate(day)}</strong>
                                <span>{dayEvents.length}건</span>
                            </button>
                            <div className="calendar-week-stack">
                                {dayEvents.length === 0
                                    ? <div className="calendar-empty-inline">등록된 일정이 없습니다.</div>
                                    : dayEvents.map((event) => renderChip(event))}
                            </div>
                        </div>
                    );
                })}
            </div>
        );
    }

    function renderList() {
        if (listGroups.length === 0) {
            return <div className="calendar-empty-panel">선택한 기간에 일정이 없습니다.</div>;
        }

        return (
            <div className="calendar-list-stack">
                {listGroups.map(([groupKey, items]) => (
                    <section key={groupKey} className="calendar-list-day">
                        <div className="calendar-list-head">
                            <h3>{toKoreanDate(groupKey)}</h3>
                            <span>{items.length}건</span>
                        </div>
                        <div className="calendar-list-items">
                            {items.map((event) => (
                                <button key={event.eventKey} type="button" className="calendar-list-item" onClick={() => setSelectedEvent(event)}>
                                    <div className="calendar-list-item-main">
                                        <div className="calendar-list-item-row">
                                            <span className="calendar-dot" style={{ background: event.colorCd || '#64748b' }} />
                                            <strong>{event.title}</strong>
                                            <span className="badge badge-gray">{sourceLabelOf(event)}</span>
                                        </div>
                                        <p>{stripHtml(event.description) || '설명이 없습니다.'}</p>
                                    </div>
                                    <div className="calendar-list-item-meta">
                                        <span>{eventRangeText(event)}</span>
                                        <span>{event.ownerUserNm || event.ownerUserId || '-'}</span>
                                    </div>
                                </button>
                            ))}
                        </div>
                    </section>
                ))}
            </div>
        );
    }

    function renderQuery() {
        return (
            <div className="calendar-query-shell">
                <form className="card" onSubmit={applyQuery}>
                    <div className="card-header">
                        <h3>일정 조회</h3>
                        <div className="calendar-inline-actions">
                            <button type="button" className="btn btn-secondary" onClick={resetQuery}>초기화</button>
                            <button type="submit" className="btn btn-primary">조회</button>
                        </div>
                    </div>
                    <div className="card-body">
                        <div className="calendar-query-grid">
                            <input
                                className="form-input"
                                type="search"
                                placeholder="제목, 설명, 장소 검색"
                                value={queryDraft.q}
                                onChange={(event) => setQueryDraft((current) => ({ ...current, q: event.target.value }))}
                            />
                            <input
                                className="form-input"
                                type="date"
                                value={queryDraft.startDate}
                                onChange={(event) => setQueryDraft((current) => ({ ...current, startDate: event.target.value }))}
                            />
                            <input
                                className="form-input"
                                type="date"
                                value={queryDraft.endDate}
                                onChange={(event) => setQueryDraft((current) => ({ ...current, endDate: event.target.value }))}
                            />
                            <select
                                className="form-input"
                                value={queryDraft.communityId}
                                onChange={(event) => setQueryDraft((current) => ({ ...current, communityId: event.target.value }))}
                            >
                                <option value="">전체 커뮤니티</option>
                                {communities.map((community) => (
                                    <option key={community.communityId} value={community.communityId}>{community.communityNm}</option>
                                ))}
                            </select>
                            <select
                                className="form-input"
                                value={queryDraft.projectId}
                                onChange={(event) => setQueryDraft((current) => ({ ...current, projectId: event.target.value }))}
                            >
                                <option value="">전체 프로젝트</option>
                                {projects.map((project) => (
                                    <option key={`${project.bizId}-${project.bizUserId || 'project'}`} value={project.bizId}>
                                        {project.bizNm || project.bizId}
                                    </option>
                                ))}
                            </select>
                            <select
                                className="form-input"
                                value={queryDraft.ownerUserId}
                                onChange={(event) => setQueryDraft((current) => ({ ...current, ownerUserId: event.target.value }))}
                            >
                                <option value="">전체 작성자</option>
                                {users.map((member) => (
                                    <option key={member.userId} value={member.userId}>{member.userNm || member.userId}</option>
                                ))}
                            </select>
                        </div>
                    </div>
                </form>

                <div className="card">
                    <div className="card-header">
                        <h3>조회 결과</h3>
                        <span className="badge badge-gray">{filteredEvents.length}건</span>
                    </div>
                    <div className="card-body" style={{ padding: 0 }}>
                        {filteredEvents.length === 0 ? (
                            <div className="calendar-empty-panel">조건에 맞는 일정이 없습니다.</div>
                        ) : (
                            <div className="calendar-query-table-wrap">
                                <table className="table calendar-query-table">
                                    <thead>
                                        <tr>
                                            <th>일시</th>
                                            <th>제목</th>
                                            <th>분류</th>
                                            <th>커뮤니티/프로젝트</th>
                                            <th>작성자</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {filteredEvents.map((event) => (
                                            <tr key={event.eventKey} onClick={() => setSelectedEvent(event)}>
                                                <td>{eventRangeText(event)}</td>
                                                <td>
                                                    <div className="calendar-query-title">
                                                        <span className="calendar-dot" style={{ background: event.colorCd || '#64748b' }} />
                                                        <strong>{event.title}</strong>
                                                    </div>
                                                </td>
                                                <td>{sourceLabelOf(event)}</td>
                                                <td>{event.communityNm || event.projectNm || event.deptNm || '-'}</td>
                                                <td>{event.ownerUserNm || event.ownerUserId || '-'}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        );
    }

    const headerTitle = viewMode === 'week'
        ? `${toKoreanDate(addDays(startOfDay(anchorDate), -startOfDay(anchorDate).getDay()))} - ${toKoreanDate(addDays(startOfDay(anchorDate), 6 - startOfDay(anchorDate).getDay()))}`
        : viewMode === 'query'
            ? '상세 일정 조회'
            : new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: 'long' }).format(anchorDate);

    return (
        <div className="animate-slide-up">
            <div className="page-header">
                <div>
                    <h2>캘린더</h2>
                    <p>나의 일정, 조직 일정, 커뮤니티 일정, 구독 일정과 할일을 한 화면에서 확인합니다.</p>
                </div>
                <div className="page-header-actions">
                    <button type="button" className="btn btn-outline" onClick={() => setAnchorDate(startOfDay(new Date()))}>오늘</button>
                    <button type="button" className="btn btn-primary" onClick={() => openCreate(anchorDate)}>일정 등록</button>
                </div>
            </div>

            {message ? (
                <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
                    <div className="card-body" style={{ color: 'var(--success)' }}>{message}</div>
                </div>
            ) : null}

            {error ? (
                <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
                    <div className="card-body" style={{ color: 'var(--danger)' }}>{error}</div>
                </div>
            ) : null}

            <div className="calendar-workspace">
                <aside className="calendar-sidebar">
                    <div className="card">
                        <div className="card-header">
                            <h3>일정 범위</h3>
                        </div>
                        <div className="card-body calendar-sidebar-stack">
                            {CATEGORY_OPTIONS.map(([value, label]) => (
                                <button
                                    key={value}
                                    type="button"
                                    className={`calendar-source-button ${category === value ? 'active' : ''}`}
                                    onClick={() => setCategory(value)}
                                >
                                    <strong>{label}</strong>
                                    <span>{(events || []).filter((event) => matchesCategory(value, event)).length}건</span>
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="card">
                        <div className="card-header">
                            <h3>구독</h3>
                        </div>
                        <div className="card-body calendar-sidebar-stack">
                            <div className="calendar-subscribe-row">
                                <select
                                    className="form-input"
                                    value={candidateUserId}
                                    onChange={(event) => setCandidateUserId(event.target.value)}
                                >
                                    <option value="">구독할 구성원 선택</option>
                                    {availableUsers.map((member) => (
                                        <option key={member.userId} value={member.userId}>{member.userNm || member.userId}</option>
                                    ))}
                                </select>
                                <button
                                    type="button"
                                    className="btn btn-primary"
                                    disabled={!candidateUserId || saving}
                                    onClick={() => toggleFavorite(candidateUserId, false)}
                                >
                                    추가
                                </button>
                            </div>

                            {loadingMeta && favorites.length === 0 ? <div className="calendar-empty-inline">구독 정보를 불러오는 중입니다.</div> : null}

                            <div className="calendar-subscription-list">
                                {favorites.length === 0 ? (
                                    <div className="calendar-empty-inline">구독 중인 구성원이 없습니다.</div>
                                ) : favorites.map((member) => (
                                    <div key={member.targetUserId} className="calendar-subscription-item">
                                        <div>
                                            <strong>{member.userNm || member.targetUserId}</strong>
                                            <span>{[member.deptNm, member.jbgdNm].filter(Boolean).join(' / ') || member.targetUserId}</span>
                                        </div>
                                        <button type="button" className="btn btn-sm btn-outline" onClick={() => toggleFavorite(member.targetUserId, true)}>해제</button>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>

                    <div className="card">
                        <div className="card-header">
                            <h3>다가오는 일정</h3>
                        </div>
                        <div className="card-body calendar-sidebar-stack">
                            {upcomingEvents.length === 0 ? (
                                <div className="calendar-empty-inline">다가오는 일정이 없습니다.</div>
                            ) : upcomingEvents.map((event) => (
                                <button key={event.eventKey} type="button" className="calendar-upcoming-item" onClick={() => setSelectedEvent(event)}>
                                    <div className="calendar-query-title">
                                        <span className="calendar-dot" style={{ background: event.colorCd || '#64748b' }} />
                                        <strong>{event.title}</strong>
                                    </div>
                                    <span>{eventRangeText(event)}</span>
                                </button>
                            ))}
                        </div>
                    </div>
                </aside>

                <section className="calendar-main">
                    <div className="card">
                        <div className="card-header calendar-toolbar">
                            <div className="calendar-toolbar-block">
                                <div className="calendar-inline-actions">
                                    <button type="button" className="btn btn-sm btn-secondary" onClick={() => shiftPeriod(-1)}>이전</button>
                                    <button type="button" className="btn btn-sm btn-secondary" onClick={() => shiftPeriod(1)}>다음</button>
                                </div>
                                <div>
                                    <h3>{headerTitle}</h3>
                                    <p className="calendar-toolbar-caption">
                                        {viewMode === 'query'
                                            ? '기간, 작성자, 커뮤니티, 프로젝트 조건으로 일정을 조회합니다.'
                                            : '날짜를 클릭하면 나의 일정 또는 조직 일정을 바로 등록할 수 있습니다.'}
                                    </p>
                                </div>
                            </div>
                            <div className="calendar-view-tabs">
                                {VIEW_OPTIONS.map(([value, label]) => (
                                    <button
                                        key={value}
                                        type="button"
                                        className={`calendar-view-tab ${viewMode === value ? 'active' : ''}`}
                                        onClick={() => setViewMode(value)}
                                    >
                                        {label}
                                    </button>
                                ))}
                            </div>
                        </div>

                        <div className="card-body">
                            <div className="calendar-legend">
                                <span><i style={{ background: '#ec4899' }} />나의 일정</span>
                                <span><i style={{ background: '#16a34a' }} />조직 일정</span>
                                <span><i style={{ background: '#3b82f6' }} />커뮤니티 일정</span>
                                <span><i style={{ background: '#84cc16' }} />구독 일정</span>
                                <span><i style={{ background: '#38bdf8' }} />나의 할일</span>
                                <span><i style={{ background: '#f97316' }} />프로젝트 일정</span>
                            </div>

                            {loadingEvents ? (
                                <div className="calendar-empty-panel">일정을 불러오는 중입니다.</div>
                            ) : viewMode === 'week' ? (
                                renderWeek()
                            ) : viewMode === 'list' ? (
                                renderList()
                            ) : viewMode === 'query' ? (
                                renderQuery()
                            ) : (
                                renderMonth()
                            )}
                        </div>
                    </div>
                </section>
            </div>

            {composeOpen ? (
                <div className="modal-overlay" onClick={closeCompose}>
                    <div className="modal calendar-modal" onClick={(event) => event.stopPropagation()}>
                        <div className="modal-header">
                            <h3>{editingEvent ? '일정 수정' : '일정 등록'}</h3>
                            <button type="button" className="btn btn-outline" onClick={closeCompose}>닫기</button>
                        </div>

                        <form onSubmit={submitForm}>
                            <div className="modal-body">
                                <div className="calendar-form-grid">
                                    <div className="form-group">
                                        <label className="form-label">일정 구분</label>
                                        <select
                                            className="form-input"
                                            value={form.source}
                                            disabled={Boolean(editingEvent)}
                                            onChange={(event) => setForm((current) => ({ ...current, source: event.target.value }))}
                                        >
                                            <option value="user">나의 일정</option>
                                            <option value="dept">조직 일정</option>
                                        </select>
                                    </div>

                                    <div className="form-group">
                                        <label className="form-label">종일 여부</label>
                                        <select
                                            className="form-input"
                                            value={form.allday}
                                            onChange={(event) => setForm((current) => ({ ...current, allday: event.target.value }))}
                                        >
                                            <option value="N">시간 지정</option>
                                            <option value="Y">종일</option>
                                        </select>
                                    </div>
                                </div>

                                <div className="form-group">
                                    <label className="form-label">제목</label>
                                    <input
                                        className="form-input"
                                        value={form.schdTtl}
                                        onChange={(event) => setForm((current) => ({ ...current, schdTtl: event.target.value }))}
                                    />
                                </div>

                                <div className="calendar-form-grid">
                                    <div className="form-group">
                                        <label className="form-label">시작 일시</label>
                                        <input
                                            type="datetime-local"
                                            className="form-input"
                                            value={form.schdStrtDt}
                                            onChange={(event) => setForm((current) => ({ ...current, schdStrtDt: event.target.value }))}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label">종료 일시</label>
                                        <input
                                            type="datetime-local"
                                            className="form-input"
                                            value={form.schdEndDt}
                                            onChange={(event) => setForm((current) => ({ ...current, schdEndDt: event.target.value }))}
                                        />
                                    </div>
                                </div>

                                <div className="form-group">
                                    <label className="form-label">설명</label>
                                    <textarea
                                        className="form-input"
                                        rows="5"
                                        value={form.description}
                                        onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
                                    />
                                </div>
                            </div>

                            <div className="modal-footer" style={{ justifyContent: 'space-between' }}>
                                <div>
                                    {editingEvent?.canDelete ? (
                                        <button type="button" className="btn btn-outline" onClick={removeEvent}>삭제</button>
                                    ) : null}
                                </div>
                                <div className="calendar-inline-actions">
                                    <button type="button" className="btn btn-secondary" onClick={closeCompose}>취소</button>
                                    <button type="submit" className="btn btn-primary" disabled={saving}>
                                        {saving ? '저장 중...' : editingEvent ? '수정 저장' : '등록'}
                                    </button>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            ) : null}

            {selectedEvent ? (
                <div className="modal-overlay" onClick={() => setSelectedEvent(null)}>
                    <div className="modal calendar-modal" onClick={(event) => event.stopPropagation()}>
                        <div className="modal-header">
                            <div>
                                <h3>{selectedEvent.title}</h3>
                                <p className="calendar-toolbar-caption">{sourceLabelOf(selectedEvent)} · {typeLabelOf(selectedEvent)}</p>
                            </div>
                            <button type="button" className="btn btn-outline" onClick={() => setSelectedEvent(null)}>닫기</button>
                        </div>

                        <div className="modal-body calendar-detail-stack">
                            <div className="calendar-detail-row">
                                <strong>일시</strong>
                                <span>{eventRangeText(selectedEvent)}</span>
                            </div>
                            <div className="calendar-detail-row">
                                <strong>작성자</strong>
                                <span>{selectedEvent.ownerUserNm || selectedEvent.ownerUserId || '-'}</span>
                            </div>
                            <div className="calendar-detail-row">
                                <strong>소속</strong>
                                <span>{selectedEvent.communityNm || selectedEvent.projectNm || selectedEvent.deptNm || '-'}</span>
                            </div>
                            {selectedEvent.placeText ? (
                                <div className="calendar-detail-row">
                                    <strong>장소</strong>
                                    <span>{selectedEvent.placeText}</span>
                                </div>
                            ) : null}
                            {selectedEvent.repeatRule ? (
                                <div className="calendar-detail-row">
                                    <strong>반복</strong>
                                    <span>{selectedEvent.repeatRule}</span>
                                </div>
                            ) : null}
                            {selectedEvent.statusLabel ? (
                                <div className="calendar-detail-row">
                                    <strong>상태</strong>
                                    <span>{selectedEvent.statusLabel === 'done' ? '완료' : selectedEvent.statusLabel === 'open' ? '진행 중' : selectedEvent.statusLabel}</span>
                                </div>
                            ) : null}
                            <div className="calendar-detail-note">
                                <strong>설명</strong>
                                <p>{stripHtml(selectedEvent.description) || '등록된 설명이 없습니다.'}</p>
                            </div>
                        </div>

                        <div className="modal-footer">
                            <div className="calendar-inline-actions">
                                {selectedEvent.detailHref ? (
                                    <button type="button" className="btn btn-outline" onClick={() => navigate(selectedEvent.detailHref)}>원본 게시글 열기</button>
                                ) : null}
                                {selectedEvent.canEdit ? (
                                    <button type="button" className="btn btn-secondary" onClick={() => openEdit(selectedEvent)}>수정</button>
                                ) : null}
                            </div>
                        </div>
                    </div>
                </div>
            ) : null}
        </div>
    );
}
