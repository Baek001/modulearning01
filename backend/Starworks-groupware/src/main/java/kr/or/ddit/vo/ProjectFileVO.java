package kr.or.ddit.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author 김주민
 * @since 2025. 10. 13.
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      			수정자           수정내용
 *  -----------   	-------------    ---------------------------
 *  2025. 10. 13.     	김주민	          최초 생성
 *
 * </pre>
 */
@Data
@EqualsAndHashCode(of = {"fileSeq", "fileId"})
public class ProjectFileVO {

    private Integer fileSeq;        // 파일 순번
    private String fileId;          // 파일 ID
    private String orgnFileNm;      // 원본 파일명
    private String saveFileNm;      // 저장 파일명
    private Long fileSize;          // 파일 크기
    private String extFile;         // 파일 확장자
    private String filePath;        // 파일 경로

    private String fileSource;      // 파일 출처 (프로젝트/업무/게시글)
    private String sourceName;      // 출처명 (프로젝트명/업무명/게시글 제목)
    private String sourceId;        // 출처 ID
    private String uploadDate;      // 업로드 날짜
    private String uploaderName;    // 업로더 이름
}