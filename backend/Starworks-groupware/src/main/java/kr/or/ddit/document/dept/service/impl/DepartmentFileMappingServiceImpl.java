package kr.or.ddit.document.dept.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.document.dept.service.DepartmentFileMappingService;
import kr.or.ddit.mybatis.mapper.DepartmentFileMappingMapper;
import kr.or.ddit.vo.DepartmentFileMappingVO;
import kr.or.ddit.websocket.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 임가영
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *    수정일      		수정자              수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	임가영	          최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class DepartmentFileMappingServiceImpl implements DepartmentFileMappingService {

	private final DepartmentFileMappingMapper mapper;
	private final FileUploadServiceImpl fUpService;

	/**
	 * 부서 문서 전체 목록 조회. (페이징 O)
	 */
	@Override
	public List<DepartmentFileMappingVO> readDepartmentFileMappingList(PaginationInfo<DepartmentFileMappingVO> paging, String deptId) {
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("paging", paging);
		paramMap.put("deptId", deptId);

		int totalRecord = mapper.selectDepartmentFileMappingTotalRecoard(paramMap);
		if (totalRecord == 0) totalRecord = 1;
		paging.setTotalRecord(totalRecord);

		paramMap.put("paging", paging);

		if(deptId.equals("DP000000")) {
			return mapper.selectCompanyFileMappingList(paramMap);
		} else {
			return mapper.selectDepartmentFileMappingList(paramMap);
		}
	}

	/**
	 * 부서 문서 전체 목록 조회. (페이징 X)
	 */
	@Override
	public List<DepartmentFileMappingVO> readDepartmentFileMappingListNonPaging(String deptId) {
		return mapper.selectDepartmentFileMappingListNonPaging(deptId);
	}

	/**
	 * 부서 문서 등록
	 */
	@Override
	public boolean createDepartmentFileMapping(DepartmentFileMappingVO departmentFileMapping) {
		if (departmentFileMapping.getDeptId().equals("DP000000")) {
			fUpService.saveFileS3(departmentFileMapping, FileFolderType.DOCUMENT_COMPANY.toString());
		} else {
			fUpService.saveFileS3(departmentFileMapping, FileFolderType.DOCUMENT_DEPART.toString() + "/" + departmentFileMapping.getDeptId());
		}
		return mapper.insertDepartmentFileMapping(departmentFileMapping) > 0;
	}

	/**
	 * 삭제된 부서&전사 문서 조회 (페이징 X)
	 */
	@Override
	public List<DepartmentFileMappingVO> readDepartmentFileMappingVONonPagingByDelY() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();

		return mapper.selectDepartmentFileMappingVONonPagingByDelY(username);
	}
}
