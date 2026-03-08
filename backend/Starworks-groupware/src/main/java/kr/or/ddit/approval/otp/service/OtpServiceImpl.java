package kr.or.ddit.approval.otp.service;

import org.springframework.stereotype.Service;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import kr.or.ddit.mybatis.mapper.OtpMapper;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;

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
 *  2025. 10. 21.     	홍현택	          최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final OtpMapper otpMapper;

    /**
     * Google Authenticator 라이브러리를 사용하여 새로운 비밀 키를 생성합니다.
     * @return Base32로 인코딩된 문자열 형태의 비밀 키
     */
    @Override
    public String generateSecretKey() {
    	//Google Authenticator 라이브러리의 인스턴스를 생성
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        // 새로운 OTP 자격증명(비밀키)을 생성
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        //생성된 GoogleAuthenticatorKey 객체에서 Base32 인코딩된 문자열 형태의 비밀키를 추출
        return key.getKey();
    }

    /**
     * OTP 앱(Google Authenticator 등)에서 스캔할 수 있는 QR 코드 데이터 URL을 생성합니다.
     * @param secretKey 사용자의 비밀 키
     * @param account 사용자 계정 (일반적으로 이메일 또는 아이디)
     * @param issuer 발급자 (모두의 러닝으로 할 예정)
     * @return QR 코드를 나타내는 데이터 URL 문자열
     */
    @Override
    public String getOtpQrCodeDataUrl(String secretKey, String account, String issuer) {
    	// 기존에 생성된 비밀키를 기반으로 GoogleAuthenticatorKey 객체를 다시 구성하고 Builder 패턴을 사용하여 QR 코드 생성을 위한 Key 객체를 만듦.
        GoogleAuthenticatorKey key = new GoogleAuthenticatorKey.Builder(secretKey).build();
        // OTP 앱에서 인식 가능한 otpauth URL 문자열을 생성하고 이 URL을 QR 코드로 변환하여 앱이 스캔할 수 있도록 제공
        return GoogleAuthenticatorQRGenerator.getOtpAuthURL(issuer, account, key);
    }

    /**
     * 사용자가 입력한 OTP 코드의 유효성을 검증합니다.
     * @param secretKey 사용자의 비밀 키
     * @param code 사용자가 입력한 6자리 숫자 코드
     * @return 코드가 유효하면 true, 그렇지 않으면 false
     */
    @Override
    public boolean validateOtp(String secretKey, int code) {
    	// OTP 검증을 수행하기 위한 Google Authenticator 인스턴스를 생성
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        // 사용자가 입력한 6자리 숫자(code)가 비밀키(secretKey)를 기반으로 계산된 현재 유효 OTP와 일치하는지 검증한다.
        // 내부적으로 현재 시간과 비밀키를 기반으로 TOTP를 계산하고, 일정한 시간 오차(±30초~60초)를 허용.
        return gAuth.authorize(secretKey, code);
    }

    /**
     * 사용자의 OTP 비밀 키를 데이터베이스에 저장(업데이트)합니다.
     * @param userId 사용자 ID
     * @param secretKey 저장할 비밀 키
     */
    @Override
    public void saveUserOtpSecret(String userId, String secretKey) {
    	// DB 매핑용 VO(Value Object) 객체를 생성
        UsersVO user = new UsersVO();
        // OTP 비밀키를 저장할 대상 사용자의 ID를 설정
        user.setUserId(userId);
        // 새로 생성된 OTP 비밀키를 VO에 저장
        user.setUserOtpSecret(secretKey);
        // MyBatis Mapper를 호출하여 해당 사용자의 USER_OTP_SECRET 컬럼을 업데이트
        otpMapper.updateUserOtpSecret(user);
    }

    /**
     * 데이터베이스에서 특정 사용자의 OTP 비밀 키를 조회합니다.
     * @param userId 사용자 ID
     * @return 저장된 비밀 키. 없는 경우 null을 반환합니다.
     */
    @Override
    public String getUserOtpSecret(String userId) {
    	// USERS 테이블에서 해당 사용자의 OTP 비밀키를 조회
        return otpMapper.selectUserOtpSecret(userId);
    }
}
