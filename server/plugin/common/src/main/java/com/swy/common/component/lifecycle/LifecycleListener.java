package com.swy.common.component.lifecycle;

/**
 * 生命周期事件监听器接口
 * <p>
 * 用于监听实体生命周期状态变化事件，实现此接口可以在状态转换时执行自定义逻辑
 * </p>
 */
public interface LifecycleListener {

    /**
     * 状态变化事件处理方法
     *
     * @param source 事件源对象
     * @param oldState 原状态
     * @param newState 新状态
     */
    void onStateChanged(Lifecycle source, LifecycleState oldState, LifecycleState newState);
}