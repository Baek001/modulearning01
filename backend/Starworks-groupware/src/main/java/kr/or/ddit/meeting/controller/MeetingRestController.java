package kr.or.ddit.meeting.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.comm.paging.SimpleSearch;
import kr.or.ddit.comm.validate.InsertGroup;
import kr.or.ddit.comm.validate.UpdateGroup;
import kr.or.ddit.meeting.exception.AlreadyReservationException;
import kr.or.ddit.meeting.exception.RoomInactiveException;
import kr.or.ddit.meeting.service.MeetingMemoService;
import kr.or.ddit.meeting.service.MeetingReservationService;
import kr.or.ddit.meeting.service.MeetingRoomService;
import kr.or.ddit.vo.MeetingMemoVO;
import kr.or.ddit.vo.MeetingReservationVO;
import kr.or.ddit.vo.MeetingRoomVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 임가영
 * @since 2025. 10. 21.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 21.     	윤서현           최초 생성
 *  2025. 10. 21.		임가영		   예약을 위한 postMapping RestController 추가
 * </pre>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MeetingRestController {

	private final MeetingRoomService meetingRoomService;
	private final MeetingReservationService meetingReservationService;
	private final MeetingMemoService meetingMemoService;

	/**
	 * 회의실 리스트 조회
	 * @return
	 */
	@GetMapping("/rest/meeting/room")
	public List<MeetingRoomVO> readMeetingRoomList(){
		return meetingRoomService.readMeetingRoomList();
	}

	/**
	 * 회의실 추가
	 * @param room
	 * @return
	 */
	@PostMapping("/rest/meeting/room")
	public Map<String, Object> createMeetingRoom(
		@RequestBody @Validated(InsertGroup.class) MeetingRoomVO room
		, BindingResult errors
	) {
		Map<String, Object> respMap = new HashMap<>();
		boolean success = false;

		if (!errors.hasErrors()) {
			success = meetingRoomService.createMeetingRoom(room);
		} else {
			respMap.put("message", "필수 값을 입력하세요.");
		}

		respMap.put("success", success);
		return respMap;
	}

	/**
	 * 회의실 정보 수정
	 * @param room
	 * @return
	 */
	@PutMapping("/rest/meeting/room")
	public Map<String, Object> modifyMeetingRoom(
		@RequestBody @Validated(UpdateGroup.class) MeetingRoomVO room
		, BindingResult errors
	) {
		Map<String, Object> respMap = new HashMap<>();
		boolean success = false;

		if (!errors.hasErrors()) {
			success = meetingRoomService.modifyMeetingRoom(room);
		} else {
			respMap.put("message", "필수 값을 입력하세요.");
		}
		respMap.put("success", success);

		return respMap;
	}

	/**
	 * 회의실 열고 닫는 함수
	 * @param room
	 * @param errors
	 * @return
	 */
	@PutMapping("/rest/meeting/room/open-close")
	public Map<String, Object> closeAndOpenMeetingRoom(
		@RequestBody MeetingRoomVO room
	) {
		Map<String, Object> respMap = new HashMap<>();
		boolean success = false;

		success = meetingRoomService.openAndCloseMeetingRoom(room);

		respMap.put("success", success);

		return respMap;
	}

	/**
	 * 오늘의 예약 현황 조회
	 * @param date
	 * @return
	 */
	@GetMapping("/rest/meeting/reservations")
	public List<MeetingReservationVO> readMeetingReservationList(
		@RequestParam(required = false) String date
		, @RequestParam(required = false, name = "role") String role
	){

		MeetingReservationVO mrVO = new MeetingReservationVO();

		// admin 이라면 삭제된 회의실에 예약된 내역까지 다 가져옴
		if(role != null && role.equals("admin")) {
			mrVO.setAdmin(true);
		}

		if (date != null && !date.isEmpty()) {
			LocalDate setDate = LocalDate.parse(date);
			mrVO.setMeetingDate(setDate);
		} else {
			LocalDate today = LocalDate.now();
			mrVO.setMeetingDate(today);
		}

		return meetingReservationService.readMeetingReservationList(mrVO);
	}

	/**
	 * 예약 한 건 조회
	 * @param reservationId
	 * @return
	 */
	@GetMapping("/rest/meeting/reservations/{reservation}")
	public MeetingReservationVO readMeetingreservation(
		@PathVariable(name = "reservation") Integer reservationId
	) {
		return meetingReservationService.readMeetingReservation(reservationId);
	}

	/**
	 * 회의실 예약(등록)
	 * @param meetingReservation
	 * @return
	 */
	@PostMapping("/rest/meeting")
	public Map<String, Object> createMeetingReservation (
		@RequestBody @Validated(InsertGroup.class) MeetingReservationVO meetingReservation
		, BindingResult errors
		, Authentication authentication
	) {
		Map<String, Object> respMap = new HashMap<>();
		boolean success = false;

		if(!errors.hasErrors()) {
			try {
				meetingReservation.setUserId(authentication.getName());
				meetingReservation.setMeetingDate(
					meetingReservation.getMeetingDate() != null
						? meetingReservation.getMeetingDate()
						: LocalDate.now()
				);
				meetingReservation  = meetingReservationService.createMeetingReservation(meetingReservation);
				if (meetingReservation.getReservationId() != null) {
					respMap.put("reservation", meetingReservation);
					success = true;
				}
			} catch (AlreadyReservationException | RoomInactiveException  e) {
				respMap.put("success", false);
				respMap.put("message", e.getMessage());
				return respMap;
			}
		} else {
			// errors.getFieldErrors() : 검증 실패한 각 필드별 에러를 리스트로
			// .stream() 리스트를 스트림으로 변환해서 연산 가능하게
			// .collect(Collectors.toMap(...)) : 스트림의 각 요소를 key/value 쌍으로 변환해서 Map으로
			return Map.of("errors", errors.getFieldErrors().stream()
											.collect(Collectors.toMap(
												e -> e.getField(),
												e -> e.getDefaultMessage()
											))
										);
		}

		respMap.put("success", success);
		return respMap;
	}

	/**
	 * 회의실 예약 수정
	 * @param meetingReservation
	 * @param errors
	 * @return
	 */
	@PutMapping("/rest/meeting")
	public Map<String, Object> modifyMeetingReservation(
		@RequestBody @Validated(UpdateGroup.class) MeetingReservationVO meetingReservation
		, BindingResult errors
		, Authentication authentication
	) {
		Map<String, Object> respMap = new HashMap<>();
		boolean success = false;

		if(!errors.hasErrors()) {
			try {
				meetingReservation.setUserId(authentication.getName());
				meetingReservation.setMeetingDate(
					meetingReservation.getMeetingDate() != null
						? meetingReservation.getMeetingDate()
						: LocalDate.now()
				);
				meetingReservation = meetingReservationService.modifyMeetingReservation(meetingReservation);
				if (meetingReservation != null) {
					success = true;
					respMap.put("reservation", meetingReservation);
				}
			} catch (AlreadyReservationException e) {
				respMap.put("success", false);
				respMap.put("message", e.getMessage());
				return respMap;
			}
		} else {
			// errors.getFieldErrors() : 검증 실패한 각 필드별 에러를 리스트로
			// .stream() 리스트를 스트림으로 변환해서 연산 가능하게
			// .collect(Collectors.toMap(...)) : 스트림의 각 요소를 key/value 쌍으로 변환해서 Map으로
			return Map.of("errors", errors.getFieldErrors().stream()
											.collect(Collectors.toMap(
												e -> e.getField(),
												e -> e.getDefaultMessage()
											))
										);
		}

		respMap.put("success", success);
		return respMap;
	}

	/**
	 * 회의실 예약 취소
	 * @param reservationId
	 * @return
	 */
	@DeleteMapping("/rest/meeting/{reservation}")
	public Map<String, Object> removeMeetingReservation(
			@PathVariable(name = "reservation") Integer reservationId
	) {
		Map<String, Object> respMap = new HashMap<>();
		boolean success = false;

		success = meetingReservationService.removeMeetingReservation(reservationId);

		respMap.put("success", success);
		return respMap;
	}

	/**
	 * 회의 메모 생성
	 * @param meetingMemo
	 * @return
	 */
	@PostMapping("/rest/meeting-memo")
	public Map<String, Object> createMeetingMemo(
		@RequestBody(required = false) MeetingMemoVO meetingMemo,
		Authentication authentication
	) {
		Map<String, Object> respMap = new HashMap<>();
		boolean success = false;

		// 만약 그냥 '새 메모' 버튼을 눌렀다면
		if (meetingMemo == null) meetingMemo = new MeetingMemoVO();
		// 메모 생성
		meetingMemo.setUserId(authentication.getName());

		meetingMemo = meetingMemoService.createMeetingMemo(meetingMemo);
		// 메모 Id 가 있다면 생성에 성공한 것
		if (meetingMemo.getMemoId() != null){
			respMap.put("memo", meetingMemo);
			success = true;
		}

		respMap.put("success", success);
		return respMap;
	}

	/**
	 * 회의 메모 삭제
	 * @param memoId
	 * @return
	 */
	@DeleteMapping("/rest/meeting-memo/{memoId}")
	public Map<String, Object> removeMeetingMemo(
		@PathVariable(name = "memoId", required = true) Integer memoId
	) {
		Map<String, Object> respMap = new HashMap<>();
		boolean success = false;

		success = meetingMemoService.removeMeetingMemo(memoId);

		respMap.put("success", success);
		return respMap;
	}

	/**
	 * 회의 메모 수정
	 * @param meetingMemo memoId 가 필수로 들어있는 vo
	 * @return
	 */
	@PutMapping("/rest/meeting-memo/{memoId}")
	public Map<String, Object> modifyMeetingMemo(
		@PathVariable(name = "memoId") String memoId
		, @RequestBody @Validated(UpdateGroup.class) MeetingMemoVO meetingMemo
		, Authentication authentication
	) {
		Map<String, Object> respMap = new HashMap<>();
		boolean success = false;

		meetingMemo.setUserId(authentication.getName());
		success = meetingMemoService.modifyMeetingMemo(meetingMemo);

		respMap.put("success", success);
		return respMap;
	}

	/**
	 * 메모 리스트 얻기
	 * @param searchWord
	 * @param authentication
	 * @return
	 */
	@GetMapping("/rest/meeting-memo")
	public Map<String, Object> readMeetingMemo(
		@RequestParam(name = "searchWord") String searchWord
		, Authentication authentication
	) {
		Map<String, Object> respMap = new HashMap<>();

		MeetingMemoVO memo = new MeetingMemoVO();
		memo.setUserId(authentication.getName());
		SimpleSearch simpleSearch = new SimpleSearch();

		if (searchWord != null && !searchWord.isBlank()) {
			simpleSearch.setSearchWord(searchWord);
			memo.setSimpleSearch(simpleSearch);
		}

		List<MeetingMemoVO> memoList = meetingMemoService.readMeetingMemoList(memo);
		respMap.put("memoList", memoList);
		return respMap;
	}

}
