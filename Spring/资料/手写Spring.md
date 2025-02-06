# **第1章：为什么需要 IoC 容器？**

## 1.1. 问题引入:对象创建的困境

### 1.1.1 实际问题场景

如果需要开发电商系统需要订单模块，在订单模块中需要创建用户对象、支付对象、通知对象、物流对象。

### 1.1.2 问题分析

#### 1.1.2.1 系统架构图

<img src="img\QQ截图20250128105606.png" style="zoom:80%;" />

#### 1.1.2.2 对象创建和管理问题

对象直接依赖于实现**无法切换是实现**，例如支付服务的实例可以有支付宝和微信支付，都需要具体，无法更改。进行配置的时候采用**硬编码**。

```java
问题1: 组件直接依赖具体实现
private PaymentService paymentService = new PaymentService);
// 问题2: 无法切换实现
// 如果要支持多种支付方式:
private PaymentService paymentService = new AlpayService(); // 改代码
private PaymentService paymentService = new WechatPayService(); / 改代码
// 问题3: 无法统一配置
private static final :
int PAYMENT_TIMEOUT = 3; // 硬编码
private static final string NOTIFY_TEMPLATE = "order_success";// 硬编码
```

#### 1.1.2.3 测试问题

```java
// 问题4: 无法进行单元测试
@Test
public void testCreateOrder() {
	OrderService orderService = new OrderService();
	// 无法注入mock对象
	// 无法模拟各种异常场景
	// 测试会真实调用外部服务
}
```

#### 1.1.2.4 扩展问题

```java
// 问题5: 扩展功能需要修改代码
public class OrderService {
    /增加新功能就要添加新的依赖
    private InventoryService inventoryService = new InventoryService();
    private PromotionService promotionService = new PromotionService);
    // 违反开闭原则
}
```

## 1.2 解决方案：IoC容器

### 1.2.1 设计思路

<img src="img\QQ截图20250128110251.png" style="zoom:80%;" />

### 1.2.2 核心接口设计

<img src="img\QQ截图20250128110412.png" style="zoom:70%;" />

### 1.2.3 改进后的代码

#### 1.2.3.1 定义接口

```java
public interface PaymentService { 
	void process(Order order) throws PaymentException;
	void refund(Order order) throws PaymentException;
}
public interface LogisticsService {
    void arrange(Order order) throws LogisticsException;
    DeliveryInfo getDeliveryInfo(string orderId);
}
public interface NotificationService {
    void sendOrderConfirmation(Order order) throws NotificationException;
    boolean isNotificationSent(string orderId);
}
```

#### 1.2.3.2 使用依赖注入

```java
public class OrderService {
    private final UserService userService;
    private final PaymentService paymentService;
    private final LogisticsService logisticsService;
    private final NotificationService notificationService;

    public OrderService(
            UserService userService,
            PaymentService paymentService,
            LogisticsService logisticsService,
            NotificationService notificationService
    ) {
        this.userService = Objects.requireNonNull(userService, "UserService must not be null");
        this.paymentService = Objects.requireNonNull(paymentService, "PaymentService must not be null");
        this.logisticsService = Objects.requireNonNull(logisticsService, "LogisticsService must not be null");
        this.notificationService = Objects.requireNonNull(notificationService, "NotificationService must not be null");
    }

    public Order createOrder(String userId, List<Product> products) {
        // 业务逻辑不变，但现在更容易测试和扩展
        User user = userService.getUser(userId);
        if (!userService.checkUserStatus(user)) {
            throw new IllegalStateException("User is not valid");
        }
        Order order = new Order(user, products);
        try {
            paymentService.process(order);
        } catch (PaymentException e) {
            throw new OrderException("payment failed"，e);
        }
        try {
            logisticsService.arrange(order);
        } catch (LogisticsException e) {
            paymentService.refund(order);
            throw new OrderException("Logistics arrangement failed",  e);
        }
        try {
            notificationService.sendOrderConfirmation(order);
        } catch (NotificationException e){
            log.warn("Failed to send notification for order: " + order.getId(), e);
        }
        return order;
    }
}
```

### 1.2.4 Bean创建时序图

<img src="img\QQ截图20250128111626.png" style="zoom:70%;" />

## 1.3 Mini-Spring 的 IoC 容器实现

### 1.3.1 Be3anFactory

