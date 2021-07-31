package com.ecnu.ljw.second_demo.service.serviceImpl;

import com.ecnu.ljw.second_demo.entity.Order;
import com.ecnu.ljw.second_demo.entity.Stock;
import com.ecnu.ljw.second_demo.entity.User;
import com.ecnu.ljw.second_demo.mapper.OrderMapper;
import com.ecnu.ljw.second_demo.mapper.UserMapper;
import com.ecnu.ljw.second_demo.service.OrderService;
import com.ecnu.ljw.second_demo.service.StockService;
import com.ecnu.ljw.second_demo.utils.CacheKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService{

    @Autowired
    private StockService stockService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public int createWrongOrder(int sid) {
        //校验库存
        Stock stock = checkStock(sid);
        //扣库存
        saleStock(stock);
        //创建订单
        int id = createOrder(stock);
        return id;
    }

    @Override
    public int createOptimisticOrder(int sid) {
        //校验库存
        Stock stock = checkStock(sid);
        //乐观锁更新库存 version
        boolean sucess = saleStockOptimistic(stock);
        if(!sucess){
            throw new RuntimeException("过期库存值，更新失败");
        }
        //创建订单
        createOrder(stock);
        return stock.getCount() - (stock.getSale() + 1);
    }

    @Transactional(rollbackFor = Exception.class, propagation =  Propagation.REQUIRED)
    @Override
    public int createPessimisticOrder(int sid) {
        //校验库存 悲观锁的方式
        Stock stock = checkStockForUpdate(sid);
        //更新库存
        saleStock(stock);
        //创建订单
        createOrder(stock);
        return stock.getCount() - (stock.getSale() + 1);
    }

    @Override
    public int createVerifiedOrder(Integer sid, Integer userId, String verifyHash) throws Exception {

        //验证是否在抢购时间内
        log.info("请自行验证是否在抢购时间内,假设此处验证成功");

        //验证hash值合法性
        String hashKey = CacheKey.HASH_KEY.getKey() + "_" + sid + "_" + userId;
        System.out.println(hashKey);
        String verifyHashInRedis = stringRedisTemplate.opsForValue().get(hashKey);

        if (!verifyHash.equals(verifyHashInRedis)) {
            throw new Exception("hash值与Redis中不符合");
        }
        log.info("验证hash值合法性成功");

        // 检查用户合法性
        User user = userMapper.selectByPrimaryKey(userId.longValue());
        if (user == null) {
            throw new Exception("用户不存在");
        }
        log.info("用户信息验证成功：[{}]", user.toString());

        // 检查商品合法性
        Stock stock = stockService.getStockById(sid);
        if (stock == null) {
            throw new Exception("商品不存在");
        }
        log.info("商品信息验证成功：[{}]", stock.toString());

        //乐观锁更新库存
        boolean success = saleStockOptimistic(stock);
        if (!success){
            throw new RuntimeException("过期库存值，更新失败");
        }
        log.info("乐观锁更新库存成功");

        //创建订单
        createOrderWithUserInfoInDB(stock, userId);
        log.info("创建订单成功");

        return stock.getCount() - (stock.getSale()+1);
    }

    @Override
    public void createOrderByMq(Integer sid, Integer userId) throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Boolean checkUserOrderInfoInCache(Integer sid, Integer userId) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }
    
    /**
     * 检查库存
     * @param sid
     * @return
     */
    private Stock checkStock(int sid) {
        Stock stock = stockService.getStockById(sid);
        if (stock.getSale().equals(stock.getCount())) {
            throw new RuntimeException("库存不足");
        }
        return stock;
    }

    /**
     * 更新库存
     * @param stock
     */
    private void saleStock(Stock stock) {
        stock.setSale(stock.getSale() + 1);
        stock.setVersion(stock.getVersion() + 1);
        stockService.updateStockById(stock);
    }

    /**
     * 更新库存 乐观锁
     * @param stock
     */
    private boolean saleStockOptimistic(Stock stock) {
        log.info("查询数据库，尝试更新库存");
        int count = stockService.updateStockByOptimistic(stock);
        return count != 0;
    }

    private Stock checkStockForUpdate(int sid){
        Stock stock = stockService.getStockByIdForUpdate(sid);
        if(stock.getSale().equals(stock.getCount())){
            throw new RuntimeException("库存不足");
        }
        return stock;
    }

    /**
     * 创建订单
     * @param stock
     * @return
     */
    private int createOrder(Stock stock) {
        Order order = new Order();
        order.setSid(stock.getId());
        order.setName(stock.getName());
        return orderMapper.insertSelective(order);
    }

    /**
     * 创建订单：保存用户订单信息到数据库
     * @param stock
     * @return
     */
    private int createOrderWithUserInfoInDB(Stock stock, Integer userId) {
        Order order = new Order();
        order.setSid(stock.getId());
        order.setName(stock.getName());
        order.setUserId(userId);
        return orderMapper.insertSelective(order);
    }
}
