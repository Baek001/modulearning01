package kr.or.ddit.meeting.exception;

/**
 * 이미 예약된 시간대에 예약을 하려는 경우 발생시키는 예외
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
public class AlreadyReservationException extends RuntimeException {

	public AlreadyReservationException() {
		super();
	}

	public AlreadyReservationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public AlreadyReservationException(String message, Throwable cause) {
		super(message, cause);
	}

	public AlreadyReservationException(String message) {
		super(message);
	}

	public AlreadyReservationException(Throwable cause) {
		super(cause);
	}
}
