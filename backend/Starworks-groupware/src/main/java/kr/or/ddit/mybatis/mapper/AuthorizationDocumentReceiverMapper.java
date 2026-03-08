package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.or.ddit.vo.AuthorizationDocumentReceiverVO;

@Mapper
public interface AuthorizationDocumentReceiverMapper {

    int insertAuthDocumentReceiver(AuthorizationDocumentReceiverVO authDocumentRec);

    List<AuthorizationDocumentReceiverVO> selectAuthDocumentReceiverList();

    List<AuthorizationDocumentReceiverVO> selectAuthDocumentReceiverListByDocId(@Param("atrzDocId") String atrzDocId);

    AuthorizationDocumentReceiverVO selectAuthDocumentReceiver(@Param("atrzRcvrId") String atrzRcvrId);
}
