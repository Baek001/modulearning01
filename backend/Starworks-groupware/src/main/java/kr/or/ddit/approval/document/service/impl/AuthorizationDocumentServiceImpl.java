package kr.or.ddit.approval.document.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.approval.document.service.AuthorizationDocumentService;
import kr.or.ddit.approval.line.service.AuthorizationLineService;
import kr.or.ddit.comm.exception.DocumentAccessDeniedException;
import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.mybatis.mapper.AuthorizationDocumentMapper;
import kr.or.ddit.mybatis.mapper.AuthorizationLineMapper;
import kr.or.ddit.mybatis.mapper.AuthorizationTempMapper;
import kr.or.ddit.vo.AuthorizationDocumentVO;
import kr.or.ddit.vo.AuthorizationLineVO;
import kr.or.ddit.vo.AuthorizationTempVO;
import kr.or.ddit.websocket.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 임가영
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           	  수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	          최초 생성
 *  2025. 9. 30. 		홍현택			전자결재 목록조회 메서드 작성
 *	2025.10. 01.		윤서현			전자결재 기안문 등록
 *	2025.10. 02.		임가영			보완 주석 추가
 *	2025. 10. 4.		임가영			 modifyAuthorizationDocumentSign(htmlData 업데이트) 메소드 추가
 *	2025. 10.10. 		홍현택			updateDocumentStatus(회수) 메소드 추가
 *	2025. 10.10. 		홍현택			회수 -> 임시저장 saveRetractedAsTemp 메소드 추가
 *	2025. 10.10.		홍현택			목록 조회에 페이징 추가
 *  2025. 10.10.		임가영			결재 요청 시 알림 발송 추가
 *  2025. 10.25.		홍현택		보안 레벨에 따른 문서접근 권한 추가
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationDocumentServiceImpl implements AuthorizationDocumentService {

	private final AuthorizationDocumentMapper mapper;
	private final AuthorizationLineService service ;
	private final AuthorizationLineMapper lineMapper;
	private final FileUploadServiceImpl fileService;
	private final AuthorizationTempMapper mapperTemp;

	private final AuthorizationDocumentMapper sqnMapper;

	// 알림 관련 의존성 주입
	private final NotificationServiceImpl notificationService;

	/**
     * 전자결재 메인 목록 조회
     */
    @Override
    public List<AuthorizationDocumentVO> readAuthDocumentList() {
        return mapper.selectAuthDocumentList();
    }

    /**
     * 전자결재 메인 상세 조회
     * @throws EntityNotFoundException 조회 결과 없을 경우
     */
    @Override
    public AuthorizationDocumentVO readAuthDocument(String atrzDocId, String loginId) {
        AuthorizationDocumentVO vo = mapper.selectAuthDocument(atrzDocId, loginId);
        if (vo == null) {
            throw new EntityNotFoundException(
                String.format("전자결재 문서(ID=%s)를 찾을 수 없거나 접근 권한이 없습니다.", atrzDocId)
            );
        }

        // 보안 레벨 체크 10.25 현택 추가
        if (vo.getAtrzSecureLvl() != null && vo.getAtrzSecureLvl() >= 3) {
            boolean isAuthor = loginId.equals(vo.getAtrzUserId());
            boolean isApprover = false;

            List<AuthorizationLineVO> approvalLines = service.readAuthorizationLineList(atrzDocId);
            vo.setApprovalLines(approvalLines);

            for (AuthorizationLineVO line : approvalLines) {
                if (loginId.equals(line.getAtrzApprUserId())) {
                    isApprover = true;
                    break;
                }
            }

            if (!isAuthor && !isApprover) {
                throw new DocumentAccessDeniedException("이 문서를 열람할 권한이 없습니다.");
            }
        } else {
            // 결재선 조회 + 세팅
            vo.setApprovalLines(service.readAuthorizationLineList(atrzDocId));
        }

        return vo;
    }

	// 아래부터 보관함

	/**
	 * 본인이 기안한 문서 중, 완료된 문서 확인 (페이징 O)
	 */
	@Override
	public List<AuthorizationDocumentVO> readAuthorizationDocumentListUser(AuthorizationDocumentVO avo, PaginationInfo<AuthorizationDocumentVO> paging) {
		List<AuthorizationDocumentVO> list = mapper.selectAuthorizationDocumentListUserNonPaging(avo);
		int totalRecoard = list.size();
		paging.setTotalRecord(totalRecoard);

		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("authorizationDocument", avo);
		paramMap.put("paging", paging);

		return mapper.selectAuthorizationDocumentListUser(paramMap);
	}

	/**
	 * 본인 부서의 모든 완료된 문서 확인 (페이징 O)
	 */
	@Override
	public List<AuthorizationDocumentVO> readAuthorizationDocumentListDepart(AuthorizationDocumentVO avo, PaginationInfo<AuthorizationDocumentVO> paging) {
		List<AuthorizationDocumentVO> list = mapper.selectAuthorizationDocumentListDepartNonPaging(avo);
		int totalRecoard = list.size();
		paging.setTotalRecord(totalRecoard);

		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("authorizationDocument", avo);
		paramMap.put("paging", paging);

		return mapper.selectAuthorizationDocumentListDepart(paramMap);
	}



	/**
	 * 내가 기안한 문서!
	 */
	@Override
	public List<AuthorizationDocumentVO> readMyAllCombined(String userId, PaginationInfo<AuthorizationDocumentVO> paging) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", userId);
        paramMap.put("paging", paging);

        int totalRecord = mapper.countMyAllCombined(paramMap);
        paging.setTotalRecord(totalRecord);

		return mapper.selectMyAllCombined(paramMap);
	}

	/**
	 * 내가 수신(결재) 하는 문서!
	 */
	@Override
	public List<AuthorizationDocumentVO> readMyInboxCombined(String userId, PaginationInfo<AuthorizationDocumentVO> paging) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", userId);
        paramMap.put("paging", paging);

        int totalRecord = mapper.countMyInboxCombined(paramMap);
        paging.setTotalRecord(totalRecord);

		return mapper.selectMyInboxCombined(paramMap);
	}


	/**
	 * 전체..!
	 */
	@Override
    public List<AuthorizationDocumentVO> readMyDraftList(String userId, PaginationInfo<AuthorizationDocumentVO> paging) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", userId);
        paramMap.put("paging", paging);

        int totalRecord = mapper.countMyDraftList(paramMap);
        paging.setTotalRecord(totalRecord);

        return mapper.selectMyDraftList(paramMap);
    }

	@Override
	public List<AuthorizationDocumentVO> readMyProcessedList(String userId, PaginationInfo<AuthorizationDocumentVO> paging) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", userId);
        paramMap.put("paging", paging);

        int totalRecord = mapper.countMyProcessedList(paramMap);
        paging.setTotalRecord(totalRecord);

		return mapper.selectMyProcessedList(paramMap);
	}

	/**
	 * 전자결재에서 작성한 기안문 등록(저장)
	 * @return
	 */
	@Override
	public AuthorizationDocumentVO insertAuthDocument(AuthorizationDocumentVO authDoc) {

		// 첨부파일 저장
		if (authDoc.getFileList() != null) {
			fileService.saveFileS3(authDoc, FileFolderType.APPROVAL.toString());
		}

		mapper.insertAuthDocument(authDoc);

		String firstApproval = null; // 첫 번째 결재자
		//결재선 저장
		if(authDoc.getApprovalLines() != null) {
			int seq = 1;
			for(AuthorizationLineVO line : authDoc.getApprovalLines()) {
				if(seq == 1) {
					line.setAtrzApprStts("A301");
					firstApproval = line.getAtrzApprUserId();
				}
				line.setAtrzDocId(authDoc.getAtrzDocId());
				line.setAtrzLineSeq(seq++);
				lineMapper.insertAuthLine(line);
			}
		}

		// =========== '새 결재 요청이 있습니다.' 알림 발송 ===========
		Map<String, Object> payload = new HashMap<>();
		payload.put("receiverId", firstApproval);
		payload.put("senderId", authDoc.getAtrzUserId());
		payload.put("alarmCode", "APPROVAL_01");
		payload.put("pk", authDoc.getAtrzDocId());

		notificationService.sendNotification(payload);
		// ===================================================

		return authDoc;
	}



	/**
	 * 기안서 업데이트
	 */
	@Override
	public boolean modifyAuthorizationDocumentSign(AuthorizationDocumentVO authorizationDocument) {
		return mapper.updateAuthorizationDocument(authorizationDocument) > 0;
	}

	/**
	 * 전자결재 회수.
	 */
	@Override
	@Transactional // Add transactional annotation as it now performs multiple DB operations
	public int updateDocumentStatus(String docId, String sttsCode) {
		int affectedRows = mapper.updateDocumentStatus(docId, sttsCode);

		// 문서 상태가 '회수'(A205)로 변경될 경우, 모든 결재 라인의 상태를 NULL로 초기화
		if (affectedRows > 0 && "A205".equals(sttsCode)) {
			lineMapper.updateAllLineStatusToNullByDocId(docId);
		}
		return affectedRows;
	}

	/**
	 * 회수 시 임시저장.
	 */
	@Override
	@Transactional
	public int saveRetractedAsTemp(String docId, String htmlData) { // htmlData parameter added
		// 1. 원본 AuthorizationDocumentVO 조회
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String loginId = authentication.getName();
		AuthorizationDocumentVO originalDoc = mapper.selectAuthDocument(docId, loginId);
		if (originalDoc == null) {
			throw new EntityNotFoundException(String.format("문서(ID=%s)를 찾을 수 없습니다.", docId));
		}

		// 2. AuthorizationTempVO 생성 및 데이터 복사
		AuthorizationTempVO tempVO = new AuthorizationTempVO();
		// atrzTempSqn은 insert 시 selectKey로 자동 생성
		tempVO.setAtrzUserId(originalDoc.getAtrzUserId());
		tempVO.setAtrzDocTmplId(originalDoc.getAtrzDocTmplId());
		tempVO.setAtrzDocTtl(originalDoc.getAtrzDocTtl());
		tempVO.setAtrzSbmtDt(originalDoc.getAtrzSbmtDt()); // 임시저장 시점은 아님, 원본 기안일 유지
		tempVO.setHtmlData(htmlData); // Use the passed htmlData
		tempVO.setAtrzFileId(originalDoc.getAtrzFileId());
		tempVO.setDelYn("N"); // 임시저장함에서는 삭제되지 않은 상태
		tempVO.setOpenYn(originalDoc.getOpenYn());

		// 3. AuthorizationTemp 테이블에 저장
		int insertedCount = mapperTemp.insertAuthorizationTemp(tempVO);
		if (insertedCount == 0) {
			throw new IllegalStateException("임시 저장함에 문서 저장 실패.");
		}

		// 4. 원본 AuthorizationDocument의 상태를 '임시저장'(A201)으로, DEL_YN을 'Y'로 변경
		return mapper.updateDocumentStatusAndDelYn(docId, "A201", "Y");
	}

	@Override
	@Transactional
	public int softDeleteDocumentAsAdmin(String docId) {
		AuthorizationDocumentVO authDoc = new AuthorizationDocumentVO();
		authDoc.setAtrzDocId(docId);
		return mapper.updateAuthDocument(authDoc);
	}

	@Override
	public String getNextDocId() {
		 return sqnMapper.getNextDocId();
	}

	/**
	 * 오늘의 결재 수 카운트(관리자 대시보드)
	 */
	@Override
	public int getApprovalCount() {
		return mapper.selectApprovalCount();
	}


}
