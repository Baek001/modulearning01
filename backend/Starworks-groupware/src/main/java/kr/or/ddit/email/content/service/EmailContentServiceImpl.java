package kr.or.ddit.email.content.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 import 추가

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.mybatis.mapper.EmailBoxMapper;
import kr.or.ddit.mybatis.mapper.EmailContentMapper;
import kr.or.ddit.mybatis.mapper.EmailMappingMapper;
import kr.or.ddit.mybatis.mapper.EmailReceiverMapper;
import kr.or.ddit.mybatis.mapper.EmailSenderPartyMapper;
import kr.or.ddit.users.service.UsersService;
import kr.or.ddit.vo.EmailContentVO;
import kr.or.ddit.vo.EmailMappingVO;
import kr.or.ddit.vo.EmailReceiverVO;
import kr.or.ddit.vo.EmailSenderPartyVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 홍현택
 *
 *         /**
 *
 * @author 홍현택
 * @since 2025. 9. 26.
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	홍현택	          최초 생성
 *  2025. 9. 26.		임가영			int totalRecoard 가져오는 코드 추가
 *  2025.10. 13.		홍현택			readEmailDetail 메일 읽음 표시 추가
 *  2025.10. 13. 		홍현택			readDraft 첨부파일 추가
 *  2025.10. 14.		홍현택			중요 편지함을 위해 특정 이메일 매핑정보 삭제
 *  2025.10. 14. 		홍현택			메일 컨텐츠 삭제 메서드 제거
 *
 *      </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailContentServiceImpl implements EmailContentService {

	@Autowired
	private EmailContentMapper mapper;
	@Autowired
	private EmailMappingMapper emailMappingMapper; // emailMappingMapper 주입
	@Autowired
	private EmailSenderPartyMapper emailSenderPartyMapper; // emailSenderPartyMapper 주입
	@Autowired
	private EmailReceiverMapper emailReceiverMapper; // emailReceiverMapper 주입
	@Autowired
	private EmailBoxMapper emailBoxMapper; // EmailBoxMapper 주입
	@Autowired
	private FileDetailService fileDetailService; // FileDetailService 주입
	@Autowired
	private UsersService usersService; // UsersService 주입

	/**
	 * 메일 전문 리스트 페이징처리로 가져오기
	 */
	@Override
	public List<EmailContentVO> readEmailContentList(PaginationInfo<EmailContentVO> paging) {
		int totalRecord = mapper.selectTotalRecord(paging);
		paging.setTotalRecord(totalRecord);

		return mapper.selectEmailContentList(paging);
	}

	/**
	 * 메일 전문 전체 레코드 수 페이징
	 */
	@Override
	public int readEmailContentTotalRecord(PaginationInfo<EmailContentVO> paging) {
		return mapper.selectTotalRecord(paging);
	}

	/**
	 * 메일 전문 리스트 가져오기
	 */
	@Override
	public EmailContentVO readEmailContent(String userId) throws EntityNotFoundException {
		EmailContentVO vo = mapper.selectEmailContent(userId);
		if (vo == null) {
			throw new EntityNotFoundException(userId);
		}
		return vo;
	}

	/**
	 * 메일 전문 등록
	 */
	@Override
	public boolean registerEmailContent(EmailContentVO emailContent) {
		return mapper.insertEmailContent(emailContent) > 0;
	}

	/**
	 *메일 읽었을때 상태 변경
	 */
	@Transactional
	@Override
	public EmailContentVO readEmailDetail(String emailContId, String userId) {
		// 1. 메일을 '읽음' 상태로 변경
		Map<String, Object> params = new HashMap<>();
		params.put("emailContId", emailContId);
		params.put("userId", userId);
		emailMappingMapper.updateReadStatus(params);

		// 2. 메일 상세 정보 조회
		EmailContentVO email = mapper.selectEmailContent(emailContId);
		if (email == null) {
			throw new EntityNotFoundException(emailContId);
		}

		// 3. 발신자 정보 조회 및 설정
		EmailSenderPartyVO sender = emailSenderPartyMapper.selectSenderByEmailId(emailContId);
		if (sender != null) {
			email.setSenderName(sender.getUserName());
			// 발신자의 상세 정보 (부서, 이메일) 조회
			UsersVO senderUser = usersService.readUser(sender.getUserId());
			if (senderUser != null) {
				email.setSenderDeptName(senderUser.getDeptNm());
				email.setSenderEmail(senderUser.getUserEmail());
			}
		}

		// 4. 수신자 목록 조회 및 설정
		List<EmailReceiverVO> receivers = emailReceiverMapper.selectReceiversByEmailId(emailContId);
		email.setReceiverList(receivers);

		// 4.1. 수신자 상세 정보 조회 (UsersVO) 및 설정 (JSP에서 사용하기 위함)
		if (receivers != null && !receivers.isEmpty()) {
		    List<UsersVO> recipientUserList = new java.util.ArrayList<>();
		    for (EmailReceiverVO receiver : receivers) {
		        UsersVO receiverUser = usersService.readUser(receiver.getUserId());
		        if (receiverUser != null) {
		            recipientUserList.add(receiverUser);
		        }
		    }
		    email.setRecipientUserList(recipientUserList);
		}

		// 5. 첨부파일 정보 조회 및 설정
		if (email.getMailFileId() != null && !email.getMailFileId().isEmpty()) {
			List<kr.or.ddit.vo.FileDetailVO> attachments = fileDetailService.readFileDetailList(email.getMailFileId());
			email.setAttachmentList(attachments);
		}
		return email;
	}

	/**
	 * 상세 조회
	 */
	@Override
	public EmailContentVO readDraft(String emailContId, String userId) {
		// 1. 메일 상세 정보 조회
		EmailContentVO email = mapper.selectEmailContent(emailContId);
		if (email == null) {
			throw new EntityNotFoundException(emailContId);
		}

		// 2. 발신자 정보 조회 및 설정
		EmailSenderPartyVO sender = emailSenderPartyMapper.selectSenderByEmailId(emailContId);
		if (sender != null) {
			email.setSenderName(sender.getUserName());
		}

		// 3. 수신자 ID 목록 조회
		List<EmailReceiverVO> receivers = emailReceiverMapper.selectReceiversByEmailId(emailContId);
		email.setReceiverList(receivers);

		// 4. 수신자 상세 정보 조회 (UsersVO) 및 설정
		if (receivers != null && !receivers.isEmpty()) {
		    List<UsersVO> recipientUserList = new java.util.ArrayList<>();
		    for (EmailReceiverVO receiver : receivers) {
		        UsersVO receiverUser = usersService.readUser(receiver.getUserId());
		        if (receiverUser != null) {
		            recipientUserList.add(receiverUser);
		        }
		    }
		    email.setRecipientUserList(recipientUserList);
		}

		// 5. 첨부파일 정보 조회 및 설정
		if (email.getMailFileId() != null && !email.getMailFileId().isEmpty()) {
			List<kr.or.ddit.vo.FileDetailVO> attachments = fileDetailService.readFileDetailList(email.getMailFileId());
			email.setAttachmentList(attachments);
		}

		return email;

	}
	/**
	 * 중요 메일 상태 변환(토글)
	 */
	@Override
    @Transactional
    public boolean toggleImportance(String userId, String emailContId) {

    	// 1. 사용자의 중요메일함(G104) ID 조회
    	Map<String, Object> findParams = new HashMap<>();
    	findParams.put("userId", userId);
    	findParams.put("mailboxTypeCd", "G104"); // G104: 중요 편지함
    	String importantMailboxId = emailBoxMapper.findMailboxIdByUserIdAndType(findParams);

    	if (importantMailboxId == null) {
    		log.error("사용자 {}의 중요 편지함(G104)을 찾을 수 없습니다.", userId);
    		throw new RuntimeException("중요 편지함을 찾을 수 없습니다.");

    	}
    	// 2. 해당 메일이 이미 중요메일함에 매핑되어 있는지 확인
    	Map<String, Object> selectParams = new HashMap<>();
    	selectParams.put("mailboxId", importantMailboxId);
    	selectParams.put("emailContId", emailContId);
    	EmailMappingVO existingMapping = emailMappingMapper.selectEmailMapping(selectParams);

    	EmailMappingVO toggleMapping = new EmailMappingVO();
    	toggleMapping.setMailboxId(importantMailboxId);
    	toggleMapping.setEmailContId(emailContId);

    	if (existingMapping != null) {
    		// 3-1. 이미 존재하면 매핑 삭제 (중요 → 일반)
    		emailMappingMapper.deleteEmailMapping(toggleMapping);
    		return false; // 중요하지 않음 상태로 변경

    	} else {
    		// 3-2. 없으면 매핑 추가 (일반 → 중요)
    		emailMappingMapper.insertEmailMapping(toggleMapping);
    		return true; // 중요함 상태로 변경

    	}

    }

	/**
	 * 관리자 대시보드 이메일 건수
	 */
	@Override
	public int readEmailCount() {
		return mapper.selectEmailCount();
	}

}
