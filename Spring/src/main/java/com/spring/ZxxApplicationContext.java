package com.spring;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZxxApplicationContext {

    private Class configClass;

    // 单例池: 存放所有的单例对象
    private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>();
    // BeanDefinition池: 存放所有的BeanDefinition对象
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public ZxxApplicationContext(Class configClass) {
        this.configClass = configClass;

        // 1.解析配置类
        // ComponentScan注解--->扫描路径--->扫描--->BeanDefinition--->BeanDefinitionMap
        scan(configClass);

        // 2.创建Bean
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if (beanDefinition.getScope().equals("singleton")) {
                Object bean = createBean(beanName, beanDefinition);// 单例Bean
                singletonObjects.put(beanName, bean);
            }
        }
    }

    private void scan(Class configClass) {
        // 1.解析配置类
        // ComponentScan注解--->扫描路径--->扫描
        // getDeclaredAnnotation(ComponentScan.class)方法返回直接存在于configClass此元素上的ComponentScan注解。
        // 如果此元素上没有这样的注解，则此方法返回null。
        // 当configClass = AppConfig.class时，
        // getDeclaredAnnotation(ComponentScan.class)返回@ComponentScan("com.zxx.service")
        // 所以这里使用强转为ComponentScan类型
        // componentScanAnnotation.value()就是@ComponentScan("com.zxx.service")中的"com.zxx.service"
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
        String path = componentScanAnnotation.value();
        System.out.println("扫描路径：" + path);
        // 至此得到扫描路径，获得扫描路径下的类。

        // 2. 扫描
        // BootStrap--->jre/lib
        // Ext--->jre/ext/lib
        // App--->classpath
        ClassLoader classLoader = ZxxApplicationContext.class.getClassLoader();
        // 2.1 获取path路径下的所有资源
        // 2.1.1 将包名中的.替换为/，因为getResource()方法中的参数是路径，返回URL对象
        // resource=file:/D:/project/java_project/handwritten_frame/Spring/target/classes/com/zxx/service
        URL resource = classLoader.getResource(path.replace(".", "/"));
        // 2.1.2 获取URL对象的文件, file=D:\project\java_project\handwritten_frame\Spring\target\classes\com\zxx\service
        File file = new File(resource.getFile());
        // 2.1.3 判断文件是目录还是文件
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            // 2.1.4 遍历文件
            for (File f : files) {
                // f=D:\project\java_project\handwritten_frame\Spring\target\classes\com\zxx\service\*.class
                String fileName = f.getAbsolutePath();
                String className = fileName.substring(fileName.indexOf("com"), fileName.indexOf(".class"));
                className = className.replace("\\", ".");
                // className=com.zxx.service.UserServiceImpl
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    if (clazz.isAnnotationPresent(Component.class)) {
                        // 2.1.5 判断类是否有Component注解，有就是Bean
                        // 解析类，判断作用域，当前bean是单例还是原型（源码中使用了懒加载）
                        // 解析类生成BeanDefinition对象
                        //
                        if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                            BeanPostProcessor instance = (BeanPostProcessor) clazz.getDeclaredConstructor().newInstance();
                            beanPostProcessorList.add(instance);
                        }

                        Component componentAnnotation = clazz.getDeclaredAnnotation(Component.class);
                        String beanName = componentAnnotation.value();

                        BeanDefinition beanDefinition = new BeanDefinition();
                        if (clazz.isAnnotationPresent(Scope.class)) {
                            Scope scopeAnnotation = clazz.getDeclaredAnnotation(Scope.class);
                            beanDefinition.setScope(scopeAnnotation.value());
                        } else {
                            // 默认是单例
                            beanDefinition.setScope("singleton");
                        }
                        beanDefinition.setClazz(clazz);
                        System.out.println(beanDefinition.getScope() + "--->" + beanDefinition.getClazz());
                        beanDefinitionMap.put(beanName, beanDefinition);
                        System.out.println(beanName);

                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    public Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getClazz();
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            // 对实例化出来的对象中属性赋值
            // 其中包括了依赖注入
            for (Field declaredField : clazz.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(Autowired.class)) {
                    Object bean = getBean(declaredField.getName());
                    declaredField.setAccessible(true);
                    declaredField.set(instance, bean);
                    System.out.println("依赖注入："+declaredField.getName());
                }
            }

            // aware回调, 可以设置bean的名字
            if (instance instanceof BeanNameAware) {
                ((BeanNameAware) instance).setBeanName(beanName);
            }
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
            }

            // 初始化，初始化bean
            if (instance instanceof InitializingBean) {
                try {
                    ((InitializingBean) instance).afterPropertiesSet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }

            return instance;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public Object getBean(String beanName) {
        if (beanDefinitionMap.containsKey(beanName)) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if (beanDefinition.getScope().equals("singleton")) {
                return singletonObjects.get(beanName);
            } else {
                // 原型bean，创建bean对象
                Object o = createBean(beanName, beanDefinition);
                return o;
            }
        } else {
            // 不存在对应的Bean
            throw new NullPointerException();
        }
    }
}
