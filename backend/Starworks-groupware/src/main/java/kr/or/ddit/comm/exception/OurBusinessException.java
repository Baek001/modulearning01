package kr.or.ddit.comm.exception;

/**
 * <pre>
 * 어플리케이션 내에서 비즈니스 로직 처리 중 발생할 수 있는 예외 정의
 * 커스텀 예외 정의시 본 예외를 상속받아 정의함.
 * </pre>
 * @author SEM
 * @since 2025. 9. 24.
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 24.     	SEM	          최초 생성
 *  2025. 9. 25 		홍현택		  예외처리를 위한 공통 예외 클래스
 *
 * </pre>
 */
public class OurBusinessException extends RuntimeException{

	public OurBusinessException() {
		super();
	}

	public OurBusinessException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public OurBusinessException(String message, Throwable cause) {
		super(message, cause);
	}

	public OurBusinessException(String message) {
		super(message);
	}

	public OurBusinessException(Throwable cause) {
		super(cause);
	}
	
}
