package kr.or.ddit.approval.bookmark.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.or.ddit.mybatis.mapper.CustomLineBookmarkMapper;
import kr.or.ddit.vo.CustomLineBookmarkVO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomLineBookmarkServiceImpl implements CustomLineBookmarkService {

    private final CustomLineBookmarkMapper mapper;

    @Override
    public boolean createCustomLineBookmark(CustomLineBookmarkVO custLineBookmark) {
        return mapper.insertCustomLineBookmark(custLineBookmark) > 0;
    }

    @Override
    public List<CustomLineBookmarkVO> readCustomLineBookmarkList(String userId) {
        return mapper.selectCustomLineBookmarkList(userId);
    }

    @Override
    public boolean removeCustomLineBookmark(String userId, String cstmLineBmNm) {
        return mapper.deleteCustomLineBookmark(userId, cstmLineBmNm) > 0;
    }
}
