package com.swy.common.thread;

import com.swy.common.result.ResultBean;

import java.util.concurrent.Future;

/**
 * 命令接口
 *
 * @author SkyWithYou
 */
public interface Command {

    /**
     * 命令ID
     *
     * @return
     */
    long commandId();

    /**
     * 执行命令
     */
    Future<ResultBean> execute();

    /**
     * 取消命令
     */
    Future<ResultBean> cancel();

    /**
     * 开始时间
     */
    long startTime();

    /**
     * 是否取消
     * @return
     */
    boolean isCancel();
}
