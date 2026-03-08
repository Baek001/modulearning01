package kr.or.ddit.comm.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.or.ddit.security.jwt.JwtProvider;
import kr.or.ddit.vo.RestLoginVO;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RestLoginController {
    private final JwtProvider provider;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;

    @Value("${app.cookie.domain:}")
    private String cookieDomain;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Strict}")
    private String cookieSameSite;

    @Value("${app.cookie.max-age:86400}")
    private long cookieMaxAge;

    @PostMapping("/common/auth")
    public ResponseEntity<?> restLogin(
        @RequestBody RestLoginVO restLoginVO,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        Authentication inputToken = UsernamePasswordAuthenticationToken
            .unauthenticated(restLoginVO.getUsername(), restLoginVO.getPassword());

        try {
            Authentication authentication = authenticationManager.authenticate(inputToken);

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
            contextRepository.saveContext(securityContext, request, response);

            String jwt = provider.generateJwt(authentication);
            ResponseCookie jwtCookie = buildAccessTokenCookie(jwt, cookieMaxAge);

            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(authentication);
        } catch (BadCredentialsException e) {
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("errorCode", "INVALID_CREDENTIALS");
            errorBody.put("message", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody);
        } catch (AuthenticationException e) {
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("errorCode", "LOGIN_FAILED");
            errorBody.put("message", "인증에 실패했습니다. 관리자에게 문의하세요.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody);
        }
    }

    @RequestMapping("/common/auth/revoke")
    public ResponseEntity<?> revoke(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) {
        SecurityContextHolder.clearContext();
        contextRepository.saveContext(SecurityContextHolder.createEmptyContext(), request, response);

        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, buildAccessTokenCookie("", 0).toString())
            .build();
    }

    private ResponseCookie buildAccessTokenCookie(String value, long maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("access_token", value)
            .httpOnly(true)
            .path("/")
            .secure(cookieSecure)
            .maxAge(maxAge)
            .sameSite(cookieSameSite);

        if (StringUtils.hasText(cookieDomain)) {
            builder.domain(cookieDomain.trim());
        }

        return builder.build();
    }
}
