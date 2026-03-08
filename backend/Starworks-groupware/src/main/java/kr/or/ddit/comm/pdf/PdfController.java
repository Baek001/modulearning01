package kr.or.ddit.comm.pdf;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.approval.document.service.AuthorizationDocumentService;
import kr.or.ddit.vo.AuthorizationDocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PDF 다운로드용 컨트롤러
 * @author 임가영
 * @since 2025. 10. 2.
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           		  수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 2.     	임가영          		  최초 생성
 *
 *      </pre>
 */
@RequiredArgsConstructor
@RestController
@Slf4j
public class PdfController {

	private final PdfServiceImpl pdfService;
	private final AuthorizationDocumentService authorizationDocumentSerivce;

	@GetMapping("/approval/downloadPdf/{atrzDocId}")
	public ResponseEntity<byte[]> downloadPdf(
		@PathVariable(required = true) String atrzDocId
		, Authentication authentication

	) {
		String username = authentication.getName();
		AuthorizationDocumentVO avo = authorizationDocumentSerivce.readAuthDocument(atrzDocId, username);

        try {
            byte[] pdfBytes = pdfService.generatePdfFromHtml(avo.getHtmlData());

            ContentDisposition contentDisposition = ContentDisposition
                    .attachment()
                    .filename(avo.getAtrzDocTtl() + ".pdf", StandardCharsets.UTF_8) // UTF-8 안전 처리
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString()) // 다운로드
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
