package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.or.ddit.vo.FileDetailVO;

@Mapper
public interface FileDetailMapper {
    List<FileDetailVO> selectFileDetailList(String fileId);

    FileDetailVO selectFileDetail(Integer fileSeq);

    FileDetailVO selectFileDetailS3(String saveFileNm);

    FileDetailVO selectFileDetailBySaveName(String saveFileNm);

    int insertFileDetail(FileDetailVO fdVO);

    int deleteFileDetail(String fileId, Integer fileSeq);

    int restoreFileDetail(FileDetailVO fdVO);
}
