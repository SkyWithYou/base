package com.swy.common.thread;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 命令线程池管理器
 * <p>
 * 基于权重分配队列，采用不公平锁设计方案消费队列
 * </p>
 *
 * @author SkyWithYou
 */
@Slf4j
public class CommandThreadPoolManager {

    /**
     * 单例实例
     */
    private static volatile CommandThreadPoolManager instance;

    /**
     * 命令队列集合
     */
    @Getter
    private final List<BaseCommandQueue<?>> commandQueues;

    /**
     * 线程池
     */
    private final ThreadPoolExecutor threadPool;

    /**
     * 队列锁（使用不公平锁）
     */
    private final ReentrantLock queueLock;

    /**
     * 是否正在运行
     */
    private volatile boolean running;

    /**
     * 私有构造函数
     */
    private CommandThreadPoolManager() {
        this.commandQueues = new ArrayList<>();
        // 创建线程池，核心线程数为可用处理器数量，最大线程数为核心线程数的2倍
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maximumPoolSize = corePoolSize * 2;
        long keepAliveTime = 60L;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(1000);
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        this.threadPool = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                threadFactory,
                handler
        );

        // 使用不公平锁，确保高优先级任务能够优先获取锁
        this.queueLock = new ReentrantLock(false);
        this.running = false;
    }

    /**
     * 获取单例实例
     *
     * @return 线程池管理器实例
     */
    public static CommandThreadPoolManager getInstance() {
        if (instance == null) {
            synchronized (CommandThreadPoolManager.class) {
                if (instance == null) {
                    instance = new CommandThreadPoolManager();
                }
            }
        }
        return instance;
    }

    /**
     * 注册命令队列
     *
     * @param commandQueue 命令队列
     */
    public void registerQueue(BaseCommandQueue<?> commandQueue) {
        if (commandQueue == null) {
            return;
        }
        commandQueues.add(commandQueue);
        // 按照优先级排序，确保高优先级的队列在前面
        sortQueues();
        log.info("注册命令队列: {}, 优先级: {}", commandQueue.getToken(), commandQueue.getPriority());
    }

    /**
     * 对队列按照优先级进行排序
     */
    private void sortQueues() {
        commandQueues.sort((q1, q2) -> Double.compare(q2.priorityFix(), q1.priorityFix()));
    }

    /**
     * 启动线程池管理器
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        // 启动队列消费线程
        threadPool.execute(this::consumeQueues);
        log.info("命令线程池管理器已启动");
    }

    /**
     * 停止线程池管理器
     */
    public void stop() {
        running = false;
        threadPool.shutdown();
        log.info("命令线程池管理器已停止");
    }

    /**
     * 消费队列任务
     */
    private void consumeQueues() {
        while (running) {
            try {
                // 重新排序队列，确保高优先级的队列优先处理
                sortQueues();
                
                boolean processed = false;
                
                // 尝试获取不公平锁
                if (queueLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                    try {
                        // 按照优先级顺序处理队列
                        for (BaseCommandQueue<?> queue : commandQueues) {
                            processed = processQueue(queue) || processed;
                        }
                    } finally {
                        queueLock.unlock();
                    }
                }
                
                // 如果没有处理任何任务，短暂休眠避免CPU空转
                if (!processed) {
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                log.error("队列消费线程被中断", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("队列消费过程中发生异常", e);
            }
        }
    }

    /**
     * 处理单个队列
     *
     * @param queue 命令队列
     * @return 是否成功处理了命令
     */
    @SuppressWarnings("unchecked")
    private <C extends Command> boolean processQueue(BaseCommandQueue<C> queue) {
        Queue<C> commands = queue.getCommands();
        if (commands.isEmpty()) {
            return false;
        }
        
        // 从队列中获取命令但不移除
        C command = commands.peek();
        if (command == null) {
            return false;
        }
        
        // 提交到线程池执行
        threadPool.execute(() -> {
            try {
                // 确保命令只被执行一次
                C cmd = commands.poll();
                if (cmd != null) {
                    cmd.execute();
                    log.debug("执行命令: {}, 队列: {}", cmd.commandId(), queue.getToken());
                }
            } catch (Exception e) {
                log.error("执行命令时发生异常", e);
            }
        });
        
        return true;
    }

    /**
     * 获取线程池状态信息
     *
     * @return 线程池状态信息
     */
    public Map<String, Object> getThreadPoolStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("activeCount", threadPool.getActiveCount());
        status.put("corePoolSize", threadPool.getCorePoolSize());
        status.put("maximumPoolSize", threadPool.getMaximumPoolSize());
        status.put("poolSize", threadPool.getPoolSize());
        status.put("queueSize", threadPool.getQueue().size());
        status.put("taskCount", threadPool.getTaskCount());
        status.put("completedTaskCount", threadPool.getCompletedTaskCount());
        
        List<Map<String, Object>> queuesInfo = new ArrayList<>();
        for (BaseCommandQueue<?> queue : commandQueues) {
            Map<String, Object> queueInfo = new HashMap<>();
            queueInfo.put("token", queue.getToken());
            queueInfo.put("priority", queue.getPriority());
            queueInfo.put("adjustedPriority", queue.priorityFix());
            queueInfo.put("queueSize", queue.getCommands().size());
            queueInfo.put("queueLimit", queue.getQueueLimit());
            queuesInfo.add(queueInfo);
        }
        status.put("queues", queuesInfo);
        
        return status;
    }
}