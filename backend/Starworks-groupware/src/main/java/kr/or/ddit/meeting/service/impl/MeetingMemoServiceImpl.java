package kr.or.ddit.meeting.service.impl;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.meeting.service.MeetingMemoService;
import kr.or.ddit.mybatis.mapper.JobGradeMapper;
import kr.or.ddit.mybatis.mapper.MeetingMemoMapper;
import kr.or.ddit.vo.MeetingMemoVO;
import lombok.RequiredArgsConstructor;

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
public class MeetingMemoServiceImpl implements MeetingMemoService{

	private final MeetingMemoMapper mapper;

	@Override
	public MeetingMemoVO createMeetingMemo(MeetingMemoVO meetingMemo) {
		if (meetingMemo.getTitle() != null && !meetingMemo.getTitle().isBlank()) {
			meetingMemo.setTitle("회의 메모 : " + meetingMemo.getTitle());
		} else {
			meetingMemo.setTitle("회의 메모");
		}

		// 메모 등록
		mapper.insertMeetingMemo(meetingMemo);
		// 등록한 메모 정보 가져오기
		meetingMemo = mapper.selectMeetingMemo(meetingMemo.getMemoId());

		return meetingMemo;
	}

	@Override
	public List<MeetingMemoVO> readMeetingMemoList(MeetingMemoVO memoVO) {
		return mapper.selectMeetingMemoList(memoVO);
	}

	@Override
	public MeetingMemoVO readMeetingMemo(int memoId) {
		MeetingMemoVO meetingMemo = mapper.selectMeetingMemo(memoId);
		if(meetingMemo == null) {
			throw new EntityNotFoundException(meetingMemo);
		}
		return meetingMemo;
	}

	@Override
	public boolean modifyMeetingMemo(MeetingMemoVO meetingMemo) {
		return mapper.updateMeetingMemo(meetingMemo) > 0;
	}

	@Override
	public boolean removeMeetingMemo(int memoId) {
		return mapper.deleteMeetingMemo(memoId) > 0;
	}




}
