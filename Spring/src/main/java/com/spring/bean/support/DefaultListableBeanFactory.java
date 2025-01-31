package com.spring.bean.support;

import com.spring.bean.*;
import com.spring.bean.config.BeanDefinition;
import com.spring.bean.config.BeanPostProcessor;
import com.spring.bean.config.ConfigurableBeanFactory;
import com.spring.bean.factory.ObjectFactory;
import com.spring.exception.BeansException;
import com.spring.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
        implements ConfigurableListableBeanFactory, BeanDefinitionRegistry {
    private static final Logger logger = LoggerFactory.getLogger(DefaultListableBeanFactory.class);

    // 存放定义beanDefinition的集合
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
    private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

    /**
     * Spring 容器中的 Bean 定义（BeanDefinition）可能来自多个来源（如父子容器、XML 配置、注解等），且可能存在继承或覆盖关系。
     * 合并过程：将父 Bean 定义与子 Bean 定义合并，生成最终的 RootBeanDefinition（合并结果）。
     */
    private final Map<String, BeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256);

    // 一级缓存：存放初始化完成的Bean
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
    // 二级缓存：存放原始的Bean对象（尚未填充属性）
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
    // 三级缓存：存放Bean工厂对象
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(16);

    // 创建Bean
    protected Object createBean(String beanName, BeanDefinition beanDefinition) {
        // 1. 创建Bean实例
        Object bean = createBeanInstance(beanDefinition);
        // 2. 填充属性
        populateBean(beanName, bean, beanDefinition);
        // 3. 初始化Bean
        return initializeBean(beanName, bean, beanDefinition);
    }

    /**
     * 初始化bean
     */
    // 1. bean的生命周期回调
    protected Object initializeBean(String beanName, Object bean, BeanDefinition beanDefinition) {
        // 1. 执行Aware接口方法
        if (bean instanceof Aware) {
            if (bean instanceof BeanFactoryAware) {
                ((BeanFactoryAware) bean).setBeanFactory(this);
            }
            if (bean instanceof BeanNameAware) {
                ((BeanNameAware) bean).setBeanName(beanName);
            }
        }

        // 2. 执行BeanPostProcessor的前置处理
        Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);

        // 3. 执行初始化方法
        try {
            invokeInitMethods(beanName, wrappedBean, beanDefinition);
        } catch (Exception e) {
            throw new BeansException("Invocation of init method failed", e);
        }

        // 4. 执行 BeanPostProcessor 的后置处理
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        return wrappedBean;
    }

    /**
     * 执行bean的初始化方法
     */
    protected void invokeInitMethods(String beanName, Object bean, BeanDefinition beanDefinition) throws Exception {
        // 执行 InitializingBean 接口的方法
        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }
        // 执行自定义的 init-method
        String initMethodName = beanDefinition.getInitMethodName();
        // initMethodName 不为null且包含非空白字符
        if (StringUtils.hasText(initMethodName)) {
            Method initMethod = bean.getClass().getMethod(initMethodName);
            if (initMethod == null) {
                throw new BeansException("Could not find an init method named '" + initMethodName + "' on bean with name '" + beanName + "'");
            }
            initMethod.invoke(bean);
        }
    }

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeansException {
        Objects.requireNonNull(beanName, "Bean name must not be null");
        Objects.requireNonNull(beanDefinition, "BeanDefinition must not be null");

        // 检查是否存在旧的bean定义
        BeanDefinition oldBeanDefinition = this.beanDefinitionMap.get(beanName);
        if (oldBeanDefinition != null) {
            // 如果作用域发生变化，需要清理相关缓存
            if (!Objects.equals(oldBeanDefinition.getScope(), beanDefinition.getScope())) {
                cleanupSingletonCache(beanName);
                // 移除旧的bean定义
                this.beanDefinitionMap.remove(beanName);
                this.mergedBeanDefinitions.remove(beanName);
                // 处理别名
                String[] aliases = getAliases(beanName);
                for (String alias : aliases) {
                    cleanupSingletonCache(alias);
                    this.mergedBeanDefinitions.remove(alias);
                }
            }
        }
        this.beanDefinitionMap.put(beanName, beanDefinition);
        // 如果是新的bean定义，添加到名称列表重
        if (!this.beanDefinitionNames.contains(beanName)) {
            this.beanDefinitionNames.add(beanName);
        }

        logger.debug("Registered bean definition for bean named '{}'", beanName);
    }

    protected void cleanupSingletonCache(String beanName) {
        synchronized (this.singletonObjects) {
            // 从所有缓存中移除
            this.singletonObjects.remove(beanName);
            this.earlySingletonObjects.remove(beanName);
            this.singletonFactories.remove(beanName);
            // 移除合并的bean定义
            this.mergedBeanDefinitions.remove(beanName);

            // 移除所有别名的缓存
            String[] aliases = getAliases(beanName);
            for (String alias : aliases) {
                this.singletonObjects.remove(alias);
                this.earlySingletonObjects.remove(alias);
                this.singletonFactories.remove(alias);
                this.mergedBeanDefinitions.remove(alias);
            }
        }
    }

    @Override
    public void removeBeanDefinition(String beanName) throws BeansException {

    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) throws BeansException {
        return null;
    }

    @Override
    public Object getBean(String name) throws BeansException {
        return doGetBean(name, null);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        return doGetBean(name, requiredType);
    }

    @Override
    protected Object doGetBean(String beanName) throws BeansException {
        return null;
    }

    @Override
    protected Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws BeansException {
            Object result = existingBean;
            // BeanPostProcessor 是一个接口，里面定义了 postProcessBeforeInitialization 和 postProcessAfterInitialization
            for (BeanPostProcessor processor : getBeanPostProcessors()) {
                // 通过初始化前的前置处理器postProcessBeforeInitialization，用于加工bean对象
                Object current = processor.postProcessBeforeInitialization(result, beanName);
                if (current == null) {
                    // 如果没有加工返回原对象
                    return result;
                }
                // 加工过后返回代理对象
                result = current;
            }
            return result;
    }

    @Override
    protected Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) throws BeansException {
            Object result = existingBean;
            for (BeanPostProcessor processor : getBeanPostProcessors()) {
                Object current = processor.postProcessAfterInitialization(result, beanName);
                if (current == null) {
                    return result;
                }
                result = current;
            }
            return result;
    }

    @Override
    public void preInstantiateSingletons() throws BeansException {

    }

    @Override
    public void ensureAllSingletonsInstantiated() throws BeansException {

    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return false;
    }

    @Override
    public int getBeanDefinitionCount() {
        return 0;
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return new String[0];
    }

    @Override
    public String[] getBeanNamesForType(Class<?> type) {
        return new String[0];
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) {
        return Map.of();
    }

    @Override
    public <T> T createBean(Class<T> beanClass) throws BeansException {
        return null;
    }

    @Override
    public void autowireBean(Object existingBean) throws BeansException {

    }

    @Override
    public Object configureBean(Object existingBean, String beanName) throws BeansException {
        return null;
    }

    @Override
    public Object resolveDependency(Class<?> descriptor, String beanName) throws BeansException {
        return null;
    }

    @Override
    public ConfigurableBeanFactory getBeanFactory() {
        return this;
    }

    @Override
    public int getBeanPostProcessorCount() {
        return 0;
    }

    @Override
    public void registerDependentBean(String beanName, String dependentBeanName) {

    }

    @Override
    public String[] getDependentBeans(String beanName) {
        return new String[0];
    }

    @Override
    public String[] getDependenciesForBean(String beanName) {
        return new String[0];
    }

    @Override
    public boolean containsLocalBean(String name) {
        return false;
    }

    @Override
    public boolean containsBean(String name) {
        return false;
    }

    @Override
    public boolean isSingleton(String name) {
        return false;
    }

    @Override
    public boolean isPrototype(String name) {
        return false;
    }

    @Override
    public boolean containsSingleton(String beanName) {
        return false;
    }

    @Override
    public String[] getSingletonNames() {
        return new String[0];
    }

    @Override
    public int getSingletonCount() {
        return 0;
    }

    @Override
    public Object getSingleton(String beanName) {
        return getSingleton(beanName, true);
    }

    protected Object getSingleton(String beanName, boolean allowEarlyReference) {
        // 首先检查一级缓存
        Object singletonObject = this.singletonObjects.get(beanName);
        if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
            synchronized (this.singletonObjects) {
                // 检查二级缓存
                singletonObject = this.earlySingletonObjects.get(beanName);

                if (singletonObject == null && allowEarlyReference) {
                    // 检查三级缓存
                    ObjectFactory<?> factory = this.singletonFactories.get(beanName);
                    if (factory != null) {
                        // 从工厂重获取对象
                        singletonObject = factory.getObject();
                        // 放入二级缓存
                        this.earlySingletonObjects.put(beanName, singletonObject);
                        // 从三级缓存移除
                        this.singletonFactories.remove(beanName);
                    }
                }
            }
        }
        return singletonObject;
    }
}
