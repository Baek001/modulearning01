package kr.or.ddit.vo;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuthorizationDocumentReceiverVO {

    @Include
    @NotBlank
    private String atrzRcvrId;

    @NotBlank
    private String atrzDocId;

    private String openYn;
    private LocalDateTime openDt;

    private String userNm;
    private String deptNm;
    private String jbgdNm;
    private String filePath;
}
