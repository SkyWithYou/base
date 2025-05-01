package com.swy.common.component.lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽象生命周期实体类
 * <p>
 * 提供Lifecycle接口的基础实现，包括：
 * - 状态管理和转换
 * - 生命周期事件监听器管理
 * - 基本生命周期操作的实现
 * </p>
 */
public abstract class AbstractLifecycleEntity implements Lifecycle {

    /**
     * 当前状态
     */
    private volatile LifecycleState state = LifecycleState.NEW;

    /**
     * 状态监听器列表
     */
    private final List<LifecycleListener> listeners = new ArrayList<>();

    @Override
    public LifecycleState getState() {
        return state;
    }

    /**
     * 更新状态并触发状态变化事件
     *
     * @param newState 新状态
     */
    protected void setState(LifecycleState newState) {
        LifecycleState oldState = this.state;
        this.state = newState;
        fireStateChangedEvent(oldState, newState);
    }

    /**
     * 触发状态变化事件
     *
     * @param oldState 原状态
     * @param newState 新状态
     */
    protected void fireStateChangedEvent(LifecycleState oldState, LifecycleState newState) {
        for (LifecycleListener listener : listeners) {
            listener.onStateChanged(this, oldState, newState);
        }
    }

    @Override
    public void initialize() throws Exception {
        if (state != LifecycleState.NEW) {
            throw new IllegalStateException("Cannot initialize entity in state: " + state);
        }
        setState(LifecycleState.INITIALIZING);
        doInitialize();
        setState(LifecycleState.INITIALIZED);
    }

    /**
     * 执行具体的初始化操作
     *
     * @throws Exception 初始化过程中的异常
     */
    protected void doInitialize() throws Exception {
    }

    @Override
    public void start() throws Exception {
        if (state != LifecycleState.INITIALIZED && state != LifecycleState.STOPPED) {
            throw new IllegalStateException("Cannot start entity in state: " + state);
        }
        doStart();
        setState(LifecycleState.RUNNING);
    }

    /**
     * 执行具体的启动操作
     *
     * @throws Exception 启动过程中的异常
     */
    protected void doStart() throws Exception {
    }

    @Override
    public void pause() throws Exception {
        if (state != LifecycleState.RUNNING) {
            throw new IllegalStateException("Cannot pause entity in state: " + state);
        }
        doPause();
        setState(LifecycleState.PAUSED);
    }

    /**
     * 执行具体的暂停操作
     *
     * @throws Exception 暂停过程中的异常
     */
    protected void doPause() throws Exception {
    }

    @Override
    public void resume() throws Exception {
        if (state != LifecycleState.PAUSED) {
            throw new IllegalStateException("Cannot resume entity in state: " + state);
        }
        doResume();
        setState(LifecycleState.RUNNING);
    }

    /**
     * 执行具体的恢复操作
     *
     * @throws Exception 恢复过程中的异常
     */
    protected void doResume() throws Exception {
    }

    @Override
    public void stop() throws Exception {
        if (state != LifecycleState.RUNNING && state != LifecycleState.PAUSED) {
            throw new IllegalStateException("Cannot stop entity in state: " + state);
        }
        setState(LifecycleState.STOPPING);
        doStop();
        setState(LifecycleState.STOPPED);
    }

    /**
     * 执行具体的停止操作
     *
     * @throws Exception 停止过程中的异常
     */
    protected void doStop() throws Exception {
    }

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
     *
     * @throws Exception 销毁过程中的异常
     */
    protected void doDestroy() throws Exception {
    }

    @Override
    public boolean isInitialized() {
        return state != LifecycleState.NEW && state != LifecycleState.DESTROYED;
    }

    @Override
    public boolean isRunning() {
        return state == LifecycleState.RUNNING;
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
}