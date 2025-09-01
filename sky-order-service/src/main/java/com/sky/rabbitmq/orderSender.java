package com.sky.rabbitmq;

import com.sky.dto.MessageDTO;
import com.sky.utils.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class orderSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 发送预约消息
    public void sendOrderMessage(MessageDTO messageDTO) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MESSAGE_EXCHANGE,
                "order.message",  // 路由键
                messageDTO  // 消息内容
        );
    }
}