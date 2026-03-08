package kr.or.ddit.comm.file.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.file.service.FileMasterService;
import kr.or.ddit.mybatis.mapper.FileMasterMapper;
import kr.or.ddit.vo.FileMasterVO;
import lombok.RequiredArgsConstructor;
/**
 * 
 * @author 장어진
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	장어진	          최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class FileMasterServiceImpl implements FileMasterService{

	private final FileMasterMapper mapper;
	
	@Override
	public List<FileMasterVO> readFileMasterList() {
		return mapper.selectFileMasterList();
	}

	@Override
	public FileMasterVO readFileMaster(String fileId) {
		FileMasterVO vo = mapper.selectFileMaster(fileId);
		if (vo == null) {
			throw new EntityNotFoundException(fileId);
		}
		return vo;
	}

	@Override
	public boolean createFileMaster(FileMasterVO dm) {
		return mapper.insertFileMaster(dm) > 0;
	}

	@Override
	public boolean modifyFileMasterDelyn(FileMasterVO dm) {
		return mapper.updateFileMasterDelyn(dm) > 0;
	}

}
