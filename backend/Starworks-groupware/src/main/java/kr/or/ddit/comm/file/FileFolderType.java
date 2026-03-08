package kr.or.ddit.comm.file;

/**
 * 파일 폴더 관리를 위한 ENUM
 * @author 임가영
 * @since 2025. 9. 30.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 30.     	임가영           최초 생성
 *
 * </pre>
 */
public enum FileFolderType {
	/** 
	 * 사용자 프로필 폴더
	 */
	PROFILE("profile"), 
	/**
	 * 전자결재 관련 폴더
	 */
	APPROVAL("approval"),
	/**
	 * 전자결재 최종 승인 완료 후 pdf 파일 저장
	 */
	APPROVAL_PDF("approval/pdf"),
	/**
	 * 전자계약 관련 폴더
	 */
	CONTRACT("contract"),
	/**
	 * 전자계약 완료 pdf 파일 저장
	 */
	CONTRACT_PDF("contract/pdf"),
	/**
	 * 개인 문서함 폴더
	 */
	DOCUMENT_USER("document/user"),
	/**
	 * 부서 문서함 폴더
	 */
	DOCUMENT_DEPART("document/depart"),
	/**
	 * 전사 문서함 폴더
	 */
	DOCUMENT_COMPANY("document/company"),
	/**
	 * 프로젝트 관련 폴더
	 */
	PROJECT("project"), 	
	/**
	 * 프로젝트 업무 폴더 
	 */
	TASK("project/task"),
	/**
	 * 프로젝트 게시판 폴더
	 */
	PROJECT_BOARD("project/board"),
	/**
	 * 상품 이미지 폴더
	 */
	PRODUCT("product"),
	/**
	 * 커뮤니티 폴더
	 */
	COMMUNITY("community"),
	/**
	 * 게시판 폴더
	 */
	BOARD("board"),
	/**
	 * 전자메일 폴더
	 */
	MAIL("mail"),
	/**
	 * 실시간 채팅 폴더
	 */
	MESSAGE("message"),
	/**
	 * 기타
	 */
	ETC("etc"); 			
	
	private final String folder;
	
	FileFolderType(String folder) {
		this.folder = folder;
	}
	
    @Override
    public String toString() {
        return folder;
    }
}
