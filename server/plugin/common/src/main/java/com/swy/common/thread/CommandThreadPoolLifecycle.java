package com.swy.common.thread;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.swy.common.component.lifecycle.AbstractLifecycleBase;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 命令线程池生命周期管理类
 * <p>
 * 继承自AbstractLifecycleBase，提供生命周期管理能力
 * 基于权重分配队列，采用不公平锁设计方案消费队列
 * 根据线程池状况动态调整命令队列优先级
 * </p>
 *
 * @author SkyWithYou
 */
@Slf4j
public class CommandThreadPoolLifecycle extends AbstractLifecycleBase {

    protected final static int CACHE_EXPIRED = 30;

    /**
     * 命令队列集合
     */
    @Getter
    private final Cache<String, BaseCommandQueue<?>> commandQueues;

    /**
     * 线程池
     */
    private final ScheduledExecutorService threadPool;

    /**
     * 队列锁（使用不公平锁）
     */
    private final ReentrantLock queueLock;

    /**
     * 线程工厂，用于创建线程并设置线程名称
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + "-thread-" + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    /**
     * 私有构造函数
     */
    private CommandThreadPoolLifecycle(String threadPoolName) {
        // 核心线程数为可用处理器数量 + 1
        this(threadPoolName, Runtime.getRuntime().availableProcessors() + 1);
    }

    public CommandThreadPoolLifecycle(String threadPoolName, int corePoolSize) {
        this.commandQueues = newQueueCache();
        // 创建线程池
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(1000);
        ThreadFactory threadFactory = new NamedThreadFactory(threadPoolName);
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        this.threadPool = Executors.newScheduledThreadPool(corePoolSize, threadFactory);

        // 使用不公平锁，确保高优先级任务能够优先获取锁
        this.queueLock = new ReentrantLock(false);
    }

    protected Cache<String, BaseCommandQueue<?>> newQueueCache() {
        return CacheBuilder.newBuilder()
                .expireAfterAccess(CACHE_EXPIRED, TimeUnit.MINUTES) // 指定时间内未读写过时移除
                .build();
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
//        log.info("注册命令队列: {}, 优先级: {}", commandQueue.getToken(), commandQueue.getPriority());
    }

    /**
     * 对队列按照优先级进行排序
     */
    private void sortQueues() {
        commandQueues.sort((q1, q2) -> Double.compare(q2.priorityFix(), q1.priorityFix()));
    }

    /**
     * 执行初始化操作
     *
     * @throws Exception 初始化过程中可能发生的异常
     */
    @Override
    protected void doInitialize() throws Exception {
        super.doInitialize();
        log.info("命令线程池初始化，核心线程数: {}, 最大线程数: {}",
                threadPool.getCorePoolSize(), threadPool.getMaximumPoolSize());
    }

    /**
     * 执行启动操作
     *
     * @throws Exception 启动过程中可能发生的异常
     */
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        running = true;
        // 启动队列消费线程
        threadPool.execute(this::consumeQueues);
        log.info("命令线程池已启动");
    }

    /**
     * 执行停止操作
     *
     * @throws Exception 停止过程中可能发生的异常
     */
    @Override
    protected void doStop() throws Exception {
        running = false;
        // 等待线程池任务完成
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("命令线程池已停止");
        super.doStop();
    }

    /**
     * 执行销毁操作
     *
     * @throws Exception 销毁过程中可能发生的异常
     */
    @Override
    protected void doDestroy() throws Exception {
        // 确保线程池已关闭
        if (!threadPool.isShutdown()) {
            threadPool.shutdownNow();
        }
        // 清空队列集合
        commandQueues.clear();
        log.info("命令线程池已销毁");
        super.doDestroy();
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

        // 获取线程池当前负载情况
        int activeCount = threadPool.getActiveCount();
        int maximumPoolSize = threadPool.getMaximumPoolSize();
        double loadFactor = (double) activeCount / maximumPoolSize;

        // 根据线程池负载情况决定是否处理命令
        // 当负载较高时，只处理高优先级队列的命令
        if (loadFactor > 0.8 && queue.getPriority() < 5) {
            // 负载高，跳过低优先级队列
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

    /**
     * 随机选择队列添加命令
     * <p>
     * 基于权重随机选择队列，权重越高的队列被选中的概率越低
     * 确保负载均衡的同时优先使用低优先级队列
     * </p>
     *
     * @param command 要添加的命令
     * @param <C>     命令类型
     * @return 是否添加成功
     */
    @SuppressWarnings("unchecked")
    public <C extends Command> boolean addCommand(C command) {
        if (command == null || commandQueues.isEmpty()) {
            return false;
        }

        // 计算总权重的倒数
        double totalInverseWeight = 0.0;
        for (BaseCommandQueue<?> queue : commandQueues) {
            // 使用优先级的倒数作为权重，优先级越高，权重越低
            totalInverseWeight += 1.0 / queue.priorityFix();
        }

        // 生成随机值
        double random = Math.random() * totalInverseWeight;
        double weightSum = 0.0;

        // 根据权重随机选择队列
        for (BaseCommandQueue<?> queue : commandQueues) {
            // 累加权重
            weightSum += 1.0 / queue.priorityFix();

            // 当累加权重超过随机值时，选择当前队列
            if (weightSum >= random) {
                BaseCommandQueue<C> selectedQueue = (BaseCommandQueue<C>) queue;

                // 检查队列是否已满
                if (selectedQueue.getCommands().size() >= selectedQueue.getQueueLimit()) {
                    // 队列已满，尝试找一个未满的队列
                    for (BaseCommandQueue<?> alternativeQueue : commandQueues) {
                        BaseCommandQueue<C> altQueue = (BaseCommandQueue<C>) alternativeQueue;
                        if (altQueue.getCommands().size() < altQueue.getQueueLimit()) {
                            // 添加到备选队列
                            altQueue.getCommands().offer(command);
                            log.debug("队列已满，添加命令到备选队列: {}, 命令ID: {}", altQueue.getToken(), command.commandId());
                            return true;
                        }
                    }
                    // 所有队列都已满
                    log.warn("所有队列已满，无法添加命令: {}", command.commandId());
                    return false;
                }
                // 添加到选中的队列
                selectedQueue.getCommands().offer(command);
                log.debug("添加命令到队列: {}, 命令ID: {}", selectedQueue.getToken(), command.commandId());
                return true;
            }
        }

        // 理论上不会执行到这里
        return false;
    }
}