package kr.or.ddit.approval.otp.service;

/**
 * Google OTP 관련 서비스 인터페이스
 */
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
public interface OtpService {

    /**
     * 신규 OTP 비밀 키를 생성
     * @return Base32로 인코딩된 비밀 키
     */
    String generateSecretKey();

    /**
     * 주어진 비밀 키로 QR 코드 URL 생성
     * @param secretKey 비밀 키
     * @param account 사용자 계정 (이메일 등..)
     * @param issuer 발급자 (모두의 러닝)
     * @return QR 코드 이미지 데이터 URL
     */
    String getOtpQrCodeDataUrl(String secretKey, String account, String issuer);

    /**
     * 제공된 OTP 코드의 유효성을 검증
     * @param secretKey 사용자의 비밀 키
     * @param code 사용자가 입력한 OTP 코드
     * @return 코드가 유효하면 true
     */
    boolean validateOtp(String secretKey, int code);

    /**
     * 사용자의 OTP 비밀 키를 DB에 저장/업데이트
     * @param userId 사용자 ID
     * @param secretKey 저장할 비밀 키
     */
    void saveUserOtpSecret(String userId, String secretKey);

    /**
     * 사용자의 OTP 비밀 키를 DB에서 조회
     * @param userId 사용자 ID
     * @return 저장된 비밀 키 (없으면 null)
     */
    String getUserOtpSecret(String userId);
}
