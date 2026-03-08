package kr.or.ddit.comm.file.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import kr.or.ddit.comm.file.FileAttachable;
import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.util.ObjectStorageUrlResolver;
import kr.or.ddit.comm.pdf.PdfServiceImpl;
import kr.or.ddit.mybatis.mapper.AuthorizationDocumentPdfMapper;
import kr.or.ddit.mybatis.mapper.FileDetailMapper;
import kr.or.ddit.mybatis.mapper.FileMasterMapper;
import kr.or.ddit.security.CustomUserDetails;
import kr.or.ddit.vo.AuthorizationDocumentPdfVO;
import kr.or.ddit.vo.AuthorizationDocumentVO;
import kr.or.ddit.vo.BoardVO;
import kr.or.ddit.vo.FileDetailVO;
import kr.or.ddit.vo.FileMasterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadServiceImpl {

    @Value("${file-info.file.path}")
    private Resource fileFolderRes;

    @Value("${file-info.storage-mode:s3}")
    private String storageMode;

    private final FileMasterMapper fileMasterMapper;
    private final FileDetailMapper fileDetailMapper;
    private final AuthorizationDocumentPdfMapper authorizationDocumentPdfMapper;
    private final FileServiceS3Impl fileService;
    private final ObjectStorageUrlResolver objectStorageUrlResolver;
    private final PdfServiceImpl pdfService;

    public void saveFileS3(FileAttachable vo, String folder) {
        if (isLocalStorage()) {
            saveFileLocal(vo, folder);
            return;
        }
        saveFileToS3(vo, folder);
    }

    public void savePdfS3(AuthorizationDocumentVO authorizationDocument) {
        try {
            byte[] pdfBytes = pdfService.generatePdfFromHtml(authorizationDocument.getHtmlData());
            String saveName = UUID.randomUUID() + ".pdf";
            String folder = FileFolderType.APPROVAL_PDF.toString();
            String mimeType = "application/pdf";
            long fileSize = pdfBytes.length;
            String filePath;

            if (isLocalStorage()) {
                Path targetFolder = resolveLocalFolder(folder);
                Files.createDirectories(targetFolder);
                Files.write(targetFolder.resolve(saveName), pdfBytes);
                filePath = targetFolder.toString();
            } else {
                fileService.uploadPdf(pdfBytes, saveName, folder, mimeType);
                filePath = fileService.getFileUrl(saveName, folder);
            }

            AuthorizationDocumentPdfVO pdfVO = new AuthorizationDocumentPdfVO();
            pdfVO.setAtrzDocId(authorizationDocument.getAtrzDocId());
            pdfVO.setSaveFileNm(saveName);
            pdfVO.setFilePath(filePath);
            pdfVO.setExtFile(".pdf");
            pdfVO.setFileSize(fileSize);
            pdfVO.setFileMimeType(mimeType);
            authorizationDocumentPdfMapper.insertAuthorizationDocumentPdf(pdfVO);
        } catch (IOException e) {
            log.error("PDF 저장 중 오류가 발생했습니다.", e);
        }
    }

    public String saveGeneratedFile(byte[] fileBytes, String originalName, String contentType, String folder) {
        if (fileBytes == null || fileBytes.length == 0) {
            return null;
        }

        String normalizedName = StringUtils.defaultIfBlank(originalName, "generated-file");
        String extension = "";
        int lastDot = normalizedName.lastIndexOf('.');
        if (lastDot >= 0) {
            extension = normalizedName.substring(lastDot);
        }
        if (StringUtils.isBlank(extension) && StringUtils.isNotBlank(contentType)) {
            if ("application/pdf".equalsIgnoreCase(contentType)) {
                extension = ".pdf";
            } else if (contentType.toLowerCase().contains("png")) {
                extension = ".png";
            }
        }

        FileMasterVO fileMaster = createFileMaster();
        String saveName = UUID.randomUUID() + extension;

        try {
            String filePath;
            if (isLocalStorage()) {
                Path targetFolder = resolveLocalFolder(folder);
                Files.createDirectories(targetFolder);
                Files.write(targetFolder.resolve(saveName), fileBytes);
                filePath = targetFolder.toString();
            } else {
                fileService.uploadPdf(fileBytes, saveName, folder, contentType);
                filePath = fileService.getFileUrl(saveName, folder);
            }

            FileDetailVO fileDetail = new FileDetailVO();
            fileDetail.setFileId(fileMaster.getFileId());
            fileDetail.setOrgnFileNm(normalizedName);
            fileDetail.setSaveFileNm(saveName);
            fileDetail.setFilePath(filePath);
            fileDetail.setFileSize((long) fileBytes.length);
            fileDetail.setExtFile(extension);
            fileDetail.setFileMimeType(contentType);
            fileDetailMapper.insertFileDetail(fileDetail);
            return fileMaster.getFileId();
        } catch (IOException e) {
            log.error("생성 파일 저장 중 오류가 발생했습니다. folder={}", folder, e);
            return null;
        }
    }

    public void copyFiles(String originalFileId, FileAttachable newVo, String folder) {
        if (StringUtils.isBlank(originalFileId)) {
            return;
        }

        List<FileDetailVO> originalFileDetails = fileDetailMapper.selectFileDetailList(originalFileId);
        if (originalFileDetails == null || originalFileDetails.isEmpty()) {
            return;
        }

        FileMasterVO newFileMaster = createFileMaster();
        newVo.setFileId(newFileMaster.getFileId());

        for (FileDetailVO originalDetail : originalFileDetails) {
            try {
                byte[] fileBytes = readStoredBytes(originalDetail);
                String extension = StringUtils.defaultString(originalDetail.getExtFile());
                String newSaveName = UUID.randomUUID() + extension;
                String filePath;

                if (isLocalStorage()) {
                    Path targetFolder = resolveLocalFolder(folder);
                    Files.createDirectories(targetFolder);
                    Files.write(targetFolder.resolve(newSaveName), fileBytes);
                    filePath = targetFolder.toString();
                } else {
                    fileService.uploadPdf(fileBytes, newSaveName, folder, originalDetail.getFileMimeType());
                    filePath = fileService.getFileUrl(newSaveName, folder);
                }

                FileDetailVO newFileDetail = new FileDetailVO();
                newFileDetail.setFileId(newFileMaster.getFileId());
                newFileDetail.setOrgnFileNm(originalDetail.getOrgnFileNm());
                newFileDetail.setSaveFileNm(newSaveName);
                newFileDetail.setFilePath(filePath);
                newFileDetail.setFileSize(originalDetail.getFileSize());
                newFileDetail.setExtFile(originalDetail.getExtFile());
                newFileDetail.setFileMimeType(originalDetail.getFileMimeType());
                fileDetailMapper.insertFileDetail(newFileDetail);
            } catch (IOException e) {
                log.error("첨부파일 복제 중 오류가 발생했습니다. fileId={}", originalFileId, e);
            }
        }
    }

    public void saveFileLocal(BoardVO board) {
        if (board == null || board.getFileList() == null || board.getFileList().isEmpty()) {
            return;
        }

        FileMasterVO fileMaster = createFileMaster();
        board.setPstFileId(fileMaster.getFileId());

        try {
            persistFilesToLocal(board.getFileList(), fileMaster.getFileId(), FileFolderType.BOARD.toString());
        } catch (IOException e) {
            log.error("로컬 게시판 파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    private void saveFileToS3(FileAttachable vo, String folder) {
        List<MultipartFile> fileList = vo.getFileList();
        if (fileList == null || fileList.isEmpty()) {
            return;
        }

        FileMasterVO fileMaster = null;
        boolean firstFile = true;

        for (MultipartFile file : fileList) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            if (firstFile) {
                firstFile = false;
                fileMaster = createFileMaster();
                vo.setFileId(fileMaster.getFileId());
            }

            String originName = file.getOriginalFilename();
            String fileExt = originName.substring(originName.lastIndexOf('.'));
            String saveName = UUID.randomUUID() + fileExt;
            String fileUrl = fileService.getFileUrl(saveName, folder);

            try {
                fileService.uploadFile(file, saveName, folder);

                FileDetailVO fileDetail = new FileDetailVO();
                fileDetail.setFileId(fileMaster.getFileId());
                fileDetail.setOrgnFileNm(originName);
                fileDetail.setSaveFileNm(saveName);
                fileDetail.setFilePath(fileUrl);
                fileDetail.setFileSize(file.getSize());
                fileDetail.setExtFile(fileExt);
                fileDetail.setFileMimeType(file.getContentType());
                fileDetailMapper.insertFileDetail(fileDetail);
            } catch (IOException e) {
                log.error("S3 파일 저장 중 오류가 발생했습니다. folder={}", folder, e);
            }
        }
    }

    private void saveFileLocal(FileAttachable vo, String folder) {
        List<MultipartFile> fileList = vo.getFileList();
        if (fileList == null || fileList.isEmpty()) {
            return;
        }

        FileMasterVO fileMaster = createFileMaster();
        vo.setFileId(fileMaster.getFileId());

        try {
            persistFilesToLocal(fileList, fileMaster.getFileId(), folder);
        } catch (IOException e) {
            log.error("로컬 파일 저장 중 오류가 발생했습니다. folder={}", folder, e);
        }
    }

    private void persistFilesToLocal(List<MultipartFile> fileList, String fileId, String folder) throws IOException {
        Path targetFolder = resolveLocalFolder(folder);
        Files.createDirectories(targetFolder);

        for (MultipartFile file : fileList) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String originName = file.getOriginalFilename();
            String fileExt = originName.substring(originName.lastIndexOf('.'));
            String saveName = UUID.randomUUID() + fileExt;
            Path targetFile = targetFolder.resolve(saveName);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            FileDetailVO fileDetail = new FileDetailVO();
            fileDetail.setFileId(fileId);
            fileDetail.setOrgnFileNm(originName);
            fileDetail.setSaveFileNm(saveName);
            fileDetail.setFilePath(targetFolder.toString());
            fileDetail.setFileSize(file.getSize());
            fileDetail.setExtFile(fileExt);
            fileDetail.setFileMimeType(file.getContentType());
            fileDetailMapper.insertFileDetail(fileDetail);
        }
    }

    private FileMasterVO createFileMaster() {
        FileMasterVO fileMaster = new FileMasterVO();
        fileMaster.setCrtUserId(resolveCurrentUserId());
        fileMasterMapper.insertFileMaster(fileMaster);
        return fileMaster;
    }

    private String resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUsername();
        }
        return "system";
    }

    private Path resolveLocalFolder(String folder) throws IOException {
        File rootFolder = fileFolderRes.getFile();
        Path basePath = rootFolder.toPath();
        if (StringUtils.isBlank(folder)) {
            return basePath;
        }
        String normalized = folder.replace("/", File.separator);
        return basePath.resolve(normalized);
    }

    private boolean isLocalStorage() {
        return "local".equalsIgnoreCase(storageMode);
    }

    private byte[] readStoredBytes(FileDetailVO originalDetail) throws IOException {
        String filePath = originalDetail.getFilePath();
        if (StringUtils.isBlank(filePath)) {
            throw new IOException("filePath is empty");
        }

        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            String key = objectStorageUrlResolver.extractObjectKey(filePath);
            return fileService.downloadFile(key);
        }

        return Files.readAllBytes(Path.of(filePath, originalDetail.getSaveFileNm()));
    }
}
