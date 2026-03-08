package kr.or.ddit.comm.paging.renderer;

import kr.or.ddit.comm.paging.PaginationInfo;

public class DefaultPaginationRenderer implements PaginationRenderer {

	@Override
	public String renderPagination(PaginationInfo paging, String fnName) {
//		String pattern = "<a href='?page=%d'>%s</a>\n";
		String pattern = "<a href='javascript:void(0);' onclick='"+fnName+"(%d)'>%s</a>\n";
		
		int startPage = paging.getStartPage();
		int endPage = paging.getEndPage();
		int totalPage = paging.getTotalPage();
		int currentPage = paging.getCurrentPage();
		
		StringBuffer html = new StringBuffer();
		if(startPage > 1) {
			html.append(
				String.format(pattern, startPage - 1, "이전")
			);
		}
		
		for(int page = startPage ; page <= endPage ; page++) {
			if(page==currentPage) {
				html.append(
					String.format("[%d]", page)	
				);
			}else {
				html.append(
					String.format(pattern, page, page)
				);
			}
		}
		
		if(endPage < totalPage) {
			html.append(
				String.format(pattern, endPage+1, "다음")
			);
		}
		return html.toString();
	}

}
