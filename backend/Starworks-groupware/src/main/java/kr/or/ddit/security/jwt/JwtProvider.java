package kr.or.ddit.security.jwt;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.annotation.PostConstruct;

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
 *  2025. 10. 20.     	홍현택	          JWT 토큰 발행 클래스
 *
 * </pre>
 */
@Component
public class JwtProvider {
	@Value("${jwt.sign-key}")
	private String secretKey;
	private byte[] keyByte;
	private JWSSigner signer;

	@PostConstruct
	public void init() throws KeyLengthException {
		keyByte = secretKey.getBytes(StandardCharsets.UTF_8);
		signer = new MACSigner(keyByte);
	}

	/**
	 * 토큰 생성
	 * @param authentication
	 * @return
	 */
	public String generateJwt(Authentication authentication) {
		try {

			JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.jwtID(UUID.randomUUID().toString())
			    .subject(authentication.getName())
			    .claim("scope",
			    		authentication.getAuthorities()
			    .stream()
			    .map(GrantedAuthority::getAuthority).toList())
			    .issueTime(new Date())
			    .expirationTime(new Date(new Date().getTime() + 86400000L))
			    .build();
			SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);

			// Apply the HMAC protection
			signedJWT.sign(signer);
			return signedJWT.serialize();
		} catch (Exception e) {
			throw new RuntimeException("JWT 생성 실패", e);
		}
	}

	/**
	 * 토큰 검증
	 * @param token
	 * @return
	 */
	public boolean verifyToken(String token) {
		try {
			SignedJWT jwt = SignedJWT.parse(token);
			return jwt.verify(new MACVerifier(keyByte));
		} catch (ParseException | JOSEException e) {
			return false;
		}
	}

	/**
	 * 토큰 파싱 후 Authentication 객체 생성
	 * @param token
	 * @return
	 */
	public Authentication parseJwt(String token) {
		 Jwt jwt = Jwt.withTokenValue(token)
				 	.build();
		 Collection<GrantedAuthority> authorities =
				 Optional.ofNullable(jwt.getClaimAsStringList("roles"))
				 	.map(AuthorityUtils::createAuthorityList)
				 	.orElse(Collections.emptyList());
		 return new JwtAuthenticationToken(jwt, authorities);
	}


}
