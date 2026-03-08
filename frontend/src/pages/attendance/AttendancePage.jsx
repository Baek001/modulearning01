import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { attendanceAPI } from '../../services/api';

function formatDateTime(value) {
    if (!value) {
        return '-';
    }

    return new Date(value).toLocaleString('ko-KR', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    });
}

function formatTime(value) {
    if (!value) {
        return '--:--';
    }

    return new Date(value).toLocaleTimeString('ko-KR', {
        hour: '2-digit',
        minute: '2-digit',
    });
}

function getTodayWorkYmd() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const date = String(now.getDate()).padStart(2, '0');
    return `${year}${month}${date}`;
}

function getWorkStatusLabel(code) {
    switch (code) {
        case 'C101':
            return ['근무중', 'badge-green'];
        case 'C103':
            return ['퇴근', 'badge-gray'];
        case 'C104':
            return ['휴가', 'badge-purple'];
        case 'C105':
            return ['출장', 'badge-orange'];
        default:
            return [code || '미정', 'badge-gray'];
    }
}

function SummaryCard({ title, items }) {
    return (
        <div className="card">
            <div className="card-header"><h3>{title}</h3></div>
            <div className="card-body">
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--spacing-md)' }}>
                    {items.map((item) => (
                        <div key={item.label} style={{ padding: 'var(--spacing-md)', background: 'var(--gray-50)', borderRadius: '12px' }}>
                            <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--gray-500)', marginBottom: '4px' }}>{item.label}</div>
                            <div style={{ fontSize: 'var(--font-size-lg)', fontWeight: 700 }}>{item.value}</div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}

export default function AttendancePage() {
    const { user } = useAuth();
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(false);
    const [error, setError] = useState('');
    const [today, setToday] = useState(null);
    const [week, setWeek] = useState(null);
    const [month, setMonth] = useState(null);
    const [department, setDepartment] = useState([]);
    const [history, setHistory] = useState([]);

    const workYmd = today?.workYmd || getTodayWorkYmd();
    const canClockIn = !today?.workBgngDt;
    const canClockOut = Boolean(today?.workBgngDt) && !today?.workEndDt;

    const weeklyItems = useMemo(() => ([
        { label: '근무일', value: `${week?.workDays ?? 0}일` },
        { label: '총 근무 분', value: week?.totalWorkHr ?? 0 },
        { label: '지각', value: `${week?.lateCount ?? 0}회` },
        { label: '초과근무 분', value: week?.totalOvertimeHr ?? 0 },
    ]), [week]);

    const monthlyItems = useMemo(() => ([
        { label: '근무일', value: `${month?.workDays ?? 0}일` },
        { label: '총 근무 분', value: month?.totalWorkHr ?? 0 },
        { label: '지각', value: `${month?.lateCount ?? 0}회` },
        { label: '결근', value: `${month?.absentDays ?? 0}일` },
    ]), [month]);

    useEffect(() => {
        if (!user?.userId) {
            return;
        }

        loadAttendance();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [user?.userId]);

    async function loadAttendance() {
        setLoading(true);
        setError('');

        try {
            const [weekResponse, monthResponse, departResponse, historyResponse] = await Promise.all([
                attendanceAPI.week(),
                attendanceAPI.month(),
                attendanceAPI.depart(),
                attendanceAPI.history(user.userId),
            ]);

            const nextHistory = historyResponse.data?.listTAA || [];
            const todayRecord = nextHistory.find((item) => item.workYmd === getTodayWorkYmd()) || null;

            setToday(todayRecord);
            setWeek(weekResponse.data?.uwaDTO || null);
            setMonth(monthResponse.data?.umaDTO || null);
            setDepartment(departResponse.data?.adsDTOList || []);
            setHistory(nextHistory);
        } catch (err) {
            setError(err.response?.data?.message || '근태 정보를 불러오지 못했습니다.');
        } finally {
            setLoading(false);
        }
    }

    async function handleClockIn() {
        setActionLoading(true);
        setError('');

        try {
            await attendanceAPI.clockIn();
            await loadAttendance();
        } catch (err) {
            setError(err.response?.data?.message || '출근 처리에 실패했습니다.');
        } finally {
            setActionLoading(false);
        }
    }

    async function handleClockOut() {
        setActionLoading(true);
        setError('');

        try {
            await attendanceAPI.clockOut(workYmd);
            await loadAttendance();
        } catch (err) {
            setError(err.response?.data?.message || '퇴근 처리에 실패했습니다.');
        } finally {
            setActionLoading(false);
        }
    }

    return (
        <div className="animate-slide-up">
            <div className="page-header">
                <div>
                    <h2>근태 관리</h2>
                    <p>실제 출근/퇴근 기록과 부서 현황을 조회합니다.</p>
                </div>
            </div>

            {error && (
                <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
                    <div className="card-body" style={{ color: 'var(--danger)' }}>{error}</div>
                </div>
            )}

            <div className="stat-grid">
                <div className="stat-card blue">
                    <div className="stat-info">
                        <h4>출근 시간</h4>
                        <div className="stat-value">{loading ? '--:--' : formatTime(today?.workBgngDt)}</div>
                    </div>
                </div>
                <div className="stat-card green">
                    <div className="stat-info">
                        <h4>퇴근 시간</h4>
                        <div className="stat-value">{loading ? '--:--' : formatTime(today?.workEndDt)}</div>
                    </div>
                </div>
                <div className="stat-card orange">
                    <div className="stat-info">
                        <h4>오늘 근무 분</h4>
                        <div className="stat-value">{loading ? '-' : today?.workHr ?? 0}</div>
                    </div>
                </div>
                <div className="stat-card purple">
                    <div className="stat-info">
                        <h4>이번 달 근무일</h4>
                        <div className="stat-value">{loading ? '-' : `${month?.workDays ?? 0}일`}</div>
                    </div>
                </div>
            </div>

            <div style={{ marginBottom: 'var(--spacing-lg)', display: 'flex', gap: 'var(--spacing-sm)' }}>
                {canClockIn && (
                    <button className="btn btn-primary btn-lg" onClick={handleClockIn} disabled={actionLoading}>
                        {actionLoading ? '처리 중...' : '출근하기'}
                    </button>
                )}
                {canClockOut && (
                    <button className="btn btn-secondary btn-lg" onClick={handleClockOut} disabled={actionLoading}>
                        {actionLoading ? '처리 중...' : '퇴근하기'}
                    </button>
                )}
                {!canClockIn && !canClockOut && (
                    <span className="badge badge-green">오늘 근태 처리가 완료되었습니다.</span>
                )}
            </div>

            <div className="grid-2">
                <SummaryCard title="주간 요약" items={weeklyItems} />
                <SummaryCard title="월간 요약" items={monthlyItems} />
            </div>

            <div className="card" style={{ marginTop: 'var(--spacing-md)' }}>
                <div className="card-header">
                    <h3>부서 근무 현황</h3>
                </div>
                <div className="card-body" style={{ padding: 0 }}>
                    {department.length === 0 && !loading ? (
                        <div className="empty-state" style={{ minHeight: '180px' }}>
                            <div className="empty-icon">-</div>
                            <h3>표시할 부서 근태가 없습니다.</h3>
                            <p>오늘 출근 기록이 쌓이면 여기에 표시됩니다.</p>
                        </div>
                    ) : (
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>이름</th>
                                    <th>출근</th>
                                    <th>퇴근</th>
                                    <th>근무 분</th>
                                    <th>상태</th>
                                </tr>
                            </thead>
                            <tbody>
                                {department.map((member) => {
                                    const [label, badgeClass] = getWorkStatusLabel(member.workSttsCd);
                                    return (
                                        <tr key={`${member.userId}-${member.workYmd}`}>
                                            <td><strong>{member.userNm || member.userId}</strong></td>
                                            <td>{formatDateTime(member.workBgngDt)}</td>
                                            <td>{formatDateTime(member.workEndDt)}</td>
                                            <td>{member.workHr ?? 0}</td>
                                            <td><span className={`badge ${badgeClass}`}>{label}</span></td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>

            <div className="card" style={{ marginTop: 'var(--spacing-md)' }}>
                <div className="card-header">
                    <h3>내 최근 기록</h3>
                </div>
                <div className="card-body" style={{ padding: 0 }}>
                    {history.length === 0 && !loading ? (
                        <div className="empty-state" style={{ minHeight: '180px' }}>
                            <div className="empty-icon">-</div>
                            <h3>근태 이력이 없습니다.</h3>
                            <p>출근 또는 퇴근 처리 후 다시 확인하세요.</p>
                        </div>
                    ) : (
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>근무일</th>
                                    <th>출근</th>
                                    <th>퇴근</th>
                                    <th>근무 분</th>
                                    <th>지각</th>
                                </tr>
                            </thead>
                            <tbody>
                                {history.slice(0, 10).map((item) => (
                                    <tr key={`${item.userId}-${item.workYmd}`}>
                                        <td>{item.workYmd}</td>
                                        <td>{formatDateTime(item.workBgngDt)}</td>
                                        <td>{formatDateTime(item.workEndDt)}</td>
                                        <td>{item.workHr ?? 0}</td>
                                        <td>{item.lateYn === 'Y' ? '지각' : '-'}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
}
