package com.spring.bean.config;

/**
 * @author: 16404
 * @date: 2025/2/6 18:16
 **/
public class BeanReference {
    private final String beanName;

    public BeanReference(String beanName) {
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }
}
