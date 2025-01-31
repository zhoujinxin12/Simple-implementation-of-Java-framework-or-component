package com.zxx.service;

import com.spring.*;

@Component("userServiceImpl")
//@Scope("prototype") // 原型模式
//@Scope("singleton") // 原型模式
public class UserServiceImpl implements InitializingBean, BeanNameAware, UserService {

    @Autowired
    private OrderService orderService;

    private String beanName;

    private String name;

    @Override
    public void test() {
        System.out.println(orderService);
        System.out.println(beanName);
        System.out.println(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // spring提供的初始化方法
        System.out.println("初始化方法");
    }
}
