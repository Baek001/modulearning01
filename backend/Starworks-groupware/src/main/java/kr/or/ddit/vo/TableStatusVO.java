package kr.or.ddit.vo;

import lombok.Data;

@Data
public class TableStatusVO {
	private String tableName;
	private String status;
	private int numRows;

}
