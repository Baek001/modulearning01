package kr.or.ddit.security.utils;

import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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
public class AuthorizeCheckUtils {
	public static boolean checkAuthorize(Authentication authentication, String role) {
		if(authentication==null) return false;
		
		Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
		return authorities.contains(new SimpleGrantedAuthority(role));		
	}
}
