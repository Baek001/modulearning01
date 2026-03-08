package kr.or.ddit.approval.document.dto;

import java.util.List;

import lombok.Data;

@Data
public class ApprovalCreateRequest {

    private String atrzTempSqn;
    private String atrzDocTmplId;
    private String atrzDocTtl;
    private String htmlData;
    private String openYn;
    private String atrzFileId;
    private List<String> receiverIds;
    private List<ApprovalLineRequest> approvalLines;
}
