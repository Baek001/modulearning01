package kr.or.ddit.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import kr.or.ddit.mybatis.mapper.UsersMapper;
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
@Service
public class CustomUserDetailsService implements UserDetailsService {

	@Autowired
	private UsersMapper mapper;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UsersVO realUser = mapper.selectUser(username);
		
		if(realUser==null) {
			throw new UsernameNotFoundException(String.format("%s 아이디의 회원 없음.", username));
		}
		
		return new CustomUserDetails(realUser);
	}

}
