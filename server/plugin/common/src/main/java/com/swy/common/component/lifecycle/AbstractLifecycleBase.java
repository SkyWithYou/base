package com.swy.common.component.lifecycle;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * 生命周期管理基类
 * <p>
 * 为所有需要生命周期管理的组件和实体提供统一的基础实现，包含：
 * - 标准状态机管理
 * - 事件监听机制
 * - 生命周期钩子方法
 * - 优先级管理
 * </p>
 *
 * @author SkyWithYou
 */
public abstract class AbstractLifecycleBase implements Lifecycle, Comparable<AbstractLifecycleBase> {

    /**
     * 状态监听器列表
     */
    private final List<LifecycleListener> listeners = Lists.newArrayList();

    /**
     * 当前状态
     */
    private volatile LifecycleState state = LifecycleState.NEW;

    /**
     * 获取当前生命周期状态
     * <p>
     * 返回实体当前所处的生命周期状态
     * </p>
     *
     * @return 当前生命周期状态枚举值
     */
    @Override
    public LifecycleState getState() {
        return state;
    }

    /**
     * 更新状态并触发状态变化事件
     * <p>
     * 将当前状态更新为新状态，并通知所有监听器状态已变化
     * 此方法在所有状态转换操作中被调用
     * </p>
     *
     * @param newState 新状态，不能为null
     */
    protected void setState(LifecycleState newState) {
        LifecycleState oldState = this.state;
        this.state = newState;
        fireStateChangedEvent(oldState, newState);
    }

    /**
     * 触发状态变化事件
     * <p>
     * 遍历所有注册的监听器，调用它们的onStateChanged方法
     * 通知它们状态已从oldState变为newState
     * </p>
     *
     * @param oldState 原状态，表示变化前的状态
     * @param newState 新状态，表示变化后的状态
     */
    protected void fireStateChangedEvent(LifecycleState oldState, LifecycleState newState) {
        for (LifecycleListener listener : listeners) {
            listener.onStateChanged(this, oldState, newState);
        }
    }

    /**
     * 初始化实体
     * <p>
     * 将实体从NEW状态转换为INITIALIZED状态
     * 执行必要的资源分配和初始化操作
     * 只有处于NEW状态的实体才能被初始化
     * </p>
     *
     * @throws Exception             初始化过程中可能发生的异常
     * @throws IllegalStateException 如果实体不处于NEW状态
     */
    @Override
    public void initialize() throws Exception {
        if (state != LifecycleState.NEW) {
            throw new IllegalStateException("初始化失败：当前状态 " + state);
        }
        setState(LifecycleState.INITIALIZING);
        doInitialize();
        setState(LifecycleState.INITIALIZED);
    }

    /**
     * 执行具体的初始化操作
     * <p>
     * 子类应重写此方法实现特定的初始化逻辑
     * 在状态转换为INITIALIZING后被调用
     * </p>
     *
     * @throws Exception 初始化过程中可能发生的异常
     */
    protected void doInitialize() throws Exception {

    }

    /**
     * 启动实体
     * <p>
     * 将实体从INITIALIZED或STOPPED状态转换为RUNNING状态
     * 启动实体的功能，使其能够正常工作
     * 只有处于INITIALIZED或STOPPED状态的实体才能被启动
     * </p>
     *
     * @throws Exception             启动过程中可能发生的异常
     * @throws IllegalStateException 如果实体不处于INITIALIZED或STOPPED状态
     */
    @Override
    public void start() throws Exception {
        if (state != LifecycleState.INITIALIZED && state != LifecycleState.STOPPED) {
            throw new IllegalStateException("启动失败：当前状态 " + state);
        }
        setState(LifecycleState.INITIALIZING);
        doStart();
        setState(LifecycleState.RUNNING);
    }

    /**
     * 执行具体的启动操作
     * <p>
     * 子类应重写此方法实现特定的启动逻辑
     * 在状态转换为INITIALIZING后被调用
     * </p>
     *
     * @throws Exception 启动过程中可能发生的异常
     */
    protected void doStart() throws Exception {

    }

    /**
     * 暂停实体
     * <p>
     * 将实体从RUNNING状态转换为PAUSED状态
     * 暂时停止实体的功能，但保持资源不释放，以便后续恢复
     * 只有处于RUNNING状态的实体才能被暂停
     * </p>
     *
     * @throws Exception             暂停过程中可能发生的异常
     * @throws IllegalStateException 如果实体不处于RUNNING状态
     */
    @Override
    public void pause() throws Exception {
        if (state != LifecycleState.RUNNING) {
            throw new IllegalStateException("暂停失败：当前状态 " + state);
        }
        doPause();
        setState(LifecycleState.PAUSED);
    }

