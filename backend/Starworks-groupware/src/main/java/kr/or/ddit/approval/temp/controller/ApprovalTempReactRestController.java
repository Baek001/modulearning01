package kr.or.ddit.approval.temp.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.or.ddit.approval.document.dto.ApprovalCreateRequest;
import kr.or.ddit.approval.temp.service.AuthorizationTempService;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.vo.AuthorizationTempVO;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rest/approval-temp")
@RequiredArgsConstructor
public class ApprovalTempReactRestController {

    private final AuthorizationTempService tempService;
    private final FileDetailService fileDetailService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public List<AuthorizationTempVO> readTempList() {
        return tempService.readAuthTempList();
    }

    @GetMapping("/{atrzTempSqn}")
    public ResponseEntity<?> readTemp(
        @PathVariable String atrzTempSqn,
        Authentication authentication
    ) {
        AuthorizationTempVO temp = requireOwnedTemp(atrzTempSqn, authentication.getName());
        return ResponseEntity.ok(Map.of(
            "temp", temp,
            "attachments", org.springframework.util.StringUtils.hasText(temp.getAtrzFileId())
                ? fileDetailService.readFileDetailList(temp.getAtrzFileId())
                : List.of()
        ));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createTemp(
        @RequestPart("payload") String payload,
        @RequestPart(name = "files", required = false) List<MultipartFile> files,
        Authentication authentication
    ) throws IOException {
        ApprovalCreateRequest request = objectMapper.readValue(payload, ApprovalCreateRequest.class);

        AuthorizationTempVO temp = new AuthorizationTempVO();
        temp.setAtrzUserId(authentication.getName());
        temp.setAtrzDocTmplId(request.getAtrzDocTmplId());
        temp.setAtrzDocTtl(request.getAtrzDocTtl());
        temp.setHtmlData(request.getHtmlData());
        temp.setOpenYn(request.getOpenYn());
        if (files != null && !files.isEmpty()) {
            temp.setFileList(files);
        } else {
            temp.setAtrzFileId(request.getAtrzFileId());
        }

        tempService.createAuthorizationTemp(temp);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "atrzTempSqn", temp.getAtrzTempSqn()
        ));
    }

    @PutMapping(value = "/{atrzTempSqn}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateTemp(
        @PathVariable String atrzTempSqn,
        @RequestPart("payload") String payload,
        @RequestPart(name = "files", required = false) List<MultipartFile> files,
        Authentication authentication
    ) throws IOException {
        AuthorizationTempVO existing = requireOwnedTemp(atrzTempSqn, authentication.getName());
        ApprovalCreateRequest request = objectMapper.readValue(payload, ApprovalCreateRequest.class);

        AuthorizationTempVO temp = new AuthorizationTempVO();
        temp.setAtrzTempSqn(existing.getAtrzTempSqn());
        temp.setAtrzUserId(existing.getAtrzUserId());
        temp.setAtrzDocTmplId(request.getAtrzDocTmplId());
        temp.setAtrzDocTtl(request.getAtrzDocTtl());
        temp.setHtmlData(request.getHtmlData());
        temp.setOpenYn(request.getOpenYn());
        if (files != null && !files.isEmpty()) {
            temp.setFileList(files);
        } else {
            temp.setAtrzFileId(request.getAtrzFileId());
        }

        tempService.modifyAuthorizationTemp(temp);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "atrzTempSqn", temp.getAtrzTempSqn()
        ));
    }

    @DeleteMapping("/{atrzTempSqn}")
    public ResponseEntity<?> deleteTemp(
        @PathVariable String atrzTempSqn,
        Authentication authentication
    ) {
        requireOwnedTemp(atrzTempSqn, authentication.getName());
        tempService.deleteAuthTemp(atrzTempSqn);
        return ResponseEntity.noContent().build();
    }

    private AuthorizationTempVO requireOwnedTemp(String atrzTempSqn, String userId) {
        AuthorizationTempVO temp = tempService.readAuthTemp(atrzTempSqn);
        if (!Objects.equals(temp.getAtrzUserId(), userId)) {
            throw new AccessDeniedException("임시저장 문서에 접근할 수 없습니다.");
        }
        return temp;
    }
}
