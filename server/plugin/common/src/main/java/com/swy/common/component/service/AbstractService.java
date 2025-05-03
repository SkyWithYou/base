package com.swy.common.component.service;

import com.swy.common.component.lifecycle.AbstractLifecycleBase;

/**
 * 抽象服务
 *
 * @author SkyWithYou
 */
public abstract class AbstractService extends AbstractLifecycleBase {

    /**
     * 获取服务名称
     *
     * @return 服务名称
     */
    String getServiceName() {
        return this.getClass().getSimpleName();
    }
}
