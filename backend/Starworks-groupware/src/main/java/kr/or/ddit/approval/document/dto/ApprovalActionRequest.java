package kr.or.ddit.approval.document.dto;

import lombok.Data;

@Data
public class ApprovalActionRequest {

    private Integer lineSqn;
    private Integer currentSeq;
    private String opinion;
    private String signFileId;
    private String htmlData;
    private Integer otpCode;
}
