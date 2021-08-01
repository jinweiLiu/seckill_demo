package com.ecnu.ljw.second_demo.service.serviceImpl;

import java.util.concurrent.TimeUnit;

import com.ecnu.ljw.second_demo.entity.Stock;
import com.ecnu.ljw.second_demo.mapper.StockMapper;
import com.ecnu.ljw.second_demo.service.StockService;
import com.ecnu.ljw.second_demo.utils.CacheKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StockServiceImpl implements StockService{

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Integer getStockCount(int id) {
        Integer left;
        left = getStockCountByCache(id);
        log.info("缓存中取得库存数：[{}]",left);
        if(left == null){
            left = getStockCountByDB(id);
            log.info("缓存未命中，查询数据库，并写入缓存");
            setStockCountCache(id, left);
        }
        return left;
    }

    @Override
    public int getStockCountByDB(int id) {
        Stock stock = stockMapper.selectByPrimaryKey(id);
        return stock.getCount() - stock.getSale();
    }

    @Override
    public Integer getStockCountByCache(int id) {
        String key = CacheKey.REDIS_KEY.getKey() + String.valueOf(id);
        String countStr = stringRedisTemplate.opsForValue().get(key);
        if(countStr == null){
            return null;
        }else{
            return Integer.parseInt(countStr);
        }
    }

    @Override
    public void setStockCountCache(int id, int count) {
        String key = CacheKey.REDIS_KEY.getKey() + String.valueOf(id);
        String countStr = String.valueOf(count);
        log.info("写入商品库存缓存: [{}] [{}]", key, countStr);
        stringRedisTemplate.opsForValue().set(key,countStr,3600,TimeUnit.SECONDS);
    }

    @Override
    public void delStockCountCache(int id) {
        String key = CacheKey.REDIS_KEY.getKey() + String.valueOf(id);
        stringRedisTemplate.delete(key);
        log.info("删除商品id：[{}] 缓存", id);
    }

    @Override
    public Stock getStockById(int id) {
        return stockMapper.selectByPrimaryKey(id);
    }

    @Override
    public Stock getStockByIdForUpdate(int id) {
        return stockMapper.selectByPrimaryKeyForUpdate(id);
    }

    @Override
    public int updateStockById(Stock stock) {
        return stockMapper.updateByPrimaryKeySelective(stock);
    }

    @Override
    public int updateStockByOptimistic(Stock stock) {
        return stockMapper.updateByOptimistic(stock);
    }
    
}
