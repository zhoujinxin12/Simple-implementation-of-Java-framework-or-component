# 1. AOP原理

<img src="img\QQ截图20250119003452.png" style="zoom:80%;" />

| 涉及问题                     | 对应的概念                                                   | 补充说明                                                     |
| ---------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 在哪里进行再加工where        | 1. JoinPoint：AOP框架支持的选择类型。  2. PointCut：在JoinPoint范围内，用户实际选择的范围类似正则表达式，核心是一个匹配条件，比如com.ttttt.service包下的公共方法/有@Xxx注解等。 | 不同的AOP框架至此的选择类型是不一样的。1. Spring AOP框架，只支方法和类。 |
| 进行什么样的再加工How        | 1.Advice：对方法进行再加工，额外加入一些逻辑。2. Introduction：对类进行再加工，让类实现额外的接口 | Advice分为before/after/around 3种。1. before：在目标方法执行前加入一些逻辑。2. after：在目标方法执行后加入一些逻辑。3.around：可以替换掉整个目标方法。 |
| 组合上面2个问题（where+how） | Aspect是一个简单的容器，把where和how放在一起。               |                                                              |

**再看Spring是如何实现AOP的**

**运行时代理**

前面讲了AOP的核心原理就是额外增加了一道工序-- 源代码再加工

AspectJ实现: 待加工的源码--> AspectJ综译器-->javac-->java。

Spring AOP实现: 待加工的源码-->javac -->java --> Spring启动时生成代理对象

Spring AOP是通过运行时代理的方式实现AOP的，再加工的时机和AspectJ不同

但只要是在实际处理用户请求前完成再加工就可以

**具体实现细节**

1. BeanDefinitionRegistryPostProcessor回调（ConfigurationClassPostProcessor）

   注入AnnotationAwareAspectJAutoProxyCreator这个BeanPostProcessor

2. BeanPostProcessor回调（AnnotationAwareAspectJAutoProxyCreator）

   根据Aspect定义，生成Advisor对象，放入bean池备用。

   - 遍历Bean池中的BeanDefinition，根据类上是否有@Aspect注解筛选出切面。
   - 更具切面，生成Advisor对象，每个Advisor包含一个PointCut和一个Advice，所以一个切面可能会生成多个Advisor

   判断当前bean是否需要植入AOP相关逻辑

   遍历bean池中的Advisor对象，根据Advisor中的PointCut判断当前Bean是否满足条件。

   如果满足条件，创建相应的代理对象

   代理对象中，会加入AOP相关逻辑。

   将创建好的代理对象注册到bean池中。