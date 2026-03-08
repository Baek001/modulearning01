package kr.or.ddit.approval.line.service;

import java.util.List;

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
 *	2025. 10. 1.		홍현택			결재선 조회 메서드 추가
 *  2025. 10. 2. 		홍현택			결재 승인 필요한 메서드 추가
 *  2025. 10. 2. 		홍현택			전결에 필요한 메서드
 *  2025. 10. 5. 		홍현택			 결재문서 열람 처리 (미열람 -> 미처리)
 * </pre>
 */
public interface AuthorizationLineService {

	/**
	 * 전자결재 결재선 추가
	 * @param authLine
	 * @return
	 */
	public boolean createAuthorizationLine(AuthorizationLineVO authLine);
	
	
	
	/**
     * 문서ID로 결재선 전체 조회
     * @param atrzDocId 문서ID
     * @return 결재선 목록
     */
	public List<AuthorizationLineVO> readAuthorizationLineList(String atrzDocId);
	
// ======================= 승인 서비스 ============================================
	/**
     * 현재 로그인 사용자의 미처리 결재 라인 1건 조회
     * @param docId 문서 ID
     * @param userId 사용자 ID
     * @return AuthorizationLineVO (없으면 null)
     */
	public AuthorizationLineVO readPendingLineForUser(String docId, String userId);

    /**
     * 선행 단계 중 승인/전결이 아닌 라인(미열람/미처리 포함) 개수
     * @param docId 문서 ID
     * @param lineSeq 현재 단계 순번
     * @return 개수(0이면 선행 모두 처리됨)
     */
	public int readPreviousUnapprovedCount(String docId, int lineSeq);

    /**
     * 승인 처리(A401) : 행위/일시/의견/서명 업데이트
     * @param docId 문서 ID
     * @param lineSqn 라인 식별 SQN
     * @param opinion 결재 의견
     * @param signFileId 서명 파일 ID
     * @return 처리성공 여부
     */
	public boolean modifyApproveLine(String docId, int lineSqn, String opinion, String signFileId);

    /**
     * 현재 이후에 남은 미처리 라인 존재 여부
     * @param docId 문서 ID
     * @param currentSeq 현재 단계 순번
     * @return 존재하면 true
     */
	public boolean readHasNextPending(String docId, int currentSeq);

    /**
     * 문서 상태 코드 갱신 (A201~A206)
     * @param docId 문서 ID
     * @param stepCode 상태 코드
     */
	public void modifyDocumentStatus(String docId, String stepCode);

    /**
     * 승인 처리 + 문서 상태 자동 갱신 트랜잭션
     * - 선행 단계 확인(선택) → 승인 처리 → 다음 미처리 라인 유무로 A203/A206 결정
     * @param docId 문서 ID
     * @param lineSqn 라인 SQN
     * @param currentSeq 현재 단계 순번
     * @param opinion 의견
     * @param signFileId 서명파일
     * @return 최종 문서 상태 코드(A203 또는 A206)
     */
	public String modifyApproveAndUpdateStatus(String docId, int lineSqn, int currentSeq, String opinion, String signFileId, String htmlData);
	
	// ===================================== 전결 서비스 ============================================
	
	/** 전결처리 
	 * 본인 라인   : 전결(A403) + 처리완료(A303)
     * - 이후 라인 : 승인(A401) + 처리완료(A303)
     * - 문서 상태 : 최종승인(A206)
	 * @param line(atrzDocId, atrzUserId, atrzLineSqn)
	 */
	public void delegateApproval(AuthorizationLineVO line, String htmlData);

	/**
	 * 반려 처리 트랜잭션
	 * - 결재선 상태 '반려'로 변경
	 * - 문서 상태 '반려'로 변경
	 * @param docId
	 * @param lineSqn
	 * @param opinion
	 */
	public void processRejection(String docId, int lineSqn, String opinion);
	/**
	 * 결재문서 열람 처리 (미열람 -> 미처리)
	 * @param docId
	 * @param lineSqn
	 * @return 성공 여부
	 */
	boolean markAsRead(String docId, int lineSqn);
}