所谓的**IoC容器**就是一个Bean工厂（**HashMap**），这个Bean工厂里面有个ConcurrentHashMap集合，该集合中存放bean对象，Bean工厂中提供方法：

- **registerBean：向容器中put一个bean对象**
- **getBean：重容器中get一个Bean对象**

### 1.3.2 ListableBeanFactory

这个接口继承了BeanFactory，它**提供了获取IoC容器中Bean信息**的功能。例如：

- Bean是否被声明在BeanDefinition容器中。`boolean containsBeanDefinition(String beanName);`
- 获取BeanDefinition的个数。`int getBeanDefinitionCount();`
- 获取所有被定义的Bean。`String[] getBeanDefinitionNames();`
- 获取所有被定义Bean的Class全类名.`String[] getBeanNamesForType(Class<?> type);`
- 根据类型获取bean实例。` <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException;`
- 获取带有指定注解的bean。`Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException;`
- 获取指定bean上的注解。`<A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) throws BeansException;`

### 1.3.3 Default Listable Bean Factory（默认的bean工厂）

**三大核心**：

1. Bean定义的存储
   - BeanDefinitionMap：存储bean名称到BeanDefinition的映射
   - BeanDefinitionNames：存储bean名称
2. 解决循环依赖的三大缓存
   - singletonObjects：一级缓存，存放完全初始化好的Bean
   - earlySingletonObjects：二级缓存，存放未填充属性的Bean
   - singletonFactories：三级缓存，存放Bean工厂对象。
3. Bean的创建流程
   - 创建Bean实例
   - 填充属性
   - 初始化Bean

## 3.4 面试题

1. Spring框架的核心主键有哪些？作用是什么
   - SpringCore：包含了控制反转（IoC容器）和依赖注入功能（DI）
   - SpringBeans：提供了BeanFactory，是工厂模式的经典实现。将对象的创建、配置和生命周期管理交给Spring（Spring管理的对象称为Bean）。
   - SpringContext：提供了一种框架式的对象访问方法，增强Spring的功能。
   - SpringJDBC：提供了JDBC的抽象层，消除繁琐的JDBC操作，提高开发效率
   - SpringAOP：提供了面向切面编程，开发者可以自定义拦截器切点，增强代码的模块化。
   - SpringWeb：Spring提供了针对Web开发的集成特性，例如上传文件、处理请求等功能，利用Servlet Listeners进行IOC容器初始化，针对Web的ApplicationContext。
   - SpringTest：未测试提供支持。

2. 解释Spring中的IoC（控制反转）原理

   Spring IoC原理可以分为四步

   - **扫描和解析配置文件或者注解信息**。
   - 将其**转化未内部的对象定义和依赖关系**
   - 根据对象定义和依赖关系，使用**反射机制动态创建和初始化对象**
   - 将对象注入到需要使用他们的地方。

   IoC的实现过程如下：

   1. 加载和解析配置文件：根据xml或者注解，将外部定义的信息转化未BeanDefinition（对象类型、构造函数参数、属性、和依赖关系等信息），将这Bean定义注册到BeanFactory容器中。

   2. 实例化Bean：更具BeanDefinition中记录的信息，利用反射机制创建Bean实例。

   3. 依赖注入和实现配置：基于配置信息将关联依赖对象注入Bean中，实现对象之间的关联。

      依赖注入可以通过以下几种方式实现：

      - **属性注入**：通过 `@Autowired`、`@Resource` 或 `@Inject` 注解，将依赖对象注入到 Bean 的属性中。
      - **构造函数注入**：通过构造函数参数，将依赖对象注入到 Bean 中。
      - **Setter 方法注入**：通过 Setter 方法，将依赖对象注入到 Bean 中。

   4. 调用Aware接口：如果Bean实现了Aware接口（如BeanNameAware或ApplicationContextAware），Spring会回调接口的方法，为Bean提供与容器相关的信息和资源。
   5. 前置处理器：初始化前会调用BeanPostProcessor的PostProcessorBeforeInitization方法。对Bean初始化前进行额外处理。
   6. 初始化：Spring检测Bean是否实现了InitializingBean接口（实现则调用afterPropertiesSet方法）和自定义的init-method，如果有这些初始化逻辑，会在此阶段执行。
   7. 后置处理器：初始化后会调用BeanPostProcessor的PostProcessorAfterInitization方法。对Bean初始化后进行额外处理。
   8. Bean就绪，此时Bean就可以被容器使用。

