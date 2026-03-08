package kr.or.ddit.security.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import kr.or.ddit.security.CustomJwtAuthenticationConverter; // Import the new converter
import org.springframework.beans.factory.annotation.Autowired; // Import Autowired

//@Component // @Component 어노테이션 제거
/**
 *
 * @author 홍현택
 * @since 2025. 10. 21.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 21.     	홍현택	    쿠키에 저장된 JWT(access_token)를 기반으로 인증을 수행하는 필터 클래스
 *
 *
 *
 *
 *
 *
 *
 * </pre>
 */
/**
* Spring Security의 OncePerRequestFilter를 상속받아,
* 요청마다 한 번씩 실행되어 JWT 쿠키를 검증하고 인증 정보를 설정한다.
*
* 주요 역할:
* 1. 요청 쿠키에서 access_token 값을 추출
* 2. JwtDecoder를 이용해 JWT 토큰을 복호화 및 검증
* 3. 검증 성공 시 CustomJwtAuthenticationConverter를 통해 Authentication 생성
* 4. SecurityContextHolder에 Authentication 객체 저장
* 5. 유효하지 않거나 만료된 토큰은 쿠키 삭제
*/
@RequiredArgsConstructor
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {


	// JWT를 검증(복호화)하기 위한 JwtDecoder
    private final JwtDecoder jwtDecoder;

    // JWT를 CustomUserDetails 기반 Authentication으로 변환하기 위한 Converter
    @Autowired
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // /rest/** 경로는 restSecurityFilterChain에서 처리하므로 이 필터에서는 건너뜀
        if (request.getServletPath().startsWith("/rest")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 요청 쿠키에서 access_token 쿠키를 찾음.
        Optional<Cookie> jwtCookie = Optional.ofNullable(request.getCookies())
                .flatMap(cookies -> Arrays.stream(cookies)
                        .filter(cookie -> "access_token".equals(cookie.getName()))
                        .findFirst());

     // access_token 쿠키가 존재하는 경우 처리
        if (jwtCookie.isPresent()) {
            String token = jwtCookie.get().getValue();

            try {
                // 1. JWT 토큰 검증 및 디코딩
                Jwt jwt = jwtDecoder.decode(token);
                // 2. 커스텀 컨버터를 사용하여 Authentication 생성
                Authentication authentication = customJwtAuthenticationConverter.convert(jwt);
                // 3. 인증 정보를 SecurityContext에 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtValidationException e) {
                // JWT 유효성 검증 실패 (서명 불일치, 만료 등)
                logger.warn("JWT validation failed: " + e.getMessage());
                // 유효하지 않은 토큰이므로 쿠키 삭제 처리
                Cookie expiredCookie = new Cookie("access_token", null);
                expiredCookie.setHttpOnly(true);
                expiredCookie.setPath("/");
                // 쿠키 유효시간. 브라우저 종료시 사라짐
                expiredCookie.setMaxAge(0);
                response.addCookie(expiredCookie);

            } catch (Exception e) {
                // 기타 처리 중 발생한 예외
                logger.error("Error processing JWT cookie: " + e.getMessage(), e);
            }
        }

        // 다음 필터로 요청을 전달
        filterChain.doFilter(request, response);
    }
}
