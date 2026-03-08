package kr.or.ddit.comm.conf;

import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;

import jakarta.servlet.DispatcherType;
import kr.or.ddit.security.CustomJwtAuthenticationConverter;
import kr.or.ddit.security.handler.CustomAuthenticationSuccessHandler;

/**
 *
 * @author ?랁쁽??
 * @since 2025. 9. 27.
 * @see
 *
 * <pre>
 * << 媛쒖젙?대젰(Modification Information) >>
 *
 *   ?섏젙??     			?섏젙??          ?섏젙?댁슜
 *  -----------	   		------------- 	   ---------------------------
 *  2025. 9. 27.     		?랁쁽??      ?몃찓紐⑤━ 濡쒓렇???ㅼ젙 -> DB ?뺣낫 濡쒓렇???ㅼ젙?쇰줈 ?섏젙, ?됰Ц ?뷀샇 ?뺥깭濡??ㅼ젙.
 *  2025.10. 20.			?랁쁽??		濡쒓렇???⑥뒪?뚮뱶 ?뷀샇???ㅼ젙.
 *  2025.10. 20.			?랁쁽??		JWT 湲곕컲 REST API 蹂댁븞 ?ㅼ젙 .
 *  2025.10. 20.			?랁쁽??		JwtCookieAuthenticationFilter 異붽?.
 *  2025.10. 21.			?랁쁽??		濡쒓렇?꾩썐 ?좏겙 留뚮즺 異붽?
 *
 * </pre>
 */
@Configuration
@EnableWebSecurity
public class SpringSecurityJavaConfig {

	@Autowired
	private CorsConfigurationSource corsConfigSource;

	private final String[] WHITELIST = {
//			"/",
			"/dist/**",
			"/css/**",
			"/js/**",
			"/html/**",
			"/error/**",
			"/images/**",
			"/swagger**",
			"/login",
			"/common/auth"
		};

		private final DispatcherType[] DISPATCHERTYPE_WHITELIST = {
			DispatcherType.FORWARD,
			DispatcherType.INCLUDE,
			DispatcherType.ERROR
		};

			@Autowired
			private CustomAuthenticationSuccessHandler successHandler;
			@Autowired
			private CustomJwtAuthenticationConverter customJwtAuthenticationConverter; // CustomJwtAuthenticationConverter 二쇱엯


			@Bean
			public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
				return authenticationConfiguration.getAuthenticationManager();
			}

			@Bean
			JwtDecoder jwtDecoder(@Value("${jwt.sign-key}") String secretKey) {
			byte[] keyBytes = secretKey.getBytes();
			SecretKeySpec key = new SecretKeySpec(keyBytes, JWSAlgorithm.HS256.getName());
			return NimbusJwtDecoder.withSecretKey(key)
											.macAlgorithm(MacAlgorithm.HS256)
											.build();
			}

