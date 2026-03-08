import { useEffect, useMemo, useState } from 'react';
import { departmentAPI, usersAPI } from '../../services/api';

const avatarColors = ['#4c6ef5', '#7950f2', '#37b24d', '#f76707', '#e64980', '#1098ad', '#ae3ec9'];

const statusMap = {
    C103: { label: '재직', badge: 'badge-green' },
    C104: { label: '휴직', badge: 'badge-purple' },
    WORK: { label: '근무 중', badge: 'badge-green' },
    MEETING: { label: '회의 중', badge: 'badge-blue' },
    BIZTRIP: { label: '출장 중', badge: 'badge-orange' },
    VACATION: { label: '휴가 중', badge: 'badge-purple' },
    AWAY: { label: '자리 비움', badge: 'badge-gray' },
};

function getStatusMeta(code) {
    return statusMap[code] || { label: code || '상태 미확인', badge: 'badge-gray' };
}

export default function OrgChartPage() {
    const [users, setUsers] = useState([]);
    const [departments, setDepartments] = useState([]);
    const [selectedDept, setSelectedDept] = useState('');
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        let active = true;

        async function loadOrgChart() {
            setLoading(true);
            setError('');

            try {
                const [usersResponse, departmentsResponse] = await Promise.all([
                    usersAPI.list(),
                    departmentAPI.list(),
                ]);

                if (!active) {
                    return;
                }

                setUsers(usersResponse.data ?? []);
                setDepartments(departmentsResponse.data ?? []);
            } catch {
                if (!active) {
                    return;
                }

                setError('백엔드에서 조직도 데이터를 불러오지 못했습니다.');
            } finally {
                if (active) {
                    setLoading(false);
                }
            }
        }

        loadOrgChart();

        return () => {
            active = false;
        };
    }, []);

    const sortedDepartments = useMemo(() => (
        [...departments].sort((left, right) => {
            const leftSort = Number(left.sortNum ?? 0);
            const rightSort = Number(right.sortNum ?? 0);

            if (leftSort !== rightSort) {
                return leftSort - rightSort;
            }

            return (left.deptNm ?? '').localeCompare(right.deptNm ?? '');
        })
    ), [departments]);

    const filteredUsers = useMemo(() => (
        users.filter((currentUser) => {
            const matchesDepartment = selectedDept ? currentUser.deptId === selectedDept : true;
            const keyword = searchTerm.trim().toLowerCase();
            const matchesSearch = keyword
                ? [currentUser.userNm, currentUser.deptNm, currentUser.userId]
                    .filter(Boolean)
                    .some((value) => value.toLowerCase().includes(keyword))
                : true;

            return matchesDepartment && matchesSearch;
        })
    ), [users, selectedDept, searchTerm]);

    if (loading) {
        return (
            <div className="animate-slide-up">
                <div className="page-header">
                    <h2>조직도</h2>
                    <p>부서와 직원 정보를 불러오는 중입니다.</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="animate-slide-up">
                <div className="page-header">
                    <h2>조직도</h2>
                    <p>{error}</p>
                </div>
                <button className="btn btn-primary" onClick={() => window.location.reload()}>
                    다시 시도
                </button>
            </div>
        );
    }

    return (
        <div className="animate-slide-up">
            <div className="page-header">
                <h2>조직도</h2>
                <p>Spring 백엔드에 저장된 부서와 구성원 정보를 실시간으로 보여줍니다.</p>
                <div className="page-header-actions">
                    <div className="topbar-search" style={{ maxWidth: '300px' }}>
                        <span className="search-icon">S</span>
                        <input
                            type="text"
                            placeholder="이름, 부서, 아이디 검색"
                            value={searchTerm}
                            onChange={(event) => setSearchTerm(event.target.value)}
                        />
                    </div>
                </div>
            </div>

            <div className="org-tree">
                <div className="dept-tree card" style={{ padding: 'var(--spacing-md)' }}>
                    <h3 style={{ fontSize: 'var(--font-size-md)', fontWeight: 600, marginBottom: 'var(--spacing-md)', padding: '0 var(--spacing-sm)' }}>
                        부서 목록
                    </h3>
                    <div
                        className={`dept-tree-item ${!selectedDept ? 'active' : ''}`}
                        onClick={() => setSelectedDept('')}
                    >
                        전체 부서
                    </div>
                    {sortedDepartments.map((department) => (
                        <div
                            key={department.deptId}
                            className={`dept-tree-item ${selectedDept === department.deptId ? 'active' : ''}`}
                            onClick={() => setSelectedDept(department.deptId)}
                        >
                            <span>{department.upDeptId ? ' - ' : ''}{department.deptNm}</span>
                            <span style={{ marginLeft: 'auto', fontSize: 'var(--font-size-xs)', color: 'var(--gray-400)' }}>
                                {users.filter((currentUser) => currentUser.deptId === department.deptId).length}
                            </span>
                        </div>
                    ))}
                </div>

                <div className="member-grid">
                    {filteredUsers.map((currentUser, index) => {
                        const status = getStatusMeta(currentUser.workSttsCd);

                        return (
                            <div key={currentUser.userId} className="member-card">
                                <div
                                    className="member-avatar"
                                    style={{
                                        background: `linear-gradient(135deg, ${avatarColors[index % avatarColors.length]}, ${avatarColors[(index + 2) % avatarColors.length]})`,
                                    }}
                                >
                                    {currentUser.userNm?.charAt(0) || '?'}
                                </div>
                                <div className="member-name">{currentUser.userNm}</div>
                                <div className="member-role">{currentUser.deptNm} · {currentUser.jbgdNm}</div>
                                <div style={{ marginTop: 'var(--spacing-sm)' }}>
                                    <span className={`badge ${status.badge}`}>{status.label}</span>
                                </div>
                                <div style={{ marginTop: 'var(--spacing-sm)', fontSize: 'var(--font-size-xs)', color: 'var(--gray-400)' }}>
                                    {currentUser.userEmail || '이메일 없음'}
                                </div>
                                <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--gray-400)' }}>
                                    내선 {currentUser.extTel || '-'}
                                </div>
                            </div>
                        );
                    })}
                </div>
            </div>
        </div>
    );
}
