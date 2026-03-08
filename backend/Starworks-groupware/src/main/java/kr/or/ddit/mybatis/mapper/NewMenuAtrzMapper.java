package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.vo.NewMenuAtrzVO;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	임가영	       최초 생성
 *
 * </pre>
 */
@Mapper
public interface NewMenuAtrzMapper {

	/**
	 * NewMenuAtrz 테이블 전체 레코드 수 조회.
	 * @param paging
	 * @return 전체 레코드 수
	 */
	public int selectTotalRecord(PaginationInfo<NewMenuAtrzVO> paging);
	
	/**
	 * 신제품 등록 기안서 목록 조회. (페이징 O)
	 * @param paging
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<NewMenuAtrzVO> selectNewMenuAtrzList(PaginationInfo<NewMenuAtrzVO> paging);
	
	/**
	 * 신제품 등록 기안서 목록 조회. (페이징 x)
	 * @return 조회 결과 없으면 list.size() == 0
	 */
	public List<NewMenuAtrzVO> selectNewMenuAtrzListNonPaging();

	/**
	 * 신제품 등록 기안서 단건 조회.
	 * @param nwmnSqn 신제품 일련번호
	 * @return 조회 결과 없으면 null
	 */
	public NewMenuAtrzVO selectNewMenuAtrz(int nwmnSqn);
	
	/**
	 * 신제품 등록.
	 * @param newMenuAtrz 신제품 정보를 담은 vo
	 * @return 성공한 레코드 수.
	 */
	public int insertNewMenuAtrz(NewMenuAtrzVO newMenuAtrz);
}
