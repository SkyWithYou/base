package com.swy.common.thread;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 优先级命令队列实现
 * <p>
 * 实现了BaseCommandQueue抽象类，提供基于优先级的命令队列
 * </p>
 *
 * @author SkyWithYou
 */
@Slf4j
public class PriorityCommandQueue<C extends Command> extends BaseCommandQueue<C> {

    /**
     * 队列优先级
     */
    @Getter
    private final int priority;

    /**
     * 队列容量限制
     */
    @Getter
    private final int queueLimit;

    /**
     * 队列类型（用于区分不同业务场景）
     */
    @Getter
    private final String queueType;

    /**
     * 构造函数
     *
     * @param token      队列标识
     * @param priority   队列优先级（值越大优先级越高）
     * @param queueLimit 队列容量限制
     * @param queueType  队列类型
     */
    public PriorityCommandQueue(String token, int priority, int queueLimit, String queueType) {
        super(token);
        this.priority = priority;
        this.queueLimit = queueLimit;
        this.queueType = queueType;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public int getQueueLimit() {
        return queueLimit;
    }

    @Override
    protected Queue<C> buildQueue() {
        // 使用线程安全的队列实现
        return new ConcurrentLinkedQueue<>();
    }

    /**
     * 添加命令到队列
     *
     * @param command 命令
     * @return 是否添加成功
     */
    public boolean addCommand(C command) {
        if (command == null) {
            return false;
        }

        // 检查队列是否已满
        if (getCommands().size() >= queueLimit) {
            log.warn("队列已满，无法添加命令: {}, 队列: {}", command.commandId(), getToken());
            return false;
        }

        boolean result = getCommands().offer(command);
        if (result) {
            log.debug("添加命令到队列: {}, 命令ID: {}, 队列大小: {}", 
                    getToken(), command.commandId(), getCommands().size());
        }
        return result;
    }

    @Override
    public void run() {
        // 将队列注册到线程池管理器
        CommandThreadPoolManager.getInstance().registerQueue(this);
        log.info("命令队列已注册到线程池管理器: {}, 优先级: {}, 类型: {}", 
                getToken(), getPriority(), getQueueType());
    }

    /**
     * 启动队列处理
     */
    public void start() {
        // 运行队列（注册到线程池管理器）
        run();
        // 确保线程池管理器已启动
        CommandThreadPoolManager.getInstance().start();
    }

    /**
     * 停止队列处理
     */
    public void stop() {
        // 清空队列
        getCommands().clear();
        log.info("命令队列已停止: {}", getToken());
    }
}