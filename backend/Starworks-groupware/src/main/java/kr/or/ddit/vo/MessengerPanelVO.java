package kr.or.ddit.vo;

import java.util.List;

import lombok.Data;

@Data
public class MessengerPanelVO {
    private List<MessengerRoomVO> rooms;
    private int unreadRoomCount;
    private int unreadMessageCount;
}
