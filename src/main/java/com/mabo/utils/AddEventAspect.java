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
