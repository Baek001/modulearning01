package kr.or.ddit.contract.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.or.ddit.contract.service.ContractService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rest/contracts")
@RequiredArgsConstructor
public class ContractRestController {

    private final ContractService contractService;
    private final ObjectMapper objectMapper;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication authentication) {
        return contractService.readDashboard(authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/templates")
    public List<Map<String, Object>> templates(Authentication authentication) {
        return contractService.readTemplates(authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/templates/{templateId}")
    public Map<String, Object> template(@PathVariable Long templateId, Authentication authentication) {
        return contractService.readTemplate(templateId, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping(value = "/templates", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createTemplateJson(@RequestBody Map<String, Object> body, Authentication authentication) {
        return contractService.createTemplate(body, null, null, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping(value = "/templates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> createTemplateMultipart(
        @RequestPart("payload") String payload,
        @RequestPart(name = "sourceFile", required = false) MultipartFile sourceFile,
        @RequestPart(name = "backgroundFile", required = false) MultipartFile backgroundFile,
        Authentication authentication
    ) throws IOException {
        Map<String, Object> body = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        return contractService.createTemplate(body, sourceFile, backgroundFile, authentication.getName(), isAdmin(authentication));
    }

    @PutMapping(value = "/templates/{templateId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> updateTemplateJson(@PathVariable Long templateId, @RequestBody Map<String, Object> body, Authentication authentication) {
        return contractService.updateTemplate(templateId, body, null, null, authentication.getName(), isAdmin(authentication));
    }

    @PutMapping(value = "/templates/{templateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> updateTemplateMultipart(
        @PathVariable Long templateId,
        @RequestPart("payload") String payload,
        @RequestPart(name = "sourceFile", required = false) MultipartFile sourceFile,
        @RequestPart(name = "backgroundFile", required = false) MultipartFile backgroundFile,
        Authentication authentication
    ) throws IOException {
        Map<String, Object> body = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        return contractService.updateTemplate(templateId, body, sourceFile, backgroundFile, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/templates/{templateId}/publish")
    public Map<String, Object> publishTemplate(@PathVariable Long templateId, @RequestBody(required = false) Map<String, Object> body, Authentication authentication) {
        Long templateVersionId = body == null || body.get("templateVersionId") == null ? null : Long.valueOf(String.valueOf(body.get("templateVersionId")));
        return contractService.publishTemplate(templateId, templateVersionId, authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/template-requests")
    public List<Map<String, Object>> templateRequests(Authentication authentication) {
        return contractService.readTemplateRequests(authentication.getName(), isAdmin(authentication));
    }

    @PostMapping(value = "/template-requests", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createTemplateRequestJson(@RequestBody Map<String, Object> body, Authentication authentication) {
        return contractService.createTemplateRequest(body, null, null, null, authentication.getName());
    }

    @PostMapping(value = "/template-requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> createTemplateRequestMultipart(
        @RequestPart("payload") String payload,
        @RequestPart(name = "sourceFile", required = false) MultipartFile sourceFile,
        @RequestPart(name = "markedFile", required = false) MultipartFile markedFile,
        @RequestPart(name = "sealFile", required = false) MultipartFile sealFile,
        Authentication authentication
    ) throws IOException {
        Map<String, Object> body = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        return contractService.createTemplateRequest(body, sourceFile, markedFile, sealFile, authentication.getName());
    }

    @PostMapping("/template-requests/{requestId}/approve")
    public Map<String, Object> approveTemplateRequest(@PathVariable Long requestId, @RequestBody(required = false) Map<String, Object> body, Authentication authentication) {
        return contractService.approveTemplateRequest(requestId, body, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/template-requests/{requestId}/reject")
    public Map<String, Object> rejectTemplateRequest(@PathVariable Long requestId, @RequestBody(required = false) Map<String, Object> body, Authentication authentication) {
        return contractService.rejectTemplateRequest(requestId, body, authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/company-settings")
    public Map<String, Object> companySettings(Authentication authentication) {
        return contractService.readCompanySettings(authentication.getName(), isAdmin(authentication));
    }

    @PutMapping(value = "/company-settings", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> updateCompanySettingsJson(@RequestBody Map<String, Object> body, Authentication authentication) {
        return contractService.updateCompanySettings(body, null, authentication.getName(), isAdmin(authentication));
    }

    @PutMapping(value = "/company-settings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> updateCompanySettingsMultipart(
        @RequestPart("payload") String payload,
        @RequestPart(name = "sealFile", required = false) MultipartFile sealFile,
        Authentication authentication
    ) throws IOException {
        Map<String, Object> body = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        return contractService.updateCompanySettings(body, sealFile, authentication.getName(), isAdmin(authentication));
    }

    @GetMapping
    public Map<String, Object> contracts(@RequestParam Map<String, String> params, Authentication authentication) {
        return contractService.readContracts(authentication.getName(), isAdmin(authentication), params);
    }

    @GetMapping("/{contractId}")
    public Map<String, Object> contract(@PathVariable Long contractId, Authentication authentication) {
        return contractService.readContract(contractId, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping
    public Map<String, Object> createContract(@RequestBody Map<String, Object> body, Authentication authentication) {
        return contractService.createContract(body, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/{contractId}/send")
    public Map<String, Object> sendContract(@PathVariable Long contractId, Authentication authentication) {
        return contractService.sendContract(contractId, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/{contractId}/cancel")
    public Map<String, Object> cancelContract(@PathVariable Long contractId, @RequestBody(required = false) Map<String, Object> body, Authentication authentication) {
        return contractService.cancelContract(contractId, body, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/{contractId}/remind")
    public Map<String, Object> remindContract(@PathVariable Long contractId, Authentication authentication) {
        return contractService.remindContract(contractId, authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/{contractId}/links")
    public List<Map<String, Object>> links(@PathVariable Long contractId, Authentication authentication) {
        return contractService.readContractLinks(contractId, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/batches")
    public Map<String, Object> createBatch(@RequestBody Map<String, Object> body, Authentication authentication) {
        return contractService.createBatch(body, authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/batches/{batchId}")
    public Map<String, Object> batch(@PathVariable Long batchId, Authentication authentication) {
        return contractService.readBatch(batchId, authentication.getName(), isAdmin(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
            && authentication.getAuthorities() != null
            && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equalsIgnoreCase(authority.getAuthority()) || "ADMIN".equalsIgnoreCase(authority.getAuthority()));
    }
}
