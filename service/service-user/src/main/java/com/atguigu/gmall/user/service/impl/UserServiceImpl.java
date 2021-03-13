package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public UserInfo login(UserInfo userInfo) {
        //select * from userInfo where userName = ? and passwd = ?
        //密码是加密的
        String passwd = userInfo.getPasswd();
        //获取到的是明文,通过MD5加密与数据库进行匹配
        String newPassword = DigestUtils.md5DigestAsHex(passwd.getBytes());

        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("login_name",userInfo.getLoginName());
        userInfoQueryWrapper.eq("passwd",newPassword);
        UserInfo info = userInfoMapper.selectOne(userInfoQueryWrapper);
        if (info != null) {
            return info;
        }
        return null;
    }
}