3. 谈谈对IoC的理解

   - IOC容器是Spring框架的核心，实现了一种基于容器管理对象机制。在Spring IoC中，对象的创建和管理交给Spring框架。开发者只需要声明使用的对象和依赖关系，无需关心细节。
   - 在IoC中，容器通过解析配置文件和注解信息，自动创建和管理对象之间的依赖关系。程序员只需要声明所需的对象和依赖关系，通过容器提供的注入方式获取这些对象，从而避免硬编码和强耦合问题。
   - IoC容器**实现机制是依赖注入**。Spring支持多种依赖注入方式，包括构造函数注入、Setter方法注入、字段注入等。

   总结：SpringIOC提供了一种松耦合、可重用、可维护的编程模式，提供了应用程序员的开发效率和代码质量。开发者专注于业务逻辑实现，无需关注底层对象的创建和依赖管理。

# 第2章：Bean的定义与注册

## 2.1 问题引入：简单容器的不足

在1.3中我们创建了简单容器，基于全局共享的HashMap。需要什么对象就创建它然后加入put进HashMap中。

存在以下问题，在项目需要支持多种支付方式的场景下，不同方式需要不同配置：

1. **配置写死：在代码中需要为不同支付方式创建不同对象，具体实现在代码中写死。**
2. **配置分散：每个服务的配置分散在代码中，不利于管理。**
3. **运行时切换：无法在运行时动态切换实现**
4. **依赖管理：无法管理服务之间的依赖关系。**

需要解决的关键问题有：

- **如何描述Bean**：类的信息、配置信息、依赖关系
- **如何管理Bean定义**：统一注册机制、运行时切换、依赖分析和注入

## 2.2 解决方案BeanDefinition

### 2.2.1 核心思路

Spring利用了2步解决

1. **设计BeanDefinition接口来描述Bean**：

   BeanDefinition中包含了Bean的完整信息、支持配置注入、支持作用域管理

2. **设计BeanDefinitionRegistry接口管理Bean定义**：

   注册Bean定义、移除Bean定义、获取Bean定义

### 2.2.2 整体设计

**类图**

<img src="img\微信截图_20250206161335.png" style="zoom:70%;" />

**时序图**

<img src="img\微信截图_20250206161615.png" style="zoom:70%;" />

## 2.3 具体实现

### 2.3.1 准备接口

**BeanDefinition接口**需要哪些东西：

1. 作用域
2. 类的基本信息
3. 是否延迟加载
4. 声明周期方法（初始化方法和销毁方法）
5. 属性值

**BeanDefinitionRegistry接口（用于注册BeanDefinition）**需要什么呢？

1. `void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeansException;`
2. `void removeBeanDefinition(String beanName) throws BeansException;`
3. `BeanDefinition getBeanDefinition(String beanName) throws BeansException;`
4. `boolean containsBeanDefinition(String beanName);`
5. `String[] getBeanDefinitionNames();`
6. `int getBeanDefinitionCount();`

​	**实际上就是BeanDefinition的增删改查**。

### 2.3.2 实现类

#### 2.3.2.1 BeanDefinition实现类

例如扫描包和依赖注入时候生产bean对象，都需要用到这个实现类。

实现类中定义了，**beanClass、scope作用域、是否懒加载、构造函数的参数列表和具体值等信息**。以及一些方法，例如get，set，add方法

```java
public class GenericBeanDefinition implements BeanDefinition {
    
    private Class<?> beanClass;
    private String scope = SCOPE_SINGLETON;
    private boolean lazyInit = false;
    private String initMethodName;
    private String destroyMethodName;
    private PropertyValues propertyValues = new PropertyValues();
    private final List<ConstructorArgumentValue> constructorArgumentValues = new ArrayList<>();
    
    public GenericBeanDefinition() {
    }
    
    public GenericBeanDefinition(Class<?> beanClass) {
        this.beanClass = beanClass;
    }
}
```

#### 2.3.2.2 注册中心

将注册BeanDefinition的任务交给IoC容器。

