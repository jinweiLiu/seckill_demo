### 秒杀系统学习和实现

- 初步搭建 \
  商品和订单

- 无锁实现方案 \
  不加锁情况会导致超卖现象，所有请求都生成了订单

- 加锁实现（悲观锁和乐观锁） 
  - 乐观锁实现   
    在更新数据库前先检查version和之前的是否一致 \
    jmeter利用100个请求测试，不会出现超卖现象，卖出数量和总数比为 21/50
  - 悲观锁(for update) \
    (1) for update是在数据库中上锁用的，可以为数据库中的行上一个排它锁。当一个事务的操作未完成时候，其他事务可以读取但是不能写入或更新。 \
    (2) @Transactional，将数据库操作变成一个事务，回滚时会抛出异常 \
    jmeter利用100个请求测试，不会出现超卖现象，请求排队进行处理，前50个请求生成订单，后50个请求没有生成订单
  - 乐观锁 + 令牌桶 \
    面临高并发的请购请求时，我们如果不对接口进行限流，可能会对后台系统造成极大的压力 \
    令牌桶实现限流，包括阻塞式和非阻塞式
- 接口隐藏 \
  避免大量恶意请求
  - 主要实现 \
    1 每次点击秒杀按钮，先从服务器获取一个秒杀验证值（接口内判断是否到秒杀时间） \
    2 Redis以缓存用户ID和商品ID为key，秒杀地址为Value缓存验证值 \
    3 用户请求秒杀商品的时候，要带上秒杀验证值进行校验
- 单用户限制频率 \
  限制用户的访问频率
  - 主要实现
    1 记录用户的访问次数 \
    2 设置访问次数限制，超过次数则禁止用户访问 