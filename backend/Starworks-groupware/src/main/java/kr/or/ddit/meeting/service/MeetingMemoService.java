package kr.or.ddit.meeting.service;

import java.util.List;

import kr.or.ddit.vo.MeetingMemoVO;

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
public interface MeetingMemoService {

	/**
	 * 회의실 메모 등록
	 * @param meetingMemo 사용자 Id, 회의 제목, 메모 내용을 담은 VO (만약 나의 회의에 추가 버튼을 눌렀다면 메모 내용X)
	 * @return MeetingMemoVO 회의 메모 Id 가 담긴 vo
	 */
	public MeetingMemoVO createMeetingMemo(MeetingMemoVO meetingMemo);

	/**
	 * 회의실 메모 리스트 조회
	 * @param 사용자 Id 를 담은 memoVO
	 * @return
	 */
	public List<MeetingMemoVO> readMeetingMemoList(MeetingMemoVO memoVO);

	/**
	 * 회의실 메모 상세조회
	 * @param memoId
	 * @return
	 */
	public MeetingMemoVO readMeetingMemo(int memoId);

	/**
	 * 회의실 메모 수정
	 * @param meetingMemo
	 * @return
	 */
	public boolean modifyMeetingMemo(MeetingMemoVO meetingMemo);

	/**
	 * 회의실 메모 삭제
	 * @param memoId
	 * @return
	 */
	public boolean removeMeetingMemo(int memoId);
}