- **IoC 容器**是 Spring 实现控制反转的核心组件，它通过依赖注入（DI）来管理对象的创建和依赖关系。
- **Bean 工厂**（`BeanFactory`）是 IoC 容器的基础实现，负责 Bean 的创建、组装和管理。
- **`ApplicationContext`** 是 `BeanFactory` 的扩展，提供了更多企业级功能（如 AOP、事件机制等）。
- **Bean 的生命周期**由 IoC 容器管理，包括实例化、属性赋值、初始化、使用和销毁。

```java
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
        implements ConfigurableListableBeanFactory, BeanDefinitionRegistry {
    // 存放定义beanDefinition的集合
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
    private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeansException {
        // .. 一些细节 ... 
        this.beanDefinitionMap.put(beanName, beanDefinition);
        // 如果是新的bean定义，添加到名称列表重
        if (!this.beanDefinitionNames.contains(beanName)) {
            this.beanDefinitionNames.add(beanName);
        }
    }
    // 其他方法
}
```

## 2.4 面试题

1. 什么是BeanDefinition？

   BeanDefinition定义了Bean的配置信息，用于描述Bean的创建和配置方式。

   **优点实现配置和实例化的解耦！**

2. BeanDefinition包含了哪些信息？

   作用域（单例/原型？）、是否懒加载、Bean的类名、构造参数、属性值、生命周期回调方法（初始化、销毁方法）

3. 为什么需要BeanDefinitionRegistry？

   **统一管理Bean定义、支持运行时注册、实现配置的集中管理**。

# 第3章：Bean的生命周期管理

## 3.1 问题引入

### 3.1.1 初始化问题

- **Bean创建后需要进行必要的初始化**

- **需要验证必要的配置**

- **需要建立资源连接**

例如在注入一个数据库连接的Connection对象的场景中：

- Bean对象创建后，需要初始化用户名、密码、端口号、IP地址。

- 并且需要验证，密码是否初始化了，IP地址是否有配置。

- 如何就算建立连接资源

- 

### 3.1.2 销毁问题

- **资源未能及时释放**
- **连接未能正确关闭**
- **临时文件未清理**

**这些问题可能导致OOM！**

### 3.1.3 管理问题

- **Bean的生命周期缺乏管理**
- **初始化和销毁的执行时机不明确**
- **缺乏统一的生命周期扩展机制**

## 3.2 Bean的生命周期管理

为了解决上面的问题，**Spring将Bean生命周期的管理交给了IoC容器。**

<img src=".\img\QQ截图20250102152500.png" style="zoom:70%;" />

<img src=".\img\QQ20250206-173846.png" style="zoom:70%;" />

首先需要调用createBeanInstance实例化bean对象。

BeanDefinition是一个抽象类，里面有存储了**bean作用域**（单例/原型bean）、**是否支持懒加载**、**初始化方法名**、**销毁方法名**、**属性值对象**（一个属性值的ArrayList）、**构造函数参数值列表**、**Bean的Class对象**、**Bean的名字**。

**属性值对象**：ArrayList里面存储了多个PropertyValue，PropertyValue包括了属性名字（String），属性的值（Object），属性的类型（Class<?>）。

**构造函数参数值列表**：List<ConstructorArgumentValue>，其中ConstructorArgumentValue里面类似于PropertyValue，它里面包括了属性名字（String），属性的值（Object），属性的类型（Class<?>）。

```java
protected Object createBean(String beanName, BeanDefinition beanDefinition) {
    // 1. 创建Bean实例
    Object bean = createBeanInstance(beanDefinition);
    // 2. 填充属性
    populateBean(beanName, bean, beanDefinition);
    // 3. 初始化Bean
    return initializeBean(beanName, bean, beanDefinition);
}
```

### **3.2.1 构造方法**

**createBeanInstance方法的程序流程图：**

- 首先获取beanDefinition中的Bean的Class对象。

- 如果beanDefinition中构造函数参数值列表为**空**，通过调用无参构造方法获得一个实例。

  beanClass.getDeclaredConstructor().newInstance();

- 构造函数参数值列表**不为空**，通过调用autowireConstructor方法获得一个实例。
  - 获取beanDefinition中的**参数列表argumentValues**。
  - 获取去beanDefinition对应类的所有构造方法。
  - 根据参数列表找到匹配的构造方法，利用该构造方法传参创建bean对象。

- **Bean实例创建完成**。

### 3.2.2 属性填充（包含依赖注入）

**填充属性（将beanDefinition.getPropertyValues()的属性值填充到实例中）**

