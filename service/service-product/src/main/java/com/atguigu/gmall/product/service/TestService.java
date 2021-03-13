package com.atguigu.gmall.product.service;

public interface TestService {

    //测试本地锁
    void testLock();

    //写锁
    String writeLock();

    //读锁
    String readLock();
}
