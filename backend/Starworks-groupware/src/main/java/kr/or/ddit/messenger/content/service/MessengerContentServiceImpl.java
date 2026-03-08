package kr.or.ddit.messenger.content.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.mybatis.mapper.MessengerContentMapper;
import kr.or.ddit.vo.MessengerContentVO;
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
public class MessengerContentServiceImpl implements MessengerContentService{

	private final MessengerContentMapper mapper;
	
	@Override
	public boolean createMessengerContent(MessengerContentVO mesContent) {
		return mapper.insertMessengerContent(mesContent) > 0;
	}

	@Override
	public List<MessengerContentVO> readMessengerContentList() {
		return mapper.selectMessengerContentList();
	}

	@Override
	public boolean removeMessengerContent(String msgContId) {
		return mapper.deleteMessengerContent(msgContId) > 0;
	}

}
