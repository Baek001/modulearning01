package kr.or.ddit.users.service.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.messenger.community.service.CommunityService;
import kr.or.ddit.mybatis.mapper.UserHistoryMapper;
import kr.or.ddit.mybatis.mapper.UsersMapper;
import kr.or.ddit.users.service.UsersService;
import kr.or.ddit.vo.UserHistoryVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
 *  2025.10. 15.     	장어진	          Working Status 테이블 기능을 Users 테이블로 옮김.

 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsersServiceImpl implements UsersService{

	private final UsersMapper mapper;
	private final UserHistoryMapper historyMapper;
	private final PasswordEncoder passwordEncoder;
	private final CommunityService communityService;

	@Override
	public boolean createUser(UsersVO user) {

		if(user.getUserPswd() != null && !user.getUserPswd().isBlank()) {
			String encodePw = passwordEncoder.encode(user.getUserPswd());
			user.setUserPswd(encodePw);
		}

		boolean created = mapper.insertUser(user) > 0;
		if (created) {
			communityService.syncOrgCommunities("system", true);
		}
		return created;
	}

	@Override
	public List<UsersVO> readUserList() {
		return mapper.selectUserList();
	}

	@Override
	public UsersVO readUser(String userId) {
		UsersVO user = mapper.selectUser(userId);
		if(user == null) {
			throw new EntityNotFoundException(user);
		}
		return user;
	}

	@Override
	public boolean modifyUser(UsersVO user) {
		//return mapper.updateUser(user) > 0 ;

		//기존 사용자 정보 가져오기
		UsersVO before = mapper.selectUserById(user.getUserId());
		if(before == null) {
			throw new EntityNotFoundException(user);
		}

		// 비밀번호는 반드시 입력해야 함 → 입력된 비밀번호는 무조건 암호화
	    if (user.getUserPswd() == null || user.getUserPswd().isBlank()) {
	        throw new IllegalArgumentException("비밀번호는 반드시 입력해야 합니다.");
	    }

	    // bcrypt 암호화 후 저장
	    user.setUserPswd(passwordEncoder.encode(user.getUserPswd()));

		//사용자 정보 업데이트
		boolean result = mapper.updateUser(user) > 0;

		//부서/직급 변경 감지
		boolean deptChanged = !Objects.equals(before.getDeptId(), user.getDeptId());
		boolean jbgdChanged = !Objects.equals(before.getJbgdCd(), user.getJbgdCd());

		//부서/직급 변경시 인사이력 insert
		if(deptChanged || jbgdChanged) {
			UserHistoryVO hist = new UserHistoryVO();
			hist.setUserId(user.getUserId());
			hist.setBeforeDeptId(before.getDeptId());
			hist.setAfterDeptId(user.getDeptId());
			hist.setBeforeJbgdCd(before.getJbgdCd());
			hist.setAfterJbgdCd(user.getJbgdCd());
			hist.setReason("관리자에 의한 인사정보 변경");

			//변경유형 코드
			if (deptChanged && jbgdChanged) {
			    hist.setChangeType("03"); // 부서+직급
			} else if (deptChanged) {
			    hist.setChangeType("01"); // 부서이동
			} else if (jbgdChanged) {
			    hist.setChangeType("02"); // 승진/강등
			}

			historyMapper.insertUserHistory(hist);
		}



		if (result) {
			communityService.syncOrgCommunities("system", true);
		}
		return result;
	}

	@Override
	public boolean retireUser(String userId) {
		boolean retired = mapper.retireUser(userId) > 0;
		if (retired) {
			communityService.syncOrgCommunities("system", true);
		}
		return retired;
	}

	@Override
	public List<UsersVO> searchUsers(String term) {
		return mapper.selectUsersByTerm(term);
	}

	@Override
	public List<UsersVO> readResignedUserList() {
		return mapper.selectResignedUserList();
	}

	@Override
	public UsersVO readWorkStts(String UserId) {
		return mapper.selectWorkStts(UserId);
	}

	@Override
	public boolean modifyWorkStts(String UserId, String WorkSttsCd) {
		return mapper.updateWorkStts(UserId, WorkSttsCd) > 0;
	}

	@Override
	public boolean createUserList(List<UsersVO> userList) {
		boolean success = true;
		for(UsersVO user : userList) {

			if(user.getUserPswd() != null && !user.getUserPswd().isBlank()) {
				user.setUserPswd(passwordEncoder.encode(user.getUserPswd()));
			}

			int result = mapper.insertUser(user);
			if(result <= 0) success = false;
		}
		if (success && !userList.isEmpty()) {
			communityService.syncOrgCommunities("system", true);
		}
		return success;
	}
}
