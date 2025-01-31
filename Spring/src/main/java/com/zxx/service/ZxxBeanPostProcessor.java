package com.zxx.service;

import com.spring.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Component
public class ZxxBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws Exception {
        System.out.println("初始化前");
        if (beanName.equals("userServiceImpl")) {
            ((UserServiceImpl) bean).setName("zxxUserServiceImpl");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
        System.out.println("初始化后");

        // 匹配切点
        if (beanName.equals("userServiceImpl")) {
            Object proxyInstance = Proxy.newProxyInstance(ZxxBeanPostProcessor.class.getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    System.out.println("执行代理逻辑");    // 找切点
                    return method.invoke(bean, args);
                }
            });
            return proxyInstance;
        }
        return bean;
    }
}
