   package com.swy.common.thread;

import com.swy.common.result.ResultBean;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 命令线程池生命周期示例
 * <p>
 * 演示如何使用基于生命周期管理的线程池和权重分配队列
 * </p>
 *
 * @author SkyWithYou
 */
@Slf4j
public class CommandThreadPoolLifecycleDemo {

    /**
     * 命令ID生成器
     */
    private static final AtomicLong COMMAND_ID_GENERATOR = new AtomicLong(1);

    /**
     * 运行示例
     */
    public static void main(String[] args) throws Exception {
        // 初始化线程池生命周期管理器
        CommandThreadPoolLifecycle threadPool = CommandThreadPoolLifecycle.getInstance();
        
        // 初始化并启动线程池
        threadPool.initialize();
        threadPool.start();
        
        // 创建不同优先级的命令队列
        SimpleCommandQueue highPriorityQueue = new SimpleCommandQueue("high-priority", 10, 100);
        SimpleCommandQueue mediumPriorityQueue = new SimpleCommandQueue("medium-priority", 5, 100);
        SimpleCommandQueue lowPriorityQueue = new SimpleCommandQueue("low-priority", 1, 100);

        // 注册队列到线程池
        threadPool.registerQueue(highPriorityQueue);
        threadPool.registerQueue(mediumPriorityQueue);
        threadPool.registerQueue(lowPriorityQueue);

        // 添加测试命令到线程池（随机分配到队列）
        for (int i = 0; i < 50; i++) {
            DemoCommand command = new DemoCommand("任务-" + i, 100 + (i % 3) * 100);
            threadPool.addCommand(command);
        }

        // 打印线程池状态
        log.info("线程池初始状态: {}", threadPool.getThreadPoolStatus());

        // 等待一段时间，让任务执行
        Thread.sleep(2000);

        // 再次打印线程池状态
        log.info("线程池执行后状态: {}", threadPool.getThreadPoolStatus());

        // 停止线程池
        threadPool.stop();
        
        // 销毁线程池
        threadPool.destroy();
    }

    /**
     * 简单命令队列实现
     */
    static class SimpleCommandQueue extends BaseCommandQueue<DemoCommand> {

        private final int priority;
        private final int queueLimit;

        public SimpleCommandQueue(String token, int priority, int queueLimit) {
            super(token);
            this.priority = priority;
            this.queueLimit = queueLimit;
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
        protected Queue<DemoCommand> buildQueue() {
            return new java.util.concurrent.ConcurrentLinkedQueue<>();
        }

        @Override
        public void run() {
            // 注册到线程池
            CommandThreadPoolLifecycle.getInstance().registerQueue(this);
        }
    }

    /**
     * 示例命令实现
     */
    static class DemoCommand implements Command {

        private final long id;
        private final String name;
        private final long executionTime;

        public DemoCommand(String name, long executionTime) {
            this.id = COMMAND_ID_GENERATOR.getAndIncrement();
            this.name = name;
            this.executionTime = executionTime;
        }

        @Override
        public long commandId() {
            return id;
        }

        @Override
        public Future<ResultBean> execute() {
            CompletableFuture<ResultBean> future = new CompletableFuture<>();
            try {
                log.info("执行命令: {}, 名称: {}", id, name);
                // 模拟命令执行时间
                Thread.sleep(executionTime);
                future.complete(ResultBean.success("命令执行成功: " + name));
            } catch (Exception e) {
                log.error("命令执行异常", e);
                future.complete(ResultBean.error("命令执行失败: " + e.getMessage()));
            }
            return future;
        }

        @Override
        public Future<ResultBean> cancel() {
            CompletableFuture<ResultBean> future = new CompletableFuture<>();
            log.info("取消命令: {}, 名称: {}", id, name);
            future.complete(ResultBean.success("命令已取消: " + name));
            return future;
        }
    }
}