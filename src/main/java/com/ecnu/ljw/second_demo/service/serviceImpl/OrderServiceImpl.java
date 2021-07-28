package com.ecnu.ljw.second_demo.service.serviceImpl;

import com.ecnu.ljw.second_demo.entity.Order;
import com.ecnu.ljw.second_demo.entity.Stock;
import com.ecnu.ljw.second_demo.mapper.OrderMapper;
import com.ecnu.ljw.second_demo.service.OrderService;
import com.ecnu.ljw.second_demo.service.StockService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService{

    @Autowired
    private StockService stockService;

    @Autowired
    private OrderMapper orderMapper;

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

    @Override
    public int createPessimisticOrder(int sid) {
        //校验库存 悲观锁的方式
        return 0;
    }

    @Override
    public int createVerifiedOrder(Integer sid, Integer userId, String verifyHash) throws Exception {
        // TODO Auto-generated method stub
        return 0;
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
}
