package kr.or.ddit.certificate.controller;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.font.FontProvider;

import jakarta.servlet.http.HttpServletResponse;
import kr.or.ddit.certificate.service.CertificateService;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.UsersVO;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author 윤서현
 * @since 2025. 10. 24.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 24.     	윤서현	          최초 생성
 *
 * </pre>
 */
@RestController
@RequestMapping("/rest/certificate")
@RequiredArgsConstructor
public class CertificateRestController {

	private final CertificateService service;

	@GetMapping("/{userId}")
	public void downloadCertificate(
		@PathVariable String userId,
		@RequestParam(defaultValue = "용도 미기재") String purpose,
		HttpServletResponse response,
		Authentication authentication
	) throws Exception {

		//DB에서 사용자 정보 조회
		Map<String, Object> user = service.getUserForCertificate(userId);
		if(user == null || user.isEmpty()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "사용자 정보를 찾을수없습니다.");
			return;
		}


		//로그인한 발급자 정보 가져오기
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		UsersVO realUser  = userDetails.getRealUser();

		String issuerDept = realUser.getDeptNm();
		String issuerName = realUser.getUserNm();
	    String issuerTel  = realUser.getUserTelno();

		// 기본 정보 세팅
        String issueNo = "제 " + (int) (Math.random() * 9000 + 1000) + " 호"; //랜덤값
        String name = (String) user.get("USER_NM");
        String dept = (String) user.get("DEPT_NM");
        String position = (String) user.get("JBGD_NM");
        String tel =(String)user.get("USER_TELNO");

	     // 안전한 날짜 변환 처리
	     Object hireDateObj = user.get("HIRE_YMD");
	     String hireDate;
	     if (hireDateObj instanceof java.sql.Timestamp ts) {
	         hireDate = ts.toLocalDateTime().toLocalDate().toString();
	     } else if (hireDateObj instanceof java.sql.Date date) {
	         hireDate = date.toLocalDate().toString();
	     } else if (hireDateObj != null) {
	         hireDate = hireDateObj.toString();
	     } else {
	         hireDate = "-";
	     }

	     String today = LocalDate.now().toString();

