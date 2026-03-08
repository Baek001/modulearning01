package kr.or.ddit.department.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.department.service.DepartmentService;
import kr.or.ddit.messenger.community.service.CommunityService;
import kr.or.ddit.mybatis.mapper.DepartmentMapper;
import kr.or.ddit.vo.DepartmentVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 윤서현
 * @since 2025. 9. 25.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 25.     	윤서현	          최초 생성
 *
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

	private final DepartmentMapper mapper;
	private final CommunityService communityService;


	@Override
	public boolean createDepartment(DepartmentVO dept) {
		// 하위부서 생성 요청인데 상위가 이미 팀일 경우 → 막기
	    if (dept.getUpDeptId() != null && !dept.getUpDeptId().isEmpty()) {
	        DepartmentVO upperDept = mapper.selectDepartment(dept.getUpDeptId());
	        // 상위 부서의 코드가 000으로 끝나지 않으면 이미 팀
	        if (!upperDept.getDeptId().endsWith("000")) {
	            throw new IllegalArgumentException("팀 부서 아래에는 하위 부서를 추가할 수 없습니다.");
	        }
	    }

		String newDeptId;
	    if (dept.getUpDeptId() == null || dept.getUpDeptId().isEmpty()) {
	        // 본부 생성
	        newDeptId = mapper.getNextTopDeptId();
	    } else {
	        // 하위부서 생성
	        newDeptId = mapper.getNextChildDeptId(dept.getUpDeptId());
	    }
		dept.setDeptId(newDeptId);

		boolean created = mapper.insertDepartment(dept) > 0;
		if (created) {
			communityService.syncOrgCommunities("system", true);
		}
		return created;
	}

	@Override
	public List<DepartmentVO> readDepartmentList() {
		return mapper.selectDepartmentList();
	}

	@Override
	public DepartmentVO readDepartment(String deptId) {
		DepartmentVO dept = mapper.selectDepartment(deptId);
		if(dept == null) {
			throw new EntityNotFoundException(dept);
		}
		return dept;
	}

	@Override
	public boolean removeDepartment(String deptId) {
		int userCount = mapper.countUsersInDepartment(deptId);
	    if (userCount > 0) {
	        return false; // 삭제 불가
	    }
		int result = mapper.deleteDepartment(deptId);
		if (result > 0) {
			communityService.syncOrgCommunities("system", true);
		}
	    return result > 0;
		//return mapper.deleteDepartment(deptId) > 0;
	}

	@Override
	public boolean modifyDepartment(DepartmentVO dept) {
		boolean modified = mapper.updateDepartment(dept) > 0;
		if (modified) {
			communityService.syncOrgCommunities("system", true);
		}
		return modified;
	}

}
