package com.swy.common.component.entity;

import com.google.common.collect.Maps;
import com.swy.common.component.BaseComponent;
import com.swy.common.component.lifecycle.AbstractLifecycleBase;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 抽象生命周期实体类
 * <p>
 * 继承自AbstractLifecycleBase，为实体提供生命周期管理能力
 * 提供组件管理功能，支持组件的添加、获取和生命周期同步
 * </p>
 *
 * @author SkyWithYou
 */
@Slf4j
public abstract class AbstractEntity<C extends BaseComponent> extends AbstractLifecycleBase {

    /**
     * 组件映射表，使用ConcurrentHashMap保证线程安全
     * 使用泛型参数提供更好的类型安全
     */
    private final Map<Class<? extends C>, C> componentMap = Maps.newConcurrentMap();

    /**
     * 获取实体ID
     * <p>
     * 返回实体的唯一标识符
     * </p>
     *
     * @return 实体的唯一ID
     */
    public abstract long getEntityId();

    /**
     * 添加组件
     * <p>
     * 向实体中添加指定类型的组件，如果同类型组件已存在则会覆盖
     * 添加过程中会自动初始化并启动组件
     * </p>
     *
     * @param component 要添加的组件，不能为null
     * @return 添加的组件实例，如果添加失败则返回原组件实例
     */
    public C addComponent(C component) {
        if (component != null) {
            @SuppressWarnings("unchecked")
            Class<? extends C> componentClass = (Class<? extends C>) component.getClass();
            if (componentMap.containsKey(componentClass)) {
                log.warn("组件已存在，将覆盖之前的组件: {}", componentClass.getName());
                // 移除旧组件并释放资源
                removeComponent(componentClass);
            }

            try {
                component.initialize();
                component.start();
                componentMap.put(componentClass, component);
                return component;
            } catch (Exception e) {
                log.error("组件初始化或启动失败: {}, 错误: {}", componentClass.getName(), e.getMessage());
                try {
                    // 如果组件已初始化但启动失败，需要清理资源
                    if (component.isInitialized()) {
                        component.stop();
                        component.destroy();
                    }
                } catch (Exception ex) {
                    log.error("组件清理资源失败: {}, 错误: {}", componentClass.getName(), ex.getMessage());
                }
            }
        }
        return component;
    }

    /**
     * 获取指定类型的组件
     * <p>
     * 根据组件类型从实体中获取对应的组件实例
     * </p>
     *
     * @param componentClass 要获取的组件类型
     * @param <T>            组件类型参数
     * @return 组件实例，如果不存在返回null
     */
    public C getComponent(Class<? extends C> componentClass) {
        return componentMap.get(componentClass);
    }

    /**
     * 移除指定类型的组件
     * <p>
     * 从实体中移除指定类型的组件，并停止和销毁该组件
     * 移除过程中会自动调用组件的stop和destroy方法释放资源
     * </p>
     *
     * @param componentClass 要移除的组件类型
     * @param <T>            组件类型参数
     * @return 被移除的组件实例，如果不存在返回null
     */
    public C removeComponent(Class<? extends C> componentClass) {
        if (componentMap.containsKey(componentClass)) {
            C component = componentMap.remove(componentClass);
            try {
                component.stop();
                component.destroy();
                return component;
            } catch (Exception e) {
                log.error("组件停止或销毁失败: {}, 错误: {}", component.getClass().getName(), e.getMessage());
                return component;
            }
        }
        return null;
    }

    /**
     * 执行实体初始化操作
     * <p>
     * 调用父类初始化方法，然后初始化实体中的所有组件
     * 组件初始化过程中的异常会被捕获并记录，不会影响其他组件的初始化
     * </p>
     *
     * @throws Exception 初始化过程中可能发生的异常
     */
    @Override
    protected void doInitialize() throws Exception {
        super.doInitialize();
        // 初始化所有组件，按优先级排序（优先级值小的先初始化）
        List<BaseComponent> sortedComponents = new ArrayList<>(componentMap.values());
        Collections.sort(sortedComponents);

        // 使用try-catch处理每个组件的异常
        for (BaseComponent component : sortedComponents) {
            try {
                component.initialize();
            } catch (Exception e) {
                // 记录异常但继续执行其他组件的初始化
                log.error("组件初始化失败: {}, 错误: {}", component.getClass().getName(), e.getMessage());
            }
        }
    }

