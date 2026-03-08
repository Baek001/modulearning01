package kr.or.ddit.mybatis.mapper;

import org.apache.ibatis.annotations.Mapper;
import kr.or.ddit.vo.UsersVO;


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
@Mapper
public interface OtpMapper {

    /**
     * 사용자의 OTP 비밀 키를 업데이트합니다.
     * @param user (userId, userOtpSecret 필드가 사용됩니다)
     * @return 업데이트된 행의 수
     */
    int updateUserOtpSecret(UsersVO user);

    /**
     * 사용자의 OTP 비밀 키를 조회합니다.
     * @param userId 사용자 ID
     * @return 데이터베이스에 저장된 OTP 비밀 키
     */
    String selectUserOtpSecret(String userId);

}
