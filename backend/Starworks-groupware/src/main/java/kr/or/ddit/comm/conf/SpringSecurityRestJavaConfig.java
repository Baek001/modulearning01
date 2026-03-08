//package kr.or.ddit.comm.conf;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.web.cors.CorsConfigurationSource;
//
///**
// *
// * @author 홍현택
// * @since 2025. 10. 20.
// * @see
// *
// * <pre>
// * << 개정이력(Modification Information) >>
// *
// *   수정일      			수정자           수정내용
// *  -----------   	-------------    ---------------------------
// *  2025. 10. 20.     	홍현택	          최초 생성
// *
// * </pre>
// */
//@Configuration
//@EnableWebSecurity
//public class SpringSecurityRestJavaConfig {
//	@Autowired
//	private CorsConfigurationSource corsConfigSource;
//
//	@Bean
//	public SecurityFilterChain restSecurityFilterChain(HttpSecurity http) throws Exception {
//		http
//			.securityMatcher("/rest/**")
//			.csrf(csrf->csrf.disable())
//			.cors(cors->cors.configurationSource(corsConfigSource))
//			.authorizeHttpRequests(authorize->
//				authorize
//					.requestMatchers("/rest/auth").permitAll()
//					.requestMatchers("/v3/**").permitAll()
//					.requestMatchers("/swagger-ui/*").permitAll()
//					.anyRequest().authenticated()
//			)
//			.sessionManagement(session->
//				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//			)
//			.oauth2ResourceServer(oauth2->
//				oauth2.jwt(Customizer.withDefaults())
//			)
//			;
//		return http.build();
//	}
//}
//
