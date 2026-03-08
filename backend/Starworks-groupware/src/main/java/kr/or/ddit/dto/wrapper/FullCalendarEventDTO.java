package kr.or.ddit.dto.wrapper;

public abstract class FullCalendarEventDTO<T> {
	private final T adaptee;


	public FullCalendarEventDTO(T adaptee) {
		super();
		this.adaptee = adaptee;
	}

	abstract String getId();

	public T getExtendedProps() {
		return adaptee;
	}
}
