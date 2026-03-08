package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.MeetingMemoVO;

/**
 *
 * @author 윤서현
 * @since 2025. 10. 17.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 17.     	윤서현	          최초 생성
 *
 * </pre>
 */
@Mapper
public interface MeetingMemoMapper {

	/**
	 * 회의록 메모 상세조회
	 * @param roomId
	 * @return
	 */
	public MeetingMemoVO selectMeetingMemo(int memoId);

	/**
	 * 회의록 메모 리스트 조회
	 * @param memoVO 사용자Id 를 담은 VO
	 * @return
	 */
	public List<MeetingMemoVO> selectMeetingMemoList(MeetingMemoVO memoVO);

	/**
	 * 회의록 메모 추가
	 * @param meetRoom
	 * @return
	 */
	public int insertMeetingMemo(MeetingMemoVO meetMemo);

	/**
	 * 회의록 메모 수정
	 * @param meetRoom
	 * @return
	 */
	public int updateMeetingMemo(MeetingMemoVO meetMemo);

	/**
	 * 회의록 메모 삭제
	 * @param roomId
	 * @return
	 */
	public int deleteMeetingMemo(int memoId);
}
