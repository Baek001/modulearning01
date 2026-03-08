package kr.or.ddit.meeting.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.meeting.exception.AlreadyReservationException;
import kr.or.ddit.meeting.exception.RoomInactiveException;
import kr.or.ddit.meeting.service.MeetingReservationService;
import kr.or.ddit.mybatis.mapper.MeetingRecurringBookingMapper;
import kr.or.ddit.mybatis.mapper.MeetingReservationMapper;
import kr.or.ddit.mybatis.mapper.MeetingRoomMapper;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.MeetingRecurringBookingVO;
import kr.or.ddit.vo.MeetingReservationVO;
import kr.or.ddit.vo.MeetingRoomVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import kr.or.ddit.mybatis.mapper.MeetingReservationMapper;
import kr.or.ddit.mybatis.mapper.MeetingRoomMapper;
import kr.or.ddit.security.CustomUserDetails;

/**
 *
 * @author 윤서현
 * @since 2025. 10. 18.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 18.     	윤서현	          최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class MeetingReservationServiceImpl implements MeetingReservationService{

	private final MeetingReservationMapper mapper;
	private final MeetingRoomMapper meetingRoomMapper;
	private final MeetingRecurringBookingMapper recurringBookingMapper;

	/**
	 * 회의실 예약 (기존 메서드 - REST Controller용, SecurityContext 사용)
	 */
	@Override
	public MeetingReservationVO createMeetingReservation(MeetingReservationVO meetingReservation) {
	    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
	    UsersVO realUser = userDetails.getRealUser();

	    return createMeetingReservationInternal(meetingReservation, realUser);
	}

	/**
	 * 회의실 예약 (오버로드 - AI/WebSocket용, UsersVO 직접 전달)
	 */
	@Override
	public MeetingReservationVO createMeetingReservation(MeetingReservationVO meetingReservation, UsersVO user) {
	    if (user == null) {
	        throw new IllegalArgumentException("사용자 정보가 필요합니다.");
	    }

	    return createMeetingReservationInternal(meetingReservation, user);
	}
	
	/**
	 * 실제 예약 로직 (공통 private 메서드)
	 */
	public MeetingReservationVO createMeetingReservationInternal(MeetingReservationVO meetingReservation, UsersVO realUser) {

		// 만약 사용여부 'N' 이면 예약 못하게
		MeetingRoomVO roomInfoVO = meetingRoomMapper.selectMeetingRoom(meetingReservation.getRoomId());
		if(roomInfoVO.getUseYn().equals("N")) {
			throw new RoomInactiveException("예약 불가능한 회의실입니다.");
		}

		// 회의 제목 입력하지 않으면 사용자 + 부서명
		String reservationTitle = meetingReservation.getTitle() != null ? meetingReservation.getTitle() : "";
		if(reservationTitle.isBlank()) {
			reservationTitle = realUser.getUserNm() + " (" + realUser.getDeptNm() + ")";
			meetingReservation.setTitle(reservationTitle);
		}

		// 해당 시간대에 회의가 예약되어있는지 리스트 가져옴
		List<MeetingReservationVO> existReservationList = mapper.selectExistMeetingReservation(meetingReservation);

		int rowcnt = 0;
		// 위에서 갖고온 리스트가 비었다면 예약 가능한 상황
		if (existReservationList.isEmpty()) {
			rowcnt = mapper.insertMeetingReservation(meetingReservation);
			if (rowcnt >  0) {
				// 예약하고 예약한 내용 컨트롤러에 보내줌 (비동기 후처리용)
				meetingReservation = mapper.selectMeetingReservation(meetingReservation.getReservationId());
			}
		} else {
			throw new AlreadyReservationException("이미 예약된 시간대입니다.");
		}

		return meetingReservation;
	}

	/**
	 * 오늘의 회의실 예약 리스트
	 * @param reservation 오늘의 날짜를 담은 vo
	 * @return
	 */
	@Override
	public List<MeetingReservationVO> readMeetingReservationList(MeetingReservationVO reservation) {
		return mapper.selectMeetingReservationList(reservation);
	}

	@Override
	public MeetingReservationVO readMeetingReservation(int reservationId) {
		MeetingReservationVO meetingReservation = mapper.selectMeetingReservation(reservationId);
		if(meetingReservation == null) {
			throw new EntityNotFoundException(meetingReservation);
		}
		return meetingReservation;
	}

	/**
	 * 회의실 예약 정보 수정 서비스
	 */
	@Override
	public MeetingReservationVO modifyMeetingReservation(MeetingReservationVO meetingReservation) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();

		List<MeetingReservationVO> existReservationList = mapper.selectExistMeetingReservation(meetingReservation);

		int rowcnt = 0;
		if (existReservationList.isEmpty()) {
			// 회의 제목 입력하지 않으면 사용자 + 부서명
			String reservationTitle = meetingReservation.getTitle() != null ? meetingReservation.getTitle() : "";
			if(reservationTitle.isBlank()) {
				reservationTitle = realUser.getUserNm() + " (" + realUser.getDeptNm() + ")";
				meetingReservation.setTitle(reservationTitle);
			}

			rowcnt = mapper.updateMeetingReservation(meetingReservation);
			if (rowcnt > 0) meetingReservation = mapper.selectMeetingReservation(meetingReservation.getReservationId());
		} else {
			throw new AlreadyReservationException("이미 예약된 시간대입니다.");
		}
		return meetingReservation;
	}

	/**
	 * 회의실 예약 취소 서비스
	 */
	@Override
	public boolean removeMeetingReservation(Integer reservationId) {
		if (reservationId == null) return false;
		
		MeetingReservationVO reservationVO = new MeetingReservationVO();
		reservationVO.setReservationId(reservationId);
		return mapper.deleteMeetingReservation(reservationVO) > 0;
	}

	/**
	 * 반복예약한 회의실 예약 취소
	 */
	@Override
	@Transactional
	public boolean removeRecurringBookingMeetingReservation(Integer recurringId) {
		if (recurringId == null) return false;

		int rowcnt = 0;

		// 반복예약 ID 를 가지고 있는거 예약내역 삭제
		MeetingReservationVO reservationVO = new MeetingReservationVO();
		reservationVO.setRecurringId(recurringId);
		rowcnt = mapper.deleteMeetingReservation(reservationVO);

		// 반복예약 취소 상태로 변경 (B305 : 취소)
		MeetingRecurringBookingVO recurringBookingVO = new MeetingRecurringBookingVO();
		recurringBookingVO.setRecurringId(recurringId);
		recurringBookingVO.setStatus("B305");
		recurringBookingMapper.updateRecurringBooking(recurringBookingVO);

		return rowcnt > 0;
	}

}
