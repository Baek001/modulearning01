package kr.or.ddit.comm.file.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.document.service.DocumentService;
import kr.or.ddit.document.users.service.DocumentUserFileFolderService;
import kr.or.ddit.vo.FileDetailVO;
import kr.or.ddit.vo.UserFileFolderVO;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@RestController
@RequiredArgsConstructor
public class FileDownloadController {

    private final S3Client s3Client;
    private final FileDetailService fileDetailService;
    private final DocumentService docService;
    private final DocumentUserFileFolderService userFolderService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${file-info.storage-mode:s3}")
    private String storageMode;

    @GetMapping({"/folder/download/{foldersqn}", "/rest/folder/download/{foldersqn}"})
    public ResponseEntity<byte[]> zipDownload(@PathVariable(name = "foldersqn") Integer folderSqn) {
        try {
            List<Map<String, Object>> fileList = docService.readAllFilesInFolderRecursive(folderSqn);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
                for (Map<String, Object> file : fileList) {
                    String key = (String) file.get("key");
                    String orgnFileNm = (String) file.get("orgnFileNm");
                    String folderPath = file.get("folderPath") != null ? (String) file.get("folderPath") : "";
                    byte[] fileBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
                    String relativePath = folderPath.startsWith("/") ? folderPath.substring(1) : folderPath;
                    String zipEntryName = folderPath.isEmpty() ? orgnFileNm : relativePath + "/" + orgnFileNm;
                    ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    zipOut.putNextEntry(zipEntry);
                    zipOut.write(fileBytes);
                    zipOut.closeEntry();
                }
            }

            UserFileFolderVO downFolder = userFolderService.readFileFolder(folderSqn);
            String zipFileName = downFolder.getFolderNm() + ".zip";
            return ResponseEntity.ok()
                .headers(downloadHeaders(zipFileName))
                .body(baos.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping({"/file/download/{saveFileNm:.+}", "/rest/file/download/{saveFileNm:.+}"})
    public ResponseEntity<byte[]> download(@PathVariable(name = "saveFileNm") String saveFileNm) throws IOException {
        FileDetailVO fileDetail = fileDetailService.readFileDetailBySaveName(saveFileNm);
        byte[] fileBytes;

        if (isLocalStorage()) {
            Path target = Paths.get(fileDetail.getFilePath()).resolve(fileDetail.getSaveFileNm());
            fileBytes = Files.readAllBytes(target);
        } else {
            Map<String, Object> respMap = fileDetailService.readFileDetailS3(saveFileNm);
            String key = (String) respMap.get("key");
            ResponseBytes<?> s3Object = s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(key).build()
            );
            fileBytes = s3Object.asByteArray();
        }

        return ResponseEntity.ok()
            .headers(downloadHeaders(fileDetail.getOrgnFileNm()))
            .body(fileBytes);
    }

    private HttpHeaders downloadHeaders(String originalFileName) {
        ContentDisposition contentDisposition = ContentDisposition.attachment()
            .filename(originalFileName, StandardCharsets.UTF_8)
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return headers;
    }

    private boolean isLocalStorage() {
        return "local".equalsIgnoreCase(storageMode);
    }
}
