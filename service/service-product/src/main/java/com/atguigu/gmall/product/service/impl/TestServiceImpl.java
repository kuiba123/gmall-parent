package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.TestService;
import jodd.time.TimeUtil;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void testLock() {
        //获取对象
        RLock lock = redissonClient.getLock("lock");
        //加锁
        //lock.lock()
        //10秒后自动解锁
        lock.lock(10, TimeUnit.SECONDS);

        //业务逻辑
        String value = redisTemplate.opsForValue().get("num");

        //判断value是否为空
        if (StringUtils.isEmpty(value)) {
            return ;
        }

        //数据转换
        int num = Integer.parseInt(value);
        //放入缓存
        redisTemplate.opsForValue().set("num",String.valueOf(++num));
    }

    @Override
    public String writeLock() {
        //定义读写锁对象
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("anyLock");
        //获取写锁对象
        RLock rLock = rwlock.writeLock();
        //10秒自动解锁
        rLock.lock(10,TimeUnit.SECONDS);
        //定义一个写入数据
        String uuid = UUID.randomUUID().toString();
        //写入缓存
        redisTemplate.opsForValue().set("msg",uuid);
        return "写入数据完成";
    }

    @Override
    public String readLock() {
        //  定义读写锁对象
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("anyRWLock");
        //  获取读锁对象
        RLock rLock = rwlock.readLock();
        // 设置锁的时间
        rLock.lock(10,TimeUnit.SECONDS);

        // 读取缓存中对应的msg
        String msg = redisTemplate.opsForValue().get("msg");

        return msg;
    }
}
