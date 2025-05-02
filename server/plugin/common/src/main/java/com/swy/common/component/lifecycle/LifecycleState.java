package com.swy.common.component.lifecycle;

import com.swy.common.annotation.Description;

/**
 * 生命周期状态枚举
 * <p>
 * 定义实体和组件在生命周期中可能处于的所有状态
 * 状态之间的转换遵循特定的规则，由AbstractLifecycleBase实现
 * </p>
 * 
 * @author SkyWithYou
 */
public enum LifecycleState {
    /**
     * 新建状态
     * <p>
     * 实体刚创建，尚未初始化
     * </p>
     */
    @Description("新建状态")
    NEW,
    
    /**
     * 正在初始化状态
     * <p>
     * 实体正在执行初始化过程
     * </p>
     */
    @Description("正在初始化")
    INITIALIZING,
    
    /**
     * 已初始化状态
     * <p>
     * 实体已完成初始化，但尚未启动
     * </p>
     */
    @Description("已初始化")
    INITIALIZED,
    
    /**
     * 运行中状态
     * <p>
     * 实体已启动并正常运行
     * </p>
     */
    @Description("运行中")
    RUNNING,
    
    /**
     * 已暂停状态
     * <p>
     * 实体暂时停止运行，但未释放资源
     * </p>
     */
    @Description("已暂停")
    PAUSED,
    
    /**
     * 正在停止状态
     * <p>
     * 实体正在执行停止过程
     * </p>
     */
    @Description("正在停止")
    STOPPING,
    
    /**
     * 已停止状态
     * <p>
     * 实体已停止运行，但可以重新启动
     * </p>
     */
    @Description("已停止")
    STOPPED,
    
    /**
     * 正在销毁状态
     * <p>
     * 实体正在执行销毁过程，释放资源
     * </p>
     */
    @Description("正在销毁")
    DESTROYING,
    
    /**
     * 已销毁状态
     * <p>
     * 实体已完全销毁，不可再使用
     * </p>
     */
    @Description("已销毁")
    DESTROYED
}