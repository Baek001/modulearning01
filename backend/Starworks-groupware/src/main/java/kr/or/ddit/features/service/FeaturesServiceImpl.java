package kr.or.ddit.features.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.or.ddit.comm.exception.EntityNotFoundException;
import kr.or.ddit.mybatis.mapper.FeaturesMapper;
import kr.or.ddit.vo.FeaturesVO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeaturesServiceImpl implements FeaturesService{

	private final FeaturesMapper mapper;
	
	@Override
	public List<FeaturesVO> readFeaturesList() {
		return mapper.selectFeaturesList();
	}

	@Override
	public FeaturesVO readFeatures(String featureId) {
		FeaturesVO features = mapper.selectFeatures(featureId);
		if(features == null) {
			throw new EntityNotFoundException(featureId);
		}
		return features;
	}

}