    /**
     * 执行具体的暂停操作
     * <p>
     * 子类应重写此方法实现特定的暂停逻辑
     * 在pause()方法中被调用，状态转换为PAUSED之前
     * </p>
     *
     * @throws Exception 暂停过程中可能发生的异常
     */
    protected void doPause() throws Exception {
    }

    /**
     * 恢复实体运行
     * <p>
     * 将实体从PAUSED状态转换为RUNNING状态
     * 恢复暂停的实体功能，使其重新开始工作
     * 只有处于PAUSED状态的实体才能被恢复
     * </p>
     *
     * @throws Exception             恢复过程中可能发生的异常
     * @throws IllegalStateException 如果实体不处于PAUSED状态
     */
    @Override
    public void resume() throws Exception {
        if (state != LifecycleState.PAUSED) {
            throw new IllegalStateException("恢复失败：当前状态 " + state);
        }
        doResume();
        setState(LifecycleState.RUNNING);
    }

    /**
     * 执行具体的恢复操作
     * <p>
     * 子类应重写此方法实现特定的恢复逻辑
     * 在resume()方法中被调用，状态转换为RUNNING之前
     * </p>
     *
     * @throws Exception 恢复过程中可能发生的异常
     */
    protected void doResume() throws Exception {
    }

    /**
     * 停止实体
     * <p>
     * 将实体从RUNNING或PAUSED状态转换为STOPPED状态
     * 停止实体的功能，但保持资源不完全释放，以便后续重新启动
     * 只有处于RUNNING或PAUSED状态的实体才能被停止
     * </p>
     *
     * @throws Exception             停止过程中可能发生的异常
     * @throws IllegalStateException 如果实体不处于RUNNING或PAUSED状态
     */
    @Override
    public void stop() throws Exception {
        if (state != LifecycleState.RUNNING && state != LifecycleState.PAUSED) {
            throw new IllegalStateException("停止失败：当前状态 " + state);
        }
        setState(LifecycleState.STOPPING);
        doStop();
        setState(LifecycleState.STOPPED);
    }

    /**
     * 执行具体的停止操作
     * <p>
     * 子类应重写此方法实现特定的停止逻辑
     * 在状态转换为STOPPING后被调用
     * </p>
     *
     * @throws Exception 停止过程中可能发生的异常
     */
    protected void doStop() throws Exception {
    }

    /**
     * 销毁实体
     * <p>
     * 将实体转换为DESTROYED状态
     * 释放实体占用的所有资源，销毁后的实体不可重新使用
     * 任何状态的实体都可以被销毁，但已销毁的实体不会重复销毁
     * </p>
     *
     * @throws Exception 销毁过程中可能发生的异常
     */
    @Override
    public void destroy() throws Exception {
        if (state == LifecycleState.DESTROYED) {
            return;
        }
        setState(LifecycleState.DESTROYING);
        doDestroy();
        setState(LifecycleState.DESTROYED);
    }

    /**
     * 执行具体的销毁操作
     * <p>
     * 子类应重写此方法实现特定的资源释放逻辑
     * 在状态转换为DESTROYING后被调用
     * </p>
     *
     * @throws Exception 销毁过程中可能发生的异常
     */
    protected void doDestroy() throws Exception {
    }

    /**
     * 检查实体是否已初始化
     * <p>
     * 判断实体是否已经完成初始化且尚未被销毁
     * </p>
     *
     * @return true表示实体已初始化且未销毁，false表示实体未初始化或已销毁
     */
    @Override
    public boolean isInitialized() {
        return state != LifecycleState.NEW && state != LifecycleState.DESTROYED;
    }

    /**
     * 检查实体是否正在运行
     * <p>
     * 判断实体当前是否处于运行状态
     * </p>
     *
     * @return true表示实体正在运行，false表示实体未运行
     */
    @Override
    public boolean isRunning() {
        return state == LifecycleState.RUNNING;
    }

    /**
     * 添加生命周期状态监听器
     * <p>
     * 将监听器添加到监听器列表中，当状态变化时会通知该监听器
     * </p>
     *
     * @param listener 要添加的状态监听器，不能为null
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * 移除生命周期状态监听器
     * <p>
     * 从监听器列表中移除指定的监听器，移除后将不再接收状态变化通知
     * </p>
     *
     * @param listener 要移除的状态监听器，不能为null
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * 比较优先级
     * <p>
     * 实现Comparable接口，用于基于优先级的排序
     * 优先级值大的排在前面（优先级高）
     * </p>
     *
     * @param other 要比较的对象
     * @return 负数表示当前对象优先级高，0表示相等，正数表示当前对象优先级低
     */
    @Override
    public int compareTo(AbstractLifecycleBase other) {
        return Integer.compare(other.getPriority(), this.getPriority());
    }
}