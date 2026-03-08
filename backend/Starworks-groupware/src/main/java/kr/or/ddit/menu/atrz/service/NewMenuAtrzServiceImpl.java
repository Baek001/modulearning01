package kr.or.ddit.menu.atrz.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.mybatis.mapper.NewMenuAtrzMapper;
import kr.or.ddit.vo.NewMenuAtrzVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Service
@RequiredArgsConstructor
@Slf4j
public class NewMenuAtrzServiceImpl implements NewMenuAtrzService {

	private final NewMenuAtrzMapper mapper;
	private final FileUploadServiceImpl fileService;


	@Override
	public List<NewMenuAtrzVO> readNewMenuAtrzList(PaginationInfo<NewMenuAtrzVO> paging) {
		int totalRecord = mapper.selectTotalRecord(paging);
		paging.setTotalRecord(totalRecord);
		return mapper.selectNewMenuAtrzList(paging);
	}

	@Override
	public List<NewMenuAtrzVO> readNewMenuAtrzNonPaging() {
		return mapper.selectNewMenuAtrzListNonPaging();
	}

	@Override
	public NewMenuAtrzVO readNewMenuAtrz(int nwmnSqn) {
		NewMenuAtrzVO newMenuAtrz = mapper.selectNewMenuAtrz(nwmnSqn);
		if (newMenuAtrz == null) {
			throw new EntityNotFoundException(nwmnSqn);
		}
		return newMenuAtrz;
	}

	@Override
	public boolean createNewMenuAtrz(NewMenuAtrzVO newMenuAtrz) {
		//fileService.saveFileS3(newMenuAtrz, FileFolderType.PRODUCT.toString());
		log.info("insert 시도 VO = {}", newMenuAtrz);
		return mapper.insertNewMenuAtrz(newMenuAtrz) > 0;

    }


}
