package kr.or.ddit.calendar.front.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.DepartmentScheduleMapper;
import kr.or.ddit.vo.DepartmentScheduleVO;
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
public class DepartmentScheduleServiceImpl implements DepartmentScheduleService {

	private final DepartmentScheduleMapper mapper;
	
	@Override
	public List<DepartmentScheduleVO> readDepartmentScheduleList() {
		return mapper.selectDepartmentScheduleList();
	}

	@Override
	public List<DepartmentScheduleVO> readDepartmentScheduleList(String deptId) {
		return mapper.selectDepartmentScheduleListByDeptId(deptId);
	}

	@Override
	public DepartmentScheduleVO readDepartmentSchedule(String deptSchdId) {
		DepartmentScheduleVO vo = mapper.selectDepartmentSchedule(deptSchdId);
		if (vo == null) {
			throw new EntityNotFoundException(deptSchdId);
		}
		return vo;
	}

	@Override
	public boolean createDepartmentSchedule(DepartmentScheduleVO ds) {
		return mapper.insertDepartmentSchedule(ds) > 0;
	}

	@Override
	public boolean modifyDepartmentSchedule(DepartmentScheduleVO ds) {
		return mapper.updateDepartmentSchedule(ds) > 0;
	}

	@Override
	public boolean removeDepartmentSchedule(String deptSchdId) {
		return mapper.softDeleteDepartmentSchedule(deptSchdId) > 0;
	}

}
