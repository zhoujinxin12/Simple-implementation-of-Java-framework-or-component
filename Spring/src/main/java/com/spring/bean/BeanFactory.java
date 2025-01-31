package com.spring.bean;

/**
 * IoC容器最基本的接口，定义了获取Bean的基本方法
 */
public interface BeanFactory {
    Object getBean(String name);
    <T> T getBean(String name, Class<T> requiredType);
    boolean containsBean(String name);
    boolean isSingleton(String name);
    boolean isPrototype(String name);
}
