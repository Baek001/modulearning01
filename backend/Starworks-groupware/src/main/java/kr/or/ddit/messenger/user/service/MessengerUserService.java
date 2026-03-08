package kr.or.ddit.messenger.user.service;

import java.util.List;

import kr.or.ddit.vo.MessengerUserVO;

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
public interface MessengerUserService {

	/**
	 * 메신저 참여자 추가
	 * @param mesUser
	 * @return
	 */
	public boolean createMessengerUser(MessengerUserVO mesUser);
	/**
	 * 메신저 참여자 목록 조회
	 * @return
	 */
	public List<MessengerUserVO> readMessengerUserList();
	/**
	 * 메신저 참여자 상세조회
	 * @param userId
	 * @return
	 */
	public MessengerUserVO readMessengerUser(String userId);
	/**
	 * 메신저 참여자 삭제
	 * @param userId
	 * @return
	 */
	public boolean removeMessengerUser(String userId);
}
