package com.swy.common.component.service;

import com.google.common.collect.Lists;
import com.swy.common.util.SpringContextUtil;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 服务生命周期管理器
 * <p>
 * 负责管理所有AbstractService实例的生命周期，包括：
 * - 在应用启动时收集所有服务实例
 * - 在应用关闭时按优先级顺序关闭所有服务
 * - 提供异常处理机制，防止单个服务故障影响整体系统
 * </p>
 * 
 * @author SkyWithYou
 */
@Slf4j
@Component
public class ServiceLifeManager implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * 服务实例映射表，用于存储所有服务实例
     */
    private Map<Class<? extends AbstractService>, AbstractService> serviceMap;

    /**
     * 应用就绪事件处理
     * <p>
     * 在Spring应用上下文完全初始化后触发
     * 收集所有AbstractService实例并存储到serviceMap中
     * </p>
     *
     * @param event 应用就绪事件
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        serviceMap = SpringContextUtil.getBeanCollection(AbstractService.class).stream()
                .collect(Collectors.toMap(
                        AbstractService::getClass,
                        service -> service,
                        (o1, o2) -> o2,
                        ConcurrentHashMap::new));

        // 初始化并启动所有服务
        initializeAndStartServices();
        
        log.info("服务生命周期管理器已初始化，共加载{}个服务", serviceMap.size());
    }
    
    /**
     * 初始化并启动所有服务
     * <p>
     * 按优先级顺序（优先级高的先启动）初始化和启动所有服务
     * 单个服务初始化或启动失败不会影响其他服务
     * </p>
     */
    private void initializeAndStartServices() {
        if (serviceMap == null || serviceMap.isEmpty()) {
            log.info("没有需要初始化的服务");
            return;
        }
        
        log.info("开始初始化并启动所有服务，共{}个服务", serviceMap.size());
        
        // 将服务按优先级排序（优先级高的先启动）
        List<AbstractService> sortedServices = Lists.newArrayList(serviceMap.values());
        Collections.sort(sortedServices);
        
        // 按顺序初始化所有服务
        for (AbstractService service : sortedServices) {
            String serviceName = service.getServiceName();
            
            // 初始化服务
            try {
                if (!service.isInitialized()) {
                    log.info("正在初始化服务: {}", serviceName);
                    service.initialize();
                    log.info("服务初始化完成: {}", serviceName);
                } else {
                    log.info("服务已初始化，跳过初始化步骤: {}", serviceName);
                }
            } catch (Exception e) {
                log.error("服务初始化失败: {}, 错误: {}", serviceName, e.getMessage(), e);
                // 初始化失败的服务跳过启动步骤
                continue;
            }
        }

        // 按顺序启动所有服务
        for (AbstractService service : sortedServices) {
            String serviceName = service.getServiceName();
            
            // 只启动已成功初始化的服务
            if (service.isInitialized()) {
                try {
                    if (!service.isRunning()) {
                        log.info("正在启动服务: {}", serviceName);
                        service.start();
                        log.info("服务启动完成: {}", serviceName);
                    } else {
                        log.info("服务已在运行状态，跳过启动步骤: {}", serviceName);
                    }
                } catch (Exception e) {
                    log.error("服务启动失败: {}, 错误: {}", serviceName, e.getMessage(), e);
                    // 启动失败不影响其他服务
                }
            } else {
                log.warn("服务未初始化，无法启动: {}", serviceName);
            }
        }
        
        log.info("所有服务初始化和启动处理完成");
    }

    /**
     * 应用关闭时的服务关闭处理
     * <p>
     * 按优先级顺序关闭所有服务
     * 优先级高的服务后关闭，确保依赖关系正确处理
     * 单个服务关闭失败不会影响其他服务的关闭
     * </p>
     */
    @PreDestroy
    private void shutdown() {
        if (serviceMap == null || serviceMap.isEmpty()) {
            log.info("没有需要关闭的服务");
            return;
        }
        
        log.info("开始关闭所有服务，共{}个服务需要关闭", serviceMap.size());
        
        // 将服务按优先级排序（优先级低的后关闭）
        List<AbstractService> sortedServices = new ArrayList<>(serviceMap.values());
        sortedServices.sort(Collections.reverseOrder());
        
        // 第一步：按顺序停止所有服务
        log.info("第一阶段：停止所有服务");
        for (AbstractService service : sortedServices) {
            try {
                String serviceName = service.getServiceName();
                log.info("正在停止服务: {}", serviceName);
                
                if (service.isRunning()) {
                    service.stop();
                    log.info("服务已停止: {}", serviceName);
                } else {
                    log.info("服务不在运行状态，无需停止: {}, 当前状态: {}", serviceName, service.getState());
                }
            } catch (Exception e) {
                log.error("停止服务时发生异常: {}, 错误: {}", service.getServiceName(), e.getMessage(), e);
                // 继续停止其他服务，不中断流程
            }
        }
        
        // 第二步：按顺序销毁所有服务
        log.info("第二阶段：销毁所有服务");
        for (AbstractService service : sortedServices) {
            try {
                String serviceName = service.getServiceName();
                log.info("正在销毁服务: {}", serviceName);
                service.destroy();
                log.info("服务已销毁: {}", serviceName);
            } catch (Exception e) {
                log.error("销毁服务时发生异常: {}, 错误: {}", service.getServiceName(), e.getMessage(), e);
                // 继续销毁其他服务，不中断流程
            }
        }
        
        log.info("所有服务已关闭");
    }
}
