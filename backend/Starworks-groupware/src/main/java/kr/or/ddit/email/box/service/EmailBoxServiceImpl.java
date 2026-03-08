package kr.or.ddit.email.box.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.mybatis.mapper.EmailBoxMapper;
import kr.or.ddit.vo.EmailBoxVO;
import kr.or.ddit.vo.EmailContentVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 홍현택
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	홍현택	          최초 생성
 *  2025.10. 10.		홍현택		메일함 목록 조회 메서드retrieveEmailList 추가
 *  2025.10. 15.		홍현택		메일함별 메일 총갯수 조회 메서드readEmailListCount
 *  2025.10. 15. 		홍현택		읽지 않은 메일 갯수 조회 메서드 readUnreadEmailCount
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class EmailBoxServiceImpl implements EmailBoxService {

	@Autowired
    private EmailBoxMapper mapper;


    /**
     *이메일함 목록을 불러오는 기능
     */
    @Override
    public List<EmailBoxVO> readEmailBoxList() {
        return mapper.selectEmailBoxList();
    }

    /**
     * 이메일함 하나 불러오는 기능
     */
    @Override
    public EmailBoxVO readEmailBox(String mailboxId) {
        EmailBoxVO box = mapper.selectEmailBox(mailboxId);
        if (box == null) throw new EntityNotFoundException(mailboxId);
        return box;
    }

    /**
     * 메일 리스트 조회
     */
    @Override
    public List<EmailContentVO> searchEmailList(String userId, String mailboxTypeCd, PaginationInfo<EmailContentVO> paging) {
        return mapper.selectEmailList(userId, mailboxTypeCd, paging);
    }

    /**
     * 메일 리스트 총 개수 조회
     */
    @Override
    public int readEmailListCount(String userId, String mailboxTypeCd, PaginationInfo<EmailContentVO> paging) {
        return mapper.selectEmailListCount(userId, mailboxTypeCd, paging);
    }

    /**
     * 메일 리스트 중 읽지 않은 메일 총 개수 조회
     */
    @Override
    public int readUnreadEmailCount(String userId, String mailboxTypeCd) {
        return mapper.selectUnreadEmailCount(userId, mailboxTypeCd);
    }
}
