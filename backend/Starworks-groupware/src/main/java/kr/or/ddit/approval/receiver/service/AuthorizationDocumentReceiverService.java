package kr.or.ddit.approval.receiver.service;

import java.util.List;

import kr.or.ddit.vo.AuthorizationDocumentReceiverVO;

public interface AuthorizationDocumentReceiverService {

    boolean createAuthDocumentReceiver(AuthorizationDocumentReceiverVO authDocumentReceiver);

    List<AuthorizationDocumentReceiverVO> readAuthDocumentReceiverList();

    List<AuthorizationDocumentReceiverVO> readAuthDocumentReceiverList(String atrzDocId);

    AuthorizationDocumentReceiverVO readAuthDocumentReceiver(String atrzRcvrId);
}
