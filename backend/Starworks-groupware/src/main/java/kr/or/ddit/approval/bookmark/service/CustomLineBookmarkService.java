package kr.or.ddit.approval.bookmark.service;

import java.util.List;

import kr.or.ddit.vo.CustomLineBookmarkVO;

public interface CustomLineBookmarkService {

    boolean createCustomLineBookmark(CustomLineBookmarkVO custLineBookmark);

    List<CustomLineBookmarkVO> readCustomLineBookmarkList(String userId);

    boolean removeCustomLineBookmark(String userId, String cstmLineBmNm);
}
