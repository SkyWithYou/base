package com.swy.common.component.service;

import com.swy.common.util.SpringContextUtil;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 *
 * @author SkyWithYou
 */
@Component
public class ServiceLifeManager implements ApplicationListener<ApplicationReadyEvent> {

    private Map<Class<? extends AbstractService>, AbstractService> serviceMap;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        serviceMap = SpringContextUtil.getBeanCollection(AbstractService.class).stream().collect(Collectors.toMap(AbstractService::getClass, service -> service));
    }

    @PreDestroy
    private void shutdown() {

    }
}
