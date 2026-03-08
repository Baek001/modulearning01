package kr.or.ddit.vo;

import lombok.Data;

/**
 *
 * @author 홍현택
 * @since 2025. 10. 20.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 20.     	홍현택	          최초 생성
 *
 * </pre>
 */
@Data
public class RestLoginVO {
    private String username;
    private String password;
}
