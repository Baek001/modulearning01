package kr.or.ddit.comm.paging;

import java.io.Serializable;

import lombok.Getter;

/**
 * 페이징 처리와 관련된 모든 데이터를 가진 객체 
 */
@Getter
public class PaginationInfo<T> implements Serializable{
	
	public PaginationInfo() {
		this(10, 5);
	}
	
	public PaginationInfo(int screenSize, int blockSize) {
		super();
		this.screenSize = screenSize;
		this.blockSize = blockSize;
	}
	
	private int totalRecord; // DB 조회
	
	private int screenSize; // 임의 결정
	private int blockSize; // 임의 결정
	
	private int currentPage; // 사용자 요청 
	
	private SimpleSearch simpleSearch; // 단순 키워드 검색에 사용
	public void setSimpleSearch(SimpleSearch simpleSearch) {
		this.simpleSearch = simpleSearch;
	}
	
	private T detailSearch;
	public void setDetailSearch(T detailSearch) {
		this.detailSearch = detailSearch;
	}
	
	
//	private int totalPage;
//	private int startRow;
//	private int endRow;
//	private int startPage;
//	private int endPage;
	
	public int getStartPage() {
		return ((currentPage - 1) / blockSize) * blockSize + 1;
	}
	
	public int getEndPage() {
		int endPage = blockSize * ( (currentPage + (blockSize-1)) / blockSize );
		return Math.min(endPage, getTotalPage());
	}
	
	public int getTotalPage() {
		return ( totalRecord + (screenSize - 1) ) / screenSize;
	}

	public int getEndRow() {
		return screenSize * currentPage;
	}
	
	public int getStartRow() {
		return getEndRow() - (screenSize - 1);
	}
	
	public void setTotalRecord(int totalRecord) {
		this.totalRecord = totalRecord;
	}
	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}
}