//			@Bean
//			public JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter(JwtDecoder jwtDecoder) {
//				return new JwtCookieAuthenticationFilter(jwtDecoder);
//			}

			@Bean
			public BearerTokenResolver cookieBearerTokenResolver() {
				return new kr.or.ddit.security.filter.CookieBearerTokenResolver("access_token");
			}

			@Bean
			@Order(1) // REST API ?꾪꽣 泥댁씤??癒쇱? ?곸슜
			public SecurityFilterChain restSecurityFilterChain(HttpSecurity http, BearerTokenResolver cookieBearerTokenResolver) throws Exception {
				http
					.securityMatcher("/rest/**", "/chat/**", "/starworks-groupware-websocket", "/starworks-groupware-websocket/**")
					.csrf(csrf->csrf.disable())
					.cors(cors->cors.configurationSource(corsConfigSource))
					.authorizeHttpRequests(authorize->
						authorize
							.requestMatchers("/rest/auth", "/rest/contracts/public/**").permitAll()
							.requestMatchers("/admin/*").hasRole("ADMIN")

							.anyRequest().authenticated()
					)

//					.sessionManagement(session->
//						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//					)
                    .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED))
                    )
					.oauth2ResourceServer(oauth2->
						oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter)) // CustomJwtAuthenticationConverter ?곸슜, CustomUserDetails ?ъ슜?섍린 ?꾪빐..
						.bearerTokenResolver(cookieBearerTokenResolver)
					)
					;
				return http.build();
			}

	@Bean
	public SecurityContextRepository contextRepository() {
		return new DelegatingSecurityContextRepository(
				new HttpSessionSecurityContextRepository(),
				new RequestAttributeSecurityContextRepository()
			);
	}


	@Bean
	@Order(2) // ???꾪꽣 泥댁씤???섏쨷???곸슜
	public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
		http
		.securityMatchers(matcher->matcher.anyRequest())
		.cors(cors -> cors.configurationSource(corsConfigSource))
		.csrf(csrf->csrf.disable())
		//.addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // JWT 荑좏궎 ?몄쬆 ?꾪꽣 異붽?
		        .authorizeHttpRequests(authorize ->
		            authorize
		                .dispatcherTypeMatchers(DISPATCHERTYPE_WHITELIST).permitAll()
		                .requestMatchers(WHITELIST).permitAll()
		                .requestMatchers("/admin/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN") // /admin/ 寃쎈줈??ADMIN/ROLE_ADMIN 沅뚰븳留??묎렐 媛??		                .anyRequest().authenticated() // 洹???紐⑤뱺 ?몄쬆???ъ슜?먮뒗 ?묎렐 媛??
		        )
		        .securityContext(sc -> sc.securityContextRepository(contextRepository()))
		        .formLogin(login ->
		            login
		            .loginPage("/login")
		            .successHandler(successHandler)
//		            .loginProcessingUrl("/login")
//		            .failureUrl("/login?error")
		            .permitAll()
		        )
		        .exceptionHandling(exception -> exception.accessDeniedPage("/access-denied")) // 沅뚰븳 ?놁쓬 ?섏씠吏 ?ㅼ젙
		        .logout(logout ->
		            logout
		                .logoutUrl("/logout") // 濡쒓렇?꾩썐 泥섎━ URL
		                .deleteCookies("access_token") // 濡쒓렇?꾩썐 ??access_token 荑좏궎 ??젣
//		                .logoutSuccessUrl("/login") // 濡쒓렇?꾩썐 ?깃났 ??由щ떎?대젆?몃맆 URL
		                .logoutSuccessHandler((request, response, authentication) ->{
		                	String accept = request.getHeader("accept");
		                	if(accept.contains("json")) {
		                		new ObjectMapper().writeValue(response.getWriter(), Map.of("success", true));

		                	}else {
		                		response.sendRedirect("/login");		                	}
		                } )
		        		);

		        return http.build();	}

	// ?⑥뒪?뚮뱶 ?뷀샇??
	@Bean
	public PasswordEncoder passwordEncoder() {
			return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

//	// ?됰Ц ?뷀샇濡쒓렇?? 媛쒕컻??......
//	@Bean
//	public PasswordEncoder passwordEncoder() {
//		return NoOpPasswordEncoder.getInstance();
//	}

	//?몃찓紐⑤━ 濡쒓렇?몄슜 ?ㅼ젙
//	 @Bean
//	    public UserDetailsService userDetailsService() {
//	        UsersVO user1 = new UsersVO();
//	        user1.setUserId("a001");
//	        user1.setUserPswd(passwordEncoder().encode("asdf"));
//	        user1.setUserRole("ROLE_USER");
//
//	        UsersVO user2 = new UsersVO();
//	        user2.setUserId("c001");
//	        user2.setUserPswd(passwordEncoder().encode("asdf"));
//	        user2.setUserRole("ROLE_ADMIN");
//
//	        UserDetails user = new UserVOWrapper(user1);
//	        UserDetails admin = new UserVOWrapper(user2);
//
//	        return new InMemoryUserDetailsManager(user, admin);
//	    }

}
