package com.swy.common.component.lifecycle;

import com.swy.common.annotation.Description;

/**
 * 生命周期状态枚举
 */
public enum LifecycleState {
    @Description("新建状态")
    NEW,
    @Description("正在初始化")
    INITIALIZING,
    @Description("已初始化")
    INITIALIZED,
    @Description("运行中")
    RUNNING,
    @Description("已暂停")
    PAUSED,
    @Description("正在停止")
    STOPPING,
    @Description("已停止")
    STOPPED,
    @Description("正在销毁")
    DESTROYING,
    @Description("已销毁")
    DESTROYED
}