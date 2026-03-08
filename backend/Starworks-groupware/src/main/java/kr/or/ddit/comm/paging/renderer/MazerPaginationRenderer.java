package kr.or.ddit.comm.paging.renderer;

import kr.or.ddit.comm.paging.PaginationInfo;

/**
 * Mazer/Bootstrap 테마용 페이지네이션 렌더러.
 *
 *[적용된 페이징 옵션]
 * 1. 총 페이지(totalPage) : 최소 1 이상으로 보정
 * 2. 현재 페이지(currentPage) : 1 이상, totalPage 이하로 보정
 * 3. 시작/끝 페이지(startPage, endPage) : null 값은 0으로 치환
 * 4. '이전' 버튼 : startPage-1 페이지로 이동, 단 최소 1
 *    - 비활성 조건: 현재 페이지가 1 이하일 경우
 * 5. '다음' 버튼 : endPage+1 페이지로 이동, 단 최대 totalPage
 *    - 비활성 조건: 현재 페이지가 totalPage 이상일 경우
 * 6. 숫자 버튼 : startPage ~ endPage 범위를 순회하며 생성
 *    - 현재 페이지는 active 표시 + aria-current="page" 적용
 * 7. 접근성(ARIA) 옵션 :
 *    - 전체 nav : aria-label="페이지"
 *    - 현재 페이지 : aria-current="page"
 *    - 비활성 버튼 : aria-disabled="true"
 */
//
/**
 *
 * @author 홍현택
 * @since 2025. 9. 26.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 9. 26.     	홍현택	          페이지 렌더러 수정.
 *  2025. 9. 27			홍현택			  페이지 렌더러 수정 2차. 주석 추가
 *
 * </pre>
 */
public class MazerPaginationRenderer implements PaginationRenderer {

    @Override
    public String renderPagination(PaginationInfo paging, String fnName) {
    	// - totalPage : 최소 1 이상 (0 이면 페이지네이션 자체가 안 그려짐...)
        int totalPage   = Math.max(1, paging.getTotalPage());
        // - startPage : 최소 1 이상
        int startPage   = Math.max(1, paging.getStartPage());
        // - endPage   : totalPage 이하
        int endPage     = Math.min(paging.getEndPage(), totalPage);
        // - currentPage : 1 이상, totalPage 이하로 보정
        int currentPage = paging.getCurrentPage();
        if (currentPage < 1) currentPage = 1;
        if (currentPage > totalPage) currentPage = totalPage;

        // 페이지가 1 이하면 렌더링 생략
//        if (totalPage <= 1) return "";

        // Mazer 테마의 pagination 구조 : <nav><ul class="pagination ..."> ... </ul></nav>
        StringBuilder html = new StringBuilder(256);
        html.append("<nav aria-label=\"페이지\">\n");
        html.append("<ul class=\"pagination pagination-primary justify-content-center\">\n");

        // "이전" 버튼의 활성화 조건... 첫 블록과 현재 페이지가 첫 페이지가 아닐때
        if (startPage > 1 && currentPage > 1) {
            html.append("<li class=\"page-item\">")
                .append("<a class=\"page-link\" href=\"javascript:void(0);\" onclick=\"")
                .append(fnName).append("(").append(startPage - 1).append("); return false;\">이전</a>")
                .append("</li>\n");
        } else {
        	// 비활성화 조건 : 첫블록/첫페이지 일경우
            html.append("<li class=\"page-item disabled\">")
                .append("<span class=\"page-link\">이전</span>")
                .append("</li>\n");
        }

        // 페이지 숫자 버튼
        for (int p = startPage; p <= endPage; p++) {
            if (p == currentPage) {
                html.append("<li class=\"page-item active\">")
                    .append("<span class=\"page-link\" aria-current=\"page\">")
                    .append(p)
                    .append("</span></li>\n");
            } else {
                html.append("<li class=\"page-item\">")
                    .append("<a class=\"page-link\" href=\"javascript:void(0);\" onclick=\"")
                    .append(fnName).append("(").append(p).append("); return false;\">")
                    .append(p)
                    .append("</a></li>\n");
            }
        }

        // "이전" 버튼의 활성화 조건... 첫 블록과 현재 페이지가 마지막이 아닐때
        if (endPage < totalPage && currentPage < totalPage) {
            html.append("<li class=\"page-item\">")
                .append("<a class=\"page-link\" href=\"javascript:void(0);\" onclick=\"")
                .append(fnName).append("(").append(endPage + 1).append("); return false;\">다음</a>")
                .append("</li>\n");
        // 비활성화 조건 : 마지막 블록/페이지 일 경우
        } else {
            html.append("<li class=\"page-item disabled\">")
                .append("<span class=\"page-link\">다음</span>")
                .append("</li>\n");
        }

        html.append("</ul>\n");
        html.append("</nav>\n");
        return html.toString();
    }
}
