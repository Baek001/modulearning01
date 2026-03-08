package kr.or.ddit.comm.file.service;

import java.util.List;
import java.util.Map;

import kr.or.ddit.vo.FileDetailVO;

public interface FileDetailService {

    List<FileDetailVO> readFileDetailList(String fileId);

    FileDetailVO readFileDetail(Integer fileSeq);

    Map<String, Object> readFileDetailS3(String saveName);

    FileDetailVO readFileDetailBySaveName(String saveFileNm);

    boolean createFileDetail(FileDetailVO fd);

    boolean removeFileDetail(String fileId, Integer fileSeq);
}