- 在官方的Spring框架中主要通过 **XML 配置** 或 **Java 配置**（`@Bean` 方法中的 `PropertyValues`）进行注入。

- 获取beanDefinition的属性值列表PropertyValues
- 取出属性名拼接成set方法名称，获取bean实例中同名的set方法。
- 调用set方法中setter.invoke(bean, value)方法，进行属性填充。

<img src=".\img\bean属性填充流程图.png" style="zoom:90%;" />

### **3.2.3 执行Aware接口方法**

```java
if (bean instanceof Aware) {
    if (bean instanceof BeanFactoryAware) {
        ((BeanFactoryAware) bean).setBeanFactory(this);
    }
    if (bean instanceof BeanNameAware) {
        ((BeanNameAware) bean).setBeanName(beanName);s
    }
}
```

`Aware` 是 Spring 框架提供的一个 **标记接口（Marker Interface）**，它的作用是让 Bean **感知** 到 Spring 容器中的某些对象或信息，如 `BeanFactory`、`ApplicationContext` 等。

Spring 提供了一系列 `Aware` 接口，使 Bean **可以感知并获取 Spring 容器中的一些核心组件**，例如：

- `BeanFactoryAware` —— 获取 `BeanFactory`（可用于**手动获取其他 Bean**）。
- `BeanNameAware` —— 获取当前 Bean 的名称。
- `ApplicationContextAware` —— 获取 `ApplicationContext`（可以操作整个 Spring 容器）。

### **3.2.4 执行Bean Post Processor #Before**

```java
Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
```

IoC容器中有个BeanPostProcessor列表，列表中每个BeanPostProcessor对象包含了postProcessBeforeInitialization前置处理方法和postProcessAfterInitialization后置处理方法.

将执行Aware接口中set方法的bean对象进行加工，通过利用BeanPostProcessor列表中的每一个BeforeInitialization方法对bean对象进行加工得到代理对象然后返回。

如果没有BeanPostProcessor则返回原本的bean对象。

```java
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
```

### 3.2.5 执行bean的初始化方法

```java
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
```

- **执行 `InitializingBean` 接口的方法**

  - Bean对象一般会继承InitializingBean接口，其中包含了**`afterPropertiesSet()`**：这个方法通常用于执行一些初始化逻辑，比如检查依赖是否注入完成、初始化一些资源等。

- **执行自定义的 `init-method`**

  - **`init-method`**：除了实现`InitializingBean`接口，Spring还允许通过配置指定一个自定义的初始化方法。这个方法可以在Bean的配置中通过`init-method`属性指定。

  - **`getInitMethodName()`**：从`BeanDefinition`中获取配置的`init-method`名称。

  - **`StringUtils.hasText(initMethodName)`**：检查`initMethodName`是否为空或只包含空白字符。如果`initMethodName`有效，则继续执行。

  - **`getMethod(initMethodName)`**：通过反射获取Bean类中名为`initMethodName`的方法。

  - **`initMethod.invoke(bean)`**：调用这个初始化方法，执行自定义的初始化逻辑。

### 3.2.6 执行Bean Post Processor #After

类似于Bean Post Processor #Before，**对bean对象进行加工返回代理对象**。

```java
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
```

## 3.3 面试题解析

1. Spring容器是如何管理Bean的生命周期？包括哪些阶段

   实例化：创建Bean实例

   属性赋值：设置属性值

   Aware接口回调：注入容器相关依赖

   初始化：执行初始化方法

   使用：正常使用Bean

   销毁：执行销毁方法

2. **Aware接口作用是什么？**

   ```java
   ((BeanFactoryAware) bean).setBeanFactory(this);
   // 这个this就是DefaultListableBeanFactory对象。
   ```

   **让Bean获得容器的功能**

   **获取Bean的名字**

   **获取Bean所在的工厂**

   **实现容器和Bean的解耦**

3. InitializingBean和DisposbleBean的作用？

   InitializingBean：属性设置完成后初始化

   DisposableBean：Bean销毁前的清理工作

   统一管理Bean的生命周期回调

# 第4章：依赖注入的实现

## 4.1 问题引入和分析

```java
public class Test{
    // 直接new创建依赖对象
    private A a = new A();
    private B b = new B();
    public void test1(String name) {
        a.setName(name);
    }
}
```

上面代码存在的问题：

