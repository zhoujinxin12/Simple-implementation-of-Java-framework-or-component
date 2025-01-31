package com.spring.bean;

import java.util.Map;

/**
 * 提高了枚举Bean的能力，可以获取容器中所有Bean的信息
 */
public interface ListableBeanFactory extends BeanFactory{
    boolean containsBeanDefinition(String beanName);
    int getBeanDefinitionCount();
    String[] getBeanDefinitionNames();
    String[] getBeanNamesForType(Class<?> type);
    <T> Map<String, T> getBeansOfType(Class<T> type);
}
