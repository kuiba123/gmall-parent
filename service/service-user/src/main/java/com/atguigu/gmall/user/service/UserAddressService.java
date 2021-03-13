package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;

import java.util.List;

public interface UserAddressService {

    //根据用户Id查询用户收货地址列表
    List<UserAddress> findUserAddressListById(String userId);

}