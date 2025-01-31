package com.spring.bean.impl;

import com.spring.bean.BeanFactory;
import com.spring.exception.BeansException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MyBeanFactory implements BeanFactory {
    private Map<String, Object> beanMap = new ConcurrentHashMap<>();

    public void registerBean(String name, Object bean) {
        beanMap.put(name, bean);
    }

    @Override
    public Object getBean(String name) {
        return beanMap.get(name);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        Object bean = beanMap.get(name);
        if (requiredType.isInstance(bean)) {
            return requiredType.cast(bean);
        }
        throw new BeansException("Bean is not of required type");
    }

    @Override
    public boolean containsBean(String name) {
        return beanMap.containsKey(name);
    }

    @Override
    public boolean isSingleton(String name) {
        return false;
    }

    @Override
    public boolean isPrototype(String name) {
        return false;
    }
}
