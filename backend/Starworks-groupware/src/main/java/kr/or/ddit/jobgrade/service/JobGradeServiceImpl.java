package kr.or.ddit.jobgrade.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.JobGradeMapper;
import kr.or.ddit.vo.JobGradeVO;
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
public class JobGradeServiceImpl implements JobGradeService {

	private final JobGradeMapper mapper;
	
	@Override
	public List<JobGradeVO> readJobGradeList() {
		return mapper.selectJobGeadeList();
	}

	@Override
	public JobGradeVO readJobGrade(String jbgdCd) {
		JobGradeVO jobGrade = mapper.selectJobGrade(jbgdCd);
		if(jobGrade == null) {
			throw new EntityNotFoundException(jobGrade);
		}
		return jobGrade;
	}

}
