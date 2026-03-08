package kr.or.ddit.comm.file.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import kr.or.ddit.mybatis.mapper.FileMasterMapper;
import kr.or.ddit.vo.FileMasterVO;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author 임가영
 * @since 2025. 10. 1.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 1.     	임가영	          최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class FileDeleteServiceImpl {
	
	private final FileMasterMapper fileMasterMapper;

	public void deleteFileDB(String fileId) {
		if (fileId != null || StringUtils.isBlank(fileId)) {
			FileMasterVO fileMaster = new FileMasterVO();
			fileMaster.setFileId(fileId);
			fileMasterMapper.updateFileMasterDelyn(fileMaster);
		}
	}
}
