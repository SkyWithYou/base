package com.swy.common.thread;

import com.swy.common.result.ResultBean;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 命令线程池示例
 * <p>
 * 演示如何使用基于权重分配队列的线程池和不公平锁消费队列
 * </p>
 *
 * @author SkyWithYou
 */
@Slf4j
public class CommandThreadPoolDemo {

    /**
     * 命令ID生成器
     */
    private static final AtomicLong COMMAND_ID_GENERATOR = new AtomicLong(1);

    /**
     * 运行示例
     */
    public static void main(String[] args) throws Exception {
        // 创建不同优先级的命令队列
        PriorityCommandQueue<DemoCommand> highPriorityQueue = 
                new PriorityCommandQueue<>("high-priority", 10, 100, "high");
        
        PriorityCommandQueue<DemoCommand> mediumPriorityQueue = 
                new PriorityCommandQueue<>("medium-priority", 5, 100, "medium");
        
        PriorityCommandQueue<DemoCommand> lowPriorityQueue = 
                new PriorityCommandQueue<>("low-priority", 1, 100, "low");

        // 启动队列处理
        highPriorityQueue.start();
        mediumPriorityQueue.start();
        lowPriorityQueue.start();

        // 添加测试命令到不同优先级的队列
        for (int i = 0; i < 20; i++) {
            // 高优先级队列添加少量命令
            if (i % 10 == 0) {
                highPriorityQueue.addCommand(new DemoCommand("高优先级任务-" + i, 100));
            }
            
            // 中优先级队列添加适量命令
            if (i % 5 == 0) {
                mediumPriorityQueue.addCommand(new DemoCommand("中优先级任务-" + i, 200));
            }
            
            // 低优先级队列添加大量命令
            lowPriorityQueue.addCommand(new DemoCommand("低优先级任务-" + i, 300));
        }

        // 打印线程池状态
        log.info("线程池初始状态: {}", CommandThreadPoolManager.getInstance().getThreadPoolStatus());

        // 等待一段时间，让任务执行
        Thread.sleep(2000);

        // 再次打印线程池状态
        log.info("线程池执行后状态: {}", CommandThreadPoolManager.getInstance().getThreadPoolStatus());

        // 停止线程池
        CommandThreadPoolManager.getInstance().stop();
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