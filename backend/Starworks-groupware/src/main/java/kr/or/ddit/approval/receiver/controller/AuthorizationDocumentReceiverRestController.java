package kr.or.ddit.approval.receiver.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.or.ddit.approval.receiver.service.AuthorizationDocumentReceiverService;
import kr.or.ddit.vo.AuthorizationDocumentReceiverVO;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rest/approval-receiver")
@RequiredArgsConstructor
public class AuthorizationDocumentReceiverRestController {

    private final AuthorizationDocumentReceiverService service;

    @GetMapping
    public List<AuthorizationDocumentReceiverVO> readAuthDocumentReceiverList() {
        return service.readAuthDocumentReceiverList();
    }

    @GetMapping("/document/{atrzDocId}")
    public List<AuthorizationDocumentReceiverVO> readAuthDocumentReceiverListByDocId(@PathVariable String atrzDocId) {
        return service.readAuthDocumentReceiverList(atrzDocId);
    }

    @GetMapping("/{atrzRcvrId}")
    public AuthorizationDocumentReceiverVO readAuthDocumentReceiver(@PathVariable String atrzRcvrId) {
        return service.readAuthDocumentReceiver(atrzRcvrId);
    }
}
