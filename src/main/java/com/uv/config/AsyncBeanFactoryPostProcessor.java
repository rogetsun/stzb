package com.uv.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * @author uvsun 2020/3/15 11:21 下午
 * 想要在当前类的内部获取当前类的代理类 需要调用:
 * AopContext.currentProxy()
 * 这样就需要设置代理的 expose-proxy=true,
 * 但是springboot 针对
 * @Async 注解的方法 设置
 * @EnableAspectJAutoProxy(exposeProxy = true)
 * 没有用, 所以增加本类 自定义生成Bean后的处理器 在其中设置 exposeProxy属性true即可
 */
@Component
public class AsyncBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(org.springframework.scheduling.config.TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME);
        beanDefinition.getPropertyValues().add("exposeProxy", true);
    }
}
