package kr.or.ddit.menu.atrz.service;

import java.util.List;

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
public interface NewMenuAtrzService {
	
	/**
	 * 신메뉴 기안 목록 전체 조회. (페이징 O)
	 * @param paging
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	public List<NewMenuAtrzVO> readNewMenuAtrzList(PaginationInfo<NewMenuAtrzVO> paging);
	
	/**
	 * 신메뉴 기안 목록 전체 조회. (페이징 X)
	 * @return 조회 결과 없으면 list.size() == 0.
	 */
	public List<NewMenuAtrzVO> readNewMenuAtrzNonPaging();
	
	/**
	 * 신메뉴 기안 목록 단건 조회.
	 * @param nwmnSqn 신제품 일련번호
	 * @return 조회 결과 없으면 EntityNotFoundException
	 */
	public NewMenuAtrzVO readNewMenuAtrz(int nwmnSqn);
	
	/**
	 * 신메뉴 기안 목록 추가.
	 * @param newMenuAtrz 신메뉴 기안 정보가 들어있는 vo
	 * @return 성공하면 true, 실패하면 false
	 */
	public boolean createNewMenuAtrz(NewMenuAtrzVO newMenuAtrz);
	
}
