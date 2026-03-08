//package kr.or.ddit.security;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.User;
//
//import kr.or.ddit.vo.UsersVO;
//import lombok.ToString;
//
///**
// * 
// * @author 홍현택
// * @since 2025. 9. 27.
// * @see
// *
// * <pre>
// * << 개정이력(Modification Information) >>
// *   
// *   수정일      			수정자           수정내용
// *  -----------   	-------------    ---------------------------
// *  2025. 9. 27.     	홍현택	          인메모리 로그인용
// *
// * </pre>
// */
//@ToString
//public class UserVOWrapper extends User implements RealUserWrapper {
//	private final UsersVO realUser;
//	
//	public UserVOWrapper(UsersVO realUser) {
//		super(realUser.getUserId(), realUser.getUserPswd(),true, true, true, true, rolesToAuthorities(realUser.getUserRole()));
//		this.realUser = realUser;
//	}
//	
//	private static Collection<GrantedAuthority> rolesToAuthorities(String...roles) {
//		List<GrantedAuthority> authorities = new ArrayList<>();
//		for(String role : roles) {
//			authorities.add(new SimpleGrantedAuthority(role));
//		}
//		return authorities;
//	}
//	
//	@Override
//	public UsersVO getRealUser() {
//		return realUser;
//	}
//
//}
