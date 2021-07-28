package com.ecnu.ljw.second_demo.controller;

import com.ecnu.ljw.second_demo.service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;
    /**
     * 下单接口：导致超卖的错误示范
     * @param sid
     * @return
     */
    @RequestMapping("/createWrongOrder/{sid}")
    public String createWrongOrder(@PathVariable int sid) {
        int id = 0;
        try {
            id = orderService.createWrongOrder(sid);
            log.info("创建订单id: [{}]", id);
        } catch (Exception e) {
            log.error("Exception", e);
        }
        return String.valueOf(id);
    }
}
