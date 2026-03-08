package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.AuthorizationDocumentVO;

@Mapper
public interface ApprovalReactMapper {

    List<AuthorizationDocumentVO> selectDraftDocuments(@Param("userId") String userId);

    List<AuthorizationDocumentVO> selectInboxDocuments(@Param("userId") String userId);

    List<AuthorizationDocumentVO> selectUpcomingDocuments(@Param("userId") String userId);

    List<AuthorizationDocumentVO> selectReferenceDocuments(@Param("userId") String userId);

    List<AuthorizationDocumentVO> selectArchiveDocuments(@Param("userId") String userId);

    int existsDocumentAccess(@Param("atrzDocId") String atrzDocId, @Param("userId") String userId);
}
