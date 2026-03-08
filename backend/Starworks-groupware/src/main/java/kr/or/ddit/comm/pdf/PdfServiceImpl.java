package kr.or.ddit.comm.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.layout.font.FontProvider;

/**
 * Pdf 서비스
 * @author 임가영
 * @since 2025. 10. 2.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 2.     		임가영           최초 생성
 *
 * </pre>
 */
@Service
public class PdfServiceImpl {

    public byte[] generatePdfFromHtml(String htmlContent) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            // PDF 변환 옵션
            ConverterProperties converterProperties = new ConverterProperties();

            // 한글 폰트 지원
            FontProvider fontProvider = new FontProvider();
            fontProvider.addStandardPdfFonts();
            fontProvider.addFont("C:/Windows/Fonts/malgun.ttf"); // 윈도우용 맑은 고딕
            converterProperties.setFontProvider(fontProvider);
            converterProperties.setCharset("UTF-8");

            // HTML → PDF 변환
            HtmlConverter.convertToPdf(htmlContent, os, converterProperties);

            return os.toByteArray();
        }
    }
}
