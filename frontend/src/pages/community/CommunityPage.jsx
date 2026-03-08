import { useEffect, useEffectEvent, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { boardAPI, communityAPI, usersAPI } from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';

const VIEW_OPTIONS = [['joined', '가입'], ['favorites', '관심'], ['discover', '탐색'], ['manageable', '관리']];
const TYPE_OPTIONS = [['general', '일반'], ['notice', '공지']];
const VISIBILITY_OPTIONS = [['public', '전체 공개'], ['private', '비공개']];
const POLICY_OPTIONS = [['instant', '즉시 가입'], ['approval', '승인 후 가입']];
const ROLE_LABELS = { owner: '소유자', operator: '운영자', member: '회원' };
const STATUS_LABELS = { active: '가입', pending: '승인 대기', invited: '초대됨', left: '탈퇴', removed: '제외됨', rejected: '반려' };
const BOARD_TYPE_LABELS = { story: '이야기', poll: '설문', schedule: '일정', todo: '할 일' };

function emptyForm() {
    return {
        communityNm: '',
        communityDesc: '',
        communityTypeCd: 'general',
        visibilityCd: 'public',
        joinPolicyCd: 'instant',
        introText: '',
        postTemplateHtml: '',
    };
}

function asArray(value) {
    return Array.isArray(value) ? value : [];
}

function labelOf(map, value, fallback = '-') {
    return map[value] || fallback;
}

function shortText(value, max = 120) {
    const text = String(value || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
    if (!text) return '내용이 없습니다.';
    return text.length <= max ? text : `${text.slice(0, max)}...`;
}

function fmt(value) {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '-';
    return date.toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

function listParams(view, keyword) {
    const params = keyword ? { q: keyword } : {};
    if (view === 'manageable') return { ...params, view: 'joined', manageable: true };
    return { ...params, view };
}

export default function CommunityPage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const isAdmin = String(user?.userRole || '').includes('ADMIN');
    const [view, setView] = useState('joined');
    const [keyword, setKeyword] = useState('');
    const [communities, setCommunities] = useState([]);
    const [users, setUsers] = useState([]);
    const [selectedId, setSelectedId] = useState('');
    const [detail, setDetail] = useState(null);
    const [members, setMembers] = useState([]);
    const [requests, setRequests] = useState([]);
    const [boardItems, setBoardItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [detailLoading, setDetailLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    const [formOpen, setFormOpen] = useState(false);
    const [editingId, setEditingId] = useState('');
    const [form, setForm] = useState(emptyForm());
    const [operatorIds, setOperatorIds] = useState([]);
    const [memberIds, setMemberIds] = useState([]);
    const [inviteIds, setInviteIds] = useState([]);
    const [iconFile, setIconFile] = useState(null);
    const [coverFile, setCoverFile] = useState(null);

    const currentCommunity = detail?.communityId ? detail : communities.find((item) => String(item.communityId) === String(selectedId)) || null;
    const activeCommunities = useMemo(() => communities.filter((item) => item.memberStatusCd === 'active' && item.communityTypeCd !== 'org'), [communities]);
    const favoriteCommunities = useMemo(() => communities.filter((item) => item.favoriteYn === 'Y'), [communities]);
    const orgCommunities = useMemo(() => communities.filter((item) => item.communityTypeCd === 'org'), [communities]);
    const selectableUsers = useMemo(() => users.filter((item) => item.userId !== user?.userId), [users, user?.userId]);
    const selectedOrderIndex = useMemo(() => activeCommunities.findIndex((item) => String(item.communityId) === String(currentCommunity?.communityId)), [activeCommunities, currentCommunity?.communityId]);

    const loadCommunitiesEffect = useEffectEvent(() => {
        void loadCommunities();
    });

    useEffect(() => {
        loadCommunitiesEffect();
    }, [view, keyword]);

    useEffect(() => {
        if (!selectedId) {
            setDetail(null);
            setMembers([]);
            setRequests([]);
            setBoardItems([]);
            return;
        }
        void loadWorkspace(selectedId);
    }, [selectedId]);

    async function loadCommunities(preferredId = '') {
        setLoading(true);
        setError('');
        try {
            const [communityRes, userRes] = await Promise.all([
                communityAPI.list(listParams(view, keyword)),
                users.length ? Promise.resolve({ data: users }) : usersAPI.list(),
            ]);
            const nextCommunities = asArray(communityRes.data);
            setCommunities(nextCommunities);
            setUsers(asArray(userRes.data));
            const current = preferredId || selectedId;
            const nextSelected = nextCommunities.find((item) => String(item.communityId) === String(current))
                ? String(current)
                : nextCommunities[0]?.communityId ? String(nextCommunities[0].communityId) : '';
            setSelectedId(nextSelected);
            if (!nextSelected) setDetail(null);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '커뮤니티 목록을 불러오지 못했습니다.');
        } finally {
            setLoading(false);
        }
    }

    async function loadWorkspace(communityId) {
        setDetailLoading(true);
        setError('');
        try {
            const communityRes = await communityAPI.detail(communityId);
            const nextDetail = communityRes.data || null;
            setDetail(nextDetail);
            const [memberRes, requestRes, boardRes] = await Promise.all([
                communityAPI.members(communityId, 'active').catch(() => ({ data: [] })),
                nextDetail?.manageable ? communityAPI.requests(communityId).catch(() => ({ data: [] })) : Promise.resolve({ data: [] }),
                boardAPI.workspace({ communityId, page: 1, sort: 'recent' }).catch(() => ({ data: { items: [] } })),
            ]);
            setMembers(asArray(memberRes.data));
            setRequests(asArray(requestRes.data));
            setBoardItems(asArray(boardRes.data?.items).slice(0, 6));
        } catch (requestError) {
            setError(requestError.response?.data?.message || '커뮤니티 상세를 불러오지 못했습니다.');
        } finally {
            setDetailLoading(false);
        }
    }

    function resetForm() {
        setEditingId('');
        setForm(emptyForm());
        setOperatorIds([]);
        setMemberIds([]);
        setInviteIds([]);
        setIconFile(null);
        setCoverFile(null);
        setFormOpen(false);
    }

    function openCreateForm() {
        resetForm();
        setFormOpen(true);
    }

    function openEditForm() {
        if (!currentCommunity || currentCommunity.communityTypeCd === 'org') return;
        setEditingId(String(currentCommunity.communityId));
        setForm({
            communityNm: currentCommunity.communityNm || '',
            communityDesc: currentCommunity.communityDesc || '',
            communityTypeCd: currentCommunity.communityTypeCd || 'general',
            visibilityCd: currentCommunity.visibilityCd === 'private' ? 'private' : 'public',
            joinPolicyCd: currentCommunity.visibilityCd === 'private' ? 'invite_only' : currentCommunity.joinPolicyCd === 'approval' ? 'approval' : 'instant',
            introText: currentCommunity.introText || '',
            postTemplateHtml: currentCommunity.postTemplateHtml || '',
        });
        setOperatorIds(members.filter((item) => item.roleCd === 'operator').map((item) => item.userId));
        setFormOpen(true);
    }

    function toggleSelection(setter, userId, checked) {
        setter((current) => checked ? [...current, userId] : current.filter((item) => item !== userId));
    }

    async function submitForm(event) {
        event.preventDefault();
        if (!form.communityNm.trim() || !form.communityDesc.trim()) {
            setError('커뮤니티명과 설명을 입력하세요.');
            return;
        }
        setSaving(true);
        setError('');
        setMessage('');
        try {
            const payload = {
                ...form,
                operatorUserIds: operatorIds,
                memberUserIds: editingId ? undefined : memberIds,
            };
            const files = { iconFile, coverFile };
            const response = editingId
                ? await communityAPI.update(editingId, payload, files)
                : await communityAPI.create(payload, files);
            const nextId = String(response.data?.communityId || editingId || '');
            setMessage(editingId ? '커뮤니티를 수정했습니다.' : '커뮤니티를 개설했습니다.');
            resetForm();
            await loadCommunities(nextId);
        } catch (requestError) {
            setError(requestError.response?.data?.message || '커뮤니티 저장에 실패했습니다.');
        } finally {
            setSaving(false);
        }
    }

    async function runAction(action, successText, reloadId = currentCommunity?.communityId) {
        setSaving(true);
        setError('');
        setMessage('');
        try {
            await action();
            setMessage(successText);
            await loadCommunities(String(reloadId || ''));
        } catch (requestError) {
            setError(requestError.response?.data?.message || '요청 처리에 실패했습니다.');
        } finally {
            setSaving(false);
        }
    }

    async function addMembers() {
        if (!currentCommunity || inviteIds.length === 0) return;
        await runAction(
            () => communityAPI.addMembers(currentCommunity.communityId, inviteIds),
            '회원을 초대했습니다.',
        );
        setInviteIds([]);
        await loadWorkspace(currentCommunity.communityId);
    }

    async function changeRole(userId, roleCd) {
        if (!currentCommunity) return;
        await runAction(
            () => communityAPI.updateRole(currentCommunity.communityId, userId, roleCd),
            '회원 권한을 변경했습니다.',
        );
        await loadWorkspace(currentCommunity.communityId);
    }

    async function removeMember(userId) {
        if (!currentCommunity || !window.confirm('이 회원을 제외하시겠습니까?')) return;
        await runAction(
            () => communityAPI.removeMember(currentCommunity.communityId, userId),
            '회원을 제외했습니다.',
        );
        await loadWorkspace(currentCommunity.communityId);
    }

    async function respondRequest(userId, approve) {
        if (!currentCommunity) return;
        await runAction(
            () => approve ? communityAPI.approveRequest(currentCommunity.communityId, userId) : communityAPI.rejectRequest(currentCommunity.communityId, userId),
            approve ? '가입 요청을 승인했습니다.' : '가입 요청을 반려했습니다.',
        );
        await loadWorkspace(currentCommunity.communityId);
    }

    async function reorder(direction) {
        if (view !== 'joined' || selectedOrderIndex < 0) return;
        const nextIndex = selectedOrderIndex + direction;
        if (nextIndex < 0 || nextIndex >= activeCommunities.length) return;
        const next = [...activeCommunities];
        const [selected] = next.splice(selectedOrderIndex, 1);
        next.splice(nextIndex, 0, selected);
        await runAction(() => communityAPI.saveOrder(next.map((item) => item.communityId)), '커뮤니티 순서를 저장했습니다.');
    }

    const renderNavItem = (item, prefix) => (
        <button key={`${prefix}-${item.communityId}`} type="button" className={`community-nav-item ${String(selectedId) === String(item.communityId) ? 'active' : ''}`} onClick={() => setSelectedId(String(item.communityId))}>
            <div className="community-nav-meta">
                <span className={`badge ${item.communityTypeCd === 'org' ? 'badge-blue' : item.communityTypeCd === 'notice' ? 'badge-orange' : 'badge-gray'}`}>
                    {item.communityTypeCd === 'org' ? '조직' : item.communityTypeCd === 'notice' ? '공지' : '일반'}
                </span>
                <span className="badge badge-gray">👥 {item.memberCount || 0}</span>
            </div>
            <strong>{item.communityNm}</strong>
            <span>{shortText(item.introText || item.communityDesc, 32)}</span>
        </button>
    );

    return (
        <div className="animate-slide-up">
            <div className="page-header">
                <div>
                    <h2>커뮤니티</h2>
                    <p>일반, 공지, 조직 커뮤니티를 한 화면에서 개설하고 운영합니다.</p>
                </div>
                <div className="page-header-actions">
                    {isAdmin && <button type="button" className="btn btn-outline" onClick={() => runAction(() => communityAPI.syncOrg(), '조직 커뮤니티를 동기화했습니다.')}>조직 동기화</button>}
                    <button type="button" className="btn btn-primary" onClick={openCreateForm}>커뮤니티 개설</button>
                </div>
            </div>

            {error && <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}><div className="card-body" style={{ color: 'var(--danger)' }}>{error}</div></div>}
            {message && <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}><div className="card-body" style={{ color: 'var(--success)' }}>{message}</div></div>}

            <div className="community-layout">
                <aside className="community-rail card">
                    <div className="card-header"><h3>커뮤니티 목록</h3></div>
                    <div className="card-body">
                        <input className="form-input" type="search" placeholder="커뮤니티 검색" value={keyword} onChange={(event) => setKeyword(event.target.value)} />
                        <div className="community-view-tabs">
                            {VIEW_OPTIONS.map(([value, label]) => (
                                <button key={value} type="button" className={`community-view-tab ${view === value ? 'active' : ''}`} onClick={() => setView(value)}>{label}</button>
                            ))}
                        </div>
                        {view === 'discover' ? (
                            <>
                                <div className="community-sidebar-label">탐색 결과</div>
                                <div className="community-nav-list">
                                    {communities.map((item) => renderNavItem(item, 'discover'))}
                                    {communities.length === 0 && <div className="community-placeholder">탐색 가능한 커뮤니티가 없습니다.</div>}
                                </div>
                            </>
                        ) : (
                            <>
                                <div className="community-sidebar-label">관심 커뮤니티</div>
                                <div className="community-nav-list">
                                    {favoriteCommunities.map((item) => renderNavItem(item, 'fav'))}
                                    {favoriteCommunities.length === 0 && <div className="community-placeholder">관심 커뮤니티가 없습니다.</div>}
                                </div>
                                <div className="community-sidebar-label">{view === 'manageable' ? '관리 가능한 커뮤니티' : '가입한 커뮤니티'}</div>
                                <div className="community-nav-list">
                                    {activeCommunities.map((item) => renderNavItem(item, 'active'))}
                                    {activeCommunities.length === 0 && <div className="community-placeholder">표시할 커뮤니티가 없습니다.</div>}
                                </div>
                                <div className="community-sidebar-label">조직 커뮤니티</div>
                                <div className="community-nav-list">
                                    {orgCommunities.map((item) => renderNavItem(item, 'org'))}
                                    {orgCommunities.length === 0 && <div className="community-placeholder">조직 커뮤니티가 없습니다.</div>}
                                </div>
                            </>
                        )}
                    </div>
                </aside>

                <section className="community-main">
                    {(loading || detailLoading) && <div className="card"><div className="card-body">커뮤니티를 불러오는 중입니다.</div></div>}
                    {!loading && !detailLoading && !currentCommunity && <div className="card empty-state"><div className="empty-icon">◎</div><h3>선택된 커뮤니티가 없습니다.</h3></div>}
                    {!loading && !detailLoading && currentCommunity && (
                        <>
                            <div className="community-hero-compact">
                                <div className="community-cover-thumbnail">
                                    {currentCommunity.iconFilePath ? <img src={currentCommunity.iconFilePath} alt="" /> : currentCommunity.coverFilePath ? <img src={currentCommunity.coverFilePath} alt="" /> : <div className="empty-icon">🗂️</div>}
                                </div>
                                <div className="community-hero-body">
                                    <div className="community-action-row" style={{ float: 'right', marginTop: 0 }}>
                                        {currentCommunity.joinable && <button type="button" className="btn btn-sm btn-primary" onClick={() => runAction(() => communityAPI.join(currentCommunity.communityId), '커뮤니티 가입 요청을 처리했습니다.', currentCommunity.communityId)}>가입하기</button>}
                                        {(currentCommunity.joined || currentCommunity.joinedYn === 'Y' || currentCommunity.memberStatusCd === 'active') && <button type="button" className="btn btn-sm btn-outline" onClick={() => runAction(() => communityAPI.favorite(currentCommunity.communityId, currentCommunity.favoriteYn === 'Y' ? 'N' : 'Y'), currentCommunity.favoriteYn === 'Y' ? '관심 커뮤니티를 해제했습니다.' : '관심 커뮤니티로 설정했습니다.', currentCommunity.communityId)}>{currentCommunity.favoriteYn === 'Y' ? '★ 관심 해제' : '☆ 관심 설정'}</button>}
                                        {currentCommunity.manageable && currentCommunity.communityTypeCd !== 'org' && <button type="button" className="btn btn-sm btn-outline" onClick={openEditForm}>⚙️ 설정</button>}
                                    </div>
                                    <div className="community-badge-row" style={{ marginBottom: '8px' }}>
                                        <span className="badge badge-blue">{currentCommunity.communityTypeCd === 'org' ? '조직' : currentCommunity.communityTypeCd === 'notice' ? '공지' : '일반'}</span>
                                        <span className="badge badge-gray">{currentCommunity.visibilityCd === 'org' ? '조직' : currentCommunity.visibilityCd === 'public' ? '전체 공개' : '비공개'}</span>
                                        <span className="badge badge-gray">{currentCommunity.communityTypeCd === 'org' ? '자동 가입' : currentCommunity.joinPolicyCd === 'approval' ? '승인 후 가입' : currentCommunity.joinPolicyCd === 'invite_only' ? '초대 전용' : '즉시 가입'}</span>
                                        {currentCommunity.memberRoleCd && <span className="badge badge-green">{labelOf(ROLE_LABELS, currentCommunity.memberRoleCd)}</span>}
                                        {currentCommunity.memberStatusCd && currentCommunity.memberStatusCd !== 'active' && <span className="badge badge-orange">{labelOf(STATUS_LABELS, currentCommunity.memberStatusCd, currentCommunity.memberStatusCd)}</span>}
                                    </div>
                                    <h3>{currentCommunity.communityNm}</h3>
                                    <p>{currentCommunity.introText || currentCommunity.communityDesc || '소개가 없습니다.'}</p>
                                </div>
                            </div>

                            <div className="card">
                                <div className="card-header">
                                    <div><h3>최근 활동</h3></div>
                                    <div className="community-inline-actions">
                                        <button type="button" className="btn btn-sm btn-outline" onClick={() => navigate(`/board?communityId=${currentCommunity.communityId}`)}>게시판 전체보기</button>
                                        {(currentCommunity.joined || currentCommunity.joinedYn === 'Y' || currentCommunity.memberStatusCd === 'active') && <button type="button" className="btn btn-sm btn-primary" onClick={() => navigate(`/board?communityId=${currentCommunity.communityId}&compose=1`)}>작성하기</button>}
                                        {view === 'joined' && selectedOrderIndex >= 0 && (
                                            <>
                                                <div className="divider-vertical" style={{ margin: '0 8px', borderLeft: '1px solid var(--border-color)', height: '24px' }} />
                                                <button type="button" className="btn btn-sm btn-outline" disabled={saving || selectedOrderIndex <= 0} onClick={() => reorder(-1)}>위로 이동</button>
                                                <button type="button" className="btn btn-sm btn-outline" disabled={saving || selectedOrderIndex >= activeCommunities.length - 1} onClick={() => reorder(1)}>아래로 이동</button>
                                            </>
                                        )}
                                    </div>
                                </div>
                                <div className="card-body">
                                    <div className="community-activity-feed">
                                        {boardItems.map((item) => (
                                            <button key={item.pstId} type="button" className="community-feed-item" onClick={() => navigate(`/board?communityId=${currentCommunity.communityId}&postId=${item.pstId}`)}>
                                                <div className="community-feed-item-content">
                                                    <div className="community-feed-item-top">
                                                        <span className="badge badge-gray">{labelOf(BOARD_TYPE_LABELS, item.pstTypeCd, '게시글')}</span>
                                                        {item.fixedYn === 'Y' && <span className="badge badge-orange">공지</span>}
                                                    </div>
                                                    <strong>{item.pstTtl}</strong>
                                                    <p>{shortText(item.contents, 100)}</p>
                                                </div>
                                                <div className="community-feed-item-meta">
                                                    <span>{item.userNm || item.crtUserId}</span>
                                                    <span style={{ marginTop: '4px' }}>{fmt(item.publishedDt || item.frstCrtDt)}</span>
                                                    <span style={{ marginTop: '4px', color: 'var(--primary)' }}>👁 {item.readCnt || 0}</span>
                                                </div>
                                            </button>
                                        ))}
                                    </div>
                                    {boardItems.length === 0 && (
                                        <div className="community-empty-action">
                                            <div className="empty-icon">✍️</div>
                                            <h3>아직 게시글이 없습니다.</h3>
                                            <p>이 커뮤니티의 첫 번째 소식을 남겨보세요.</p>
                                            {(currentCommunity.joined || currentCommunity.joinedYn === 'Y' || currentCommunity.memberStatusCd === 'active') && (
                                                <button type="button" className="btn btn-primary" style={{ marginTop: '12px' }} onClick={() => navigate(`/board?communityId=${currentCommunity.communityId}&compose=1`)}>첫 게시글 작성</button>
                                            )}
                                        </div>
                                    )}
                                </div>
                            </div>
                        </>
                    )}
                </section>

                <aside className="community-side community-admin-panel">
                    {currentCommunity && (
                        <>
                            <div className="card">
                                <div className="card-header"><h3>운영 정보</h3></div>
                                <div className="card-body community-side-list">
                                    <div><strong>소유자</strong><span>{currentCommunity.ownerUserNm || currentCommunity.ownerUserId || '-'}</span></div>
                                    <div><strong>유형</strong><span>{currentCommunity.communityTypeCd === 'org' ? '조직' : currentCommunity.communityTypeCd === 'notice' ? '공지' : '일반'}</span></div>
                                    <div><strong>공개</strong><span>{currentCommunity.visibilityCd === 'org' ? '조직' : currentCommunity.visibilityCd === 'public' ? '전체 공개' : '비공개'}</span></div>
                                    <div><strong>가입 정책</strong><span>{currentCommunity.communityTypeCd === 'org' ? '자동 가입' : currentCommunity.joinPolicyCd === 'approval' ? '승인 후 가입' : currentCommunity.joinPolicyCd === 'invite_only' ? '초대 전용' : '즉시 가입'}</span></div>
                                </div>
                            </div>

                            <div className="card">
                                <div className="card-header"><h3>회원</h3></div>
                                <div className="card-body community-member-list">
                                    {members.map((member) => (
                                        <div key={member.userId} className={`community-member-item ${member.roleCd === 'operator' || member.roleCd === 'owner' ? 'operator' : ''}`}>
                                            <div>
                                                <strong>{member.userNm}</strong>
                                                <span>{[member.deptNm, member.jbgdNm].filter(Boolean).join(' / ') || member.userId}</span>
                                                <small>{labelOf(ROLE_LABELS, member.roleCd, member.roleCd)}</small>
                                            </div>
                                            {currentCommunity.manageable && member.roleCd !== 'owner' && currentCommunity.communityTypeCd !== 'org' && (
                                                <div className="community-member-actions">
                                                    <select className="form-input" value={member.roleCd || 'member'} onChange={(event) => changeRole(member.userId, event.target.value)}>
                                                        <option value="member">회원</option>
                                                        <option value="operator">운영자</option>
                                                    </select>
                                                    <button type="button" className="btn btn-sm btn-outline" onClick={() => removeMember(member.userId)}>제외</button>
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                    {members.length === 0 && <div className="community-placeholder">회원이 없습니다.</div>}
                                </div>
                            </div>

                            {currentCommunity.manageable && currentCommunity.communityTypeCd !== 'org' && (
                                <div className="card">
                                    <div className="card-header"><h3>회원 초대</h3></div>
                                    <div className="card-body">
                                        <div className="community-selector-list">
                                            {selectableUsers.map((member) => (
                                                <label key={`invite-${member.userId}`} className="community-selector-item">
                                                    <input type="checkbox" checked={inviteIds.includes(member.userId)} onChange={(event) => toggleSelection(setInviteIds, member.userId, event.target.checked)} />
                                                    <span>{member.userNm} ({member.userId})</span>
                                                </label>
                                            ))}
                                        </div>
                                        <div className="form-actions">
                                            <button type="button" className="btn btn-primary" disabled={inviteIds.length === 0 || saving} onClick={addMembers}>회원 초대</button>
                                        </div>
                                    </div>
                                </div>
                            )}

                            {currentCommunity.manageable && requests.length > 0 && (
                                <div className="card">
                                    <div className="card-header"><h3>가입 승인</h3></div>
                                    <div className="card-body community-member-list">
                                        {requests.map((member) => (
                                            <div key={`request-${member.userId}`} className="community-member-item">
                                                <div>
                                                    <strong>{member.userNm}</strong>
                                                    <span>{[member.deptNm, member.jbgdNm].filter(Boolean).join(' / ') || member.userId}</span>
                                                    <small>{labelOf(STATUS_LABELS, member.statusCd, member.statusCd)}</small>
                                                </div>
                                                <div className="community-member-actions">
                                                    <button type="button" className="btn btn-sm btn-primary" onClick={() => respondRequest(member.userId, true)}>승인</button>
                                                    <button type="button" className="btn btn-sm btn-outline" onClick={() => respondRequest(member.userId, false)}>반려</button>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </>
                    )}
                </aside>
            </div>

            {formOpen && (
                <div className="modal-backdrop">
                    <div className="modal-panel community-modal-shell">
                        <div className="modal-header">
                            <div><h3>{editingId ? '커뮤니티 관리' : '커뮤니티 개설'}</h3><p>새로운 소통의 공간을 설정합니다.</p></div>
                            <button type="button" className="modal-close" onClick={resetForm}>×</button>
                        </div>
                        <div className="modal-body">
                            <form className="form-shell" onSubmit={submitForm}>
                                <div className="community-modal-section">
                                    <div className="community-modal-section-header">
                                        <h4>기본 정보</h4>
                                        <p>커뮤니티의 이름과 성격을 정의합니다.</p>
                                    </div>
                                    <div className="community-form-grid">
                                        <div className="form-group"><label className="form-label">커뮤니티명</label><input className="form-input" value={form.communityNm} onChange={(event) => setForm((current) => ({ ...current, communityNm: event.target.value }))} /></div>
                                        <div className="form-group"><label className="form-label">유형</label><select className="form-input" value={form.communityTypeCd} onChange={(event) => setForm((current) => ({ ...current, communityTypeCd: event.target.value }))}>{TYPE_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
                                        <div className="form-group form-span-2"><label className="form-label">소개 및 설명</label><textarea className="form-input" rows="2" value={form.communityDesc} onChange={(event) => setForm((current) => ({ ...current, communityDesc: event.target.value }))} /></div>
                                    </div>
                                </div>

                                <div className="community-modal-section">
                                    <div className="community-modal-section-header">
                                        <h4>공개 및 정책</h4>
                                        <p>가입 방식과 콘텐츠 공개 범위를 설정합니다.</p>
                                    </div>
                                    <div className="community-form-grid">
                                        <div className="form-group"><label className="form-label">공개 여부</label><select className="form-input" value={form.visibilityCd} onChange={(event) => setForm((current) => ({ ...current, visibilityCd: event.target.value, joinPolicyCd: event.target.value === 'private' ? 'invite_only' : current.joinPolicyCd === 'approval' ? 'approval' : 'instant' }))}>{VISIBILITY_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
                                        <div className="form-group"><label className="form-label">가입 정책</label><select className="form-input" value={form.joinPolicyCd} onChange={(event) => setForm((current) => ({ ...current, joinPolicyCd: event.target.value }))} disabled={form.visibilityCd === 'private'}>{form.visibilityCd === 'private' ? <option value="invite_only">초대 전용</option> : POLICY_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
                                    </div>
                                </div>

                                <div className="community-modal-section">
                                    <div className="community-modal-section-header">
                                        <h4>브랜딩 및 운영</h4>
                                        <p>시각적 요소와 권한을 지정합니다.</p>
                                    </div>
                                    <div className="community-form-grid">
                                        <div className="form-group"><label className="form-label">아이콘</label><input className="form-input" type="file" accept="image/*" onChange={(event) => setIconFile(event.target.files?.[0] || null)} /><small>권장 40x40px</small></div>
                                        <div className="form-group"><label className="form-label">커버 이미지</label><input className="form-input" type="file" accept="image/*" onChange={(event) => setCoverFile(event.target.files?.[0] || null)} /><small>권장 600x270px</small></div>

                                        <div className="form-group form-span-2">
                                            <label className="form-label">운영자</label>
                                            <div className="community-selector-list">
                                                {selectableUsers.map((member) => (
                                                    <label key={`operator-${member.userId}`} className="community-selector-item">
                                                        <input type="checkbox" checked={operatorIds.includes(member.userId)} onChange={(event) => toggleSelection(setOperatorIds, member.userId, event.target.checked)} />
                                                        <span>{member.userNm} ({member.userId})</span>
                                                    </label>
                                                ))}
                                            </div>
                                        </div>
                                        {!editingId && (
                                            <div className="form-group form-span-2">
                                                <label className="form-label">초기 회원</label>
                                                <div className="community-selector-list">
                                                    {selectableUsers.map((member) => (
                                                        <label key={`member-${member.userId}`} className="community-selector-item">
                                                            <input type="checkbox" checked={memberIds.includes(member.userId)} onChange={(event) => toggleSelection(setMemberIds, member.userId, event.target.checked)} />
                                                            <span>{member.userNm} ({member.userId})</span>
                                                        </label>
                                                    ))}
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                </div>

                                <div className="community-modal-section" style={{ background: 'var(--bg-muted)' }}>
                                    <div className="form-actions" style={{ marginTop: 0 }}>
                                        <button type="button" className="btn btn-outline" onClick={resetForm}>취소</button>
                                        <button type="submit" className="btn btn-primary" disabled={saving}>{editingId ? '저장' : '개설'}</button>
                                    </div>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
