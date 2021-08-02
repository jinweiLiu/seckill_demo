package com.ecnu.ljw.second_demo.receiver;

import com.alibaba.fastjson.JSONObject;
import com.ecnu.ljw.second_demo.service.OrderService;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@RabbitListener(queues = "orderQueue")
@Slf4j
public class OrderMqReceiver {

    @Autowired
    private OrderService orderService;

    @RabbitHandler
    public void process(String message) {
        log.info("OrderMqReceiver收到消息开始用户下单流程: " + message);
        JSONObject jsonObject = JSONObject.parseObject(message);
        try {
            orderService.createOrderByMq(jsonObject.getInteger("sid"), jsonObject.getInteger("userId"));
        } catch (Exception e) {
            log.error("消息处理异常：", e);;
        }
    }
}
