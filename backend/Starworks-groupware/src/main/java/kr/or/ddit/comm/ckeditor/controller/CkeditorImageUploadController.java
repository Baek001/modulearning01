package kr.or.ddit.comm.ckeditor.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.vo.EmailContentVO;
import kr.or.ddit.vo.FileDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 홍현택
 * @since 2025. 10. 13.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 13.     	홍현택	          최초 생성
 *  2025. 10. 13. 		홍현택			CKEditor를 이용한 파일 업로드 추가
 *
 * </pre>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class CkeditorImageUploadController {

    private final FileUploadServiceImpl fileUploadService;
    private final FileDetailService fileDetailService; // FileDetailService 주입

    @PostMapping("/ckeditor/imageUpload")
    @ResponseBody
    public String imageUpload(@RequestParam("upload") MultipartFile uploadFile) throws IOException {
        Map<String, Object> responseData = new HashMap<>();

        if (!uploadFile.isEmpty()) {
            try {
                EmailContentVO tempVo = new EmailContentVO();
                tempVo.setFileList(java.util.Collections.singletonList(uploadFile));

                fileUploadService.saveFileS3(tempVo, FileFolderType.ETC.toString());

                // 업로드된 파일의 실제 URL (S3 URL)을 FileDetailService를 통해 조회
                // CKEditor는 단일 파일 업로드이므로, tempVo.getMailFileId()로 조회된 첫 번째 FileDetailVO 사용
                String fileId = tempVo.getMailFileId();
                List<FileDetailVO> fileDetails = fileDetailService.readFileDetailList(fileId);
                String fileUrl = "";
                if (fileDetails != null && !fileDetails.isEmpty()) {
                    fileUrl = fileDetails.get(0).getFilePath(); // S3 URL
                }

                responseData.put("uploaded", 1);
                responseData.put("fileName", uploadFile.getOriginalFilename());
                responseData.put("url", fileUrl);

            } catch (Exception e) {
                log.error("CKEditor 이미지 업로드 실패: ", e);
                responseData.put("uploaded", 0);
                responseData.put("error", Map.of("message", "이미지 업로드 중 오류가 발생했습니다."));
            }
        } else {
            responseData.put("uploaded", 0);
            responseData.put("error", Map.of("message", "업로드할 파일이 없습니다."));
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(responseData);
    }
}
