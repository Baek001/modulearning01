package kr.or.ddit.comm.exception;


/**
 *
 * @author 홍현택
 * @since 2025. 10. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 25.     	홍현택	   보안등급에 따른 열람 권한이 없을시 발생하는 예외
 *
 * </pre>
 */
public class DocumentAccessDeniedException extends RuntimeException {

	public DocumentAccessDeniedException(String message) {
		super(message);
	}

}