        // HTML 작성
	     String html = """
	    		 <!DOCTYPE html>
	    		 <html lang="ko">
	    		 <head>
	    		 <meta charset="UTF-8">
	    		 <style>
	    		     body {
	    		         font-family: 'Noto Sans KR';
	    		         padding: 70px 60px;
	    		         line-height: 1.8;
	    		         color: #111;
	    		         font-size: 13px;
	    		     }

	    		     h1 {
	    		         text-align: center;
	    		         font-size: 26px;
	    		         margin-bottom: 35px;
	    		         font-weight: 800;
	    		         letter-spacing: 2px;
	    		     }

	    		     table {
	    		         width: 100%%;
	    		         border-collapse: collapse;
	    		         margin: 18px 0;
	    		     }

	    		     td {
	    		         border: 1px solid #000;
	    		         padding: 10px 14px;
	    		         font-size: 12.5px;
	    		         vertical-align: middle;
	    		     }

	    		     .section-title {
	    		         font-weight: 700;
	    		         background: #e6e6e6;
	    		         width: 110px;
	    		         text-align: center;
	    		     }

	    		     .footer {
	    		         margin-top: 30px;
	    		         text-align: center;
	    		         font-size: 16px;
	    		         font-weight: 700;
	    		     }

	    		     .issuer-table {
	    		         width: 280px;
	    		         border-collapse: collapse;
	    		         margin-top: 25px;
	    		         margin-left: auto;
	    		         text-align: center;
	    		     }

	    		     .issuer-table td {
	    		         border: 1px solid #000;
	    		         padding: 8px 10px;
	    		         font-size: 13px;
	    		         font-weight: 700;
	    		     }

	    		     .issue-date {
	    		         margin-top: 35px;
	    		         text-align: center;
	    		         font-size: 15px;
	    		         font-weight: 700;
	    		         letter-spacing: 1px;
	    		     }

	    		     .sign {
	    		         margin-top: 60px;
	    		         text-align: right;
	    		         font-size: 15px;
	    		         font-weight: 700;
	    		         line-height: 1.6;
	    		         position: relative;
	    		     }

	    		     .sign img {
	    		         width: 85px;
	    		         position: absolute;
	    		         right: -40px;
	    		         top: -5px;
	    		     }
	    		 </style>
	    		 </head>
	    		 <body>
	    		     <h1>재직증명서</h1>

	    		     <p style="font-weight:700;">발급번호: %s</p>

	    		     <table>
	    		         <tr><td class='section-title'>성명</td><td>%s</td></tr>
	    		         <tr><td class='section-title'>생년월일</td><td>1998.09.18</td></tr>
	    		         <tr><td class='section-title'>주 소</td><td>대전광역시 서구 365번지 대덕아파트</td></tr>
	    		         <tr><td class='section-title'>전화번호</td><td>%s</td></tr>
	    		     </table>

	    		     <table>
	    		         <tr><td class='section-title'>근무처</td><td>\uBAA8\uB450\uC758 \uB7EC\uB2DD</td></tr>
	    		         <tr><td class='section-title'>직 위</td><td>%s</td></tr>
	    		         <tr><td class='section-title'>입사일자</td><td>%s</td></tr>
	    		         <tr><td class='section-title'>용 도</td><td>%s</td></tr>
	    		     </table>

	    		     <div class="footer">
	    		         위와 같이 재직하고 있음을 증명합니다.
	    		     </div>

	    		     <table class="issuer-table">
	    		         <tr><td class="section-title">부서</td><td>%s</td></tr>
	    		         <tr><td class="section-title">담당자</td><td>%s</td></tr>
	    		         <tr><td class="section-title">연락처</td><td>%s</td></tr>
	    		     </table>

	    		     <div class="issue-date">%s</div>

	    		     <div class="sign">
	    		         \uC8FC\uC2DD\uD68C\uC0AC \uBAA8\uB450\uC758 \uB7EC\uB2DD<br>
	    		         대표 하재관 (인)
	    		         <img src="file:src/main/resources/static/images/stamp.png" alt="도장">
	    		     </div>
	    		 </body>
	    		 </html>
	    		 """.formatted(issueNo, name, tel, position, hireDate, purpose, issuerDept, issuerName, issuerTel, today);



        // PDF 변환
        ByteArrayOutputStream baos = new ByteArrayOutputStream(); //메모리상에 PDF 바이트를 쌓아두는 버퍼
        PdfWriter writer = new PdfWriter(baos); //iText가 PDF를 쓸 출력대상을 지정
        PdfDocument pdfDoc = new PdfDocument(writer); //실제 PDF문서를 표현하는 IText 객체(얘가 pdf 구조로 생성)

        //HTML→PDF 변환 동작을 제어하는 설정 컨테이너
        ConverterProperties props = new ConverterProperties();
        props.setCharset(StandardCharsets.UTF_8.name()); //기본 문자셋을 UTF-8로 인코딩 설정
        //FontProvider fontProvider = new DefaultFontProvider(true, true, true); //PDF 표준 폰트 등록
        FontProvider fontProvider = new DefaultFontProvider(false, false, false); //PDF 표준 폰트 등록
        fontProvider.addFont("src/main/resources/fonts/NotoSansKR-Bold.ttf");
        //Starworks-groupware/src/main/resources/fonts/NotoSansKR-Bold.ttf
        props.setFontProvider(fontProvider); //폰트 공급자를 변환 설정에 연결.

        //HTML을 PDF로 변환
        //iText가 관리하는 PDF 문서(pdfDoc)로 변환하고, 그 변환 과정의 옵션을 props로 제어
        HtmlConverter.convertToPdf(html, pdfDoc, props);


        //PDF 전송(다운로드)

        // 응답 헤더 설정
        response.setContentType("application/pdf"); //브라우저가 PDF로 인식하도록

        String fileName = name + "_재직증명서.pdf";
        String encodedFileName = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        //첨부파일로 전송
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + encodedFileName);

        //바이트 스트림을 응답
        try (OutputStream os = response.getOutputStream()) {
            baos.writeTo(os);
            os.flush();
        }

	}
}
