package com.ecnu.ljw.second_demo.service.serviceImpl;

import java.util.concurrent.TimeUnit;

import com.ecnu.ljw.second_demo.entity.Stock;
import com.ecnu.ljw.second_demo.entity.User;
import com.ecnu.ljw.second_demo.mapper.UserMapper;
import com.ecnu.ljw.second_demo.service.StockService;
import com.ecnu.ljw.second_demo.service.UserService;
import com.ecnu.ljw.second_demo.utils.CacheKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl implements UserService{

    private static final String SALT = "randomString";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StockService stockService;

    @Override
    public String getVerifyHash(Integer sid, Integer userId) throws Exception {

        //验证是否在抢购时间
        log.info("请自行验证是否在抢购时间内");

        //检查用户的合法性
        User user = userMapper.selectByPrimaryKey(userId.longValue());
        if(user == null){
            throw new Exception("用户不存在");
        }
        log.info("用户信息：[{}]", user.toString());

        //检查商品的合法性
        Stock stock = stockService.getStockById(sid);
        if(stock == null){
            throw new Exception("商品不存在");
        }
        log.info("商品信息：[{}]", stock.toString());

        //生成hash
        String verify = SALT + sid + userId;
        String verifyHash = DigestUtils.md5DigestAsHex(verify.getBytes());

        //将hash和用户商品信息存入redis
        String hashKey = CacheKey.HASH_KEY.getKey() + "_" + sid + "_" + userId;
        stringRedisTemplate.opsForValue().set(hashKey, verifyHash, 3600,TimeUnit.SECONDS);
        log.info("Redis写入：[{}] [{}]", hashKey, verifyHash);
        return verifyHash;
    }

    @Override
    public int addUserCount(Integer userId) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean getUserIsBanned(Integer userId) {
        // TODO Auto-generated method stub
        return false;
    }

}
