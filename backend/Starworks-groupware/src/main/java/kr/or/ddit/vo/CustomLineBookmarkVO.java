package kr.or.ddit.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CustomLineBookmarkVO {

    @Include
    @NotBlank
    private String cstmLineBmSqn;

    @NotBlank
    private String userId;
    private String cstmLineBmNm;
    private Integer atrzLineSeq;
    private String atrzApprId;
    private String apprAtrzYn;

    private String userNm;
    private String deptNm;
    private String jbgdNm;
    private String filePath;
}
