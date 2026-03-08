package kr.or.ddit.mybatis.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
class TableStatusMapperTest {
	
	@Autowired
	TableStatusMapper mapper;
	
	@Test
	void testSelectTableStatusList() {
		mapper.selectTableStatusList().forEach(ts->log.info("{}",ts));
	}

}
