package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.service.TestService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "测试接口")
@RestController
@RequestMapping("admin/product/test")
public class TestController {

    @Autowired
    private TestService testService;
    @GetMapping("testLock")
    public Result testLock(){

        testService.testLock();
        return Result.ok();
    }

    //写锁
    @GetMapping("write")
    public  Result writeLock(){
        //调用服务层方法
        String msg = testService.writeLock();
        //返回
        return Result.ok(msg);
    }

    //读锁
    @GetMapping("read")
    public Result readLock(){
        String msg = testService.readLock();
        //返回
        return Result.ok(msg);
    }
}
