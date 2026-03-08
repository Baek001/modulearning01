package kr.or.ddit.certificate.service;

import java.util.Map;

/**
 *
 * @author 윤서현
 * @since 2025. 10. 24.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 24.     	윤서현	          최초 생성
 *
 * </pre>
 */
public interface CertificateService {

	/**
	 * 재직증명서 pdf로 발급
	 * @param userId
	 * @return
	 */
	Map<String, Object> getUserForCertificate(String userId);

}
