package kr.or.ddit.security.handler;

import java.io.IOException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import kr.or.ddit.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	private final JwtProvider jwtProvider;

    @Value("${app.frontend.base-url:}")
    private String frontendBaseUrl;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws ServletException, IOException {

        String jwt = jwtProvider.generateJwt(authentication);

        // JWT를 HttpOnly 쿠키에 담아 전송
        Cookie jwtCookie = new Cookie("access_token", jwt);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/"); // 모든 경로에서 접근 가능하도록 설정
        jwtCookie.setMaxAge(60 * 30); // 30분 (JWT 만료 시간과 동일하게 설정)
        jwtCookie.setSecure(false); // HTTP 환경에서 쿠키 전송을 위해 false로 설정 (개발용)
        jwtCookie.setAttribute("SameSite", "Lax"); // 개발 환경 호환성을 위해 Lax로 설정

        response.addCookie(jwtCookie);

        String authoritiesString = authentication.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .collect(Collectors.joining(","));
        log.info("사용자 권한: [{}]", authoritiesString); // 로그는 유지

        boolean hasUserRole = authentication.getAuthorities().stream()
            .anyMatch(grantedAuthority -> "ROLE_USER".equalsIgnoreCase(grantedAuthority.getAuthority().trim()));
        boolean hasAdminRole = authentication.getAuthorities().stream()
            .anyMatch(grantedAuthority -> "ROLE_ADMIN".equalsIgnoreCase(grantedAuthority.getAuthority().trim()));

        if (hasAdminRole) { // 관리자 역할이 있다면
            getRedirectStrategy().sendRedirect(request, response, resolveRedirectUrl()); // React 앱으로 리디렉션
        } else if (hasUserRole) { // 일반 사용자 역할이 있다면
            getRedirectStrategy().sendRedirect(request, response, resolveRedirectUrl()); // 일반 사용자 페이지로 리디렉션
        } else {
            // 어떤 역할도 없는 경우 (이 경우는 발생하지 않아야 함, 디버깅용...)
            log.warn("인증된 사용자에게 일치하는 역할(ROLE_USER, ROLE_ADMIN)이 없습니다. 기본 URL로 리디렉션합니다.");
            getRedirectStrategy().sendRedirect(request, response, resolveRedirectUrl());
        }
	}

    private String resolveRedirectUrl() {
        if (!StringUtils.hasText(frontendBaseUrl)) {
            return "/";
        }

        return frontendBaseUrl.endsWith("/") ? frontendBaseUrl : frontendBaseUrl + "/";
    }
}
