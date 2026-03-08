package kr.or.ddit.messenger.user.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.MessengerUserMapper;
import kr.or.ddit.vo.MessengerUserVO;
import lombok.RequiredArgsConstructor;

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
@Service
@RequiredArgsConstructor
public class MessengerUserServiceImpl implements MessengerUserService{

	private final MessengerUserMapper mapper;

	@Override
	public boolean createMessengerUser(MessengerUserVO mesUser) {
		return mapper.insertMessengerUser(mesUser) > 0;
	}

	@Override
	public List<MessengerUserVO> readMessengerUserList() {
		return mapper.selectMessengerUserList();
	}

	@Override
	public MessengerUserVO readMessengerUser(String userId) {
		MessengerUserVO mesUser = mapper.selectMessengerUser(userId);
		if(mesUser == null) {
			throw new EntityNotFoundException(mesUser);
		}
		return mesUser;
	}

	@Override
	public boolean removeMessengerUser(String userId) {
		return mapper.deleteMessengerUser(userId) > 0;
	}
}
