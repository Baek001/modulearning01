package kr.or.ddit.comm.file.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.comm.file.service.FileDetailService;
import kr.or.ddit.comm.file.util.ObjectStorageUrlResolver;
import kr.or.ddit.mybatis.mapper.FileDetailMapper;
import kr.or.ddit.vo.FileDetailVO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileDetailServiceImpl implements FileDetailService {

    private final FileDetailMapper mapper;
    private final ObjectStorageUrlResolver objectStorageUrlResolver;

    @Override
    public List<FileDetailVO> readFileDetailList(String fileId) {
        return mapper.selectFileDetailList(fileId);
    }

    @Override
    public FileDetailVO readFileDetail(Integer fileSeq) {
        FileDetailVO vo = mapper.selectFileDetail(fileSeq);
        if (vo == null) {
            Map<String, Integer> fileDetail = new HashMap<>();
            fileDetail.put("fileSeq", fileSeq);
            throw new EntityNotFoundException(fileDetail);
        }
        return vo;
    }

    @Override
    public Map<String, Object> readFileDetailS3(String saveFileNm) {
        FileDetailVO fileDetail = mapper.selectFileDetailS3(saveFileNm);
        if (fileDetail == null) {
            throw new EntityNotFoundException(Map.of("saveFileNm", saveFileNm));
        }

        String filePath = fileDetail.getFilePath();
        String keyString = objectStorageUrlResolver.extractObjectKey(filePath);

        Map<String, Object> respMap = new HashMap<>();
        respMap.put("key", keyString);
        respMap.put("orgnFileNm", fileDetail.getOrgnFileNm());
        return respMap;
    }

    @Override
    public FileDetailVO readFileDetailBySaveName(String saveFileNm) {
        FileDetailVO fileDetail = mapper.selectFileDetailBySaveName(saveFileNm);
        if (fileDetail == null) {
            throw new EntityNotFoundException(Map.of("saveFileNm", saveFileNm));
        }
        return fileDetail;
    }

    @Override
    public boolean createFileDetail(FileDetailVO fd) {
        return mapper.insertFileDetail(fd) > 0;
    }

    @Override
    public boolean removeFileDetail(String fileId, Integer fileSeq) {
        return mapper.deleteFileDetail(fileId, fileSeq) > 0;
    }
}
