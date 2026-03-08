package kr.or.ddit.users.controller;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.users.service.UsersService;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 윤서현
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	윤서현	          최초 생성
 *  2025.10. 15.     	장어진	          Working Status 테이블 기능을 Users 테이블로 옮김.
 *  2025.10. 21.		홍현택			관리자 페이지 (react) 로그인 유저 확인용 getLoginUser 추가
 *
 * </pre>
 */
@RestController
@RequestMapping("/rest/comm-user")
@RequiredArgsConstructor
public class UsersRestController {

	private final UsersService service;


	/**
	 * 조직도 구성원 리스트 조회. RestController
	 * @param userId
	 * @return
	 */
	@GetMapping
	public List<UsersVO> readUserList(){
		return service.readUserList();
	}

	/**
	 * 조직도 구성원 상세조회. RestController
	 * @param userId
	 * @return
	 */
	@GetMapping("/{userId}")
	public UsersVO readUser(@PathVariable String userId) {
		return service.readUser(userId);
	}

	/**
	 * 사용자 이름 자동 완성을 위한 검색
	 *
	 * @param term 검색어 (부분 이름)
	 * @return 검색된 사용자 목록
	 */
	@GetMapping("/autocomplete")
	public List<UsersVO> autocompleteUsers(@RequestParam String term) {
		return service.searchUsers(term);
	}

	/**
	 * 조직도 구성원 추가. RestController
	 * @param newUser
	 * @return
	 */
	@PostMapping
	public Map<String, Object> createUser(@RequestBody UsersVO newUser){
		boolean success = service.createUser(newUser);
		Map<String, Object> result = new HashMap<>();
		result.put("success", success);
		return result;
	}

	/**
	 * 조직도 구성원 수정
	 * @param userId
	 * @param vo
	 * @return
	 */
	@PutMapping("/{userId}")
	public boolean modifyUser(@PathVariable String userId, @RequestBody UsersVO vo) {
		vo.setUserId(userId);

		return service.modifyUser(vo);
	}

	/**
	 * 조직도 관리 구성원 퇴사처리
	 * @param userId
	 * @return
	 */
	@PatchMapping("/{userId}/retire")
	public boolean retireUser(@PathVariable String userId) {
		return service.retireUser(userId);
	}

	/**
	 * 퇴사자 목록 조회
	 * @return
	 */
	@GetMapping("/resigned")
	public List<UsersVO> getMethodName() {
		return service.readResignedUserList();
	}


	/**
	 * 조직도 이름, 부서 검색
	 * @param term
	 * @return
	 */
	@GetMapping("/search")
	public List<UsersVO> searchUsers(@RequestParam String term){
		return service.searchUsers(term);
	}

	/**
	 *
	 */
	@GetMapping("/work-status/{userId}")
	public Map<String, Object> getWorkStts(@PathVariable("userId") String userId){
		UsersVO workStts = service.readWorkStts(userId);
		Map<String, Object> result = new HashMap<>();
		result.put("workStts", workStts);
		return result;
	}

	@PutMapping("/work-status")
	public Map<String, Object> modifyWorkStts(
		@RequestBody UsersVO vo
		, Authentication authentication
	){
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser = userDetails.getRealUser();

		boolean success = service.modifyWorkStts(realUser.getUserId(), vo.getWorkSttsCd());
		Map<String, Object> result = new HashMap<>();
		result.put("success", success);
		return result;
	}


    /**
     * 관리자 페이지에서 로그인 유저 확인용
     * @param authentication
     * @return
     */
    @GetMapping("/me")
    public Authentication getLoginUser(Authentication authentication) {
        return authentication;
    }


    /**
     * 관리자페이지 엑셀을 통해 일괄 등록
     * @param file
     * @return
     */
    @PostMapping("/uploadExcel")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file){
    	try (
			InputStream inputStream = file.getInputStream()){ //form-data의 'file' 필드에 담긴 파일을 받음
				Workbook workbook = WorkbookFactory.create(inputStream); // Apache POI가 xls/xlsx를 자동 판별하여 Workbook 생성
				Sheet sheet = workbook.getSheetAt(0); //첫번째 시트를 사용
				List<UsersVO> userList = new ArrayList<>(); //엑셀의 각 행을 usersVO로 변환해서 담을 컬렉션

				for(int i = 1; i <=sheet.getLastRowNum(); i++) {
					Row row = sheet.getRow(i); // i번째 행을 가져옴
					if(row == null) continue; //빈행은 스킵
					if(row.getCell(0)==null || getCellValue(row.getCell(0)).trim().isEmpty()) continue;



					UsersVO user = new UsersVO(); //한행 = 한명의 사용자
					// 셀 인덱스와 컬럼 매핑: (0) userId, (1) userPswd, (2) userNm, ...
					user.setUserId(getCellValue(row.getCell(0)));
					user.setUserPswd(getCellValue(row.getCell(1)));
					user.setUserNm(getCellValue(row.getCell(2)));
					user.setUserEmail(getCellValue(row.getCell(3)));
					user.setUserTelno(getCellValue(row.getCell(4)));
					user.setExtTel(getCellValue(row.getCell(5)));
					user.setDeptId(getCellValue(row.getCell(6)));
					user.setJbgdCd(getCellValue(row.getCell(7)));
					user.setHireYmd(getLocalDateValue(row.getCell(8)));
					userList.add(user); //변환 완료된 사용자 1명 목록추가
				}

				workbook.close();
				service.createUserList(userList); //서비스로 일괄 등록

				return ResponseEntity.ok(userList.size() + "명의 사용자가 등록되었습니다.");
			}catch (Exception e) {
				e.printStackTrace();
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
									 .body("엑셀 업로드 실패" + e.getMessage());
			}
    }



	/**
	 * 문자열 셀값 추출 유틸
	 * @param cell
	 * @return
	 */
	private String getCellValue(Cell cell) {
		if(cell == null) return "";
//		cell.setCellType(CellType.STRING); // 셀 타입을 문자열로 강제 변환
//		return cell.getStringCellValue().trim(); //문자열 값 앞뒤 공백 제거
		DataFormatter formatter = new DataFormatter(); //문자열로 변환(숫자, 텍스트 구분 없이)
		String value = formatter.formatCellValue(cell);
		return value.trim();
	}

	/**
	 * 날짜셀 변환 유틸
	 * @param cell
	 * @return
	 */
	private LocalDate getLocalDateValue(Cell cell) {

		if(cell == null) return null;

		//날짜형 셀인 경우
		if(cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
			return cell.getLocalDateTimeCellValue().toLocalDate(); // POI가 제공하는 LocalDateTime → LocalDate
		}

		//문자열형 셀인 경우(예: "2025-10-21" 또는 "2025/10/21")
		if(cell.getCellType() == CellType.STRING) {
			String value = cell.getStringCellValue().trim(); // 값 추출
			if(value.isEmpty()) return null; //공백이면 null
			try {
				return LocalDate.parse(value); // ISO-8601 형식일 경우 ("2025-10-21")
			}catch(Exception e) {
				try {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/hh");
					return LocalDate.parse(value, formatter); // 대안 포맷으로 재시도

				} catch (Exception ex) {
					return null;
				}
			}
		}
		return null;
	}

}
