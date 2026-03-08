package kr.or.ddit.approval.receiver.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.AuthorizationDocumentReceiverMapper;
import kr.or.ddit.vo.AuthorizationDocumentReceiverVO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthorizationDocumentReceiverServiceImpl implements AuthorizationDocumentReceiverService {

    private final AuthorizationDocumentReceiverMapper mapper;

    @Override
    public boolean createAuthDocumentReceiver(AuthorizationDocumentReceiverVO authDocumentReceiver) {
        return mapper.insertAuthDocumentReceiver(authDocumentReceiver) > 0;
    }

    @Override
    public List<AuthorizationDocumentReceiverVO> readAuthDocumentReceiverList() {
        return mapper.selectAuthDocumentReceiverList();
    }

    @Override
    public List<AuthorizationDocumentReceiverVO> readAuthDocumentReceiverList(String atrzDocId) {
        return mapper.selectAuthDocumentReceiverListByDocId(atrzDocId);
    }

    @Override
    public AuthorizationDocumentReceiverVO readAuthDocumentReceiver(String atrzRcvrId) {
        AuthorizationDocumentReceiverVO authDocumentReceiver = mapper.selectAuthDocumentReceiver(atrzRcvrId);
        if (authDocumentReceiver == null) {
            throw new EntityNotFoundException(atrzRcvrId);
        }
        return authDocumentReceiver;
    }
}
