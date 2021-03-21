package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ActivityController {

    //自定义一个控制器
    @GetMapping("couponInfo.html")
    public String couponInfo(){

        //返回优惠劵列表
        return "couponInfo/index";
    }
}
