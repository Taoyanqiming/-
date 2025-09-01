package com.sky.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Integer sender;
    private Integer receiver;
    //0系统消息，1回复消息，2点赞消息，3订单消息,4预约消息
    private Integer type;
    private String sourceModule;
    private Integer sourceId;
    private String content;
}
