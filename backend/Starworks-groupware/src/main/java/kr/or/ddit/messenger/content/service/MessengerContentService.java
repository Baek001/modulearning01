package kr.or.ddit.messenger.content.service;

import java.util.List;

import kr.or.ddit.vo.MessengerContentVO;

/**
 * 
 * @author 윤서현
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	윤서현	          최초 생성
 *
 * </pre>
 */
public interface MessengerContentService {
	/**
	 * 메신저 대화 추가
	 * @param mesContent
	 * @return
	 */
	public boolean createMessengerContent(MessengerContentVO mesContent);
	/**
	 * 메신저 대화 목록 조회
	 * @return
	 */
	public List<MessengerContentVO> readMessengerContentList();
	/**
	 * 메신저 대화 삭제
	 * @param msgContId
	 * @return
	 */
	public boolean removeMessengerContent(String msgContId);
}
