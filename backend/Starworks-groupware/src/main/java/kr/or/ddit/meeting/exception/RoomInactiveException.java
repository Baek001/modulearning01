package kr.or.ddit.meeting.exception;

/**
 * 사용 불가한 회의실을 예약했을 때 발생하는 예외
 * @author 임가영
 * @since 2025. 10. 24.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 24.     	임가영           최초 생성
 *
 * </pre>
 */
public class RoomInactiveException extends RuntimeException{

	public RoomInactiveException() {
		super();
	}

	public RoomInactiveException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public RoomInactiveException(String message, Throwable cause) {
		super(message, cause);
	}

	public RoomInactiveException(String message) {
		super(message);
	}

	public RoomInactiveException(Throwable cause) {
		super(cause);
	}


}
