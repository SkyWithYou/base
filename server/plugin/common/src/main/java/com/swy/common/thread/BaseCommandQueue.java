package com.swy.common.thread;

import lombok.Getter;

import java.util.Queue;

/**
 * 命令队列
 *
 * @author SkyWithYou
 */
@Getter
public abstract class BaseCommandQueue<C extends Command> implements Runnable {

    /**
     * 队列token
     */
    private final String token;

    /**
     * 命令队列
     */
    private final Queue<C> commands;

    // 避免使用魔法值
    /**
     * 最小命令队列大小阈值
     */
    private final int MIN_COMMAND_THRESHOLD = 10;
    /**
     * 低使用率阈值
     */
    private final double LOW_USAGE_THRESHOLD = 0.3;

    /**
     * 当前优先级
     */
    private int currentPriority;

    public BaseCommandQueue(String token) {
        this.token = token;
        this.commands = buildQueue();
    }

    public BaseCommandQueue(String token, Queue<C> commands) {
        this.token = token;
        this.commands = commands;
    }

    /**
     * 获取初始优先级
     *
     * @return 优先级
     */
    public abstract int getInitPriority();

    /**
     * 获取队列限制
     *
     * @return 队列限制
     */
    public abstract int getQueueLimit();

    /**
     * 构建队列
     *
     * @return 队列
     */
    protected abstract Queue<C> buildQueue();

    /**
     * 优先级修正算法
     * <p>
     * 新算法基于当前队列大小与队列限制的比例，动态调整优先级。
     * 当队列接近满载时，优先级会显著提高，以确保高负载下的任务优先处理。
     * </p>
     *
     * @return 修正后的优先级值
     */
    public synchronized double priorityFix() {
        double queueSize = commands.size();
        int queueLimit = getQueueLimit();

        // 如果队列大小小于最小阈值或使用率低于低使用率阈值，直接返回原始优先级
        if (queueSize < MIN_COMMAND_THRESHOLD || queueSize / queueLimit < LOW_USAGE_THRESHOLD) {
            return getInitPriority();
        }

        // 防止除零错误
        if (queueLimit <= 0) {
            return getInitPriority();
        }

        // 动态调整因子，根据队列使用率调整优先级
        double usageRate = queueSize / queueLimit;
        double adjustmentFactor = 1 + Math.pow(usageRate, 2) * 2;

        this.currentPriority = (int) (getInitPriority() * adjustmentFactor);

        return this.currentPriority;
    }
}