- 硬编码依赖
- 依赖管理困难
- 测试不方便：无法方便的替换mock对象（测试对象的依赖对象）
- 扩展受限

**问题分析**：

- 依赖创建问题：
  - 谁赋值创建依赖对象
  - 何时创建依赖对象
  - 如何管理依赖对象的生命周期
- 依赖注入问题：
  - 如何将依赖对象注入到目标Bean
  - 支持哪些注入方式
  - 如何处理依赖和递归依赖
- 依赖配置问题
  - 如何描述Bean之间的依赖关系
  - 如何支持不同环境的配置
  - 如何处理依赖的可选性

## 4.2 依赖注入系统

### 4.2.1 设计思路

1. 依赖注入方式：
   - 构造器注入：通过构造函数注入依赖
   - 属性注入：通过setter方法注入依赖
   - 字段注入：直接注入字段值
2. 依赖描述方式
   - 引用注入：注入容器中的其他Bean
   - 值注入：注入基本类型或字符串
   - 集合注入：注入数组、列表集合类型
3. 类型转换支持
   - 基本类型转换
   - 字符串到复杂对象的转换
   - 集合类型的转换

### 4.2.2 核心接口设计

为了**定义属性值和Bean引用**，设计了**PropertyValues和BeanReference**类。

依赖注入流程图

<img src=".\img\QQ20250206-181953.png" style="zoom:90%;" />

### 4.2.3 Abstract Autowire Capable Bean Factory

`AbstractAutowireCapableBeanFactory` 是 Spring 框架中一个核心的抽象类，属于 `BeanFactory` 体系结构的一部分。它是 Spring IoC 容器实现 Bean 的创建、依赖注入（自动装配）和初始化的核心基类。以下是对它的简要介绍：

1. **实例化 Bean**

   - ```java
     Object createBeanInstance(BeanDefinition beanDefinition)
     ```

   - 支持通过构造函数、工厂方法或反射创建 Bean 实例。
   - 处理构造函数的循环依赖问题（通过提前暴露对象引用）。

2. **属性填充（依赖注入）**

   - ```java
     void populateBean(String beanName, Object bean, BeanDefinition beanDefinition)
     ```

   - 自动注入依赖的 Bean（通过 XML 配置或注解）。
   - 处理 `@Autowired`、`@Resource`、`@Value` 等注解。

3. **销毁**

   - ```java
     void destroySingletons()
     ```

   - 调用销毁方法（如 `destroy-method`、`@PreDestroy`）。

   - ```java
     /**
      * registerDisposableBean用来注册需要销毁的bean对象
      * disposableBeans容器用来存储待销毁的bean对象
      * destroySingletons是销毁的函数。
      */
     protected void registerDisposableBean(String beanName, DisposableBean bean) {
         disposableBeans.put(beanName, bean);
     }
     
     public void destroySingletons() {
         synchronized (this.disposableBeans) {
             for (Map.Entry<String, DisposableBean> entry : disposableBeans.entrySet()) {
                 try {
                     entry.getValue().destroy();
                     logger.debug("Invoked destroy-method of bean '{}'", entry.getKey());
                 } catch (Exception e) {
                     logger.error("Error destroying bean '{}'", entry.getKey(), e);
                 }
             }
             disposableBeans.clear();
         }
     }
     ```

     

4. **扩展点**

   - 通过 `BeanPostProcessor` 接口支持自定义逻辑（如 AOP 代理的生成）。

### 4.2.4 Singleton Bean Registry

SingletonBeanRegistry是一个接口，**定义了方法对单例bean进行注册**。

```java
public interface SingletonBeanRegistry {

    /**
     * 注册一个单例bean
     *
     * @param beanName bean名称
     * @param singletonObject bean实例
     */
    void registerSingleton(String beanName, Object singletonObject);

    /**
     * 获取单例bean
     *
     * @param beanName bean名称
     * @return bean实例，如果不存在返回null
     */
    Object getSingleton(String beanName);

    /**
     * 判断是否包含指定名称的单例bean
     *
     * @param beanName bean名称
     * @return 如果包含返回true，否则返回false
     */
    boolean containsSingleton(String beanName);

    /**
     * 获取所有单例bean的名称
     *
     * @return 单例bean名称数组
     */
    String[] getSingletonNames();

    /**
     * 获取单例bean的数量
     *
     * @return 单例bean的数量
     */
    int getSingletonCount();
} 
```