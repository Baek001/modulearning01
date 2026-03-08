package kr.or.ddit.certificate.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import kr.or.ddit.mybatis.mapper.AuthorizationDocumentReceiverMapper;
import kr.or.ddit.mybatis.mapper.UsersMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateServiceImpl implements CertificateService{

	private final UsersMapper mapper;

	@Override
	public Map<String, Object> getUserForCertificate(String userId) {
		Map<String, Object> user = mapper.selectUserForCertificate(userId);
        if (user == null) {
            log.warn("재직증명서 발급 실패 - 해당 사번 없음: {}", userId);
        } else {
            log.info("재직증명서 사용자 조회 완료: {}", userId);
        }
        return user;
	}

}
