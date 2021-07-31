package com.ecnu.ljw.second_demo.controller;

import com.ecnu.ljw.second_demo.service.OrderService;
import com.ecnu.ljw.second_demo.service.UserService;
import com.google.common.util.concurrent.RateLimiter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    // Guava令牌桶：每秒放行10个请求
    RateLimiter rateLimiter = RateLimiter.create(10);
    
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
        //1、阻塞式获取令牌
        //阻塞式获取令牌：请求进来后，若令牌桶里没有足够的令牌，就在这里阻塞住，等待令牌的发放。
        log.info("等待时间" + rateLimiter.acquire());
        //2、非阻塞式获取令牌
        //非阻塞式获取令牌：请求进来后，若令牌桶里没有足够的令牌，会尝试等待设置好的时间（这里写了1000ms），
        //其会自动判断在1000ms后，这个请求能不能拿到令牌，如果不能拿到，直接返回抢购失败。
        //如果timeout设置为0，则等于阻塞时获取令牌。
        /*
        if(!rateLimiter.tryAcquire(1000,TimeUnit.MILLISECONDS)){
            log.warn("你被限流了，真不幸，直接返回失败");
            return "购买失败，库存不足";
        }
        */
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
    
    /**
     * 下单接口：悲观锁更新库存，事务for update更新库存
     */
    @RequestMapping(value="/createPessimisticOrder/{sid}", method=RequestMethod.GET)
    public String createPessimisticOrder(@PathVariable int sid) {
        int id;
        try {
            id = orderService.createPessimisticOrder(sid);
            log.info("购买成功，剩余库存为：[{}]", id);
        } catch (Exception e) {
            log.info("购买失败：[{}]", e.getMessage());
            return "购买失败，库存不足";
        }
        return String.format("购买成功，剩余库存为：%d", id);
    }

    /**
     * 验证接口：下单前用户获取验证值
     * @return
     */
    @RequestMapping(value = "/getVerifyHash", method = {RequestMethod.GET})
    public String getVerifyHash(@RequestParam(value = "sid") Integer sid,
                                @RequestParam(value = "userId") Integer userId) {
        String hash;
        try {
            hash = userService.getVerifyHash(sid, userId);
        } catch (Exception e) {
            log.error("获取验证hash失败，原因：[{}]", e.getMessage());
            return "获取验证hash失败";
        }
        return String.format("请求抢购验证hash值为：%s", hash);
    }

    /**
     * 下单接口：要求用户验证的抢购接口
     * @param sid
     * @param userId
     * @param verifyHash
     * @return
     */
    @RequestMapping(value = "/createOrderWithVerifiedUrl", method = {RequestMethod.GET})
    public String createOrderWithVerifiedUrl(@RequestParam(value = "sid") Integer sid,
                                             @RequestParam(value = "userId") Integer userId,
                                             @RequestParam(value = "verifyHash") String verifyHash) {
        int stockLeft;
        try {
            stockLeft = orderService.createVerifiedOrder(sid, userId, verifyHash);
            log.info("购买成功，剩余库存为: [{}]", stockLeft);
        } catch (Exception e) {
            log.error("购买失败：[{}]", e.getMessage());
            return e.getMessage();
        }
        return String.format("购买成功，剩余库存为：%d", stockLeft);
    }
}
