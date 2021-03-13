package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserInfo;

public interface UserService {

    /*
    * 登录方法
    * @param userInfo:
    * @return: com.atguigu.gmall.model.user.UserInfo
    */
    UserInfo login(UserInfo userInfo);


}
