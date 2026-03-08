package kr.or.ddit.security.filter;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.util.StringUtils;

import java.util.Arrays;

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
 *  2025. 10. 20.     	홍현택	     최초 생성. JWT 액세스 토큰을 Header 대신 쿠키에서 추출하도록 커스터마이징
 *
 * </pre>
 */
public class CookieBearerTokenResolver implements BearerTokenResolver {

	// 액세스 토큰이 저장된 쿠키의 이름
    private final String cookieName;

    /**
     * @param cookieName 토큰이 저장된 쿠키의 이름 (예: "access_token")
     */
    public CookieBearerTokenResolver(String cookieName) {
        this.cookieName = cookieName;
    }

    /**
     * HttpServletRequest로부터 지정된 이름의 쿠키를 찾아
     * JWT 토큰 문자열을 반환한다.
     *
     * @param request 현재 요청 객체
     * @return 쿠키에서 추출한 토큰 값 (없으면 null 반환)
     */
    @Override
    public String resolve(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
     // 쿠키 배열에서 cookieName과 일치하는 쿠키를 찾아 값 반환
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)		// 쿠키 값 추출
                .filter(StringUtils::hasText)		// 공백아닌 부분만 필터링
                .findFirst()						// 첫 번째 일치 항목 선택
                .orElse(null);						// 없으면 null 반환
    }
}
