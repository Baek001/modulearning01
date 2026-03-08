package kr.or.ddit.board.comment.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import kr.or.ddit.board.comment.service.BoardCommentService;
import kr.or.ddit.comm.file.FileFolderType;
import kr.or.ddit.comm.file.service.impl.FileDeleteServiceImpl;
import kr.or.ddit.comm.file.service.impl.FileUploadServiceImpl;
import kr.or.ddit.mybatis.mapper.BoardCommentMapper;
import kr.or.ddit.mybatis.mapper.BoardMapper;
import kr.or.ddit.vo.BoardCommentVO;
import kr.or.ddit.vo.BoardVO;
import kr.or.ddit.websocket.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoardCommentServiceImpl implements BoardCommentService {

    private final BoardCommentMapper mapper;
    private final FileUploadServiceImpl fileUploadService;
    private final FileDeleteServiceImpl fileDeleteService;

    @Autowired
    private NotificationServiceImpl notificationService;

    @Autowired
    private BoardMapper boardMapper;

    @Override
    public int readBoardCommentTotalCount(BoardCommentVO boardComment) {
        return mapper.selectBoardCommentTotalCount(boardComment);
    }

    @Override
    public List<BoardCommentVO> readBoardCommentList(String pstId) {
        return mapper.selectBoardCommentList(pstId);
    }

    @Override
    public BoardCommentVO readBoardCommentDetail(BoardCommentVO boardCommentVO) {
        return mapper.selectBoardCommentDetail(boardCommentVO);
    }

    @Override
    public BoardCommentVO readBoardReplyDetail(BoardCommentVO boardCommentVO) {
        return mapper.selectBoardReplyDetail(boardCommentVO);
    }

    @Override
    public boolean createBoardComment(BoardCommentVO boardComment) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderId = authentication.getName();
        String receiverId;
        String alarmCode;

        if (boardComment.getUpCmntSqn() == null) {
            BoardVO board = boardMapper.selectBoard(boardComment.getPstId());
            receiverId = board.getCrtUserId();
            alarmCode = "BOARD_01";
        } else {
            BoardCommentVO boardUpComment = new BoardCommentVO();
            boardUpComment.setCmntSqn(boardComment.getUpCmntSqn());
            boardUpComment = mapper.selectBoardCommentDetail(boardUpComment);
            receiverId = boardUpComment.getCrtUserId();
            alarmCode = "BOARD_02";
        }

        if (!receiverId.equals(senderId)) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("receiverId", receiverId);
            payload.put("senderId", senderId);
            payload.put("alarmCode", alarmCode);
            payload.put("pk", boardComment.getPstId());
            notificationService.sendNotification(payload);
        }

        fileUploadService.saveFileS3(boardComment, FileFolderType.BOARD.toString());
        return mapper.insertBoardComment(boardComment) > 0;
    }

    @Override
    public boolean modifyBoardComment(BoardCommentVO boardComment) {
        if (boardComment.getFileList() != null && !boardComment.getFileList().isEmpty()) {
            BoardCommentVO existing = mapper.selectBoardCommentDetail(boardComment);
            if (existing != null && existing.getCmntFileId() != null) {
                fileDeleteService.deleteFileDB(existing.getCmntFileId());
            }
            fileUploadService.saveFileS3(boardComment, FileFolderType.BOARD.toString());
        }
        return mapper.updateBoardComment(boardComment) > 0;
    }

    @Override
    public boolean removeBoardComment(String cmntId) {
        BoardCommentVO target = new BoardCommentVO();
        target.setCmntSqn(cmntId);
        BoardCommentVO existing = mapper.selectBoardCommentDetail(target);
        if (existing != null && existing.getCmntFileId() != null) {
            fileDeleteService.deleteFileDB(existing.getCmntFileId());
        }
        return mapper.deleteBoardComment(cmntId) > 0;
    }
}
