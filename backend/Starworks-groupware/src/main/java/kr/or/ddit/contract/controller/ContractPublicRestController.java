package kr.or.ddit.contract.controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.contract.service.ContractService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rest/contracts/public")
@RequiredArgsConstructor
public class ContractPublicRestController {

    private final ContractService contractService;

    @GetMapping("/{token}")
    public Map<String, Object> detail(@PathVariable String token) {
        return contractService.readPublicContract(token);
    }

    @PostMapping("/{token}/claim")
    public Map<String, Object> claim(@PathVariable String token, @RequestBody(required = false) Map<String, Object> body) {
        return contractService.claimPublicContract(token, body == null ? Map.of() : body);
    }

    @PostMapping("/{token}/submit")
    public Map<String, Object> submit(@PathVariable String token, @RequestBody(required = false) Map<String, Object> body) {
        return contractService.submitPublicContract(token, body == null ? Map.of() : body);
    }

    @GetMapping("/{token}/download")
    public ResponseEntity<byte[]> download(@PathVariable String token) {
        ContractService.FileDownloadPayload payload = contractService.readPublicDownload(token);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(payload.fileName(), StandardCharsets.UTF_8).build());
        headers.setContentType(MediaType.parseMediaType(payload.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : payload.contentType()));
        return ResponseEntity.ok().headers(headers).body(payload.bytes());
    }
}
