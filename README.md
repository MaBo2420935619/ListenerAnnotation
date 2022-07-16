# Listener
使用两个注解，三步完成SpringBoot事件监听（反射，切面实现）



# 一、前言
当某个事件需要被监听的时候，我们需要去做其他的事前，最简单的方式就是将自己的业务 方法追加到该事件之后。
但是当有N多个这样的需求的时候我们都这样一个个去添加修改事件的源码吗？
**这篇文章将告诉你如何用一个注解，就可以将你的业务代码通过切面的方式添加到事件的前后，而不需要修改事件的代码**
# 二、三步编写监听
## 1.创建事件

> 创建事件，并在事件方法上使用注解@AddEvent("")，参数是事件 id

```java
@Component
@Data
public class Event {
    /**
     * @Description : 如果该方法被调用了，则AddEventListener修饰的方法也执行
     */
    @AddEvent("eventId")
    public String event(String s){
        System.out.println("addEvent执行,参数:"+s);
        return s;
    }
}
```
## 2.编写监听方法

> 编写监听方法，监听方法上必须用@AddEventListener("")主机标注，value参数必须和事件 id一致，才知道当前方法监听的是哪个事件
> 第二个参数是用来指定当前方法和事件的执行先后顺序，有EventType.AFTEREVENT和EventType.BEFOREEVENT

```java
package com.mabo.listener;

import com.mabo.utils.AddEventListener;
import com.mabo.utils.EventType;
import org.springframework.stereotype.Component;

@Component
public class EventListener {
    @AddEventListener(value = "eventId",position = EventType.AFTEREVENT)
    public void test1(String s) {
        System.out.println("test1执行成功，参数:"+s);
    }
}

```
## 3.引入工具utils包下的所有文件

> 第三步把图片中的java文件全部引入到项目中就可以启动项目了，由于文件太多，代码粘贴到文章末尾
> utils包内放的都是监听的核心源码，有兴趣可以修改和学习

![在这里插入图片描述](https://img-blog.csdnimg.cn/c225116556b740f1b70dc054f0a7ab33.png)

# 三、 效果图
如下图所示，add方法内并没有调用test方法，但是可以通过@AddEvent和AddEventListener两个注解配合使用就可以实现监听event方法
当event方法被调用 test1方法自动执行。

**调用事件**

```java
package com.mabo.controller;
import com.mabo.event.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("addEvent")
public class AddEventController {
    @Autowired
    private Event event;
    // 浏览器输入下方地址即可测试
     // http://localhost:8099/addEvent/add
    @RequestMapping("add")
    public String add(){
        String test = event.event("测试");
        return test;
    }
}

```

**调用事件的执行结果**
![在这里插入图片描述](https://img-blog.csdnimg.cn/e180fbc6a49d4437bd4f02a828476b77.png)

# 四、监听原理
该方法是利用切面、注解、反射来实现SpringBoot的事件监听的
## 1.通过Aspect的切面，切入事件方法
首先使用Aspec的Around（也可以用before或者after，但是比较麻烦）注解，切入Event的方法中，around注解的方法中，可以在事件方法的执行前后添加业务代码。但是我们不直接加入需要添加的业务，进入第二步骤。
## 2.利用反射获取被AddEvent注解的方法
切入后，先获取所有的Bean，然后获取被AddEvent注解的Bean，获取Bean被AddEventListener注解的方法，如果AddEventListener的value值事件注解AddEvent的value值相等，则执行该方法，则监听执行成功。

```java
if (vent.value().equals(annotation.value())){
    method.invoke(bean, args);                 
}
```
## 注意（非常重要）

- 事件的类必须是bean，否则切面失败。
- 监听方法和（被监听方法）事件方法的参数数量，类型，顺序必须一致，否则可能导致反射执行方法失败


# 五、 Github源码地址
Github项目地址



# 六、 工具utils包下代码
##  核心源码 AddEventAspect

```java
package com.mabo.utils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description : 定义自定义注解的切面
 * @Author : mabo
 */
@Component
@Aspect
public class AddEventAspect {
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * @Description : 使用Around可以修改方法的参数，返回值，
     * 甚至不执行原来的方法,但是原来的方法不执行会导致before和after注解的内容不执行
     * 通过around给原方法赋给参数
     */
    @Around("@annotation(event)")
    public Object addEventListener(ProceedingJoinPoint joinPoint, AddEvent event) throws Throwable {
        Object[] args = joinPoint.getArgs();
        //存储需要在方法执行之后再执行的类
        List<Method> afterEventMethod = new ArrayList<>();
        //反射获取AddEventListener修饰的方法并执行
        //获取所有bean
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String beanName: beanDefinitionNames) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> aClass = bean.getClass();
            String name = aClass.getName();
            //aop切面会导致方法注解丢失，在这里处理获取原类名
            if (name.contains("$$")){
                String[] names = name.split("\\$\\$");
                name=names[0];
                aClass = Class.forName(name);
            }
            Method[] methods = aClass.getMethods();
            for (Method method : methods) {
                //获取指定方法上的注解的属性
                AddEventListener annotation = method.getAnnotation(AddEventListener.class);
                if (annotation!=null&&event.value().equals(annotation.value())){
                    //执行所有的注解了该类的方法
                    EventType position = annotation.position();
                    if (position.equals(EventType.BEFOREEVENT)){
                        method.invoke(bean, args);
                    }else{
                        afterEventMethod.add(method);
                    }
                }
            }
        }
        //执行被切面的方法
        Object proceed = joinPoint.proceed(args);
        //执行需要在方法执行之后再执行的方法
        for (Method method : afterEventMethod) {
            Class<?> aClass = method.getDeclaringClass();
            Object o = aClass.newInstance();
            method.invoke(o, args);
        }
        return proceed;
    }
}
```
## EventType枚举

```java
package com.mabo.utils;
/**
 * @Author mabo
 * @Description   事件执行前后的两种枚举值
 */

public enum EventType {
    AFTEREVENT,BEFOREEVENT
}


```

## AddEvent事件上的注解

```java
package com.mabo.utils;
/**
 * @Description : 在当前修饰的方法前后执行其他的方法
 * @Author : mabo
*/

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface AddEvent {
    String value();
}

```

## AddEventListener监听注解

```java
package com.mabo.utils;
/**
 * @Description : 在当前修饰的方法前后执行其他的方法
 * @Author : mabo
*/
import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface AddEventListener {
    String value() ;
    EventType position() default EventType.BEFOREEVENT;
}

```
