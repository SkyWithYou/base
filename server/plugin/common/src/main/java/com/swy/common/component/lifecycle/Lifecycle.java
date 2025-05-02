package com.swy.common.component.lifecycle;

/**
 * 实体生命周期接口
 * <p>
 * 定义实体对象的生命周期管理方法，包括：
 * - 状态管理：获取和转换实体状态
 * - 基本操作：初始化、启动、暂停、恢复、停止、销毁等
 * - 事件监听：添加和移除状态变化监听器
 * </p>
 */
public interface Lifecycle {

    int PRIORITY_LOWEST = 10;
    int PRIORITY_LOW = 30;
    int PRIORITY_NORMAL = 50;
    int PRIORITY_HIGH = 70;
    int PRIORITY_HIGHEST = 90;

    /**
     * 获取当前状态
     *
     * @return 当前生命周期状态
     */
    LifecycleState getState();

    int getPriority();

    /**
     * 初始化实体
     * <p>
     * 完成实体所需的基本配置和资源准备工作
     * </p>
     *
     * @throws Exception 初始化过程中的异常
     */
    void initialize() throws Exception;

    /**
     * 启动实体
     * <p>
     * 使实体进入运行状态
     * </p>
     *
     * @throws Exception 启动过程中的异常
     */
    void start() throws Exception;

    /**
     * 暂停实体
     * <p>
     * 暂时停止实体的运行，但保持资源不释放
     * </p>
     *
     * @throws Exception 暂停过程中的异常
     */
    void pause() throws Exception;

    /**
     * 恢复实体运行
     * <p>
     * 将暂停的实体恢复到运行状态
     * </p>
     *
     * @throws Exception 恢复过程中的异常
     */
    void resume() throws Exception;

    /**
     * 停止实体
     * <p>
     * 停止实体的运行，可以后续重新启动
     * </p>
     *
     * @throws Exception 停止过程中的异常
     */
    void stop() throws Exception;

    /**
     * 销毁实体
     * <p>
     * 清理实体占用的资源，销毁后的实体不可重新使用
     * </p>
     *
     * @throws Exception 销毁过程中的异常
     */
    void destroy() throws Exception;

    /**
     * 检查实体是否已初始化
     *
     * @return true表示实体已初始化，false表示实体未初始化或已销毁
     */
    boolean isInitialized();

    /**
     * 检查实体是否正在运行
     *
     * @return true表示实体正在运行，false表示实体未运行
     */
    boolean isRunning();

    /**
     * 添加生命周期状态监听器
     *
     * @param listener 状态监听器
     */
    void addLifecycleListener(LifecycleListener listener);

    /**
     * 移除生命周期状态监听器
     *
     * @param listener 状态监听器
     */
    void removeLifecycleListener(LifecycleListener listener);
}