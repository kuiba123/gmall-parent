package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo, HttpServletRequest request){

        System.out.println("进入控制器!");
        UserInfo info = userService.login(userInfo);

        if (info != null) {
            //  说明用户在数据库中存在登录成功
            String token = UUID.randomUUID().toString();
            //  需要在页面显示用户昵称
            String nickName = info.getNickName();
            HashMap<String, Object> map = new HashMap<>();
            map.put("nickName",nickName);
            map.put("token",token);

            //  当用户登录成功之后，我们需要将数据放入缓存！目的是为了判断用户在访问其他业务的时候，是否登录了。
            JSONObject userJson = new JSONObject();
            //  存储用户Id
            userJson.put("userId",info.getId().toString());
            //  存储当前的Ip 地址防止用户盗用cookie 中的token！
            userJson.put("ip", IpUtil.getIpAddress(request));

            //  放入缓存：
            //  key = user:login:token
            String userLoginKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(userLoginKey,userJson.toJSONString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            return Result.ok(map);
        }else {
            return Result.fail().message("用户名或密码错误");
        }
    }

    /*
    * 退出登录
    * @param request:
    * @return: com.atguigu.gmall.common.result.Result
    */
    @GetMapping("logout")
    public Result logout(HttpServletRequest request){
        redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX + request.getHeader("token"));
        return Result.ok();
    }
}
