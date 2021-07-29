package com.ecnu.ljw.second_demo.controller;

import com.ecnu.ljw.second_demo.service.OrderService;
import com.google.common.util.concurrent.RateLimiter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMethod;


@RestController
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Guava令牌桶：每秒放行10个请求
    //RateLimiter rateLimiter = RateLimiter.create(10);
    
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

    /**
     * 下单接口：乐观锁更新库存 + 令牌桶限流
     */
    @RequestMapping(value="/createOptimisticOrder/{sid}", method=RequestMethod.GET)
    public String createOptimisticOrder(@PathVariable int sid) {
        //1、阻塞使获取令牌
        //log.info("等待时间" + rateLimiter.acquire());
        int id;
        try{
            id = orderService.createOptimisticOrder(sid);
            log.info("购买成功，剩余库存为: [{}]", id);
        }catch(Exception e){
            log.error("购买失败：[{}]", e.getMessage());
            return "购买失败，库存不足";
        }
        return String.format("购买成功，剩余库存为：%d", id);
    }
    
}
