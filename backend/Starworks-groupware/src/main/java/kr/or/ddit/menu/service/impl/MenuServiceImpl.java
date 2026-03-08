package kr.or.ddit.menu.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.menu.service.MenuService;
import kr.or.ddit.mybatis.mapper.MenuMapper;
import kr.or.ddit.vo.FileDetailVO;
import kr.or.ddit.vo.MenuVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 김주민
 * @since 2025. 9. 25.
 * @see MenuServiceImpl
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	김주민	          최초 생성
 *  2025. 9. 25.        김주민 			readMenuList 추가
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

	@Autowired
	private MenuMapper mapper;

	@Autowired
	private FileDetailService fileDetailService;

	/**
	 * 신제품 메뉴 추가
	 */
	@Override
	public boolean createMenu(MenuVO newMenu) {
		int rowcnt = mapper.insertMenu(newMenu);
		if(rowcnt == 0) {
			return false;
		}
		return true;
	}

	/**
	 * 신제품 메뉴 목록 전체 조회
	 */
	@Override
	public List<MenuVO> readMenuListNonPaging() {
		return mapper.selectMenuListNonPaging();
	}

	/**
	 * 신제품 메뉴 단건 조회.
	 */
	@Override
	public MenuVO readMenu(String menuId) {
		MenuVO menu = mapper.selectMenu(menuId);

	    if(menu == null) {
	        throw new EntityNotFoundException(menu);
	    }

	    // menuFileId를 임시 변수에 저장(덮어쓰지 않기 위함)
	    String originalFileId = menu.getMenuFileId();
	    if (originalFileId != null && !originalFileId.isEmpty()) {
	        try {
	            // FileDetailService를 사용하여 파일 상세 정보 목록 조회
	            List<FileDetailVO> fileDetails = fileDetailService.readFileDetailList(originalFileId);

	            if (fileDetails != null && !fileDetails.isEmpty()) {
	                // FileDetailVO에서 S3 URL (filePath)을 추출
	                String s3FullUrl = fileDetails.get(0).getFilePath();

	                // S3 Full URL을 mageUrl 필드에 담아 JavaScript로 전송
	                menu.setImageUrl(s3FullUrl);
	            }
	        } catch (Exception e) {
	            System.err.println("Error loading file detail for menuId " + menuId + ": " + e.getMessage());
	        }
	    }
	    return menu;
	}

	/**
	 * 신제품 메뉴 수정
	 */
	@Override
	public boolean modifyMenu(MenuVO menu) {
		int rowcnt = mapper.updateMenu(menu);
		if(rowcnt == 0) {
			return false;
		}
		return true;
	}

	/**
	 * 신제품 메뉴 삭제
	 */
	@Override
	public boolean removeMenu(String menuId) {
		int rowcnt = mapper.deleteMenu(menuId);
		if(rowcnt == 0) {
			return false;
		}
		return true;
	}

	/**
	 * 신제품 메뉴 목록 조회(페이징 O)
	 */
	@Override
	public List<MenuVO> readMenuList(PaginationInfo<MenuVO> paging) {
		int totalRecord = mapper.selectTotalRecord(paging);
		paging.setTotalRecord(totalRecord);

		return mapper.selectMenuList(paging);
	}


}
