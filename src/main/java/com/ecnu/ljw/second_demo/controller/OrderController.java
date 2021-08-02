package com.ecnu.ljw.second_demo.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSONObject;
import com.ecnu.ljw.second_demo.service.OrderService;
import com.ecnu.ljw.second_demo.service.StockService;
import com.ecnu.ljw.second_demo.service.UserService;
import com.google.common.util.concurrent.RateLimiter;

import org.springframework.amqp.core.AmqpTemplate;
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
    private StockService stockService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private AmqpTemplate rabbitTemplate;

    // Guava令牌桶：每秒放行10个请求
    RateLimiter rateLimiter = RateLimiter.create(10);

    // 延时时间：预估读数据库数据业务逻辑的耗时，用来做缓存再删除
    private static final int DELAY_MILLSECONDS = 1000;

    // 延时双删线程池
    private static ExecutorService cachedThreadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,new SynchronousQueue<Runnable>());
    
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

    /**
     * 下单接口：要求验证的抢购接口 + 单用户限制访问频率
     */
    @RequestMapping(value = "/createOrderWithVerifiedUrlAndLimit", method = {RequestMethod.GET})
    public String createOrderWithVerifiedUrlAndLimit(@RequestParam(value = "sid") Integer sid,
                                                     @RequestParam(value = "userId") Integer userId,
                                                     @RequestParam(value = "verifyHash") String verifyHash){
        int stockLeft;
        try {
            int count = userService.addUserCount(userId);
            log.info("用户截至该次的访问次数为：[{}]", count);
            boolean isBanned = userService.getUserIsBanned(userId);
            if(isBanned){
                return "购买失败，超过频率限制";
            }
            stockLeft = orderService.createVerifiedOrder(sid, userId, verifyHash);
            log.info("购买成功，剩余库存为: [{}]", stockLeft);
        } catch (Exception e) {
            log.error("购买失败：[{}]", e.getMessage());
            return e.getMessage();
        }
        return String.format("购买成功，剩余库存为：%d", stockLeft);
    }
    
    /**
     * 下单接口：先删除缓存，再更新数据库
     * @param sid
     * @return
     */
    @RequestMapping("/createOrderWithCacheV1/{sid}")
    public String createOrderWithCacheV1(@PathVariable int sid){
        int count = 0;
        try {
            //删除库存缓存
            stockService.delStockCountCache(sid);
            //完成扣库存下单事务
            count = orderService.createPessimisticOrder(sid);
        } catch (Exception e) {
            log.error("购买失败：[{}]", e.getMessage());
            return "购买失败，库存不足";
        }
        log.info("购买成功，剩余库存为: [{}]", count);
        return String.format("购买成功，剩余库存为：%d", count);
    }

    /**
     * 下单接口：先更新数据库，再删缓存
     * @param sid
     * @return
     */
    @RequestMapping("/createOrderWithCacheV2/{sid}")
    public String createOrderWithCacheV2(@PathVariable int sid) {
        int count = 0;
        try {
            // 完成扣库存下单事务
            count = orderService.createPessimisticOrder(sid);
            // 删除库存缓存
            stockService.delStockCountCache(sid);
        } catch (Exception e) {
            log.error("购买失败：[{}]", e.getMessage());
            return "购买失败，库存不足";
        }
        log.info("购买成功，剩余库存为: [{}]", count);
        return String.format("购买成功，剩余库存为：%d", count);
    }

    /**
     * 下单接口：先删除缓存，再更新数据库，缓存延时双删
     * @param sid
     * @return
     */
    @RequestMapping("/createOrderWithCacheV3/{sid}")
    public String createOrderWithCacheV3(@PathVariable int sid) {
        int count;
        try {
            // 删除库存缓存
            stockService.delStockCountCache(sid);
            // 完成扣库存下单事务
            count = orderService.createPessimisticOrder(sid);
            log.info("完成下单事务");
            // 延时指定时间后再次删除缓存
            cachedThreadPool.execute(new delCacheByThread(sid));
        } catch (Exception e) {
            log.error("购买失败：[{}]", e.getMessage());
            return "购买失败，库存不足";
        }
        log.info("购买成功，剩余库存为: [{}]", count);
        return String.format("购买成功，剩余库存为：%d", count);
    }

    /**
     * 缓存再删除线程
     */
    private class delCacheByThread implements Runnable {
        private int sid;
        public delCacheByThread(int sid) {
            this.sid = sid;
        }
        public void run() {
            try {
                log.info("异步执行缓存再删除，商品id：[{}]， 首先休眠：[{}] 毫秒", sid, DELAY_MILLSECONDS);
                Thread.sleep(DELAY_MILLSECONDS);
                stockService.delStockCountCache(sid);
                log.info("再次删除商品id：[{}] 缓存", sid);
            } catch (Exception e) {
                log.error("delCacheByThread执行出错", e);
            }
        }
    }

    /**
     * 下单接口：异步处理订单
     * @param sid
     * @return
     */
    @RequestMapping(value = "/createOrderWithMq", method = {RequestMethod.GET})
    public String createOrderWithMq(@RequestParam(value = "sid") Integer sid,
                                  @RequestParam(value = "userId") Integer userId) {
        try {
            // 检查缓存中商品是否还有库存
            Integer count = stockService.getStockCount(sid);
            if (count == 0) {
                return "秒杀请求失败，库存不足.....";
            }

            // 有库存，则将用户id和商品id封装为消息体传给消息队列处理
            // 注意这里的有库存和已经下单都是缓存中的结论，存在不可靠性，在消息队列中会查表再次验证
            log.info("有库存：[{}]", count);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sid", sid);
            jsonObject.put("userId", userId);
            sendToOrderQueue(jsonObject.toJSONString());
            return "秒杀请求提交成功";
        } catch (Exception e) {
            log.error("下单接口：异步处理订单异常：", e);
            return "秒杀请求失败，服务器正忙.....";
        }
    }


    /**
     * 向消息队列orderQueue发送消息
     * @param message
     */
    private void sendToOrderQueue(String message) {
        log.info("这就去通知消息队列开始下单：[{}]", message);
        this.rabbitTemplate.convertAndSend("orderQueue", message);
    }
}
