
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { boardAPI, dashboardAPI, messengerAPI } from '../services/api';

const CATEGORY_OPTIONS = [
    { value: 'F101', label: '공지' },
    { value: 'F104', label: '사내소식' },
    { value: 'F102', label: '동호회' },
    { value: 'F103', label: '경조사' },
    { value: 'F105', label: '건의사항' },
    { value: 'F106', label: '기타' },
];

const INTEREST_CATEGORIES = CATEGORY_OPTIONS.filter((item) => item.value !== 'F101');
const SCOPE_OPTIONS = [
    { value: 'all', label: '전체' },
    { value: 'unread', label: '읽지 않은 글' },
    { value: 'saved', label: '저장한 글' },
    { value: 'my-posts', label: '내가 쓴 글' },
    { value: 'commented', label: '댓글 단 글' },
    { value: 'activity', label: '업무활동' },
];

const EMPTY_WIDGETS = { notices: [], sharedSchedules: [], mySchedules: [], quickLinks: [], favoriteUsers: [], todoItems: [] };

function formatDateTime(value) {
    if (!value) return '-';
    return new Date(value).toLocaleString('ko-KR', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
}

function formatRelative(value) {
    if (!value) return '-';
    const diff = Date.now() - new Date(value).getTime();
    const minutes = Math.max(1, Math.round(diff / 60000));
    if (minutes < 60) return `${minutes}분 전`;
    const hours = Math.round(minutes / 60);
    if (hours < 24) return `${hours}시간 전`;
    const days = Math.round(hours / 24);
    return `${days}일 전`;
}

function categoryLabel(code) {
    return CATEGORY_OPTIONS.find((item) => item.value === code)?.label || '게시글';
}

function badgeClass(item) {
    if (item.itemType === 'activity') return 'feed-badge feed-badge-activity';
    const code = item.categoryCode || item.bbsCtgrCd || '';
    if (code === 'F101') return 'feed-badge feed-badge-notice';
    if (item.fixedYn === 'Y') return 'feed-badge feed-badge-important';
    return 'feed-badge feed-badge-general';
}

function WidgetCard({ title, subtitle, items, emptyText, onSelect }) {
    return (
        <div className="feed-widget-card">
            <div className="feed-widget-header">
                <div>
                    <h3>{title}</h3>
                    {subtitle && <p>{subtitle}</p>}
                </div>
            </div>
            <div className="feed-widget-list">
                {items.length === 0 ? (
                    <div className="feed-widget-empty">{emptyText}</div>
                ) : (
                    items.map((item, index) => (
                        <button key={`${item.itemType}-${item.title}-${index}`} type="button" className="feed-widget-item" onClick={() => onSelect(item)}>
                            <div className="feed-widget-item-main">
                                <strong>{item.title}</strong>
                                {item.subtitle && <span>{item.subtitle}</span>}
                                {item.description && <p>{item.description}</p>}
                            </div>
                            <div className="feed-widget-item-meta">
                                {item.badge && <span className="badge badge-gray">{item.badge}</span>}
                                {item.createdAt && <small>{formatDateTime(item.createdAt)}</small>}
                            </div>
                        </button>
                    ))
                )}
            </div>
        </div>
    );
}

export default function DashboardPage() {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [searchParams, setSearchParams] = useSearchParams();
    const [feed, setFeed] = useState(null);
    const [widgets, setWidgets] = useState(EMPTY_WIDGETS);
    const [preferences, setPreferences] = useState(null);
    const [favoriteCategories, setFavoriteCategories] = useState([]);
    const [todos, setTodos] = useState([]);
    const [recommendations, setRecommendations] = useState([]);
    const [feedLoading, setFeedLoading] = useState(true);
    const [metaLoading, setMetaLoading] = useState(true);
    const [error, setError] = useState('');
    const [composeError, setComposeError] = useState('');
    const [composing, setComposing] = useState(false);
    const [searchInput, setSearchInput] = useState(searchParams.get('q') || '');
    const [composeForm, setComposeForm] = useState({ bbsCtgrCd: 'F104', pstTtl: '', contents: '', fixedYn: 'N' });
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState('');
    const [detailPost, setDetailPost] = useState(null);
    const [detailComments, setDetailComments] = useState([]);
    const [profileLoading, setProfileLoading] = useState(false);
    const [profileError, setProfileError] = useState('');
    const [profile, setProfile] = useState(null);
    const [todoDraft, setTodoDraft] = useState({ todoTtl: '', todoCn: '', dueDt: '' });
    const [recommendDraft, setRecommendDraft] = useState({ categoryCodes: [], message: '' });
    const [submittingAction, setSubmittingAction] = useState(false);
    const preferenceSaveTimerRef = useRef(null);

    /* UI-only state for collapsible sections */
    const [sidebarCollapsed, setSidebarCollapsed] = useState({ interest: true, org: true });
    const [composerOpen, setComposerOpen] = useState(false);

    const scope = searchParams.get('scope') || preferences?.defaultScope || 'all';
    const category = searchParams.get('category') || preferences?.defaultCategory || 'all';
    const deptId = searchParams.get('deptId') || preferences?.lastDeptId || '';
    const sort = searchParams.get('sort') || preferences?.defaultSort || 'recent';
    const view = searchParams.get('view') || preferences?.defaultView || 'summary';
    const page = Number(searchParams.get('page') || '1');
    const q = searchParams.get('q') || preferences?.lastSearchQ || '';
    const isAdmin = String(user?.userRole || '').includes('ADMIN');

    const composeCategories = useMemo(
        () => (isAdmin ? CATEGORY_OPTIONS : CATEGORY_OPTIONS.filter((item) => item.value !== 'F101')),
        [isAdmin]
    );

    useEffect(() => {
        setSearchInput(q);
    }, [q]);

    const updateParams = useCallback((nextValues) => {
        const next = new URLSearchParams(searchParams);
        Object.entries(nextValues).forEach(([key, value]) => {
            if (value === undefined || value === null || value === '' || value === 'all' && key !== 'scope') {
                next.delete(key);
            } else {
                next.set(key, String(value));
            }
        });
        if (!next.get('scope')) next.set('scope', 'all');
        setSearchParams(next);
    }, [searchParams, setSearchParams]);

    const loadMeta = useCallback(async () => {
        setMetaLoading(true);
        try {
            const response = await dashboardAPI.bootstrap();
            setPreferences(response.data?.preferences || null);
            setFavoriteCategories(response.data?.categories || []);
            setWidgets(response.data?.widgets || EMPTY_WIDGETS);
            setTodos(response.data?.todos || []);
            setRecommendations(response.data?.recommendations?.items || []);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '대시보드 개인화 정보를 불러오지 못했습니다.');
        } finally {
            setMetaLoading(false);
        }
    }, []);

    const loadFeed = useCallback(async () => {
        setFeedLoading(true);
        setError('');
        try {
            const response = await dashboardAPI.feed({
                scope,
                category,
                ...(deptId ? { deptId } : {}),
                ...(q ? { q } : {}),
                sort: scope === 'activity' ? 'recent' : sort,
                view,
                page,
            });
            setFeed(response.data);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '뉴스피드를 불러오지 못했습니다.');
        } finally {
            setFeedLoading(false);
        }
    }, [scope, category, deptId, q, sort, view, page]);

    useEffect(() => {
        loadMeta();
    }, [loadMeta]);

    useEffect(() => {
        loadFeed();
    }, [loadFeed]);

    useEffect(() => {
        if (metaLoading) return;
        if (
            preferences
            && preferences.defaultScope === scope
            && preferences.defaultSort === sort
            && preferences.defaultView === view
            && preferences.defaultCategory === category
            && (preferences.lastDeptId || '') === deptId
            && (preferences.lastSearchQ || '') === q
        ) {
            return;
        }

        window.clearTimeout(preferenceSaveTimerRef.current);
        preferenceSaveTimerRef.current = window.setTimeout(() => {
            dashboardAPI.savePreferences({
                defaultScope: scope,
                defaultSort: sort,
                defaultView: view,
                defaultCategory: category,
                lastDeptId: deptId || null,
                lastSearchQ: q || '',
            }).then((response) => setPreferences(response.data)).catch(() => { });
        }, 350);

        return () => {
            window.clearTimeout(preferenceSaveTimerRef.current);
        };
    }, [metaLoading, preferences, scope, sort, view, category, deptId, q]);

    useEffect(() => () => {
        window.clearTimeout(preferenceSaveTimerRef.current);
    }, []);
    async function handleComposeSubmit(event) {
        event.preventDefault();
        if (!composeForm.pstTtl.trim() || !composeForm.contents.trim()) {
            setComposeError('제목과 내용을 입력해 주세요.');
            return;
        }
        setComposeError('');
        setComposing(true);
        try {
            await boardAPI.create(composeForm);
            setComposeForm({ bbsCtgrCd: composeForm.bbsCtgrCd, pstTtl: '', contents: '', fixedYn: 'N' });
            updateParams({ scope: 'all', category: 'all', page: 1 });
            await Promise.all([loadFeed(), loadMeta()]);
        } catch (requestError) {
            setComposeError(requestError.response?.data?.message || '게시글 등록에 실패했습니다.');
        } finally {
            setComposing(false);
        }
    }

    async function openBoardDetail(pstId) {
        setDetailLoading(true);
        setDetailError('');
        try {
            await Promise.allSettled([dashboardAPI.markRead(pstId), boardAPI.incrementView(pstId)]);
            const [detailResponse, commentResponse] = await Promise.all([
                boardAPI.detail(pstId),
                boardAPI.comments(pstId),
            ]);
            setDetailPost(detailResponse.data);
            setDetailComments((commentResponse.data || []).filter((comment) => comment.delYn !== 'Y'));
            await loadFeed();
        } catch (requestError) {
            setDetailError(requestError.response?.data?.message || '게시글 상세를 불러오지 못했습니다.');
        } finally {
            setDetailLoading(false);
        }
    }

    async function handleToggleSavePost(item) {
        try {
            if (item.saved) {
                await dashboardAPI.unsavePost(item.feedId);
            } else {
                await dashboardAPI.savePost(item.feedId);
            }
            await loadFeed();
        } catch (requestError) {
            setError(requestError.response?.data?.message || '저장 상태를 변경하지 못했습니다.');
        }
    }

    async function handleToggleFavoriteCategory(categoryCode) {
        const next = favoriteCategories.includes(categoryCode)
            ? favoriteCategories.filter((value) => value !== categoryCode)
            : [...favoriteCategories, categoryCode];
        try {
            const response = await dashboardAPI.saveCategories(next);
            setFavoriteCategories(response.data?.categories || next);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '관심 커뮤니티를 저장하지 못했습니다.');
        }
    }

    async function openProfile(userId) {
        if (!userId) return;
        setProfileLoading(true);
        setProfileError('');
        setTodoDraft({ todoTtl: '', todoCn: '', dueDt: '' });
        setRecommendDraft({ categoryCodes: [], message: '' });
        try {
            const response = await dashboardAPI.profile(userId);
            setProfile(response.data);
        } catch (requestError) {
            setProfileError(requestError.response?.data?.message || '프로필 정보를 불러오지 못했습니다.');
        } finally {
            setProfileLoading(false);
        }
    }

    async function handleToggleFavoriteUser(targetUserId, favorite) {
        try {
            if (favorite) {
                await dashboardAPI.removeFavoriteUser(targetUserId);
            } else {
                await dashboardAPI.addFavoriteUser(targetUserId);
            }
            await loadMeta();
            if (profile?.user?.userId === targetUserId) {
                await openProfile(targetUserId);
            }
        } catch (requestError) {
            setProfileError(requestError.response?.data?.message || '즐겨찾기 상태를 변경하지 못했습니다.');
        }
    }

    async function handleCreateTodoForProfile(event) {
        event.preventDefault();
        if (!profile?.user?.userId || !todoDraft.todoTtl.trim()) return;
        setSubmittingAction(true);
        try {
            await dashboardAPI.createTodo({
                targetUserId: profile.user.userId,
                todoTtl: todoDraft.todoTtl,
                todoCn: todoDraft.todoCn,
                dueDt: todoDraft.dueDt ? new Date(todoDraft.dueDt).toISOString() : null,
            });
            await loadMeta();
            setTodoDraft({ todoTtl: '', todoCn: '', dueDt: '' });
        } catch (requestError) {
            setProfileError(requestError.response?.data?.message || '할 일을 저장하지 못했습니다.');
        } finally {
            setSubmittingAction(false);
        }
    }

    async function handleCreateRecommendation(event) {
        event.preventDefault();
        if (!profile?.user?.userId || recommendDraft.categoryCodes.length === 0) return;
        setSubmittingAction(true);
        try {
            await dashboardAPI.createRecommendations(profile.user.userId, recommendDraft.categoryCodes, recommendDraft.message);
            await loadMeta();
            setRecommendDraft({ categoryCodes: [], message: '' });
        } catch (requestError) {
            setProfileError(requestError.response?.data?.message || '커뮤니티 추천을 저장하지 못했습니다.');
        } finally {
            setSubmittingAction(false);
        }
    }

    async function handleRecommendationAction(recommendId, acceptedYn) {
        try {
            await dashboardAPI.updateRecommendation(recommendId, { acceptedYn, readYn: 'Y' });
            await Promise.all([loadMeta(), loadFeed()]);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '추천 상태를 변경하지 못했습니다.');
        }
    }

    async function handleStartChat(targetUserId) {
        try {
            const response = await messengerAPI.findOrCreate(targetUserId);
            navigate(`/messenger${response.data?.msgrId ? `?room=${response.data.msgrId}` : ''}`);
        } catch (requestError) {
            setProfileError(requestError.response?.data?.message || '대화방을 열지 못했습니다.');
        }
    }

    function handleFeedItemClick(item) {
        if (item.itemType === 'board') {
            openBoardDetail(item.feedId);
            return;
        }
        navigate(item.route || '/');
    }

    const counts = feed?.counts || {};
    const feedItems = feed?.items || [];
    const departments = feed?.departments || [];
    const favoriteCategoryOptions = INTEREST_CATEGORIES.filter((item) => favoriteCategories.includes(item.value));

    return (
        <div className="newsfeed-shell animate-slide-up">
            {/* ═══════════ LEFT SIDEBAR ═══════════ */}
            <aside className="newsfeed-sidebar-left">
                <div className="feed-sidebar-card">
                    <h3 className="feed-sidebar-title">피드 범위</h3>
                    <div className="feed-filter-stack">
                        {SCOPE_OPTIONS.map((option) => (
                            <button key={option.value} type="button" className={`feed-filter-button ${scope === option.value ? 'active' : ''}`} onClick={() => updateParams({ scope: option.value, page: 1, sort: option.value === 'activity' ? 'recent' : sort, category: option.value === 'activity' ? 'activity' : category === 'activity' ? 'all' : category })}>
                                <span>{option.label}</span>
                                <strong>{counts[option.value] || 0}</strong>
                            </button>
                        ))}
                    </div>
                </div>

                <div className="feed-sidebar-card">
                    <h3 className="feed-sidebar-title">분류</h3>
                    <div className="feed-filter-stack">
                        <button type="button" className={`feed-filter-button ${category === 'all' ? 'active' : ''}`} onClick={() => updateParams({ category: 'all', page: 1 })}><span>전체 분류</span><strong>{counts.all || 0}</strong></button>
                        {CATEGORY_OPTIONS.map((option) => (
                            <div key={option.value} className="feed-filter-row">
                                <button type="button" className={`feed-filter-button ${category === option.value ? 'active' : ''}`} onClick={() => updateParams({ scope: option.value === 'activity' ? 'activity' : scope, category: option.value, page: 1 })}>
                                    <span>{option.label}</span>
                                    <strong>{counts[option.value] || 0}</strong>
                                </button>
                                {option.value !== 'F101' && <button type="button" className={`feed-pin-button ${favoriteCategories.includes(option.value) ? 'active' : ''}`} onClick={() => handleToggleFavoriteCategory(option.value)}>★</button>}
                            </div>
                        ))}
                    </div>
                </div>

                <div className="feed-sidebar-card">
                    <button type="button" className="feed-collapse-toggle" onClick={() => setSidebarCollapsed((c) => ({ ...c, interest: !c.interest }))}>
                        <h3 className="feed-sidebar-title">관심 커뮤니티</h3>
                        <span className={`feed-collapse-arrow ${sidebarCollapsed.interest ? '' : 'open'}`}>▸</span>
                    </button>
                    {!sidebarCollapsed.interest && (
                        <div className="feed-chip-list">
                            {favoriteCategoryOptions.length === 0 ? <div className="feed-sidebar-empty">저장한 관심 분류가 없습니다.</div> : favoriteCategoryOptions.map((item) => (
                                <button key={item.value} type="button" className="feed-chip" onClick={() => updateParams({ scope: 'all', category: item.value, page: 1 })}>{item.label}</button>
                            ))}
                        </div>
                    )}
                </div>

                <div className="feed-sidebar-card">
                    <button type="button" className="feed-collapse-toggle" onClick={() => setSidebarCollapsed((c) => ({ ...c, org: !c.org }))}>
                        <h3 className="feed-sidebar-title">조직</h3>
                        <span className={`feed-collapse-arrow ${sidebarCollapsed.org ? '' : 'open'}`}>▸</span>
                    </button>
                    {!sidebarCollapsed.org && (
                        <select className="form-input" value={deptId} onChange={(event) => updateParams({ deptId: event.target.value, page: 1 })}>
                            <option value="">전체 부서</option>
                            {departments.map((department) => <option key={department.deptId} value={department.deptId}>{department.deptNm}</option>)}
                        </select>
                    )}
                </div>
            </aside>

            {/* ═══════════ CENTER FEED ═══════════ */}
            <main className="newsfeed-main">
                {/* Toolbar: single compact row */}
                <section className="feed-toolbar-card">
                    <div className="feed-toolbar-row">
                        <form className="feed-search-form" onSubmit={(event) => { event.preventDefault(); updateParams({ q: searchInput, page: 1 }); }}>
                            <input className="form-input" type="search" placeholder="검색" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
                            <button type="submit" className="btn btn-outline btn-sm">검색</button>
                        </form>
                        <div className="feed-toolbar-actions">
                            <div className="feed-toggle-group">
                                <button type="button" className={`feed-toggle-button ${view === 'summary' ? 'active' : ''}`} onClick={() => updateParams({ view: 'summary' })}>요약형</button>
                                <button type="button" className={`feed-toggle-button ${view === 'expanded' ? 'active' : ''}`} onClick={() => updateParams({ view: 'expanded' })}>확장형</button>
                            </div>
                            <div className="feed-toggle-group">
                                <button type="button" className={`feed-toggle-button ${sort === 'recent' ? 'active' : ''}`} onClick={() => updateParams({ sort: 'recent', page: 1 })}>등록순</button>
                                <button type="button" className={`feed-toggle-button ${sort === 'reaction' ? 'active' : ''}`} onClick={() => updateParams({ sort: 'reaction', page: 1 })} disabled={scope === 'activity'}>반응순</button>
                            </div>
                        </div>
                    </div>
                </section>

                {error && <div className="card"><div className="card-body" style={{ color: 'var(--danger)' }}>{error}</div></div>}

                {/* Feed timeline */}
                <section className="feed-list-section">
                    {feedLoading ? <div className="feed-empty">뉴스피드를 불러오는 중입니다.</div> : feedItems.length === 0 ? <div className="feed-empty">조건에 맞는 글이나 활동이 없습니다.</div> : (
                        <div className="feed-card-list">
                            {feedItems.map((item) => (
                                <div key={item.feedId} className={`feed-card ${item.itemType}`}>
                                    {/* Row 1: badge + author + time */}
                                    <div className="feed-card-top">
                                        <div className="feed-card-top-left">
                                            <span className={badgeClass(item)}>{item.badge || item.categoryLabel}</span>
                                            <button type="button" className="feed-author-link" onClick={() => openProfile(item.actorUserId)}>
                                                <span className="feed-avatar-sm">{(item.actorName || '?').slice(0, 1)}</span>
                                                <strong>{item.actorName || '알 수 없음'}</strong>
                                            </button>
                                            <span className="feed-card-dept">{[item.actorDeptName, item.actorJobGradeName].filter(Boolean).join(' · ')}</span>
                                        </div>
                                        <small className="feed-card-time">{formatRelative(item.createdAt)}</small>
                                    </div>
                                    {/* Row 2-3: title + preview */}
                                    <button type="button" className="feed-card-title-button" onClick={() => handleFeedItemClick(item)}>
                                        <h4>{item.title}</h4>
                                        <p className={`feed-card-preview ${view}`}>{item.bodyPreview || '내용이 없습니다.'}</p>
                                    </button>
                                    {/* Row 4: stats + actions */}
                                    <div className="feed-card-footer">
                                        <div className="feed-card-stats">
                                            <span className="feed-read-status">{item.itemType === 'board' ? (item.read ? '읽음' : '읽지 않음') : '업무활동'}</span>
                                            {item.itemType === 'board' && <span>💬 {item.commentCount || 0}</span>}
                                            {item.itemType === 'board' && <span>👁 {item.viewCount || 0}</span>}
                                        </div>
                                        <div className="feed-action-row compact">
                                            {item.itemType === 'board' && <button type="button" className={`feed-action-button ${item.saved ? 'active' : ''}`} onClick={() => handleToggleSavePost(item)}>{item.saved ? '저장 해제' : '저장'}</button>}
                                            <button type="button" className="feed-action-button" onClick={() => handleFeedItemClick(item)}>{item.itemType === 'board' ? '상세' : '이동'}</button>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </section>

                <div className="feed-pagination">
                    <button type="button" className="btn btn-outline btn-sm" disabled={(feed?.page || 1) <= 1} onClick={() => updateParams({ page: Math.max(1, (feed?.page || 1) - 1) })}>이전</button>
                    <span>{feed?.page || 1} / {feed?.totalPages || 1}</span>
                    <button type="button" className="btn btn-outline btn-sm" disabled={(feed?.page || 1) >= (feed?.totalPages || 1)} onClick={() => updateParams({ page: Math.min(feed?.totalPages || 1, (feed?.page || 1) + 1) })}>다음</button>
                </div>

                {/* Quick composer — collapsible */}
                <section className="feed-composer-card">
                    <button type="button" className="feed-composer-toggle" onClick={() => setComposerOpen(!composerOpen)}>
                        <span>✏️ 빠른 글쓰기</span>
                        <span className={`feed-collapse-arrow ${composerOpen ? 'open' : ''}`}>▸</span>
                    </button>
                    {composerOpen && (
                        <form className="feed-composer-form" onSubmit={handleComposeSubmit}>
                            <div className="feed-composer-row">
                                <select className="form-input" value={composeForm.bbsCtgrCd} onChange={(event) => setComposeForm((current) => ({ ...current, bbsCtgrCd: event.target.value }))}>
                                    {composeCategories.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
                                </select>
                                {isAdmin && composeForm.bbsCtgrCd === 'F101' && <label className="feed-checkbox"><input type="checkbox" checked={composeForm.fixedYn === 'Y'} onChange={(event) => setComposeForm((current) => ({ ...current, fixedYn: event.target.checked ? 'Y' : 'N' }))} /> 상단 고정</label>}
                            </div>
                            <input className="form-input" type="text" placeholder="제목" value={composeForm.pstTtl} onChange={(event) => setComposeForm((current) => ({ ...current, pstTtl: event.target.value }))} />
                            <textarea className="form-input feed-composer-textarea" rows="3" placeholder="내용을 입력해 주세요." value={composeForm.contents} onChange={(event) => setComposeForm((current) => ({ ...current, contents: event.target.value }))} />
                            {composeError && <div className="form-error">{composeError}</div>}
                            <div className="feed-composer-actions"><button type="submit" className="btn btn-primary btn-sm" disabled={composing}>{composing ? '등록 중...' : '게시하기'}</button></div>
                        </form>
                    )}
                </section>
            </main>

            {/* ═══════════ RIGHT SIDEBAR ═══════════ */}
            <aside className="newsfeed-sidebar-right">
                {/* 1. 공지사항 — top priority */}
                <WidgetCard title="공지사항" items={widgets.notices || []} emptyText={metaLoading ? '불러오는 중...' : '등록된 공지가 없습니다.'} onSelect={(item) => navigate(item.route || '/board')} />

                {/* 2. 일정 (전체 + 나의 일정 통합) */}
                <div className="feed-widget-card">
                    <div className="feed-widget-header"><div><h3>일정</h3></div></div>
                    <div className="feed-schedule-group">
                        <div className="feed-schedule-section">
                            <span className="feed-schedule-label">전체 일정</span>
                            <div className="feed-widget-list">
                                {(widgets.sharedSchedules || []).length === 0
                                    ? <div className="feed-widget-empty">{metaLoading ? '불러오는 중...' : '다가오는 일정이 없습니다.'}</div>
                                    : (widgets.sharedSchedules || []).map((item, idx) => (
                                        <button key={`shared-${idx}`} type="button" className="feed-widget-item" onClick={() => navigate(item.route || '/calendar')}>
                                            <div className="feed-widget-item-main"><strong>{item.title}</strong>{item.subtitle && <span>{item.subtitle}</span>}</div>
                                            <div className="feed-widget-item-meta">{item.createdAt && <small>{formatDateTime(item.createdAt)}</small>}</div>
                                        </button>
                                    ))}
                            </div>
                        </div>
                        <div className="feed-schedule-section">
                            <span className="feed-schedule-label">나의 일정</span>
                            <div className="feed-widget-list">
                                {(widgets.mySchedules || []).length === 0
                                    ? <div className="feed-widget-empty">{metaLoading ? '불러오는 중...' : '나의 일정이 없습니다.'}</div>
                                    : (widgets.mySchedules || []).map((item, idx) => (
                                        <button key={`my-${idx}`} type="button" className="feed-widget-item" onClick={() => navigate(item.route || '/calendar')}>
                                            <div className="feed-widget-item-main"><strong>{item.title}</strong>{item.subtitle && <span>{item.subtitle}</span>}</div>
                                            <div className="feed-widget-item-meta">{item.createdAt && <small>{formatDateTime(item.createdAt)}</small>}</div>
                                        </button>
                                    ))}
                            </div>
                        </div>
                    </div>
                </div>

                {/* 3. 할 일 */}
                <div className="feed-widget-card">
                    <div className="feed-widget-header"><div><h3>할 일</h3></div></div>
                    <div className="feed-widget-list">
                        {todos.length === 0 ? <div className="feed-widget-empty">등록한 할 일이 없습니다.</div> : todos.slice(0, 6).map((todo) => (
                            <div key={todo.todoId} className="feed-inline-item">
                                <div><strong>{todo.todoTtl}</strong><small>{todo.targetUserName ? `${todo.targetUserName} · ` : ''}{todo.dueDt ? formatDateTime(todo.dueDt) : '기한 없음'}</small></div>
                                <button type="button" className="feed-action-button" onClick={async () => { await dashboardAPI.deleteTodo(todo.todoId); await loadMeta(); }}>삭제</button>
                            </div>
                        ))}
                    </div>
                </div>

                {/* 4. 바로가기 + 즐겨찾기 사용자 (lighter group) */}
                <div className="feed-widget-card feed-widget-light">
                    <div className="feed-widget-header"><div><h3>바로가기</h3></div></div>
                    <div className="feed-widget-list">
                        {(widgets.quickLinks || []).length === 0
                            ? <div className="feed-widget-empty">바로가기가 없습니다.</div>
                            : (widgets.quickLinks || []).map((item, idx) => (
                                <button key={`ql-${idx}`} type="button" className="feed-widget-item" onClick={() => navigate(item.route || '/')}>
                                    <div className="feed-widget-item-main"><strong>{item.title}</strong></div>
                                </button>
                            ))}
                    </div>
                </div>

                <div className="feed-widget-card feed-widget-light">
                    <div className="feed-widget-header"><div><h3>즐겨찾기 사용자</h3></div></div>
                    <div className="feed-widget-list">
                        {(widgets.favoriteUsers || []).length === 0 ? <div className="feed-widget-empty">즐겨찾기한 사용자가 없습니다.</div> : widgets.favoriteUsers.map((favorite) => (
                            <div key={favorite.targetUserId} className="feed-user-card">
                                <button type="button" className="feed-author-link" onClick={() => openProfile(favorite.targetUserId)}>
                                    <span className="feed-avatar-sm">{(favorite.userNm || '?').slice(0, 1)}</span>
                                    <strong>{favorite.userNm}</strong>
                                    <span className="feed-card-dept">{[favorite.deptNm, favorite.jbgdNm].filter(Boolean).join(' · ')}</span>
                                </button>
                                <div className="feed-action-row compact">
                                    <button type="button" className="feed-action-button" onClick={() => handleStartChat(favorite.targetUserId)}>채팅</button>
                                    <button type="button" className="feed-action-button" onClick={() => handleToggleFavoriteUser(favorite.targetUserId, true)}>해제</button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* 5. 추천함 (lightest) */}
                <div className="feed-widget-card feed-widget-light">
                    <div className="feed-widget-header"><div><h3>추천함</h3></div></div>
                    <div className="feed-widget-list">
                        {recommendations.length === 0 ? <div className="feed-widget-empty">새 추천이 없습니다.</div> : recommendations.slice(0, 5).map((recommendation) => (
                            <div key={recommendation.recommendId} className="feed-recommend-card">
                                <strong>{recommendation.categoryLabel}</strong>
                                <span>{recommendation.fromUserName} · {recommendation.fromDeptName || '소속 없음'}</span>
                                {recommendation.message && <p>{recommendation.message}</p>}
                                <div className="feed-action-row compact">
                                    <button type="button" className="feed-action-button" onClick={() => handleRecommendationAction(recommendation.recommendId, 'Y')}>수락</button>
                                    <button type="button" className="feed-action-button" onClick={() => handleRecommendationAction(recommendation.recommendId, 'N')}>읽음</button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </aside>

            {/* ═══════════ DETAIL MODAL ═══════════ */}
            {detailPost && (
                <div className="modal-overlay" onClick={() => { setDetailPost(null); setDetailComments([]); setDetailError(''); }}>
                    <div className="modal newsfeed-detail-modal" onClick={(event) => event.stopPropagation()}>
                        <div className="modal-header"><h3>{detailPost.pstTtl}</h3><button type="button" className="btn btn-outline" onClick={() => setDetailPost(null)}>닫기</button></div>
                        <div className="modal-body">
                            {detailLoading ? <div className="feed-empty">게시글을 불러오는 중입니다.</div> : detailError ? <div className="form-error">{detailError}</div> : (
                                <div className="newsfeed-detail-body">
                                    <div className="newsfeed-detail-meta"><span className="badge badge-blue">{categoryLabel(detailPost.bbsCtgrCd)}</span><span>{detailPost.userNm || detailPost.crtUserId}</span><span>{formatDateTime(detailPost.frstCrtDt)}</span></div>
                                    <div className="newsfeed-detail-content" dangerouslySetInnerHTML={{ __html: detailPost.contents || '' }} />
                                    <div className="newsfeed-detail-comments">
                                        <h4>댓글 {detailComments.length}</h4>
                                        {detailComments.length === 0 ? <div className="feed-widget-empty">등록된 댓글이 없습니다.</div> : detailComments.map((comment) => (
                                            <div key={comment.cmntSqn} className="newsfeed-comment"><div className="newsfeed-comment-meta"><strong>{comment.userNm || comment.crtUserId}</strong><span>{formatDateTime(comment.frstCrtDt)}</span></div><p>{comment.contents}</p></div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* ═══════════ PROFILE MODAL ═══════════ */}
            {profile && (
                <div className="modal-overlay" onClick={() => { setProfile(null); setProfileError(''); }}>
                    <div className="modal profile-sheet" onClick={(event) => event.stopPropagation()}>
                        <div className="modal-header"><h3>{profile.user?.userNm} 프로필</h3><button type="button" className="btn btn-outline" onClick={() => setProfile(null)}>닫기</button></div>
                        <div className="modal-body">
                            {profileLoading ? <div className="feed-empty">프로필을 불러오는 중입니다.</div> : (
                                <div className="profile-layout">
                                    <div className="profile-main-card">
                                        <div className="feed-profile-button static"><span className="feed-avatar xl">{(profile.user?.userNm || '?').slice(0, 1)}</span><span><strong>{profile.user?.userNm}</strong><small>{[profile.user?.deptNm, profile.user?.jbgdNm].filter(Boolean).join(' · ')}</small></span></div>
                                        <div className="profile-meta-grid"><span>이메일</span><strong>{profile.user?.userEmail || '-'}</strong><span>휴대폰</span><strong>{profile.user?.userTelno || '-'}</strong><span>내선</span><strong>{profile.user?.extTel || '-'}</strong></div>
                                        <div className="feed-action-row">
                                            <button type="button" className="feed-action-button" onClick={() => handleToggleFavoriteUser(profile.user.userId, profile.favorite)}>{profile.favorite ? '즐겨찾기 해제' : '즐겨찾기 추가'}</button>
                                            <button type="button" className="feed-action-button" onClick={() => handleStartChat(profile.user.userId)}>채팅 시작</button>
                                        </div>
                                        {profileError && <div className="form-error">{profileError}</div>}
                                    </div>

                                    <div className="profile-side-grid">
                                        <section className="feed-widget-card compact-card">
                                            <div className="feed-widget-header"><div><h3>관련 할 일 추가</h3><p>이 사용자를 참조하는 개인 할 일을 남깁니다.</p></div></div>
                                            <form className="feed-composer-form" onSubmit={handleCreateTodoForProfile}>
                                                <input className="form-input" type="text" placeholder="할 일 제목" value={todoDraft.todoTtl} onChange={(event) => setTodoDraft((current) => ({ ...current, todoTtl: event.target.value }))} />
                                                <textarea className="form-input" rows="3" placeholder="메모" value={todoDraft.todoCn} onChange={(event) => setTodoDraft((current) => ({ ...current, todoCn: event.target.value }))} />
                                                <input className="form-input" type="datetime-local" value={todoDraft.dueDt} onChange={(event) => setTodoDraft((current) => ({ ...current, dueDt: event.target.value }))} />
                                                <button type="submit" className="btn btn-primary" disabled={submittingAction}>할 일 저장</button>
                                            </form>
                                        </section>

                                        <section className="feed-widget-card compact-card">
                                            <div className="feed-widget-header"><div><h3>커뮤니티 추천</h3><p>관심 있게 보면 좋을 분류를 제안합니다.</p></div></div>
                                            <form className="feed-composer-form" onSubmit={handleCreateRecommendation}>
                                                <div className="feed-chip-list">
                                                    {INTEREST_CATEGORIES.map((item) => (
                                                        <button key={item.value} type="button" className={`feed-chip ${recommendDraft.categoryCodes.includes(item.value) ? 'active' : ''}`} onClick={() => setRecommendDraft((current) => ({ ...current, categoryCodes: current.categoryCodes.includes(item.value) ? current.categoryCodes.filter((value) => value !== item.value) : [...current.categoryCodes, item.value] }))}>{item.label}</button>
                                                    ))}
                                                </div>
                                                <textarea className="form-input" rows="3" placeholder="추천 메시지" value={recommendDraft.message} onChange={(event) => setRecommendDraft((current) => ({ ...current, message: event.target.value }))} />
                                                <button type="submit" className="btn btn-outline" disabled={submittingAction}>추천 보내기</button>
                                            </form>
                                        </section>
                                    </div>

                                    <section className="feed-widget-card"><div className="feed-widget-header"><div><h3>최근 글</h3></div></div><div className="feed-widget-list">{(profile.recentBoards || []).length === 0 ? <div className="feed-widget-empty">최근 글이 없습니다.</div> : profile.recentBoards.map((board) => <button key={board.pstId} type="button" className="feed-widget-item" onClick={() => openBoardDetail(board.pstId)}><div className="feed-widget-item-main"><strong>{board.pstTtl}</strong><span>{categoryLabel(board.bbsCtgrCd)}</span></div><div className="feed-widget-item-meta"><small>{formatDateTime(board.frstCrtDt)}</small></div></button>)}</div></section>
                                    <section className="feed-widget-card"><div className="feed-widget-header"><div><h3>최근 활동</h3></div></div><div className="feed-widget-list">{(profile.recentActivities || []).length === 0 ? <div className="feed-widget-empty">최근 활동이 없습니다.</div> : profile.recentActivities.map((activity) => <button key={activity.feedId} type="button" className="feed-widget-item" onClick={() => navigate(activity.route || '/')}><div className="feed-widget-item-main"><strong>{activity.title}</strong><span>{activity.badge}</span><p>{activity.bodyPreview}</p></div><div className="feed-widget-item-meta"><small>{formatDateTime(activity.createdAt)}</small></div></button>)}</div></section>
                                    <section className="feed-widget-card"><div className="feed-widget-header"><div><h3>인사 히스토리</h3></div></div><div className="feed-widget-list">{(profile.histories || []).length === 0 ? <div className="feed-widget-empty">이동 이력이 없습니다.</div> : profile.histories.map((history, index) => <div key={`${index}-${history.historyId || 'history'}`} className="feed-inline-item"><div><strong>{history.afterDeptNm || history.beforeDeptNm || '부서 이동'}</strong><small>{formatDateTime(history.crtDt)}</small></div></div>)}</div></section>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
