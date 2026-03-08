package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.EmailContentVO;

/**
 *
 * @author 홍현택
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	홍현택	          최초 생성
 *  2025.10. 10. 		홍현택			임시저장한 메일 수정 시그니처
 *
 * </pre>
 */
@Mapper
public interface EmailContentMapper {


	/** EmailContentMapper 전체 레코드 수 조회.
	 * @param paging
	 * @return
	 */
	public int selectTotalRecord(PaginationInfo<EmailContentVO> paging);

	/**메일 전문 목록 조회
	 * @param paging
	 * @return
	 */
	public List<EmailContentVO> selectEmailContentList(PaginationInfo<EmailContentVO> paging);

	/**
	 * 메일전문 목록 조회. (페이징 x)
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<EmailContentVO> selectEmailContentListNonPaging();

	/**
	 * 메일 전문 등록 단건 조회.
	 * @param userId
	 * @return 조회 결과 없으면 null
	 */
	public EmailContentVO selectEmailContent(String userId);

	/**
	 * 메일 전문 등록.
	 * @param EmailContent vo
	 * @return 성공한 레코드 수.
	 */
	public int insertEmailContent(EmailContentVO emailCont);
    /**
     * 임시저장 수정용
     * @param emailContent
     * @return
     */
    boolean registerEmailContent(EmailContentVO emailContent);

    /**
     * 메일 전문 수정
     * @param emailContent VO
     * @return 성공한 레코드 수
     */
    public int updateEmailContent(EmailContentVO emailContent);

	/**
	 * 특정 이메일 콘텐츠의 DEL_YN 상태를 'Y'로 업데이트합니다.
	 * @param emailContId 이메일 콘텐츠 ID
	 * @return 업데이트된 행 수
	 */
	public int updateEmailContentDelYn(String emailContId);

	/**
	 * 관리자 대시보드 이메일 건수
	 * @return
	 */
	public Integer selectEmailCount();

}
