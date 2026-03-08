package kr.or.ddit.vo;

import lombok.Data;

@Data
public class BoardPollOptionVO {
    private Long optionId;
    private String pstId;
    private String optionText;
    private Integer sortOrdr;
    private Integer voteCount;
    private Boolean selected;
}