    /**
     * 执行实体启动操作
     * <p>
     * 调用父类启动方法，然后启动实体中的所有组件
     * 组件启动过程中的异常会被捕获并记录，不会影响其他组件的启动
     * </p>
     *
     * @throws Exception 启动过程中可能发生的异常
     */
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        // 启动所有组件，按优先级排序（优先级值小的先启动）
        List<BaseComponent> sortedComponents = new ArrayList<>(componentMap.values());
        Collections.sort(sortedComponents);

        // 使用try-catch处理每个组件的异常
        for (BaseComponent component : sortedComponents) {
            try {
                component.start();
            } catch (Exception e) {
                // 记录异常但继续执行其他组件的启动
                log.error("组件启动失败: {}, 错误: {}", component.getClass().getName(), e.getMessage());
            }
        }
    }

    /**
     * 执行实体暂停操作
     * <p>
     * 暂停实体中的所有组件，然后调用父类暂停方法
     * 组件暂停过程中的异常会被捕获并记录，不会影响其他组件的暂停
     * </p>
     *
     * @throws Exception 暂停过程中可能发生的异常
     */
    @Override
    protected void doPause() throws Exception {
        // 暂停所有组件，按优先级逆序排序（优先级值大的先暂停）
        List<BaseComponent> sortedComponents = new ArrayList<>(componentMap.values());
        Collections.sort(sortedComponents, Collections.reverseOrder());

        // 使用try-catch处理每个组件的异常
        for (BaseComponent component : sortedComponents) {
            try {
                component.pause();
            } catch (Exception e) {
                // 记录异常但继续执行其他组件的暂停
                log.error("组件暂停失败: {}, 错误: {}", component.getClass().getName(), e.getMessage());
            }
        }
        super.doPause();
    }

    /**
     * 执行实体恢复操作
     * <p>
     * 调用父类恢复方法，然后恢复实体中的所有组件
     * 组件恢复过程中的异常会被捕获并记录，不会影响其他组件的恢复
     * </p>
     *
     * @throws Exception 恢复过程中可能发生的异常
     */
    @Override
    protected void doResume() throws Exception {
        super.doResume();
        // 恢复所有组件，按优先级排序（优先级值小的先恢复）
        List<BaseComponent> sortedComponents = new ArrayList<>(componentMap.values());
        Collections.sort(sortedComponents);

        // 使用try-catch处理每个组件的异常
        for (BaseComponent component : sortedComponents) {
            try {
                component.resume();
            } catch (Exception e) {
                // 记录异常但继续执行其他组件的恢复
                log.error("组件恢复失败: {}, 错误: {}", component.getClass().getName(), e.getMessage());
            }
        }
    }

    /**
     * 执行实体停止操作
     * <p>
     * 停止实体中的所有组件，然后调用父类停止方法
     * 组件停止过程中的异常会被捕获并记录，不会影响其他组件的停止
     * </p>
     *
     * @throws Exception 停止过程中可能发生的异常
     */
    @Override
    protected void doStop() throws Exception {
        // 停止所有组件，按优先级逆序排序（优先级值大的先停止）
        List<BaseComponent> sortedComponents = new ArrayList<>(componentMap.values());
        Collections.sort(sortedComponents, Collections.reverseOrder());

        // 使用try-catch处理每个组件的异常
        for (BaseComponent component : sortedComponents) {
            try {
                component.stop();
            } catch (Exception e) {
                // 记录异常但继续执行其他组件的停止
                log.error("组件停止失败: {}, 错误: {}", component.getClass().getName(), e.getMessage());
            }
        }
        super.doStop();
    }

    /**
     * 执行实体销毁操作
     * <p>
     * 销毁实体中的所有组件，清空组件映射表，然后调用父类销毁方法
     * 组件销毁过程中的异常会被捕获并记录，不会影响其他组件的销毁
     * </p>
     *
     * @throws Exception 销毁过程中可能发生的异常
     */
    @Override
    protected void doDestroy() throws Exception {
        // 销毁所有组件，按优先级逆序排序（优先级值大的先销毁）
        List<BaseComponent> sortedComponents = new ArrayList<>(componentMap.values());
        Collections.sort(sortedComponents, Collections.reverseOrder());

        // 使用try-catch处理每个组件的异常
        for (BaseComponent component : sortedComponents) {
            try {
                component.destroy();
            } catch (Exception e) {
                // 记录异常但继续执行其他组件的销毁
                log.error("组件销毁失败: {}, 错误: {}", component.getClass().getName(), e.getMessage());
            }
        }
        componentMap.clear();
        super.doDestroy();
    }
}