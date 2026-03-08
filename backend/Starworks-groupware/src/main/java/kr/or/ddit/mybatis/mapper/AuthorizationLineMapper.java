package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.AuthorizationLineVO;

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
 *	2025. 10. 1.		홍현택			결재선 조회 시그니처, 주석 추가
 *	2025. 10. 2. 		홍현택			전결처리 시그니처 추가.
 *  2025. 10. 5. 		홍현택			 결재문서 열람 처리 시그니처 추가 (미열람 -> 미처리)
 *  2025. 10. 10.		홍현택			반려 기능 추가
 *  2025. 10. 10.		홍현택			문서 회수 시, 결재선 상태 Null 기능 시그니처 추가 updateAllLineStatusToNullByDocId
 * </pre>
 */
@Mapper
public interface AuthorizationLineMapper {

	/**
	 * 전자결재 결재선 추가
	 * @param authLine
	 * @return
	 */
	public int insertAuthLine(AuthorizationLineVO authLine);
	
	/**
     * 특정 문서의 결재선 전체 조회
     * @param atrzDocId
     * @return 결재선 목록
     */
    public List<AuthorizationLineVO> selectAuthorizationLineList(String atrzDocId);
    
    
 // ======== 승인 처리에 필요한 메소드========
    
 /**
  *                  
  *                  
  *                  
  *                  
  *                  전자결재 승인 시 이루어져야 하는 것들....


				1. 현재 로그인한 결재자의 미처리 '결재라인' 조회 (select)
				 ==> 자신의 승인차례인지 검증. 없으면 권한없음/이미처리로 막음
				
				2. 현재 라인(시퀀스) 중 내 차례 라인 이전 순번에서 승인/전결이 아닌 라인 수 확인. 0이면 단계확인 완료(select)
				==> 앞 선 라인이 완료되지 않았으면 승인되면 안됨.. 
				
				3. 승인 처리 : 현재 라인에 A401(승인) 처리일시/의견/서명파일ID..? 저장 (update)
				
				4. 현재 라인(시퀀스) 이후 다음 라인 중 가작 값이 작은 라인(시퀀스) 조회. (select)
				 4번과 5번으로 ==> 다음 결재 대상을 찾아서 문서 상태를 바꿔줌.
				 if 다음 대기라인이 있다 -> 문서 상태 A203 결재 중.
				 if 다음 대기라인이 없다.-> 문서상태 A206(최종승인)
				
				5. 문서의 현재 상태를 전달받은 상태로 업에이트 (update)
				승인 후에는 A203(결재중) 또는 A206(최종승인) 으로 변경.
				
				승인 시 변경되는 흐름
				
				라인(AUTHORIZATION_LINE)
				해당 결재자의 행이 ATRZ_ACT = 'A401', PRCS_DT = SYSDATE, ATRZ_OPNN 등으로 업데이트됨.
				
				문서(AUTHORIZATION_DOCUMENT)
				이후 결재자가 남아있으면 → CRNT_ATRZ_STEP_CD = 'A203' (결재 중).
				더 이상 남은 결재자가 없으면 → CRNT_ATRZ_STEP_CD = 'A206' (최종승인).               
  *                  
  *                  
  * 
  */

  
    /**
     * 현재 로그인 사용자의 미처리된 결재 라인 1건 조회
     * @param docId
     * @param userId
     * @return 결재 라인 1건
     */
    public AuthorizationLineVO selectPendingLineForUser(
            @Param("docId") String docId,
            @Param("userId") String userId
            );

    /**
     * 결재 중 승인/전결이 아닌 라인 개수
     * @param docId
     * @param lineSeq
     * @return 승인/전결이 아닌 라인이 0일시 단계확인.
     */
    public int selectPreviousUnapprovedCount(
            @Param("docId") String docId,
            @Param("lineSeq") int lineSeq
            );

   
    /**
     * 승인 처리(A401) : 행위/일시/의견/서명 업데이트
     * @param docId
     * @param lineSeq
     * @param opinion
     * @param signFileId
     * @return
     */
    public int updateApproveLine(
    	    @Param("docId") String docId,
    	    @Param("lineSqn") int lineSqn,
    	    @Param("opinion") String opinion,
    	    @Param("signFileId") String signFileId
    	);

    /**
     * 반려 처리(A402) : 행위/일시/의견 업데이트
     * @param docId
     * @param lineSqn
     * @param opinion
     * @return
     */
    public int updateRejectLine(
    	    @Param("docId") String docId,
    	    @Param("lineSqn") int lineSqn,
    	    @Param("opinion") String opinion
    	);

   
    /**
     * 현재 이후에 남은 미처리 라인 존재 여부
     * @param docId
     * @param currentSeq
     * @return
     */
    public int existsNextPending(
    	    @Param("docId") String docId,
    	    @Param("currentSeq") int currentSeq
    	);


    /**
     * 통합 시그니처 (승인 컨트롤러에서 쓰는거!)
     * @param docId
     * @param stepCode
     * @return
     */
    public int updateDocumentStatus(
            @Param("docId") String docId,
            @Param("stepCode") String stepCode
            );
    
    //================================== 전결처리 시그니처========================================
    
    /**
     * 전결 처리(A403) : 현재 결재자의 행위/일시/의견/서명 업데이트 + 이후 모든 결재선을 승인 처리
     * @param line 결재 라인 정보
     * @return 반영 건수
     */
    public int updateLineForDelegation(AuthorizationLineVO line);

    /**
     * 전결 처리 시 이후 결재선 모두 승인 완료 처리
     * @param docId 문서 ID
     * @param lineSqn 현재 라인 순번
     * @return 반영 건수
     */
    public int updateSubsequentLinesAsDelegated(
            @Param("docId") String docId,
            @Param("lineSqn") int lineSqn
    );

    /**
     * +1로 다음 순번의 결재라인 조회
     * @param docId
     * @param currentSeq
     * @return
     */
    public AuthorizationLineVO selectNextLineBySeq(
            @Param("docId") String docId,
            @Param("currentSeq") int currentSeq
    );

    /**
     * 특정 결재 라인의 상태를 '미열람(A301)'으로 변경
     * @param docId
     * @param lineSqn
     * @return
     */
    public int updateLineStatusToUnread(
            @Param("docId") String docId,
            @Param("lineSqn") int lineSqn
    );

    /**
     * 결재문서 열람 시 상태를 '미처리'로 변경 (A301 -> A302)
     * @param docId
     * @param lineSqn
     * @return
     */
    public int updateLineStatusToUnprocessed(
        @Param("docId") String docId,
        @Param("lineSqn") int lineSqn
    );

    /**
     * 문서 회수 시 모든 결재 라인의 상태를 NULL로 변경
     * @param docId 문서 ID
     * @return 반영 건수
     */
    public int updateAllLineStatusToNullByDocId(
        @Param("docId") String docId
    );
}

