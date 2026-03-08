package kr.or.ddit.security;

import kr.or.ddit.vo.UsersVO;

/**
 * 
 * @author 홍현택
 * @since 2025. 9. 27.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 2025. 9. 27.     	홍현택	          최초 생성
 *
 * </pre>
 */
public interface RealUserWrapper {
	UsersVO getRealUser();
}
