import { useEffect, useMemo, useState } from 'react';
import { meetingAPI } from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';

function getToday() {
    return new Date().toISOString().slice(0, 10);
}

function hourOptions() {
    return Array.from({ length: 13 }, (_, index) => 8 + index);
}

function formatHour(hour) {
    return `${String(hour).padStart(2, '0')}:00`;
}

export default function MeetingPage() {
    const { user } = useAuth();
    const [selectedDate, setSelectedDate] = useState(getToday());
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');
    const [rooms, setRooms] = useState([]);
    const [reservations, setReservations] = useState([]);
    const [roomForm, setRoomForm] = useState({
        roomName: '',
        location: '',
        capacity: 6,
        useYn: 'Y',
    });
    const [reservationForm, setReservationForm] = useState({
        roomId: '',
        title: '',
        meetingDate: getToday(),
        startTime: 9,
        endTime: 10,
    });

    const roomReservations = useMemo(() => {
        return rooms.map((room) => ({
            ...room,
            reservations: reservations.filter((reservation) => reservation.roomId === room.roomId),
        }));
    }, [rooms, reservations]);

    const activeRooms = useMemo(() => {
        return rooms.filter((room) => room.useYn !== 'N');
    }, [rooms]);

    useEffect(() => {
        loadMeetingData(selectedDate);
    }, [selectedDate]);

    async function loadMeetingData(date) {
        setLoading(true);
        setError('');

        try {
            const params = { date };
            if (user?.userRole?.toLowerCase?.().includes('admin')) {
                params.role = 'admin';
            }

            const [roomResponse, reservationResponse] = await Promise.all([
                meetingAPI.rooms(),
                meetingAPI.reservations(params),
            ]);

            const nextRooms = roomResponse.data || [];
            setRooms(nextRooms);
            setReservations(reservationResponse.data || []);
            const nextActiveRooms = nextRooms.filter((room) => room.useYn !== 'N');
            setReservationForm((current) => ({
                ...current,
                meetingDate: date,
                roomId: nextActiveRooms.some((room) => room.roomId === current.roomId)
                    ? current.roomId
                    : nextActiveRooms[0]?.roomId || '',
            }));
        } catch (err) {
            setError(err.response?.data?.message || '회의실 정보를 불러오지 못했습니다.');
        } finally {
            setLoading(false);
        }
    }

    async function handleCreateRoom(event) {
        event.preventDefault();
        setSubmitting(true);
        setError('');

        try {
            await meetingAPI.createRoom({
                ...roomForm,
                capacity: Number(roomForm.capacity),
            });
            setRoomForm({ roomName: '', location: '', capacity: 6, useYn: 'Y' });
            await loadMeetingData(selectedDate);
        } catch (err) {
            setError(err.response?.data?.message || '회의실 생성에 실패했습니다.');
        } finally {
            setSubmitting(false);
        }
    }

    async function handleCreateReservation(event) {
        event.preventDefault();
        if (!reservationForm.roomId) {
            setError('회의실을 먼저 선택하세요.');
            return;
        }
        if (Number(reservationForm.startTime) >= Number(reservationForm.endTime)) {
            setError('종료 시간은 시작 시간보다 뒤여야 합니다.');
            return;
        }

        setSubmitting(true);
        setError('');

        try {
            await meetingAPI.createReservation({
                roomId: reservationForm.roomId,
                title: reservationForm.title,
                meetingDate: reservationForm.meetingDate,
                startTime: Number(reservationForm.startTime),
                endTime: Number(reservationForm.endTime),
            });
            setReservationForm((current) => ({
                ...current,
                title: '',
            }));
            await loadMeetingData(selectedDate);
        } catch (err) {
            setError(err.response?.data?.message || '회의 예약에 실패했습니다.');
        } finally {
            setSubmitting(false);
        }
    }

    async function handleDeleteReservation(reservationId) {
        setSubmitting(true);
        setError('');

        try {
            await meetingAPI.deleteReservation(reservationId);
            await loadMeetingData(selectedDate);
        } catch (err) {
            setError(err.response?.data?.message || '예약 삭제에 실패했습니다.');
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div className="animate-slide-up">
            <div className="page-header">
                <div>
                    <h2>회의실 예약</h2>
                    <p>회의실 생성과 날짜별 예약을 실제 데이터로 관리합니다.</p>
                </div>
                <div className="page-header-actions">
                    <input
                        type="date"
                        className="form-input"
                        value={selectedDate}
                        onChange={(event) => setSelectedDate(event.target.value)}
                    />
                </div>
            </div>

            {error && (
                <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
                    <div className="card-body" style={{ color: 'var(--danger)' }}>{error}</div>
                </div>
            )}

            <div className="grid-2" style={{ alignItems: 'start' }}>
                <div className="card">
                    <div className="card-header"><h3>회의실 만들기</h3></div>
                    <div className="card-body">
                        <form className="form-shell" onSubmit={handleCreateRoom}>
                            <div className="form-section">
                                <div className="form-grid">
                                    <div className="form-group">
                                        <label className="form-label" htmlFor="meeting-room-name">회의실 이름</label>
                                        <input
                                            id="meeting-room-name"
                                            type="text"
                                            className="form-input"
                                            placeholder="예: 3층 대회의실"
                                            value={roomForm.roomName}
                                            onChange={(event) => setRoomForm((current) => ({ ...current, roomName: event.target.value }))}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label" htmlFor="meeting-room-location">위치</label>
                                        <input
                                            id="meeting-room-location"
                                            type="text"
                                            className="form-input"
                                            placeholder="예: 본관 3층"
                                            value={roomForm.location}
                                            onChange={(event) => setRoomForm((current) => ({ ...current, location: event.target.value }))}
                                        />
                                    </div>
                                    <div className="form-group form-span-2">
                                        <label className="form-label" htmlFor="meeting-room-capacity">수용 인원</label>
                                        <input
                                            id="meeting-room-capacity"
                                            type="number"
                                            min="1"
                                            className="form-input"
                                            placeholder="최대 참석 인원"
                                            value={roomForm.capacity}
                                            onChange={(event) => setRoomForm((current) => ({ ...current, capacity: event.target.value }))}
                                        />
                                    </div>
                                </div>
                            </div>
                            <div className="form-actions">
                                <button type="submit" className="btn btn-primary" disabled={submitting}>
                                    {submitting ? '저장 중...' : '회의실 생성'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>

                <div className="card">
                    <div className="card-header"><h3>예약 만들기</h3></div>
                    <div className="card-body">
                        <form className="form-shell" onSubmit={handleCreateReservation}>
                            <div className="form-section">
                                <div className="form-grid">
                                    <div className="form-group">
                                        <label className="form-label" htmlFor="reservation-room-id">회의실</label>
                                        <select
                                            id="reservation-room-id"
                                            className="form-input"
                                            value={reservationForm.roomId}
                                            onChange={(event) => setReservationForm((current) => ({ ...current, roomId: event.target.value }))}
                                        >
                                            <option value="">회의실 선택</option>
                                            {activeRooms.map((room) => (
                                                <option key={room.roomId} value={room.roomId}>{room.roomName}</option>
                                            ))}
                                        </select>
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label" htmlFor="reservation-title">회의 제목</label>
                                        <input
                                            id="reservation-title"
                                            type="text"
                                            className="form-input"
                                            placeholder="예: 주간 운영 회의"
                                            value={reservationForm.title}
                                            onChange={(event) => setReservationForm((current) => ({ ...current, title: event.target.value }))}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label" htmlFor="reservation-date">예약 날짜</label>
                                        <input
                                            id="reservation-date"
                                            type="date"
                                            className="form-input"
                                            value={reservationForm.meetingDate}
                                            onChange={(event) => setReservationForm((current) => ({ ...current, meetingDate: event.target.value }))}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label">예약 시간</label>
                                        <div className="form-grid" style={{ gridTemplateColumns: '1fr 1fr', gap: 'var(--spacing-sm)' }}>
                                            <select
                                                className="form-input"
                                                value={reservationForm.startTime}
                                                onChange={(event) => setReservationForm((current) => ({ ...current, startTime: event.target.value }))}
                                            >
                                                {hourOptions().map((hour) => (
                                                    <option key={hour} value={hour}>{formatHour(hour)}</option>
                                                ))}
                                            </select>
                                            <select
                                                className="form-input"
                                                value={reservationForm.endTime}
                                                onChange={(event) => setReservationForm((current) => ({ ...current, endTime: event.target.value }))}
                                            >
                                                {hourOptions().map((hour) => (
                                                    <option key={hour} value={hour}>{formatHour(hour)}</option>
                                                ))}
                                            </select>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div className="form-actions">
                                <button type="submit" className="btn btn-primary" disabled={submitting || activeRooms.length === 0}>
                                    {submitting ? '저장 중...' : '예약 등록'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>

            <div className="room-grid" style={{ margin: 'var(--spacing-lg) 0' }}>
                {roomReservations.length === 0 && !loading ? (
                    <div className="card">
                        <div className="card-body">
                            아직 등록된 회의실이 없습니다. 먼저 회의실을 생성하세요.
                        </div>
                    </div>
                ) : (
                    roomReservations.map((room) => (
                        <div key={room.roomId} className="room-card">
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--spacing-md)' }}>
                                <h3 style={{ fontSize: 'var(--font-size-md)', fontWeight: 600 }}>{room.roomName}</h3>
                                <span className={`badge ${room.useYn === 'N' ? 'badge-gray' : room.reservations.length === 0 ? 'badge-green' : 'badge-orange'}`}>
                                    {room.useYn === 'N' ? '비활성' : room.reservations.length === 0 ? '예약 가능' : `${room.reservations.length}건 예약`}
                                </span>
                            </div>
                            <div style={{ display: 'grid', gap: '4px', fontSize: 'var(--font-size-sm)', color: 'var(--gray-600)' }}>
                                <div>위치: {room.location || '-'}</div>
                                <div>수용 인원: {room.capacity || '-'}명</div>
                            </div>
                            <div style={{ marginTop: 'var(--spacing-md)', paddingTop: 'var(--spacing-sm)', borderTop: '1px solid var(--gray-100)' }}>
                                <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--gray-500)', marginBottom: '6px' }}>{selectedDate} 예약</div>
                                {room.reservations.length === 0 ? (
                                    <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--gray-500)' }}>예약이 없습니다.</div>
                                ) : (
                                    room.reservations.map((reservation) => (
                                        <div key={reservation.reservationId} style={{ fontSize: 'var(--font-size-sm)', padding: '6px 0' }}>
                                            {formatHour(reservation.startTime)} - {formatHour(reservation.endTime)} · {reservation.title || '제목 없음'}
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>
                    ))
                )}
            </div>

            <div className="card">
                <div className="card-header">
                    <h3>예약 목록</h3>
                </div>
                <div className="card-body" style={{ padding: 0 }}>
                    {reservations.length === 0 && !loading ? (
                        <div className="empty-state" style={{ minHeight: '180px' }}>
                            <div className="empty-icon">-</div>
                            <h3>이 날짜의 예약이 없습니다.</h3>
                            <p>회의실을 만든 뒤 예약을 등록하면 여기에 표시됩니다.</p>
                        </div>
                    ) : (
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>회의실</th>
                                    <th>제목</th>
                                    <th>예약자</th>
                                    <th>시간</th>
                                    <th style={{ width: '96px' }}>관리</th>
                                </tr>
                            </thead>
                            <tbody>
                                {reservations.map((reservation) => (
                                    <tr key={reservation.reservationId}>
                                        <td>{reservation.roomName || reservation.roomId}</td>
                                        <td><strong>{reservation.title || '제목 없음'}</strong></td>
                                        <td>{reservation.userNm || reservation.userId}</td>
                                        <td>{formatHour(reservation.startTime)} - {formatHour(reservation.endTime)}</td>
                                        <td>
                                            <button
                                                className="btn btn-sm btn-secondary"
                                                onClick={() => handleDeleteReservation(reservation.reservationId)}
                                                disabled={submitting}
                                            >
                                                삭제
                                            </button>
                                        </td>
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
