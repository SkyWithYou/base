package com.swy.common.component;

import com.swy.common.component.lifecycle.AbstractLifecycleBase;

/**
 * 生命周期组件基类
 * <p>
 * 继承自AbstractLifecycleBase，为组件提供生命周期管理能力
 * 作为所有组件的基础类，定义组件的共同特性和行为
 * 子类可以通过继承此类实现特定功能的组件
 * </p>
 *
 * @author SkyWithYou
 */
public abstract class BaseComponent extends AbstractLifecycleBase {
    /**
     * 组件特有的实现
     * <p>
     * 子类应该根据具体需求重写生命周期方法：
     * - doInitialize(): 初始化组件资源
     * - doStart(): 启动组件
     * - doPause(): 暂停组件
     * - doResume(): 恢复组件
     * - doStop(): 停止组件
     * - doDestroy(): 销毁组件资源
     * </p>
     */
}