package kr.or.ddit.document.exception;

/**
 * 파일 삭제 권한이 없을 시 발생하는 예외
 * @author 임가영
 * @since 2025. 10. 24.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 24.     		임가영           최초 생성
 *
 * </pre>
 */
public class NotAccessFileRemoveException extends RuntimeException {

	public NotAccessFileRemoveException() {
		super();
	}

	public NotAccessFileRemoveException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NotAccessFileRemoveException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotAccessFileRemoveException(String message) {
		super(message);
	}

	public NotAccessFileRemoveException(Throwable cause) {
		super(cause);
	}

}
