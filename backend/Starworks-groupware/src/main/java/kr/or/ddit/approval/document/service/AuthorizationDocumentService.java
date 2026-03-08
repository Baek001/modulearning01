package kr.or.ddit.approval.document.service;

import java.util.List;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.AuthorizationDocumentVO;

/**
 *
 * @author 임가영
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	          최초 생성
 *	2025. 9. 30. 		홍현택			 로그인 사용자의 전자결재 목록 조회 작성 메서드
 *	2025. 10. 1.		홍현택   			 결재 상세보기 조회 메서드
 *	2025. 10. 1.  		윤서현			 기안서 작성 폼 등록 메서드
 *	2025. 10. 4.		임가영			 modifyAuthorizationDocumentSign(htmlData 업데이트) 메소드 추가
 *	2025. 10.10. 		홍현택			updateDocumentStatus(회수로 임시저장상태로 변경) 메소드 추가
 *	2025. 10.10.		홍현택			회수 -> 임시저장 saveRetractedAsTemp 메소드 추가
 *	2025. 10.10.		홍현택			목록 조회 페이징 추가
 * </pre>
 */
public interface AuthorizationDocumentService {


	/** 전자결재 메인에서 목록 조회
	 * @return
	 */
	public List<AuthorizationDocumentVO> readAuthDocumentList();

	// 아래부터 보관함

//	/**
//	 * 본인이 기안한 문서 중, 완료된 문서 확인 (페이징 X)
//	 * @param
//	 * @return 조회 결과 없으면 list.size() == 0
//	 */
//	public List<AuthorizationDocumentVO> readAuthorizationDocumentListUserNonPaging(AuthorizationDocumentVO avo);
//
//	/**
//	 * 본인 부서의 모든 완료된 문서 확인 (페이징 X)
//	 * @param
//	 * @return 조회 결과 없으면 list.size() == 0
//	 */
//	public List<AuthorizationDocumentVO> readAuthorizationDocumentListDepartNonPaging(AuthorizationDocumentVO avo);

	/**
	 * 본인이 기안한 문서 중, 완료된 문서 확인 (페이징 O)
	 * @param
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<AuthorizationDocumentVO> readAuthorizationDocumentListUser(AuthorizationDocumentVO avo, PaginationInfo<AuthorizationDocumentVO> paging);

	/**
	 * 본인 부서의 모든 완료된 문서 확인 (페이징 O)
	 * @param
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<AuthorizationDocumentVO> readAuthorizationDocumentListDepart(AuthorizationDocumentVO avo, PaginationInfo<AuthorizationDocumentVO> paging);


	//아래부터 로그인 사용자 전자결재 목록 조회

	/**
	 * 로그인 사용자가 기안한 문서
	 * @param userId 기안자Id
	 * @return
	 */
	public List<AuthorizationDocumentVO> readMyAllCombined(String userId, PaginationInfo<AuthorizationDocumentVO> paging);


	/** 로그인 사용자가 수신(결재승인)해야하는 문서
	 * @param userId 기안자 Id
	 * @return
	 */
	public List<AuthorizationDocumentVO> readMyInboxCombined(String userId, PaginationInfo<AuthorizationDocumentVO> paging);

	/**
	 * 로그인 유저에 해당하는 전체...
	 * @param userId
	 * @return
	 */
	public List<AuthorizationDocumentVO> readMyDraftList(String userId, PaginationInfo<AuthorizationDocumentVO> paging);

	/**
	 * 내가 결재한(처리완료) 문서 목록 조회
	 * @param userId
	 * @return
	 */
	public List<AuthorizationDocumentVO> readMyProcessedList(String userId, PaginationInfo<AuthorizationDocumentVO> paging);

	/**
	 * 로그인 유저가 전자결재 메인에서 상세 조회
	 * @param atrzDocId
	 * @return 문서 + (Impl에서 결재선까지 세팅해서)
	 */
	public AuthorizationDocumentVO readAuthDocument(String atrzDocId, String loginId);


    /**
     * 기안문 템플릿 기안문에 등록
     * @param authDoc
     */
    public AuthorizationDocumentVO insertAuthDocument(AuthorizationDocumentVO authDoc);


    /**
     * 기안서 업데이트 (가영 추가)
     * @param authorizationDocument	기안서 Id 와 수정할 정보가 들어있는 vo
     * @return 성공하면 true, 실패하면 false
     */
    public boolean modifyAuthorizationDocumentSign(AuthorizationDocumentVO authorizationDocument);

    /**
     * 문서 상태코드 업데이트
     * @param docId   문서 ID
     * @param sttsCode 상태 코드 (A201~A206)
     * @return 반영 건수
     */
    int updateDocumentStatus(String docId, String sttsCode);

    /**
     * 회수된 문서를 임시 저장함에 저장 (상태 A201, DEL_YN='Y')
     * @param docId 문서 ID
     * @return 반영 건수
     */
    int saveRetractedAsTemp(String docId, String htmlData);

    /**
     * 관리자 전용 결재문서 삭제(소프트 삭제)
     * @param docId 문서 ID
     * @return 반영 건수
     */
    int softDeleteDocumentAsAdmin(String docId);

    /** 무슨코드? */
    public String getNextDocId();


    /**
     * 관리자페이지 오늘의 결재 수
     * @return
     */
    public int getApprovalCount();

}
