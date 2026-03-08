package kr.or.ddit.mybatis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import kr.or.ddit.vo.TableStatusVO;
@Mapper
public interface TableStatusMapper  {
	@Select("""
			select TABLE_NAME,STATUS, NUM_ROWS
from USER_TABLES
			""")
	public List<TableStatusVO> selectTableStatusList();

}
