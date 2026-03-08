package kr.or.ddit.security;

import kr.or.ddit.users.service.UsersService;
import kr.or.ddit.vo.UsersVO;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

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
 *  2025. 10. 21.     	홍현택	          최초 생성, 기존 CustomUserDeatils 사용 하기 위해서 만듬..
 *
 * </pre>
 */
@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

	@Lazy
    @Autowired
    private UsersService usersService;

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        String userId = jwt.getSubject(); // JWT의 subject 클레임에서 사용자 ID 추출

        UsersVO realUser = usersService.readUser(userId);
        if (realUser == null) {
            throw new UsernameNotFoundException("User not found with ID: " + userId);
        }

        CustomUserDetails customUserDetails = new CustomUserDetails(realUser);

        Collection<GrantedAuthority> authorities = Optional.ofNullable(jwt.getClaimAsStringList("scope"))
                .map(AuthorityUtils::createAuthorityList)
                .orElse(Collections.emptyList());

        // CustomUserDetails를 principal로 사용하여 UsernamePasswordAuthenticationToken 반환
        return new UsernamePasswordAuthenticationToken(
                customUserDetails,
                null, // credentials는 필요 없음 (JWT는 이미 인증됨)
                authorities
        );
    }
}
