package kr.or.ddit.approval.controller.advice;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import kr.or.ddit.approval.document.service.AuthorizationDocumentService;
import kr.or.ddit.comm.paging.PaginationInfo;
import kr.or.ddit.mybatis.mapper.AuthorizationDocumentMapper;
import kr.or.ddit.mybatis.mapper.AuthorizationTempMapper;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.AuthorizationDocumentVO;
import kr.or.ddit.vo.AuthorizationTempVO;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 임가영
 * @since 2025. 10. 15.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      		수정자           수정내용
 *  -----------	   	-------------	   ---------------------------
 *  2025. 10. 15.     	임가영	          최초 생성
 *  2025. 10. 24.		홍현택      	      결재 문서 카운트 기능 추가
 *  2025. 10. 24.		홍현택      	      부서보관함 카운트 오류 수정
 *
 * </pre>
 */
@ControllerAdvice(basePackages = "kr.or.ddit.approval")
@RequiredArgsConstructor
public class ApprovalControllerAdvice {

	private final AuthorizationDocumentService docService;
	private final AuthorizationTempMapper tempMapper;
	private final AuthorizationDocumentMapper docMapper;

	// 사이드바 메뉴 고정
	@ModelAttribute("currentMenu")
	public String sidebarCurrentMenu() {
		return "approval";
	}

	@ModelAttribute
	public void addApprovalCountsToModel(Authentication authentication, Model model) {
		if (authentication == null) return;

		Object principal = authentication.getPrincipal();
		if (!(principal instanceof CustomUserDetails)) return;

		CustomUserDetails customUser = (CustomUserDetails) principal;
		UsersVO realUser = customUser.getRealUser();
		String userId = realUser.getUserId();

		// 전자결재 홈 카운트
		PaginationInfo<AuthorizationDocumentVO> draftsPaging = new PaginationInfo<>();
		docService.readMyDraftList(userId, draftsPaging);
		model.addAttribute("draftsCount", draftsPaging.getTotalRecord());

		PaginationInfo<AuthorizationDocumentVO> inboxPaging = new PaginationInfo<>();
		docService.readMyInboxCombined(userId, inboxPaging);
		model.addAttribute("inboxCount", inboxPaging.getTotalRecord());

		PaginationInfo<AuthorizationDocumentVO> processedPaging = new PaginationInfo<>();
		docService.readMyProcessedList(userId, processedPaging);
		model.addAttribute("processedCount", processedPaging.getTotalRecord());

		int allCountSum = draftsPaging.getTotalRecord() + inboxPaging.getTotalRecord() + processedPaging.getTotalRecord();
		model.addAttribute("allCount", allCountSum);

		// 결재 보관함 카운트
		List<AuthorizationTempVO> tempDocs = tempMapper.selectAuthTempList(userId);
		model.addAttribute("tempCount", tempDocs.size());

		AuthorizationDocumentVO personalAvo = new AuthorizationDocumentVO();
		personalAvo.setAtrzUserId(userId);
		List<AuthorizationDocumentVO> personalDocs = docMapper.selectAuthorizationDocumentListUserNonPaging(personalAvo);
		model.addAttribute("personalCount", personalDocs.size());

		AuthorizationDocumentVO deptAvo = new AuthorizationDocumentVO();
		deptAvo.setAtrzUserId(userId);
		List<AuthorizationDocumentVO> deptDocs = docMapper.selectAuthorizationDocumentListDepartNonPaging(deptAvo);
		model.addAttribute("deptCount", deptDocs.size());
	}
}


