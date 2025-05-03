package com.swy.common.util;

import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.util.Collection;
import java.util.Locale;

/**
 * spring上下文
 *
 * @author zyh
 * @date 2022/3/7
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {
    @Getter
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext con) throws BeansException {
        SpringContextUtil.context = con;
    }

    public static Object getBean(String beanName) {
        return context.getBean(beanName);
    }

    public static <T> T getBean(Class<T> tClass) {
        return context.getBean(tClass);
    }

    public static <T> Collection<T> getBeanCollection(Class<T> tClass) {
        return context.getBeansOfType(tClass).values();
    }

    public static String getMessage(String key) {
        return context.getMessage(key, null, Locale.getDefault());
    }
}
