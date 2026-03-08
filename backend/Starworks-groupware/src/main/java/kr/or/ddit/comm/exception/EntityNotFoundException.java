package kr.or.ddit.comm.exception;

/**
 * PK 검색조건으로 상세 조회시 발생할 예외
 * 
 * @author SEM
 * @since 2025. 9. 24.

 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 24.     	SEM	          최초 생성
 *  2025  9. 25   		홍혀택		  entity 조건으로 예외처리 안내를 위한 예외클래스 
 * </pre>
 */
public class EntityNotFoundException extends OurBusinessException{
	private final Object byPk;

	public EntityNotFoundException(Object byPk) {
		super(String.format("[%s] 조건으로 상세 조회 실패, 해당 레코드 없음.", byPk));
		this.byPk = byPk;
	}
	
}
