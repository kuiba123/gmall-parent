package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AlipayController {

    /*
    * 支付成功页面
    * @return: java.lang.String
    */
    @GetMapping("pay/success.html")
    public String success(){
        return "payment/success";
    }
}
