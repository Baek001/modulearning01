package kr.or.ddit.comm.paging;

import lombok.Data;

@Data
public class SimpleSearch {
    private String searchType;
    private String searchWord;
    private String startDate;
    private String endDate;
    private String searchStatus;
}