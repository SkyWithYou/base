package com.swy.common.thread;

import lombok.Data;
import lombok.Getter;

import java.util.Queue;
import java.util.concurrent.Future;

import com.swy.common.result.ResultBean;

/**
 * 命令队列
 *
 * @author SkyWithYou
 */
@Data
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
     * 当前执行的命令
     */
    private C currentCommand;

    /**
     * 当前执行的命令结果
     */
    private Future<ResultBean> currentCommandFuture;

    /**
     * 命令执行结果
     */

    /**
     * 当前优先级
     */
    private int currentPriority;

    /**
     * 开始时间
     */
    private int startTime;

    /*
     * 是否撤回
     */
    private boolean cancel;

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

    public void run() {
        // 记录开始时间
        this.startTime = TimeUtil.getCurrentTime();

        // 执行命令
        while (true) {
            // 检查是否需要撤回
            if (this.isCancel()) {
                this.startTime = 0;
                break;
            }

            // 检查是否有新的命令
            this.currentCommand = commands.poll();
            if (currentCommand != null) {
                // 检查命令是否需要撤回
                if (currentCommand.isCancel()) {
                    continue;
                }

                // 执行命令
                this.currentCommandFuture = currentCommand.execute();
            } else {
                // 没有新的命令，退出循环
                this.currentCommandFuture = null;
                this.startTime = 0;
                break;
            }
        }
    }
}