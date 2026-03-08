package kr.or.ddit.vo;

import java.util.Date;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MessengerUserVO {

    @Include
    @NotBlank
    private String userId;

    @Include
    @NotBlank
    private String msgrId;

    @NotBlank
    private Date joinDt;
    private Date leftDt;
    private String roleCd;
    private String notifyYn;
}
