package com.swy.common.component.entity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.Maps;
import com.swy.common.component.BaseComponent;
import com.swy.common.component.lifecycle.AbstractLifecycleBase;
import com.swy.common.component.service.AbstractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 抽象缓存服务基类
 * <p>
 * 实现BaseService接口，提供实体和组件的缓存管理功能
 * 继承自AbstractLifecycleBase，具备生命周期管理能力
 * 支持缓存过期机制、LRU淘汰策略和容量限制
 * </p>
 *
 * @author SkyWithYou
 */
@Slf4j
public abstract class AbstractEntityCacheService<E extends AbstractEntity<C>, C extends BaseComponent>
        extends AbstractService {

    /**
     * 常驻实体容器，使用ConcurrentHashMap保证线程安全
     */
    private Map<Long, E> residentEntities;
    /**
     * 实体缓存，使用Guava Cache实现LRU策略和过期机制
     */
    private Cache<Long, E> entityCache;

    /**
     * 缓存过期时间（毫秒）
     */
    @Value("${cache.expire-time:3600000}")
    private long expireTimeMs;
    /**
     * 缓存容量
     */
    @Value("${cache.max-capacity:1000}")
    private int maxCapacity;

    @Value("${cache.init-capacity:100}")
    private int initCapacity;

    /**
     * 初始化缓存服务
     * <p>
     * 创建常驻实体容器和实体缓存，配置缓存的过期策略、容量限制和移除监听器
     * </p>
     *
     * @throws Exception 初始化过程中可能发生的异常
     */
    @Override
    protected void doInitialize() throws Exception {
        // 使用ConcurrentHashMap直接初始化，提高代码可读性
        residentEntities = Maps.newConcurrentMap();

        // 初始化缓存，使用类型安全的方式构建
        this.entityCache = CacheBuilder.newBuilder()
                .initialCapacity(initCapacity)
                .maximumSize(maxCapacity)
                .expireAfterWrite(expireTimeMs, TimeUnit.MILLISECONDS)
                .removalListener((RemovalListener<Long, E>) notification -> {
                    try {
                        E entity = notification.getValue();
                        if (entity != null) {
                            // 记录移除原因，便于调试和监控
                            log.debug("实体从缓存中移除: ID={}, 原因={}", notification.getKey(), notification.getCause());
                            entity.stop();
                            entity.destroy();
                        }
                    } catch (Exception e) {
                        log.error("实体销毁失败: ID={}, 错误: {}", notification.getKey(), e.getMessage(), e);
                    }
                })
                .build();

        super.doInitialize();
    }

    /**
     * 启动缓存服务
     * <p>
     * 调用父类启动方法，并初始化缓存中的实体
     * </p>
     *
     * @throws Exception 启动过程中可能发生的异常
     */
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        // 初始化缓存实体
        initializeCacheEntities();
    }

    /**
     * 停止缓存服务
     * <p>
     * 清理并销毁所有缓存和常驻实体，释放资源
     * </p>
     *
     * @throws Exception 停止过程中可能发生的异常
     */
    @Override
    protected void doStop() throws Exception {
        // 销毁缓存中的实体
        entityCache.invalidateAll();

        // 停止常驻实体
        for (Map.Entry<Long, E> entry : residentEntities.entrySet()) {
            try {
                E e = entry.getValue();
                e.stop();
            } catch (Exception e) {
                log.error("常驻实体停止失败: {}, 错误: {}", entry.getKey(), e.getMessage());
            }
        }

        // 销毁常驻实体
        for (Map.Entry<Long, E> entry : residentEntities.entrySet()) {
            try {
                E e = entry.getValue();
                e.destroy();
            } catch (Exception e) {
                log.error("常驻实体销毁失败: {}, 错误: {}", entry.getKey(), e.getMessage());
            }
        }


        residentEntities.clear();

        super.doStop();
    }

    /**
     * 初始化缓存实体
     * <p>
     * 子类需要实现此方法，加载和初始化需要预缓存的实体
     * </p>
     *
     * @throws Exception 初始化过程中可能发生的异常
     */
    protected abstract void initializeCacheEntities() throws Exception;

    /**
     * 将实体从缓存迁移到常驻容器
     * <p>
     * 从缓存中移除实体并添加到常驻容器中，使实体不会被自动过期淘汰
     * </p>
     *
     * @param entityId 实体ID
     * @return 迁移的实体实例，如果缓存中不存在该实体则返回null
     */
    public E moveToResident(long entityId) {
        E entity = entityCache.getIfPresent(entityId);
        if (entity != null) {
            try {
                entityCache.invalidate(entityId);
                residentEntities.put(entityId, entity);
                log.debug("实体已从缓存移动到常驻容器: ID={}", entityId);
                return entity;
            } catch (Exception e) {
                log.error("将实体移动到常驻容器失败: ID={}, 错误: {}", entityId, e.getMessage(), e);
                // 恢复原状态，确保实体不会丢失
                entityCache.put(entityId, entity);
                residentEntities.remove(entityId);
            }
        } else {
            log.debug("尝试移动不存在的实体到常驻容器: ID={}", entityId);
        }
        return null;
    }

    /**
     * 将实体从常驻容器迁移到缓存
     * <p>
     * 从常驻容器中移除实体并添加到缓存中，使实体受缓存过期和淘汰策略管理
     * </p>
     *
     * @param entityId 实体ID
     * @return 迁移的实体实例，如果常驻容器中不存在该实体则返回null
     */
    public E moveToCache(long entityId) {
        E entity = residentEntities.get(entityId);
        if (entity != null) {
            try {
                residentEntities.remove(entityId);
                entityCache.put(entityId, entity);
                log.debug("实体已从常驻容器移动到缓存: ID={}", entityId);
                return entity;
            } catch (Exception e) {
                log.error("将实体移动到缓存失败: ID={}, 错误: {}", entityId, e.getMessage(), e);
            }
        } else {
            log.debug("尝试移动不存在的实体到缓存: ID={}", entityId);
        }
        return null;
    }

    /**
     * 创建实体实例
     * <p>
     * 子类需要实现此方法，创建一个新的实体实例
     * </p>
     *
     * @return 新创建的实体实例
     */
    protected abstract E createNewEntityInstance();

    /**
     * 尝试构建实体
     * <p>
     * 当实体不存在时，子类通过实现此方法来构建新的实体实例
     * 可以从数据库或其他数据源加载实体数据并初始化
     * </p>
     *
     * @param entityId 实体ID
     * @return 构建的实体实例，如果无法构建则返回null
     */
    protected abstract E tryBuildEntity(long entityId);

    /**
     * 获取指定ID的实体
     * <p>
     * 先从常驻容器中查找，再从缓存中查找，如果都没有找到且loadIfAbsent为true，
     * 则尝试构建新的实体并加入缓存
     * </p>
     *
     * @param entityId     实体ID
     * @param loadIfAbsent 如果实体不存在，是否尝试构建
     * @return 找到或新建的实体实例，如果无法找到或构建则返回null
     */
    public E getEntity(long entityId, boolean loadIfAbsent) {
        // 先从常驻容器中查找
        E entity = residentEntities.get(entityId);

        // 如果常驻容器中不存在，则从缓存中查找
        if (entity == null) {
            entity = entityCache.getIfPresent(entityId);
            if (entity != null) {
                log.debug("从缓存中获取实体: ID={}", entityId);
            }
        } else {
            log.debug("从常驻容器中获取实体: ID={}", entityId);
        }

        // 如果实体不存在且需要加载，尝试构建
        if (entity == null && loadIfAbsent) {
            log.debug("尝试构建实体: ID={}", entityId);
            entity = tryBuildEntity(entityId);
            if (entity != null) {
                log.debug("成功构建实体并加入缓存: ID={}", entityId);
                entityCache.put(entityId, entity);
            } else {
                log.debug("无法构建实体: ID={}", entityId);
            }
        }

        return entity;
    }

    /**
     * 获取指定ID的实体
     * <p>
     * 默认在实体不存在时尝试构建新实体
     * </p>
     *
     * @param entityId 实体ID
     * @return 找到或新建的实体实例，如果无法找到或构建则返回null
     */
    public E getEntity(long entityId) {
        return getEntity(entityId, true);
    }

    /**
     * 移除并销毁指定ID的实体
     * <p>
     * 从常驻容器和缓存中移除实体，并执行停止和销毁操作
     * </p>
     *
     * @param entityId 实体ID
     * @return 被移除的实体实例，如果不存在返回null
     */
    public E removeEntity(long entityId) {
        // 先检查常驻容器
        E entity = residentEntities.remove(entityId);
        if (entity == null) {
            // 再检查缓存
            entity = entityCache.getIfPresent(entityId);
            if (entity != null) {
                entityCache.invalidate(entityId);
            }
        } else {
            // 确保从两个容器中都移除
            entityCache.invalidate(entityId);
        }

        // 如果找到实体，执行清理操作
        if (entity != null) {
            try {
                log.debug("正在移除实体: ID={}", entityId);
                entity.stop();
                entity.destroy();
            } catch (Exception e) {
                log.error("实体销毁失败: ID={}, 错误: {}", entityId, e.getMessage(), e);
            }
        } else {
            log.debug("尝试移除不存在的实体: ID={}", entityId);
        }

        return entity;
    }
}