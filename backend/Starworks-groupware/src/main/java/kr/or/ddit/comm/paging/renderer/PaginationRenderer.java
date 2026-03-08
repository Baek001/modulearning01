package kr.or.ddit.comm.paging.renderer;

import kr.or.ddit.comm.paging.PaginationInfo;

public interface PaginationRenderer {
	default String renderPagination(PaginationInfo paging) {
		return renderPagination(paging, "fnPaging");
	}
	
	String renderPagination(PaginationInfo paging, String fnName);
}
