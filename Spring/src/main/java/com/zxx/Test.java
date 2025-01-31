package com.zxx;

import com.spring.ZxxApplicationContext;
import com.zxx.service.OrderService;
import com.zxx.service.UserService;

public class Test {
    public static void main(String[] args) {
        ZxxApplicationContext applicationContext = new ZxxApplicationContext(AppConfig.class);

        UserService userService = (UserService) applicationContext.getBean("userServiceImpl");
        OrderService orderService  = (OrderService) applicationContext.getBean("orderService");
        userService.test();   // 1. 代理对象 2. 业务test方法
        System.out.println(orderService);
    }
}
