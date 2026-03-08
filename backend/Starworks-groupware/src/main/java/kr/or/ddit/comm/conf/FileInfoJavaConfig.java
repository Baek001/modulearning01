package kr.or.ddit.comm.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 
 * @author 임가영
 * @since 2025. 9. 29.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *   
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 29.     	임가영           최초 생성
 *
 * </pre>
 */
@Configuration
public class FileInfoJavaConfig implements WebMvcConfigurer{

	// Tomcat이 정적 리소스로 서빙하도록 매핑을 해주어야 한다.
	// 브라우저는 URL(예: /starworks_medias/board/abc.png)로 요청을 보냄
	// 그런데 현재 파일은 서버 PC의 물리 경로(D:/starworks_medias/board/abc.png)에만 있음
	// 톰캣은 기본적으로 src/main/resources/static 같은 정적 리소스만 서빙하고, 외부 디렉토리(D:/...)는 그대로 노출하지 않음
	
	@Value("${file-info.file.url}")
	String fileUrl;
	@Value("${file-info.file.path}")
	Resource filePath;
	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler(fileUrl + "/**") // 클라이언트가 접근할 URL 패턴
				.addResourceLocations(filePath); // 실제 파일이 저장된 물리 위치
	}
}
