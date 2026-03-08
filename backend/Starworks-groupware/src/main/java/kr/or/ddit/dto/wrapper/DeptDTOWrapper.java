package kr.or.ddit.dto.wrapper;

import kr.or.ddit.dto.FullCalendarDeptDTO;

public class DeptDTOWrapper extends FullCalendarEventDTO<FullCalendarDeptDTO> {

	public DeptDTOWrapper(FullCalendarDeptDTO adaptee) {
		super(adaptee);
		// TODO Auto-generated constructor stub
	}

	@Override
	String getId() {
		// TODO Auto-generated method stub
		return getExtendedProps().getEventId();
	}

}
