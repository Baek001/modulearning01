package kr.or.ddit.mybatis.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.AuthorizationDocumentVO;

/**
 *
 * @author 윤서현
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	윤서현	          최초 생성
 *  2025. 9. 25.		임가영			전자결재 개인보관함, 전체보관함 목록 조회 구현
 *  2025. 9. 30. 		홍현택			전자결재 로그인 사용자의 목록 조회
 *  2025. 10.02. 		홍현택			전결 후 문서상태를 최종승인상태로 바꾸기 위한 시그니처 추가
 *  2025. 10.04.		임가영			updateAuthorizationDocument(기안서 업데이트) 메소드 추가
 *  2025. 10.10.		홍현택			회수 후 임시저장 됐을때 삭제여부 Y로 변경 하는 시그니처 추가 updateDocumentStatusAndDelYn
 *  2025. 10.10.		홍현택			목록 조회 메서드에 페이징 추가
 *
 * </pre>
 */
@Mapper
public interface AuthorizationDocumentMapper {

	/**
	 * 전자결재 기안문 등록
	 * @param authDoc
	 * @return
	 */
	public int insertAuthDocument(AuthorizationDocumentVO authDoc);
	/**
	 * 전자결재 기안서 목록 조회
	 * @return
	 */
	public List<AuthorizationDocumentVO> selectAuthDocumentList();

	/**
	 * 전자결재 기안서 삭제..?
	 * @param authDoc
	 * @return DEL_YN = 'Y'로 변경되면 프론트단에선 안보임
	 */
	public int updateAuthDocument(AuthorizationDocumentVO authDoc);

	// 아래부터 보관함

	/**
	 * 본인이 기안한 문서 중, 완료된 문서 확인 (페이징 X)
	 * @param
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<AuthorizationDocumentVO> selectAuthorizationDocumentListUserNonPaging(AuthorizationDocumentVO avo);

	/**
	 * 본인 부서의 모든 완료된 문서 확인 (페이징 X)
	 * @param
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<AuthorizationDocumentVO> selectAuthorizationDocumentListDepartNonPaging(AuthorizationDocumentVO avo);

	/**
	 * 본인이 기안한 문서 중, 완료된 문서 확인 (페이징 O)
	 * @param
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<AuthorizationDocumentVO> selectAuthorizationDocumentListUser(Map<String, Object> paramMap);

	/**
	 * 본인 부서의 모든 완료된 문서 확인 (페이징 O)
	 * @param
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<AuthorizationDocumentVO> selectAuthorizationDocumentListDepart(Map<String, Object> paramMap);

	// 전자결재 기안서 [로그인한 유저]에 해당하는 문서 목록 조회


	// === 로그인 사용자 전용 목록 ===

	/**
	 * 나의 기안 문서
	 * @param paramMap
	 * @return
	 */
	public List<AuthorizationDocumentVO> selectMyDraftList(Map<String, Object> paramMap);
	/**
	 * 나의 기안 문서 개수
	 * @param paramMap
	 * @return
	 */
	int countMyDraftList(Map<String, Object> paramMap);


	/**나에게 온 결재문서
	 * @param paramMap
	 * @return
	 */
	public List<AuthorizationDocumentVO> selectMyInboxCombined(Map<String, Object> paramMap);
	/**
	 * 나에게 온 결재문서 개수
	 * @param paramMap
	 * @return
	 */
	int countMyInboxCombined(Map<String, Object> paramMap);


	/**나의 기안 문서 + 나에게 온 결재문서
	 * @param paramMap
	 * @return
	 */
	public List<AuthorizationDocumentVO> selectMyAllCombined(Map<String, Object> paramMap);
	/**
	 * 나의 기안 문서 + 나에게 온 결재문서 개수
	 * @param paramMap
	 * @return
	 */
	int countMyAllCombined(Map<String, Object> paramMap);

	/**
	 * 내가 결재한(처리완료) 문서
	 * @param paramMap
	 * @return
	 */
	public List<AuthorizationDocumentVO> selectMyProcessedList(Map<String, Object> paramMap);
	/**
	 * 내가 결재한(처리완료) 문서 개수
	 * @param paramMap
	 * @return
	 */
	int countMyProcessedList(Map<String, Object> paramMap);



	/**
	 * 전자결재 기안서 상세 조회, 인증된 사용자만 접근
	 * @param atrzDocId
	 * @return
	 */
	public AuthorizationDocumentVO selectAuthDocument(
			@Param("atrzDocId") String atrzDocId,
	        @Param("loginId")   String loginId
			);

	//================= 전결 처리 후 문서 상태를 최종승인으로 업데이트 하기 위한 시그니처===============

	/**
     * 문서 상태코드 업데이트
     * @param docId   문서 ID
     * @param sttsCode 상태 코드 (A201~A206)
     * @return 반영 건수
     */
    int updateDocumentStatus(
        @Param("docId") String docId,
        @Param("sttsCode") String sttsCode
    );

    /**
     * 기안서 업데이트 하는 로직
     * @param authorizationDocument
     * @return
     */
    int updateAuthorizationDocument(AuthorizationDocumentVO authorizationDocument);

    /**
     * 문서 상태 및 삭제 여부 업데이트
     * @param docId 문서 ID
     * @param sttsCode 상태 코드 (A201~A206)
     * @param delYn 삭제 여부 ('Y' 또는 'N')
     * @return 반영 건수
     */
    int updateDocumentStatusAndDelYn(
        @Param("docId") String docId,
        @Param("sttsCode") String sttsCode,
        @Param("delYn") String delYn
    );

    /**
	 * 다음 결재 문서번호 시퀀스 조회
	 * @return 새로 생성된 문서번호 (예: ATRZ000000012345)
	 */
	String getNextDocId();


	/**
	 * 오늘의 결재 수 카운트(관리자 대시보드)
	 * @return
	 */
	    int selectApprovalCount();
	
	    /**
	     * 월별 결재 양식 사용량 및 카테고리별 통계 조회
	     * @return 월, 카테고리, 개수를 포함하는 맵 리스트
	     */
	    List<Map<String, Object>> selectMonthlyApprovalUsageByCategory();
	}
