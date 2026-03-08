package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.dto.AttendanceDepartStatusDTO;

/**
 *
 * @author 장어진
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 15.     	장어진	          최초 생성
 *
 * </pre>
 */
@Mapper
public interface AttendanceDepartStatusMapper {
	/**
	 * 부서 근태 현황 조회
	 * @param dto
	 * @return
	 */
	public List<AttendanceDepartStatusDTO> selectAttendanceDepartStatusList(AttendanceDepartStatusDTO dto);

	/**
	 * 부서 근태 현황 조회 달력용
	 * @param dto
	 * @return
	 */
	public List<AttendanceDepartStatusDTO> selectAttendanceDepartStatisticsList(AttendanceDepartStatusDTO dto);
}
